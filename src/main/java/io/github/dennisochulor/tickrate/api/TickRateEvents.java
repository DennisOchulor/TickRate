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
public interface TickRateEvents {

    Event<EntityRate> ENTITY_RATE = EventFactory.createArrayBacked(EntityRate.class, callbacks -> ((entity, rate) -> {
        for (EntityRate callback : callbacks) {
            callback.onEntityRate(entity, rate);
        }
    }));

    Event<EntityFreeze> ENTITY_FREEZE = EventFactory.createArrayBacked(EntityFreeze.class, callbacks -> ((entity, freeze) -> {
        for (EntityFreeze callback : callbacks) {
            callback.onEntityFreeze(entity, freeze);
        }
    }));

    Event<EntityStep> ENTITY_STEP = EventFactory.createArrayBacked(EntityStep.class, callbacks -> ((entity, stepTicks) -> {
        for (EntityStep callback : callbacks) {
            callback.onEntityStep(entity, stepTicks);
        }
    }));

    Event<EntitySprint> ENTITY_SPRINT = EventFactory.createArrayBacked(EntitySprint.class, callbacks -> ((entity, sprintTicks) -> {
        for (EntitySprint callback : callbacks) {
            callback.onEntitySprint(entity, sprintTicks);
        }
    }));

    Event<ChunkRate> CHUNK_RATE = EventFactory.createArrayBacked(ChunkRate.class, callbacks -> ((world, chunkPos, rate) -> {
        for (ChunkRate callback : callbacks) {
            callback.onChunkRate(world, chunkPos, rate);
        }
    }));

    Event<ChunkFreeze> CHUNK_FREEZE = EventFactory.createArrayBacked(ChunkFreeze.class, callbacks -> ((world, chunkPos, freeze) -> {
        for (ChunkFreeze callback : callbacks) {
            callback.onChunkFreeze(world, chunkPos, freeze);
        }
    }));

    Event<ChunkStep> CHUNK_STEP = EventFactory.createArrayBacked(ChunkStep.class, callbacks -> ((world, chunkPos, stepTicks) -> {
        for (ChunkStep callback : callbacks) {
            callback.onChunkStep(world, chunkPos, stepTicks);
        }
    }));

    Event<ChunkSprint> CHUNK_SPRINT = EventFactory.createArrayBacked(ChunkSprint.class, callbacks -> ((world, chunkPos, sprintTicks) -> {
        for (ChunkSprint callback : callbacks) {
            callback.onChunkSprint(world, chunkPos, sprintTicks);
        }
    }));

    @FunctionalInterface
    interface EntityRate {
        void onEntityRate(Entity entity, float rate);
    }

    @FunctionalInterface
   interface EntityFreeze {
        void onEntityFreeze(Entity entity, boolean freeze);
    }

    @FunctionalInterface
    interface EntityStep {
        void onEntityStep(Entity entity, int stepTicks);
    }

    @FunctionalInterface
    interface EntitySprint {
        void onEntitySprint(Entity entity, int sprintTicks);
    }

    @FunctionalInterface
    interface ChunkRate {
        void onChunkRate(World world, ChunkPos chunkPos, float rate);
    }

    @FunctionalInterface
    interface ChunkFreeze {
        void onChunkFreeze(World world, ChunkPos chunkPos, boolean freeze);
    }

    @FunctionalInterface
    interface ChunkStep {
        void onChunkStep(World world, ChunkPos chunkPos, int stepTicks);
    }

    @FunctionalInterface
    interface ChunkSprint {
        void onChunkSprint(World world, ChunkPos chunkPos, int sprintTicks);
    }

}
