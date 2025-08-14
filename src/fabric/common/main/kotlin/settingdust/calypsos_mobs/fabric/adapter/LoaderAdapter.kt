package settingdust.calypsos_mobs.fabric.adapter

import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.fabric.api.registry.FuelRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import settingdust.calypsos_mobs.adapter.LoaderAdapter
import settingdust.calypsos_mobs.fabric.ServerEntityCreatedEvents

class LoaderAdapter : LoaderAdapter {
    override val isClient: Boolean
        get() = FabricLoader.getInstance().environmentType === EnvType.CLIENT

    override fun ItemStack.getBurnTime(): Int = FuelRegistry.INSTANCE.get(item)

    override fun <T : Entity> T.onCreatedInLevel(callback: () -> Unit) =
        ServerEntityCreatedEvents.CREATED.register { entity, _ ->
            if (entity === this) callback()
        }

    override fun <T : LivingEntity> EntityType<T>.defaultAttributes(supplier: AttributeSupplier) {
        FabricDefaultAttributeRegistry.register(this, supplier)
    }

    override fun <T : Entity> EntityType<T>.rendererProvider(provider: EntityRendererProvider<T>) {
        EntityRendererRegistry.register(this, provider)
    }

    override fun <T : Item> T.creativeTab(key: ResourceKey<CreativeModeTab>) {
        ItemGroupEvents.modifyEntriesEvent(key).register { it.accept(this) }
    }

    override fun registerEntityDataSerializer(id: ResourceLocation, serializer: EntityDataSerializer<*>) {
        EntityDataSerializers.registerSerializer(serializer)
    }
}