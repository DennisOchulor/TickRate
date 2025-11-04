package io.github.dennisochulor.tickrate.mixin.chunk;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {

    @Shadow @Final ServerLevel level;

    @Unique private boolean bl;

    @ModifyVariable(method = "tick()V", at = @At("STORE"))
    boolean tick$getBl(boolean bl) {
        this.bl = bl;
        return bl;
    }

    /**
     * This relates to MC-76973
     * Logically, this should apply uniformly to all entities, but for some unknown reason only projectiles/items
     * work properly with this. Other entities will become noticeably less smooth at low TPS. Hence the
     * distinction below is required. Sigh.
     */
    @Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerEntity;sendChanges()V"))
    void tick$tickServerEntity(ServerEntity serverEntity) {
        Entity entity = serverEntity.tickRate$getEntity();
        if(entity instanceof Projectile || entity instanceof ItemEntity) {
            ServerTickRateManager tickManager = (ServerTickRateManager) level.tickRateManager();
            if(this.bl || tickManager.tickRate$shouldTickEntity(entity)) serverEntity.sendChanges();
        }
        else serverEntity.sendChanges();
    }

    @Redirect(method = "collectSpawningChunks", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private <E> boolean collectSpawningChunks(List<E> list, E e) {
        ServerTickRateManager tickManager = (ServerTickRateManager) level.tickRateManager();
        LevelChunk chunk = (LevelChunk) e;
        if(tickManager.tickRate$shouldTickChunk(chunk))
            return list.add(e);
        else return false;
    }

}
