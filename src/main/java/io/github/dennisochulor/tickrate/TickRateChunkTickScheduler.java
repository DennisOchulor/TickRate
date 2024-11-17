package io.github.dennisochulor.tickrate;

import net.minecraft.world.tick.OrderedTick;

import java.util.List;

public interface TickRateChunkTickScheduler<T> {

    void tickRate$setServerTime(long time);
    List<OrderedTick<T>> tickRate$tick();
    void tickRate$toggleMode(boolean followServerTick);

}
