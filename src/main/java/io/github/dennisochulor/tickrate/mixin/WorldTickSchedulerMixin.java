package io.github.dennisochulor.tickrate.mixin;

import net.minecraft.world.tick.WorldTickScheduler;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WorldTickScheduler.class)
public class WorldTickSchedulerMixin {

    /*
    Modify ChunkTickScheduler to internally track the ticks (NOT THE BLUMMIN TIME)
    Though keep the tickQueue and queuedTicks going either way for ease of transition
    switch between time mode and tick mode
     */

}
