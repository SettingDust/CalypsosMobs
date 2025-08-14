package settingdust.calypsos_mobs.neoforge.adapter

import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.EntityRenderers
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeType
import net.neoforged.fml.loading.FMLLoader
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent
import net.neoforged.neoforge.registries.NeoForgeRegistries
import net.neoforged.neoforge.registries.RegisterEvent
import settingdust.calypsos_mobs.adapter.LoaderAdapter
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

class LoaderAdapter : LoaderAdapter {
    override val isClient: Boolean
        get() = FMLLoader.getDist().isClient

    override fun ItemStack.getBurnTime() = getBurnTime(RecipeType.SMELTING)

    override fun <T : Entity> T.onCreatedInLevel(callback: () -> Unit) {
        NeoForge.EVENT_BUS.addListener { event: EntityJoinLevelEvent ->
            if (event.entity == this) callback()
        }
    }

    override fun <T : LivingEntity> EntityType<T>.defaultAttributes(supplier: AttributeSupplier) {
        MOD_BUS.addListener { event: EntityAttributeCreationEvent ->
            event.put(this, supplier)
        }
    }

    override fun <T : Entity> EntityType<T>.rendererProvider(provider: EntityRendererProvider<T>) {
        if (FMLLoader.getDist().isClient) {
            EntityRenderers.register(this, provider)
        }
    }

    override fun <T : Item> T.creativeTab(key: ResourceKey<CreativeModeTab>) {
        MOD_BUS.addListener { event: BuildCreativeModeTabContentsEvent ->
            if (event.tabKey == key) event.accept(this)
        }
    }

    override fun registerEntityDataSerializer(id: ResourceLocation, serializer: EntityDataSerializer<*>) {
        MOD_BUS.addListener { event: RegisterEvent ->
            if (event.registryKey == NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS) {
                event.register(NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, id) { serializer }
            }
        }
    }
}