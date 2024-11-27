package io.github.dennisochulor.tickrate.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ServerLoginNetworkHandler.class)
public class ServerLoginNetworkHandlerMixin {

    @Shadow @Final MinecraftServer server;

    @ModifyConstant(method = "tick", constant = @Constant(intValue = 600))
    public int tick$tookTooLongToLogin(int constant) {
        // 30 seconds
        ServerTickManager tickManager = server.getTickManager();
        return (int) tickManager.getTickRate() * 30;
    }

}
