package io.github.dennisochulor.tickrate;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import java.util.Collection;

public interface TickRateTickManager {

    void tickRate$serverStarted();
    void tickRate$serverStopped();
    void tickRate$addPlayerWithMod(ServerPlayerEntity player);
    void tickRate$removePlayerWithMod(ServerPlayerEntity player);
    boolean tickRate$hasClientMod(ServerPlayerEntity player);
    void tickRate$sendUpdatePacket();
    boolean tickRate$shouldTickEntity(Entity entity);
    boolean tickRate$shouldTickChunk(World world, long chunkPos);
    boolean tickRate$shouldTickServer();
    void tickRate$setServerRate(float rate);
    float tickRate$getServerRate();
    void tickRate$ticked();
    boolean tickRate$isIndividualSprint();
    void tickRate$removeEntity(Entity entity, boolean rate, boolean steps, boolean sprint);
    void tickRate$setEntityRate(float rate, Collection<? extends Entity> entities);
    float tickRate$getEntityRate(Entity entity);
    void tickRate$setEntityFrozen(boolean frozen, Collection<? extends Entity> entities);
    boolean tickRate$stepEntity(int steps, Collection<? extends Entity> entities);
    boolean tickRate$sprintEntity(int ticks, Collection<? extends Entity> entities);
    void tickRate$setChunkRate(float rate, World world, long chunkPos);
    float tickRate$getChunkRate(World world, long chunkPos);
    void tickRate$setChunkFrozen(boolean frozen, World world, long chunkPos);
    boolean tickRate$stepChunk(int steps, World world, long chunkPos);
    boolean tickRate$sprintChunk(int ticks, World world, long chunkPos);
    TickState tickRate$getChunkTickState(World world, long chunkPos);
    TickState tickRate$getEntityTickState(Entity entity);
    TickState tickRate$getServerTickState();

}
