package io.github.dennisochulor.tickrate.client.mixin.render;

import io.github.dennisochulor.tickrate.client.PlayerDeltaTracker;
import io.github.dennisochulor.tickrate.client.TickRateClientManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Unique private static final PlayerDeltaTracker playerDeltaTracker = new PlayerDeltaTracker();

    @ModifyVariable(method = "render", at = @At(value = "HEAD"), argsOnly = true)
    private DeltaTracker render(DeltaTracker deltaTracker) {
        if(TickRateClientManager.serverHasMod()) return playerDeltaTracker;
        else return deltaTracker;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;tick(Lnet/minecraft/client/Camera;)V"))
    private void tick(LevelRenderer instance, Camera camera) { // levelRenderer follows server tick
        if(!TickRateClientManager.serverHasMod()) instance.tick(camera);
        // otherwise NO-OP
    }

}
