package io.github.dennisochulor.tickrate.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;


/**
 * TickRate related events. <p>
 * These events are fired when the associated <code>/tick</code> command is run and also when the associated
 * API method is called. Registering event handlers for these events is identical to registering event handlers for Fabric events. <p>
 *
 * Unlike {@link TickRateAPI}, event handlers can be registered before the server has fully initialised.
 */
@SuppressWarnings("unused")
public interface TickRateEvents {

    /**
     * Called when the tick rate of an entity has been modified.
     */
    Event<EntityRate> ENTITY_RATE = EventFactory.createArrayBacked(EntityRate.class, callbacks -> ((entity, rate) -> {
        for (EntityRate callback : callbacks) {
            callback.onEntityRate(entity, rate);
        }
    }));

    /**
     * Called when an entity is frozen or unfrozen.
     */
    Event<EntityFreeze> ENTITY_FREEZE = EventFactory.createArrayBacked(EntityFreeze.class, callbacks -> ((entity, freeze) -> {
        for (EntityFreeze callback : callbacks) {
            callback.onEntityFreeze(entity, freeze);
        }
    }));

    /**
     * Called when an entity starts stepping.
     */
    Event<EntityStep> ENTITY_STEP = EventFactory.createArrayBacked(EntityStep.class, callbacks -> ((entity, stepTicks) -> {
        for (EntityStep callback : callbacks) {
            callback.onEntityStep(entity, stepTicks);
        }
    }));

    /**
     * Called when an entity starts sprinting.
     */
    Event<EntitySprint> ENTITY_SPRINT = EventFactory.createArrayBacked(EntitySprint.class, callbacks -> ((entity, sprintTicks) -> {
        for (EntitySprint callback : callbacks) {
            callback.onEntitySprint(entity, sprintTicks);
        }
    }));

    /**
     * Called when the tick rate of a chunk is modified.
     */
    Event<ChunkRate> CHUNK_RATE = EventFactory.createArrayBacked(ChunkRate.class, callbacks -> ((world, chunkPos, rate) -> {
        for (ChunkRate callback : callbacks) {
            callback.onChunkRate(world, chunkPos, rate);
        }
    }));

    /**
     * Called when a chunk is frozen or unfrozen.
     */
    Event<ChunkFreeze> CHUNK_FREEZE = EventFactory.createArrayBacked(ChunkFreeze.class, callbacks -> ((world, chunkPos, freeze) -> {
        for (ChunkFreeze callback : callbacks) {
            callback.onChunkFreeze(world, chunkPos, freeze);
        }
    }));

    /**
     * Called when a chunk starts stepping.
     */
    Event<ChunkStep> CHUNK_STEP = EventFactory.createArrayBacked(ChunkStep.class, callbacks -> ((world, chunkPos, stepTicks) -> {
        for (ChunkStep callback : callbacks) {
            callback.onChunkStep(world, chunkPos, stepTicks);
        }
    }));

    /**
     * Called when a chunk starts sprinting.
     */
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
