package settingdust.calypsos_mobs.forge

import net.minecraft.core.registries.Registries
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.registries.RegisterEvent
import settingdust.calypsos_mobs.CalypsosMobs
import settingdust.calypsos_mobs.CalypsosMobsEntities
import settingdust.calypsos_mobs.CalypsosMobsEntrypoints
import settingdust.calypsos_mobs.CalypsosMobsItems
import thedarkcolour.kotlinforforge.KotlinModLoadingContext

@Mod(CalypsosMobs.ID)
object CalypsosMobsForge {
    init {
        KotlinModLoadingContext.get().getKEventBus().apply {
            addListener { event: FMLCommonSetupEvent ->
                CalypsosMobsEntrypoints.init()
            }
            addListener { event: FMLClientSetupEvent ->
                CalypsosMobsEntrypoints.clientInit()
            }
            @Suppress("UNCHECKED_CAST")
            addListener { event: RegisterEvent ->
                when (event.registryKey) {
                    Registries.ITEM -> CalypsosMobsItems.register { id, value ->
                        event.register(Registries.ITEM, id) { value }
                    }

                    Registries.ENTITY_TYPE -> CalypsosMobsEntities.register { id, value ->
                        event.register(Registries.ENTITY_TYPE, id) { value }
                    }
                }
            }
        }
    }
}