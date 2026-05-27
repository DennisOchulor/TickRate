package io.github.dennisochulor.tickrate.client.mixin.render;

import io.github.dennisochulor.tickrate.client.PlayerDeltaTracker;
import io.github.dennisochulor.tickrate.client.TickRateClientManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Camera.class)
abstract class CameraMixin {

    @Unique private static final PlayerDeltaTracker playerDeltaTracker = new PlayerDeltaTracker();

    @ModifyVariable(method = "getCameraEntityPartialTicks", at = @At("HEAD"), argsOnly = true)
    private DeltaTracker getCameraEntityPartialTicks(DeltaTracker deltaTracker) {
        return playerDeltaTracker;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/attribute/EnvironmentAttributeProbe;tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;)V"))
    private void tickEnvAttrs(EnvironmentAttributeProbe instance, Level level, Vec3 position) {
        // Make env attrs follow world's ticking
        if (!TickRateClientManager.serverHasMod()) instance.tick(level, position);
        // otherwise NO-OP
    }

}
