package io.github.dennisochulor.tickrate.injected_interface;

import net.minecraft.world.tick.OrderedTick;

import java.util.List;

public interface TickRateChunkTickScheduler<T> {

    default void tickRate$setServerTime(long time) {}
    default List<OrderedTick<T>> tickRate$tick() {return null;}
    default void tickRate$toggleMode(boolean followServerTick) {}

}
