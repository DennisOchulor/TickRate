package io.github.dennisochulor.tickrate.mixin.client.misc;

import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EnchantmentScreen.class)
public class EnchantmentScreenMixin {

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    public float render(float delta) {
        return TickRateClientManager.getEntityTickDelta(MinecraftClient.getInstance().player).tickDelta();
    }
}
