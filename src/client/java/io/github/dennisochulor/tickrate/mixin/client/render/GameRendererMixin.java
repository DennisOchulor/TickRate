package io.github.dennisochulor.tickrate.mixin.client.render;

import io.github.dennisochulor.tickrate.PlayerRenderTickCounter;
import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Unique private static final PlayerRenderTickCounter playerRenderTickCounter = new PlayerRenderTickCounter();

    @ModifyVariable(method = "render", at = @At(value = "HEAD"), argsOnly = true, ordinal = 0)
    public RenderTickCounter render(RenderTickCounter renderTickCounter) {
        if(TickRateClientManager.serverHasMod()) return playerRenderTickCounter;
        else return renderTickCounter;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;addWeatherParticlesAndSound(Lnet/minecraft/client/render/Camera;)V"))
    public void tick(WorldRenderer instance, Camera camera) {
        if(!TickRateClientManager.serverHasMod()) instance.addWeatherParticlesAndSound(camera);
        // otherwise NO-OP
    }

}
