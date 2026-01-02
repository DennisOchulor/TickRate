package io.github.dennisochulor.tickrate.mixin.networking;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ServerLoginPacketListenerImpl.class)
public class ServerLoginPacketListenerImplMixin {

    @Shadow @Final private MinecraftServer server;

    @ModifyConstant(method = "tick", constant = @Constant(intValue = 600))
    public int tick$tookTooLongToLogin(int constant) {
        // 30 seconds
        return (int) server.tickRateManager().tickrate() * 30;
    }

}
