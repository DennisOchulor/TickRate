package io.github.dennisochulor.tickrate.mixin.chunk;

import io.github.dennisochulor.tickrate.injected_interface.TickRateLevelTicks;
import io.github.dennisochulor.tickrate.TickState;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.spongepowered.asm.mixin.*;

import java.util.List;
import java.util.Queue;
import java.util.function.LongPredicate;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;

// formerly WorldTickScheduler
@Mixin(LevelTicks.class)
public class LevelTicksMixin<T> implements TickRateLevelTicks {

    @Shadow @Final private Long2ObjectMap<LevelChunkTicks<T>> allContainers;
    @Shadow @Final private Queue<ScheduledTick<T>> toRunThisTick;
    @Shadow @Final private LongPredicate tickCheck;
    @Shadow @Final private Long2LongMap nextTickForContainer;
    @Shadow @Final private Queue<LevelChunkTicks<T>> containersToTick;

    @Unique private Level level;

    /**
     * @author Ninjaking312
     * @reason Because there is no other good way to do this damn it (continuing the while loop)
     */
    @Overwrite
    @SuppressWarnings("unchecked")
    private void sortContainersToTick(long time) {
        ObjectIterator<Long2LongMap.Entry> objectIterator = Long2LongMaps.fastIterator(nextTickForContainer);

        while (objectIterator.hasNext()) {
            Long2LongMap.Entry entry = objectIterator.next();
            long l = entry.getLongKey();
            long m = entry.getLongValue();

            LevelChunkTicks<T> levelChunkTicks = allContainers.get(l);
            ServerTickRateManager tickManager = (ServerTickRateManager) level.tickRateManager();
            levelChunkTicks.tickRate$setServerTime(time);
            TickState tickState = tickManager.tickRate$getChunkTickStateShallow(level, new ChunkPos(l));
            if(tickState.rate() == -1 && !tickState.frozen() && !tickState.sprinting()) {
                levelChunkTicks.tickRate$toggleMode(true);
            }
            else {
                levelChunkTicks.tickRate$toggleMode(false);
                if(tickManager.tickRate$shouldTickChunk(level,new ChunkPos(l))) {
                    List<ScheduledTick<T>> list = levelChunkTicks.tickRate$tick();
                    this.toRunThisTick.addAll(list);
                }
                continue; // this is your fault
            }

            if (m <= time) {
                LevelChunkTicks<T> levelChunkTicks1 = this.allContainers.get(l);
                if (levelChunkTicks1 == null) {
                    objectIterator.remove();
                } else {
                    ScheduledTick<T> scheduledTick = levelChunkTicks1.peek();
                    if (scheduledTick == null) {
                        objectIterator.remove();
                    } else if (scheduledTick.triggerTick() > time) {
                        entry.setValue(scheduledTick.triggerTick());
                    } else if (this.tickCheck.test(l)) {
                        objectIterator.remove();
                        this.containersToTick.add(levelChunkTicks1);
                    }
                }
            }
        }
    }

    @Unique // should only be called ONCE inside ServerWorld <init>
    public void tickRate$setLevel(ServerLevel level) {
        this.level = level;
    }

}
