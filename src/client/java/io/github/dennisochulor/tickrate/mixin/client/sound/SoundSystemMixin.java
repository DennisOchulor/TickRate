package io.github.dennisochulor.tickrate.mixin.client.sound;

import io.github.dennisochulor.tickrate.TickRate;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(SoundEngine.class)
public class SoundSystemMixin {

    @ModifyConstant(method = "calculatePitch", constant = @Constant(floatValue = 0.5f))
    private float calculatePitchMin(float constant) {
        return TickRate.MIN_SOUND_PITCH;
    }

    @ModifyConstant(method = "calculatePitch", constant = @Constant(floatValue = 2.0f))
    private float calculatePitchMax(float constant) {
        return TickRate.MAX_SOUND_PITCH;
    }

}
