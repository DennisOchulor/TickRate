package io.github.dennisochulor.tickrate.injected_interface;

import io.github.dennisochulor.tickrate.TickDeltaInfo;

public interface TickRateRenderTickCounter {

    default TickDeltaInfo tickRate$getSpecificTickDelta(float millisPerTick, String key) {return null;}
    default void tickRate$setMovingI(int newI) {}
    default int tickRate$getMovingI() {return 0;}
    default int tickRate$getI() {return 0;}

}