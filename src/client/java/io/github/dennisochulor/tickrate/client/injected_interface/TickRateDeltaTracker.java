package io.github.dennisochulor.tickrate.client.injected_interface;

import io.github.dennisochulor.tickrate.client.DeltaTrackerInfo;

public interface TickRateDeltaTracker {

    default DeltaTrackerInfo tickRate$getDeltaTrackerInfo(int tps) { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default DeltaTrackerInfo tickRate$getClientPlayerDeltaTrackerInfo(int tps) { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default void tickRate$setMovingTicksToDo(int newI) { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default int tickRate$getMovingTicksToDo() { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default int tickRate$getTicksToDo() { throw new UnsupportedOperationException("Implemented via mixin!"); }

}
