package io.github.dennisochulor.tickrate.injected_interface;

import io.github.dennisochulor.tickrate.TickState;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Collection;

public interface TickRateTickManager {

    default void tickRate$serverStarting() {}
    default void tickRate$saveData() {}
    default void tickRate$addPlayerWithMod(ServerPlayerEntity player) {}
    default void tickRate$removePlayerWithMod(ServerPlayerEntity player) {}
    default boolean tickRate$hasClientMod(ServerPlayerEntity player) {return false;}

    default boolean tickRate$shouldTickEntity(Entity entity) {return false;}
    default boolean tickRate$shouldTickChunk(World world, ChunkPos chunkPos) {return false;}
    default boolean tickRate$shouldTickChunk(WorldChunk chunk) {return false;}
    default boolean tickRate$shouldTickServer() {return false;}

    default void tickRate$updateLoad(AttachmentTarget attachmentTarget, boolean loaded) {}
    default void tickRate$setServerRate(int rate) {}
    default int tickRate$getServerRate() {return 0;}
    default void tickRate$ticked() {}
    default boolean tickRate$isIndividualSprint() {return false;}
    default boolean tickRate$isServerSprint() {return false;}

    default void tickRate$setRate(int rate, Collection<? extends AttachmentTarget> targets) {}
    default void tickRate$setFrozen(boolean frozen, Collection<? extends AttachmentTarget> targets) {}
    default boolean tickRate$step(int steps, Collection<? extends AttachmentTarget> targets) {return false;}
    default boolean tickRate$sprint(int ticks, Collection<? extends AttachmentTarget> targets) {return false;}

    default int tickRate$getEntityRate(Entity entity) {return 0;}
    default int tickRate$getChunkRate(WorldChunk chunk) {return 0;}

    default TickState tickRate$getChunkTickStateShallow(World world, ChunkPos chunkPos) {return null;}
    default TickState tickRate$getEntityTickStateShallow(Entity entity) {return null;}
    default TickState tickRate$getChunkTickStateDeep(World world, ChunkPos chunkPos) {return null;}
    default TickState tickRate$getEntityTickStateDeep(Entity entity) {return null;}
    default TickState tickRate$getServerTickState() {return null;}

}
