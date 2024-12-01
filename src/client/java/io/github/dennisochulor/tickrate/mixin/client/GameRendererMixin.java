package io.github.dennisochulor.tickrate.mixin.client;

import io.github.dennisochulor.tickrate.PlayerRenderTickCounter;
import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Unique private static final PlayerRenderTickCounter playerRenderTickCounter = new PlayerRenderTickCounter();

    @ModifyVariable(method = "render", at = @At(value = "HEAD"), argsOnly = true, ordinal = 0)
    public RenderTickCounter render(RenderTickCounter renderTickCounter) {
        if(TickRateClientManager.serverHasMod()) return playerRenderTickCounter;
        else return renderTickCounter;
    }

}
