package io.github.dennisochulor.tickrate;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TickRate implements ModInitializer {

	public static final String MOD_ID = "tickrate";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing tickrate!");
	}
}