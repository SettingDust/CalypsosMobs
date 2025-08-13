package settingdust.calypsos_mobs.v1_20.fabric

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import settingdust.calypsos_mobs.adapter.Entrypoint
import settingdust.calypsos_mobs.v1_20.CalypsosMobsEntities
import settingdust.calypsos_mobs.v1_20.CalypsosMobsItems

class Entrypoint : Entrypoint {
    override fun init() {
        CalypsosMobsItems.registerItems { id, item -> Registry.register(BuiltInRegistries.ITEM, id, item) }
        CalypsosMobsEntities.registerTypes { id, type -> Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type) }
    }
}