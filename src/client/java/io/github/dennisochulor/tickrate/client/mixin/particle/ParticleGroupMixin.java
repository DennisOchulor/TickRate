package io.github.dennisochulor.tickrate.client.mixin.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.dennisochulor.tickrate.client.TickRateClientManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Objects;

@Mixin(ParticleGroup.class)
public class ParticleGroupMixin {

    @WrapOperation(method = "tickParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;tick()V"))
    private void tickParticle(Particle particle, Operation<Void> original) { // particle ticking
        if(TickRateClientManager.serverHasMod()) {
            Minecraft minecraft = Minecraft.getInstance();
            Objects.requireNonNull(minecraft.player);

            // Modded particle types can't be accounted for properly, so just give em playerChunkI
            ParticleRenderType renderType = particle.getGroup();
            boolean isVanillaParticle = renderType == ParticleRenderType.SINGLE_QUADS || renderType == ParticleRenderType.NO_RENDER ||
                                        renderType == ParticleRenderType.ELDER_GUARDIANS || renderType == ParticleRenderType.ITEM_PICKUP;
            int i = isVanillaParticle ?
                    TickRateClientManager.getChunkDeltaTrackerInfo(ChunkPos.containing(particle.tickRate$getBlockPos())).ticksToDo() :
                    TickRateClientManager.getChunkDeltaTrackerInfo(minecraft.player.chunkPosition()).ticksToDo();

            DeltaTracker deltaTracker = minecraft.getDeltaTracker();
            if(deltaTracker.tickRate$getMovingTicksToDo() < i) {
                original.call(particle);
            }
        }
        else original.call(particle);
    }

}
