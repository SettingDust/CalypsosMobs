package settingdust.calypsos_mobs

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent
import settingdust.calypsos_mobs.item.FurnaceSpriteItem
import thedarkcolour.kotlinforforge.KotlinModLoadingContext

@Suppress("DEPRECATION")
object CalypsosMobsItems {
    val FURNACE_SPRITE by lazy { BuiltInRegistries.ITEM.get(CalypsosMobsKeys.FURNACE_SPRITE) }

    init {
        KotlinModLoadingContext.get().getKEventBus()
            .apply {
                addListener { event: BuildCreativeModeTabContentsEvent ->
                    when (event.tabKey) {
                        CreativeModeTabs.SPAWN_EGGS -> {
                            event.accept(FURNACE_SPRITE)
                        }
                    }
                }
            }
    }

    fun register(register: (ResourceLocation, Item) -> Unit) {
        register(CalypsosMobsKeys.FURNACE_SPRITE, FurnaceSpriteItem())
    }
}