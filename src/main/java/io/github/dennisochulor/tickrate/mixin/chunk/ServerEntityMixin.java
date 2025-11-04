package io.github.dennisochulor.tickrate.mixin.chunk;

import io.github.dennisochulor.tickrate.injected_interface.TickRateServerEntity;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerEntity.class)
public class ServerEntityMixin implements TickRateServerEntity {

    @Shadow @Final private Entity entity;

    public Entity tickRate$getEntity() {
        return entity;
    }

}
