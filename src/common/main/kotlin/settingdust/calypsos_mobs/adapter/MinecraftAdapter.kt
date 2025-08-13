package settingdust.calypsos_mobs.adapter

import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.resources.ResourceLocation
import settingdust.calypsos_mobs.ServiceLoaderUtil
import settingdust.calypsos_mobs.util.HeatLevel

interface MinecraftAdapter {
    companion object : MinecraftAdapter by ServiceLoaderUtil.findService()

    fun ResourceLocation(namespace: String, path: String): ResourceLocation

    fun createHeatLevelDataSerializer(): EntityDataSerializer<HeatLevel>
}