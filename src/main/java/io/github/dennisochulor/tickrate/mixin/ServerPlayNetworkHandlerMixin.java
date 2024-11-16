package io.github.dennisochulor.tickrate.mixin;

import io.github.dennisochulor.tickrate.TickRateTickManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void tick(CallbackInfo ci) {
        ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler) (Object) this;
        TickRateTickManager tickManager = (TickRateTickManager) handler.getPlayer().getServer().getTickManager();
        if(!tickManager.tickRate$shouldTickEntity(handler.getPlayer())) ci.cancel();
    }

}
