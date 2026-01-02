package io.github.dennisochulor.tickrate.injected_interface;

import io.github.dennisochulor.tickrate.TickState;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import java.util.Collection;

public interface TickRateTickManager {

    default void tickRate$serverStarting() { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default void tickRate$serverStarted() { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default void tickRate$saveData() { throw new UnsupportedOperationException("Implemented via mixin!"); }

    default boolean tickRate$shouldTickEntity(Entity entity) { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default boolean tickRate$shouldTickChunk(Level world, ChunkPos chunkPos) { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default boolean tickRate$shouldTickChunk(LevelChunk chunk) { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default boolean tickRate$shouldTickServer() { throw new UnsupportedOperationException("Implemented via mixin!"); }

    default void tickRate$updateLoad(AttachmentTarget attachmentTarget, boolean loaded) { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default void tickRate$setServerRate(int rate) { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default int tickRate$getServerRate() { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default void tickRate$ticked() { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default boolean tickRate$isIndividualSprint() { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default boolean tickRate$isServerSprint() { throw new UnsupportedOperationException("Implemented via mixin!"); }

    default void tickRate$setRate(int rate, Collection<? extends AttachmentTarget> targets) { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default void tickRate$setFrozen(boolean frozen, Collection<? extends AttachmentTarget> targets) { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default boolean tickRate$step(int steps, Collection<? extends AttachmentTarget> targets) { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default boolean tickRate$sprint(int ticks, Collection<? extends AttachmentTarget> targets) { throw new UnsupportedOperationException("Implemented via mixin!"); }

    default int tickRate$getEntityRate(Entity entity) { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default int tickRate$getChunkRate(LevelChunk chunk) { throw new UnsupportedOperationException("Implemented via mixin!"); }

    default TickState tickRate$getChunkTickStateShallow(Level world, ChunkPos chunkPos) { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default TickState tickRate$getEntityTickStateShallow(Entity entity) { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default TickState tickRate$getChunkTickStateDeep(Level world, ChunkPos chunkPos) { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default TickState tickRate$getEntityTickStateDeep(Entity entity) { throw new UnsupportedOperationException("Implemented via mixin!"); }
    default TickState tickRate$getServerTickState() { throw new UnsupportedOperationException("Implemented via mixin!"); }

}
