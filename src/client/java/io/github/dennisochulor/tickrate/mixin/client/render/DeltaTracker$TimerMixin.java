package io.github.dennisochulor.tickrate.mixin.client.render;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.dennisochulor.tickrate.DeltaTrackerInfo;
import io.github.dennisochulor.tickrate.TickRateClientManager;
import io.github.dennisochulor.tickrate.injected_interface.TickRateDeltaTracker;
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
import net.minecraft.client.DeltaTracker;

@Mixin(DeltaTracker.Timer.class)
public class DeltaTracker$TimerMixin implements TickRateDeltaTracker {

    @Shadow private long lastMs; // formerly prevTickMillis
    @Shadow private float deltaTickResidual; // formerly tickProgress
    @Shadow @Final private float msPerTick;

    @Unique private long lastLastTimeMillis; // formerly prevPrevTickMillis
    @Unique private float lastPartialTick; // formerly prevTickProgress
    @Unique private int movingTicksToDo;
    @Unique private int ticksToDo;
    // all things (except client's player) ticking at a certain TPS will tick/animate the exact same client-side, so just map TPS->TickProgressInfo
    @Unique private final Map<Integer, DeltaTrackerInfo> prevPartialTicks = new HashMap<>();
    @Unique private final Set<Integer> isUpdated = new HashSet<>();
    // the client's player can go above 20TPS, so it needs special treatment
    @Unique private DeltaTrackerInfo clientPlayerDeltaTrackerInfo = DeltaTrackerInfo.NO_ANIMATE;
    @Unique private boolean clientPlayerUpdated = false;

    @Inject(method = "advanceGameTime(J)I", at = @At("HEAD"))
    private void beginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        lastLastTimeMillis = lastMs;
        lastPartialTick = deltaTickResidual;
        isUpdated.clear();
        clientPlayerUpdated = false;
    }

    @Inject(method = "advanceGameTime(J)I", at = @At("TAIL"))
    private void beginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir, @Local(name = "ticks") int ticks) {
        this.ticksToDo = ticks;
        TickRateClientManager.clearCache();
    }

    @Unique
    public DeltaTrackerInfo tickRate$getDeltaTrackerInfo(int tps) {
        float millisPerTick = 1000.0f / tps;
        if(isUpdated.contains(tps)) return prevPartialTicks.get(tps);
        float dynamicDeltaTicks = (float)(lastMs - lastLastTimeMillis) / Math.max(millisPerTick, msPerTick);
        float specificPartialTick = prevPartialTicks.getOrDefault(tps, new DeltaTrackerInfo(lastPartialTick,0,0)).partialTick() + dynamicDeltaTicks;
        int i = (int) specificPartialTick;
        specificPartialTick -= (float) i;
        DeltaTrackerInfo info = new DeltaTrackerInfo(specificPartialTick, i, dynamicDeltaTicks);
        isUpdated.add(tps);
        prevPartialTicks.put(tps, info);
        return info;
    }

    @Unique
    public DeltaTrackerInfo tickRate$getClientPlayerDeltaTrackerInfo(int tps) {
        // the client's player can go above 20TPS, so it needs special treatment
        float millisPerTick = 1000.0f / tps;
        if(clientPlayerUpdated) return clientPlayerDeltaTrackerInfo;
        float deltaTicks = (float)(lastMs - lastLastTimeMillis) / millisPerTick;
        float specificPartialTick = clientPlayerDeltaTrackerInfo.partialTick() + deltaTicks;
        int i = (int) specificPartialTick;
        specificPartialTick -= (float) i;
        clientPlayerDeltaTrackerInfo = new DeltaTrackerInfo(specificPartialTick, i, deltaTicks);
        clientPlayerUpdated = true;
        return clientPlayerDeltaTrackerInfo;
    }

    @Override
    public void tickRate$setMovingTicksToDo(int i) {
        movingTicksToDo = i;
    }

    @Override
    public int tickRate$getMovingTicksToDo() {
        return movingTicksToDo;
    }

    @Override
    public int tickRate$getTicksToDo() {
        return ticksToDo;
    }

}
