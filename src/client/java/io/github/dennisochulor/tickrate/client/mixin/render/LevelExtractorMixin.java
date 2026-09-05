package io.github.dennisochulor.tickrate.client.mixin.render;

import io.github.dennisochulor.tickrate.client.PlayerDeltaTracker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelExtractor.class)
public class LevelExtractorMixin {
    @Unique
    private static final PlayerDeltaTracker playerDeltaTracker = new PlayerDeltaTracker();

    @ModifyVariable(method = "extractPlayerState", at = @At("HEAD"), argsOnly = true)
    private DeltaTracker usePlayerDeltaTracker(DeltaTracker deltaTracker) {
        return playerDeltaTracker;
    }

    @ModifyVariable(method = "extractPlayerState", at = @At("HEAD"), argsOnly = true)
    private float usePlayerPartialTicks(float partialTicks) {
        return playerDeltaTracker.getGameTimeDeltaPartialTick(true);
    }
}
