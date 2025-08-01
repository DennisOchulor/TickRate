package io.github.dennisochulor.tickrate.api;

import io.github.dennisochulor.tickrate.api_impl.TickRateAPIImpl;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.Collection;

/**
 * API v3 for TickRate v0.5.0 <p>
 * This class represents the sole entrypoint for TickRate's API. This API should only be used on the logical server.
 *
 * @see TickRateEvents
 */
public interface TickRateAPI {

    /**
     * Obtain the API instance. This method should only be called after the server has been fully initialised (e.g. when the SERVER_STARTED event is fired). <p>
     * In singleplayer, a new API instance is used each time a new save is loaded.
     * @throws IllegalStateException if the server has not been fully intialised yet.
     */
    static TickRateAPI getInstance() {
        return TickRateAPIImpl.getInstance();
    }

    /**
     * Returns the server's tick rate.
     */
    float queryServer();

    /**
     * Set the tick rate of the server. <code>rate</code> is rounded using {@link Math#round(float)}
     *
     * @throws IllegalArgumentException if <code>rate</code> is less than 1.
     */
    void rateServer(float rate);

    /**
     * Freezes or unfreezes the server depending on <code>freeze</code>.
     *
     * @param freeze <code>true</code> to freeze, <code>false</code> to unfreeze.
     */
    void freezeServer(boolean freeze);

    /**
     * Steps the server. The server will step according to its current TPS. <p>
     * If <code>stepTicks</code> is 0, the server will stop stepping.
     *
     * @throws IllegalArgumentException if <code>stepTicks</code> is less than 0.
     * @throws IllegalStateException if the server is not frozen.
     */
    void stepServer(int stepTicks);

    /**
     * Sprints the server. <p>
     * If <code>sprintTicks</code> is 0, the server will stop sprinting.
     *
     * @throws IllegalArgumentException if <code>sprintTicks</code> is less than 0.
     */
    void sprintServer(int sprintTicks);



    /**
     * Returns the tick rate of the entity. <p>
     * If the entity has no specific tick rate, the tick rate of the chunk it is in will be returned.
     * If the chunk it is in also has no specific tick rate, the server's tick rate will be returned. <p>
     *
     * If the entity is a passenger, the tick rate of its {@link Entity#getRootVehicle() root vehicle} will be returned.
     * If the server is stepping, the server's tick rate is returned.
     *
     * @throws IllegalArgumentException if {@link Entity#isRemoved()} is true.
     */
    float queryEntity(Entity entity);

    /**
     * Sets the tick rate of the specified entities. <code>rate</code> is rounded using {@link Math#round(float)} <p>
     * If <code>rate</code> is exactly <code>0.0f</code>, then the entities' tick rate will be reset.
     *
     * @throws IllegalArgumentException if {@link Entity#isRemoved()} is true for any of the specified entities OR if <code>rate</code> is less than 1 and not 0.
     */
    void rateEntity(Collection<? extends Entity> entities, float rate);

    /**
     * Sets the tick rate of the specified entity. <code>rate</code> is rounded using {@link Math#round(float)} <p>
     * If <code>rate</code> is exactly <code>0.0f</code>, then the entity's tick rate will be reset.
     *
     * @throws IllegalArgumentException if {@link Entity#isRemoved()} is true OR if <code>rate</code> is less than 1 and not 0.
     */
    void rateEntity(Entity entity, float rate);

    /**
     * Freezes or unfreezes the specified entities depending on <code>freeze</code>.
     *
     * @param freeze <code>true</code> to freeze, <code>false</code> to unfreeze
     * @throws IllegalArgumentException if {@link Entity#isRemoved()} is true for any of the specified entities.
     */
    void freezeEntity(Collection<? extends Entity> entities, boolean freeze);

    /**
     * Freezes or unfreezes the specified entity depending on <code>freeze</code>.
     *
     * @param freeze <code>true</code> to freeze, <code>false</code> to unfreeze
     * @throws IllegalArgumentException if {@link Entity#isRemoved()} is true the specified entity.
     */
    void freezeEntity(Entity entity, boolean freeze);

    /**
     * Steps the specified entities for <code>stepTicks</code> ticks. The entities will step according to their current TPS. <p>
     * If <code>stepTicks</code> is 0, the entities will stop stepping.
     *
     * @throws IllegalArgumentException if any of the following conditions are met: <p>
     * <ul>
     *          <li> {@link Entity#isRemoved()} is true for any of the specified entities.
     *          <li> Any of the specified entities are currently NOT frozen or ARE sprinting.
     *          <li> <code>stepTicks</code> is less than 0.
     * </ul>
     */
    void stepEntity(Collection<? extends Entity> entities, int stepTicks);

    /**
     * Steps the specified entity for <code>stepTicks</code> ticks. The entity will step according to its current TPS. <p>
     * If <code>stepTicks</code> is 0, the entitiy will stop stepping.
     *
     * @throws IllegalArgumentException if any of the following conditions are met: <p>
     * <ul>
     *          <li> {@link Entity#isRemoved()} is true.
     *          <li> The specified entity is currently NOT frozen or IS sprinting.
     *          <li> <code>stepTicks</code> is less than 0.
     * </ul>
     */
    void stepEntity(Entity entity, int stepTicks);

    /**
     * Sprints the specified entities for <code>sprintTicks</code> ticks. <p>
     * If <code>sprintTicks</code> is 0, the entities will stop sprinting.
     *
     * @throws IllegalArgumentException if any of the following conditions are met: <p>
     * <ul>
     *          <li> {@link Entity#isRemoved()} is true for any of the specified entities.
     *          <li> Any of the specified entities are currently stepping.
     *          <li> <code>sprintTicks</code> is less than 0.
     * </ul>
     */
    void sprintEntity(Collection<? extends Entity> entities, int sprintTicks);

    /**
     * Sprints the specified entity for <code>sprintTicks</code> ticks. <p>
     * If <code>sprintTicks</code> is 0, the entity will stop sprinting.
     *
     * @throws IllegalArgumentException if any of the following conditions are met: <p>
     * <ul>
     *          <li> {@link Entity#isRemoved()} is true.
     *          <li> The specified entity is currently stepping.
     *          <li> <code>sprintTicks</code> is less than 0.
     * </ul>
     */
    void sprintEntity(Entity entity, int sprintTicks);



    /**
     * Returns the tick rate of the chunk. <p>
     * If the chunk has no specific tick rate, the server's tick rate will be returned.
     * <p>
     * If the server is stepping, the server's tick rate is returned.
     *
     * @throws IllegalArgumentException if the chunk is {@link ChunkLevelType#INACCESSIBLE INACCESSIBLE} (not loaded).
     */
    float queryChunk(World world, ChunkPos chunk);

    /**
     * Sets the tick rate of the specified chunks. <code>rate</code> is rounded using {@link Math#round(float)} <p>
     * If <code>rate</code> is exactly <code>0.0f</code>, then the chunks' tick rate will be reset.
     *
     * @throws IllegalArgumentException if any of the chunks are {@link ChunkLevelType#INACCESSIBLE INACCESSIBLE} (not loaded) OR if <code>rate</code> is less than 1 and not 0.
     */
    void rateChunk(World world, Collection<ChunkPos> chunks, float rate);

    /**
     * Sets the tick rate of the specified chunk. <code>rate</code> is rounded using {@link Math#round(float)} <p>
     * If <code>rate</code> is exactly <code>0.0f</code>, then the chunk's tick rate will be reset.
     *
     * @throws IllegalArgumentException if the chunk is {@link ChunkLevelType#INACCESSIBLE INACCESSIBLE} (not loaded) OR if <code>rate</code> is less than 1 and not 0.
     */
    void rateChunk(World world, ChunkPos chunk, float rate);

    /**
     * Freezes or unfreezes the specified chunks depending on <code>freeze</code>.
     *
     * @param freeze <code>true</code> to freeze, <code>false</code> to unfreeze
     * @throws IllegalArgumentException if any of the chunks are {@link ChunkLevelType#INACCESSIBLE INACCESSIBLE} (not loaded).
     */
    void freezeChunk(World world, Collection<ChunkPos> chunks, boolean freeze);

    /**
     * Freezes or unfreezes the specified chunk depending on <code>freeze</code>.
     *
     * @param freeze <code>true</code> to freeze, <code>false</code> to unfreeze
     * @throws IllegalArgumentException if the chunk is {@link ChunkLevelType#INACCESSIBLE INACCESSIBLE} (not loaded).
     */
    void freezeChunk(World world, ChunkPos chunk, boolean freeze);

    /**
     * Steps the specified chunks for <code>stepTicks</code> ticks. The chunks will step according to their current TPS. <p>
     * If <code>stepTicks</code> is 0, the chunks will stop stepping.
     *
     * @throws IllegalArgumentException if any of the following conditions are met: <p>
     * <ul>
     *          <li> Any of the chunks are {@link ChunkLevelType#INACCESSIBLE INACCESSIBLE} (not loaded).
     *          <li> Any of the specified chunks are currently NOT frozen or ARE sprinting.
     *          <li> <code>stepTicks</code> is less than 0.
     * </ul>
     */
    void stepChunk(World world, Collection<ChunkPos> chunks, int stepTicks);

    /**
     * Steps the specified chunk for <code>stepTicks</code> ticks. The chunk will step according to its current TPS. <p>
     * If <code>stepTicks</code> is 0, the chunk will stop stepping.
     *
     * @throws IllegalArgumentException if any of the following conditions are met: <p>
     * <ul>
     *          <li> The chunk is {@link ChunkLevelType#INACCESSIBLE INACCESSIBLE} (not loaded).
     *          <li> The specified chunk is currently NOT frozen or IS sprinting.
     *          <li> <code>stepTicks</code> is less than 0.
     * </ul>
     */
    void stepChunk(World world, ChunkPos chunk, int stepTicks);

    /**
     * Sprints the specified chunks for <code>sprintTicks</code> ticks. <p>
     * If <code>sprintTicks</code> is 0, the chunks will stop sprinting.
     *
     * @throws IllegalArgumentException if any of the following conditions are met: <p>
     * <ul>
     *          <li> Any of the chunks are {@link ChunkLevelType#INACCESSIBLE INACCESSIBLE} (not loaded).
     *          <li> Any of the specified chunks are currently stepping.
     *          <li> <code>sprintTicks</code> is less than 0.
     * </ul>
     */
    void sprintChunk(World world, Collection<ChunkPos> chunks, int sprintTicks);

    /**
     * Sprints the specified chunk for <code>sprintTicks</code> ticks. <p>
     * If <code>sprintTicks</code> is 0, the chunk will stop sprinting.
     *
     * @throws IllegalArgumentException if any of the following conditions are met: <p>
     * <ul>
     *          <li> The chunk is {@link ChunkLevelType#INACCESSIBLE INACCESSIBLE} (not loaded).
     *          <li> The specified chunk is currently stepping.
     *          <li> <code>sprintTicks</code> is less than 0.
     * </ul>
     */
    void sprintChunk(World world, ChunkPos chunk, int sprintTicks);

}
