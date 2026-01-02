package io.github.dennisochulor.tickrate.injected_interface;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface TickRateParticle {

    default BlockPos tickRate$getBlockPos() { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default Level tickRate$getLevel() { throw new UnsupportedOperationException("Implemented via mixin!"); }

}
