package io.github.dennisochulor.tickrate.mixin.chunk;

import io.github.dennisochulor.tickrate.injected_interface.TickRateEntityTrackerEntry;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.EntityTrackerEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityTrackerEntry.class)
public class EntityTrackerEntryMixin implements TickRateEntityTrackerEntry {

    @Shadow @Final private Entity entity;

    public Entity tickRate$getEntity() {
        return entity;
    }

}
