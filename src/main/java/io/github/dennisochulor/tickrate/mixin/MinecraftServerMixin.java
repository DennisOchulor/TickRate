package io.github.dennisochulor.tickrate.mixin;

import io.github.dennisochulor.tickrate.TickRateTickManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.function.CommandFunctionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

	@Shadow public abstract ServerTickManager getTickManager();

	@Inject(method = "runServer", at= @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/jfr/FlightProfiler;onTick(F)V", shift = At.Shift.AFTER, args = "ldc="))
	private void runServer$tickTail(CallbackInfo ci) {
		TickRateTickManager tickManager = (TickRateTickManager) this.getTickManager();
		tickManager.tickRate$ticked();
	}

	@Redirect(method = "tickWorlds", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/function/CommandFunctionManager;tick()V"))
	protected void tickWorlds$CommandTick(CommandFunctionManager instance) {
		TickRateTickManager tickManager = (TickRateTickManager) this.getTickManager();
		if(tickManager.tickRate$shouldTickServer()) instance.tick();
	}



}