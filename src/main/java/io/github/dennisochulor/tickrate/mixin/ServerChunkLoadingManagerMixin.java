package io.github.dennisochulor.tickrate.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerChunkLoadingManager.class)
public class ServerChunkLoadingManagerMixin {

    @Shadow @Final ServerWorld world;

    @Unique private boolean bl;

    // this is done instead of CHUNK_LOAD/CHUNK_UNLOAD because CHUNK_UNLOAD only fires around chunk level 45 (11 after inaccessible)
    @Inject(method = "onChunkStatusChange", at = @At("TAIL"))
    void onChunkStatusChange(ChunkPos chunkPos, ChunkLevelType levelType, CallbackInfo ci) {
        ServerTickManager tickManager = (ServerTickManager) world.getTickManager();
        // consider chunk LOADED if FULL, BLOCK_TICKING, ENTITY_TICKING
        // consider chunk UNLOADED if INACCESSIBLE
        tickManager.tickRate$updateChunkLoad(world, chunkPos.toLong(), levelType.isAfter(ChunkLevelType.FULL));
    }

    @ModifyVariable(method = "tickEntityMovement", at = @At("STORE"))
    boolean tickEntityMovement$getBl(boolean bl) {
        this.bl = bl;
        return bl;
    }

    @Redirect(method = "tickEntityMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/EntityTrackerEntry;tick()V"))
    void tickEntityMovement$tickEntityTrackerEntry(EntityTrackerEntry entry) {
        Entity entity = entry.tickRate$getEntity();
        if(entity instanceof ProjectileEntity || entity instanceof ItemEntity) {
            ServerTickManager tickManager = (ServerTickManager) world.getTickManager();
            if(this.bl || tickManager.tickRate$shouldTickEntity(entity)) entry.tick();
        }
        else entry.tick();
    }

}
