package settingdust.calypsos_mobs.v1_20.adapter

import settingdust.calypsos_mobs.adapter.MinecraftAdapter

class MinecraftAdapter : MinecraftAdapter {
    override fun ResourceLocation(namespace: String, path: String) =
        net.minecraft.resources.ResourceLocation(namespace, path)
}