package io.github.dennisochulor.tickrate.mixin.client.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.EmitterParticle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/EmitterParticle;tick()V"))
    private void tick$emitters(EmitterParticle particle, Operation<Void> original) { // emitter particle ticking
        if(TickRateClientManager.serverHasMod()) {
            RenderTickCounter renderTickCounter = MinecraftClient.getInstance().getRenderTickCounter();
            if(renderTickCounter.tickRate$getMovingI() < TickRateClientManager.getChunkTickProgress(new ChunkPos(particle.tickRate$getBlockPos())).i()) {
                original.call(particle);
            }
        }
        else original.call(particle);
    }

}
