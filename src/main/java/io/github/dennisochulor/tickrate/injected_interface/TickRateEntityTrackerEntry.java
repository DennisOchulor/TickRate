package io.github.dennisochulor.tickrate.injected_interface;

import net.minecraft.entity.Entity;

public interface TickRateEntityTrackerEntry {
    default Entity tickRate$getEntity() { return null; }
}
