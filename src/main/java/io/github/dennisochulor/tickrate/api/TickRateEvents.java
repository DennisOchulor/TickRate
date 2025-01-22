package io.github.dennisochulor.tickrate.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;


/**
 * TickRate related events.
 */
@SuppressWarnings("unused")
public final class TickRateEvents {
    private TickRateEvents() {}

    public static final Event<EntityRate> ENTITY_RATE = EventFactory.createArrayBacked(EntityRate.class, callbacks -> ((entity, rate) -> {
        for (EntityRate callback : callbacks) {
            callback.onEntityRate(entity, rate);
        }
    }));

    public static final Event<EntityFreeze> ENTITY_FREEZE = EventFactory.createArrayBacked(EntityFreeze.class, callbacks -> ((entity, freeze) -> {
        for (EntityFreeze callback : callbacks) {
            callback.onEntityFreeze(entity, freeze);
        }
    }));

    public static final Event<EntityStep> ENTITY_STEP = EventFactory.createArrayBacked(EntityStep.class, callbacks -> ((entity, stepTicks) -> {
        for (EntityStep callback : callbacks) {
            callback.onEntityStep(entity, stepTicks);
        }
    }));

    public static final Event<EntitySprint> ENTITY_SPRINT = EventFactory.createArrayBacked(EntitySprint.class, callbacks -> ((entity, sprintTicks) -> {
        for (EntitySprint callback : callbacks) {
            callback.onEntitySprint(entity, sprintTicks);
        }
    }));

    public static final Event<ChunkRate> CHUNK_RATE = EventFactory.createArrayBacked(ChunkRate.class, callbacks -> ((world, chunkPos, rate) -> {
        for (ChunkRate callback : callbacks) {
            callback.onChunkRate(world, chunkPos, rate);
        }
    }));

    public static final Event<ChunkFreeze> CHUNK_FREEZE = EventFactory.createArrayBacked(ChunkFreeze.class, callbacks -> ((world, chunkPos, freeze) -> {
        for (ChunkFreeze callback : callbacks) {
            callback.onChunkFreeze(world, chunkPos, freeze);
        }
    }));

    public static final Event<ChunkStep> CHUNK_STEP = EventFactory.createArrayBacked(ChunkStep.class, callbacks -> ((world, chunkPos, stepTicks) -> {
        for (ChunkStep callback : callbacks) {
            callback.onChunkStep(world, chunkPos, stepTicks);
        }
    }));

    public static final Event<ChunkSprint> CHUNK_SPRINT = EventFactory.createArrayBacked(ChunkSprint.class, callbacks -> ((world, chunkPos, sprintTicks) -> {
        for (ChunkSprint callback : callbacks) {
            callback.onChunkSprint(world, chunkPos, sprintTicks);
        }
    }));

    @FunctionalInterface
    public interface EntityRate {
        void onEntityRate(Entity entity, float rate);
    }

    @FunctionalInterface
    public interface EntityFreeze {
        void onEntityFreeze(Entity entity, boolean freeze);
    }

    @FunctionalInterface
    public interface EntityStep {
        void onEntityStep(Entity entity, int stepTicks);
    }

    @FunctionalInterface
    public interface EntitySprint {
        void onEntitySprint(Entity entity, int sprintTicks);
    }

    @FunctionalInterface
    public interface ChunkRate {
        void onChunkRate(World world, ChunkPos chunkPos, float rate);
    }

    @FunctionalInterface
    public interface ChunkFreeze {
        void onChunkFreeze(World world, ChunkPos chunkPos, boolean freeze);
    }

    @FunctionalInterface
    public interface ChunkStep {
        void onChunkStep(World world, ChunkPos chunkPos, int stepTicks);
    }

    @FunctionalInterface
    public interface ChunkSprint {
        void onChunkSprint(World world, ChunkPos chunkPos, int sprintTicks);
    }

}
