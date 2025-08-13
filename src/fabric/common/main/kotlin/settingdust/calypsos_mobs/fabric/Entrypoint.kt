package settingdust.calypsos_mobs.fabric

import settingdust.calypsos_mobs.CalypsosMobs
import settingdust.calypsos_mobs.adapter.Entrypoint

fun init() {
    requireNotNull(CalypsosMobs)
    Entrypoint.init()
}

fun clientInit() {
    Entrypoint.clientInit()
}