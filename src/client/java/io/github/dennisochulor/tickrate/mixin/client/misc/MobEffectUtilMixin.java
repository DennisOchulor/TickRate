package io.github.dennisochulor.tickrate.mixin.client.misc;

import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Objects;

@Mixin(MobEffectUtil.class)
public abstract class MobEffectUtilMixin {

    @ModifyArg(method = "formatDuration", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/StringUtil;formatTickDuration(IF)Ljava/lang/String;"))
    private static float formatDuration$tickRate(float tickRate) {
        if(!TickRateClientManager.serverHasMod()) return tickRate;
        else return TickRateClientManager.getEntityState(Objects.requireNonNull(Minecraft.getInstance().player)).rate();
    }

}
