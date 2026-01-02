package io.github.dennisochulor.tickrate;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;

public class TickRateClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		TickRate.LOGGER.info("Initializing tickrate client!");

		ClientPlayNetworking.registerGlobalReceiver(TickRateHelloPayload.ID, (_, context) -> {
			context.responseSender().sendPacket(new TickRateHelloPayload());
			TickRateClientManager.setServerHasMod(true);
		});

		ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> {
			TickRateClientManager.setServerHasMod(false);
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, _) -> dispatcher.register(ClientCommands.literal("tick_indicator")
				.executes(context -> {
					if(TickIndicator.toggle()) context.getSource().sendFeedback(Component.literal("Tick indicator toggled on."));
					else context.getSource().sendFeedback(Component.literal("Tick indicator toggled off."));
					return 0;
				})
		));
	}
}