package io.github.dennisochulor.tickrate;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.util.Collection;

public interface TickRateTickManager {

    void tickRate$serverStarted();
    void tickRate$serverStopped();
    boolean tickRate$shouldTickEntity(Entity entity);
    boolean tickRate$shouldTickChunk(World world, long chunkPos);
    boolean tickRate$shouldTickServer();
    void tickRate$setServerRate(float rate);
    float tickRate$getServerRate();
    void tickRate$ticked();
    void tickRate$setEntityRate(float rate, Collection<? extends Entity> entities);
    float tickRate$getEntityRate(Entity entity);
    void tickRate$setChunkRate(float rate, World world, long chunkPos);
    float tickRate$getChunkRate(World world, long chunkPos);

}
