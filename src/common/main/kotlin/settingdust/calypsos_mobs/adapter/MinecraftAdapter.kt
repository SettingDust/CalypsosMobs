package settingdust.calypsos_mobs.adapter

import net.minecraft.resources.ResourceLocation
import settingdust.calypsos_mobs.ServiceLoaderUtil

interface MinecraftAdapter {
    companion object : MinecraftAdapter by ServiceLoaderUtil.findService()

    fun ResourceLocation(namespace: String, path: String): ResourceLocation
}