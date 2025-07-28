package settingdust.calypsos_mobs.entity

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.SimpleContainer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.behavior.BehaviorUtils
import net.minecraft.world.entity.ai.behavior.EntityTracker
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.npc.InventoryCarrier
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.schedule.Activity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraftforge.common.ForgeHooks
import net.tslat.smartbrainlib.api.SmartBrainOwner
import net.tslat.smartbrainlib.api.core.BrainActivityGroup
import net.tslat.smartbrainlib.api.core.SmartBrainProvider
import net.tslat.smartbrainlib.api.core.behaviour.FirstApplicableBehaviour
import net.tslat.smartbrainlib.api.core.behaviour.OneRandomBehaviour
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtTarget
import net.tslat.smartbrainlib.api.core.behaviour.custom.misc.Idle
import net.tslat.smartbrainlib.api.core.behaviour.custom.misc.Panic
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.MoveToWalkTarget
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetRandomLookTarget
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor
import net.tslat.smartbrainlib.api.core.sensor.vanilla.HurtBySensor
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyPlayersSensor
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearestItemSensor
import net.tslat.smartbrainlib.util.BrainUtils
import settingdust.calypsos_mobs.CalypsosMobsItems
import settingdust.calypsos_mobs.WeightedMap
import settingdust.calypsos_mobs.brain.behaviour.MoveToNearestVisibleWantedItem
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.core.animation.AnimatableManager
import software.bernie.geckolib.core.animation.Animation
import software.bernie.geckolib.core.animation.AnimationController
import software.bernie.geckolib.core.animation.RawAnimation
import software.bernie.geckolib.core.`object`.PlayState
import software.bernie.geckolib.util.GeckoLibUtil
import kotlin.math.max
import kotlin.math.min


class FurnaceSprite(type: EntityType<FurnaceSprite>, level: Level) :
    PathfinderMob(type, level),
    GeoEntity,
    SmartBrainOwner<FurnaceSprite>,
    InventoryCarrier {

    companion object {
        @JvmStatic
        val INITIALIZED: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(FurnaceSprite::class.java, EntityDataSerializers.BOOLEAN)

        @JvmStatic
        val HEAT: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(FurnaceSprite::class.java, EntityDataSerializers.INT)

        @JvmStatic
        val SLEEPY_DURATION: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(FurnaceSprite::class.java, EntityDataSerializers.INT)

        @JvmStatic
        val WORKING: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(FurnaceSprite::class.java, EntityDataSerializers.BOOLEAN)

        private object Animations {
            val IDLE = RawAnimation.begin().then("furnace_sprite.idle", Animation.LoopType.PLAY_ONCE)
            val IDLE2 = RawAnimation.begin().then("furnace_sprite.idle2", Animation.LoopType.PLAY_ONCE)
            val IDLE3 = RawAnimation.begin().then("furnace_sprite.idle3", Animation.LoopType.PLAY_ONCE)
            val IDLE4 = RawAnimation.begin().then("furnace_sprite.idle4", Animation.LoopType.PLAY_ONCE)
            val IDLE5 = RawAnimation.begin().then("furnace_sprite.idle5", Animation.LoopType.PLAY_ONCE)

            val WEIGHTED_IDLE = WeightedMap(
                mapOf(
                    IDLE to 1.0,
                    IDLE2 to 0.2,
                    IDLE3 to 0.05,
                    IDLE4 to 0.05,
                    IDLE5 to 0.05
                )
            )

            val WALK = RawAnimation.begin().thenPlay("furnace_sprite.walk")
            val WAKEUP = RawAnimation.begin().thenPlay("furnace_sprite.wake_up")
            val SLEEP = RawAnimation.begin().thenPlay("furnace_sprite.sleep").thenLoop("furnace_sprite.sleep_idle")
            val ABSORB = RawAnimation.begin().thenPlay("furnace_sprite.absorb")
            val SPIT = RawAnimation.begin().thenPlay("furnace_sprite.spit")
            val SPIT2 = RawAnimation.begin().thenPlay("furnace_sprite.spit2")

            val SPITS = arrayOf("Spit", "Spit2")
        }

        val HEAT_TO_TIME = listOf(
            30 * 20 to 10 * 20,
            60 * 20 to 8 * 20,
            90 * 20 to 4 * 20,
            120 * 20 to 2 * 20
        )

        val HEAT_TO_PARTICLE = arrayOf<(Level, FurnaceSprite) -> Unit>(
            { level, entity ->
                if (entity.random.nextDouble() > 0.1) return@arrayOf
                val offsetX = (entity.random.nextDouble() - 0.5) * 1.2
                val offsetZ = (entity.random.nextDouble() - 0.5) * 1.2

                level.addParticle(
                    ParticleTypes.FLAME,
                    entity.x + offsetX,
                    entity.y + 1.1 + (entity.random.nextDouble() - 0.5) * 0.2,
                    entity.z + offsetZ,
                    0.0, 0.0, 0.0
                )
            },
            { level, entity ->
                repeat(2) {
                    val offsetX = (entity.random.nextDouble() - 0.5) * 1.2
                    val offsetZ = (entity.random.nextDouble() - 0.5) * 1.2
                    if (entity.random.nextDouble() > 0.1) return@repeat

                    level.addParticle(
                        ParticleTypes.FLAME,
                        entity.x + offsetX,
                        entity.y + 1.1 + (entity.random.nextDouble() - 0.5) * 0.2,
                        entity.z + offsetZ,
                        0.0, 0.0, 0.0
                    )
                }
            },
            { level, entity ->
                run {
                    val offsetX = (entity.random.nextDouble() - 0.5) * 1.2
                    val offsetZ = (entity.random.nextDouble() - 0.5) * 1.2
                    if (entity.random.nextDouble() > 0.1) return@run

                    level.addParticle(
                        ParticleTypes.FLAME,
                        entity.x + offsetX,
                        entity.y + 1.1 + (entity.random.nextDouble() - 0.5) * 0.2,
                        entity.z + offsetZ,
                        0.0, 0.0, 0.0
                    )
                }

                run {
                    val offsetX = (entity.random.nextDouble() - 0.5) * 1.2
                    val offsetZ = (entity.random.nextDouble() - 0.5) * 1.2
                    if (entity.random.nextDouble() > 0.1) return@run

                    level.addParticle(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        entity.x + offsetX,
                        entity.y + 1.1 + (entity.random.nextDouble() - 0.5) * 0.2,
                        entity.z + offsetZ,
                        0.0, 0.0, 0.0
                    )
                }
            },
            { level, entity ->
                repeat(2) {
                    val offsetX = (entity.random.nextDouble() - 0.5) * 1.2
                    val offsetZ = (entity.random.nextDouble() - 0.5) * 1.2
                    if (entity.random.nextDouble() > 0.1) return@repeat

                    level.addParticle(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        entity.x + offsetX,
                        entity.y + 1.1 + (entity.random.nextDouble() - 0.5) * 0.2,
                        entity.z + offsetZ,
                        0.0, 0.0, 0.0
                    )
                }
            }
        )

        const val SLEEP_THRESHOLD = 60 * 20
    }

    init {
        setPersistenceRequired()
        setCanPickUpLoot(true)
    }

    private val inventory = SimpleContainer(1)
    private val testInventory = SimpleContainer(1)

    private var targetItemEntity: ItemEntity? = null

    override fun getInventory() = inventory

    private val geoCache = GeckoLibUtil.createInstanceCache(this)
    private var progress = 0.0

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = geoCache

    override fun registerControllers(registrar: AnimatableManager.ControllerRegistrar): Unit = registrar.run {
        add(AnimationController(this@FurnaceSprite, 0) { state ->
            val moving = state.isMoving
            val idling by lazy { state.controller.currentRawAnimation in Animations.WEIGHTED_IDLE.original.keys }
            when {
                entityData.get(SLEEPY_DURATION) > SLEEP_THRESHOLD -> state.setAndContinue(Animations.SLEEP)
                !moving && (state.controller.hasAnimationFinished() || !idling) ->
                    state.setAndContinue(Animations.WEIGHTED_IDLE.randomByWeight())

                moving -> state.setAndContinue(Animations.WALK)
                else -> PlayState.CONTINUE
            }
        })
        add(
            AnimationController(this@FurnaceSprite, "ItemInteract") { PlayState.STOP }
                .triggerableAnim("Absorb", Animations.ABSORB)
                .triggerableAnim("Spit", Animations.SPIT)
                .triggerableAnim("Spit2", Animations.SPIT2)
        )
        add(
            AnimationController(this@FurnaceSprite, "WakeUp") { PlayState.STOP }
                .triggerableAnim("WakeUp", Animations.WAKEUP)
        )
    }

    private fun tryWakeUp() {
        if (entityData.get(SLEEPY_DURATION) > SLEEP_THRESHOLD) {
            triggerAnim("WakeUp", "WakeUp")
        }
        entityData.set(SLEEPY_DURATION, 0)
    }

    private val recipeCheck = RecipeManager.createCheck(RecipeType.SMELTING)

    override fun brainProvider() = SmartBrainProvider(this)

    override fun customServerAiStep() {
        tickBrain(this)
    }

    override fun getSensors(): List<ExtendedSensor<out FurnaceSprite>> = listOf(
        NearbyPlayersSensor<FurnaceSprite>()
            .setRadius(10.0, 4.0)
            .setPredicate { player, entity ->
                !player.isSpectator && entity.wantsToPickUp(player.mainHandItem)
            }
            .afterScanning { entity ->
                val player =
                    BrainUtils.getMemory(entity, MemoryModuleType.NEAREST_VISIBLE_PLAYER)
                val target = BrainUtils.getMemory(entity, MemoryModuleType.LOOK_TARGET)
                if (target is EntityTracker && target.entity is Player && player == null) {
                    BrainUtils.clearMemory(entity, MemoryModuleType.LOOK_TARGET)
                } else if (player != null) {
                    BrainUtils.setMemory(entity, MemoryModuleType.LOOK_TARGET, EntityTracker(player, false))
                }
            },
        NearestItemSensor<FurnaceSprite>().setRadius(10.0, 4.0).setPredicate { item, entity ->
            item.owner != entity && entity.wantsToPickUp(item.item) && entity.hasLineOfSight(item)
        },
        HurtBySensor<FurnaceSprite>()
    )

    override fun getCoreTasks(): BrainActivityGroup<out FurnaceSprite> = BrainActivityGroup.coreTasks(
        MoveToWalkTarget<FurnaceSprite>()
    )

    override fun getAdditionalTasks(): Map<Activity, BrainActivityGroup<out FurnaceSprite>> = mapOf(
        Activity.PANIC to BrainActivityGroup<FurnaceSprite>(Activity.PANIC)
            .behaviours(Panic<FurnaceSprite>().panicFor { _, _ -> 10 }.setRadius(3.0))
            .requireAndWipeMemoriesOnUse(MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY)
    )

    override fun getIdleTasks(): BrainActivityGroup<out FurnaceSprite> = BrainActivityGroup.idleTasks(
        FirstApplicableBehaviour(
            MoveToNearestVisibleWantedItem<FurnaceSprite> { _, _ -> 1.25f }.startCondition { entity ->
                val itemEntity = BrainUtils.getMemory(entity, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM)
                    ?: return@startCondition false
                val canMerge by lazy { ItemStack.isSameItemSameTags(entity.inventory.getItem(0), itemEntity.item) }
                entity.tryWakeUp()
                (entity.inventory.isEmpty || canMerge)
                        && (targetItemEntity == null || targetItemEntity!!.isRemoved
                        || entity.distanceToSqr(targetItemEntity) > entity.distanceTo(itemEntity))
            }.cooldownFor { 20 },
            LookAtTarget<FurnaceSprite>().runFor { 20 }.whenStarting { entity ->
                val target = BrainUtils.getMemory(entity, MemoryModuleType.LOOK_TARGET)
                if (target !is EntityTracker) return@whenStarting
                entity.tryWakeUp()
                entity.lookAt(target.entity, 90F, 90F)
            },
            OneRandomBehaviour(
                SetRandomLookTarget<FurnaceSprite>().startCondition { entity ->
                    entity.entityData.get(SLEEPY_DURATION) < SLEEP_THRESHOLD
                },
                Idle<FurnaceSprite>().runFor { it.getRandom().nextIntBetweenInclusive(30, 60) }
            )
        )
    )

    override fun getActivityPriorities() = listOf(Activity.PANIC, Activity.IDLE)

    override fun defineSynchedData() {
        super.defineSynchedData()
        entityData.define(HEAT, 0)
        entityData.define(SLEEPY_DURATION, 0)
        entityData.define(INITIALIZED, false)
        entityData.define(WORKING, false)
    }

    override fun canHoldItem(stack: ItemStack): Boolean {
        testInventory.setItem(0, stack)
        val recipe = recipeCheck.getRecipeFor(testInventory, level())
        testInventory.clearContent()
        return recipe.isPresent
    }

    override fun pickUpItem(itemEntity: ItemEntity) {
        if (itemEntity.owner == this) return
        triggerAnim("ItemInteract", "Absorb")
        InventoryCarrier.pickUpItem(this, this, itemEntity)
        targetItemEntity = null
    }

    private fun dropAllItems() {
        val forward = forward.scale(0.6)
        val horizontalForward = Vec3(forward.x, 0.0, forward.z).normalize().scale(0.2).add(position())
        this.inventory.removeAllItems()
            .forEach { BehaviorUtils.throwItem(this, it, horizontalForward) }
        triggerAnim("ItemInteract", Animations.SPITS[random.nextInt(Animations.SPITS.size - 1)])
    }

    override fun dropEquipment() {
        super.dropEquipment()
        dropAllItems()
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        tryWakeUp()
        dropAllItems()
        return super.hurt(source, amount)
    }

    override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
        val itemInHand by lazy { player.getItemInHand(hand) }
        val burnTime by lazy { ForgeHooks.getBurnTime(itemInHand, RecipeType.SMELTING) }
        return when {
            player.mainHandItem.isEmpty && player.offhandItem.isEmpty -> {
                dropAllItems()
                tryWakeUp()
                InteractionResult.sidedSuccess(level().isClientSide)
            }

            burnTime > 0 -> {
                entityData.set(HEAT, min(HEAT_TO_TIME.last().first, entityData.get(HEAT) + burnTime))
                itemInHand.shrink(1)
                tryWakeUp()
                InteractionResult.sidedSuccess(level().isClientSide)
            }

            else -> InteractionResult.PASS
        }
    }

    private var regenTimer = 0

    override fun tick() {
        super.tick()
        if (!level().isClientSide) {
            val prevHeat = entityData.get(HEAT)
            val heat = if (inventory.isEmpty) {
                max(0, prevHeat - 1)
            } else {
                min(HEAT_TO_TIME.last().first, prevHeat + 1)
            }
            entityData.set(HEAT, heat)

            val neededTicks = HEAT_TO_TIME.first { (key) -> heat <= key }.second

            if (heat > 0 && ++regenTimer >= 20) {
                heal(1f)
            } else {
                regenTimer = 0
            }

            if (inventory.isEmpty) {
                entityData.set(WORKING, false)
                progress = 0.0
                val sleepy = level().isNight || level().getRawBrightness(blockPosition(), 0) < 8
                if (sleepy) {
                    entityData.set(SLEEPY_DURATION, entityData.get(SLEEPY_DURATION) + 1)
                } else if (entityData.get(SLEEPY_DURATION) > SLEEP_THRESHOLD) {
                    tryWakeUp()
                }
                return
            }

            entityData.set(WORKING, true)

            tryWakeUp()

            progress += 1.0 / neededTicks

            if (progress >= 1.0) {
                val recipe = recipeCheck.getRecipeFor(inventory, level()).orElseThrow()
                val result = recipe.assemble(inventory, level().registryAccess())
                inventory.getItem(0).shrink(1)
                triggerAnim("ItemInteract", Animations.SPITS[random.nextInt(Animations.SPITS.size - 1)])
                val forward = forward.scale(0.6)
                val horizontalForward = Vec3(forward.x, 0.0, forward.z).normalize().scale(0.2).add(position())
                BehaviorUtils.throwItem(this, result, horizontalForward)
                progress = 0.0
            }
        } else {
            if (entityData.get(WORKING)) {
                if (random.nextDouble() < 0.1) {
                    playSound(
                        SoundEvents.FURNACE_FIRE_CRACKLE,
                        1.0f,
                        1.0f
                    )
                }
            }

            val heat = entityData.get(HEAT)
            if (heat > 0) {
                val tierIndex = HEAT_TO_TIME.indexOfFirst { (key) -> heat <= key }
                HEAT_TO_PARTICLE[tierIndex](level(), this)
            }
        }
    }

    override fun getPickResult() = ItemStack(CalypsosMobsItems.FURNACE_SPRITE)

    override fun getBoundingBoxForCulling(): AABB {
        return super.getBoundingBoxForCulling().inflate(0.6)
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putBoolean("Initialized", entityData.get(INITIALIZED))
        compound.putInt("Heat", entityData.get(HEAT))
        compound.putInt("SleepyDuration", entityData.get(SLEEPY_DURATION))
        compound.putBoolean("Working", entityData.get(WORKING))
        writeInventoryToTag(compound)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        entityData.set(INITIALIZED, compound.getBoolean("Initialized"))
        entityData.set(HEAT, compound.getInt("Heat"))
        entityData.set(SLEEPY_DURATION, compound.getInt("SleepyDuration"))
        entityData.set(WORKING, compound.getBoolean("Working"))
        readInventoryFromTag(compound)
    }

    override fun onAddedToWorld() {
        super.onAddedToWorld()
        if (level().isClientSide) return
        if (!entityData.get(INITIALIZED)) {
            entityData.set(INITIALIZED, true)
            triggerAnim("WakeUp", "WakeUp")
        }
    }
}
