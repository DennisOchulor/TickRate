package io.github.dennisochulor.tickrate.mixin.client.sound;

import io.github.dennisochulor.tickrate.TickRate;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(SoundSystem.class)
public class SoundSystemMixin {

    @ModifyConstant(method = "getAdjustedPitch", constant = @Constant(floatValue = 0.5f))
    private float getAdjustedPitchMin(float constant) {
        return TickRate.MIN_SOUND_PITCH;
    }

    @ModifyConstant(method = "getAdjustedPitch", constant = @Constant(floatValue = 2.0f))
    private float getAdjustedPitchMax(float constant) {
        return TickRate.MAX_SOUND_PITCH;
    }

}
