package settingdust.calypsos_mobs

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import settingdust.calypsos_mobs.adapter.LoaderAdapter.Companion.creativeTab

interface CalypsosMobsItems {
    companion object : CalypsosMobsItems {
        val FURNACE_SPRITE by lazy {
            BuiltInRegistries.ITEM.get(CalypsosMobsKeys.FURNACE_SPRITE).apply {
                creativeTab(CreativeModeTabs.SPAWN_EGGS)
            }
        }

        private val implementations = ServiceLoaderUtil.findServices<CalypsosMobsItems>()

        override fun registerItems(register: (ResourceLocation, Item) -> Unit) {
            for (implementation in implementations) {
                implementation.registerItems(register)
            }
        }
    }

    fun registerItems(register: (ResourceLocation, Item) -> Unit)
}