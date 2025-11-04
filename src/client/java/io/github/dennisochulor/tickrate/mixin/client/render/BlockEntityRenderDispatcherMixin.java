package io.github.dennisochulor.tickrate.mixin.client.render;

import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {

    // modify BLOCK ENTITY partial tick
    @ModifyVariable(method = "tryExtractRenderState", at = @At("HEAD"), argsOnly = true)
    private float tryExtractRenderState(float partialTick, BlockEntity blockEntity) {
        return TickRateClientManager.getChunkDeltaTrackerInfo(new ChunkPos(blockEntity.getBlockPos())).partialTick();
    }

}
