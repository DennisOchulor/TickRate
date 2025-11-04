package io.github.dennisochulor.tickrate.mixin.client.tick;

import io.github.dennisochulor.tickrate.TickRateClientManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Shadow public abstract boolean isClientSide();

    @Inject(method = "guardEntityTick", at = @At("HEAD"), cancellable = true)
    public <T extends Entity> void tickEntity(Consumer<T> tickConsumer, T entity, CallbackInfo ci) {
        if(isClientSide() && TickRateClientManager.serverHasMod()) {
            DeltaTracker deltaTracker = Minecraft.getInstance().getDeltaTracker();
            if(deltaTracker.tickRate$getMovingI() >= TickRateClientManager.getEntityDeltaTrackerInfo(entity).i()) {
                ci.cancel();
            }
        }
    }

    @Redirect(method = "tickBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    protected void tickBlockEntities(TickingBlockEntity instance) {
        if(isClientSide() && TickRateClientManager.serverHasMod()) {
            DeltaTracker deltaTracker = Minecraft.getInstance().getDeltaTracker();
            if(deltaTracker.tickRate$getMovingI() < TickRateClientManager.getChunkDeltaTrackerInfo(new ChunkPos(instance.getPos())).i()) {
                instance.tick();
            }
        }
        else instance.tick();
    }

}
