package io.github.dennisochulor.tickrate.api_impl;

import io.github.dennisochulor.tickrate.api.TickRateAPI;
import io.github.dennisochulor.tickrate.api.TickRateEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class TickRateAPIImpl implements TickRateAPI {

    private static TickRateAPI INSTANCE;

    public static TickRateAPI getInstance() {
        if(INSTANCE == null) throw new IllegalStateException("The MinecraftServer must be fully initialised first before using the TickRateAPI!");
        return INSTANCE;
    }

    /** Only ever called ONCE by TickRate's ModInitializer */
    public static void init(MinecraftServer server) {
        if(INSTANCE != null) throw new IllegalStateException("Only one instance can be created!");
        INSTANCE = new TickRateAPIImpl(server);
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
            if(entity instanceof ServerPlayerEntity player && !tickManager.tickRate$hasClientMod(player))
                throw new IllegalArgumentException("Some of the specified entities are players that do not have TickRate mod installed on their client, so their tick rate cannot be manipulated.");
            if(entity.isRemoved()) throw new IllegalArgumentException("Entity must not be removed!");
        });
    }

    private void chunkCheck(World world, Collection<ChunkPos> chunks) throws IllegalArgumentException {
        Objects.requireNonNull(world, "world cannot be null!");
        Objects.requireNonNull(chunks, "chunks cannot be null!");
        chunks.forEach(chunkPos -> {
            WorldChunk worldChunk = (WorldChunk) world.getChunk(chunkPos.x,chunkPos.z,ChunkStatus.FULL,false);
            if(worldChunk==null || worldChunk.getLevelType() == ChunkLevelType.INACCESSIBLE)
                throw new IllegalArgumentException("Some of the specified chunks are not loaded!");
        });
    }

    private void send(Runnable runnable) {
        server.send(server.createTask(runnable));
    }


    // API Implementation

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
        tickManager.tickRate$setEntityRate(roundRate, entities);
        tickManager.tickRate$sendUpdatePacket();
        send(() -> entities.forEach(entity -> TickRateEvents.ENTITY_RATE.invoker().onEntityRate(entity, roundRate)));
    }

    @Override
    public void rateEntity(Entity entity, float rate) {
        rateEntity(List.of(entity), rate);
    }

    @Override
    public void freezeEntity(Collection<? extends Entity> entities, boolean freeze) {
        entityCheck(entities);

        tickManager.tickRate$setEntityFrozen(freeze, entities);
        tickManager.tickRate$sendUpdatePacket();
        send(() -> entities.forEach(entity -> TickRateEvents.ENTITY_FREEZE.invoker().onEntityFreeze(entity, freeze)));
    }

    @Override
    public void freezeEntity(Entity entity, boolean freeze) {
        freezeEntity(List.of(entity), freeze);
    }

    @Override
    public void stepEntity(Collection<? extends Entity> entities, int stepTicks) {
        if(stepTicks < 0) throw new IllegalArgumentException("stepTicks must be >= 0");
        entityCheck(entities);

        if(tickManager.tickRate$stepEntity(stepTicks, entities)) {
            tickManager.tickRate$sendUpdatePacket();
            if(stepTicks != 0) send(() -> entities.forEach(entity -> TickRateEvents.ENTITY_STEP.invoker().onEntityStep(entity, stepTicks)));
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

        if(tickManager.tickRate$sprintEntity(sprintTicks, entities)) {
            tickManager.tickRate$sendUpdatePacket();
            if(sprintTicks != 0) send(() -> entities.forEach(entity -> TickRateEvents.ENTITY_SPRINT.invoker().onEntitySprint(entity, sprintTicks)));
        }
        else throw new IllegalArgumentException("All of the specified entities must not be stepping!");
    }

    @Override
    public void sprintEntity(Entity entity, int sprintTicks) {
        sprintEntity(List.of(entity), sprintTicks);
    }

    @Override
    public float queryChunk(World world, ChunkPos chunk) {
        chunkCheck(world, List.of(chunk));
        return tickManager.tickRate$getChunkRate(world, chunk.toLong());
    }

    @Override
    public void rateChunk(World world, Collection<ChunkPos> chunks, float rate) {
        if(rate < 1.0f && rate != 0.0f) throw new IllegalArgumentException("rate must be >= 1 or exactly 0");
        chunkCheck(world, chunks);

        int roundRate = Math.round(rate);
        tickManager.tickRate$setChunkRate(rate, world, chunks);
        tickManager.tickRate$sendUpdatePacket();
        send(() -> chunks.forEach(chunkPos -> TickRateEvents.CHUNK_RATE.invoker().onChunkRate(world, chunkPos, roundRate)));
    }

    @Override
    public void rateChunk(World world, ChunkPos chunk, float rate) {
        rateChunk(world, List.of(chunk), rate);
    }

    @Override
    public void freezeChunk(World world, Collection<ChunkPos> chunks, boolean freeze) {
        chunkCheck(world, chunks);

        tickManager.tickRate$setChunkFrozen(freeze, world, chunks);
        tickManager.tickRate$sendUpdatePacket();
        send(() -> chunks.forEach(chunkPos -> TickRateEvents.CHUNK_FREEZE.invoker().onChunkFreeze(world, chunkPos, freeze)));
    }

    @Override
    public void freezeChunk(World world, ChunkPos chunk, boolean freeze) {
        freezeChunk(world, List.of(chunk), freeze);
    }

    @Override
    public void stepChunk(World world, Collection<ChunkPos> chunks, int stepTicks) {
        if(stepTicks < 0) throw new IllegalArgumentException("stepTicks must be >= 0");
        chunkCheck(world, chunks);

        if(tickManager.tickRate$stepChunk(stepTicks, world, chunks)) {
            tickManager.tickRate$sendUpdatePacket();
            if(stepTicks != 0) send(() -> chunks.forEach(chunkPos -> TickRateEvents.CHUNK_STEP.invoker().onChunkStep(world, chunkPos, stepTicks)));
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
        chunkCheck(world, chunks);

        if(tickManager.tickRate$sprintChunk(sprintTicks, world, chunks)) {
            tickManager.tickRate$sendUpdatePacket();
            if(sprintTicks != 0) send(() -> chunks.forEach(chunkPos -> TickRateEvents.CHUNK_SPRINT.invoker().onChunkSprint(world, chunkPos, sprintTicks)));
        }
        else throw new IllegalArgumentException("All of the specified chunks must not be stepping!");
    }

    @Override
    public void sprintChunk(World world, ChunkPos chunk, int sprintTicks) {
        sprintChunk(world, List.of(chunk), sprintTicks);
    }

}
