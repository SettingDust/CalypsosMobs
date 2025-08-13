package settingdust.calypsos_mobs.util

import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.cache.`object`.GeoBone

fun BakedGeoModel.copy(): BakedGeoModel {
    return BakedGeoModel(this.topLevelBones().mapTo(mutableListOf()) { it.copy() }, this.properties())
}

fun GeoBone.copy(parent: GeoBone? = null): GeoBone = GeoBone(
    parent ?: this.parent?.copy(null),
    this.name,
    this.mirror,
    this.inflate,
    this.shouldNeverRender(),
    this.reset
).apply {
    pivotX = this@copy.pivotX
    pivotY = this@copy.pivotY
    pivotZ = this@copy.pivotZ

    posX = this@copy.posX
    posY = this@copy.posY
    posZ = this@copy.posZ

    rotX = this@copy.rotX
    rotY = this@copy.rotY
    rotZ = this@copy.rotZ

    scaleX = this@copy.scaleX
    scaleY = this@copy.scaleY
    scaleZ = this@copy.scaleZ

    isHidden = this@copy.isHidden
    setChildrenHidden(this@copy.isHidingChildren)

    cubes.addAll(this@copy.cubes)
    childBones.addAll(this@copy.childBones.map { it.copy(this@copy) })
}