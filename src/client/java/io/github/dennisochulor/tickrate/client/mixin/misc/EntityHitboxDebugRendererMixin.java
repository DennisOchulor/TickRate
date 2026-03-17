package io.github.dennisochulor.tickrate.client.mixin.misc;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.dennisochulor.tickrate.client.TickRateClientManager;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityHitboxDebugRenderer.class)
public abstract class EntityHitboxDebugRendererMixin {
    @ModifyExpressionValue(method = "emitGizmos", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/DeltaTracker;getGameTimeDeltaPartialTick(Z)F"))
    public float emitGizmos$entityPartialTick(float original, @Local(name = "entity") Entity entity) {
        return TickRateClientManager.getEntityDeltaTrackerInfo(entity).partialTick();
    }
}
