package settingdust.calypsos_mobs

import net.minecraft.client.renderer.entity.EntityRenderers
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraftforge.event.entity.EntityAttributeCreationEvent
import settingdust.calypsos_mobs.client.entity.FurnaceSpriteRenderer
import settingdust.calypsos_mobs.entity.FurnaceSprite
import settingdust.calypsos_mobs.mixin.SynchedEntityDataAccessor
import thedarkcolour.kotlinforforge.KotlinModLoadingContext

@Suppress("DEPRECATION", "UNCHECKED_CAST")
object CalypsosMobsEntities {
    val FURNACE_SPRITE by lazy { BuiltInRegistries.ENTITY_TYPE.get(CalypsosMobsKeys.FURNACE_SPRITE) as EntityType<FurnaceSprite> }

    init {
        KotlinModLoadingContext.get().getKEventBus()
            .apply {
                addListener { event: EntityAttributeCreationEvent ->
                    event.put(
                        FURNACE_SPRITE,
                        PathfinderMob.createMobAttributes()
                            .add(Attributes.MAX_HEALTH, 10.0)
                            .add(Attributes.KNOCKBACK_RESISTANCE, 4.0)
                            .add(Attributes.MOVEMENT_SPEED, 0.2)
                            .build()
                    )
                }
            }
    }

    fun init() {
    }

    fun clientInit() {
        EntityRenderers.register(FURNACE_SPRITE, ::FurnaceSpriteRenderer)
    }

    fun register(register: (ResourceLocation, EntityType<*>) -> Unit) {
        register(
            CalypsosMobsKeys.FURNACE_SPRITE,
            EntityType.Builder.of(::FurnaceSprite, MobCategory.CREATURE).sized(1f, 1f)
                .build(CalypsosMobsKeys.FURNACE_SPRITE.toString())
        )
    }
}

internal fun <T> SynchedEntityData.getItem(key: EntityDataAccessor<T>) =
    (this as SynchedEntityDataAccessor).invokeGetItem(key)