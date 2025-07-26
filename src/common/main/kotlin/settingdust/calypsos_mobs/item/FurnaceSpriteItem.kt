package settingdust.calypsos_mobs.item

import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.gameevent.GameEvent
import settingdust.calypsos_mobs.CalypsosMobsEntities
import kotlin.math.atan2

class FurnaceSpriteItem : Item(Properties().stacksTo(64)) {

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level as? ServerLevel ?: return InteractionResult.SUCCESS
        val itemstack = context.itemInHand
        val pos = context.clickedPos
        val direction = context.clickedFace
        val blockstate = level.getBlockState(pos)
        val finalPos = if (blockstate.getCollisionShape(level, pos).isEmpty) {
            pos
        } else {
            pos.relative(direction)
        }
        val player = context.player
        val entity = CalypsosMobsEntities.FURNACE_SPRITE.create(
            level,
            null,
            { entity ->
                if (player != null) {
                    val dx = player.x - entity.x
                    val dz = player.z - entity.z
                    val angle = atan2(dz, dx)
                    entity.yRot = (angle * (180f / Math.PI)).toFloat() - 90f
                    entity.yRotO = entity.yRot
                    entity.yBodyRotO = entity.yRot
                    entity.yBodyRot = entity.yRot
                    entity.yHeadRotO = entity.yRot
                    entity.yHeadRot = entity.yRot
                }
            },
            finalPos,
            MobSpawnType.SPAWN_EGG,
            true,
            pos != finalPos && direction === Direction.UP
        )
        if (entity != null) {
            level.addFreshEntityWithPassengers(entity)
            itemstack.shrink(1)
            level.gameEvent(context.player, GameEvent.ENTITY_PLACE, finalPos)
        }

        return InteractionResult.CONSUME
    }
}