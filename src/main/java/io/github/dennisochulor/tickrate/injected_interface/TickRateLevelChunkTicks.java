package io.github.dennisochulor.tickrate.injected_interface;

import java.util.List;
import net.minecraft.world.ticks.ScheduledTick;

public interface TickRateLevelChunkTicks<T> {

    default void tickRate$setServerTime(long time) {}
    default List<ScheduledTick<T>> tickRate$tick() {return null;}
    default void tickRate$toggleMode(boolean followServerTick) {}

}
