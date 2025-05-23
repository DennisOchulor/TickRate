package io.github.dennisochulor.tickrate;

import io.github.dennisochulor.tickrate.api_impl.TickRateAPIImpl;
import io.github.dennisochulor.tickrate.test.Test;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.world.ChunkLevelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TickRate implements ModInitializer {

	public static final String MOD_ID = "tickrate";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final float MAX_SOUND_PITCH = 2.0F;
	public static final float MIN_SOUND_PITCH = 0.25F;


	@Override
	public void onInitialize() {
		LOGGER.info("Initializing tickrate!");
		TickRateAttachments.init();
		PayloadTypeRegistry.playS2C().register(TickRateHelloPayload.ID, TickRateHelloPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(TickRateHelloPayload.ID, TickRateHelloPayload.CODEC);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			sender.sendPacket(new TickRateHelloPayload());
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			server.getTickManager().tickRate$removePlayerWithMod(handler.getPlayer());
		});

		ServerPlayNetworking.registerGlobalReceiver(TickRateHelloPayload.ID, ((payload, context) -> {
			ServerTickManager tickManager = context.server().getTickManager();
			tickManager.tickRate$addPlayerWithMod(context.player());
		}));

		ServerLifecycleEvents.SERVER_STARTING.register(TickRateAPIImpl::init);

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			server.getTickManager().tickRate$serverStarted();
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> TickRateAPIImpl.uninit());

		ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> { // for autosaves and when server stops
			server.getTickManager().tickRate$saveData();
		});

		// called when entity's chunk level becomes ACCESSIBLE (at least FULL) if it was previously INACCESSIBLE
		ServerEntityEvents.ENTITY_LOAD.register((entity,serverWorld) -> {
			ServerTickManager tickManager = (ServerTickManager) serverWorld.getTickManager();
			tickManager.tickRate$updateLoad(entity, true);
		});

		/* During player repsawns, attachments are only transferred when AFTER_RESPAWN is called.
		 *  ENTITY_LOAD is called before AFTER_RESPAWN, so attachment data is not up to date.
		 */
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			ServerTickManager tickManager = (ServerTickManager) oldPlayer.getWorld().getTickManager();
			tickManager.tickRate$updateLoad(oldPlayer, true); // use the oldPlayer cause it is not guaranteed the attachment transfer has already happened.
		});

		// called when entity's chunk level becomes INACCESSIBLE
		ServerEntityEvents.ENTITY_UNLOAD.register((entity,serverWorld) -> {
			ServerTickManager tickManager = (ServerTickManager) serverWorld.getTickManager();
			tickManager.tickRate$updateLoad(entity, false);
		});

		ServerChunkEvents.CHUNK_LEVEL_TYPE_CHANGE.register((serverWorld, worldChunk, oldLevelType, newLevelType) -> {
			ServerTickManager tickManager = (ServerTickManager) serverWorld.getTickManager();
			// consider chunk LOADED if FULL, BLOCK_TICKING, ENTITY_TICKING
			// consider chunk UNLOADED if INACCESSIBLE
			if(oldLevelType == ChunkLevelType.INACCESSIBLE && newLevelType.isAfter(ChunkLevelType.FULL)) {
				tickManager.tickRate$updateLoad(worldChunk, true);
			}
			else if(newLevelType == ChunkLevelType.INACCESSIBLE) {
				tickManager.tickRate$updateLoad(worldChunk, false);
			}
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