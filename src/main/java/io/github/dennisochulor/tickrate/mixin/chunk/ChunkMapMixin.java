package io.github.dennisochulor.tickrate.mixin.chunk;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
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

    /**
     * This relates to MC-76973
     * Logically, this should apply uniformly to all entities, but for some unknown reason only projectiles/items
     * work properly with this. Other entities will become noticeably less smooth at low TPS. Hence the
     * distinction below is required. Sigh.
     */
    @WrapOperation(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerEntity;sendChanges()V"))
    void tick$tickServerEntity(ServerEntity serverEntity, Operation<Void> original) {
        Entity entity = serverEntity.tickRate$getEntity();
        if(entity instanceof Projectile || entity instanceof ItemEntity) {
            ServerTickRateManager tickManager = (ServerTickRateManager) level.tickRateManager();
            if(tickManager.tickRate$shouldTickEntity(entity)) original.call(serverEntity);
        }
        else original.call(serverEntity);
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
