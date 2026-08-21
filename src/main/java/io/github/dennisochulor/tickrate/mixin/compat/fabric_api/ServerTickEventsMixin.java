package io.github.dennisochulor.tickrate.mixin.compat.fabric_api;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Make Fabric API's server/level tick events follow server TPS instead of mainloop TPS.
 */
@Mixin(ServerTickEvents.class)
public class ServerTickEventsMixin {
    @Inject(method = "lambda$static$0", at = @At("HEAD"), cancellable = true)
    private static void serverStartTick(ServerTickEvents.StartTick[] callbacks, MinecraftServer server, CallbackInfo ci) {
        if (!server.getTickManager().tickRate$shouldTickServer()) ci.cancel();
    }

    @Inject(method = "lambda$static$2", at = @At("HEAD"), cancellable = true)
    private static void serverEndTick(ServerTickEvents.EndTick[] callbacks, MinecraftServer server, CallbackInfo ci) {
        if (!server.getTickManager().tickRate$shouldTickServer()) ci.cancel();
    }

    @Inject(method = "lambda$static$4", at = @At("HEAD"), cancellable = true)
    private static void levelStartTick(ServerTickEvents.StartWorldTick[] callbacks, ServerWorld level, CallbackInfo ci) {
        if (!level.getServer().getTickManager().tickRate$shouldTickServer()) ci.cancel();
    }

    @Inject(method = "lambda$static$6", at = @At("HEAD"), cancellable = true)
    private static void levelEndTick(ServerTickEvents.EndWorldTick[] callbacks, ServerWorld level, CallbackInfo ci) {
        if (!level.getServer().getTickManager().tickRate$shouldTickServer()) ci.cancel();
    }
}
