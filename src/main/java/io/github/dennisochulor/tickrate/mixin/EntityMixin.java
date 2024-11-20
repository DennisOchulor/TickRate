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
        Entity entity = (Entity)(Object)this;
        TickRateTickManager tickManager = (TickRateTickManager) this.getServer().getTickManager();
        if(reason.shouldDestroy()) tickManager.tickRate$removeEntity(entity,true,true,true);
        else if(reason == Entity.RemovalReason.UNLOADED_TO_CHUNK || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER)
            tickManager.tickRate$removeEntity(entity,false,false,true);
    }

    public void tickiii(CallbackInfo ci) {
        Entity e = (Entity) (Object) this;
        if(e.isPlayer()) {
            var a = Thread.currentThread().getStackTrace();
            for(int i=0;i<10;i++){
                System.out.println(a[i]);
            }
        }
    }



}
