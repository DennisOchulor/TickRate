package io.github.dennisochulor.tickrate.mixin.client.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.dennisochulor.tickrate.PlayerRenderTickCounter;
import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Unique private static final PlayerRenderTickCounter playerRenderTickCounter = new PlayerRenderTickCounter();
    @Unique private static final boolean isIrisLoaded = FabricLoader.getInstance().isModLoaded("iris");

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    public RenderTickCounter render$renderTickCounter(RenderTickCounter renderTickCounter) {
        return MinecraftClient.getInstance().getRenderTickCounter(); //replace player's RTC with server's RTC
    }

    @ModifyExpressionValue(method = "fillEntityRenderStates", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderTickCounter;getTickProgress(Z)F"))
    private float fillEntityRenderStates(float original, @Local Entity entity) { // modify entity tickProgress
        return TickRateClientManager.getEntityTickProgress(entity).tickProgress();
    }

    @ModifyArg(method = "fillBlockEntityRenderStates", index = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/entity/BlockEntityRenderManager;getRenderState(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)Lnet/minecraft/client/render/block/entity/state/BlockEntityRenderState;"))
    private float fillBlockEntityRenderStates(float tickProgress, @Local BlockEntity blockEntity) { // modify block entity tickProgess
        return TickRateClientManager.getChunkTickProgress(new ChunkPos(blockEntity.getPos())).tickProgress();
    }



    /*
     IRIS COMPATIBILITY - fixes player hand/held item stutter issue
     Iris uses the server's RTC to render player hand, so give Iris the player's RTC instead
     See https://github.com/IrisShaders/Iris/blob/1.21.9/common/src/main/java/net/irisshaders/iris/mixin/MixinLevelRenderer.java
     */

    @ModifyVariable(method = "render", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4fStack;popMatrix()Lorg/joml/Matrix4fStack;"), order = 100, argsOnly = true)
    public RenderTickCounter renderEndSwapIn$iris(RenderTickCounter renderTickCounter) {
        if(isIrisLoaded) return playerRenderTickCounter;
        else return renderTickCounter;
    }

    @ModifyVariable(method = "render", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4fStack;popMatrix()Lorg/joml/Matrix4fStack;"), order = 1100, argsOnly = true)
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
