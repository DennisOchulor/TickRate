package io.github.dennisochulor.tickrate;

import io.github.dennisochulor.tickrate.api_impl.TickRateAPIImpl;
import io.github.dennisochulor.tickrate.test.Test;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.command.CommandManager;
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
			server.getTickManager().tickRate$removePlayerWithMod(handler.getPlayer());
		});

		ServerPlayNetworking.registerGlobalReceiver(TickRateHelloPayload.ID, ((payload, context) -> {
			LOGGER.debug("received TickRate Hello");
			ServerTickManager tickManager = context.server().getTickManager();
			tickManager.tickRate$addPlayerWithMod(context.player());
			tickManager.tickRate$sendUpdatePacket();
		}));

		ServerLifecycleEvents.SERVER_STARTING.register(server -> server.getTickManager().tickRate$serverStarted());

		ServerLifecycleEvents.SERVER_STARTED.register(TickRateAPIImpl::init);

		ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> { // for autosaves and when server stops
			server.getTickManager().tickRate$saveData();
		});

		// called when entity's chunk level becomes ACCESSIBLE (at least FULL) if it was previously INACCESSIBLE
		ServerEntityEvents.ENTITY_LOAD.register((entity,serverWorld) -> {
			ServerTickManager tickManager = (ServerTickManager) serverWorld.getTickManager();
			tickManager.tickRate$updateEntityLoad(entity,true);
		});

		// called when entity's chunk level becomes INACCESSIBLE
		ServerEntityEvents.ENTITY_UNLOAD.register((entity,serverWorld) -> {
			ServerTickManager tickManager = (ServerTickManager) serverWorld.getTickManager();
			tickManager.tickRate$updateEntityLoad(entity,false);
		});

		// TickRate testing command
		if(FabricLoader.getInstance().isDevelopmentEnvironment()) {
			CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
				dispatcher.register(CommandManager.literal("tickratetest").then(CommandManager.argument("entity", EntityArgumentType.entity()).executes(context -> {
					Test.test(EntityArgumentType.getEntity(context, "entity"));
					return 1;
				}))
				.then(CommandManager.literal("server").executes(context -> {
					Test.test(context.getSource().getServer());
					return 1;
				})));
			});
		}

	}


}