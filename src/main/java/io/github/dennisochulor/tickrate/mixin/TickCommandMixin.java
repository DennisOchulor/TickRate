package io.github.dennisochulor.tickrate.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.dennisochulor.tickrate.TickRateTickManager;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.TimeArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TickCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

@Mixin(TickCommand.class)
public class TickCommandMixin {

    @Shadow private static final float MAX_TICK_RATE = 10000.0F;
    @Shadow private static final String DEFAULT_TICK_RATE_STRING = String.valueOf(20);

    @Shadow private static int executeSprint(ServerCommandSource source, int ticks) { return 0; }
    @Shadow private static int executeFreeze(ServerCommandSource source, boolean frozen) { return 0; }
    @Shadow private static int executeStep(ServerCommandSource source, int steps) { return 0; }
    @Shadow private static int executeStopStep(ServerCommandSource source) { return 0; }
    @Shadow private static int executeStopSprint(ServerCommandSource source) { return 0; }
    @Shadow private static String format(long nanos) { return ""; }

    /**
     * @author Ninjaking312
     * @reason Need to add multiple subcommands
     */
    @Overwrite
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("tick")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("query").executes(context -> executeQuery(context.getSource())))
                        .then(
                                CommandManager.literal("rate")
                                        .then(
                                                CommandManager.argument("rate", FloatArgumentType.floatArg(1.0F, 10000.0F))
                                                        .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{DEFAULT_TICK_RATE_STRING}, suggestionsBuilder))
                                                        .executes(context -> executeRate(context.getSource(), FloatArgumentType.getFloat(context, "rate")))
                                        )
                        )
                        .then(
                                CommandManager.literal("step")
                                        .executes(context -> executeStep(context.getSource(), 1))
                                        .then(CommandManager.literal("stop").executes(context -> executeStopStep(context.getSource())))
                                        .then(
                                                CommandManager.argument("time", TimeArgumentType.time(1))
                                                        .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{"1t", "1s"}, suggestionsBuilder))
                                                        .executes(context -> executeStep(context.getSource(), IntegerArgumentType.getInteger(context, "time")))
                                        )
                        )
                        .then(
                                CommandManager.literal("sprint")
                                        .then(CommandManager.literal("stop").executes(context -> executeStopSprint(context.getSource())))
                                        .then(
                                                CommandManager.argument("time", TimeArgumentType.time(1))
                                                        .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{"60s", "1d", "3d"}, suggestionsBuilder))
                                                        .executes(context -> executeSprint(context.getSource(), IntegerArgumentType.getInteger(context, "time")))
                                        )
                        )
                        .then(CommandManager.literal("unfreeze").executes(context -> executeFreeze(context.getSource(), false)))
                        .then(CommandManager.literal("freeze").executes(context -> executeFreeze(context.getSource(), true)))
                        .then(
                                CommandManager.literal("entity")
                                        .then(CommandManager.literal("query")
                                                .then(CommandManager.argument("entity", EntityArgumentType.entity())
                                                        .executes(context -> executeEntityQuery(context.getSource(), EntityArgumentType.getEntity(context, "entity"))))
                                        )
                                        .then(CommandManager.literal("rate")
                                                .then(CommandManager.argument("entities", EntityArgumentType.entities())
                                                        .then(CommandManager.literal("reset").executes(context -> executeEntityRate(context.getSource(), EntityArgumentType.getEntities(context, "entities"), 0.0f)))
                                                        .then(CommandManager.argument("rate", FloatArgumentType.floatArg(1.0F, 10000.0F))
                                                                .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{DEFAULT_TICK_RATE_STRING,"reset"}, suggestionsBuilder))
                                                                .executes(context -> executeEntityRate(context.getSource(), EntityArgumentType.getEntities(context, "entities"), FloatArgumentType.getFloat(context, "rate")))))
                                        )
                                        .then(CommandManager.literal("unfreeze")
                                            .then(CommandManager.argument("entities",EntityArgumentType.entities())
                                            .executes(context -> executeEntityFreeze(context.getSource(), EntityArgumentType.getEntities(context, "entities"), false)))
                                        )
                                        .then(CommandManager.literal("freeze")
                                            .then(CommandManager.argument("entities",EntityArgumentType.entities())
                                            .executes(context -> executeEntityFreeze(context.getSource(), EntityArgumentType.getEntities(context, "entities"), true)))
                                        )
                                        .then(CommandManager.literal("step")
                                                .then(CommandManager.argument("entities", EntityArgumentType.entities())
                                                    .executes(context -> executeEntityStep(context.getSource(), EntityArgumentType.getEntities(context,"entities"), 1))
                                                    .then(CommandManager.literal("stop").executes(context -> executeEntityStep(context.getSource(), EntityArgumentType.getEntities(context, "entities"), 0)))
                                                    .then(
                                                         CommandManager.argument("time", TimeArgumentType.time(1))
                                                                  .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{"1t", "1s"}, suggestionsBuilder))
                                                                  .executes(context -> executeEntityStep(context.getSource(), EntityArgumentType.getEntities(context, "entities"), IntegerArgumentType.getInteger(context, "time")))))
                                        )
                                        .then(CommandManager.literal("sprint")
                                                .then(CommandManager.argument("entities", EntityArgumentType.entities())
                                                .then(CommandManager.literal("stop").executes(context -> executeEntitySprint(context.getSource(), EntityArgumentType.getEntities(context, "entities"),0)))
                                                .then(
                                                        CommandManager.argument("time", TimeArgumentType.time(1))
                                                                .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{"60s", "1d", "3d"}, suggestionsBuilder))
                                                                .executes(context -> executeEntitySprint(context.getSource(), EntityArgumentType.getEntities(context, "entities"), IntegerArgumentType.getInteger(context, "time")))))
                                        )
                        )
                        .then(
                                CommandManager.literal("chunk")
                                        .then(CommandManager.literal("query")
                                                .then(CommandManager.argument("blockPos", BlockPosArgumentType.blockPos())
                                                .executes(context -> executeChunkQuery(context.getSource(), BlockPosArgumentType.getBlockPos(context, "blockPos")))))
                                        .then(CommandManager.literal("rate")
                                                .then(CommandManager.argument("blockPos", BlockPosArgumentType.blockPos())
                                                        .then(CommandManager.literal("reset").executes(context -> executeChunkRate(context.getSource(), BlockPosArgumentType.getBlockPos(context, "blockPos"), 0.0f)))
                                                        .then(CommandManager.argument("rate", FloatArgumentType.floatArg(1.0F, 10000.0F))
                                                                .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{DEFAULT_TICK_RATE_STRING,"reset"}, suggestionsBuilder))
                                                                .executes(context -> executeChunkRate(context.getSource(), BlockPosArgumentType.getBlockPos(context, "blockPos"), FloatArgumentType.getFloat(context, "rate")))))
                                        )
                                        .then(CommandManager.literal("unfreeze")
                                                .then(CommandManager.argument("chunk", BlockPosArgumentType.blockPos())
                                                .executes(context -> executeChunkFreeze(context.getSource(), BlockPosArgumentType.getBlockPos(context,"chunk"), false)))
                                        )
                                        .then(CommandManager.literal("freeze")
                                                .then(CommandManager.argument("chunk", BlockPosArgumentType.blockPos())
                                                .executes(context -> executeChunkFreeze(context.getSource(), BlockPosArgumentType.getBlockPos(context,"chunk"), true)))
                                        )
                                        .then(CommandManager.literal("step")
                                                .then(CommandManager.argument("chunk", BlockPosArgumentType.blockPos())
                                                .executes(context -> executeChunkStep(context.getSource(), BlockPosArgumentType.getBlockPos(context,"chunk"), 1))
                                                .then(CommandManager.literal("stop").executes(context -> executeChunkStep(context.getSource(), BlockPosArgumentType.getBlockPos(context,"chunk"), 0)))
                                                .then(
                                                        CommandManager.argument("time", TimeArgumentType.time(1))
                                                                .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{"1t", "1s"}, suggestionsBuilder))
                                                                .executes(context -> executeChunkStep(context.getSource(), BlockPosArgumentType.getBlockPos(context,"chunk"), IntegerArgumentType.getInteger(context, "time")))))
                                        )
                                        .then(CommandManager.literal("sprint")
                                                .then(CommandManager.argument("chunk", BlockPosArgumentType.blockPos())
                                                .then(CommandManager.literal("stop").executes(context -> executeChunkSprint(context.getSource(), BlockPosArgumentType.getBlockPos(context,"chunk"), 0)))
                                                .then(
                                                        CommandManager.argument("time", TimeArgumentType.time(1))
                                                                .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{"60s", "1d", "3d"}, suggestionsBuilder))
                                                                .executes(context -> executeChunkSprint(context.getSource(), BlockPosArgumentType.getBlockPos(context,"chunk"), IntegerArgumentType.getInteger(context, "time")))))
                                        )
                        )
        );
    }

    /**
     * @author Ninjaking312
     * @reason Because I need to
     */
    @Overwrite
    private static int executeRate(ServerCommandSource source, float rate) {
        int roundRate = Math.round(rate); // can't actually accept decimals
        TickRateTickManager tickManager = (TickRateTickManager) source.getServer().getTickManager();
        tickManager.tickRate$setServerRate(roundRate);
        tickManager.tickRate$sendUpdatePacket();
        source.sendFeedback(() -> Text.translatable("commands.tick.rate.success", roundRate), true);
        return roundRate;
    }

    /**
     * @author Ninjaking312
     * @reason To add distinction between mainloop and server tick rate
     */
    @Overwrite
    private static int executeQuery(ServerCommandSource source) {
        ServerTickManager serverTickManager = source.getServer().getTickManager();
        TickRateTickManager tickRateTickManager = (TickRateTickManager) serverTickManager;
        String string = format(source.getServer().getAverageNanosPerTick());
        float f = serverTickManager.getTickRate();
        String string2 = String.format(Locale.ROOT, "%.1f", f);
        if (serverTickManager.isSprinting()) {
            source.sendFeedback(() -> Text.translatable("commands.tick.status.sprinting"), false);
            source.sendFeedback(() -> Text.translatable("commands.tick.query.rate.sprinting", string2, string), false);
        } else {
            if (serverTickManager.isFrozen()) {
                source.sendFeedback(() -> Text.translatable("commands.tick.status.frozen"), false);
            } else if (serverTickManager.getNanosPerTick() < source.getServer().getAverageNanosPerTick()) {
                source.sendFeedback(() -> Text.translatable("commands.tick.status.lagging"), false);
            } else {
                source.sendFeedback(() -> Text.translatable("commands.tick.status.running"), false);
            }

            source.sendFeedback(() -> Text.literal("Server's target tick rate: " + tickRateTickManager.tickRate$getServerRate() + " per second (" + format((long)((double) TimeHelper.SECOND_IN_NANOS / (double)tickRateTickManager.tickRate$getServerRate())) + " mspt)"), false);
            source.sendFeedback(() -> Text.literal("Mainloop's target tick rate: " + serverTickManager.getTickRate() + " per second (" + format((long)((double) TimeHelper.SECOND_IN_NANOS / (double)serverTickManager.getTickRate())) + " mspt)"), false);
        }

        long[] ls = Arrays.copyOf(source.getServer().getTickTimes(), source.getServer().getTickTimes().length);
        Arrays.sort(ls);
        String p50 = format(ls[ls.length / 2]);
        String p95 = format(ls[(int)((double)ls.length * 0.95)]);
        String p99 = format(ls[(int)((double)ls.length * 0.99)]);
        float avg = source.getServer().getAverageTickTime();
        source.sendFeedback(() -> Text.literal("Avg: %.1fms P50: %sms P95: %sms P99: %sms, sample: %s".formatted(avg,p50,p95,p99,ls.length)), false);
        return (int)f;
    }

    @Unique
    private static int executeChunkRate(ServerCommandSource source, BlockPos blockPos, float rate) {
        int roundRate = Math.round(rate); // can't actually accept decimals
        TickRateTickManager tickManager = (TickRateTickManager) source.getServer().getTickManager();
        tickManager.tickRate$setChunkRate(roundRate, source.getWorld(), ChunkPos.toLong(blockPos));
        if(roundRate != 0) {
            source.sendFeedback(() -> Text.of("Successfully set target rate of the specified chunk to " + roundRate), false);
            tickManager.tickRate$sendUpdatePacket();
            return roundRate;
        }
        else {
            source.sendFeedback(() -> Text.literal("Reset the target rate of the specified chunk according to the server's tick rate."), false);
            tickManager.tickRate$sendUpdatePacket();
            return (int) tickManager.tickRate$getServerRate();
        }
    }

    @Unique
    private static int executeChunkQuery(ServerCommandSource source, BlockPos blockPos) {
        TickRateTickManager tickManager = (TickRateTickManager) source.getServer().getTickManager();
        float rate = tickManager.tickRate$getChunkRate(source.getWorld(), ChunkPos.toLong(blockPos));
        source.sendFeedback(() -> Text.literal("The tick rate of the specified chunk is " + rate + " TPS."), false);
        return (int) rate;
    }

    @Unique
    private static int executeChunkFreeze(ServerCommandSource source, BlockPos blockPos, boolean frozen) {
        TickRateTickManager tickManager = (TickRateTickManager) source.getServer().getTickManager();
        tickManager.tickRate$setChunkFrozen(frozen, source.getWorld(), ChunkPos.toLong(blockPos));
        if(frozen) source.sendFeedback(() -> Text.literal("The specified chunk has been frozen"), false);
        else source.sendFeedback(() -> Text.literal("The specified chunk has been unfrozen"), false);
        tickManager.tickRate$sendUpdatePacket();
        return 1;
    }

    @Unique
    private static int executeChunkStep(ServerCommandSource source, BlockPos blockPos, int steps) {
        TickRateTickManager tickManager = (TickRateTickManager) source.getServer().getTickManager();
        boolean success = tickManager.tickRate$stepChunk(steps, source.getWorld(), ChunkPos.toLong(blockPos));
        if(success && steps != 0) source.sendFeedback(() -> Text.literal("The specified chunk will step " + steps + " ticks."), false);
        else if(success && steps == 0) source.sendFeedback(() -> Text.literal("The specified chunk has stopped stepping."), false);
        else source.sendFeedback(() -> Text.literal("The specified chunk must be frozen first!"), false);
        tickManager.tickRate$sendUpdatePacket();
        return success ? 1 : 0;
    }

    @Unique
    private static int executeChunkSprint(ServerCommandSource source, BlockPos blockPos, int ticks) {
        TickRateTickManager tickManager = (TickRateTickManager) source.getServer().getTickManager();
        boolean success = tickManager.tickRate$sprintChunk(ticks, source.getWorld(), ChunkPos.toLong(blockPos));
        if(success && ticks != 0) source.sendFeedback(() -> Text.literal("The specified chunk will sprint for " + ticks + " ticks."), false);
        else if(success && ticks == 0) source.sendFeedback(() -> Text.literal("The specified chunk has stopped sprinting."), false);
        else source.sendFeedback(() -> Text.literal("The specified chunk must not be frozen!"), false);
        tickManager.tickRate$sendUpdatePacket();
        return success ? 1 : 0;
    }

    @Unique
    private static int executeEntityRate(ServerCommandSource source, Collection<? extends Entity> entities, float rate) {
        int roundRate = Math.round(rate); // can't actually accept decimals
        if(entityCheck(entities,source)) return 0;

        TickRateTickManager tickManager = (TickRateTickManager) source.getServer().getTickManager();
        tickManager.tickRate$setEntityRate(roundRate, entities);
        if(roundRate != 0) {
            source.sendFeedback(() -> Text.of("Successfully set target rate of the specified entities to " + roundRate), false);
            tickManager.tickRate$sendUpdatePacket();
            return roundRate;
        }
        else {
            source.sendFeedback(() -> Text.literal("Reset the target rate of the specified entities according to the server's tick rate."), false);
            tickManager.tickRate$sendUpdatePacket();
            return (int) tickManager.tickRate$getServerRate();
        }
    }

    @Unique
    private static int executeEntityQuery(ServerCommandSource source, Entity entity) {
        TickRateTickManager tickManager = (TickRateTickManager) source.getServer().getTickManager();
        float rate = tickManager.tickRate$getEntityRate(entity);
        source.sendFeedback(() -> Text.literal("The tick rate of the specified entity is " + rate + " TPS."), false);
        return (int) rate;
    }

    @Unique
    private static int executeEntityFreeze(ServerCommandSource source, Collection<? extends Entity> entities, boolean frozen) {
        if(entityCheck(entities,source)) return 0;

        TickRateTickManager tickManager = (TickRateTickManager) source.getServer().getTickManager();
        tickManager.tickRate$setEntityFrozen(frozen, entities);
        if(frozen) source.sendFeedback(() -> Text.literal("The specified entities have been frozen."), false);
        else source.sendFeedback(() -> Text.literal("The specified entities have been unfrozen."), false);
        tickManager.tickRate$sendUpdatePacket();
        return 1;
    }

    @Unique
    private static int executeEntityStep(ServerCommandSource source, Collection<? extends Entity> entities, int steps) {
        if(entityCheck(entities,source)) return 0;

        TickRateTickManager tickManager = (TickRateTickManager) source.getServer().getTickManager();
        boolean success = tickManager.tickRate$stepEntity(steps,entities);
        if(success && steps != 0) source.sendFeedback(() -> Text.literal("The specified entities will step " + steps + " ticks."), false);
        else if(success && steps == 0) source.sendFeedback(() -> Text.literal("The specified entities have stopped stepping."), false);
        else source.sendFeedback(() -> Text.literal("The specified entities must be frozen first!"), false);
        tickManager.tickRate$sendUpdatePacket();
        return success ? 1 : 0;
    }

    @Unique
    private static int executeEntitySprint(ServerCommandSource source, Collection<? extends Entity> entities, int ticks) {
        if(entityCheck(entities,source)) return 0;

        TickRateTickManager tickManager = (TickRateTickManager) source.getServer().getTickManager();
        boolean success = tickManager.tickRate$sprintEntity(ticks,entities);
        if(success && ticks != 0) source.sendFeedback(() -> Text.literal("The specified entities will sprint for " + ticks + " ticks."), false);
        else if(success && ticks == 0) source.sendFeedback(() -> Text.literal("The specified entities have stopped sprinting."), false);
        else source.sendFeedback(() -> Text.literal("The specified entities must not be frozen!"), false);
        tickManager.tickRate$sendUpdatePacket();
        return success ? 1 : 0;
    }

    @Unique
    // returns true if any of the entities cannot be the command's target
    private static boolean entityCheck(Collection<? extends Entity> entities, ServerCommandSource source) {
        TickRateTickManager tickManager = (TickRateTickManager) source.getServer().getTickManager();
        boolean match = entities.stream().anyMatch(e -> {
            if(e instanceof ServerPlayerEntity player) return !tickManager.tickRate$hasClientMod(player);
            else return false;
        });
        if(match) source.sendFeedback(() -> Text.literal("Some of the specified entities are players that do not have TickRate mod installed on their client, so their tick rate cannot be manipulated."), false);
        return match;
    }

}
