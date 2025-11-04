package io.github.dennisochulor.tickrate.injected_interface;

import io.github.dennisochulor.tickrate.DeltaTrackerInfo;

public interface TickRateDeltaTracker {

    default DeltaTrackerInfo tickRate$getDeltaTrackerInfo(int tps) {return null;}
    default DeltaTrackerInfo tickRate$getClientPlayerDeltaTrackerInfo(int tps) { return null;}
    default void tickRate$setMovingI(int newI) {}
    default int tickRate$getMovingI() {return 0;}
    default int tickRate$getI() {return 0;}

}
