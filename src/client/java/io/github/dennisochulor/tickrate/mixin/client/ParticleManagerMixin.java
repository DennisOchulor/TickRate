package io.github.dennisochulor.tickrate.mixin.client;

import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.EmitterParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    @Shadow protected ClientWorld world;

    @Redirect(method = "tickParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;tick()V"))
    private void tickParticle(Particle particle) {
        if(TickRateClientManager.serverHasMod()) {
            RenderTickCounter renderTickCounter = MinecraftClient.getInstance().getRenderTickCounter();
            if(renderTickCounter.tickRate$getMovingI() < TickRateClientManager.getChunkTickDelta(world, ChunkPos.toLong(particle.tickRate$getBlockPos())).i()) {
                particle.tick();
            }
        }
        else particle.tick();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/EmitterParticle;tick()V"))
    public void tick$emitters(EmitterParticle particle) {
        if(TickRateClientManager.serverHasMod()) {
            RenderTickCounter renderTickCounter = MinecraftClient.getInstance().getRenderTickCounter();
            if(renderTickCounter.tickRate$getMovingI() < TickRateClientManager.getChunkTickDelta(world, ChunkPos.toLong(particle.tickRate$getBlockPos())).i()) {
                particle.tick();
            }
        }
        else particle.tick();
    }

    @Redirect(method = "renderParticles(Lnet/minecraft/client/render/Camera;FLnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/particle/ParticleTextureSheet;Ljava/util/Queue;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;render(Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/Camera;F)V"))
    private static void renderParticles(Particle particle, VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        particle.render(vertexConsumer, camera, TickRateClientManager.getChunkTickDelta(particle.tickRate$getWorld(), ChunkPos.toLong(particle.tickRate$getBlockPos())).tickDelta());
    }

    @Redirect(method = "renderCustomParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;renderCustom(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/render/Camera;F)V"))
    private static void renderParticles(Particle particle, MatrixStack matrices, VertexConsumerProvider vertexConsumers, Camera camera, float tickDelta) {
        particle.renderCustom(matrices, vertexConsumers, camera, TickRateClientManager.getChunkTickDelta(particle.tickRate$getWorld(), ChunkPos.toLong(particle.tickRate$getBlockPos())).tickDelta());
    }

}
