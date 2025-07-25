package settingdust.calypsos_mobs.item

import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.UseAnim
import net.minecraft.world.item.context.UseOnContext
import settingdust.calypsos_mobs.CalypsosMobsEntities
import kotlin.math.atan2

class FurnaceSpriteItem : Item(Properties().stacksTo(64)) {

    override fun useOn(context: UseOnContext): InteractionResult {
        val entity = CalypsosMobsEntities.FURNACE_SPRITE.create(context.level)!!
        entity.setPos(context.clickLocation)
        val player = context.player
        if (player != null) {
            val dx = player.x - entity.x
            val dz = player.z - entity.z
            val yRot = (atan2(dz, dx) * (180f / Math.PI)).toFloat() - 90f

            entity.yRot = yRot
            entity.yHeadRot = yRot
            entity.yBodyRot = yRot
        }
        context.level.addFreshEntity(entity)
        context.itemInHand.shrink(1)
        return InteractionResult.sidedSuccess(context.level.isClientSide);
    }

    override fun getUseAnimation(stack: ItemStack) = UseAnim.BLOCK
}