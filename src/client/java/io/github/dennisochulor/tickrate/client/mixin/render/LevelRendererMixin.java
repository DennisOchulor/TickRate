package io.github.dennisochulor.tickrate.client.mixin.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @ModifyVariable(method = "renderLevel", at = @At("HEAD"), argsOnly = true, name = "deltaTracker")
    public DeltaTracker renderLevel(DeltaTracker deltaTracker) {
        // replace player's DeltaTracker with world's DeltaTracker
        // technically it is already the world's, but keep this for futureproofing
        return Minecraft.getInstance().getDeltaTracker();
    }

    @ModifyVariable(method = "extractLevel", at = @At("HEAD"), argsOnly = true, name = "deltaTracker")
    public DeltaTracker extractLevel(DeltaTracker deltaTracker) {
        // replace player's DeltaTracker with world's DeltaTracker
        // technically it is already the world's, but keep this for futureproofing
        return Minecraft.getInstance().getDeltaTracker();
    }


}
