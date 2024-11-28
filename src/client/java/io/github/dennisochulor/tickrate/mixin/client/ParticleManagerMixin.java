package io.github.dennisochulor.tickrate.mixin.client;

import io.github.dennisochulor.tickrate.TickRateClientManager;
import io.github.dennisochulor.tickrate.TickRateRenderTickCounter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.EmitterParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
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
            Vec3d vec = particle.getBoundingBox().getMinPos();
            TickRateRenderTickCounter renderTickCounter = (TickRateRenderTickCounter) MinecraftClient.getInstance().getRenderTickCounter();
            if(renderTickCounter.tickRate$getMovingI() < TickRateClientManager.getChunkTickDelta(world, ChunkPos.toLong(new BlockPos((int)vec.x,(int)vec.y,(int)vec.z))).i()) {
                particle.tick();
            }
        }
        else particle.tick();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/EmitterParticle;tick()V"))
    public void tick$emitters(EmitterParticle particle) {
        if(TickRateClientManager.serverHasMod()) {
            Vec3d vec = particle.getBoundingBox().getMinPos();
            TickRateRenderTickCounter renderTickCounter = (TickRateRenderTickCounter) MinecraftClient.getInstance().getRenderTickCounter();
            if(renderTickCounter.tickRate$getMovingI() < TickRateClientManager.getChunkTickDelta(world, ChunkPos.toLong(new BlockPos((int)vec.x,(int)vec.y,(int)vec.z))).i()) {
                particle.tick();
            }
        }
        else particle.tick();
    }

    @Redirect(method = "renderParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;buildGeometry(Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/Camera;F)V"))
    public void renderParticles(Particle particle, VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        Vec3d vec = particle.getBoundingBox().getMinPos();
        particle.buildGeometry(vertexConsumer, camera, TickRateClientManager.getChunkTickDelta(world, ChunkPos.toLong(new BlockPos((int)vec.x,(int)vec.y,(int)vec.z))).tickDelta());
    }

}
