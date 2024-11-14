package io.github.dennisochulor.tickrate;

import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Collection;

public interface TickRateTickManager {

    public void tickRate$serverStarted();
    public void tickRate$serverStopped();
    public boolean tickRate$shouldTickEntity(Entity entity);
    public boolean tickRate$shouldTickChunk(RegistryKey<World> registryKey, BlockPos pos);
    public boolean tickRate$shouldTickServer();
    public void tickRate$ticked();
    public void tickRate$setEntityRate(float rate, Collection<? extends Entity> entities);
    public float tickRate$getEntityRate(Entity entity);
    public void tickRate$setChunkRate(float rate, RegistryKey<World> registryKey, BlockPos pos);
    public float tickRate$getChunkRate(RegistryKey<World> registryKey, BlockPos pos);

}
