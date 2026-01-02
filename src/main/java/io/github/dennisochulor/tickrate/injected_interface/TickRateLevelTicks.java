package io.github.dennisochulor.tickrate.injected_interface;

import net.minecraft.server.level.ServerLevel;

public interface TickRateLevelTicks {

    default void tickRate$setLevel(ServerLevel level) { throw new UnsupportedOperationException("Implemented via mixin!"); }

}
