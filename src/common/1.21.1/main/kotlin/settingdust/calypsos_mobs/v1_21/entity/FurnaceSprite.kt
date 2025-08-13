package settingdust.calypsos_mobs.v1_21.entity

import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.SingleRecipeInput
import net.minecraft.world.level.Level
import settingdust.calypsos_mobs.WeightedMap
import settingdust.calypsos_mobs.adapter.LoaderAdapter.Companion.onCreatedInLevel
import settingdust.calypsos_mobs.entity.FurnaceSprite
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.animation.Animation
import software.bernie.geckolib.animation.AnimationController
import software.bernie.geckolib.animation.PlayState
import software.bernie.geckolib.animation.RawAnimation
import software.bernie.geckolib.util.GeckoLibUtil

class FurnaceSprite(type: EntityType<settingdust.calypsos_mobs.v1_21.entity.FurnaceSprite>, level: Level) :
    FurnaceSprite(type, level) {

    companion object {
        private object Animations {
            val IDLE = RawAnimation.begin().then("furnace_sprite.idle", Animation.LoopType.PLAY_ONCE)
            val IDLE2 = RawAnimation.begin().then("furnace_sprite.idle2", Animation.LoopType.PLAY_ONCE)
            val IDLE3 = RawAnimation.begin().then("furnace_sprite.idle3", Animation.LoopType.PLAY_ONCE)
            val IDLE4 = RawAnimation.begin().then("furnace_sprite.idle4", Animation.LoopType.PLAY_ONCE)
            val IDLE5 = RawAnimation.begin().then("furnace_sprite.idle5", Animation.LoopType.PLAY_ONCE)

            val WEIGHTED_IDLE = WeightedMap(
                mapOf(
                    IDLE to 1.0,
                    IDLE2 to 0.2,
                    IDLE3 to 0.05,
                    IDLE4 to 0.05,
                    IDLE5 to 0.05
                )
            )

            val WALK = RawAnimation.begin().thenPlay("furnace_sprite.walk")
            val WAKEUP = RawAnimation.begin().thenPlay("furnace_sprite.wake_up")
            val SLEEP = RawAnimation.begin().thenPlay("furnace_sprite.sleep").thenLoop("furnace_sprite.sleep_idle")
            val ABSORB = RawAnimation.begin().thenPlay("furnace_sprite.absorb")
            val SPIT = RawAnimation.begin().thenPlay("furnace_sprite.spit")
            val SPIT2 = RawAnimation.begin().thenPlay("furnace_sprite.spit2")

            val SPITS = arrayOf("Spit", "Spit2")
        }
    }

    init {
        setPersistenceRequired()
        setCanPickUpLoot(true)

        onCreatedInLevel {
            if (level().isClientSide) return@onCreatedInLevel
            triggerAnim("WakeUp", "WakeUp")
        }
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        defineSyncedData { key, value -> builder.define(key as EntityDataAccessor<Any>, value) }
    }

    private val geoCache = GeckoLibUtil.createInstanceCache(this)

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = geoCache

    override fun registerControllers(registrar: AnimatableManager.ControllerRegistrar): Unit = registrar.run {
        add(AnimationController(this@FurnaceSprite, 0) { state ->
            val moving = state.isMoving
            val idling by lazy { state.controller.currentRawAnimation in Animations.WEIGHTED_IDLE.original.keys }
            when {
                entityData.get(SLEEP) -> state.setAndContinue(Animations.SLEEP)
                !moving && (state.controller.hasAnimationFinished() || !idling) ->
                    state.setAndContinue(Animations.WEIGHTED_IDLE.randomByWeight())

                moving -> state.setAndContinue(Animations.WALK)
                else -> PlayState.CONTINUE
            }
        })
        add(
            AnimationController(this@FurnaceSprite, "ItemInteract") { PlayState.STOP }
                .triggerableAnim("Absorb", Animations.ABSORB)
                .triggerableAnim("Spit", Animations.SPIT)
                .triggerableAnim("Spit2", Animations.SPIT2)
        )
        add(
            AnimationController(this@FurnaceSprite, "WakeUp") { PlayState.STOP }
                .triggerableAnim("WakeUp", Animations.WAKEUP)
        )
    }

    private val recipeCheck = RecipeManager.createCheck(RecipeType.SMELTING)

    override fun canHoldItem(stack: ItemStack): Boolean {
        val recipe = recipeCheck.getRecipeFor(SingleRecipeInput(stack), level())
        return recipe.isPresent
    }

    override fun getSmeltingResult(): ItemStack {
        val input = SingleRecipeInput(inventory.getItem(0))
        val recipe = recipeCheck.getRecipeFor(input, level()).orElseThrow().value()
        return recipe.assemble(input, level().registryAccess())
    }

    override fun triggerSpitAnimation() =
        triggerAnim("ItemInteract", Animations.SPITS[random.nextInt(Animations.SPITS.size - 1)])
}