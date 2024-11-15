package io.github.dennisochulor.tickrate.mixin;

import io.github.dennisochulor.tickrate.TickRateTickManager;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public abstract MinecraftServer getServer();

    @Inject(method = "onRemove", at = @At("TAIL"))
    public void onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        TickRateTickManager tickManager = (TickRateTickManager) this.getServer().getTickManager();
        if(reason.shouldDestroy()) tickManager.tickRate$setEntityRate(0.0f, List.of((Entity)(Object)this));
    }
}
