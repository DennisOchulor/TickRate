package io.github.dennisochulor.tickrate.mixin.client.compat;

import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.irisshaders.iris.pathways.HandRenderer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 IRIS COMPATIBILITY - fixes player hand/held item stutter issue
 Iris uses the server's partialTick to render player hand, so give Iris the player's partialTick instead
 See <a href="https://github.com/IrisShaders/Iris/blob/1.21.9/common/src/main/java/net/irisshaders/iris/mixin/MixinLevelRenderer.java">here</a>
 */
@Mixin(HandRenderer.class)
public abstract class IrisHandRendererMixin {

    @ModifyVariable(method = "renderSolid", at = @At("HEAD"), argsOnly = true, remap = false)
    private float renderSolid(float tickProgress) {
        return TickRateClientManager.getEntityDeltaTrackerInfo(Minecraft.getInstance().player).partialTick();
    }

    @ModifyVariable(method = "renderTranslucent", at = @At("HEAD"), argsOnly = true, remap = false)
    private float renderTranslucent(float tickProgress) {
        return TickRateClientManager.getEntityDeltaTrackerInfo(Minecraft.getInstance().player).partialTick();
    }

}
