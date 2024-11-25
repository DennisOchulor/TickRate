package io.github.dennisochulor.tickrate.mixin.client;

import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(StatusEffectUtil.class)
public class StatusEffectUtilMixin {

    @ModifyArg(method = "getDurationText", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/StringHelper;formatTicks(IF)Ljava/lang/String;"))
    private static float getDurationText$tickDelta(float tickDelta) {
        if(!TickRateClientManager.serverHasMod()) return tickDelta;
        else return TickRateClientManager.getEntityState(MinecraftClient.getInstance().player).rate();
    }

}
