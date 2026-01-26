package io.github.dennisochulor.tickrate.client.mixin.misc;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import io.github.dennisochulor.tickrate.client.TickRateClientManager;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityHitboxDebugRenderer.class)
public abstract class EntityHitboxDebugRendererMixin {
    @Inject(method = "emitGizmos", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/debug/EntityHitboxDebugRenderer;showHitboxes(Lnet/minecraft/world/entity/Entity;FZ)V",
            ordinal = 0))
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks, CallbackInfo ci,
                           @Local(argsOnly = true, name = "partialTicks") LocalFloatRef partialTicksRef, @Local(name = "entity") Entity entity) {
        partialTicksRef.set(TickRateClientManager.getEntityDeltaTrackerInfo(entity).partialTick());
    }
}
