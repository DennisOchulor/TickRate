package io.github.dennisochulor.tickrate.injected_interface;

import java.util.List;
import net.minecraft.world.ticks.ScheduledTick;

public interface TickRateLevelChunkTicks<T> {

    default void tickRate$setServerTime(long time) { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default List<ScheduledTick<T>> tickRate$tick() { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default void tickRate$toggleMode(boolean followServerTick) { throw new UnsupportedOperationException("Implemented via mixin!"); }

}
