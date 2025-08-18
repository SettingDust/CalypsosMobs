package settingdust.calypsos_mobs

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import settingdust.calypsos_mobs.entity.FurnaceSprite

@Suppress("UNCHECKED_CAST")
interface CalypsosMobsEntities {
    companion object : CalypsosMobsEntities{
        val FURNACE_SPRITE by lazy { BuiltInRegistries.ENTITY_TYPE.get(CalypsosMobsKeys.FURNACE_SPRITE) as EntityType<FurnaceSprite> }

        private val implementations = ServiceLoaderUtil.findServices<CalypsosMobsEntities>()

        override fun registerTypes(register: (ResourceLocation, EntityType<*>) -> Unit) {
            for (implementation in implementations) {
                implementation.registerTypes(register)
            }
        }
    }

    fun registerTypes(register: (ResourceLocation, EntityType<*>) -> Unit)
}