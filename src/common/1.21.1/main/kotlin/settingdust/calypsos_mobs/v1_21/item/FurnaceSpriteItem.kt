package settingdust.calypsos_mobs.v1_21.item

import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.gameevent.GameEvent
import settingdust.calypsos_mobs.CalypsosMobsEntities
import settingdust.calypsos_mobs.adapter.LoaderAdapter
import settingdust.calypsos_mobs.entity.FurnaceSprite
import settingdust.calypsos_mobs.item.FurnaceSpriteItem
import kotlin.math.atan2

class FurnaceSpriteItem : FurnaceSpriteItem() {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        components: MutableList<Component>,
        isAdvanced: TooltipFlag
    ) {
        components.add(Component.translatable("item.calypsos_mobs.furnace_sprite.desc"))
        if (!LoaderAdapter.isClient || Screen.hasShiftDown()) {
            repeat(5) {
                components.add(Component.translatable("item.calypsos_mobs.furnace_sprite.desc$it"))
            }
        }
    }

    override fun createEntity(
        level: ServerLevel,
        player: Player?,
        clickedPos: BlockPos,
        pos: BlockPos,
        direction: Direction
    ): FurnaceSprite? {
        return CalypsosMobsEntities.FURNACE_SPRITE.create(
            level,
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
            clickedPos,
            MobSpawnType.SPAWN_EGG,
            true,
            clickedPos != pos && direction === Direction.UP
        )
    }

    override fun triggerGameEvent(level: ServerLevel, player: Player?, pos: BlockPos) {
        level.gameEvent(player, GameEvent.ENTITY_PLACE, pos)
    }
}