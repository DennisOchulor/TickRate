package io.github.dennisochulor.tickrate.mixin.client.particle;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.BillboardParticleRenderer;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Since 1.21.9, Particle#render no longer exists.
 * Different ParticleRenderers can decide how to render their particles on their own.
 * So there is no choice but to individually mixin to each ParticleRenderer, luckily vanilla has only 4 (and the NoRender one is moot).
 * However, for modded ParticleRenderers, idk man...
 */
@Mixin(BillboardParticleRenderer.class)
public class BillboardParticleRendererMixin {

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/BillboardParticle;render(Lnet/minecraft/client/particle/BillboardParticleSubmittable;Lnet/minecraft/client/render/Camera;F)V"))
    private float render(float tickProgress, @Local BillboardParticle particle) {
        return TickRateClientManager.getChunkTickProgress(new ChunkPos(particle.tickRate$getBlockPos())).tickProgress();
    }

}
