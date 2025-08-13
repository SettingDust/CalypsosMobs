package settingdust.calypsos_mobs.fabric

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import settingdust.calypsos_mobs.CalypsosMobs
import settingdust.calypsos_mobs.CalypsosMobsEntities
import settingdust.calypsos_mobs.CalypsosMobsItems
import settingdust.calypsos_mobs.adapter.Entrypoint

object Entrypoint {
    init {
        requireNotNull(CalypsosMobs)
        Entrypoint.construct()
    }

    fun init() {
        CalypsosMobsItems.registerItems { id, item -> Registry.register(BuiltInRegistries.ITEM, id, item) }
        CalypsosMobsEntities.registerTypes { id, type -> Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type) }
        Entrypoint.init()
    }

    fun clientInit() {
        Entrypoint.clientInit()
    }
}
