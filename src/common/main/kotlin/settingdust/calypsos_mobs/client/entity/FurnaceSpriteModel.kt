package settingdust.calypsos_mobs.client.entity

import net.minecraft.resources.ResourceLocation
import settingdust.calypsos_mobs.CalypsosMobsKeys
import settingdust.calypsos_mobs.entity.FurnaceSprite
import settingdust.calypsos_mobs.mixin.GeoModelAccessor
import settingdust.calypsos_mobs.util.HeatLevel
import settingdust.calypsos_mobs.util.copy
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.model.DefaultedEntityGeoModel

abstract class FurnaceSpriteModel<T : FurnaceSprite> : DefaultedEntityGeoModel<T>(CalypsosMobsKeys.FURNACE_SPRITE) {

    object Variants {
        val CACHE = hashMapOf<Context, BakedGeoModel>()

        data class Context(val working: Boolean, val level: HeatLevel)

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
                        val eyeLBones = buildMap<HeatLevel, GeoBone> {
                            this[HeatLevel.ZERO] = bone.childBones.single { it.name == "eyeL_1" }
                            bone.childBones.forEach {
                                this[HeatLevel.entries[it.name.last().digitToInt()]] = it
                            }
                        }
                        bone.childBones.clear()
                        bone.childBones += eyeLBones[context.level]!!
                    }

                    register { context, model ->
                        val bone = model.getBone("eyeR").orElseThrow()
                        val eyeRBones = buildMap<HeatLevel, GeoBone> {
                            this[HeatLevel.ZERO] = bone.childBones.single { it.name == "eyeR_1" }
                            bone.childBones.forEach {
                                this[HeatLevel.entries[it.name.last().digitToInt()]] = it
                            }
                        }
                        bone.childBones.clear()
                        bone.childBones += eyeRBones[context.level]!!
                    }

                    register { context, model ->
                        val bone = model.getBone("fire").orElseThrow()
                        var fireWorkingBone: GeoBone? = null
                        val fireLevelBones = mutableMapOf<HeatLevel, GeoBone>()
                        for (childBone in bone.childBones) {
                            when {
                                childBone.name == "fire_on" -> fireWorkingBone = childBone
                                childBone.name == "fire_off" -> fireLevelBones[HeatLevel.ZERO] = childBone

                                childBone.name.startsWith("fire_ember_") ->
                                    fireLevelBones[
                                        HeatLevel.entries[childBone.name.last().digitToInt()]
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

    protected var currentEntity: T? = null

    protected abstract fun setAnimationProcessorActiveModel(model: BakedGeoModel)

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
            setAnimationProcessorActiveModel(transformed)
            (this as GeoModelAccessor).setCurrentModel(transformed)
        }
        return transformed
    }

    protected fun recordCurrentEntity(entity: T) {
        currentEntity = entity
    }
}