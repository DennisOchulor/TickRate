package io.github.dennisochulor.tickrate.mixin.client.render;

import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityRenderManager.class)
public abstract class EntityRenderManagerMixin {

    // modify ENTITY tick progress
    @ModifyVariable(method = "getAndUpdateRenderState", at = @At("HEAD"), argsOnly = true)
    private float modifyTickProgress$getAndUpdateRenderState(float tickProgress, Entity entity) {
        return TickRateClientManager.getEntityTickProgress(entity).tickProgress();
    }

}
