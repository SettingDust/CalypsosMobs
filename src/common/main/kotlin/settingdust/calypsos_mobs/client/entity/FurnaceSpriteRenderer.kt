package settingdust.calypsos_mobs.client.entity

import net.minecraft.client.renderer.entity.EntityRendererProvider
import settingdust.calypsos_mobs.CalypsosMobs
import settingdust.calypsos_mobs.entity.FurnaceSprite
import software.bernie.geckolib.model.DefaultedEntityGeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer

class FurnaceSpriteRenderer(context: EntityRendererProvider.Context) :
    GeoEntityRenderer<FurnaceSprite>(context, DefaultedEntityGeoModel(CalypsosMobs.id("furnace_sprite"))) {
    override fun getMotionAnimThreshold(animatable: FurnaceSprite) = 0.0005f
}