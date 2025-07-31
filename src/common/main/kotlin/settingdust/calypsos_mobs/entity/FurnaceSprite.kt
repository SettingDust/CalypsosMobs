package settingdust.calypsos_mobs.entity

import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializer
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
    enum class HeatLevel(
        val maxHeatTicks: Int,
        val smeltingTicks: Int,
        val activatingParticle: ((Level, FurnaceSprite) -> Unit)? = null,
        val upgradingParticle: ((Level, FurnaceSprite) -> Unit)? = null
    ) {
        ZERO(0, 10 * 20),
        ONE(
            30 * 20,
            10 * 20,
            { level, entity -> addWorkingParticle(level, entity, ParticleTypes.FLAME) },
            { level, entity ->
                addUpgradingParticle(level, entity) { if (entity.random.nextBoolean()) ParticleTypes.FLAME else null }
            }
        ),
        TWO(
            60 * 20,
            8 * 20,
            { level, entity -> addWorkingParticle(level, entity, ParticleTypes.FLAME, 2) },
            { level, entity -> addUpgradingParticle(level, entity) { ParticleTypes.FLAME } }
        ),
        THREE(
            90 * 20,
            4 * 20,
            { level, entity ->
                repeat(2) {
                    addWorkingParticle(
                        level,
                        entity,
                        if (entity.random.nextBoolean()) ParticleTypes.FLAME else ParticleTypes.SOUL_FIRE_FLAME
                    )
                }
            },
            { level, entity ->
                addUpgradingParticle(
                    level,
                    entity
                ) { if (entity.random.nextBoolean()) ParticleTypes.FLAME else ParticleTypes.SOUL_FIRE_FLAME }
            }
        ),
        FOUR(
            160 * 20,
            2 * 20,
            { level, entity -> addWorkingParticle(level, entity, ParticleTypes.SOUL_FIRE_FLAME, 2) },
            { level, entity -> addUpgradingParticle(level, entity) { ParticleTypes.SOUL_FIRE_FLAME } }
        );

        companion object {
            val last by lazy { entries.last() }

            fun fromHeat(heat: Int) = HeatLevel.entries.first { heat <= it.maxHeatTicks }

            private fun addWorkingParticle(
                level: Level,
                entity: FurnaceSprite,
                particle: ParticleOptions,
                count: Int = 1
            ) {
                repeat(count) {
                    if (entity.random.nextDouble() > 0.1) return@repeat
                    val offsetX = (entity.random.nextDouble() - 0.5) * 1.2
                    val offsetZ = (entity.random.nextDouble() - 0.5) * 1.2
                    level.addParticle(
                        particle,
                        entity.x + offsetX,
                        entity.y + 1.1 + (entity.random.nextDouble() - 0.5) * 0.2,
                        entity.z + offsetZ,
                        0.0, 0.0, 0.0
                    )
                }
            }

            fun addUpgradingParticle(level: Level, entity: FurnaceSprite, particle: () -> ParticleOptions?) {
                repeat(10 + entity.random.nextInt(4)) {
                    val particleOptions = particle() ?: return@repeat

                    val offset = Vec3.ZERO.offsetRandom(entity.random, 1f)
                        .multiply(1.0, .25, 1.0)
                        .normalize()

                    val translated =
                        entity.position().add(offset.scale(.5 + entity.random.nextDouble() * .125)).add(0.0, .125, 0.0)

                    val speed = offset.scale(1 / 32.0)

                    level.addParticle(
                        particleOptions,
                        translated.x,
                        translated.y,
                        translated.z,
                        speed.x, speed.y, speed.z
                    )
                }
            }
        }

        fun isAtLeast(level: HeatLevel) = this.ordinal >= level.ordinal
    }

    companion object {
        @JvmStatic
        val INITIALIZED: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(FurnaceSprite::class.java, EntityDataSerializers.BOOLEAN)

        @JvmStatic
        val SLEEP: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(FurnaceSprite::class.java, EntityDataSerializers.BOOLEAN)


        val heatLevelSerializer = EntityDataSerializer.simpleEnum(HeatLevel::class.java)
            .apply { EntityDataSerializers.registerSerializer(this) }

        @JvmStatic
        val HEAT_LEVEL: EntityDataAccessor<HeatLevel> =
            SynchedEntityData.defineId(FurnaceSprite::class.java, heatLevelSerializer)

        @JvmStatic
        val PREV_HEAT_LEVEL: EntityDataAccessor<HeatLevel> =
            SynchedEntityData.defineId(FurnaceSprite::class.java, heatLevelSerializer)

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

    private var heat = 0
    private var regenTimer = 0
    private var sleepyDuration = 0

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = geoCache

    override fun registerControllers(registrar: AnimatableManager.ControllerRegistrar): Unit = registrar.run {
        add(AnimationController(this@FurnaceSprite, 0) { state ->
            val moving = state.isMoving
            val idling by lazy { state.controller.currentRawAnimation in Animations.WEIGHTED_IDLE.original.keys }
            when {
                entityData.get(SLEEP) -> state.setAndContinue(Animations.SLEEP)
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
        sleepyDuration = 0
        entityData.set(SLEEP, false)
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
                        || entity.distanceToSqr(targetItemEntity!!) > entity.distanceTo(itemEntity))
            }.cooldownFor { 20 },
            LookAtTarget<FurnaceSprite>().runFor { 20 }.whenStarting { entity ->
                val target = BrainUtils.getMemory(entity, MemoryModuleType.LOOK_TARGET)
                if (target !is EntityTracker) return@whenStarting
                entity.tryWakeUp()
                entity.lookAt(target.entity, 90F, 90F)
            },
            OneRandomBehaviour(
                SetRandomLookTarget<FurnaceSprite>().startCondition { entity ->
                    !entity.entityData.get(SLEEP)
                },
                Idle<FurnaceSprite>().runFor { it.getRandom().nextIntBetweenInclusive(30, 60) }
            )
        )
    )

    override fun getActivityPriorities() = listOf(Activity.PANIC, Activity.IDLE)

    override fun onSyncedDataUpdated(key: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(key)
        when (key) {
            SLEEP -> {
                if (level().isClientSide) {
                    if (!entityData.get(SLEEP)) {
                        triggerAnim("WakeUp", "WakeUp")
                    }
                }
            }

            HEAT_LEVEL -> {
                if (level().isClientSide) {
                    val prevHeatLevel = entityData.get(PREV_HEAT_LEVEL)
                    val heatLevel = entityData.get(HEAT_LEVEL)
                    if (heatLevel === prevHeatLevel) return
                    if (heatLevel.isAtLeast(prevHeatLevel)) {
                        level().playLocalSound(
                            blockPosition(),
                            SoundEvents.BLAZE_SHOOT,
                            soundSource,
                            1.0f,
                            1.0f,
                            false
                        )
                        heatLevel.upgradingParticle?.invoke(level(), this)
                    } else {
                        level().playLocalSound(
                            blockPosition(),
                            SoundEvents.GENERIC_BURN,
                            soundSource,
                            1.0f,
                            1.0f,
                            false
                        )
                        HeatLevel.addUpgradingParticle(level(), this) {
                            if (random.nextBoolean()) ParticleTypes.SMOKE else ParticleTypes.LARGE_SMOKE
                        }
                    }
                }
            }
        }
    }

    override fun defineSynchedData() {
        super.defineSynchedData()
        entityData.define(HEAT_LEVEL, HeatLevel.ZERO)
        entityData.define(PREV_HEAT_LEVEL, HeatLevel.ZERO)
        entityData.define(SLEEP, false)
        entityData.define(INITIALIZED, false)
        entityData.define(WORKING, false)
    }

    fun setHeatLevel(heatLevel: HeatLevel) {
        if (heatLevel == entityData.get(HEAT_LEVEL)) return
        entityData.set(PREV_HEAT_LEVEL, entityData.get(HEAT_LEVEL))
        entityData.set(HEAT_LEVEL, heatLevel)
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
        val itemInHand = player.getItemInHand(hand)
        val burnTime by lazy { ForgeHooks.getBurnTime(itemInHand, RecipeType.SMELTING) }
        return when {
            itemInHand.isEmpty -> {
                player.setItemInHand(hand, inventory.getItem(0))
                inventory.clearContent()
                tryWakeUp()
                InteractionResult.sidedSuccess(level().isClientSide)
            }

            burnTime > 0 -> {
                heat = min(HeatLevel.last.maxHeatTicks, heat + burnTime)
                val heatLevel = HeatLevel.fromHeat(heat)
                setHeatLevel(heatLevel)
                itemInHand.shrink(1)
                tryWakeUp()
                InteractionResult.sidedSuccess(level().isClientSide)
            }

            else -> InteractionResult.PASS
        }
    }

    override fun tick() {
        super.tick()
        yBodyRot = yRot
        yHeadRot = yRot
        yBodyRotO = yRotO
        yHeadRotO = yRotO
        if (!level().isClientSide) {
            heat = if (inventory.isEmpty) {
                max(0, heat - 1)
            } else {
                min(HeatLevel.last.maxHeatTicks, heat + 1)
            }

            val newHeatLevel = HeatLevel.fromHeat(heat)
            setHeatLevel(newHeatLevel)

            if (heat > 0) {
                if (++regenTimer >= 20) heal(1f)
            } else {
                regenTimer = 0
            }

            if (inventory.isEmpty) {
                entityData.set(WORKING, false)
                progress = 0.0
                val sleepy = level().isNight || level().getRawBrightness(blockPosition(), 0) < 8
                if (sleepy) {
                    sleepyDuration++
                } else if (entityData.get(SLEEP) && level().random.nextDouble() < 0.2) {
                    tryWakeUp()
                }
                return
            }

            entityData.set(WORKING, true)
            tryWakeUp()

            progress += 1.0 / newHeatLevel.smeltingTicks

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

            entityData.get(HEAT_LEVEL).activatingParticle?.invoke(level(), this)
        }
    }

    override fun getPickResult() = ItemStack(CalypsosMobsItems.FURNACE_SPRITE)

    override fun getBoundingBoxForCulling(): AABB {
        return super.getBoundingBoxForCulling().inflate(0.6)
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putInt("Heat", heat)
        compound.putBoolean("Initialized", entityData.get(INITIALIZED))
        compound.putInt("SleepyDuration", sleepyDuration)
        compound.putBoolean("Working", entityData.get(WORKING))
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        heat = compound.getInt("Heat")
        entityData.set(HEAT_LEVEL, HeatLevel.fromHeat(heat))
        entityData.set(PREV_HEAT_LEVEL, entityData.get(HEAT_LEVEL))
        entityData.set(INITIALIZED, compound.getBoolean("Initialized"))
        sleepyDuration = compound.getInt("SleepyDuration")
        entityData.set(SLEEP, sleepyDuration >= SLEEP_THRESHOLD)
        entityData.set(WORKING, compound.getBoolean("Working"))
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
