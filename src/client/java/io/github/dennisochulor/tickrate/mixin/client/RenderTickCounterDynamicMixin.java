package io.github.dennisochulor.tickrate.mixin.client;

import io.github.dennisochulor.tickrate.TickDeltaInfo;
import io.github.dennisochulor.tickrate.injected_interface.TickRateRenderTickCounter;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(RenderTickCounter.Dynamic.class)
public class RenderTickCounterDynamicMixin implements TickRateRenderTickCounter {

    @Shadow private long prevTimeMillis;
    @Shadow private float tickDelta;
    @Shadow @Final private float tickTime;

    @Unique private long prevPrevTickMillis;
    @Unique private float prevTickDelta;
    @Unique private int movingI;
    @Unique private int i;
    // all things ticking at a certain TPS will tick/animate the exact same client-side, so just map TPS->TickDeltaInfo
    @Unique private final Map<Integer, TickDeltaInfo> prevTickDeltas = new HashMap<>();
    @Unique private final Set<Integer> isUpdated = new HashSet<>();

    @Inject(method = "beginRenderTick(J)I", at = @At("HEAD"))
    private void beginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        prevPrevTickMillis = prevTimeMillis;
        prevTickDelta = tickDelta;
        isUpdated.clear();
    }

    @Inject(method = "beginRenderTick(J)I", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void beginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir, int i) {
        this.i = i;
    }

    @Unique
    public TickDeltaInfo tickRate$getSpecificTickDeltaInfo(int tps) {
        float millisPerTick = 1000.0f / tps;
        if(isUpdated.contains(tps)) return prevTickDeltas.get(tps);
        float lastFrameDuration = (float)(prevTimeMillis - prevPrevTickMillis) / Math.max(millisPerTick, tickTime);
        float specificTickDelta = prevTickDeltas.getOrDefault(tps,new TickDeltaInfo(prevTickDelta,0,0)).tickDelta() + lastFrameDuration;
        int i = (int) specificTickDelta;
        specificTickDelta -= (float) i;
        TickDeltaInfo info = new TickDeltaInfo(specificTickDelta,i,lastFrameDuration);
        isUpdated.add(tps);
        prevTickDeltas.put(tps,info);
        return info;
    }

    @Override
    public void tickRate$setMovingI(int newI) {
        movingI = newI;
    }

    @Override
    public int tickRate$getMovingI() {
        return movingI;
    }

    @Override
    public int tickRate$getI() {
        return i;
    }

}
