package io.github.dennisochulor.tickrate.mixin.core;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Shadow public abstract TickRateManager tickRateManager();

    @Shadow @Final private LevelTicks<Block> blockTicks;
    @Shadow @Final private LevelTicks<Fluid> fluidTicks;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        this.blockTicks.tickRate$setLevel((ServerLevel) (Object)this);
        this.fluidTicks.tickRate$setLevel((ServerLevel) (Object)this);
    }

    @ModifyVariable(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("STORE"), ordinal = 0)
    private boolean tick$shouldTick(boolean value) {
        ServerTickRateManager tickManager = (ServerTickRateManager) tickRateManager();
        return tickManager.tickRate$shouldTickServer();
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V",  at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", args = "ldc=tickPending"))
    private void tick$modifyBl(CallbackInfo ci, @Local LocalBooleanRef bl) {
        ServerTickRateManager serverTickManager = (ServerTickRateManager) tickRateManager();
        if(serverTickManager.tickRate$isServerSprint()) bl.set(true);
        else if(serverTickManager.isFrozen()) bl.set(serverTickManager.isSteppingForward());
        else bl.set(true);
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V",  at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=raid"))
    private void tick$modifyBl2(CallbackInfo ci, @Local LocalBooleanRef bl) {
        ServerTickRateManager tickManager = (ServerTickRateManager) tickRateManager();
        bl.set(tickManager.tickRate$shouldTickServer());
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V",  at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=blockEvents"))
    private void tick$modifyBl3(CallbackInfo ci, @Local LocalBooleanRef bl) {
        ServerTickRateManager serverTickManager = (ServerTickRateManager) tickRateManager();
        if(serverTickManager.tickRate$isServerSprint()) bl.set(true);
        else if(serverTickManager.isFrozen()) bl.set(serverTickManager.isSteppingForward());
        else bl.set(true);
    }

    @Inject(method = "method_31420",  at = @At(value = "HEAD"), cancellable = true)
    private void tick$entity(TickRateManager tickManager, ProfilerFiller profiler, Entity entity, CallbackInfo ci) {
        ServerTickRateManager tickManager1 = (ServerTickRateManager) tickManager;
        if(!tickManager1.tickRate$shouldTickEntity(entity)) ci.cancel();
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
