package io.github.dennisochulor.tickrate.mixin.client.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ParticleGroup.class)
public class ParticleGroupMixin {

    @WrapOperation(method = "tickParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;tick()V"))
    private void tickParticle(Particle particle, Operation<Void> original) { // particle ticking
        if(TickRateClientManager.serverHasMod()) {
            // Modded particle types can't be accounted for properly, so just give em playerChunkI
            ParticleRenderType renderType = particle.getGroup();
            boolean isVanillaParticle = renderType == ParticleRenderType.SINGLE_QUADS || renderType == ParticleRenderType.NO_RENDER || renderType == ParticleRenderType.ELDER_GUARDIANS || renderType == ParticleRenderType.ITEM_PICKUP;
            int i = isVanillaParticle ? TickRateClientManager.getChunkDeltaTrackerInfo(new ChunkPos(particle.tickRate$getBlockPos())).i() : TickRateClientManager.getChunkDeltaTrackerInfo(Minecraft.getInstance().player.chunkPosition()).i();

            DeltaTracker deltaTracker = Minecraft.getInstance().getDeltaTracker();
            if(deltaTracker.tickRate$getMovingI() < i) {
                original.call(particle);
            }
        }
        else original.call(particle);
    }

}
