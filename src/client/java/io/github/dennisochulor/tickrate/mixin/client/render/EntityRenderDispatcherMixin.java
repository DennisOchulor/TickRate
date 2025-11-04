package io.github.dennisochulor.tickrate.mixin.client.render;

import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    // modify ENTITY partial tick
    @ModifyVariable(method = "extractEntity", at = @At("HEAD"), argsOnly = true)
    private float extractEntity(float partialTick, Entity entity) {
        return TickRateClientManager.getEntityDeltaTrackerInfo(entity).partialTick();
    }

}
