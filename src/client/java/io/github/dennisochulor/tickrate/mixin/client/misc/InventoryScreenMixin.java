package io.github.dennisochulor.tickrate.mixin.client.misc;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @Redirect(method = "drawEntity(Lnet/minecraft/client/gui/DrawContext;FFFLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/entity/LivingEntity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;draw(Ljava/util/function/Consumer;)V"))
    private static void drawEntity(DrawContext context, Consumer<VertexConsumerProvider> drawer, @Local EntityRenderDispatcher entityRenderDispatcher, @Local(argsOnly = true) LivingEntity entity) {
        context.draw(vertexConsumers -> entityRenderDispatcher.render(entityRenderDispatcher.getRenderer(entity).getAndUpdateRenderState(entity, 1.0F), 0.0, 0.0, 0.0, context.getMatrices(), vertexConsumers, 15728880));
    }

}
