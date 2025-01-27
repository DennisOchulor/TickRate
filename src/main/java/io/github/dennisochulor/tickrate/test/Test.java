package io.github.dennisochulor.tickrate.test;

import io.github.dennisochulor.tickrate.api.TickRateAPI;
import io.github.dennisochulor.tickrate.api.TickRateEvents;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import static io.github.dennisochulor.tickrate.TickRate.LOGGER;

public final class Test {

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
            World world = testEntity.getWorld();
            ChunkPos chunkPos = testEntity.getChunkPos();

            LOGGER.info("STARTING TICKRATE TEST");
            sleep(5);

            LOGGER.info("ENTITY TEST");
            api.rateEntity(testEntity, 10);
            sleep(2);
            api.freezeEntity(testEntity, true);
            sleep(2);
            api.stepEntity(testEntity, 50);
            sleep(7);
            api.sprintEntity(testEntity, 4000);
            sleep(5);
            api.freezeEntity(testEntity, false);
            sleep(2);
            api.rateEntity(testEntity, 0.0f);
            sleep(5);

            LOGGER.info("CHUNK TESTS");
            api.rateChunk(world, chunkPos, 50);
            sleep(2);
            api.freezeChunk(world, chunkPos, true);
            sleep(2);
            api.stepChunk(world, chunkPos, 250);
            sleep(7);
            api.sprintChunk(world, chunkPos, 4000);
            sleep(5);
            api.freezeChunk(world, chunkPos, false);
            sleep(2);
            api.rateChunk(world, chunkPos, 0.0f);
            sleep(2);
            LOGGER.info("FINISH TICKRATE TEST");
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
