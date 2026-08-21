package io.github.dennisochulor.tickrate.mixin.client.compat.fabric_api;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * START/END_CLIENT_TICK follow client's player TPS
 * START/END_LEVEL_TICK follow server's TPS
 */
@Mixin(ClientTickEvents.class)
public class ClientTickEventsMixin {
    @Inject(method = "lambda$static$4", at = @At("HEAD"), cancellable = true)
    private static void startLevelTick(ClientTickEvents.StartWorldTick[] callbacks, ClientWorld world, CallbackInfo ci) {
        RenderTickCounter deltaTracker = MinecraftClient.getInstance().getRenderTickCounter();
        if (deltaTracker.tickRate$getMovingI() >= deltaTracker.tickRate$getI()) {
            ci.cancel();
        }
    }
}
