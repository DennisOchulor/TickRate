package io.github.dennisochulor.tickrate;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class TickRateClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		TickRate.LOGGER.info("Initializing tickrate client!");

		ClientPlayNetworking.registerGlobalReceiver(TickRateHelloPayload.ID, (payload, context) -> {
			context.responseSender().sendPacket(new TickRateHelloPayload());
		});

		ClientPlayNetworking.registerGlobalReceiver(TickRateS2CUpdatePayload.ID, (payload, context) -> {

		});
	}
}