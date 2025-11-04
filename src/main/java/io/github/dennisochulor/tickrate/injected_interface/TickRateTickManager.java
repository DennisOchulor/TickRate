package io.github.dennisochulor.tickrate.injected_interface;

import io.github.dennisochulor.tickrate.TickState;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import java.util.Collection;

public interface TickRateTickManager {

    default void tickRate$serverStarting() {}
    default void tickRate$serverStarted() {}
    default void tickRate$saveData() {}

    default boolean tickRate$shouldTickEntity(Entity entity) {return false;}
    default boolean tickRate$shouldTickChunk(Level world, ChunkPos chunkPos) {return false;}
    default boolean tickRate$shouldTickChunk(LevelChunk chunk) {return false;}
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
    default int tickRate$getChunkRate(LevelChunk chunk) {return 0;}

    default TickState tickRate$getChunkTickStateShallow(Level world, ChunkPos chunkPos) {return null;}
    default TickState tickRate$getEntityTickStateShallow(Entity entity) {return null;}
    default TickState tickRate$getChunkTickStateDeep(Level world, ChunkPos chunkPos) {return null;}
    default TickState tickRate$getEntityTickStateDeep(Entity entity) {return null;}
    default TickState tickRate$getServerTickState() {return null;}

}
