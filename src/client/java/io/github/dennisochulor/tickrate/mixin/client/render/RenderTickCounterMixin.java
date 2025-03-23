package io.github.dennisochulor.tickrate.mixin.client.render;

import io.github.dennisochulor.tickrate.TickProgressInfo;
import io.github.dennisochulor.tickrate.injected_interface.TickRateRenderTickCounter;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RenderTickCounter.class)
public interface RenderTickCounterMixin extends TickRateRenderTickCounter {

    @Unique
    default TickProgressInfo tickRate$getSpecificTickProgressInfo(int tps) {return null;}
    @Unique
    default TickProgressInfo tickRate$getClientPlayerTickProgressInfo(int tps) {return null;}
    @Unique
    default void tickRate$setMovingI(int newI) {}
    @Unique
    default int tickRate$getMovingI() {return 0;}
    @Unique
    default int tickRate$getI() {return 0;}

}
