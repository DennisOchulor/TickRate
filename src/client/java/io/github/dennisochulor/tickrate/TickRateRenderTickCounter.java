package io.github.dennisochulor.tickrate;

public interface TickRateRenderTickCounter {

    TickDeltaInfo tickRate$getSpecificTickDelta(float millisPerTick, String key);
    void tickRate$setMovingI(int newI);
    int tickRate$getMovingI();
    int tickRate$getI();

}
