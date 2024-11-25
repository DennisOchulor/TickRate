package io.github.dennisochulor.tickrate.mixin.client;

import io.github.dennisochulor.tickrate.TickRateClientManager;
import io.github.dennisochulor.tickrate.TickRateRenderTickCounter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.tick.TickManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

	@Shadow public abstract RenderTickCounter getRenderTickCounter();
	@Shadow public ClientWorld world;
	@Shadow public ClientPlayerEntity player;

	@Shadow @Final public ParticleManager particleManager;

	@Redirect(method = "getTargetMillisPerTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/tick/TickManager;getMillisPerTick()F"))
	private float getMillisPerTick(TickManager instance) {
		if(TickRateClientManager.serverHasMod()) return TickRateClientManager.getMillisPerServerTick();
		else return instance.getMillisPerTick();
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V", ordinal = 1, shift = At.Shift.BEFORE))
	private void render(boolean tick, CallbackInfo ci) {
		if(!TickRateClientManager.serverHasMod()) return;
		TickRateRenderTickCounter renderTickCounter = (TickRateRenderTickCounter) getRenderTickCounter();
		for(int i=0; i<10; i++) { // these things need to tick all 10 times
			renderTickCounter.tickRate$setMovingI(i);
			this.world.tickEntities();
			this.particleManager.tick();
		}
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;tickEntities()V"))
	public void tick$tickEntities(ClientWorld instance) {
		if(!TickRateClientManager.serverHasMod()) instance.tickEntities();
		// otherwise NO-OP
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleManager;tick()V"))
	public void tick$tickParticles(ParticleManager instance) {
		if(!TickRateClientManager.serverHasMod()) instance.tick();
		// otherwise NO-OP
	}

	@ModifyVariable(method = "render", at = @At(value = "STORE"), ordinal = 0)
	private int render(int i) {
		if(!TickRateClientManager.serverHasMod()) return i;
		return TickRateClientManager.getEntityTickDelta(getRenderTickCounter().getTickDelta(false), this.player).i();
	}

}