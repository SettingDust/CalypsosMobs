package settingdust.calypsos_mobs.entity

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
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
import net.tslat.smartbrainlib.api.core.sensor.vanilla.HurtBySensor
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyPlayersSensor
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearestItemSensor
import net.tslat.smartbrainlib.util.BrainUtils
import settingdust.calypsos_mobs.CalypsosMobsItems
import settingdust.calypsos_mobs.adapter.LoaderAdapter.Companion.getBurnTime
import settingdust.calypsos_mobs.adapter.MinecraftAdapter.Companion.isSameItemSameComponents
import settingdust.calypsos_mobs.brain.behaviour.MoveToNearestVisibleWantedItem
import settingdust.calypsos_mobs.util.HeatLevel
import software.bernie.geckolib.animatable.GeoEntity
import kotlin.math.max
import kotlin.math.min

abstract class FurnaceSprite(type: EntityType<out FurnaceSprite>, level: Level) :
    PathfinderMob(type, level),
    GeoEntity,
    SmartBrainOwner<FurnaceSprite>,
    InventoryCarrier {

    companion object {
        @JvmStatic
        lateinit var INITIALIZED: EntityDataAccessor<Boolean>

        @JvmStatic
        lateinit var SLEEP: EntityDataAccessor<Boolean>

        @JvmStatic
        lateinit var HEAT_LEVEL: EntityDataAccessor<HeatLevel>

        @JvmStatic
        lateinit var PREV_HEAT_LEVEL: EntityDataAccessor<HeatLevel>

        @JvmStatic
        lateinit var WORKING: EntityDataAccessor<Boolean>

        const val SLEEP_THRESHOLD = 60 * 20
    }

    @JvmField
    val inventory = SimpleContainer(1)

    var targetItemEntity: ItemEntity? = null

    var progress = 0.0
    var heat = 0
    var regenTimer = 0
    var sleepyDuration = 0

    override fun getInventory() = inventory

    private fun tryWakeUp() {
        sleepyDuration = 0
        entityData.set(SLEEP, false)
    }

    override fun brainProvider() = SmartBrainProvider(this)

    override fun customServerAiStep() {
        tickBrain(this)
    }

    override fun getSensors() = listOf(
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
                val canMerge by lazy { entity.inventory.getItem(0).isSameItemSameComponents(itemEntity.item) }
                tryWakeUp()
                (entity.inventory.isEmpty || canMerge)
                        && (targetItemEntity == null || targetItemEntity!!.isRemoved
                        || entity.distanceToSqr(targetItemEntity!!) > entity.distanceTo(itemEntity))
            }.cooldownFor { 20 },
            LookAtTarget<FurnaceSprite>().runFor { 20 }.whenStarting { entity ->
                val target = BrainUtils.getMemory(entity, MemoryModuleType.LOOK_TARGET)
                if (target !is EntityTracker) return@whenStarting
                tryWakeUp()
                entity.lookAt(target.entity, 90F, 90F)
            },
            OneRandomBehaviour(
                SetRandomLookTarget<FurnaceSprite>().startCondition { entity ->
                    !entity.entityData.get(SLEEP)
                },
                Idle<FurnaceSprite>().runFor { it.random.nextIntBetweenInclusive(30, 60) }
            )
        )
    )

    override fun getActivityPriorities() = listOf(Activity.PANIC, Activity.IDLE)

    fun defineSyncedData(define: (EntityDataAccessor<out Any>, Any) -> Unit) {
        define(HEAT_LEVEL, HeatLevel.ZERO)
        define(PREV_HEAT_LEVEL, HeatLevel.ZERO)
        define(SLEEP, false)
        define(INITIALIZED, false)
        define(WORKING, false)
    }

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

    fun setHeatLevel(heatLevel: HeatLevel) {
        if (heatLevel == entityData.get(HEAT_LEVEL)) return
        entityData.set(PREV_HEAT_LEVEL, entityData.get(HEAT_LEVEL))
        entityData.set(HEAT_LEVEL, heatLevel)
    }

    override fun pickUpItem(itemEntity: ItemEntity) {
        if (itemEntity.owner == this) return
        triggerAnim("ItemInteract", "Absorb")
        InventoryCarrier.pickUpItem(this, this, itemEntity)
        targetItemEntity = null
    }

    abstract fun triggerSpitAnimation(): Unit

    fun dropAllItems() {
        val forward = forward.scale(0.6)
        val horizontalForward = Vec3(forward.x, 0.0, forward.z).normalize().scale(0.2).add(position())
        this.inventory.removeAllItems()
            .forEach { BehaviorUtils.throwItem(this, it, horizontalForward) }
        triggerSpitAnimation()
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
        val burnTime by lazy { itemInHand.getBurnTime() }
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
                if (level().isClientSide) {
                    triggerAnim("ItemInteract", "Absorb")
                    repeat(4) {
                        val d0 = random.nextGaussian() * 0.02
                        val d1 = random.nextGaussian() * 0.02
                        val d2 = random.nextGaussian() * 0.02
                        this.level().addParticle(
                            ParticleTypes.ANGRY_VILLAGER,
                            getRandomX(1.0),
                            randomY,
                            getRandomZ(1.0),
                            d0,
                            d1,
                            d2
                        )
                    }
                }
                InteractionResult.sidedSuccess(level().isClientSide)
            }

            else -> InteractionResult.PASS
        }
    }

    abstract fun getSmeltingResult(): ItemStack

    abstract override fun canHoldItem(stack: ItemStack): Boolean

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
                    if (sleepyDuration >= SLEEP_THRESHOLD) {
                        entityData.set(SLEEP, true)
                    }
                } else if (entityData.get(SLEEP) && level().random.nextDouble() < 0.2) {
                    tryWakeUp()
                }
                return
            }

            entityData.set(WORKING, true)
            tryWakeUp()

            progress += 1.0 / newHeatLevel.smeltingTicks

            if (progress >= 1.0) {
                val result = getSmeltingResult()
                inventory.getItem(0).shrink(1)
                triggerSpitAnimation()
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
}
