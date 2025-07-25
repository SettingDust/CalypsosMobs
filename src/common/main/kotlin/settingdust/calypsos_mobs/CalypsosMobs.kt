package settingdust.calypsos_mobs

import net.minecraft.resources.ResourceLocation

object CalypsosMobs {
    const val ID = "calypsos_mobs"

    @Suppress("DEPRECATION", "removal")
    fun id(path: String) = ResourceLocation(ID, path)
}