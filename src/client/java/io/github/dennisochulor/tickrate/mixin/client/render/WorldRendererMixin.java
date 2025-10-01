package io.github.dennisochulor.tickrate.mixin.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    public RenderTickCounter render$renderTickCounter(RenderTickCounter renderTickCounter) {
        return MinecraftClient.getInstance().getRenderTickCounter(); //replace player's RTC with server's RTC
    }

    // NOTE: Since 1.21.9, (Block) Entity tickProgress is modified in (Block)EntityRenderManagerMixin respectively
    // Mainly to maintain compat with Sodium that overrides fillBlockEntityRenderStates() bleh...

}
