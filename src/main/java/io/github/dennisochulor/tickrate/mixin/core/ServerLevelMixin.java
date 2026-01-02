package io.github.dennisochulor.tickrate.mixin.core;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelTicks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Shadow public abstract TickRateManager tickRateManager();

    @Shadow @Final private LevelTicks<Block> blockTicks;
    @Shadow @Final private LevelTicks<Fluid> fluidTicks;

    @SuppressWarnings("DataFlowIssue") // class cast will not fail at runtime cause mixins
    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        this.blockTicks.tickRate$setLevel((ServerLevel) (Object)this);
        this.fluidTicks.tickRate$setLevel((ServerLevel) (Object)this);
    }

    @ModifyVariable(method = "tick", at = @At("STORE"), name = "runs")
    private boolean tick$shouldTick(boolean value) {
        ServerTickRateManager tickManager = (ServerTickRateManager) tickRateManager();
        return tickManager.tickRate$shouldTickServer();
    }

    @Inject(method = "tick",  at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", args = "ldc=tickPending"))
    private void tick$tickPending(CallbackInfo ci, @Local(name = "runs") LocalBooleanRef runs) {
        ServerTickRateManager serverTickManager = (ServerTickRateManager) tickRateManager();
        if(serverTickManager.tickRate$isServerSprint()) runs.set(true);
        else if(serverTickManager.isFrozen()) runs.set(serverTickManager.isSteppingForward());
        else runs.set(true);
    }

    @Inject(method = "tick",  at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=raid"))
    private void tick$raid(CallbackInfo ci, @Local(name = "runs") LocalBooleanRef runs) {
        ServerTickRateManager tickManager = (ServerTickRateManager) tickRateManager();
        runs.set(tickManager.tickRate$shouldTickServer());
    }

    // also handles emptyTime
    @Inject(method = "tick",  at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=blockEvents"))
    private void tick$blockEvents(CallbackInfo ci, @Local(name = "runs") LocalBooleanRef runs) {
        ServerTickRateManager serverTickManager = (ServerTickRateManager) tickRateManager();
        if(serverTickManager.tickRate$isServerSprint()) runs.set(true);
        else if(serverTickManager.isFrozen()) runs.set(serverTickManager.isSteppingForward());
        else runs.set(true);
    }

    // head of entity tick lambda
    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/EntityTickList;forEach(Ljava/util/function/Consumer;)V"))
    private Consumer<Entity> tick$entity(Consumer<Entity> output) {
        ServerTickRateManager serverTickManager = (ServerTickRateManager) tickRateManager();
        return entity -> {
            if(serverTickManager.tickRate$shouldTickEntity(entity)) output.accept(entity);
        };
    }

    // for random ticks
    @Inject(method = "tickChunk", at = @At("HEAD"), cancellable = true)
    private void tickChunk(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        ServerTickRateManager tickManager = (ServerTickRateManager) tickRateManager();
        if(!tickManager.tickRate$shouldTickChunk(chunk))
            ci.cancel();
    }

    // tickSpawners doesn't differentiate between chunks, so use server TPS i guess...
    @Inject(method = "tickCustomSpawners", at = @At("HEAD"), cancellable = true)
    private void tickSpawners(boolean spawnMonsters, CallbackInfo ci) {
        ServerTickRateManager tickManager = (ServerTickRateManager) tickRateManager();
        if(!tickManager.tickRate$shouldTickServer())
            ci.cancel();
    }

    // handles block entity ticking, among other things
    @Inject(method = "shouldTickBlocksAt(J)Z", at = @At("HEAD"), cancellable = true)
    public void shouldTickBlocksAt(long chunkPos, CallbackInfoReturnable<Boolean> cir) {
        ServerLevel level = (ServerLevel) (Object) this;
        ServerTickRateManager tickManager = (ServerTickRateManager) tickRateManager();
        if(!tickManager.tickRate$shouldTickChunk(level, new ChunkPos(chunkPos))) cir.setReturnValue(false);
    }

    @Inject(method = "anyPlayerCloseEnoughForSpawning(Lnet/minecraft/world/level/ChunkPos;)Z", at = @At("HEAD"), cancellable = true)
    public void shouldTickChunkAt(ChunkPos pos, CallbackInfoReturnable<Boolean> cir) {
        ServerLevel level = (ServerLevel) (Object) this;
        ServerTickRateManager tickManager = (ServerTickRateManager) tickRateManager();
        if(!tickManager.tickRate$shouldTickChunk(level, pos)) cir.setReturnValue(false);
    }

    @Inject(method = "isPositionEntityTicking", at = @At("HEAD"), cancellable = true)
    public void shouldTickEntityAt(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ServerLevel level = (ServerLevel) (Object) this;
        ServerTickRateManager tickManager = (ServerTickRateManager) tickRateManager();
        if(!tickManager.tickRate$shouldTickChunk(level, new ChunkPos(pos))) cir.setReturnValue(false);
    }

    @Inject(method = "canSpawnEntitiesInChunk", at = @At("HEAD"), cancellable = true)
    public void canSpawnEntitiesAt(ChunkPos pos, CallbackInfoReturnable<Boolean> cir) {
        ServerLevel level = (ServerLevel) (Object) this;
        ServerTickRateManager tickManager = (ServerTickRateManager) tickRateManager();
        if(!tickManager.tickRate$shouldTickChunk(level, pos)) cir.setReturnValue(false);
    }

}
