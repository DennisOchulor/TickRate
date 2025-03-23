package io.github.dennisochulor.tickrate.injected_interface;

import io.github.dennisochulor.tickrate.TickProgressInfo;

public interface TickRateRenderTickCounter {

    default TickProgressInfo tickRate$getSpecificTickProgressInfo(int tps) {return null;}
    default TickProgressInfo tickRate$getClientPlayerTickProgressInfo(int tps) { return null;}
    default void tickRate$setMovingI(int newI) {}
    default int tickRate$getMovingI() {return 0;}
    default int tickRate$getI() {return 0;}

}
