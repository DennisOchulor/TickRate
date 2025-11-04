package io.github.dennisochulor.tickrate.mixin.client.render;

import io.github.dennisochulor.tickrate.DeltaTrackerInfo;
import io.github.dennisochulor.tickrate.injected_interface.TickRateDeltaTracker;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DeltaTracker.class)
public interface DeltaTrackerMixin extends TickRateDeltaTracker {

    @Unique
    default DeltaTrackerInfo tickRate$getDeltaTrackerInfo(int tps) {return null;}
    @Unique
    default DeltaTrackerInfo tickRate$getClientPlayerDeltaTrackerInfo(int tps) {return null;}
    @Unique
    default void tickRate$setMovingI(int newI) {}
    @Unique
    default int tickRate$getMovingI() {return 0;}
    @Unique
    default int tickRate$getI() {return 0;}

}
