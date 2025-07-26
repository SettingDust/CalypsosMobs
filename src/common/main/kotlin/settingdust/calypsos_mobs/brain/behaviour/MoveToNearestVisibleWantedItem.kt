package settingdust.calypsos_mobs.brain.behaviour

import com.mojang.datafixers.util.Pair
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.behavior.EntityTracker
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.ai.memory.MemoryStatus
import net.minecraft.world.entity.ai.memory.WalkTarget
import net.minecraft.world.phys.Vec3
import net.tslat.smartbrainlib.api.core.behaviour.ExtendedBehaviour
import net.tslat.smartbrainlib.util.BrainUtils


class MoveToNearestVisibleWantedItem<E : PathfinderMob>(
    private val speedModifier: (entity: E, targetPos: Vec3) -> Float = { _: E, _: Vec3 -> 1f }
) : ExtendedBehaviour<E>() {
    companion object {
        private val MEMORY_REQUIREMENTS: List<Pair<MemoryModuleType<*>, MemoryStatus>> =
            listOf(
                Pair.of(
                    MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM,
                    MemoryStatus.VALUE_PRESENT
                )
            )
    }

    override fun getMemoryRequirements() = MEMORY_REQUIREMENTS

    override fun start(entity: E) {
        val itemEntity = BrainUtils.getMemory(entity, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM)!!

        val entityLookTarget = EntityTracker(itemEntity, false)
        val walkTarget =
            WalkTarget(entityLookTarget, this.speedModifier(entity, itemEntity.position()), 0)

        BrainUtils.setMemory(entity, MemoryModuleType.WALK_TARGET, walkTarget)
        BrainUtils.setMemory(entity, MemoryModuleType.LOOK_TARGET, entityLookTarget)
    }
}