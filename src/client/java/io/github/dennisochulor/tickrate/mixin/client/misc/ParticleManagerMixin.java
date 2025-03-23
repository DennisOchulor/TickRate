package io.github.dennisochulor.tickrate.mixin.client.misc;

import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.EmitterParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    @Redirect(method = "tickParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;tick()V"))
    private void tickParticle(Particle particle) {
        if(TickRateClientManager.serverHasMod()) {
            RenderTickCounter renderTickCounter = MinecraftClient.getInstance().getRenderTickCounter();
            if(renderTickCounter.tickRate$getMovingI() < TickRateClientManager.getChunkTickDelta(ChunkPos.toLong(particle.tickRate$getBlockPos())).i()) {
                particle.tick();
            }
        }
        else particle.tick();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/EmitterParticle;tick()V"))
    public void tick$emitters(EmitterParticle particle) {
        if(TickRateClientManager.serverHasMod()) {
            RenderTickCounter renderTickCounter = MinecraftClient.getInstance().getRenderTickCounter();
            if(renderTickCounter.tickRate$getMovingI() < TickRateClientManager.getChunkTickDelta(ChunkPos.toLong(particle.tickRate$getBlockPos())).i()) {
                particle.tick();
            }
        }
        else particle.tick();
    }

    @Redirect(method = "renderParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;buildGeometry(Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/Camera;F)V"))
    private void renderParticles(Particle particle, VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        particle.buildGeometry(vertexConsumer, camera, TickRateClientManager.getChunkTickDelta(ChunkPos.toLong(particle.tickRate$getBlockPos())).tickDelta());
    }

}
