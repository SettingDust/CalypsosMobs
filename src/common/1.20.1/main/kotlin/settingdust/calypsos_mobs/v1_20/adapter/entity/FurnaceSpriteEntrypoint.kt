package settingdust.calypsos_mobs.v1_20.adapter.entity

import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import settingdust.calypsos_mobs.CalypsosMobsKeys
import settingdust.calypsos_mobs.adapter.Entrypoint
import settingdust.calypsos_mobs.adapter.LoaderAdapter
import settingdust.calypsos_mobs.entity.FurnaceSprite
import settingdust.calypsos_mobs.util.HeatLevel

class FurnaceSpriteEntrypoint : Entrypoint {
    override fun construct() {
        HeatLevel.DATA_SERIALIZER =
            EntityDataSerializer.simpleEnum(HeatLevel::class.java).also {
                LoaderAdapter.registerEntityDataSerializer(CalypsosMobsKeys.HEAT_LEVEL, it)
            }

        FurnaceSprite.INITIALIZED =
            SynchedEntityData.defineId(FurnaceSprite::class.java, EntityDataSerializers.BOOLEAN)
        FurnaceSprite.SLEEP =
            SynchedEntityData.defineId(FurnaceSprite::class.java, EntityDataSerializers.BOOLEAN)
        FurnaceSprite.HEAT_LEVEL =
            SynchedEntityData.defineId(FurnaceSprite::class.java, HeatLevel.DATA_SERIALIZER)
        FurnaceSprite.PREV_HEAT_LEVEL =
            SynchedEntityData.defineId(FurnaceSprite::class.java, HeatLevel.DATA_SERIALIZER)
        FurnaceSprite.WORKING =
            SynchedEntityData.defineId(FurnaceSprite::class.java, EntityDataSerializers.BOOLEAN)
    }
}