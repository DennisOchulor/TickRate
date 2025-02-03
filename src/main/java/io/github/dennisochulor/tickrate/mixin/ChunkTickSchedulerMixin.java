package io.github.dennisochulor.tickrate.mixin;

import io.github.dennisochulor.tickrate.injected_interface.TickRateChunkTickScheduler;
import net.minecraft.world.tick.ChunkTickScheduler;
import net.minecraft.world.tick.OrderedTick;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

@Mixin(ChunkTickScheduler.class)
public abstract class ChunkTickSchedulerMixin<T> implements TickRateChunkTickScheduler<T> {

    @Shadow @Final private Queue<OrderedTick<T>> tickQueue;
    @Shadow public abstract OrderedTick<T> peekNextTick();

    @Shadow public abstract @Nullable OrderedTick<T> pollNextTick();

    @Unique private long chunkTime;
    @Unique private long serverTime; // actually should be worldTime
    @Unique private boolean isFollowingServerTick = true;

    @ModifyVariable(method = "scheduleTick", at = @At("HEAD"), argsOnly = true)
    public OrderedTick<T> scheduleTick(OrderedTick<T> orderedTick) {
        if(!isFollowingServerTick) {
            long newTriggerTick = chunkTime + (orderedTick.triggerTick()-serverTime);
            return new OrderedTick<>(orderedTick.type(),orderedTick.pos(),newTriggerTick,orderedTick.subTickOrder());
        }
        return orderedTick;
    }

    @ModifyVariable(method = "toNbt(JLjava/util/function/Function;)Lnet/minecraft/nbt/NbtList;", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/tick/Tick;orderedTickToNbt(Lnet/minecraft/world/tick/OrderedTick;Ljava/util/function/Function;J)Lnet/minecraft/nbt/NbtCompound;"))
    public OrderedTick<T> collectTicks(OrderedTick<T> orderedTick) {
        if(isFollowingServerTick) return orderedTick;
        else {
            long newTriggerTick = serverTime + (orderedTick.triggerTick() - chunkTime);
            return new OrderedTick<>(orderedTick.type(),orderedTick.pos(),newTriggerTick,orderedTick.subTickOrder());
        }
    }

    @Unique
    public void tickRate$setServerTime(long time) {
        serverTime = time;
    }

    @Unique
    public List<OrderedTick<T>> tickRate$tick() {
        chunkTime++;
        List<OrderedTick<T>> list = new ArrayList<>();
        while(peekNextTick() != null) {
            if(peekNextTick().triggerTick() <= chunkTime) list.add(pollNextTick());
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
            List<OrderedTick<T>> list = new ArrayList<>();
            tickQueue.forEach(orderedTick -> {
                long newTriggerTick = serverTime + (orderedTick.triggerTick() - chunkTime);
                OrderedTick<T> orderedTick1 = new OrderedTick<>(orderedTick.type(),orderedTick.pos(),newTriggerTick,orderedTick.subTickOrder());
                list.add(orderedTick1);
            });
            tickQueue.clear();
            tickQueue.addAll(list);
        }
    }



}
