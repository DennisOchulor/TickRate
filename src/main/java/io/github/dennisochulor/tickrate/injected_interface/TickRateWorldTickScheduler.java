package io.github.dennisochulor.tickrate.injected_interface;

import net.minecraft.server.world.ServerWorld;

public interface TickRateWorldTickScheduler {

    default void tickRate$setWorld(ServerWorld world) {}

}
