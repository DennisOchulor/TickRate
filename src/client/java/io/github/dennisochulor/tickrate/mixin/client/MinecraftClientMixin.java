package io.github.dennisochulor.tickrate.mixin.client;

import io.github.dennisochulor.tickrate.TickRateClientManager;
import io.github.dennisochulor.tickrate.TickRateRenderTickCounter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.tick.TickManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

	@Shadow public abstract RenderTickCounter getRenderTickCounter();
	@Shadow public ClientWorld world;

	@Shadow @Final public GameRenderer gameRenderer;

	@Shadow @Nullable public ClientPlayerEntity player;

	@Redirect(method = "getTargetMillisPerTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/tick/TickManager;getMillisPerTick()F"))
	private float getMillisPerTick(TickManager instance) {
		if(TickRateClientManager.serverHasMod()) return TickRateClientManager.getMillisPerServerTick();
		else return instance.getMillisPerTick();
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V", ordinal = 1, shift = At.Shift.BEFORE))
	private void render(boolean tick, CallbackInfo ci) {
		if(!TickRateClientManager.serverHasMod()) return;
		TickRateRenderTickCounter renderTickCounter = (TickRateRenderTickCounter) getRenderTickCounter();
		for(int i=0; i<10; i++) {
			renderTickCounter.tickRate$setMovingI(i);
			this.world.tickEntities();
			if(i < TickRateClientManager.getEntityTickDelta(MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false), this.player).i()) {
				this.gameRenderer.tick(); // only affects the player
			}
		}
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;tickEntities()V"))
	public void tick(ClientWorld instance) {
		if(!TickRateClientManager.serverHasMod()) instance.tickEntities();
		// otherwise NO-OP
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;tick()V"))
	public void tick(GameRenderer instance) {
		if(!TickRateClientManager.serverHasMod()) instance.tick();
		// otherwise NO-OP
	}

}