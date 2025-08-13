package settingdust.calypsos_mobs.v1_21.adapter

import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.resources.ResourceLocation
import settingdust.calypsos_mobs.adapter.MinecraftAdapter
import settingdust.calypsos_mobs.util.HeatLevel

val HEAT_LEVEL_STREAM_CODEC by lazy {
    ByteBufCodecs.BYTE.map({ HeatLevel.entries[it.toInt()] }, { it.ordinal.toByte() })
}

class MinecraftAdapter : MinecraftAdapter {
    override fun ResourceLocation(namespace: String, path: String): ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(namespace, path)

    override fun createHeatLevelDataSerializer(): EntityDataSerializer<HeatLevel> =
        EntityDataSerializer.forValueType(HEAT_LEVEL_STREAM_CODEC)
}