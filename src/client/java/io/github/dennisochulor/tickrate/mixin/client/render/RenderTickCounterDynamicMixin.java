package io.github.dennisochulor.tickrate.mixin.client.render;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.dennisochulor.tickrate.TickDeltaInfo;
import io.github.dennisochulor.tickrate.TickRateClientManager;
import io.github.dennisochulor.tickrate.injected_interface.TickRateRenderTickCounter;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
    // all things (except client's player) ticking at a certain TPS will tick/animate the exact same client-side, so just map TPS->TickDeltaInfo
    @Unique private final Map<Integer, TickDeltaInfo> prevTickDeltas = new HashMap<>();
    @Unique private final Set<Integer> isUpdated = new HashSet<>();
    // the client's player can go above 20TPS, so it needs special treatment
    @Unique private TickDeltaInfo clientPlayerTickDeltaInfo = TickDeltaInfo.NO_ANIMATE;
    @Unique private boolean clientPlayerUpdated = false;

    @Inject(method = "beginRenderTick(J)I", at = @At("HEAD"))
    private void beginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        prevPrevTickMillis = prevTimeMillis;
        prevTickDelta = tickDelta;
        isUpdated.clear();
        clientPlayerUpdated = false;
    }

    @Inject(method = "beginRenderTick(J)I", at = @At("TAIL"))
    private void beginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir, @Local int i) {
        this.i = i;
        TickRateClientManager.clearCache();
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

    @Unique
    public TickDeltaInfo tickRate$getClientPlayerTickDeltaInfo(int tps) {
        // the client's player can go above 20TPS, so it needs special treatment
        float millisPerTick = 1000.0f / tps;
        if(clientPlayerUpdated) return clientPlayerTickDeltaInfo;
        float lastFrameDuration = (float)(prevTimeMillis - prevPrevTickMillis) / millisPerTick;
        float specificTickDelta = clientPlayerTickDeltaInfo.tickDelta() + lastFrameDuration;
        int i = (int) specificTickDelta;
        specificTickDelta -= (float) i;
        clientPlayerTickDeltaInfo = new TickDeltaInfo(specificTickDelta,i,lastFrameDuration);
        clientPlayerUpdated = true;
        return clientPlayerTickDeltaInfo;
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
