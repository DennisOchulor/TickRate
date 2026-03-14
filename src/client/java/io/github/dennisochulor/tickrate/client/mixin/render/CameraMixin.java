package io.github.dennisochulor.tickrate.client.mixin.render;

import io.github.dennisochulor.tickrate.client.PlayerDeltaTracker;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Camera.class)
abstract class CameraMixin {

    @Unique private static final PlayerDeltaTracker playerDeltaTracker = new PlayerDeltaTracker();

    @ModifyVariable(method = "getCameraEntityPartialTicks", at = @At("HEAD"), argsOnly = true)
    private DeltaTracker getCameraEntityPartialTicks(DeltaTracker deltaTracker) {
        return playerDeltaTracker;
    }

}
