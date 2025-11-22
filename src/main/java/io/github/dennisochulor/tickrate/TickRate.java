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
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.FullChunkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;

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

		ServerPlayNetworking.registerGlobalReceiver(TickRateHelloPayload.ID, ((payload, context) -> {
			// No-op for now until there is a use case for it.
		}));

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			server.tickRateManager().tickRate$serverStarting();
			TickRateAPIImpl.init(server);
		});

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			server.tickRateManager().tickRate$serverStarted();
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> TickRateAPIImpl.uninit());

		ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> { // for autosaves and when server stops
			server.tickRateManager().tickRate$saveData();
		});

		// called when entity's chunk level becomes ACCESSIBLE (at least FULL) if it was previously INACCESSIBLE
		ServerEntityEvents.ENTITY_LOAD.register((entity,serverWorld) -> {
			ServerTickRateManager tickManager = (ServerTickRateManager) serverWorld.tickRateManager();
			tickManager.tickRate$updateLoad(entity, true);
		});

		/* During player repsawns, attachments are only transferred when AFTER_RESPAWN is called.
		 *  ENTITY_LOAD is called before AFTER_RESPAWN, so attachment data is not up to date.
		 */
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			ServerTickRateManager tickManager = (ServerTickRateManager) oldPlayer.level().tickRateManager();
			tickManager.tickRate$updateLoad(oldPlayer, true); // use the oldPlayer cause it is not guaranteed the attachment transfer has already happened.
		});

		// called when entity's chunk level becomes INACCESSIBLE
		ServerEntityEvents.ENTITY_UNLOAD.register((entity,serverWorld) -> {
			ServerTickRateManager tickManager = (ServerTickRateManager) serverWorld.tickRateManager();
			tickManager.tickRate$updateLoad(entity, false);
		});

		ServerChunkEvents.CHUNK_LEVEL_TYPE_CHANGE.register((serverWorld, worldChunk, oldLevelType, newLevelType) -> {
			ServerTickRateManager tickManager = (ServerTickRateManager) serverWorld.tickRateManager();
			// consider chunk LOADED if FULL, BLOCK_TICKING, ENTITY_TICKING
			// consider chunk UNLOADED if INACCESSIBLE
			if(oldLevelType == FullChunkStatus.INACCESSIBLE && newLevelType.isOrAfter(FullChunkStatus.FULL)) {
				tickManager.tickRate$updateLoad(worldChunk, true);
			}
			else if(newLevelType == FullChunkStatus.INACCESSIBLE) {
				tickManager.tickRate$updateLoad(worldChunk, false);
			}
		});

		// TickRate dev-only stuff
		if(FabricLoader.getInstance().isDevelopmentEnvironment()) {
			// Testing command
			CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
				dispatcher.register(Commands.literal("tickratetest").then(Commands.argument("entity", EntityArgument.entity()).executes(context -> {
					Test.test(EntityArgument.getEntity(context, "entity"));
					return 1;
				}))
				.then(Commands.literal("server").executes(context -> {
					Test.test(context.getSource().getServer());
					return 1;
				})));
			});

			MixinEnvironment.getCurrentEnvironment().audit(); // also audit the mixins!
		}

	}
}