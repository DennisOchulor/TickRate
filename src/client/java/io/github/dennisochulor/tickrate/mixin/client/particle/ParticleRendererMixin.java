package io.github.dennisochulor.tickrate.mixin.client.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderer;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({ParticleRenderer.class})
public class ParticleRendererMixin {

    @WrapOperation(method = "tickParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;tick()V"))
    private void tickParticle(Particle particle, Operation<Void> original) { // particle ticking
        if(TickRateClientManager.serverHasMod()) {
            // Modded particle types can't be accounted for properly, so just give em playerChunkI
            ParticleTextureSheet sheet = particle.textureSheet();
            boolean isVanillaParticle = sheet == ParticleTextureSheet.SINGLE_QUADS || sheet == ParticleTextureSheet.NO_RENDER || sheet == ParticleTextureSheet.ELDER_GUARDIANS || sheet == ParticleTextureSheet.ITEM_PICKUP;
            int i = isVanillaParticle ? TickRateClientManager.getChunkTickProgress(new ChunkPos(particle.tickRate$getBlockPos())).i() : TickRateClientManager.getChunkTickProgress(MinecraftClient.getInstance().player.getChunkPos()).i();

            RenderTickCounter renderTickCounter = MinecraftClient.getInstance().getRenderTickCounter();
            if(renderTickCounter.tickRate$getMovingI() < i) {
                original.call(particle);
            }
        }
        else original.call(particle);
    }

}
