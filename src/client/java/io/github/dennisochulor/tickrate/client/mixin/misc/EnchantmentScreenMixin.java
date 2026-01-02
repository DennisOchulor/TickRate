package io.github.dennisochulor.tickrate.client.mixin.misc;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.dennisochulor.tickrate.client.TickRateClientManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Objects;

@Mixin(EnchantmentScreen.class)
public class EnchantmentScreenMixin {

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/DeltaTracker;getGameTimeDeltaPartialTick(Z)F"))
    public float render(DeltaTracker instance, boolean ignoreFreeze) {
        return TickRateClientManager.getEntityDeltaTrackerInfo(Objects.requireNonNull(Minecraft.getInstance().player)).partialTick();
    }

    @ModifyExpressionValue(method = "renderBook", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/DeltaTracker;getGameTimeDeltaPartialTick(Z)F"))
    private float renderBook(float original) {
        return TickRateClientManager.getEntityDeltaTrackerInfo(Objects.requireNonNull(Minecraft.getInstance().player)).partialTick();
    }

}
