package io.github.dennisochulor.tickrate.mixin.client;

import io.github.dennisochulor.tickrate.TickRate;
import io.github.dennisochulor.tickrate.TickRateRenderTickCounter;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTickCounter.Dynamic.class)
public class RenderTickCounterMixin implements TickRateRenderTickCounter {

    @Shadow private long prevTimeMillis;
    @Shadow private float tickDelta;
    @Shadow @Final private float tickTime;
    @Shadow @Final private FloatUnaryOperator targetMillisPerTick;
    @Shadow private float lastFrameDuration;

    @Unique private long prevPrevTickMillis;
    @Unique private float prevTickDelta;

    @Inject(method = "beginRenderTick(J)I", at = @At("HEAD"))
    private void beginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        prevPrevTickMillis = prevTimeMillis;
        prevTickDelta = tickDelta;
    }

    @Unique
    public float tickRate$getSpecificTickDelta(float millisPerTick) {
        //float lastFrameDuration = (float)(prevTimeMillis - prevPrevTickMillis) / getTargetMillisPerTick(millisPerTick,tickTime);
        float specificTickDelta = prevTickDelta + lastFrameDuration;
        int i = (int) specificTickDelta;
        specificTickDelta -= (float) i;
        return specificTickDelta * millisPerTick / targetMillisPerTick.apply(tickTime);
    }

    @Unique
    private float getTargetMillisPerTick(float target, float millis) {
        return Math.max(millis, target);
    }

}
