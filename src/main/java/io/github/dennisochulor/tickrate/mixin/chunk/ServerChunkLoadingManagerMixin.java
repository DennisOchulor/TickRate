package io.github.dennisochulor.tickrate.mixin.chunk;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(ServerChunkLoadingManager.class)
public class ServerChunkLoadingManagerMixin {

    @Shadow @Final ServerWorld world;

    @Unique private boolean bl;

    @ModifyVariable(method = "tickEntityMovement", at = @At("STORE"))
    boolean tickEntityMovement$getBl(boolean bl) {
        this.bl = bl;
        return bl;
    }

    /**
     * This relates to MC-76973
     * Logically, this should apply uniformly to all entities, but for some unknown reason only projectiles/items
     * work properly with this. Other entities will become noticeably less smooth at low TPS. Hence the
     * distinction below is required. Sigh.
     */
    @Redirect(method = "tickEntityMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/EntityTrackerEntry;tick()V"))
    void tickEntityMovement$tickEntityTrackerEntry(EntityTrackerEntry entry) {
        Entity entity = entry.tickRate$getEntity();
        if(entity instanceof ProjectileEntity || entity instanceof ItemEntity) {
            ServerTickManager tickManager = (ServerTickManager) world.getTickManager();
            if(this.bl || tickManager.tickRate$shouldTickEntity(entity)) entry.tick();
        }
        else entry.tick();
    }

    @Redirect(method = "collectSpawningChunks", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private <E> boolean collectSpawningChunks(List<E> list, E e) {
        ServerTickManager tickManager = (ServerTickManager) world.getTickManager();
        WorldChunk chunk = (WorldChunk) e;
        if(tickManager.tickRate$shouldTickChunk(chunk))
            return list.add(e);
        else return false;
    }

}
