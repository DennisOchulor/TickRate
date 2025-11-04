package io.github.dennisochulor.tickrate.api_impl;

import io.github.dennisochulor.tickrate.api.TickRateAPI;
import io.github.dennisochulor.tickrate.api.TickRateEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
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
    private final ServerTickRateManager tickManager;

    private TickRateAPIImpl(MinecraftServer server) {
        this.server = server;
        this.tickManager = this.server.tickRateManager();
    }


    private void entityCheck(Collection<? extends Entity> entities) throws IllegalArgumentException {
        Objects.requireNonNull(entities, "entities cannot be null!");
        entities.forEach(entity -> {
            if(entity.isRemoved()) throw new IllegalArgumentException("Entity must not be removed!");
        });
    }

    private List<LevelChunk> chunkCheck(Level level, Collection<ChunkPos> chunks) throws IllegalArgumentException {
        List<LevelChunk> levelChunks = new ArrayList<>();

        Objects.requireNonNull(level, "level cannot be null!");
        Objects.requireNonNull(chunks, "chunks cannot be null!");
        chunks.forEach(chunkPos -> {
            LevelChunk levelChunk = (LevelChunk) level.getChunk(chunkPos.x,chunkPos.z,ChunkStatus.FULL,false);
            if(levelChunk==null || levelChunk.getFullStatus() == FullChunkStatus.INACCESSIBLE)
                throw new IllegalArgumentException("Some of the specified chunks are not loaded!");
            levelChunks.add(levelChunk);
        });

        return levelChunks;
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
            if(tickManager.isSteppingForward()) tickManager.stopStepping();
        }
        tickManager.setFrozen(freeze);
        TickRateEvents.SERVER_FREEZE.invoker().onServerFreeze(server, freeze);
    }

    @Override
    public void stepServer(int stepTicks) {
        if(stepTicks < 0) throw new IllegalArgumentException("stepTicks must be >= 0");

        if(stepTicks == 0) tickManager.stopStepping();
        else {
            if(!tickManager.stepGameIfPaused(stepTicks)) throw new IllegalStateException("server must be frozen to step!");
            TickRateEvents.SERVER_STEP.invoker().onServerStep(server, stepTicks);
        }
    }

    @Override
    public void sprintServer(int sprintTicks) {
        if(sprintTicks < 0) throw new IllegalArgumentException("sprintTicks must be >= 0");

        if(sprintTicks == 0) tickManager.stopSprinting();
        else {
            tickManager.requestGameToSprint(sprintTicks);
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
    public float queryChunk(Level level, ChunkPos chunkPos) {
        return tickManager.tickRate$getChunkRate(chunkCheck(level, List.of(chunkPos)).getFirst());
    }

    @Override
    public void rateChunk(Level level, Collection<ChunkPos> chunks, float rate) {
        if(rate < 1.0f && rate != 0.0f) throw new IllegalArgumentException("rate must be >= 1 or exactly 0");
        List<LevelChunk> levelChunks = chunkCheck(level, chunks);

        int roundRate = Math.round(rate);
        tickManager.tickRate$setRate(roundRate==0 ? -1 : roundRate, levelChunks);
        levelChunks.forEach(levelChunk -> TickRateEvents.CHUNK_RATE.invoker().onChunkRate(levelChunk, roundRate));
    }

    @Override
    public void rateChunk(Level level, ChunkPos chunk, float rate) {
        rateChunk(level, List.of(chunk), rate);
    }

    @Override
    public void freezeChunk(Level level, Collection<ChunkPos> chunks, boolean freeze) {
        List<LevelChunk> levelChunks = chunkCheck(level, chunks);

        tickManager.tickRate$setFrozen(freeze, levelChunks);
        levelChunks.forEach(levelChunk -> TickRateEvents.CHUNK_FREEZE.invoker().onChunkFreeze(levelChunk, freeze));
    }

    @Override
    public void freezeChunk(Level level, ChunkPos chunk, boolean freeze) {
        freezeChunk(level, List.of(chunk), freeze);
    }

    @Override
    public void stepChunk(Level level, Collection<ChunkPos> chunks, int stepTicks) {
        if(stepTicks < 0) throw new IllegalArgumentException("stepTicks must be >= 0");
        List<LevelChunk> levelChunks = chunkCheck(level, chunks);

        if(tickManager.tickRate$step(stepTicks, levelChunks)) {
            if(stepTicks != 0) levelChunks.forEach(levelChunk -> TickRateEvents.CHUNK_STEP.invoker().onChunkStep(levelChunk, stepTicks));
        }
        else throw new IllegalArgumentException("All of the specified chunks must be frozen first and cannot be sprinting!");
    }

    @Override
    public void stepChunk(Level level, ChunkPos chunk, int stepTicks) {
        stepChunk(level, List.of(chunk), stepTicks);
    }

    @Override
    public void sprintChunk(Level level, Collection<ChunkPos> chunks, int sprintTicks) {
        if(sprintTicks < 0) throw new IllegalArgumentException("sprintTicks must be >= 0");
        List<LevelChunk> levelChunks = chunkCheck(level, chunks);

        if(tickManager.tickRate$sprint(sprintTicks, levelChunks)) {
            if(sprintTicks != 0) levelChunks.forEach(levelChunk -> TickRateEvents.CHUNK_SPRINT.invoker().onChunkSprint(levelChunk, sprintTicks));
        }
        else throw new IllegalArgumentException("All of the specified chunks must not be stepping!");
    }

    @Override
    public void sprintChunk(Level level, ChunkPos chunk, int sprintTicks) {
        sprintChunk(level, List.of(chunk), sprintTicks);
    }

}
