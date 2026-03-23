package io.github.dennisochulor.tickrate.client.mixin.compat.fabric_api;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
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
    @Inject(method = "lambda$static$5", at = @At("HEAD"), cancellable = true)
    private static void startLevelTick(ClientTickEvents.StartLevelTick[] callbacks, ClientLevel level, CallbackInfo ci) {
        DeltaTracker deltaTracker = Minecraft.getInstance().getDeltaTracker();
        if (deltaTracker.tickRate$getMovingTicksToDo() >= deltaTracker.tickRate$getTicksToDo()) {
            ci.cancel();
        }
    }
}
