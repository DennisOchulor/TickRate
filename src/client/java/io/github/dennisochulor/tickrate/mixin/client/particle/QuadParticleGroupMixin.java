package io.github.dennisochulor.tickrate.mixin.client.particle;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Since 1.21.9, Particle#render no longer exists.
 * Different ParticleRenderers can decide how to render their particles on their own.
 * So there is no choice but to individually mixin to each ParticleRenderer, luckily vanilla has only 4 (and the NoRender one is moot).
 * However, for modded ParticleRenderers, idk man...
 */
@Mixin(QuadParticleGroup.class)
public class QuadParticleGroupMixin {

    @ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/SingleQuadParticle;extract(Lnet/minecraft/client/renderer/state/QuadParticleRenderState;Lnet/minecraft/client/Camera;F)V"))
    private float extractRenderState(float partialTick, @Local SingleQuadParticle particle) {
        return TickRateClientManager.getChunkDeltaTrackerInfo(new ChunkPos(particle.tickRate$getBlockPos())).partialTick();
    }

}
