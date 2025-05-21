package io.github.dennisochulor.tickrate.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;


/**
 * TickRate related events. <p>
 * These events are fired when the associated <code>/tick</code> command is run and also when the associated
 * API method is called. Registering event handlers for these events is identical to registering event handlers for Fabric events. <p>
 *
 * Unlike {@link TickRateAPI}, event handlers can be registered before the server has fully initialised.<p>
 *
 * Note that if any {@link TickRateAPI} methods that trigger events are <i>not</i> called on the server thread, then the event will fire off-thread.
 * For regular in-game command executions, events are always fired on-thread.
 */
public interface TickRateEvents {

    /**
     * Called when the tick rate of the server has been modified.
     */
    Event<ServerRate> SERVER_RATE = EventFactory.createArrayBacked(ServerRate.class, callbacks -> (server, rate) -> {
        for (ServerRate callback : callbacks) {
            callback.onServerRate(server, rate);
        }
    });

    /**
     * Called when the server is frozen or unfrozen.
     */
    Event<ServerFreeze> SERVER_FREEZE = EventFactory.createArrayBacked(ServerFreeze.class, callbacks -> (server, freeze) -> {
        for (ServerFreeze callback : callbacks) {
            callback.onServerFreeze(server, freeze);
        }
    });

    /**
     * Called when the server starts stepping.
     */
    Event<ServerStep> SERVER_STEP = EventFactory.createArrayBacked(ServerStep.class, callbacks -> (server, stepTicks) -> {
        for (ServerStep callback : callbacks) {
            callback.onServerStep(server, stepTicks);
        }
    });

    /**
     * Called when the server starts sprinting.
     */
    Event<ServerSprint> SERVER_SPRINT = EventFactory.createArrayBacked(ServerSprint.class, callbacks -> (server, sprintTicks) -> {
        for (ServerSprint callback : callbacks) {
            callback.onServerSprint(server, sprintTicks);
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
    Event<ChunkRate> CHUNK_RATE = EventFactory.createArrayBacked(ChunkRate.class, callbacks -> ((chunk, rate) -> {
        for (ChunkRate callback : callbacks) {
            callback.onChunkRate(chunk, rate);
        }
    }));

    /**
     * Called when a chunk is frozen or unfrozen.
     */
    Event<ChunkFreeze> CHUNK_FREEZE = EventFactory.createArrayBacked(ChunkFreeze.class, callbacks -> ((chunk, freeze) -> {
        for (ChunkFreeze callback : callbacks) {
            callback.onChunkFreeze(chunk, freeze);
        }
    }));

    /**
     * Called when a chunk starts stepping.
     */
    Event<ChunkStep> CHUNK_STEP = EventFactory.createArrayBacked(ChunkStep.class, callbacks -> ((chunk, stepTicks) -> {
        for (ChunkStep callback : callbacks) {
            callback.onChunkStep(chunk, stepTicks);
        }
    }));

    /**
     * Called when a chunk starts sprinting.
     */
    Event<ChunkSprint> CHUNK_SPRINT = EventFactory.createArrayBacked(ChunkSprint.class, callbacks -> ((chunk, sprintTicks) -> {
        for (ChunkSprint callback : callbacks) {
            callback.onChunkSprint(chunk, sprintTicks);
        }
    }));



    @FunctionalInterface
    interface ServerRate {
        void onServerRate(MinecraftServer server, float rate);
    }

    @FunctionalInterface
    interface ServerFreeze {
        void onServerFreeze(MinecraftServer server, boolean freeze);
    }

    @FunctionalInterface
    interface ServerStep {
        void onServerStep(MinecraftServer server, int stepTicks);
    }

    @FunctionalInterface
    interface ServerSprint {
        void onServerSprint(MinecraftServer server, int sprintTicks);
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
        void onChunkRate(WorldChunk chunk, float rate);
    }

    @FunctionalInterface
    interface ChunkFreeze {
        void onChunkFreeze(WorldChunk chunk, boolean freeze);
    }

    @FunctionalInterface
    interface ChunkStep {
        void onChunkStep(WorldChunk chunk, int stepTicks);
    }

    @FunctionalInterface
    interface ChunkSprint {
        void onChunkSprint(WorldChunk chunk, int sprintTicks);
    }

}
