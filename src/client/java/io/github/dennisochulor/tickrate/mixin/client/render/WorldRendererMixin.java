package io.github.dennisochulor.tickrate.mixin.client.render;

import io.github.dennisochulor.tickrate.PlayerRenderTickCounter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Unique private static final PlayerRenderTickCounter playerRenderTickCounter = new PlayerRenderTickCounter();
    @Unique private static final boolean isIrisLoaded = FabricLoader.getInstance().isModLoaded("iris");

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    public RenderTickCounter render$renderTickCounter(RenderTickCounter renderTickCounter) {
        return MinecraftClient.getInstance().getRenderTickCounter(); //replace player's RTC with server's RTC
    }

    // NOTE: Since 1.21.9, (Block) Entity tickProgress is modified in (Block)EntityRenderManagerMixin respectively
    // Mainly to maintain compat with Sodium that overrides fillBlockEntityRenderStates() bleh...

    /*
     IRIS COMPATIBILITY - fixes player hand/held item stutter issue
     Iris uses the server's RTC to render player hand, so give Iris the player's RTC instead
     See https://github.com/IrisShaders/Iris/blob/1.21.9/common/src/main/java/net/irisshaders/iris/mixin/MixinLevelRenderer.java
     */

    @ModifyVariable(method = "render", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4fStack;popMatrix()Lorg/joml/Matrix4fStack;"), order = 100, argsOnly = true, remap = false)
    public RenderTickCounter renderEndSwapIn$iris(RenderTickCounter renderTickCounter) {
        if(isIrisLoaded) return playerRenderTickCounter;
        else return renderTickCounter;
    }

    @ModifyVariable(method = "render", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4fStack;popMatrix()Lorg/joml/Matrix4fStack;"), order = 1100, argsOnly = true, remap = false)
    public RenderTickCounter renderEndSwapBack$iris(RenderTickCounter renderTickCounter) {
        return MinecraftClient.getInstance().getRenderTickCounter();
    }


    // Big problem, here Iris directly gets RTC from MinecraftClient
//    @ModifyVariable(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw()V", ordinal = 1), order = 100, argsOnly = true)
//    public RenderTickCounter renderMainSwapIn$iris(RenderTickCounter renderTickCounter) {
//        if(isIrisLoaded) return playerRenderTickCounter;
//        else return renderTickCounter;
//    }
//
//    @ModifyVariable(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw()V", ordinal = 1), order = 1100, argsOnly = true)
//    public RenderTickCounter renderMainSwapBack$iris(RenderTickCounter renderTickCounter) {
//        return MinecraftClient.getInstance().getRenderTickCounter();
//    }

}
