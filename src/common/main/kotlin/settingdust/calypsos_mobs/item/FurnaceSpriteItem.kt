package settingdust.calypsos_mobs.item

import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.gameevent.GameEvent
import settingdust.calypsos_mobs.CalypsosMobsEntities

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
        if (CalypsosMobsEntities.FURNACE_SPRITE.spawn(
                level,
                itemstack,
                context.player,
                finalPos,
                MobSpawnType.SPAWN_EGG,
                true,
                pos != finalPos && direction === Direction.UP
            ) != null
        ) {
            itemstack.shrink(1)
            level.gameEvent(context.player, GameEvent.ENTITY_PLACE, finalPos)
        }

        return InteractionResult.CONSUME
    }
}