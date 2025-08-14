package settingdust.calypsos_mobs.v1_20

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import settingdust.calypsos_mobs.CalypsosMobsItems
import settingdust.calypsos_mobs.CalypsosMobsKeys
import settingdust.calypsos_mobs.adapter.LoaderAdapter.Companion.creativeTab
import settingdust.calypsos_mobs.v1_20.item.FurnaceSpriteItem

class CalypsosMobsItems : CalypsosMobsItems {
    override fun registerItems(register: (ResourceLocation, Item) -> Unit) {
        register(CalypsosMobsKeys.FURNACE_SPRITE, FurnaceSpriteItem().apply {
            creativeTab(CreativeModeTabs.SPAWN_EGGS)
        })
    }
}