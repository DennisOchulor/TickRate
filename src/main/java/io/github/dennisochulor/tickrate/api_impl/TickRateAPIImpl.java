package io.github.dennisochulor.tickrate.api_impl;

import io.github.dennisochulor.tickrate.api.TickRateAPI;
import io.github.dennisochulor.tickrate.api.TickRateEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class TickRateAPIImpl implements TickRateAPI {

    private static TickRateAPI INSTANCE;

    public static TickRateAPI getInstance() {
        if(INSTANCE == null) throw new IllegalStateException("The MinecraftServer must be fully initialised first before using the TickRateAPI!");
        return INSTANCE;
    }

    /** Only ever called when the logical server is fully initialised */
    public static void init(MinecraftServer server) {
        if(INSTANCE != null) throw new IllegalStateException("Only one instance can be present at any given time!");
        INSTANCE = new TickRateAPIImpl(server);
    }

    /** Only ever called when the logical server starts shutting down */
    public static void uninit() {
        INSTANCE = null;
    }


    private final MinecraftServer server;
    private final ServerTickManager tickManager;

    private TickRateAPIImpl(MinecraftServer server) {
        this.server = server;
        this.tickManager = this.server.getTickManager();
    }


    private void entityCheck(Collection<? extends Entity> entities) throws IllegalArgumentException {
        Objects.requireNonNull(entities, "entities cannot be null!");
        entities.forEach(entity -> {
            if(entity.isRemoved()) throw new IllegalArgumentException("Entity must not be removed!");
        });
    }

    private List<WorldChunk> chunkCheck(World world, Collection<ChunkPos> chunks) throws IllegalArgumentException {
        List<WorldChunk> worldChunks = new ArrayList<>();

        Objects.requireNonNull(world, "world cannot be null!");
        Objects.requireNonNull(chunks, "chunks cannot be null!");
        chunks.forEach(chunkPos -> {
            WorldChunk worldChunk = (WorldChunk) world.getChunk(chunkPos.x,chunkPos.z,ChunkStatus.FULL,false);
            if(worldChunk==null || worldChunk.getLevelType() == ChunkLevelType.INACCESSIBLE)
                throw new IllegalArgumentException("Some of the specified chunks are not loaded!");
            worldChunks.add(worldChunk);
        });

        return worldChunks;
    }


    // API Implementation

    @Override
    public float queryServer() {
        return tickManager.tickRate$getServerRate();
    }

    @Override
    public void rateServer(float rate) {
        if(rate < 1) throw new IllegalArgumentException("rate must be >= 1");
        int roundRate = Math.round(rate);
        tickManager.tickRate$setServerRate(roundRate);
        TickRateEvents.SERVER_RATE.invoker().onServerRate(server, roundRate);
    }

    @Override
    public void freezeServer(boolean freeze) {
        if(freeze) {
            if(tickManager.tickRate$isServerSprint()) tickManager.stopSprinting();
            if(tickManager.isStepping()) tickManager.stopStepping();
        }
        tickManager.setFrozen(freeze);
        TickRateEvents.SERVER_FREEZE.invoker().onServerFreeze(server, freeze);
    }

    @Override
    public void stepServer(int stepTicks) {
        if(stepTicks < 0) throw new IllegalArgumentException("stepTicks must be >= 0");

        if(stepTicks == 0) tickManager.stopStepping();
        else {
            if(!tickManager.step(stepTicks)) throw new IllegalStateException("server must be frozen to step!");
            TickRateEvents.SERVER_STEP.invoker().onServerStep(server, stepTicks);
        }
    }

    @Override
    public void sprintServer(int sprintTicks) {
        if(sprintTicks < 0) throw new IllegalArgumentException("sprintTicks must be >= 0");

        if(sprintTicks == 0) tickManager.stopSprinting();
        else {
            tickManager.startSprint(sprintTicks);
            TickRateEvents.SERVER_SPRINT.invoker().onServerSprint(server, sprintTicks);
        }
    }



    @Override
    public float queryEntity(Entity entity) {
        entityCheck(List.of(entity));
        return tickManager.tickRate$getEntityRate(entity);
    }

    @Override
    public void rateEntity(Collection<? extends Entity> entities, float rate) {
        if(rate < 1.0f && rate != 0.0f) throw new IllegalArgumentException("rate must be >= 1 or exactly 0");
        entityCheck(entities);

        int roundRate = Math.round(rate);
        tickManager.tickRate$setRate(roundRate==0 ? -1 : roundRate, entities);
        entities.forEach(entity -> TickRateEvents.ENTITY_RATE.invoker().onEntityRate(entity, roundRate));
    }

    @Override
    public void rateEntity(Entity entity, float rate) {
        rateEntity(List.of(entity), rate);
    }

    @Override
    public void freezeEntity(Collection<? extends Entity> entities, boolean freeze) {
        entityCheck(entities);

        tickManager.tickRate$setFrozen(freeze, entities);
        entities.forEach(entity -> TickRateEvents.ENTITY_FREEZE.invoker().onEntityFreeze(entity, freeze));
    }

    @Override
    public void freezeEntity(Entity entity, boolean freeze) {
        freezeEntity(List.of(entity), freeze);
    }

    @Override
    public void stepEntity(Collection<? extends Entity> entities, int stepTicks) {
        if(stepTicks < 0) throw new IllegalArgumentException("stepTicks must be >= 0");
        entityCheck(entities);

        if(tickManager.tickRate$step(stepTicks, entities)) {
            if(stepTicks != 0) entities.forEach(entity -> TickRateEvents.ENTITY_STEP.invoker().onEntityStep(entity, stepTicks));
        }
        else throw new IllegalArgumentException("All of the specified entities must be frozen first and cannot be sprinting!");
    }

    @Override
    public void stepEntity(Entity entity, int stepTicks) {
        stepEntity(List.of(entity), stepTicks);
    }

    @Override
    public void sprintEntity(Collection<? extends Entity> entities, int sprintTicks) {
        if(sprintTicks < 0) throw new IllegalArgumentException("sprintTicks must be >= 0");
        entityCheck(entities);

        if(tickManager.tickRate$sprint(sprintTicks, entities)) {
            if(sprintTicks != 0) entities.forEach(entity -> TickRateEvents.ENTITY_SPRINT.invoker().onEntitySprint(entity, sprintTicks));
        }
        else throw new IllegalArgumentException("All of the specified entities must not be stepping!");
    }

    @Override
    public void sprintEntity(Entity entity, int sprintTicks) {
        sprintEntity(List.of(entity), sprintTicks);
    }



    @Override
    public float queryChunk(World world, ChunkPos chunkPos) {
        return tickManager.tickRate$getChunkRate(chunkCheck(world, List.of(chunkPos)).getFirst());
    }

    @Override
    public void rateChunk(World world, Collection<ChunkPos> chunks, float rate) {
        if(rate < 1.0f && rate != 0.0f) throw new IllegalArgumentException("rate must be >= 1 or exactly 0");
        List<WorldChunk> worldChunks = chunkCheck(world, chunks);

        int roundRate = Math.round(rate);
        tickManager.tickRate$setRate(roundRate==0 ? -1 : roundRate, worldChunks);
        worldChunks.forEach(worldChunk -> TickRateEvents.CHUNK_RATE.invoker().onChunkRate(worldChunk, roundRate));
    }

    @Override
    public void rateChunk(World world, ChunkPos chunk, float rate) {
        rateChunk(world, List.of(chunk), rate);
    }

    @Override
    public void freezeChunk(World world, Collection<ChunkPos> chunks, boolean freeze) {
        List<WorldChunk> worldChunks = chunkCheck(world, chunks);

        tickManager.tickRate$setFrozen(freeze, worldChunks);
        worldChunks.forEach(worldChunk -> TickRateEvents.CHUNK_FREEZE.invoker().onChunkFreeze(worldChunk, freeze));
    }

    @Override
    public void freezeChunk(World world, ChunkPos chunk, boolean freeze) {
        freezeChunk(world, List.of(chunk), freeze);
    }

    @Override
    public void stepChunk(World world, Collection<ChunkPos> chunks, int stepTicks) {
        if(stepTicks < 0) throw new IllegalArgumentException("stepTicks must be >= 0");
        List<WorldChunk> worldChunks = chunkCheck(world, chunks);

        if(tickManager.tickRate$step(stepTicks, worldChunks)) {
            if(stepTicks != 0) worldChunks.forEach(worldChunk -> TickRateEvents.CHUNK_STEP.invoker().onChunkStep(worldChunk, stepTicks));
        }
        else throw new IllegalArgumentException("All of the specified chunks must be frozen first and cannot be sprinting!");
    }

    @Override
    public void stepChunk(World world, ChunkPos chunk, int stepTicks) {
        stepChunk(world, List.of(chunk), stepTicks);
    }

    @Override
    public void sprintChunk(World world, Collection<ChunkPos> chunks, int sprintTicks) {
        if(sprintTicks < 0) throw new IllegalArgumentException("sprintTicks must be >= 0");
        List<WorldChunk> worldChunks = chunkCheck(world, chunks);

        if(tickManager.tickRate$sprint(sprintTicks, worldChunks)) {
            if(sprintTicks != 0) worldChunks.forEach(worldChunk -> TickRateEvents.CHUNK_SPRINT.invoker().onChunkSprint(worldChunk, sprintTicks));
        }
        else throw new IllegalArgumentException("All of the specified chunks must not be stepping!");
    }

    @Override
    public void sprintChunk(World world, ChunkPos chunk, int sprintTicks) {
        sprintChunk(world, List.of(chunk), sprintTicks);
    }

}
