package io.github.dennisochulor.tickrate.mixin.compat.fabric_api;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Make Fabric API's server/level tick events follow server TPS instead of mainloop TPS.
 */
@Mixin(ServerTickEvents.class)
public class ServerTickEventsMixin {
    @Inject(method = "lambda$static$1", at = @At("HEAD"), cancellable = true)
    private static void serverStartTick(ServerTickEvents.StartTick[] callbacks, MinecraftServer server, CallbackInfo ci) {
        if (!server.tickRateManager().tickRate$shouldTickServer()) ci.cancel();
    }

    @Inject(method = "lambda$static$3", at = @At("HEAD"), cancellable = true)
    private static void serverEndTick(ServerTickEvents.EndTick[] callbacks, MinecraftServer server, CallbackInfo ci) {
        if (!server.tickRateManager().tickRate$shouldTickServer()) ci.cancel();
    }

    @Inject(method = "lambda$static$5", at = @At("HEAD"), cancellable = true)
    private static void levelStartTick(ServerTickEvents.StartLevelTick[] callbacks, ServerLevel level, CallbackInfo ci) {
        if (!level.getServer().tickRateManager().tickRate$shouldTickServer()) ci.cancel();
    }

    @Inject(method = "lambda$static$7", at = @At("HEAD"), cancellable = true)
    private static void levelEndTick(ServerTickEvents.EndLevelTick[] callbacks, ServerLevel level, CallbackInfo ci) {
        if (!level.getServer().tickRateManager().tickRate$shouldTickServer()) ci.cancel();
    }
}
