package io.github.dennisochulor.tickrate.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.ColumnPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.TimeArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TickCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(TickCommand.class)
public class TickCommandMixin {

    @Shadow @Final private static String DEFAULT_TICK_RATE_STRING;
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
        LiteralCommandNode<ServerCommandSource> chunkQuery = CommandManager.literal("query")
                    .executes(context -> executeChunkQuery(context.getSource(), getChunks(context,1))).build();

        LiteralCommandNode<ServerCommandSource> chunkRate = CommandManager.literal("rate")
                    .then(CommandManager.literal("reset").executes(context -> executeChunkRate(context.getSource(), getChunks(context,2), 0.0f)))
                    .then(CommandManager.argument("rate", FloatArgumentType.floatArg(1.0F, 10000.0F))
                            .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{DEFAULT_TICK_RATE_STRING,"reset"}, suggestionsBuilder))
                            .executes(context -> executeChunkRate(context.getSource(), getChunks(context,2), FloatArgumentType.getFloat(context, "rate")))).build();

        LiteralCommandNode<ServerCommandSource> chunkUnfreeze = CommandManager.literal("unfreeze")
                    .executes(context -> executeChunkFreeze(context.getSource(), getChunks(context,1), false)).build();

        LiteralCommandNode<ServerCommandSource> chunkFreeze = CommandManager.literal("freeze")
                    .executes(context -> executeChunkFreeze(context.getSource(), getChunks(context,1), true)).build();

        LiteralCommandNode<ServerCommandSource> chunkStep = CommandManager.literal("step")
                    .executes(context -> executeChunkStep(context.getSource(), getChunks(context,1), 1))
                    .then(CommandManager.literal("stop").executes(context -> executeChunkStep(context.getSource(), getChunks(context,2), 0)))
                    .then(CommandManager.argument("time", TimeArgumentType.time(1))
                            .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{"1t", "1s"}, suggestionsBuilder))
                            .executes(context -> executeChunkStep(context.getSource(), getChunks(context,2), IntegerArgumentType.getInteger(context, "time")))).build();

        LiteralCommandNode<ServerCommandSource> chunkSprint = CommandManager.literal("sprint")
                    .then(CommandManager.literal("stop").executes(context -> executeChunkSprint(context.getSource(), getChunks(context,2), 0)))
                    .then(CommandManager.argument("time", TimeArgumentType.time(1))
                            .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{"60s", "1d", "3d"}, suggestionsBuilder))
                            .executes(context -> executeChunkSprint(context.getSource(), getChunks(context,2), IntegerArgumentType.getInteger(context, "time")))).build();


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
                                        .then(CommandManager.argument("entities", EntityArgumentType.entities())
                                                .then(CommandManager.literal("query")
                                                        .executes(context -> executeEntityQuery(context.getSource(), EntityArgumentType.getEntities(context, "entities")))
                                                )
                                                .then(CommandManager.literal("rate")
                                                        .then(CommandManager.literal("reset").executes(context -> executeEntityRate(context.getSource(), EntityArgumentType.getEntities(context, "entities"), 0.0f)))
                                                        .then(CommandManager.argument("rate", FloatArgumentType.floatArg(1.0F, 10000.0F))
                                                                .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{DEFAULT_TICK_RATE_STRING,"reset"}, suggestionsBuilder))
                                                                .executes(context -> executeEntityRate(context.getSource(), EntityArgumentType.getEntities(context, "entities"), FloatArgumentType.getFloat(context, "rate"))))
                                                )
                                                .then(CommandManager.literal("unfreeze")
                                                        .executes(context -> executeEntityFreeze(context.getSource(), EntityArgumentType.getEntities(context, "entities"), false))
                                                )
                                                .then(CommandManager.literal("freeze")
                                                        .executes(context -> executeEntityFreeze(context.getSource(), EntityArgumentType.getEntities(context, "entities"), true))
                                                )
                                                .then(CommandManager.literal("step")
                                                        .executes(context -> executeEntityStep(context.getSource(), EntityArgumentType.getEntities(context,"entities"), 1))
                                                        .then(CommandManager.literal("stop").executes(context -> executeEntityStep(context.getSource(), EntityArgumentType.getEntities(context, "entities"), 0)))
                                                        .then(CommandManager.argument("time", TimeArgumentType.time(1))
                                                                .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{"1t", "1s"}, suggestionsBuilder))
                                                                .executes(context -> executeEntityStep(context.getSource(), EntityArgumentType.getEntities(context, "entities"), IntegerArgumentType.getInteger(context, "time"))))
                                                )
                                                .then(CommandManager.literal("sprint")
                                                        .then(CommandManager.literal("stop").executes(context -> executeEntitySprint(context.getSource(), EntityArgumentType.getEntities(context, "entities"),0)))
                                                        .then(CommandManager.argument("time", TimeArgumentType.time(1))
                                                                .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{"60s", "1d", "3d"}, suggestionsBuilder))
                                                                .executes(context -> executeEntitySprint(context.getSource(), EntityArgumentType.getEntities(context, "entities"), IntegerArgumentType.getInteger(context, "time"))))
                                                )
                                        )
                        )
                        .then(
                                CommandManager.literal("chunk")
                                        .then(CommandManager.argument("from", ColumnPosArgumentType.columnPos())
                                                .then(chunkQuery)
                                                .then(chunkRate)
                                                .then(chunkUnfreeze)
                                                .then(chunkFreeze)
                                                .then(chunkStep)
                                                .then(chunkSprint)
                                                .then(CommandManager.argument("to", ColumnPosArgumentType.columnPos())
                                                        .then(chunkQuery)
                                                        .then(chunkRate)
                                                        .then(chunkUnfreeze)
                                                        .then(chunkFreeze)
                                                        .then(chunkStep)
                                                        .then(chunkSprint)
                                                )
                                                .then(CommandManager.literal("radius")
                                                        .then(CommandManager.argument("radius", FloatArgumentType.floatArg(1))
                                                                .then(chunkQuery)
                                                                .then(chunkRate)
                                                                .then(chunkUnfreeze)
                                                                .then(chunkFreeze)
                                                                .then(chunkStep)
                                                                .then(chunkSprint)
                                                        )
                                                )
                                        )
                        )
        );
    }

    /**
     * @author Ninjaking312
     * @reason To round the rate and set server (not mainloop) rate
     */
    @Overwrite
    private static int executeRate(ServerCommandSource source, float rate) {
        int roundRate = Math.round(rate); // can't actually accept decimals
        ServerTickManager tickManager = source.getServer().getTickManager();
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
        String string = format(source.getServer().getAverageNanosPerTick());
        float f = serverTickManager.getTickRate();
        String string2 = String.format(Locale.ROOT, "%.1f", f);
        if (serverTickManager.tickRate$isServerSprint()) {
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

            source.sendFeedback(() -> Text.literal("Server's target tick rate: " + serverTickManager.tickRate$getServerRate() + " per second (" + format((long)((double) TimeHelper.SECOND_IN_NANOS / (double)serverTickManager.tickRate$getServerRate())) + " mspt)"), false);
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

    @Inject(method = "executeSprint", at = @At("TAIL"))
    private static void executeSprint(ServerCommandSource source, int ticks, CallbackInfoReturnable<Integer> cir) {
        source.getServer().getTickManager().tickRate$sendUpdatePacket();
    }

    @Inject(method = "executeFreeze", at = @At("TAIL"))
    private static void executeFreeze(ServerCommandSource source, boolean frozen, CallbackInfoReturnable<Integer> cir) {
        source.getServer().getTickManager().tickRate$sendUpdatePacket();
    }

    @Inject(method = "executeStep", at = @At("TAIL"))
    private static void executeStep(ServerCommandSource source, int ticks, CallbackInfoReturnable<Integer> cir) {
        source.getServer().getTickManager().tickRate$sendUpdatePacket();
    }

    @Inject(method = "executeStopStep", at = @At("RETURN"))
    private static void executeStopStep(ServerCommandSource source, CallbackInfoReturnable<Integer> cir) {
        source.getServer().getTickManager().tickRate$sendUpdatePacket();
    }

    @Inject(method = "executeStopSprint", at = @At("RETURN"))
    private static void executeStopSprint(ServerCommandSource source, CallbackInfoReturnable<Integer> cir) {
        source.getServer().getTickManager().tickRate$sendUpdatePacket();
    }


    @Unique
    private static int executeChunkRate(ServerCommandSource source, List<ChunkPos> chunks, float rate) {
        if(chunkCheck(chunks, source)) return 0;

        int roundRate = Math.round(rate); // can't actually accept decimals
        ServerTickManager tickManager = source.getServer().getTickManager();
        tickManager.tickRate$setChunkRate(roundRate, source.getWorld(), chunks);
        tickManager.tickRate$sendUpdatePacket();
        if(roundRate != 0) {
            source.sendFeedback(() -> Text.of("Set tick rate of " + chunks.size() + " chunks to " + roundRate + " TPS."), false);
            return roundRate;
        }
        else {
            source.sendFeedback(() -> Text.literal("Reset the target rate of " + chunks.size() + " chunks according to the server's tick rate."), false);
            return (int) tickManager.tickRate$getServerRate();
        }
    }

    @Unique
    private static int executeChunkQuery(ServerCommandSource source, List<ChunkPos> chunks) {
        if(chunkCheck(chunks, source)) return 0;

        ServerTickManager tickManager = source.getServer().getTickManager();
        MutableText text = Text.literal("The tick rates of the specified chunks are as follows:\n");
        float firstRate = tickManager.tickRate$getChunkRate(source.getWorld(), chunks.stream().findFirst().orElseThrow().toLong());
        chunks.forEach(chunk -> text.append("Chunk ").append(chunk.toString()).append(" - ").append(String.valueOf(tickManager.tickRate$getChunkRate(source.getWorld(), chunk.toLong()))).append(" TPS").append("\n"));
        text.getSiblings().removeLast(); // to remove last \n
        source.sendFeedback(() -> text, false);
        return (int) firstRate;
    }

    @Unique
    private static int executeChunkFreeze(ServerCommandSource source, List<ChunkPos> chunks, boolean frozen) {
        if(chunkCheck(chunks, source)) return 0;

        ServerTickManager tickManager = source.getServer().getTickManager();
        tickManager.tickRate$setChunkFrozen(frozen, source.getWorld(), chunks);
        if(frozen) source.sendFeedback(() -> Text.literal(chunks.size() + " chunks have been frozen."), false);
        else source.sendFeedback(() -> Text.literal(chunks.size() + " chunks have been unfrozen."), false);
        tickManager.tickRate$sendUpdatePacket();
        return 1;
    }

    @Unique
    private static int executeChunkStep(ServerCommandSource source, List<ChunkPos> chunks, int steps) {
        if(chunkCheck(chunks, source)) return 0;

        ServerTickManager tickManager = source.getServer().getTickManager();
        boolean success = tickManager.tickRate$stepChunk(steps, source.getWorld(), chunks);
        if(success && steps != 0) source.sendFeedback(() -> Text.literal(chunks.size() + " chunks will step " + steps + " ticks."), false);
        else if(success && steps == 0) source.sendFeedback(() -> Text.literal(chunks.size() + " chunks have stopped stepping."), false);
        else source.sendFeedback(() -> Text.literal("All of the specified chunks must be frozen first and cannot be sprinting!").withColor(Colors.LIGHT_RED), false);
        tickManager.tickRate$sendUpdatePacket();
        return success ? 1 : 0;
    }

    @Unique
    private static int executeChunkSprint(ServerCommandSource source, List<ChunkPos> chunks, int ticks) {
        if(chunkCheck(chunks, source)) return 0;

        ServerTickManager tickManager = source.getServer().getTickManager();
        boolean success = tickManager.tickRate$sprintChunk(ticks, source.getWorld(), chunks);
        if(success && ticks != 0) source.sendFeedback(() -> Text.literal(chunks.size() + " chunks will sprint for " + ticks + " ticks."), false);
        else if(success && ticks == 0) source.sendFeedback(() -> Text.literal(chunks.size() + " chunks have stopped sprinting."), false);
        else source.sendFeedback(() -> Text.literal("All of the specified chunks must not be stepping!").withColor(Colors.LIGHT_RED), false);
        tickManager.tickRate$sendUpdatePacket();
        return success ? 1 : 0;
    }

    @Unique
    private static int executeEntityRate(ServerCommandSource source, Collection<? extends Entity> entities, float rate) {
        int roundRate = Math.round(rate); // can't actually accept decimals
        if(entityCheck(entities,source)) return 0;

        ServerTickManager tickManager = source.getServer().getTickManager();
        tickManager.tickRate$setEntityRate(roundRate, entities);
        if(roundRate != 0) {
            source.sendFeedback(() -> Text.of("Set tick rate of " + entities.size() + " entities to " + roundRate + " TPS."), false);
            tickManager.tickRate$sendUpdatePacket();
            return roundRate;
        }
        else {
            source.sendFeedback(() -> Text.literal("Reset the tick rate of " + entities.size() + " entities according to the server's tick rate."), false);
            tickManager.tickRate$sendUpdatePacket();
            return (int) tickManager.tickRate$getServerRate();
        }
    }

    @Unique
    private static int executeEntityQuery(ServerCommandSource source, Collection<? extends Entity> entities) {
        ServerTickManager tickManager = source.getServer().getTickManager();
        MutableText text = Text.literal("The tick rates of the specified entities are as follows:\n");
        float firstRate = tickManager.tickRate$getEntityRate(entities.stream().findFirst().orElseThrow());
        entities.forEach(entity -> text.append(entity.getType().getName()).append(" ").append(entity.getNameForScoreboard()).append(" - ").append(String.valueOf(tickManager.tickRate$getEntityRate(entity))).append(" TPS").append("\n"));
        text.getSiblings().removeLast(); // to remove last \n
        source.sendFeedback(() -> text, false);
        return (int) firstRate;
    }

    @Unique
    private static int executeEntityFreeze(ServerCommandSource source, Collection<? extends Entity> entities, boolean frozen) {
        if(entityCheck(entities,source)) return 0;

        ServerTickManager tickManager = source.getServer().getTickManager();
        tickManager.tickRate$setEntityFrozen(frozen, entities);
        if(frozen) source.sendFeedback(() -> Text.literal(entities.size() + " entities have been frozen."), false);
        else source.sendFeedback(() -> Text.literal(entities.size() + " entities have been unfrozen."), false);
        tickManager.tickRate$sendUpdatePacket();
        return 1;
    }

    @Unique
    private static int executeEntityStep(ServerCommandSource source, Collection<? extends Entity> entities, int steps) {
        if(entityCheck(entities,source)) return 0;

        ServerTickManager tickManager = source.getServer().getTickManager();
        boolean success = tickManager.tickRate$stepEntity(steps,entities);
        if(success && steps != 0) source.sendFeedback(() -> Text.literal(entities.size() + " entities will step " + steps + " ticks."), false);
        else if(success && steps == 0) source.sendFeedback(() -> Text.literal(entities.size() + " entities have stopped stepping."), false);
        else source.sendFeedback(() -> Text.literal("All of the specified entities must be frozen first and cannot be sprinting!").withColor(Colors.LIGHT_RED), false);
        tickManager.tickRate$sendUpdatePacket();
        return success ? 1 : 0;
    }

    @Unique
    private static int executeEntitySprint(ServerCommandSource source, Collection<? extends Entity> entities, int ticks) {
        if(entityCheck(entities,source)) return 0;

        ServerTickManager tickManager = source.getServer().getTickManager();
        boolean success = tickManager.tickRate$sprintEntity(ticks,entities);
        if(success && ticks != 0) source.sendFeedback(() -> Text.literal(entities.size() + " entities will sprint for " + ticks + " ticks."), false);
        else if(success && ticks == 0) source.sendFeedback(() -> Text.literal(entities.size() + " entities have stopped sprinting."), false);
        else source.sendFeedback(() -> Text.literal("All of the specified entities must not be stepping!").withColor(Colors.LIGHT_RED), false);
        tickManager.tickRate$sendUpdatePacket();
        return success ? 1 : 0;
    }

    @Unique
    // returns true if any of the entities cannot be the command's target
    private static boolean entityCheck(Collection<? extends Entity> entities, ServerCommandSource source) {
        ServerTickManager tickManager = source.getServer().getTickManager();
        boolean match = entities.stream().anyMatch(e -> {
            if(e instanceof ServerPlayerEntity player) return !tickManager.tickRate$hasClientMod(player);
            else return false;
        });
        if(match) source.sendFeedback(() -> Text.literal("Some of the specified entities are players that do not have TickRate mod installed on their client, so their tick rate cannot be manipulated.").withColor(Colors.LIGHT_RED), false);
        return match;
    }

    @Unique
    // returns true if any of the chunks cannot be the command's target (meaning they are unloaded)
    private static boolean chunkCheck(List<ChunkPos> chunks, ServerCommandSource source) {
        boolean match = chunks.stream().anyMatch(chunkPos -> {
            WorldChunk worldChunk = (WorldChunk) source.getWorld().getChunk(chunkPos.x,chunkPos.z,ChunkStatus.FULL,false);
            return worldChunk==null || worldChunk.getLevelType() == ChunkLevelType.INACCESSIBLE;
        });
        if(match) source.sendFeedback(() -> Text.literal("Some of the specified chunks are not loaded!").withColor(Colors.LIGHT_RED), false);
        return match;
    }

    /**
     * @param depth number of steps back up the command tree to get to the node right before the chunkOperations
     */
    @Unique
    private static List<ChunkPos> getChunks(CommandContext<ServerCommandSource> context, int depth) throws CommandSyntaxException {
        // CommandContext#getArgument is not used because it throws an Exception when not found, which is not great for performance
        String lastNode = context.getNodes().get(context.getNodes().size() - depth - 1).getNode().getName();
        return switch(lastNode) {
            case "from" -> {
                ColumnPos from = ColumnPosArgumentType.getColumnPos(context, "from");
                if (from.x() < -30000000 || from.z() < -30000000 || from.x() >= 30000000 || from.z() >= 30000000)
                    throw BlockPosArgumentType.OUT_OF_WORLD_EXCEPTION.create();
                yield List.of(from.toChunkPos());
            }
            case "to" -> {
                // logic taken from ForceLoadCommand :)
                ColumnPos from = ColumnPosArgumentType.getColumnPos(context, "from");
                ColumnPos to = ColumnPosArgumentType.getColumnPos(context, "to");
                int minX = Math.min(from.x(), to.x());
                int minZ = Math.min(from.z(), to.z());
                int maxX = Math.max(from.x(), to.x());
                int maxZ = Math.max(from.z(), to.z());
                if (minX < -30000000 || minZ < -30000000 || maxX >= 30000000 || maxZ >= 30000000)
                    throw BlockPosArgumentType.OUT_OF_WORLD_EXCEPTION.create();

                int chunkMinX = ChunkSectionPos.getSectionCoord(minX);
                int chunkMinZ = ChunkSectionPos.getSectionCoord(minZ);
                int chunkMaxX = ChunkSectionPos.getSectionCoord(maxX);
                int chunkMaxZ = ChunkSectionPos.getSectionCoord(maxZ);
                List<ChunkPos> chunks = new ArrayList<>();
                for(int chunkX = chunkMinX; chunkX <= chunkMaxX; chunkX++) {
                    for(int chunkZ = chunkMinZ; chunkZ <= chunkMaxZ; chunkZ++) {
                        chunks.add(new ChunkPos(chunkX, chunkZ));
                    }
                }
                yield chunks;
            }
            case "radius" -> {
                // logic taken from ForceLoadCommand :)
                ColumnPos circleCentre = ColumnPosArgumentType.getColumnPos(context, "from");
                float radius = FloatArgumentType.getFloat(context, "radius");
                float minX = circleCentre.x() - radius;
                float minZ = circleCentre.z() - radius;
                float maxX = circleCentre.x() + radius;
                float maxZ = circleCentre.z() + radius;
                if (minX < -30000000 || minZ < -30000000 || maxX >= 30000000 || maxZ >= 30000000)
                    throw BlockPosArgumentType.OUT_OF_WORLD_EXCEPTION.create();

                int chunkMinX = ChunkSectionPos.getSectionCoord(minX);
                int chunkMinZ = ChunkSectionPos.getSectionCoord(minZ);
                int chunkMaxX = ChunkSectionPos.getSectionCoord(maxX);
                int chunkMaxZ = ChunkSectionPos.getSectionCoord(maxZ);
                List<ChunkPos> chunks = new ArrayList<>();
                for(int chunkX = chunkMinX; chunkX <= chunkMaxX; chunkX++) {
                    for(int chunkZ = chunkMinZ; chunkZ <= chunkMaxZ; chunkZ++) {
                        // https://www.geeksforgeeks.org/check-if-any-point-overlaps-the-given-circle-and-rectangle/
                        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                        ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(chunkPos,0);
                        int X1 = chunkSectionPos.getMinX();
                        int X2 = chunkSectionPos.getMaxX();
                        int Z1 = chunkSectionPos.getMinZ();
                        int Z2 = chunkSectionPos.getMaxZ();
                        int Xc = circleCentre.x();
                        int Zc = circleCentre.z();

                        // find closest point of chunk to centre of circle
                        int Xn = Math.max(X1, Math.min(Xc, X2));
                        int Yn = Math.max(Z1, Math.min(Zc, Z2));

                        // find distance between nearest point and circle centre
                        int Dx = Xn - Xc;
                        int Dz = Yn - Zc;
                        if((Dx * Dx + Dz * Dz) <= radius * radius) chunks.add(chunkPos); // if overlap, add it
                    }
                }
                yield chunks;
            }
            default -> throw new IllegalStateException("Unexpected value: " + lastNode);
        };
    }

}
