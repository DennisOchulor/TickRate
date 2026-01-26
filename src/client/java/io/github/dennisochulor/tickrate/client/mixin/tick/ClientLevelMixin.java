package io.github.dennisochulor.tickrate.client.mixin.tick;

import io.github.dennisochulor.tickrate.client.TickRateClientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "doAnimateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos$MutableBlockPos;set(III)Lnet/minecraft/core/BlockPos$MutableBlockPos;", shift = At.Shift.AFTER), cancellable = true)
    public void randomBlockDisplayTicks(int centerX, int centerY, int centerZ, int radius, RandomSource random, Block block, BlockPos.MutableBlockPos pos, CallbackInfo ci) {
        Objects.requireNonNull(minecraft.player);
        float playerChunkRate = Math.min(20, TickRateClientManager.getChunkState(minecraft.player.chunkPosition()).rate());
        float chunkRate = TickRateClientManager.getChunkState(ChunkPos.containing(pos)).rate();
        if(playerChunkRate > chunkRate) { // slow it down by chance if player's chunk ticking faster than the random chunk, otherwise ignore
            int chance = (int) (chunkRate / playerChunkRate * 100);
            if(chance < random.nextIntBetweenInclusive(1,100)) ci.cancel();
        }
    }

}
