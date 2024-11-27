package io.github.dennisochulor.tickrate.mixin;

import io.github.dennisochulor.tickrate.TickRateTickManager;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public abstract MinecraftServer getServer();

    @Inject(method = "onRemove", at = @At("TAIL"))
    public void onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity entity = (Entity)(Object)this;
        if(entity.getWorld().isClient) return;

        TickRateTickManager tickManager = (TickRateTickManager) this.getServer().getTickManager();
        if(reason.shouldDestroy()) tickManager.tickRate$removeEntity(entity,true,true,true);
        else if(reason == Entity.RemovalReason.UNLOADED_TO_CHUNK || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER)
            tickManager.tickRate$removeEntity(entity,false,false,true);
    }

}
