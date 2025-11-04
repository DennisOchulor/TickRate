package io.github.dennisochulor.tickrate.mixin.core;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.ServerTickRateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

	@Shadow public abstract ServerTickRateManager tickRateManager();

	@Inject(method = "runServer", at= @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/jfr/JvmProfiler;onServerTick(F)V", shift = At.Shift.AFTER, args = "ldc="))
	private void runServer$tickTail(CallbackInfo ci) {
		tickRateManager().tickRate$ticked();
	}

	@Redirect(method = "tickChildren", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerFunctionManager;tick()V"))
	protected void tickChildren$CommandTick(ServerFunctionManager instance) {
		if(tickRateManager().tickRate$shouldTickServer()) instance.tick();
	}

	@ModifyConstant(method = "tickServer", constant = @Constant(intValue = 20))
	private int tick$pauseWhenEmptySeconds(int constant) {
		return (int) this.tickRateManager().tickrate(); // mainloop rate
	}

	@Redirect(method = "runServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerTickRateManager;checkShouldSprintThisTick()Z"))
	private boolean runServer$sprint(ServerTickRateManager instance) {
		if(instance.tickRate$isServerSprint()) return instance.checkShouldSprintThisTick();
		return instance.tickRate$isIndividualSprint();
	}

}