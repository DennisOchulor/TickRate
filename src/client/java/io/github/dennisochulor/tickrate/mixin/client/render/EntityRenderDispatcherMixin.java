package io.github.dennisochulor.tickrate.mixin.client.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(method = "render", at = @At("HEAD"))
    public void render(Entity entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci, @Local(argsOnly = true, ordinal = 1) LocalFloatRef tickDeltaRef) {
        if(StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(stream -> stream.skip(2).findFirst().get()).getDeclaringClass() == InventoryScreen.class) {
            return; // this is a TERRIBLE way of coding, but I don't know how else to fix the entity rendering for the InventoryScreen
        }
        tickDeltaRef.set(TickRateClientManager.getEntityTickDelta(entity).tickDelta());
    }

}
