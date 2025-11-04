package io.github.dennisochulor.tickrate.mixin.client.particle;

import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Since 1.21.9, Particle#render no longer exists.
 * Different ParticleRenderers can decide how to render their particles on their own.
 * So there is no choice but to individually mixin to each ParticleRenderer, luckily vanilla has only 4 (and the NoRender one is moot).
 * However, for modded ParticleRenderers, idk man...
 */
@Mixin(targets = "net/minecraft/client/particle/ItemPickupParticleGroup$ParticleInstance")
public class ItemPickupParticleGroupMixin {

    @ModifyVariable(method = "fromParticle", at = @At("HEAD"), argsOnly = true)
    private static float fromParticle(float tickProgress, ItemPickupParticle particle) {
        return TickRateClientManager.getChunkDeltaTrackerInfo(new ChunkPos(particle.tickRate$getBlockPos())).partialTick();
    }

}
