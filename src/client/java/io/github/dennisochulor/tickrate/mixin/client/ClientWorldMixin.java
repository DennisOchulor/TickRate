package io.github.dennisochulor.tickrate.mixin.client;

import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "randomBlockDisplayTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos$Mutable;set(III)Lnet/minecraft/util/math/BlockPos$Mutable;", shift = At.Shift.AFTER), cancellable = true)
    public void randomBlockDisplayTicks(int centerX, int centerY, int centerZ, int radius, Random random, Block block, BlockPos.Mutable pos, CallbackInfo ci) {
        float playerRate = TickRateClientManager.getEntityState(client.player).rate();
        if(playerRate > 20) playerRate = 20; // player can only animate at 20 TPS max
        float chunkRate = TickRateClientManager.getChunkState((World)(Object)this, ChunkPos.toLong(pos)).rate();
        if(playerRate > chunkRate) { // slow it down by chance if player ticking faster than chunk, otherwise ignore
            int chance = (int) (chunkRate / playerRate * 100);
            if(chance < random.nextBetween(1,100)) ci.cancel();
        }
    }

}
