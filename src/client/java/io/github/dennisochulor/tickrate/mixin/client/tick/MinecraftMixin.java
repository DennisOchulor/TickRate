package io.github.dennisochulor.tickrate.mixin.client.tick;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.dennisochulor.tickrate.TickIndicator;
import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.TickRateManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

	@Shadow public abstract DeltaTracker getDeltaTracker();
	@Shadow public ClientLevel level;
	@Shadow public LocalPlayer player;
	@Shadow @Final public ParticleEngine particleEngine;
	@Shadow @Final public Options options;
	@Shadow protected abstract boolean isLevelRunningNormally();
	@Shadow public abstract void openChatScreen(ChatComponent.ChatMethod method);
	@Shadow private volatile boolean pause;
	@Shadow @Final public LevelRenderer levelRenderer;
	@Shadow @Final public GameRenderer gameRenderer;


	@Redirect(method = "getTickTargetMillis", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/TickRateManager;millisecondsPerTick()F"))
	private float getMillisPerTick(TickRateManager instance) {
		if(TickRateClientManager.serverHasMod()) return TickRateClientManager.getMillisPerServerTick();
		else return instance.millisecondsPerTick();
	}

	@Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V", ordinal = 0))
	private void runTick(boolean tick, CallbackInfo ci) {
		if(!TickRateClientManager.serverHasMod()) return;
		DeltaTracker deltaTracker = getDeltaTracker();
		int playerChunkI = TickRateClientManager.getChunkDeltaTrackerInfo(this.player.chunkPosition()).i();
		for(int i=0; i<10; i++) { // these things need to tick all 10 times
			deltaTracker.tickRate$setMovingI(i);
			this.level.tickEntities();
			this.level.tickBlockEntities();
			this.particleEngine.tick();

			if(this.isLevelRunningNormally() && i < playerChunkI) { // animate according to the player's chunk (not the player themself)
				this.level.animateTick(this.player.getBlockX(), this.player.getBlockY(), this.player.getBlockZ());
			}

			if(!this.pause && i < deltaTracker.tickRate$getI()) { // tick according to server, not the player
				this.level.tickRateManager().tick();
				if(this.level.tickRateManager().runsNormally()) this.levelRenderer.tick(this.gameRenderer.getMainCamera());
				this.level.tick(() -> true);
			}
		}

		while (this.options.keyChat.consumeClick()) { // to allow player to chat even when FROZEN
			this.openChatScreen(ChatComponent.ChatMethod.MESSAGE);
		}
		TickIndicator.tick();
	}

	@ModifyExpressionValue(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/DeltaTracker$Timer;advanceTime(JZ)I"))
	private int modifyPlayerI(int i) { // make the clientTick follow the player's tick rate (which may differ from the server)
		if(!TickRateClientManager.serverHasMod()) return i;
		return TickRateClientManager.getEntityDeltaTrackerInfo(this.player).i();
	}

	@Definition(id = "i", local = @Local(type = int.class))
	@Expression("i > 0")
	@ModifyExpressionValue(method = "runTick", at = @At("MIXINEXTRAS:EXPRESSION"))
	private boolean tickTexturesFollowingPlayerChunkI(boolean original) {
		if (this.player == null) return original;
		else return TickRateClientManager.getChunkDeltaTrackerInfo(this.player.chunkPosition()).i() > 0;
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;tickEntities()V"))
	public void tick$tickEntities(ClientLevel instance) {
		if(!TickRateClientManager.serverHasMod()) instance.tickEntities();
		// otherwise NO-OP
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;tickBlockEntities()V"))
	public void tick$tickBlockEntities(ClientLevel instance) {
		if(!TickRateClientManager.serverHasMod()) instance.tickBlockEntities();
		// otherwise NO-OP
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;tick()V"))
	public void tick$tickParticles(ParticleEngine instance) {
		if(!TickRateClientManager.serverHasMod()) instance.tick();
		// otherwise NO-OP
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;tick(Ljava/util/function/BooleanSupplier;)V"))
	public void tick$tickLevel(ClientLevel instance, BooleanSupplier shouldKeepTicking) {
		if(!TickRateClientManager.serverHasMod()) instance.tick(shouldKeepTicking);
		// otherwise NO-OP
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/TickRateManager;tick()V"))
	public void tick$tickTickManager(TickRateManager instance) {
		if(!TickRateClientManager.serverHasMod()) instance.tick();
		// otherwise NO-OP
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;animateTick(III)V"))
	public void tick$animateTick(ClientLevel instance, int centerX, int centerY, int centerZ) {
		if(!TickRateClientManager.serverHasMod()) instance.animateTick(centerX,centerY,centerZ);
		// otherwise NO-OP
	}

}