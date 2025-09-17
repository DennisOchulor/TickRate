package io.github.dennisochulor.tickrate.mixin.client.tick;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.dennisochulor.tickrate.TickIndicator;
import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.tick.TickManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

	@Shadow public abstract RenderTickCounter getRenderTickCounter();
	@Shadow public ClientWorld world;
	@Shadow public ClientPlayerEntity player;
	@Shadow @Final public ParticleManager particleManager;
	@Shadow @Final public GameOptions options;
	@Shadow protected abstract boolean shouldTick();
	@Shadow public abstract void openChatScreen(ChatHud.ChatMethod method);
	@Shadow @Final private TextureManager textureManager;
	@Shadow private volatile boolean paused;
	@Shadow @Final public WorldRenderer worldRenderer;
	@Shadow @Final public GameRenderer gameRenderer;


	@Redirect(method = "getTargetMillisPerTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/tick/TickManager;getMillisPerTick()F"))
	private float getMillisPerTick(TickManager instance) {
		if(TickRateClientManager.serverHasMod()) return TickRateClientManager.getMillisPerServerTick();
		else return instance.getMillisPerTick();
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V", ordinal = 0))
	private void render(boolean tick, CallbackInfo ci) {
		if(!TickRateClientManager.serverHasMod()) return;
		RenderTickCounter renderTickCounter = getRenderTickCounter();
		int playerChunkI = TickRateClientManager.getChunkTickProgress(this.player.getChunkPos()).i();
		for(int i=0; i<10; i++) { // these things need to tick all 10 times
			renderTickCounter.tickRate$setMovingI(i);
			this.world.tickEntities();
			this.particleManager.tick();

			if(this.shouldTick() && i < playerChunkI) { // animate according to the player's chunk (not the player themself)
				this.textureManager.tick();
				this.world.doRandomBlockDisplayTicks(this.player.getBlockX(), this.player.getBlockY(), this.player.getBlockZ());
			}

			if(!this.paused && i < renderTickCounter.tickRate$getI()) { // tick according to server, not the player
				if(this.world.getTickManager().shouldTick()) this.worldRenderer.tick(this.gameRenderer.getCamera());
				this.world.tick(() -> true);
			}
		}

		while (this.options.chatKey.wasPressed()) { // to allow player to chat even when FROZEN
			this.openChatScreen(ChatHud.ChatMethod.MESSAGE);
		}
		TickIndicator.tick();
	}

	@ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderTickCounter$Dynamic;beginRenderTick(JZ)I"))
	private int modifyPlayerI(int i) { // make the clientTick follow the player's tick rate (which may differ from the server)
		if(!TickRateClientManager.serverHasMod()) return i;
		return TickRateClientManager.getEntityTickProgress(this.player).i();
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

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/TextureManager;tick()V"))
	public void tick$tickTextures(TextureManager instance) {
		if(!TickRateClientManager.serverHasMod()) instance.tick();
		// otherwise NO-OP
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;tick(Ljava/util/function/BooleanSupplier;)V"))
	public void tick$tickWorld(ClientWorld instance, BooleanSupplier shouldKeepTicking) {
		if(!TickRateClientManager.serverHasMod()) instance.tick(shouldKeepTicking);
		// otherwise NO-OP
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/tick/TickManager;step()V"))
	public void tick$stepTickManager(TickManager instance) {
		if(!TickRateClientManager.serverHasMod()) instance.step();
		// otherwise NO-OP
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;doRandomBlockDisplayTicks(III)V"))
	public void tick$randomBlockDisplayTicks(ClientWorld instance, int centerX, int centerY, int centerZ) {
		if(!TickRateClientManager.serverHasMod()) instance.doRandomBlockDisplayTicks(centerX,centerY,centerZ);
		// otherwise NO-OP
	}

}