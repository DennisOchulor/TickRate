package io.github.dennisochulor.tickrate.mixin;

import net.minecraft.network.packet.s2c.play.UpdateTickRateS2CPacket;
import net.minecraft.world.tick.TickManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(UpdateTickRateS2CPacket.class)
public class UpdateTickRateS2CPacketMixin {

    @Redirect(method = "create", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/tick/TickManager;getTickRate()F"))
    private static float create(TickManager instance) {
        return instance.getTickRate();
    }
}
