package settingdust.calypsos_mobs.util

import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

enum class HeatLevel(
    val maxHeatTicks: Int,
    val smeltingTicks: Int,
    val activatingParticle: ((Level, LivingEntity) -> Unit)? = null,
    val upgradingParticle: ((Level, LivingEntity) -> Unit)? = null
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
        lateinit var DATA_SERIALIZER: EntityDataSerializer<HeatLevel>

        val last by lazy { entries.last() }

        fun fromHeat(heat: Int) = entries.first { heat <= it.maxHeatTicks }

        private fun addWorkingParticle(
            level: Level,
            entity: Entity,
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

        fun addUpgradingParticle(level: Level, entity: Entity, particle: () -> ParticleOptions?) {
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