package settingdust.calypsos_mobs.v1_21

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.attributes.Attributes
import settingdust.calypsos_mobs.CalypsosMobsEntities
import settingdust.calypsos_mobs.CalypsosMobsKeys
import settingdust.calypsos_mobs.adapter.LoaderAdapter.Companion.defaultAttributes
import settingdust.calypsos_mobs.adapter.LoaderAdapter.Companion.rendererProvider
import settingdust.calypsos_mobs.v1_21.client.entity.FurnaceSpriteRenderer
import settingdust.calypsos_mobs.v1_21.entity.FurnaceSprite

class CalypsosMobsEntities : CalypsosMobsEntities {
    override fun registerTypes(register: (ResourceLocation, EntityType<*>) -> Unit) {
        register(
            CalypsosMobsKeys.FURNACE_SPRITE,
            EntityType.Builder.of(::FurnaceSprite, MobCategory.CREATURE)
                .sized(1f, 1f)
                .build(CalypsosMobsKeys.FURNACE_SPRITE.toString())
                .apply {
                    defaultAttributes(
                        PathfinderMob.createMobAttributes()
                            .add(Attributes.MAX_HEALTH, 10.0)
                            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                            .add(Attributes.MOVEMENT_SPEED, 0.3)
                            .build()
                    )

                    rendererProvider(::FurnaceSpriteRenderer)
                }
        )
    }
}