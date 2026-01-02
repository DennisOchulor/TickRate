package io.github.dennisochulor.tickrate.injected_interface;

public interface TickRateServerGamePacketListenerImpl {

    default boolean tickRate$hasClientMod() { throw new UnsupportedOperationException("Implemented via mixin!"); }

}
