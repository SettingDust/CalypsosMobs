package settingdust.calypsos_mobs.forge

import net.minecraft.core.registries.Registries
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.registries.RegisterEvent
import settingdust.calypsos_mobs.CalypsosMobs
import settingdust.calypsos_mobs.CalypsosMobsEntities
import settingdust.calypsos_mobs.CalypsosMobsItems
import settingdust.calypsos_mobs.adapter.Entrypoint
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(CalypsosMobs.ID)
object CalypsosMobsForge {
    init {
        requireNotNull(CalypsosMobs)
        Entrypoint.construct()
        MOD_BUS.apply {
            addListener<FMLCommonSetupEvent> { Entrypoint.init() }
            addListener<FMLClientSetupEvent> { Entrypoint.clientInit() }
            @Suppress("UNCHECKED_CAST")
            addListener { event: RegisterEvent ->
                when (event.registryKey) {
                    Registries.ITEM -> CalypsosMobsItems.registerItems { id, value ->
                        event.register(Registries.ITEM, id) { value }
                    }

                    Registries.ENTITY_TYPE -> CalypsosMobsEntities.registerTypes { id, value ->
                        event.register(Registries.ENTITY_TYPE, id) { value }
                    }
                }
            }
        }
    }
}