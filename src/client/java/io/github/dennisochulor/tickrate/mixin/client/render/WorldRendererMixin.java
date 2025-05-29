package io.github.dennisochulor.tickrate.mixin.client.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import io.github.dennisochulor.tickrate.PlayerRenderTickCounter;
import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Unique private static final PlayerRenderTickCounter playerRenderTickCounter = new PlayerRenderTickCounter();
    @Unique private static final boolean isIrisLoaded = FabricLoader.getInstance().isModLoaded("iris");

    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci, @Local(argsOnly = true) LocalFloatRef tickDeltaRef) {
        tickDeltaRef.set(TickRateClientManager.getEntityTickProgress(entity).tickProgress()); // tickProgress
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    public RenderTickCounter render$renderTickCounter(RenderTickCounter renderTickCounter) {
        return MinecraftClient.getInstance().getRenderTickCounter(); //replace player's RTC with server's RTC
    }



    /*
     IRIS COMPATIBILITY - fixes player hand/held item stutter issue
     Iris uses the server's RTC to render player hand, so give Iris the player's RTC instead
     See https://github.com/IrisShaders/Iris/blob/1.21.6/common/src/main/java/net/irisshaders/iris/mixin/MixinLevelRenderer.java
     */

    @ModifyVariable(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V"), order = 100, argsOnly = true)
    public RenderTickCounter renderEndSwapIn$iris(RenderTickCounter renderTickCounter) {
        if(isIrisLoaded) return playerRenderTickCounter;
        else return renderTickCounter;
    }

    @ModifyVariable(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V"), order = 1100, argsOnly = true)
    public RenderTickCounter renderEndSwapBack$iris(RenderTickCounter renderTickCounter) {
        return MinecraftClient.getInstance().getRenderTickCounter();
    }


    @ModifyVariable(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw()V", ordinal = 1), order = 100, argsOnly = true)
    public RenderTickCounter renderMainSwapIn$iris(RenderTickCounter renderTickCounter) {
        if(isIrisLoaded) return playerRenderTickCounter;
        else return renderTickCounter;
    }

    @ModifyVariable(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw()V", ordinal = 1), order = 1100, argsOnly = true)
    public RenderTickCounter renderMainSwapBack$iris(RenderTickCounter renderTickCounter) {
        return MinecraftClient.getInstance().getRenderTickCounter();
    }

}
