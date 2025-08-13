package settingdust.calypsos_mobs.v1_20.util

import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import settingdust.calypsos_mobs.v1_20.entity.FurnaceSprite

enum class HeatLevel(
    val maxHeatTicks: Int,
    val smeltingTicks: Int,
    val activatingParticle: ((Level, FurnaceSprite) -> Unit)? = null,
    val upgradingParticle: ((Level, FurnaceSprite) -> Unit)? = null
) {
    ZERO(0, 10 * 20),
    ONE(
        30 * 20,
        10 * 20,
        { level, entity -> addWorkingParticle(level, entity, ParticleTypes.FLAME) },
        { level, entity ->
            addUpgradingParticle(level, entity) { if (entity.random.nextBoolean()) ParticleTypes.FLAME else null }
        }
    ),
    TWO(
        60 * 20,
        8 * 20,
        { level, entity -> addWorkingParticle(level, entity, ParticleTypes.FLAME, 2) },
        { level, entity -> addUpgradingParticle(level, entity) { ParticleTypes.FLAME } }
    ),
    THREE(
        90 * 20,
        4 * 20,
        { level, entity ->
            repeat(2) {
                addWorkingParticle(
                    level,
                    entity,
                    if (entity.random.nextBoolean()) ParticleTypes.FLAME else ParticleTypes.SOUL_FIRE_FLAME
                )
            }
        },
        { level, entity ->
            addUpgradingParticle(
                level,
                entity
            ) { if (entity.random.nextBoolean()) ParticleTypes.FLAME else ParticleTypes.SOUL_FIRE_FLAME }
        }
    ),
    FOUR(
        160 * 20,
        2 * 20,
        { level, entity -> addWorkingParticle(level, entity, ParticleTypes.SOUL_FIRE_FLAME, 2) },
        { level, entity -> addUpgradingParticle(level, entity) { ParticleTypes.SOUL_FIRE_FLAME } }
    );

    companion object {
        val dataSerializer = EntityDataSerializer.simpleEnum(HeatLevel::class.java)
            .apply { EntityDataSerializers.registerSerializer(this) }

        val last by lazy { entries.last() }

        fun fromHeat(heat: Int) = entries.first { heat <= it.maxHeatTicks }

        private fun addWorkingParticle(
            level: Level,
            entity: FurnaceSprite,
            particle: ParticleOptions,
            count: Int = 1
        ) {
            repeat(count) {
                if (entity.random.nextDouble() > 0.1) return@repeat
                val offsetX = (entity.random.nextDouble() - 0.5) * 1.2
                val offsetZ = (entity.random.nextDouble() - 0.5) * 1.2
                level.addParticle(
                    particle,
                    entity.x + offsetX,
                    entity.y + 1.1 + (entity.random.nextDouble() - 0.5) * 0.2,
                    entity.z + offsetZ,
                    0.0, 0.0, 0.0
                )
            }
        }

        fun addUpgradingParticle(level: Level, entity: FurnaceSprite, particle: () -> ParticleOptions?) {
            repeat(10 + entity.random.nextInt(4)) {
                val particleOptions = particle() ?: return@repeat

                val offset = Vec3.ZERO.offsetRandom(entity.random, 1f)
                    .multiply(1.0, .25, 1.0)
                    .normalize()

                val translated =
                    entity.position().add(offset.scale(.5 + entity.random.nextDouble() * .125)).add(0.0, .125, 0.0)

                val speed = offset.scale(1 / 32.0)

                level.addParticle(
                    particleOptions,
                    translated.x,
                    translated.y,
                    translated.z,
                    speed.x, speed.y, speed.z
                )
            }
        }
    }

    fun isAtLeast(level: HeatLevel) = this.ordinal >= level.ordinal
}