package settingdust.calypsos_mobs.item

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import settingdust.calypsos_mobs.entity.FurnaceSprite

abstract class FurnaceSpriteItem : Item(Properties().stacksTo(64)) {
    abstract fun createEntity(
        level: ServerLevel,
        player: Player?,
        clickedPos: BlockPos,
        pos: BlockPos,
        direction: Direction
    ): FurnaceSprite?

    abstract fun triggerGameEvent(level: ServerLevel, player: Player?, pos: BlockPos)

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level as? ServerLevel ?: return InteractionResult.SUCCESS
        val itemstack = context.itemInHand
        val clickedPos = context.clickedPos
        val direction = context.clickedFace
        val finalPos = if (level.getBlockState(clickedPos).getCollisionShape(level, clickedPos).isEmpty) {
            clickedPos
        } else {
            clickedPos.relative(direction)
        }
        val player = context.player
        val entity = createEntity(level, player, clickedPos, finalPos, direction)
        if (entity != null) {
            level.addFreshEntityWithPassengers(entity)
            itemstack.shrink(1)
            triggerGameEvent(level, player, finalPos)
        }

        return InteractionResult.CONSUME
    }
}