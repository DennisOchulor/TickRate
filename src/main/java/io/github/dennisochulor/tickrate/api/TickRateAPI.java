package io.github.dennisochulor.tickrate.api;

import io.github.dennisochulor.tickrate.api_impl.TickRateAPIImpl;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.Collection;

/**
 * API v0 for TickRate v0.3.0
 * <p>
 * This class represents the sole entrypoint for TickRate's API. This API should only be used on the server-side.
 */
public interface TickRateAPI {

    /**
     * Obtain the API instance. This method should only be called after the server has been fully initialised.
     * @throws IllegalStateException if the server has not been fully intialised yet.
     */
    static TickRateAPI getInstance() {
        return TickRateAPIImpl.getInstance();
    }

    /**
     * Returns the tick rate of the entity. <p>
     * If the entity has no specific tick rate, the tick rate of the chunk it is in will be returned.
     * If the chunk it is in also has no specific tick rate, the server's tick rate will be returned. <p>
     *
     * If the entity is a passenger, the tick rate of its {@link Entity#getRootVehicle() root vehicle} will be returned.
     *
     * @throws IllegalArgumentException if {@link Entity#isRemoved()} is true.
     */
    float queryEntity(Entity entity);

    void rateEntity(Collection<? extends Entity> entities, float rate);

    void rateEntity(Entity entity, float rate);

    void freezeEntity(Collection<? extends Entity> entities, boolean freeze);

    void freezeEntity(Entity entity, boolean freeze);

    void stepEntity(Collection<? extends Entity> entities, int stepTicks);

    void stepEntity(Entity entity, int stepTicks);

    void sprintEntity(Collection<? extends Entity> entities, int sprintTicks);

    void sprintEntity(Entity entity, int sprintTicks);


    /**
     * Returns the tick rate of the chunk. <p>
     * If the chunk has no specific tick rate, the server's tick rate will be returned.
     *
     * @throws IllegalArgumentException if the chunk's level type is {@link ChunkLevelType#INACCESSIBLE INACCESSIBLE}.
     */
    float queryChunk(World world, ChunkPos chunk);

    void rateChunk(Collection<? extends ChunkPos> chunks, float rate);

    void rateChunk(ChunkPos chunk, float rate);

    void freezeChunk(Collection<? extends ChunkPos> chunks, boolean freeze);

    void freezeChunk(ChunkPos chunk, boolean freeze);

    void stepChunk(Collection<? extends ChunkPos> chunks, int stepTicks);

    void stepChunk(ChunkPos chunk, int stepTicks);

    void sprintChunk(Collection<? extends ChunkPos> chunks, int sprintTicks);

    void sprintChunk(ChunkPos chunk, int sprintTicks);

}
