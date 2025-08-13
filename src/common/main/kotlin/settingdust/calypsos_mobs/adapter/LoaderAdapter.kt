package settingdust.calypsos_mobs.adapter

import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import settingdust.calypsos_mobs.ServiceLoaderUtil

interface LoaderAdapter {
    companion object : LoaderAdapter by ServiceLoaderUtil.findService()

    val isClient: Boolean

    fun ItemStack.getBurnTime(): Int

    fun <T : Entity> T.onCreatedInLevel(callback: () -> Unit)

    fun <T : LivingEntity> EntityType<T>.defaultAttributes(supplier: AttributeSupplier)

    fun <T : Entity> EntityType<T>.rendererProvider(provider: EntityRendererProvider<T>)

    fun <T : Item> T.creativeTab(key: ResourceKey<CreativeModeTab>)
}