package io.github.dennisochulor.tickrate.mixin.sound;

import io.github.dennisochulor.tickrate.TickRate;
import io.github.dennisochulor.tickrate.TickState;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public class EntityMixin {

    @Shadow private Level level;

    @ModifyVariable(method = "playSound(Lnet/minecraft/sounds/SoundEvent;FF)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    public float playSound(float pitch) {
        if(level instanceof ServerLevel serverLevel) {
            Entity entity = (Entity) (Object) this;
            ServerTickRateManager tickManager = serverLevel.getServer().tickRateManager();
            TickState state = tickManager.tickRate$getEntityTickStateDeep(entity);
            if(state.sprinting()) return TickRate.MAX_SOUND_PITCH;
            else return pitch * (state.rate() / 20.0F);
        }
        else return pitch;
    }

}
