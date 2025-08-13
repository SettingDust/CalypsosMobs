package settingdust.calypsos_mobs

import org.apache.logging.log4j.LogManager
import settingdust.calypsos_mobs.adapter.MinecraftAdapter.Companion.ResourceLocation


object CalypsosMobs {
    const val ID = "calypsos_mobs"

    val LOGGER = LogManager.getLogger()

    init {
        ServiceLoaderUtil.defaultLogger = LOGGER
    }

    fun id(path: String) = ResourceLocation(ID, path)
}