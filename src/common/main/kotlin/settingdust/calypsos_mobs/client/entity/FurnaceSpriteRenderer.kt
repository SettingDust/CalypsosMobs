package settingdust.calypsos_mobs.client.entity

import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.resources.ResourceLocation
import settingdust.calypsos_mobs.CalypsosMobsKeys
import settingdust.calypsos_mobs.copy
import settingdust.calypsos_mobs.entity.FurnaceSprite
import settingdust.calypsos_mobs.mixin.GeoModelAccessor
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.model.DefaultedEntityGeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer

class FurnaceSpriteRenderer(context: EntityRendererProvider.Context) :
    GeoEntityRenderer<FurnaceSprite>(context, FurnaceSpriteModel) {
    override fun getMotionAnimThreshold(animatable: FurnaceSprite) = 0.005f
}

object FurnaceSpriteModel : DefaultedEntityGeoModel<FurnaceSprite>(CalypsosMobsKeys.FURNACE_SPRITE) {

    object Variants {
        val CACHE = hashMapOf<Context, BakedGeoModel>()

        data class Context(val working: Boolean, val level: FurnaceSprite.HeatLevel)

        fun interface Transformer {
            companion object {
                val ALL = mutableListOf<Transformer>()

                fun register(transformer: Transformer): Transformer {
                    ALL.add(transformer)
                    return transformer
                }

                init {
                    register { context, model ->
                        val bone = model.getBone("eyeL").orElseThrow()
                        val eyeLBones = buildMap<FurnaceSprite.HeatLevel, GeoBone> {
                            this[FurnaceSprite.HeatLevel.ZERO] = bone.childBones.single { it.name == "eyeL_1" }
                            bone.childBones.forEach {
                                this[FurnaceSprite.HeatLevel.entries[it.name.last().digitToInt()]] = it
                            }
                        }
                        bone.childBones.clear()
                        bone.childBones += eyeLBones[context.level]!!
                    }

                    register { context, model ->
                        val bone = model.getBone("eyeR").orElseThrow()
                        val eyeRBones = buildMap<FurnaceSprite.HeatLevel, GeoBone> {
                            this[FurnaceSprite.HeatLevel.ZERO] = bone.childBones.single { it.name == "eyeR_1" }
                            bone.childBones.forEach {
                                this[FurnaceSprite.HeatLevel.entries[it.name.last().digitToInt()]] = it
                            }
                        }
                        bone.childBones.clear()
                        bone.childBones += eyeRBones[context.level]!!
                    }

                    register { context, model ->
                        val bone = model.getBone("fire").orElseThrow()
                        var fireWorkingBone: GeoBone? = null
                        val fireLevelBones = mutableMapOf<FurnaceSprite.HeatLevel, GeoBone>()
                        for (childBone in bone.childBones) {
                            when {
                                childBone.name == "fire_on" -> fireWorkingBone = childBone
                                childBone.name == "fire_off" -> fireLevelBones[FurnaceSprite.HeatLevel.ZERO] = childBone

                                childBone.name.startsWith("fire_ember_") ->
                                    fireLevelBones[
                                        FurnaceSprite.HeatLevel.entries[childBone.name.last().digitToInt()]
                                    ] = childBone
                            }
                        }
                        bone.childBones.clear()
                        if (context.working) bone.childBones += fireWorkingBone!!
                        else bone.childBones += fireLevelBones[context.level]!!
                    }
                }
            }

            fun transform(context: Context, model: BakedGeoModel)
        }
    }

    private var currentEntity: FurnaceSprite? = null

    override fun getBakedModel(location: ResourceLocation): BakedGeoModel {
        val bakedModel = super.getBakedModel(location)
        if (currentEntity == null) return bakedModel
        val transformed =
            Variants.CACHE.computeIfAbsent(
                Variants.Context(
                    currentEntity!!.entityData.get(FurnaceSprite.WORKING),
                    currentEntity!!.entityData.get(FurnaceSprite.HEAT_LEVEL)
                )
            ) { key ->
                val model = bakedModel.copy()
                Variants.Transformer.ALL.forEach { it.transform(key, model) }
                model
            }
        currentEntity = null
        if (transformed !== bakedModel) {
            this.animationProcessor.setActiveModel(transformed)
            (this as GeoModelAccessor).setCurrentModel(transformed)
        }
        return transformed
    }

    override fun getModelResource(animatable: FurnaceSprite): ResourceLocation {
        currentEntity = animatable
        return super.getModelResource(animatable)
    }
}