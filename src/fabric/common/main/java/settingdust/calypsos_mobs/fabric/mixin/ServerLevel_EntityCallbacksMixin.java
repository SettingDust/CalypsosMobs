package settingdust.calypsos_mobs.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import settingdust.calypsos_mobs.fabric.ServerEntityCreatedEvents;

@Mixin(targets = "net.minecraft.server.level.ServerLevel$EntityCallbacks")
public class ServerLevel_EntityCallbacksMixin {
    @Shadow
    @Final
    ServerLevel this$0;

    @Inject(method = "onCreated(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void calypsos_mobs$onCreated(final Entity entity, final CallbackInfo ci) {
        ServerEntityCreatedEvents.CREATED.invoker().onCreated(entity, this$0);
    }
}
