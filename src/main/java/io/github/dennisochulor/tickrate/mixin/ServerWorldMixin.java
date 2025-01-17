package io.github.dennisochulor.tickrate.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.profiler.Profiler;
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
        this.blockTickScheduler.tickRate$setWorld((ServerWorld) (Object)this); // this sucks man...
        this.fluidTickScheduler.tickRate$setWorld((ServerWorld) (Object)this); // this sucks man...
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

    @Inject(method = "shouldTickBlocksInChunk", at = @At("HEAD"), cancellable = true)
    public void shouldTickBlocksInChunk(long chunkPos, CallbackInfoReturnable<Boolean> cir) {
        ServerWorld world = (ServerWorld) (Object) this;
        ServerTickManager tickManager = (ServerTickManager) getTickManager();
        if(!tickManager.tickRate$shouldTickChunk(world, chunkPos)) cir.setReturnValue(false);
    }

    @Inject(method = "shouldTick(Lnet/minecraft/util/math/ChunkPos;)Z", at = @At("HEAD"), cancellable = true)
    public void shouldTick(ChunkPos pos, CallbackInfoReturnable<Boolean> cir) {
        ServerWorld world = (ServerWorld) (Object) this;
        ServerTickManager tickManager = (ServerTickManager) getTickManager();
        if(!tickManager.tickRate$shouldTickChunk(world, pos.toLong())) cir.setReturnValue(false);
    }

}
