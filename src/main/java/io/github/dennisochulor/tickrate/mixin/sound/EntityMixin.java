package io.github.dennisochulor.tickrate.mixin.sound;

import io.github.dennisochulor.tickrate.TickRate;
import io.github.dennisochulor.tickrate.TickState;
import net.minecraft.entity.Entity;
import net.minecraft.server.ServerTickManager;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public class EntityMixin {

    @Shadow private World world;

    @ModifyVariable(method = "playSound", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    public float playSound(float pitch) {
        if(world.isClient()) return pitch;

        Entity entity = (Entity) (Object) this;
        ServerTickManager tickManager = entity.getServer().getTickManager();
        TickState state = tickManager.tickRate$getEntityTickStateDeep(entity);
        if(state.sprinting()) return TickRate.MAX_SOUND_PITCH;
        else return pitch * (state.rate() / 20.0F);
    }

}
