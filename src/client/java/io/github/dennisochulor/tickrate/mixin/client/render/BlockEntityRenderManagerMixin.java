package io.github.dennisochulor.tickrate.mixin.client.render;

import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BlockEntityRenderManager.class)
public abstract class BlockEntityRenderManagerMixin {

    // modify BLOCK ENTITY tick progress
    @ModifyVariable(method = "getRenderState", at = @At("HEAD"), argsOnly = true)
    private float getRenderState(float tickProgress, BlockEntity blockEntity) {
        return TickRateClientManager.getChunkTickProgress(new ChunkPos(blockEntity.getPos())).tickProgress();
    }

}
