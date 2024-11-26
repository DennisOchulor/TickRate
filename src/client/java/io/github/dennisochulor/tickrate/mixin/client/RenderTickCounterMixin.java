package io.github.dennisochulor.tickrate.mixin.client;

import io.github.dennisochulor.tickrate.TickDeltaInfo;
import io.github.dennisochulor.tickrate.TickRateRenderTickCounter;
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
public class RenderTickCounterMixin implements TickRateRenderTickCounter {

    @Shadow private long prevTimeMillis;
    @Shadow private float tickDelta;
    @Shadow @Final private float tickTime;

    @Unique private long prevPrevTickMillis;
    @Unique private float prevTickDelta;
    @Unique private int movingI;
    @Unique private int i;
    @Unique private final Map<String, TickDeltaInfo> prevTickDeltas = new HashMap<>();
    @Unique private final Set<String> isUpdated = new HashSet<>();

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
    public TickDeltaInfo tickRate$getSpecificTickDelta(float millisPerTick, String key) {
        if(isUpdated.contains(key)) return prevTickDeltas.get(key);
        float lastFrameDuration = (float)(prevTimeMillis - prevPrevTickMillis) / Math.max(millisPerTick, tickTime);
        float specificTickDelta = prevTickDeltas.getOrDefault(key,new TickDeltaInfo(prevTickDelta,0,0)).tickDelta() + lastFrameDuration;
        int i = (int) specificTickDelta;
        specificTickDelta -= (float) i;
        TickDeltaInfo info = new TickDeltaInfo(specificTickDelta,i,lastFrameDuration);
        isUpdated.add(key);
        prevTickDeltas.put(key,info);
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
