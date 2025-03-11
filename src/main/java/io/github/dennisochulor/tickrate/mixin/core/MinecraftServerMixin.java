package io.github.dennisochulor.tickrate.mixin.core;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.function.CommandFunctionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

	@Shadow public abstract ServerTickManager getTickManager();

	@Inject(method = "runServer", at= @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/jfr/FlightProfiler;onTick(F)V", shift = At.Shift.AFTER, args = "ldc="))
	private void runServer$tickTail(CallbackInfo ci) {
		getTickManager().tickRate$ticked();
	}

	@Redirect(method = "tickWorlds", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/function/CommandFunctionManager;tick()V"))
	protected void tickWorlds$CommandTick(CommandFunctionManager instance) {
		if(getTickManager().tickRate$shouldTickServer()) instance.tick();
	}

	@Redirect(method = "runServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerTickManager;sprint()Z"))
	private boolean runServer$sprint(ServerTickManager instance) {
		if(instance.tickRate$isServerSprint()) return instance.sprint();
		return instance.tickRate$isIndividualSprint();
	}

}