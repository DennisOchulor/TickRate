package io.github.dennisochulor.tickrate.client.mixin.render;

import io.github.dennisochulor.tickrate.client.DeltaTrackerInfo;
import io.github.dennisochulor.tickrate.client.injected_interface.TickRateDeltaTracker;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DeltaTracker.class)
public interface DeltaTrackerMixin extends TickRateDeltaTracker {

    @Unique
    default DeltaTrackerInfo tickRate$getDeltaTrackerInfo(int tps) { throw new UnsupportedOperationException("Implemented via mixin!"); }
    @Unique
    default DeltaTrackerInfo tickRate$getClientPlayerDeltaTrackerInfo(int tps) { throw new UnsupportedOperationException("Implemented via mixin!"); }
    @Unique
    default void tickRate$setMovingTicksToDo(int newI) { throw new UnsupportedOperationException("Implemented via mixin!"); }
    @Unique
    default int tickRate$getMovingTicksToDo() { throw new UnsupportedOperationException("Implemented via mixin!"); }
    @Unique
    default int tickRate$getTicksToDo() { throw new UnsupportedOperationException("Implemented via mixin!"); }

}
