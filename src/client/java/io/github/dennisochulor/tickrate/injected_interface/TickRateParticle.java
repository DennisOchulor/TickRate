package io.github.dennisochulor.tickrate.injected_interface;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface TickRateParticle {

    default BlockPos tickRate$getBlockPos() {return null;}
    default Level tickRate$getLevel() {return null;}

}
