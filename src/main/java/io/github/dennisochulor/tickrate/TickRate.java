package io.github.dennisochulor.tickrate;

import io.github.dennisochulor.tickrate.mixin.TickRateTickManager;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TickRate implements ModInitializer {

	public static final String MOD_ID = "tickrate";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing tickrate!");
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			TickRateTickManager mixin = (TickRateTickManager) server.getTickManager();
			mixin.tickRate$serverStarted();
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			TickRateTickManager mixin = (TickRateTickManager) server.getTickManager();
			mixin.tickRate$serverStopped();
		});
	}
}