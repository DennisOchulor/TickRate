package io.github.dennisochulor.tickrate.mixin;

import io.github.dennisochulor.tickrate.TickRateChunkTickScheduler;
import io.github.dennisochulor.tickrate.TickRateTickManager;
import io.github.dennisochulor.tickrate.TickRateWorldTickScheduler;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.tick.ChunkTickScheduler;
import net.minecraft.world.tick.OrderedTick;
import net.minecraft.world.tick.WorldTickScheduler;
import org.spongepowered.asm.mixin.*;

import java.util.List;
import java.util.Queue;
import java.util.function.LongPredicate;

@Mixin(WorldTickScheduler.class)
public class WorldTickSchedulerMixin<T> implements TickRateWorldTickScheduler {

    @Shadow @Final private Long2ObjectMap<ChunkTickScheduler<T>> chunkTickSchedulers;
    @Shadow @Final private Queue<OrderedTick<T>> tickableTicks;
    @Shadow @Final private LongPredicate tickingFutureReadyPredicate;
    @Shadow @Final private Long2LongMap nextTriggerTickByChunkPos;
    @Shadow @Final private Queue<ChunkTickScheduler<T>> tickableChunkTickSchedulers;
    @Unique private World world;

    /**
     * @author Ninjaking312
     * @reason Because there is no other good way to do this damn it (continuing the while loop)
     */
    @Overwrite
    @SuppressWarnings("unchecked") // sigh
    private void collectTickableChunkTickSchedulers(long time) {
        ObjectIterator<Long2LongMap.Entry> objectIterator = Long2LongMaps.fastIterator(nextTriggerTickByChunkPos);

        while (objectIterator.hasNext()) {
            Long2LongMap.Entry entry = objectIterator.next();
            long l = entry.getLongKey();
            long m = entry.getLongValue();

            TickRateChunkTickScheduler<T> chunkTickScheduler = (TickRateChunkTickScheduler<T>) chunkTickSchedulers.get(l);
            TickRateTickManager tickManager = (TickRateTickManager) world.getTickManager();
            chunkTickScheduler.tickRate$setServerTime(time);
            if(tickManager.tickRate$getServerRate() == tickManager.tickRate$getChunkRate(world,l)) {
                chunkTickScheduler.tickRate$toggleMode(true);
            }
            else {
                chunkTickScheduler.tickRate$toggleMode(false);
                if(tickManager.tickRate$shouldTickChunk(world,l)) {
                    List<OrderedTick<T>> list = chunkTickScheduler.tickRate$tick();
                    this.tickableTicks.addAll(list);
                }
                continue; // this is your fault
            }

            if (m <= time) {
                ChunkTickScheduler<T> chunkTickScheduler1 = this.chunkTickSchedulers.get(l);
                if (chunkTickScheduler1 == null) {
                    objectIterator.remove();
                } else {
                    OrderedTick<T> orderedTick = chunkTickScheduler1.peekNextTick();
                    if (orderedTick == null) {
                        objectIterator.remove();
                    } else if (orderedTick.triggerTick() > time) {
                        entry.setValue(orderedTick.triggerTick());
                    } else if (this.tickingFutureReadyPredicate.test(l)) {
                        objectIterator.remove();
                        this.tickableChunkTickSchedulers.add(chunkTickScheduler1);
                    }
                }
            }
        }
    }

    @Unique
    public void tickRate$setWorld(ServerWorld world) {
        this.world = world;
    }

}
