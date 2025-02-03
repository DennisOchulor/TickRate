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
 * Unlike {@link TickRateAPI}, event handlers can be registered before the server has fully initialised. <p>
 * It should be noted that these events do not necessarily fire immediately when a modification is made, there may be a delay
 * of a couple ticks. Though usually it will be fired within the same tick.
 */
public interface TickRateEvents {

    /**
     * Called when the tick rate of the server has been modified.
     */
    Event<ServerRate> SERVER_RATE = EventFactory.createArrayBacked(ServerRate.class, callbacks -> rate -> {
        for (ServerRate callback : callbacks) {
            callback.onServerRate(rate);
        }
    });

    /**
     * Called when the server is frozen or unfrozen.
     */
    Event<ServerFreeze> SERVER_FREEZE = EventFactory.createArrayBacked(ServerFreeze.class, callbacks -> freeze -> {
        for (ServerFreeze callback : callbacks) {
            callback.onServerFreeze(freeze);
        }
    });

    /**
     * Called when the server starts stepping.
     */
    Event<ServerStep> SERVER_STEP = EventFactory.createArrayBacked(ServerStep.class, callbacks -> stepTicks -> {
        for (ServerStep callback : callbacks) {
            callback.onServerStep(stepTicks);
        }
    });

    /**
     * Called when the server starts sprinting.
     */
    Event<ServerSprint> SERVER_SPRINT = EventFactory.createArrayBacked(ServerSprint.class, callbacks -> sprintTicks -> {
        for (ServerSprint callback : callbacks) {
            callback.onServerSprint(sprintTicks);
        }
    });


    /**
     * Called when the tick rate of an entity has been modified. <p>
     * If the entity's rate has been reset, <code>rate</code> will be 0.
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
     * Called when the tick rate of a chunk is modified. <p>
     * If the chunk's rate has been reset, <code>rate</code> will be 0.
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
    interface ServerRate {
        void onServerRate(float rate);
    }

    @FunctionalInterface
    interface ServerFreeze {
        void onServerFreeze(boolean freeze);
    }

    @FunctionalInterface
    interface ServerStep {
        void onServerStep(int stepTicks);
    }

    @FunctionalInterface
    interface ServerSprint {
        void onServerSprint(int sprintTicks);
    }

    @FunctionalInterface
    interface EntityRate {
        /**
         * If the entity's rate has been reset, <code>rate</code> will be 0.
         */
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
        /**
         * If the chunk's rate has been reset, <code>rate</code> will be 0.
         */
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
