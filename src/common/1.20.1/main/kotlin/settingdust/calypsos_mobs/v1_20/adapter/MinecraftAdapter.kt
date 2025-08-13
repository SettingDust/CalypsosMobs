package settingdust.calypsos_mobs.v1_20.adapter

import net.minecraft.network.syncher.EntityDataSerializer
import settingdust.calypsos_mobs.adapter.MinecraftAdapter
import settingdust.calypsos_mobs.util.HeatLevel

class MinecraftAdapter : MinecraftAdapter {
    override fun ResourceLocation(namespace: String, path: String) =
        net.minecraft.resources.ResourceLocation(namespace, path)

    override fun createHeatLevelDataSerializer(): EntityDataSerializer<HeatLevel> =
        EntityDataSerializer.simpleEnum(HeatLevel::class.java)
}