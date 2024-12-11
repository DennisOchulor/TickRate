package io.github.dennisochulor.tickrate;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;

public class TickRateClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		TickRate.LOGGER.info("Initializing tickrate client!");

		ClientPlayNetworking.registerGlobalReceiver(TickRateHelloPayload.ID, (payload, context) -> {
			context.responseSender().sendPacket(new TickRateHelloPayload());
		});

		ClientPlayNetworking.registerGlobalReceiver(TickRateS2CUpdatePayload.ID, (payload, context) -> {
			TickRateClientManager.update(payload);
			TickRateClientManager.setServerHasMod(true); // it's here and not at Hello to ensure TickRateClientManager#server is not null
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			TickRateClientManager.setServerHasMod(false);
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("tick_indicator")
				.executes(context -> {
					if(TickIndicator.toggle()) context.getSource().sendFeedback(Text.literal("Tick indicator toggled on."));
					else context.getSource().sendFeedback(Text.literal("Tick indicator toggled off."));
					return 0;
				})
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if(!TickIndicator.isEnabled() || !TickRateClientManager.serverHasMod()) return;
			int chunkRate = (int) TickRateClientManager.getChunkState(client.player.clientWorld, client.player.getChunkPos().toLong()).rate();
			int entityRate = (int) (client.targetedEntity!=null ? TickRateClientManager.getEntityState(client.targetedEntity).rate() : 0);
			client.player.sendMessage(Text.literal("Entity: " + entityRate + " TPS       Chunk: " + chunkRate + " TPS"), true);
		});

	}
}