package io.github.dennisochulor.tickrate.injected_interface;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface TickRateParticle {

    default BlockPos tickRate$getBlockPos() {return null;}
    default World tickRate$getWorld() {return null;}

}
