package io.github.dennisochulor.tickrate.mixin.core;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.dennisochulor.tickrate.api.TickRateEvents;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
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
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(TickCommand.class)
public class TickCommandMixin {

    @Shadow @Final private static String DEFAULT_TICK_RATE_STRING;
    @Shadow private static String format(long nanos) { return ""; }

    // the requires lambda
    @ModifyConstant(method = "method_54709", constant = @Constant(intValue = 3))
    private static int modifyPermissionLevel(int permissionLevel) {
        return 2;
    }

    // register the subcommands by modifying literal("tick") return value
    @ModifyExpressionValue(method = "register", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/command/CommandManager;literal(Ljava/lang/String;)Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;", args = "ldc=tick", ordinal = 0))
    private static LiteralArgumentBuilder<ServerCommandSource> register(LiteralArgumentBuilder<ServerCommandSource> builder) {
        LiteralCommandNode<ServerCommandSource> chunkQuery = CommandManager.literal("query")
                .executes(context -> executeQuery(context.getSource(), chunkCheck(getChunks(context,1), context.getSource()))).build();

        LiteralCommandNode<ServerCommandSource> chunkRate = CommandManager.literal("rate")
                .then(CommandManager.literal("reset").executes(context -> executeRate(context.getSource(), chunkCheck(getChunks(context,2), context.getSource()), -1.0f)))
                .then(CommandManager.argument("rate", FloatArgumentType.floatArg(1.0F, 10000.0F))
                        .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{DEFAULT_TICK_RATE_STRING,"reset"}, suggestionsBuilder))
                        .executes(context -> executeRate(context.getSource(), chunkCheck(getChunks(context,2), context.getSource()), FloatArgumentType.getFloat(context, "rate")))).build();

        LiteralCommandNode<ServerCommandSource> chunkUnfreeze = CommandManager.literal("unfreeze")
                .executes(context -> executeFreeze(context.getSource(), chunkCheck(getChunks(context,1), context.getSource()), false)).build();

        LiteralCommandNode<ServerCommandSource> chunkFreeze = CommandManager.literal("freeze")
                .executes(context -> executeFreeze(context.getSource(), chunkCheck(getChunks(context,1), context.getSource()), true)).build();

        LiteralCommandNode<ServerCommandSource> chunkStep = CommandManager.literal("step")
                .executes(context -> executeStep(context.getSource(), chunkCheck(getChunks(context,1), context.getSource()), 1))
                .then(CommandManager.literal("stop").executes(context -> executeStep(context.getSource(), chunkCheck(getChunks(context,2), context.getSource()), 0)))
                .then(CommandManager.argument("time", TimeArgumentType.time(1))
                        .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{"1t", "1s"}, suggestionsBuilder))
                        .executes(context -> executeStep(context.getSource(), chunkCheck(getChunks(context,2), context.getSource()), IntegerArgumentType.getInteger(context, "time")))).build();

        LiteralCommandNode<ServerCommandSource> chunkSprint = CommandManager.literal("sprint")
                .then(CommandManager.literal("stop").executes(context -> executeSprint(context.getSource(), chunkCheck(getChunks(context,2), context.getSource()), 0)))
                .then(CommandManager.argument("time", TimeArgumentType.time(1))
                        .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{"60s", "1d", "3d"}, suggestionsBuilder))
                        .executes(context -> executeSprint(context.getSource(), chunkCheck(getChunks(context,2), context.getSource()), IntegerArgumentType.getInteger(context, "time")))).build();

        builder.then(
                CommandManager.literal("entity")
                        .then(CommandManager.argument("entities", EntityArgumentType.entities())
                                .then(CommandManager.literal("query")
                                        .executes(context -> executeQuery(context.getSource(), entityCheck(EntityArgumentType.getEntities(context, "entities"), context.getSource())))
                                )
                                .then(CommandManager.literal("rate")
                                        .then(CommandManager.literal("reset").executes(context -> executeRate(context.getSource(), entityCheck(EntityArgumentType.getEntities(context, "entities"), context.getSource()), -1.0f)))
                                        .then(CommandManager.argument("rate", FloatArgumentType.floatArg(1.0F, 10000.0F))
                                                .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{DEFAULT_TICK_RATE_STRING,"reset"}, suggestionsBuilder))
                                                .executes(context -> executeRate(context.getSource(), entityCheck(EntityArgumentType.getEntities(context, "entities"), context.getSource()), FloatArgumentType.getFloat(context, "rate"))))
                                )
                                .then(CommandManager.literal("unfreeze")
                                        .executes(context -> executeFreeze(context.getSource(), entityCheck(EntityArgumentType.getEntities(context, "entities"), context.getSource()), false))
                                )
                                .then(CommandManager.literal("freeze")
                                        .executes(context -> executeFreeze(context.getSource(), entityCheck(EntityArgumentType.getEntities(context, "entities"), context.getSource()), true))
                                )
                                .then(CommandManager.literal("step")
                                        .executes(context -> executeStep(context.getSource(), entityCheck(EntityArgumentType.getEntities(context, "entities"), context.getSource()), 1))
                                        .then(CommandManager.literal("stop").executes(context -> executeStep(context.getSource(), entityCheck(EntityArgumentType.getEntities(context, "entities"), context.getSource()), 0)))
                                        .then(CommandManager.argument("time", TimeArgumentType.time(1))
                                                .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{"1t", "1s"}, suggestionsBuilder))
                                                .executes(context -> executeStep(context.getSource(), entityCheck(EntityArgumentType.getEntities(context, "entities"), context.getSource()), IntegerArgumentType.getInteger(context, "time"))))
                                )
                                .then(CommandManager.literal("sprint")
                                        .then(CommandManager.literal("stop").executes(context -> executeSprint(context.getSource(), entityCheck(EntityArgumentType.getEntities(context, "entities"), context.getSource()), 0)))
                                        .then(CommandManager.argument("time", TimeArgumentType.time(1))
                                                .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{"60s", "1d", "3d"}, suggestionsBuilder))
                                                .executes(context -> executeSprint(context.getSource(), entityCheck(EntityArgumentType.getEntities(context, "entities"), context.getSource()), IntegerArgumentType.getInteger(context, "time"))))
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
        return builder;
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
        TickRateEvents.SERVER_RATE.invoker().onServerRate(source.getServer(), roundRate);
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
        if (serverTickManager.isSprinting()) {
            source.sendFeedback(() -> Text.translatable("commands.tick.status.sprinting"), false);
        }
        else {
            if (serverTickManager.isFrozen()) {
                source.sendFeedback(() -> Text.translatable("commands.tick.status.frozen"), false);
            } else if (serverTickManager.getNanosPerTick() < source.getServer().getAverageNanosPerTick()) {
                source.sendFeedback(() -> Text.translatable("commands.tick.status.lagging"), false);
            } else {
                source.sendFeedback(() -> Text.translatable("commands.tick.status.running"), false);
            }
        }

        source.sendFeedback(() -> Text.literal("Server's target tick rate: " + serverTickManager.tickRate$getServerRate() + " per second (" + format((long)((double) TimeHelper.SECOND_IN_NANOS / (double)serverTickManager.tickRate$getServerRate())) + " mspt)"), false);
        source.sendFeedback(() -> Text.literal("Mainloop's target tick rate: " + serverTickManager.getTickRate() + " per second (" + format((long)((double) TimeHelper.SECOND_IN_NANOS / (double)serverTickManager.getTickRate())) + " mspt)"), false);

        long[] ls = Arrays.copyOf(source.getServer().getTickTimes(), source.getServer().getTickTimes().length);
        Arrays.sort(ls);
        String p50 = format(ls[ls.length / 2]);
        String p95 = format(ls[(int)((double)ls.length * 0.95)]);
        String p99 = format(ls[(int)((double)ls.length * 0.99)]);
        float avg = source.getServer().getAverageTickTime();
        source.sendFeedback(() -> Text.literal("Avg: %.1fms P50: %sms P95: %sms P99: %sms, sample: %s".formatted(avg,p50,p95,p99,ls.length)), false);
        return serverTickManager.tickRate$getServerRate();
    }

    @Inject(method = "executeSprint", at = @At("TAIL"))
    private static void executeSprint(ServerCommandSource source, int ticks, CallbackInfoReturnable<Integer> cir) {
        TickRateEvents.SERVER_SPRINT.invoker().onServerSprint(source.getServer(), ticks);
    }

    @Inject(method = "executeFreeze", at = @At("TAIL"))
    private static void executeFreeze(ServerCommandSource source, boolean frozen, CallbackInfoReturnable<Integer> cir) {
        TickRateEvents.SERVER_FREEZE.invoker().onServerFreeze(source.getServer(), frozen);
    }

    @Inject(method = "executeStep", at = @At("TAIL"))
    private static void executeStep(ServerCommandSource source, int ticks, CallbackInfoReturnable<Integer> cir) {
        TickRateEvents.SERVER_STEP.invoker().onServerStep(source.getServer(), ticks);
    }


    @Unique
    private static int executeRate(ServerCommandSource source, List<? extends AttachmentTarget> targets, float rate) {
        if(targets == null) return 0;

        int roundRate = Math.round(rate); // can't actually accept decimals
        ServerTickManager tickManager = source.getServer().getTickManager();
        tickManager.tickRate$setRate(roundRate, targets);

        String targetType;
        switch(targets.getFirst()) {
            case Entity ignored -> {
                targetType = "entities";
                targets.forEach(target -> TickRateEvents.ENTITY_RATE.invoker().onEntityRate((Entity) target, roundRate));
            }
            case WorldChunk ignored -> {
                targetType = "chunks";
                targets.forEach(target -> TickRateEvents.CHUNK_RATE.invoker().onChunkRate((WorldChunk) target, roundRate));
            }
            default -> throw new IllegalArgumentException("Unknown target type: " + targets.getFirst());
        }

        if(roundRate != 0) {
            source.sendFeedback(() -> Text.of("Set tick rate of " + targets.size() + " " + targetType + " to " + roundRate + " TPS."), false);
            return roundRate;
        }
        else {
            source.sendFeedback(() -> Text.literal("Reset the target rate of " + targets.size() + " " + targetType), false);
            return 0;
        }
    }

    @Unique
    private static int executeQuery(ServerCommandSource source, List<? extends AttachmentTarget> targets) {
        if(targets == null) return 0;

        ServerTickManager tickManager = source.getServer().getTickManager();
        int firstRate;
        String targetType;
        StringBuilder sb = new StringBuilder();
        switch(targets.getFirst()) {
            case Entity first -> {
                targetType = "entities";
                firstRate = tickManager.tickRate$getEntityRate(first);
                targets.forEach(e -> {
                    Entity entity = (Entity) e;
                    sb.append(entity.getType().getName()).append(" ").append(entity.getNameForScoreboard()).append(" - ").append(tickManager.tickRate$getEntityRate(entity)).append(" TPS").append("\n");
                });
            }
            case WorldChunk first -> {
                targetType = "chunks";
                firstRate = tickManager.tickRate$getChunkRate(first);
                targets.forEach(chunk -> {
                    WorldChunk worldChunk = (WorldChunk) chunk;
                    sb.append("Chunk ").append(worldChunk.getPos().toString()).append(" - ").append(tickManager.tickRate$getChunkRate(worldChunk)).append(" TPS").append("\n");
                });
            }
            default -> throw new IllegalArgumentException("Unknown target type: " + targets.getFirst());
        }

        sb.insert(0, "The tick rates of the specified " + targetType + " are as follows:\n");
        sb.deleteCharAt(sb.length()-1); // to remove last \n
        source.sendFeedback(() -> Text.of(sb.toString()), false);
        return firstRate;
    }

    @Unique
    private static int executeFreeze(ServerCommandSource source, List<? extends AttachmentTarget> targets, boolean frozen) {
        if(targets == null) return 0;

        ServerTickManager tickManager = source.getServer().getTickManager();
        tickManager.tickRate$setFrozen(frozen, targets);

        String targetType;
        switch(targets.getFirst()) {
            case Entity ignored -> {
                targetType = "entities";
                targets.forEach(entity -> TickRateEvents.ENTITY_FREEZE.invoker().onEntityFreeze((Entity) entity, frozen));
            }
            case WorldChunk ignored -> {
                targetType = "chunks";
                targets.forEach(chunk -> TickRateEvents.CHUNK_FREEZE.invoker().onChunkFreeze((WorldChunk) chunk, frozen));
            }
            default -> throw new IllegalArgumentException("Unknown target type: " + targets.getFirst());
        }

        source.sendFeedback(() -> Text.literal(targets.size() + " " + targetType + " have been " + (frozen ? "frozen." : "unfrozen.")), false);
        return 1;
    }

    @Unique
    private static int executeStep(ServerCommandSource source, List<? extends AttachmentTarget> targets, int steps) {
        if(targets == null) return 0;

        ServerTickManager tickManager = source.getServer().getTickManager();
        boolean success = tickManager.tickRate$step(steps, targets);

        String targetType;
        switch(targets.getFirst()) {
            case Entity ignored -> {
                targetType = "entities";
                if(success && steps != 0) targets.forEach(entity -> TickRateEvents.ENTITY_STEP.invoker().onEntityStep((Entity) entity, steps));
            }
            case WorldChunk ignored -> {
                targetType = "chunks";
                if(success && steps != 0) targets.forEach(chunk -> TickRateEvents.CHUNK_STEP.invoker().onChunkStep((WorldChunk) chunk, steps));
            }
            default -> throw new IllegalArgumentException("Unknown target type: " + targets.getFirst());
        }

        if(success) {
            if(steps != 0) source.sendFeedback(() -> Text.literal(targets.size() + " " + targetType + " will step " + steps + " ticks."), false);
            else source.sendFeedback(() -> Text.literal(targets.size() + " " + targetType + " have stopped stepping."), false);
        }
        else source.sendFeedback(() -> Text.literal("All of the specified " + targetType + " must be frozen first and cannot be sprinting!").withColor(Colors.LIGHT_RED), false);
        return success ? 1 : 0;
    }

    @Unique
    private static int executeSprint(ServerCommandSource source, List<? extends AttachmentTarget> targets, int ticks) {
        if(targets == null) return 0;

        ServerTickManager tickManager = source.getServer().getTickManager();
        boolean success = tickManager.tickRate$sprint(ticks, targets);

        String targetType;
        switch(targets.getFirst()) {
            case Entity ignored -> {
                targetType = "entities";
                if(success && ticks != 0) targets.forEach(entity -> TickRateEvents.ENTITY_SPRINT.invoker().onEntitySprint((Entity) entity, ticks));
            }
            case WorldChunk ignored -> {
                targetType = "chunks";
                if(success && ticks != 0) targets.forEach(chunk -> TickRateEvents.CHUNK_SPRINT.invoker().onChunkSprint((WorldChunk) chunk, ticks));
            }
            default -> throw new IllegalArgumentException("Unknown target type: " + targets.getFirst());
        }

        if(success) {
            if(ticks != 0) source.sendFeedback(() -> Text.literal(targets.size() + " " + targetType + " will sprint " + ticks + " ticks."), false);
            else source.sendFeedback(() -> Text.literal(targets.size() + " " + targetType + " have stopped sprinting."), false);
        }
        else source.sendFeedback(() -> Text.literal("All of the specified " + targetType + " must not be stepping!").withColor(Colors.LIGHT_RED), false);
        return success ? 1 : 0;
    }



    @Unique
    // returns NULL if any of the entities cannot be the command's target
    private static List<? extends Entity> entityCheck(Collection<? extends Entity> entities, ServerCommandSource source) {
        ServerTickManager tickManager = source.getServer().getTickManager();
        boolean match = entities.stream().anyMatch(e -> {
            if(e instanceof ServerPlayerEntity player) return !tickManager.tickRate$hasClientMod(player);
            else return false;
        });
        if(match) {
            source.sendFeedback(() -> Text.literal("Some of the specified entities are players that do not have TickRate mod installed on their client, so their tick rate cannot be manipulated.").withColor(Colors.LIGHT_RED), false);
            return null;
        }
        return (List<? extends Entity>) entities;
    }

    @Unique
    // returns NULL if any of the chunks cannot be the command's target (meaning they are unloaded)
    private static List<WorldChunk> chunkCheck(List<ChunkPos> chunks, ServerCommandSource source) {
        List<WorldChunk> worldChunks = new ArrayList<>();
        boolean match = chunks.stream().anyMatch(chunkPos -> {
            WorldChunk worldChunk = (WorldChunk) source.getWorld().getChunk(chunkPos.x,chunkPos.z,ChunkStatus.FULL,false);
            worldChunks.add(worldChunk);
            return worldChunk==null || worldChunk.getLevelType() == ChunkLevelType.INACCESSIBLE;
        });

        if(match) {
            source.sendFeedback(() -> Text.literal("Some of the specified chunks are not loaded!").withColor(Colors.LIGHT_RED), false);
            return null;
        }
        return worldChunks;
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
