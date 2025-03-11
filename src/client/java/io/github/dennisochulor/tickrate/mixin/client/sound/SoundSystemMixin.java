package io.github.dennisochulor.tickrate.mixin.client.sound;

import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(SoundSystem.class)
public class SoundSystemMixin {

    @ModifyConstant(method = "getAdjustedPitch", constant = @Constant(floatValue = 0.5f))
    private float getAdjustedPitchMin(float constant) {
        return 0.25f;
    }

    @ModifyConstant(method = "getAdjustedPitch", constant = @Constant(floatValue = 2.0f))
    private float getAdjustedPitchMax(float constant) {
        return 4.0f;
    }

}
