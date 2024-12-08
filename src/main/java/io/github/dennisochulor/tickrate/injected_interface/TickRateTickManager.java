package io.github.dennisochulor.tickrate.injected_interface;

import io.github.dennisochulor.tickrate.TickState;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import java.util.Collection;

public interface TickRateTickManager {

    default void tickRate$serverStarted() {}
    default void tickRate$serverStopped() {}
    default void tickRate$addPlayerWithMod(ServerPlayerEntity player) {}
    default void tickRate$removePlayerWithMod(ServerPlayerEntity player) {}
    default boolean tickRate$hasClientMod(ServerPlayerEntity player) {return false;}
    default void tickRate$sendUpdatePacket() {}
    default boolean tickRate$shouldTickEntity(Entity entity) {return false;}
    default boolean tickRate$shouldTickChunk(World world, long chunkPos) {return false;}
    default boolean tickRate$shouldTickServer() {return false;}
    default void tickRate$updateChunkLoad(World world, long chunkPos, boolean loaded) {}
    default void tickRate$updateEntityLoad(Entity entity, boolean loaded) {}
    default void tickRate$setServerRate(float rate) {}
    default float tickRate$getServerRate() {return 0;}
    default void tickRate$ticked() {}
    default boolean tickRate$isIndividualSprint() {return false;}
    default void tickRate$removeEntity(Entity entity, boolean rate, boolean steps, boolean sprint) {}
    default void tickRate$setEntityRate(float rate, Collection<? extends Entity> entities) {}
    default float tickRate$getEntityRate(Entity entity) {return 0;}
    default void tickRate$setEntityFrozen(boolean frozen, Collection<? extends Entity> entities) {}
    default boolean tickRate$stepEntity(int steps, Collection<? extends Entity> entities) {return false;}
    default boolean tickRate$sprintEntity(int ticks, Collection<? extends Entity> entities) {return false;}
    default void tickRate$setChunkRate(float rate, World world, long chunkPos) {}
    default float tickRate$getChunkRate(World world, long chunkPos) {return 0;}
    default void tickRate$setChunkFrozen(boolean frozen, World world, long chunkPos) {}
    default boolean tickRate$stepChunk(int steps, World world, long chunkPos) {return false;}
    default boolean tickRate$sprintChunk(int ticks, World world, long chunkPos) {return false;}
    default TickState tickRate$getChunkTickState(World world, long chunkPos) {return null;}
    default TickState tickRate$getEntityTickState(Entity entity) {return null;}
    default TickState tickRate$getServerTickState() {return null;}

}
