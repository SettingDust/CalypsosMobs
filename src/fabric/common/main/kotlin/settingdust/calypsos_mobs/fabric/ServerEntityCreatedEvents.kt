package settingdust.calypsos_mobs.fabric

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity

object ServerEntityCreatedEvents {
    @JvmField
    val CREATED: Event<Created> = EventFactory.createArrayBacked(Created::class.java) { callbacks ->
        Created { entity, world ->
            for (event in callbacks) {
                event.onCreated(entity, world)
            }
        }
    }

    fun interface Created {
        fun onCreated(entity: Entity, world: ServerLevel)
    }
}