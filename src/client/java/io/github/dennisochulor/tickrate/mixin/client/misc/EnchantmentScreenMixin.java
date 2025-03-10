package io.github.dennisochulor.tickrate.mixin.client.misc;

import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnchantmentScreen.class)
public class EnchantmentScreenMixin {

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderTickCounter;getTickDelta(Z)F"))
    public float render(RenderTickCounter instance, boolean ignoreFreeze) {
        return TickRateClientManager.getEntityTickDelta(MinecraftClient.getInstance().player).tickDelta();
    }
}
