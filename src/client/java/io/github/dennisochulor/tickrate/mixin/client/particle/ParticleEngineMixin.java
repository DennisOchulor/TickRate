package io.github.dennisochulor.tickrate.mixin.client.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Objects;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/TrackingEmitter;tick()V"))
    private void tick$emitters(TrackingEmitter particle, Operation<Void> original) { // emitter particle ticking
        if(TickRateClientManager.serverHasMod()) {
            DeltaTracker deltaTracker = Minecraft.getInstance().getDeltaTracker();
            if(deltaTracker.tickRate$getMovingTicksToDo() < TickRateClientManager.getChunkDeltaTrackerInfo(new ChunkPos(particle.tickRate$getBlockPos())).ticksToDo()) {
                original.call(particle);
            }
        }
        else original.call(particle);
    }

    @ModifyVariable(method = "extract", at = @At("HEAD"), argsOnly = true)
    private float extract(float partialTick) {
        // Modded particle types can't really be accounted for, so just give em player's chunk partial tick
        return TickRateClientManager.getChunkDeltaTrackerInfo(Objects.requireNonNull(Minecraft.getInstance().player).chunkPosition()).partialTick();
    }

}
