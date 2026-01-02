package io.github.dennisochulor.tickrate.client.mixin.tick;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.dennisochulor.tickrate.client.TickIndicator;
import io.github.dennisochulor.tickrate.client.TickRateClientManager;
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
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.function.BooleanSupplier;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

	@Shadow public abstract DeltaTracker getDeltaTracker();
	@Shadow @Nullable public ClientLevel level;
	@Shadow @Nullable public LocalPlayer player;
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
		Objects.requireNonNull(level);
		Objects.requireNonNull(player);

		DeltaTracker deltaTracker = getDeltaTracker();
		int playerChunkTicksToDo = TickRateClientManager.getChunkDeltaTrackerInfo(this.player.chunkPosition()).ticksToDo();
		for(int ticksToDo = 0; ticksToDo < 10; ticksToDo++) { // these things need to tick all 10 times
			deltaTracker.tickRate$setMovingTicksToDo(ticksToDo);
			this.level.tickEntities();
			this.level.tickBlockEntities();
			this.particleEngine.tick();

			if(this.isLevelRunningNormally() && ticksToDo < playerChunkTicksToDo) { // animate according to the player's chunk (not the player themself)
				this.level.animateTick(this.player.getBlockX(), this.player.getBlockY(), this.player.getBlockZ());
			}

			if(!this.pause && ticksToDo < deltaTracker.tickRate$getTicksToDo()) { // tick according to server, not the player
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
	private int modifyPlayerTicksToDo(int i) { // make the clientTick follow the player's tick rate (which may differ from the server)
		if(!TickRateClientManager.serverHasMod()) return i;
		return TickRateClientManager.getEntityDeltaTrackerInfo(Objects.requireNonNull(this.player)).ticksToDo();
	}

	@Definition(id = "ticksToDo", local = @Local(type = int.class))
	@Expression("ticksToDo > 0")
	@ModifyExpressionValue(method = "runTick", at = @At("MIXINEXTRAS:EXPRESSION"))
	private boolean tickTexturesFollowingPlayerChunk(boolean original) {
		if (this.player == null) return original;
		else return TickRateClientManager.getChunkDeltaTrackerInfo(this.player.chunkPosition()).ticksToDo() > 0;
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