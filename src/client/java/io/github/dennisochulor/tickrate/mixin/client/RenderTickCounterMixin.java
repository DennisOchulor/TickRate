package io.github.dennisochulor.tickrate.mixin.client;

import io.github.dennisochulor.tickrate.TickDeltaInfo;
import io.github.dennisochulor.tickrate.injected_interface.TickRateRenderTickCounter;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RenderTickCounter.class)
public interface RenderTickCounterMixin extends TickRateRenderTickCounter {

    @Unique
    default TickDeltaInfo tickRate$getSpecificTickDeltaInfo(int tps) {return null;}
    @Unique
    default void tickRate$setMovingI(int newI) {}
    @Unique
    default int tickRate$getMovingI() {return 0;}
    @Unique
    default int tickRate$getI() {return 0;}

}
