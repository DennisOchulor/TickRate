package io.github.dennisochulor.tickrate.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci, @Local(argsOnly = true) LocalFloatRef tickDeltaRef) {
        tickDeltaRef.set(TickRateClientManager.getEntityTickDelta(entity).tickDelta()); // tickDelta
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    public RenderTickCounter render$renderTickCounter(RenderTickCounter renderTickCounter) {
        return MinecraftClient.getInstance().getRenderTickCounter(); //replace player's RTC with server's RTC
    }

}
