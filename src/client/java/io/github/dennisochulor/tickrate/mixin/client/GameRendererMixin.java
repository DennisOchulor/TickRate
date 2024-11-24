package io.github.dennisochulor.tickrate.mixin.client;

import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Redirect(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderTickCounter;getTickDelta(Z)F"))
    public float renderWorld(RenderTickCounter instance, boolean b) {
        if(!TickRateClientManager.serverHasMod()) return instance.getTickDelta(b);
        return TickRateClientManager.getEntityTickDelta(instance.getTickDelta(b), MinecraftClient.getInstance().player).tickDelta();
    }

}
