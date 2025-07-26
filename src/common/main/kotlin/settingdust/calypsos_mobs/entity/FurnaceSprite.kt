package settingdust.calypsos_mobs.entity

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
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
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
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
import settingdust.calypsos_mobs.getItem
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
        val HEAT: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(FurnaceSprite::class.java, EntityDataSerializers.INT)
        @JvmStatic
        val SLEEPY_DURATION: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(FurnaceSprite::class.java, EntityDataSerializers.INT)
        
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
            0 * 20 to 10 * 20,
            30 * 20 to 8 * 20,
            60 * 20 to 4 * 20,
            90 * 20 to 2 * 20
        )

        val HEAT_TO_PARTICLE = arrayOf<(Level, FurnaceSprite) -> Unit>(
            { level, entity ->
                val forward = entity.forward
                val horizontalForward = Vec3(forward.x, 0.0, forward.z).normalize().scale(0.6)

                val side = Vec3(-forward.z, 0.0, forward.x).normalize()

                level.addParticle(
                    ParticleTypes.FLAME,
                    entity.x + horizontalForward.x + side.x * (entity.random.nextDouble() * 0.4 - 0.2),
                    entity.y + entity.random.nextDouble() * 0.6,
                    entity.z + horizontalForward.z + side.z * (entity.random.nextDouble() * 0.4 - 0.2),
                    0.0, 0.0, 0.0
                )
            },
            { level, entity ->
                val forward = entity.forward
                val horizontalForward = Vec3(forward.x, 0.0, forward.z).normalize().scale(0.6)

                val side = Vec3(-forward.z, 0.0, forward.x).normalize()
                repeat(2) {
                    level.addParticle(
                        ParticleTypes.FLAME,
                        entity.x + horizontalForward.x + side.x * (entity.random.nextDouble() * 0.4 - 0.2),
                        entity.y + entity.random.nextDouble() * 0.6,
                        entity.z + horizontalForward.z + side.z * (entity.random.nextDouble() * 0.4 - 0.2),
                        0.0, 0.0, 0.0
                    )
                }
            },
            { level, entity ->
                val forward = entity.forward
                val horizontalForward = Vec3(forward.x, 0.0, forward.z).normalize().scale(0.6)

                val side = Vec3(-forward.z, 0.0, forward.x).normalize()

                level.addParticle(
                    ParticleTypes.FLAME,
                    entity.x + horizontalForward.x + side.x * (entity.random.nextDouble() * 0.4 - 0.2),
                    entity.y + entity.random.nextDouble() * 0.6,
                    entity.z + horizontalForward.z + side.z * (entity.random.nextDouble() * 0.4 - 0.2),
                    0.0, 0.0, 0.0
                )
                level.addParticle(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    entity.x + horizontalForward.x + side.x * (entity.random.nextDouble() * 0.4 - 0.2),
                    entity.y + entity.random.nextDouble() * 0.6,
                    entity.z + horizontalForward.z + side.z * (entity.random.nextDouble() * 0.4 - 0.2),
                    0.0, 0.0, 0.0
                )
            },
            { level, entity ->
                val forward = entity.forward
                val horizontalForward = Vec3(forward.x, 0.0, forward.z).normalize().scale(0.6)

                val side = Vec3(-forward.z, 0.0, forward.x).normalize()

                repeat(2) {
                    level.addParticle(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        entity.x + horizontalForward.x + side.x * (entity.random.nextDouble() * 0.4 - 0.2),
                        entity.y + entity.random.nextDouble() * 0.6,
                        entity.z + horizontalForward.z + side.z * (entity.random.nextDouble() * 0.4 - 0.2),
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
        add(AnimationController(this@FurnaceSprite, 10) { state ->
            val moving = state.isMoving
            val idling by lazy { state.controller.currentRawAnimation in Animations.WEIGHTED_IDLE.original.keys }
            when {
                tickCount < 2 -> state.setAndContinue(Animations.WAKEUP)
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

    override fun getFightTasks(): BrainActivityGroup<out FurnaceSprite> = BrainActivityGroup.fightTasks(
        Panic<FurnaceSprite>().panicFor { _, _ -> 200 }
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

    override fun defineSynchedData() {
        super.defineSynchedData()
        entityData.define(HEAT, 0)
        entityData.define(SLEEPY_DURATION, 0)
    }

    override fun canHoldItem(stack: ItemStack): Boolean {
        testInventory.setItem(0, stack)
        val recipe = recipeCheck.getRecipeFor(testInventory, level())
        testInventory.clearContent()
        return recipe.isPresent
    }

    override fun pickUpItem(itemEntity: ItemEntity) {
        triggerAnim("ItemInteract", "Absorb")
        InventoryCarrier.pickUpItem(this, this, itemEntity)
        targetItemEntity = null
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        tryWakeUp()
        return super.hurt(source, amount)
    }

    override fun tick() {
        super.tick()
        val prevHeat = entityData.get(HEAT)
        val heat = if (inventory.isEmpty) {
            max(0, prevHeat - 1)
        } else {
            min(HEAT_TO_TIME.last().first, prevHeat + 1)
        }
        entityData.set(HEAT, heat)

        val tierIndex = HEAT_TO_TIME.indexOfLast { (key) -> heat >= key }
        val neededTicks = HEAT_TO_TIME[tierIndex].second

        if (heat > 0 && level().isClientSide)
            HEAT_TO_PARTICLE[tierIndex](level(), this)

        if (inventory.isEmpty) {
            progress = 0.0
            val dataItem = entityData.getItem(SLEEPY_DURATION)
            if (entityData.get(SLEEPY_DURATION) > SLEEP_THRESHOLD) {
                entityData.set(SLEEPY_DURATION, dataItem.value, true)
            } else if (level().isNight || level().getRawBrightness(blockPosition(), 0) < 8) {
                dataItem.value += 1
            }
            return
        }

        tryWakeUp()

        if (level().isClientSide && random.nextDouble() < 0.1) {
            level().playLocalSound(
                blockPosition(),
                SoundEvents.FURNACE_FIRE_CRACKLE,
                SoundSource.AMBIENT,
                1.0f,
                1.0f,
                false
            )
        }

        val forward = forward
        val horizontalForward = Vec3(forward.x, 0.0, forward.z).normalize().scale(0.6)
        val side = Vec3(-forward.z, 0.0, forward.x).normalize()
        level().addParticle(
            ParticleTypes.SMOKE,
            x + horizontalForward.x + side.x * (random.nextDouble() * 0.4 - 0.2),
            y + random.nextDouble() * 0.6,
            z + horizontalForward.z + side.z * (random.nextDouble() * 0.4 - 0.2),
            0.0, 0.3, 0.0
        )

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
    }

    override fun getPickResult() = ItemStack(CalypsosMobsItems.FURNACE_SPRITE)

    override fun dropEquipment() {
        super.dropEquipment()
        this.inventory.removeAllItems().forEach(::spawnAtLocation)
    }

    override fun getBoundingBoxForCulling(): AABB {
        return super.getBoundingBoxForCulling().inflate(0.6)
    }
}