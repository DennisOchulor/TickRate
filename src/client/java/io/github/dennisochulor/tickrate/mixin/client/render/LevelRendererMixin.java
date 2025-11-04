package io.github.dennisochulor.tickrate.mixin.client.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @ModifyVariable(method = "renderLevel", at = @At("HEAD"), argsOnly = true)
    public DeltaTracker renderLevel(DeltaTracker deltaTracker) {
        return Minecraft.getInstance().getDeltaTracker(); //replace player's DeltaTracker with server's DeltaTracker
    }

    // NOTE: Since 1.21.9, (Block) Entity partialTick is modified in (Block)EntityRenderDispatcherMixin respectively
    // Mainly to maintain compat with Sodium that overrides extractVisibleBlockEntities() bleh...

}
