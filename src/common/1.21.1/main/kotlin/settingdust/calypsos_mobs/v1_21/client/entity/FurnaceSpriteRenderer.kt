package settingdust.calypsos_mobs.v1_21.client.entity

import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.resources.ResourceLocation
import settingdust.calypsos_mobs.v1_21.entity.FurnaceSprite
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer

class FurnaceSpriteRenderer(context: EntityRendererProvider.Context) :
    GeoEntityRenderer<FurnaceSprite>(context, FurnaceSpriteModel)

object FurnaceSpriteModel : settingdust.calypsos_mobs.client.entity.FurnaceSpriteModel<FurnaceSprite>() {
    override fun setAnimationProcessorActiveModel(model: BakedGeoModel) = animationProcessor.setActiveModel(model)

    override fun getModelResource(animatable: FurnaceSprite): ResourceLocation {
        recordCurrentEntity(animatable)
        return super.getModelResource(animatable)
    }
}