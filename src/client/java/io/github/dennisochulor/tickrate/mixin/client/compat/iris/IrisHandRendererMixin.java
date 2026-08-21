package io.github.dennisochulor.tickrate.mixin.client.compat.iris;

import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.irisshaders.iris.pathways.HandRenderer;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Objects;

/**
 IRIS COMPATIBILITY - fixes player hand/held item stutter issue
 Iris uses the server's partialTick to render player hand, so give Iris the player's partialTick instead
 See <a href="https://github.com/IrisShaders/Iris/blob/26.2/common/src/main/java/net/irisshaders/iris/mixin/MixinLevelRenderer.java">here</a>
 */
@Mixin(HandRenderer.class)
public abstract class IrisHandRendererMixin {

    @ModifyVariable(method = "renderSolid", at = @At("HEAD"), argsOnly = true, remap = false)
    private float renderSolid(float partialTick) {
        return TickRateClientManager.getEntityTickDelta(Objects.requireNonNull(MinecraftClient.getInstance().player)).tickDelta();
    }

    @ModifyVariable(method = "renderTranslucent", at = @At("HEAD"), argsOnly = true, remap = false)
    private float renderTranslucent(float partialTick) {
        return TickRateClientManager.getEntityTickDelta(Objects.requireNonNull(MinecraftClient.getInstance().player)).tickDelta();
    }

}
