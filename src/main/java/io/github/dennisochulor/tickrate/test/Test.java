package io.github.dennisochulor.tickrate.test;

import io.github.dennisochulor.tickrate.api.TickRateAPI;
import io.github.dennisochulor.tickrate.api.TickRateEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

import static io.github.dennisochulor.tickrate.TickRate.LOGGER;

/**
 * Set rate, then query it
 * Freeze, then step 5 secs
 * Step again and step stop
 * Sprint for 5 secs, then sprint stop
 * Unfreeze, then reset rate
 */
public final class Test {
    private Test() {}

    private static boolean registered = false;

    public static void test(MinecraftServer server) {
        register();

        Thread.ofVirtual().name("TickRateTest Thread").start(() -> {
            TickRateAPI api = TickRateAPI.getInstance();
            CommandSourceStack src = server.createCommandSourceStack();
            Commands commander = server.getCommands();

            LOGGER.info("STARTING TICKRATE **API** SERVER TEST");
            sleep(5);

            api.rateServer(10);
            LOGGER.info("{} TPS", api.queryServer());
            sleep(2);
            api.freezeServer(true);
            sleep(2);

            api.stepServer(50);
            sleep(7);
            api.stepServer(10000);
            sleep(2);
            api.stepServer(0);
            sleep(2);

            api.sprintServer(1000000);
            sleep(5);
            api.sprintServer(0);
            sleep(2);
            api.freezeServer(false);
            sleep(2);
            api.rateServer(20);
            sleep(2);
            LOGGER.info("FINISH TICKRATE **API** SERVER TEST");

            sleep(2);

            LOGGER.info("STARTING TICKRATE **COMMAND** SERVER TEST");
            sleep(5);

            commander.performPrefixedCommand(src, "tick rate 10");
            commander.performPrefixedCommand(src, "tick query");
            sleep(2);
            commander.performPrefixedCommand(src, "tick freeze");
            sleep(2);

            commander.performPrefixedCommand(src, "tick step 50");
            sleep(7);
            commander.performPrefixedCommand(src, "tick step 10000");
            sleep(2);
            commander.performPrefixedCommand(src, "tick step stop");
            sleep(2);

            commander.performPrefixedCommand(src, "tick sprint 1000000");
            sleep(5);
            commander.performPrefixedCommand(src, "tick sprint stop");
            sleep(2);
            commander.performPrefixedCommand(src, "tick unfreeze");
            sleep(2);
            commander.performPrefixedCommand(src, "tick rate 20");
            sleep(2);
            LOGGER.info("FINISH TICKRATE **COMMAND** SERVER TEST");
        });
    }

    public static void test(Entity testEntity) {
        register();

        Thread.ofVirtual().name("TickRateTest Thread").start(() -> {
            TickRateAPI api = TickRateAPI.getInstance();
            String uuid = testEntity.getStringUUID();
            ServerLevel level = (ServerLevel) testEntity.level();
            CommandSourceStack src = level.getServer().createCommandSourceStack();
            Commands commander = level.getServer().getCommands();
            ChunkPos chunkPos = testEntity.chunkPosition();
            String strChunkPos = chunkPos.getMiddleBlockX() + " " + chunkPos.getMiddleBlockZ();

            LOGGER.info("STARTING TICKRATE **API** TEST");
            sleep(5);

            LOGGER.info("ENTITY TEST");
            api.rateEntity(testEntity, 10);
            LOGGER.info("{} TPS", api.queryEntity(testEntity));
            sleep(2);
            api.freezeEntity(testEntity, true);
            sleep(2);

            api.stepEntity(testEntity, 50);
            sleep(7);
            api.stepEntity(testEntity, 10000);
            sleep(2);
            api.stepEntity(testEntity, 0);
            sleep(2);

            api.sprintEntity(testEntity, 1000000);
            sleep(5);
            api.sprintEntity(testEntity, 0);
            sleep(2);
            api.freezeEntity(testEntity, false);
            sleep(2);
            api.rateEntity(testEntity, 0.0f);
            sleep(5);

            LOGGER.info("CHUNK TESTS");
            api.rateChunk(level, chunkPos, 50);
            LOGGER.info("{} TPS", api.queryChunk(level, chunkPos));
            sleep(2);
            api.freezeChunk(level, chunkPos, true);
            sleep(2);

            api.stepChunk(level, chunkPos, 250);
            sleep(7);
            api.stepChunk(level, chunkPos, 10000);
            sleep(2);
            api.stepChunk(level, chunkPos, 0);
            sleep(2);

            api.sprintChunk(level, chunkPos, 1000000);
            sleep(5);
            api.sprintChunk(level, chunkPos, 0);
            sleep(2);
            api.freezeChunk(level, chunkPos, false);
            sleep(2);
            api.rateChunk(level, chunkPos, 0.0f);
            sleep(2);
            LOGGER.info("FINISH TICKRATE **API** TEST");

            sleep(2);

            LOGGER.info("STARTING TICKRATE **COMMAND** TEST");
            sleep(5);

            LOGGER.info("ENTITY TEST");
            commander.performPrefixedCommand(src, "tick entity " + uuid + " rate 10");
            commander.performPrefixedCommand(src, "tick entity " + uuid + " query");
            sleep(2);
            commander.performPrefixedCommand(src, "tick entity " + uuid + " freeze");
            sleep(2);

            commander.performPrefixedCommand(src, "tick entity " + uuid + " step 50");
            sleep(7);
            commander.performPrefixedCommand(src, "tick entity " + uuid + " step 10000");
            sleep(2);
            commander.performPrefixedCommand(src, "tick entity " + uuid + " step stop");
            sleep(2);

            commander.performPrefixedCommand(src, "tick entity " + uuid + " sprint 1000000");
            sleep(5);
            commander.performPrefixedCommand(src, "tick entity " + uuid + " sprint stop");
            sleep(2);
            commander.performPrefixedCommand(src, "tick entity " + uuid + " unfreeze");
            sleep(2);
            commander.performPrefixedCommand(src, "tick entity " + uuid + " rate reset");
            sleep(5);

            LOGGER.info("CHUNK TESTS");
            commander.performPrefixedCommand(src, "tick chunk " + strChunkPos + " rate 50");
            commander.performPrefixedCommand(src, "tick chunk " + strChunkPos + " query");
            sleep(2);
            commander.performPrefixedCommand(src, "tick chunk " + strChunkPos + " freeze");
            sleep(2);

            commander.performPrefixedCommand(src, "tick chunk " + strChunkPos + " step 250");
            sleep(7);
            commander.performPrefixedCommand(src, "tick chunk " + strChunkPos + " step 10000");
            sleep(2);
            commander.performPrefixedCommand(src, "tick chunk " + strChunkPos + " step stop");
            sleep(2);

            commander.performPrefixedCommand(src, "tick chunk " + strChunkPos + " sprint 1000000");
            sleep(5);
            commander.performPrefixedCommand(src, "tick chunk " + strChunkPos + " sprint stop");
            sleep(2);
            commander.performPrefixedCommand(src, "tick chunk " + strChunkPos + " unfreeze");
            sleep(2);
            commander.performPrefixedCommand(src, "tick chunk " + strChunkPos + " rate reset");
            sleep(2);
            LOGGER.info("FINISH TICKRATE **COMMAND** TEST");
        });
    }

    private static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void register() {
        if(!registered) { // only do it once
            TickRateEvents.SERVER_RATE.register((_, rate) -> LOGGER.info("server rate {}", rate));
            TickRateEvents.SERVER_FREEZE.register((_, freeze) -> LOGGER.info("server freeze {}", freeze));
            TickRateEvents.SERVER_STEP.register((_, stepTicks) -> LOGGER.info("server step {}", stepTicks));
            TickRateEvents.SERVER_SPRINT.register((_, sprintTicks) -> LOGGER.info("server sprint {}", sprintTicks));

            TickRateEvents.ENTITY_RATE.register((entity, rate) -> LOGGER.info("{} rate {}", entity.getStringUUID(), rate));
            TickRateEvents.ENTITY_FREEZE.register((entity, freeze) -> LOGGER.info("{} freeze {}", entity.getStringUUID(), freeze));
            TickRateEvents.ENTITY_STEP.register((entity, stepTicks) -> LOGGER.info("{} step {}", entity.getStringUUID(), stepTicks));
            TickRateEvents.ENTITY_SPRINT.register((entity, sprintTicks) -> LOGGER.info("{} sprint {}", entity.getStringUUID(), sprintTicks));

            TickRateEvents.CHUNK_RATE.register((chunk, rate) -> LOGGER.info("{} {} rate {}", chunk.getLevel().dimension().identifier(), chunk.getPos(), rate));
            TickRateEvents.CHUNK_FREEZE.register((chunk, freeze) -> LOGGER.info("{} {} freeze {}", chunk.getLevel().dimension().identifier(), chunk.getPos(), freeze));
            TickRateEvents.CHUNK_STEP.register((chunk, stepTicks) -> LOGGER.info("{} {} step {}", chunk.getLevel().dimension().identifier(), chunk.getPos(), stepTicks));
            TickRateEvents.CHUNK_SPRINT.register((chunk, sprintTicks) -> LOGGER.info("{} {} sprint {}", chunk.getLevel().dimension().identifier(), chunk.getPos(), sprintTicks));
            registered = true;
        }
    }

}
