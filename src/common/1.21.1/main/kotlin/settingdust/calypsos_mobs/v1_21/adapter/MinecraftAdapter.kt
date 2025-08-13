package settingdust.calypsos_mobs.v1_21.adapter

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import settingdust.calypsos_mobs.adapter.MinecraftAdapter

class MinecraftAdapter : MinecraftAdapter {
    override fun ResourceLocation(namespace: String, path: String): ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(namespace, path)

    override fun ItemStack.isSameItemSameComponents(other: ItemStack) = ItemStack.isSameItemSameComponents(this, other)
}