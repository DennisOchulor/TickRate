package io.github.dennisochulor.tickrate;

import io.github.dennisochulor.tickrate.TickRateTickManager;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TickRate implements ModInitializer {

	public static final String MOD_ID = "tickrate";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing tickrate!");
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			TickRateTickManager mixin = (TickRateTickManager) server.getTickManager();
			mixin.tickRate$serverStarted();
			ses.schedule(() -> {
				server.getWorld(World.OVERWORLD).iterateEntities().forEach(entity -> {
					if(entity.getType() == EntityType.BLAZE) {
						ses.scheduleWithFixedDelay(() -> server.execute(() -> {
							entity.tick();
						}), 10, 10, TimeUnit.MILLISECONDS);
					}
				});
			}, 15, TimeUnit.SECONDS);
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			TickRateTickManager mixin = (TickRateTickManager) server.getTickManager();
			mixin.tickRate$serverStopped();
		});
	}
}