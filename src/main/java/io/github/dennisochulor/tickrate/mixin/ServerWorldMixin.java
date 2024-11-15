package io.github.dennisochulor.tickrate.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import io.github.dennisochulor.tickrate.TickRateTickManager;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.tick.TickManager;
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

    @ModifyVariable(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("STORE"), ordinal = 0)
    private boolean tick$shouldTick(boolean value) {
        TickRateTickManager tickManager = (TickRateTickManager) getTickManager();
        return tickManager.tickRate$shouldTickServer();
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V",  at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V", args = "ldc=tickPending"))
    private void tick$modifyBl(CallbackInfo ci, @Local LocalBooleanRef bl) {
        bl.set(true);
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V",  at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=raid"))
    private void tick$modifyBl2(CallbackInfo ci, @Local LocalBooleanRef bl) {
        TickRateTickManager tickManager = (TickRateTickManager) getTickManager();
        bl.set(tickManager.tickRate$shouldTickServer());
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V",  at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=blockEvents"))
    private void tick$modifyBl3(CallbackInfo ci, @Local LocalBooleanRef bl) {
        bl.set(true);
    }

    @Inject(method = "method_31420",  at = @At(value = "HEAD"), cancellable = true)
    private void tick$entity(TickManager tickManager, Profiler profiler, Entity entity, CallbackInfo ci) {
        TickRateTickManager tickManager1 = (TickRateTickManager) tickManager;
        if(!tickManager1.tickRate$shouldTickEntity(entity)) ci.cancel();
    }

    @Inject(method = "shouldTickBlocksInChunk", at = @At("HEAD"), cancellable = true)
    public void shouldTickBlocksInChunk(long chunkPos, CallbackInfoReturnable<Boolean> cir) {
        ServerWorld world = (ServerWorld) (Object) this;
        TickRateTickManager tickManager = (TickRateTickManager) getTickManager();
        if(!tickManager.tickRate$shouldTickChunk(world, chunkPos)) cir.setReturnValue(false);
    }
    @Inject(method = "shouldTick(Lnet/minecraft/util/math/ChunkPos;)Z", at = @At("HEAD"), cancellable = true)
    public void shouldTick(ChunkPos pos, CallbackInfoReturnable<Boolean> cir) {
        ServerWorld world = (ServerWorld) (Object) this;
        TickRateTickManager tickManager = (TickRateTickManager) getTickManager();
        if(!tickManager.tickRate$shouldTickChunk(world, pos.toLong())) cir.setReturnValue(false);
    }

}
