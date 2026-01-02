package io.github.dennisochulor.tickrate.mixin.chunk;

import io.github.dennisochulor.tickrate.injected_interface.TickRateLevelChunkTicks;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.SavedTick;
import net.minecraft.world.ticks.ScheduledTick;

// formerly ChunkTickScheduler
@Mixin(LevelChunkTicks.class)
public abstract class LevelChunkTicksMixin<T> implements TickRateLevelChunkTicks<T> {

    @Shadow @Final private Queue<ScheduledTick<T>> tickQueue;
    @Shadow public abstract @Nullable ScheduledTick<T> peek();
    @Shadow public abstract @Nullable ScheduledTick<T> poll();

    @Unique private long chunkTime;
    @Unique private long serverTime;
    @Unique private boolean isFollowingServerTick = true;

    @ModifyVariable(method = "schedule", at = @At("HEAD"), argsOnly = true)
    public ScheduledTick<T> schedule(ScheduledTick<T> scheduledTick) {
        if(!isFollowingServerTick) {
            long newTriggerTick = chunkTime + (scheduledTick.triggerTick()-serverTime);
            return new ScheduledTick<>(scheduledTick.type(), scheduledTick.pos(), newTriggerTick, scheduledTick.subTickOrder());
        }
        return scheduledTick;
    }

    @Redirect(method = "pack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ticks/ScheduledTick;toSavedTick(J)Lnet/minecraft/world/ticks/SavedTick;"))
    public SavedTick<T> pack(ScheduledTick<T> scheduledTick, long currentTick) {
        if(isFollowingServerTick) return scheduledTick.toSavedTick(currentTick);
        else {
            long newTriggerTick = currentTick + (scheduledTick.triggerTick() - chunkTime);
            ScheduledTick<T> orderedTick1 = new ScheduledTick<>(scheduledTick.type(), scheduledTick.pos(),newTriggerTick, scheduledTick.subTickOrder());
            return orderedTick1.toSavedTick(currentTick);
        }

    }

    @Unique
    public void tickRate$setServerTime(long time) {
        serverTime = time;
    }

    @Unique
    public List<ScheduledTick<T>> tickRate$tick() {
        chunkTime++;
        List<ScheduledTick<T>> list = new ArrayList<>();
        ScheduledTick<T> scheduledTick = peek();
        while(scheduledTick != null) {
            if(scheduledTick.triggerTick() <= chunkTime) {
                list.add(scheduledTick);
                poll();
                scheduledTick = peek();
            }
            else break;
        }
        return list;
    }

    @Unique
    public void tickRate$toggleMode(boolean followServerTick) {
        if(!followServerTick && isFollowingServerTick) {
            isFollowingServerTick = false;
            chunkTime = serverTime-1;
        }
        else if(followServerTick && !isFollowingServerTick) {
            isFollowingServerTick = true;
            List<ScheduledTick<T>> list = new ArrayList<>();
            tickQueue.forEach(scheduledTick -> {
                long newTriggerTick = serverTime + (scheduledTick.triggerTick() - chunkTime);
                ScheduledTick<T> scheduledTick1 = new ScheduledTick<>(scheduledTick.type(), scheduledTick.pos(),newTriggerTick, scheduledTick.subTickOrder());
                list.add(scheduledTick1);
            });
            tickQueue.clear();
            tickQueue.addAll(list);
        }
    }



}
