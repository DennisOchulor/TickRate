package io.github.dennisochulor.tickrate.injected_interface;

import net.minecraft.world.entity.Entity;

public interface TickRateServerEntity {
    default Entity tickRate$getEntity() { throw new UnsupportedOperationException("Implemented via mixin!"); }
}
