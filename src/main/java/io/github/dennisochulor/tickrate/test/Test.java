package io.github.dennisochulor.tickrate.test;

import io.github.dennisochulor.tickrate.api.TickRateAPI;
import io.github.dennisochulor.tickrate.api.TickRateEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

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

    public static void test(Entity testEntity) {
        if(!registered) { // only do it once
            TickRateEvents.ENTITY_RATE.register((entity, rate) -> LOGGER.info("{} rate {}", entity.getUuidAsString(), rate));
            TickRateEvents.ENTITY_FREEZE.register((entity, freeze) -> LOGGER.info("{} freeze {}", entity.getUuidAsString(), freeze));
            TickRateEvents.ENTITY_STEP.register((entity, stepTicks) -> LOGGER.info("{} step {}", entity.getUuidAsString(), stepTicks));
            TickRateEvents.ENTITY_SPRINT.register((entity, sprintTicks) -> LOGGER.info("{} sprint {}", entity.getUuidAsString(), sprintTicks));

            TickRateEvents.CHUNK_RATE.register((world, chunkPos, rate) -> LOGGER.info("{} {} rate {}", world.getRegistryKey().getValue(), chunkPos, rate));
            TickRateEvents.CHUNK_FREEZE.register((world, chunkPos, freeze) -> LOGGER.info("{} {} freeze {}", world.getRegistryKey().getValue(), chunkPos, freeze));
            TickRateEvents.CHUNK_STEP.register((world, chunkPos, stepTicks) -> LOGGER.info("{} {} step {}", world.getRegistryKey().getValue(), chunkPos, stepTicks));
            TickRateEvents.CHUNK_SPRINT.register((world, chunkPos, sprintTicks) -> LOGGER.info("{} {} sprint {}", world.getRegistryKey().getValue(), chunkPos, sprintTicks));
            registered = true;
        }

        Thread.ofVirtual().name("TickRateTest Thread").start(() -> {
            TickRateAPI api = TickRateAPI.getInstance();
            String uuid = testEntity.getUuidAsString();
            ServerCommandSource src = testEntity.getServer().getCommandSource();
            CommandManager commander = testEntity.getServer().getCommandManager();
            World world = testEntity.getWorld();
            ChunkPos chunkPos = testEntity.getChunkPos();
            String strChunkPos = chunkPos.getCenterX() + " " + chunkPos.getCenterZ();

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
            api.rateChunk(world, chunkPos, 50);
            LOGGER.info("{} TPS", api.queryChunk(world, chunkPos));
            sleep(2);
            api.freezeChunk(world, chunkPos, true);
            sleep(2);

            api.stepChunk(world, chunkPos, 250);
            sleep(7);
            api.stepChunk(world, chunkPos, 10000);
            sleep(2);
            api.stepChunk(world, chunkPos, 0);
            sleep(2);

            api.sprintChunk(world, chunkPos, 1000000);
            sleep(5);
            api.sprintChunk(world, chunkPos, 0);
            sleep(2);
            api.freezeChunk(world, chunkPos, false);
            sleep(2);
            api.rateChunk(world, chunkPos, 0.0f);
            sleep(2);
            LOGGER.info("FINISH TICKRATE **API** TEST");

            sleep(2);

            LOGGER.info("STARTING TICKRATE **COMMAND** TEST");
            sleep(5);

            LOGGER.info("ENTITY TEST");
            commander.executeWithPrefix(src, "tick entity " + uuid + " rate 10");
            commander.executeWithPrefix(src, "tick entity " + uuid + " query");
            sleep(2);
            commander.executeWithPrefix(src, "tick entity " + uuid + " freeze");
            sleep(2);

            commander.executeWithPrefix(src, "tick entity " + uuid + " step 50");
            sleep(7);
            commander.executeWithPrefix(src, "tick entity " + uuid + " step 10000");
            sleep(2);
            commander.executeWithPrefix(src, "tick entity " + uuid + " step stop");
            sleep(2);

            commander.executeWithPrefix(src, "tick entity " + uuid + " sprint 1000000");
            sleep(5);
            commander.executeWithPrefix(src, "tick entity " + uuid + " sprint stop");
            sleep(2);
            commander.executeWithPrefix(src, "tick entity " + uuid + " unfreeze");
            sleep(2);
            commander.executeWithPrefix(src, "tick entity " + uuid + " rate reset");
            sleep(5);

            LOGGER.info("CHUNK TESTS");
            commander.executeWithPrefix(src, "tick chunk " + strChunkPos + " rate 50");
            commander.executeWithPrefix(src, "tick chunk " + strChunkPos + " query");
            sleep(2);
            commander.executeWithPrefix(src, "tick chunk " + strChunkPos + " freeze");
            sleep(2);

            commander.executeWithPrefix(src, "tick chunk " + strChunkPos + " step 250");
            sleep(7);
            commander.executeWithPrefix(src, "tick chunk " + strChunkPos + " step 10000");
            sleep(2);
            commander.executeWithPrefix(src, "tick chunk " + strChunkPos + " step stop");
            sleep(2);

            commander.executeWithPrefix(src, "tick chunk " + strChunkPos + " sprint 1000000");
            sleep(5);
            commander.executeWithPrefix(src, "tick chunk " + strChunkPos + " sprint stop");
            sleep(2);
            commander.executeWithPrefix(src, "tick chunk " + strChunkPos + " unfreeze");
            sleep(2);
            commander.executeWithPrefix(src, "tick chunk " + strChunkPos + " rate reset");
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

}
