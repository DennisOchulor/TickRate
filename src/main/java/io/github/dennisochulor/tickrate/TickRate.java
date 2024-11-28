package io.github.dennisochulor.tickrate;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TickRate implements ModInitializer {

	public static final String MOD_ID = "tickrate";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing tickrate!");
		PayloadTypeRegistry.playS2C().register(TickRateHelloPayload.ID, TickRateHelloPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(TickRateHelloPayload.ID, TickRateHelloPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(TickRateS2CUpdatePayload.ID, TickRateS2CUpdatePayload.CODEC);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			LOGGER.debug("send tickrate Hello");
			sender.sendPacket(new TickRateHelloPayload());
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			LOGGER.debug("disconnect tickrate");
			TickRateTickManager mixin = (TickRateTickManager) server.getTickManager();
			mixin.tickRate$removePlayerWithMod(handler.getPlayer());
		});

		ServerPlayNetworking.registerGlobalReceiver(TickRateHelloPayload.ID, ((payload, context) -> {
			LOGGER.debug("received TickRate Hello");
			TickRateTickManager mixin = (TickRateTickManager) context.server().getTickManager();
			mixin.tickRate$addPlayerWithMod(context.player());
			mixin.tickRate$sendUpdatePacket();
		}));

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			TickRateTickManager mixin = (TickRateTickManager) server.getTickManager();
			mixin.tickRate$serverStarted();
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			TickRateTickManager mixin = (TickRateTickManager) server.getTickManager();
			mixin.tickRate$serverStopped();
		});

		ServerChunkEvents.CHUNK_LOAD.register((serverWorld,chunk) -> {
			TickRateTickManager mixin = (TickRateTickManager) serverWorld.getTickManager();
			mixin.tickRate$updateChunkLoad(serverWorld,chunk.getPos().toLong(),true);
		});

		ServerChunkEvents.CHUNK_UNLOAD.register((serverWorld,chunk) -> {
			TickRateTickManager mixin = (TickRateTickManager) serverWorld.getTickManager();
			mixin.tickRate$updateChunkLoad(serverWorld,chunk.getPos().toLong(),false);
		});

		ServerEntityEvents.ENTITY_LOAD.register((entity,serverWorld) -> {
			TickRateTickManager mixin = (TickRateTickManager) serverWorld.getTickManager();
			mixin.tickRate$updateEntityLoad(entity,true);
		});

		ServerEntityEvents.ENTITY_UNLOAD.register((entity,serverWorld) -> {
			TickRateTickManager mixin = (TickRateTickManager) serverWorld.getTickManager();
			mixin.tickRate$updateEntityLoad(entity,false);
		});

	}


}