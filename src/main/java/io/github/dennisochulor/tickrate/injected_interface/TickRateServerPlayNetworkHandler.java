package io.github.dennisochulor.tickrate.injected_interface;

public interface TickRateServerPlayNetworkHandler {

    default boolean tickRate$hasClientMod() { return true; }

}
