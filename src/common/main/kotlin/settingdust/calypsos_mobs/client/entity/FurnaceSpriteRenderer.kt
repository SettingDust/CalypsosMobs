package settingdust.calypsos_mobs.client.entity

import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.resources.ResourceLocation
import settingdust.calypsos_mobs.CalypsosMobsKeys
import settingdust.calypsos_mobs.copy
import settingdust.calypsos_mobs.entity.FurnaceSprite
import settingdust.calypsos_mobs.mixin.GeoModelAccessor
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.model.DefaultedEntityGeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer

class FurnaceSpriteRenderer(context: EntityRendererProvider.Context) :
    GeoEntityRenderer<FurnaceSprite>(context, FurnaceSpriteModel) {
    override fun getMotionAnimThreshold(animatable: FurnaceSprite) = 0.005f
}

object FurnaceSpriteModel : DefaultedEntityGeoModel<FurnaceSprite>(CalypsosMobsKeys.FURNACE_SPRITE) {

    object Variants {
        val CACHE = hashMapOf<Key, BakedGeoModel>()

        data class Key(val lit: Boolean)

        fun interface Transformer {
            companion object {
                val ALL = mutableListOf<Transformer>()

                fun register(transformer: Transformer): Transformer {
                    ALL.add(transformer)
                    return transformer
                }

                init {
                    register { key, model ->
                        val working = key.lit
                        val bone = model.getBone("fire").orElseThrow()
                        if (working) {
                            bone.childBones.removeAt(bone.childBones.indexOfFirst { it.name == "fire_off" })
                        } else {
                            bone.childBones.removeAt(bone.childBones.indexOfFirst { it.name == "fire_on" })
                        }
                    }
                }
            }

            fun transform(key: Key, model: BakedGeoModel)
        }
    }

    private var currentEntity: FurnaceSprite? = null

    override fun getBakedModel(location: ResourceLocation): BakedGeoModel {
        val bakedModel = super.getBakedModel(location)
        if (currentEntity == null) return bakedModel
        val transformed =
            Variants.CACHE.computeIfAbsent(Variants.Key(currentEntity!!.entityData.get(FurnaceSprite.WORKING))) { key ->
                val model = bakedModel.copy()
                Variants.Transformer.ALL.forEach { it.transform(key, model) }
                model
            }
        currentEntity = null
        if (transformed !== bakedModel) {
            this.animationProcessor.setActiveModel(transformed)
            (this as GeoModelAccessor).currentModel = transformed
        }
        return transformed
    }

    override fun getModelResource(animatable: FurnaceSprite): ResourceLocation {
        currentEntity = animatable
        return super.getModelResource(animatable)
    }
}