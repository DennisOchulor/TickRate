package io.github.dennisochulor.tickrate.mixin.client.tick;

import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(World.class)
public abstract class WorldMixin {

    @Shadow public abstract boolean isClient();

    @Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
    public <T extends Entity> void tickEntity(Consumer<T> tickConsumer, T entity, CallbackInfo ci) {
        if(isClient() && TickRateClientManager.serverHasMod()) {
            RenderTickCounter renderTickCounter = MinecraftClient.getInstance().getRenderTickCounter();
            if(renderTickCounter.tickRate$getMovingI() >= TickRateClientManager.getEntityTickProgress(entity).i()) {
                ci.cancel();
            }
        }
    }

    @Redirect(method = "tickBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/BlockEntityTickInvoker;tick()V"))
    protected void tickBlockEntities(BlockEntityTickInvoker instance) {
        if(isClient() && TickRateClientManager.serverHasMod()) {
            RenderTickCounter renderTickCounter = MinecraftClient.getInstance().getRenderTickCounter();
            if(renderTickCounter.tickRate$getMovingI() < TickRateClientManager.getChunkTickProgress(ChunkPos.toLong(instance.getPos())).i()) {
                instance.tick();
            }
        }
        else instance.tick();
    }

}
