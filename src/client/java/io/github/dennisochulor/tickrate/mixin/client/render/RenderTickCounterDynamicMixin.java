package io.github.dennisochulor.tickrate.mixin.client.render;

import io.github.dennisochulor.tickrate.TickProgressInfo;
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
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(RenderTickCounter.Dynamic.class)
public class RenderTickCounterDynamicMixin implements TickRateRenderTickCounter {

    @Shadow private long lastTimeMillis; // formerly prevTickMillis
    @Shadow private float tickProgress; // formerly tickDelta
    @Shadow @Final private float tickTime;

    @Unique private long lastLastTimeMillis; // formerly prevPrevTickMillis
    @Unique private float lastTickProgress; // formerly prevTickDelta
    @Unique private int movingI;
    @Unique private int i;
    // all things (except client's player) ticking at a certain TPS will tick/animate the exact same client-side, so just map TPS->TickProgressInfo
    @Unique private final Map<Integer, TickProgressInfo> prevTickProgress = new HashMap<>();
    @Unique private final Set<Integer> isUpdated = new HashSet<>();
    // the client's player can go above 20TPS, so it needs special treatment
    @Unique private TickProgressInfo clientPlayerTickProgressInfo = TickProgressInfo.NO_ANIMATE;
    @Unique private boolean clientPlayerUpdated = false;

    @Inject(method = "beginRenderTick(J)I", at = @At("HEAD"))
    private void beginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        lastLastTimeMillis = lastTimeMillis;
        lastTickProgress = tickProgress;
        isUpdated.clear();
        clientPlayerUpdated = false;
    }

    @Inject(method = "beginRenderTick(J)I", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void beginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir, int i) {
        this.i = i;
        TickRateClientManager.clearCache();
    }

    @Unique
    public TickProgressInfo tickRate$getSpecificTickProgressInfo(int tps) {
        float millisPerTick = 1000.0f / tps;
        if(isUpdated.contains(tps)) return prevTickProgress.get(tps);
        float dynamicDeltaTicks = (float)(lastTimeMillis - lastLastTimeMillis) / Math.max(millisPerTick, tickTime);
        float specificTickProgress = prevTickProgress.getOrDefault(tps,new TickProgressInfo(lastTickProgress,0,0)).tickProgress() + dynamicDeltaTicks;
        int i = (int) specificTickProgress;
        specificTickProgress -= (float) i;
        TickProgressInfo info = new TickProgressInfo(specificTickProgress,i, dynamicDeltaTicks);
        isUpdated.add(tps);
        prevTickProgress.put(tps,info);
        return info;
    }

    @Unique
    public TickProgressInfo tickRate$getClientPlayerTickProgressInfo(int tps) {
        // the client's player can go above 20TPS, so it needs special treatment
        float millisPerTick = 1000.0f / tps;
        if(clientPlayerUpdated) return clientPlayerTickProgressInfo;
        float dynamicDeltaTicks = (float)(lastTimeMillis - lastLastTimeMillis) / millisPerTick;
        float specificTickProgress = clientPlayerTickProgressInfo.tickProgress() + dynamicDeltaTicks;
        int i = (int) specificTickProgress;
        specificTickProgress -= (float) i;
        clientPlayerTickProgressInfo = new TickProgressInfo(specificTickProgress,i, dynamicDeltaTicks);
        clientPlayerUpdated = true;
        return clientPlayerTickProgressInfo;
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
