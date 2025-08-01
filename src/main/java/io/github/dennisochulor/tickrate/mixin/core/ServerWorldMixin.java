package io.github.dennisochulor.tickrate.mixin.core;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.tick.TickManager;
import net.minecraft.world.tick.WorldTickScheduler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {

    @Shadow public abstract TickManager getTickManager();

    @Shadow @Final private WorldTickScheduler<Block> blockTickScheduler;
    @Shadow @Final private WorldTickScheduler<Fluid> fluidTickScheduler;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        this.blockTickScheduler.tickRate$setWorld((ServerWorld) (Object)this);
        this.fluidTickScheduler.tickRate$setWorld((ServerWorld) (Object)this);
    }

    @ModifyVariable(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("STORE"), ordinal = 0)
    private boolean tick$shouldTick(boolean value) {
        ServerTickManager tickManager = (ServerTickManager) getTickManager();
        return tickManager.tickRate$shouldTickServer();
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V",  at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V", args = "ldc=tickPending"))
    private void tick$modifyBl(CallbackInfo ci, @Local LocalBooleanRef bl) {
        ServerTickManager serverTickManager = (ServerTickManager) getTickManager();
        if(serverTickManager.tickRate$isServerSprint()) bl.set(true);
        else if(serverTickManager.isFrozen()) bl.set(serverTickManager.isStepping());
        else bl.set(true);
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V",  at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=raid"))
    private void tick$modifyBl2(CallbackInfo ci, @Local LocalBooleanRef bl) {
        ServerTickManager tickManager = (ServerTickManager) getTickManager();
        bl.set(tickManager.tickRate$shouldTickServer());
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V",  at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=blockEvents"))
    private void tick$modifyBl3(CallbackInfo ci, @Local LocalBooleanRef bl) {
        ServerTickManager serverTickManager = (ServerTickManager) getTickManager();
        if(serverTickManager.tickRate$isServerSprint()) bl.set(true);
        else if(serverTickManager.isFrozen()) bl.set(serverTickManager.isStepping());
        else bl.set(true);
    }

    @Inject(method = "method_31420",  at = @At(value = "HEAD"), cancellable = true)
    private void tick$entity(TickManager tickManager, Profiler profiler, Entity entity, CallbackInfo ci) {
        ServerTickManager tickManager1 = (ServerTickManager) tickManager;
        if(!tickManager1.tickRate$shouldTickEntity(entity)) ci.cancel();
    }

    // for random ticks
    @Inject(method = "tickChunk", at = @At("HEAD"), cancellable = true)
    private void tickChunk(WorldChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        ServerTickManager tickManager = (ServerTickManager) getTickManager();
        if(!tickManager.tickRate$shouldTickChunk(chunk))
            ci.cancel();
    }

    // tickSpawners doesn't differentiate between chunks, so use server TPS i guess...
    @Inject(method = "tickSpawners", at = @At("HEAD"), cancellable = true)
    private void tickSpawners(boolean spawnMonsters, boolean spawnAnimals, CallbackInfo ci) {
        ServerTickManager tickManager = (ServerTickManager) getTickManager();
        if(!tickManager.tickRate$shouldTickServer())
            ci.cancel();
    }

    // handles block entity ticking, among other things
    @Inject(method = "shouldTickBlocksInChunk", at = @At("HEAD"), cancellable = true)
    public void shouldTickBlocksInChunk(long chunkPos, CallbackInfoReturnable<Boolean> cir) {
        ServerWorld world = (ServerWorld) (Object) this;
        ServerTickManager tickManager = (ServerTickManager) getTickManager();
        if(!tickManager.tickRate$shouldTickChunk(world, new ChunkPos(chunkPos))) cir.setReturnValue(false);
    }

    @Inject(method = "shouldTickChunkAt", at = @At("HEAD"), cancellable = true)
    public void shouldTickChunkAt(ChunkPos pos, CallbackInfoReturnable<Boolean> cir) {
        ServerWorld world = (ServerWorld) (Object) this;
        ServerTickManager tickManager = (ServerTickManager) getTickManager();
        if(!tickManager.tickRate$shouldTickChunk(world, pos)) cir.setReturnValue(false);
    }

    @Inject(method = "shouldTickEntityAt", at = @At("HEAD"), cancellable = true)
    public void shouldTickEntityAt(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ServerWorld world = (ServerWorld) (Object) this;
        ServerTickManager tickManager = (ServerTickManager) getTickManager();
        if(!tickManager.tickRate$shouldTickChunk(world, new ChunkPos(pos))) cir.setReturnValue(false);
    }

    @Inject(method = "canSpawnEntitiesAt", at = @At("HEAD"), cancellable = true)
    public void canSpawnEntitiesAt(ChunkPos pos, CallbackInfoReturnable<Boolean> cir) {
        ServerWorld world = (ServerWorld) (Object) this;
        ServerTickManager tickManager = (ServerTickManager) getTickManager();
        if(!tickManager.tickRate$shouldTickChunk(world, pos)) cir.setReturnValue(false);
    }

}
