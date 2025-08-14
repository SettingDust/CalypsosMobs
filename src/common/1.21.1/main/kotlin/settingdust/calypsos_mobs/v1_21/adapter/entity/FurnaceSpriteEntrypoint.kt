package settingdust.calypsos_mobs.v1_21.adapter.entity

import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import settingdust.calypsos_mobs.CalypsosMobsKeys
import settingdust.calypsos_mobs.adapter.Entrypoint
import settingdust.calypsos_mobs.adapter.LoaderAdapter
import settingdust.calypsos_mobs.entity.FurnaceSprite
import settingdust.calypsos_mobs.util.HeatLevel

class FurnaceSpriteEntrypoint : Entrypoint {
    companion object {
        val HEAT_LEVEL_STREAM_CODEC: StreamCodec<ByteBuf, HeatLevel> by lazy {
            ByteBufCodecs.BYTE.map({ HeatLevel.entries[it.toInt()] }, { it.ordinal.toByte() })
        }
    }

    override fun construct() {
        HeatLevel.DATA_SERIALIZER =
            EntityDataSerializer.forValueType(HEAT_LEVEL_STREAM_CODEC).also {
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