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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.commands.TickCommand;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.util.CommonColors;
import net.minecraft.util.TimeUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(TickCommand.class)
public class TickCommandMixin {

    @Shadow @Final private static String DEFAULT_TICKRATE;
    @Shadow private static String nanosToMilisString(long nanos) { return ""; }

    // requires method
    @ModifyArg(method = "register", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/Commands;hasPermission(I)Lnet/minecraft/server/commands/PermissionCheck;"))
    private static int modifyPermissionLevel(int requiredLevel) {
        return 2;
    }

    // register the subcommands by modifying literal("tick") return value
    @ModifyExpressionValue(method = "register", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/Commands;literal(Ljava/lang/String;)Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;", args = "ldc=tick", ordinal = 0))
    private static LiteralArgumentBuilder<CommandSourceStack> register(LiteralArgumentBuilder<CommandSourceStack> builder) {
        LiteralCommandNode<CommandSourceStack> chunkQuery = Commands.literal("query")
                .executes(context -> executeQuery(context.getSource(), chunkCheck(getChunks(context,1), context.getSource()))).build();

        LiteralCommandNode<CommandSourceStack> chunkRate = Commands.literal("rate")
                .then(Commands.literal("reset").executes(context -> executeRate(context.getSource(), chunkCheck(getChunks(context,2), context.getSource()), -1.0f)))
                .then(Commands.argument("rate", FloatArgumentType.floatArg(1.0F, 10000.0F))
                        .suggests((context, suggestionsBuilder) -> SharedSuggestionProvider.suggest(new String[]{DEFAULT_TICKRATE,"reset"}, suggestionsBuilder))
                        .executes(context -> executeRate(context.getSource(), chunkCheck(getChunks(context,2), context.getSource()), FloatArgumentType.getFloat(context, "rate")))).build();

        LiteralCommandNode<CommandSourceStack> chunkUnfreeze = Commands.literal("unfreeze")
                .executes(context -> executeFreeze(context.getSource(), chunkCheck(getChunks(context,1), context.getSource()), false)).build();

        LiteralCommandNode<CommandSourceStack> chunkFreeze = Commands.literal("freeze")
                .executes(context -> executeFreeze(context.getSource(), chunkCheck(getChunks(context,1), context.getSource()), true)).build();

        LiteralCommandNode<CommandSourceStack> chunkStep = Commands.literal("step")
                .executes(context -> executeStep(context.getSource(), chunkCheck(getChunks(context,1), context.getSource()), 1))
                .then(Commands.literal("stop").executes(context -> executeStep(context.getSource(), chunkCheck(getChunks(context,2), context.getSource()), 0)))
                .then(Commands.argument("time", TimeArgument.time(1))
                        .suggests((context, suggestionsBuilder) -> SharedSuggestionProvider.suggest(new String[]{"1t", "1s"}, suggestionsBuilder))
                        .executes(context -> executeStep(context.getSource(), chunkCheck(getChunks(context,2), context.getSource()), IntegerArgumentType.getInteger(context, "time")))).build();

        LiteralCommandNode<CommandSourceStack> chunkSprint = Commands.literal("sprint")
                .then(Commands.literal("stop").executes(context -> executeSprint(context.getSource(), chunkCheck(getChunks(context,2), context.getSource()), 0)))
                .then(Commands.argument("time", TimeArgument.time(1))
                        .suggests((context, suggestionsBuilder) -> SharedSuggestionProvider.suggest(new String[]{"60s", "1d", "3d"}, suggestionsBuilder))
                        .executes(context -> executeSprint(context.getSource(), chunkCheck(getChunks(context,2), context.getSource()), IntegerArgumentType.getInteger(context, "time")))).build();

        builder.then(
                Commands.literal("entity")
                        .then(Commands.argument("entities", EntityArgument.entities())
                                .then(Commands.literal("query")
                                        .executes(context -> executeQuery(context.getSource(), entityCheck(EntityArgument.getEntities(context, "entities"), context.getSource())))
                                )
                                .then(Commands.literal("rate")
                                        .then(Commands.literal("reset").executes(context -> executeRate(context.getSource(), entityCheck(EntityArgument.getEntities(context, "entities"), context.getSource()), -1.0f)))
                                        .then(Commands.argument("rate", FloatArgumentType.floatArg(1.0F, 10000.0F))
                                                .suggests((context, suggestionsBuilder) -> SharedSuggestionProvider.suggest(new String[]{DEFAULT_TICKRATE,"reset"}, suggestionsBuilder))
                                                .executes(context -> executeRate(context.getSource(), entityCheck(EntityArgument.getEntities(context, "entities"), context.getSource()), FloatArgumentType.getFloat(context, "rate"))))
                                )
                                .then(Commands.literal("unfreeze")
                                        .executes(context -> executeFreeze(context.getSource(), entityCheck(EntityArgument.getEntities(context, "entities"), context.getSource()), false))
                                )
                                .then(Commands.literal("freeze")
                                        .executes(context -> executeFreeze(context.getSource(), entityCheck(EntityArgument.getEntities(context, "entities"), context.getSource()), true))
                                )
                                .then(Commands.literal("step")
                                        .executes(context -> executeStep(context.getSource(), entityCheck(EntityArgument.getEntities(context, "entities"), context.getSource()), 1))
                                        .then(Commands.literal("stop").executes(context -> executeStep(context.getSource(), entityCheck(EntityArgument.getEntities(context, "entities"), context.getSource()), 0)))
                                        .then(Commands.argument("time", TimeArgument.time(1))
                                                .suggests((context, suggestionsBuilder) -> SharedSuggestionProvider.suggest(new String[]{"1t", "1s"}, suggestionsBuilder))
                                                .executes(context -> executeStep(context.getSource(), entityCheck(EntityArgument.getEntities(context, "entities"), context.getSource()), IntegerArgumentType.getInteger(context, "time"))))
                                )
                                .then(Commands.literal("sprint")
                                        .then(Commands.literal("stop").executes(context -> executeSprint(context.getSource(), entityCheck(EntityArgument.getEntities(context, "entities"), context.getSource()), 0)))
                                        .then(Commands.argument("time", TimeArgument.time(1))
                                                .suggests((context, suggestionsBuilder) -> SharedSuggestionProvider.suggest(new String[]{"60s", "1d", "3d"}, suggestionsBuilder))
                                                .executes(context -> executeSprint(context.getSource(), entityCheck(EntityArgument.getEntities(context, "entities"), context.getSource()), IntegerArgumentType.getInteger(context, "time"))))
                                )
                        )
                )
                .then(
                        Commands.literal("chunk")
                                .then(Commands.argument("from", ColumnPosArgument.columnPos())
                                        .then(chunkQuery)
                                        .then(chunkRate)
                                        .then(chunkUnfreeze)
                                        .then(chunkFreeze)
                                        .then(chunkStep)
                                        .then(chunkSprint)
                                        .then(Commands.argument("to", ColumnPosArgument.columnPos())
                                                .then(chunkQuery)
                                                .then(chunkRate)
                                                .then(chunkUnfreeze)
                                                .then(chunkFreeze)
                                                .then(chunkStep)
                                                .then(chunkSprint)
                                        )
                                        .then(Commands.literal("radius")
                                                .then(Commands.argument("radius", FloatArgumentType.floatArg(1))
                                                        .then(chunkQuery)
                                                        .then(chunkRate)
                                                        .then(chunkUnfreeze)
                                                        .then(chunkFreeze)
                                                        .then(chunkStep)
                                                        .then(chunkSprint)
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
    private static int setTickingRate(CommandSourceStack source, float rate) {
        int roundRate = Math.round(rate); // can't actually accept decimals
        ServerTickRateManager tickManager = source.getServer().tickRateManager();
        tickManager.tickRate$setServerRate(roundRate);
        TickRateEvents.SERVER_RATE.invoker().onServerRate(source.getServer(), roundRate);
        source.sendSuccess(() -> Component.translatable("commands.tick.rate.success", roundRate), true);
        return roundRate;
    }

    /**
     * @author Ninjaking312
     * @reason To add distinction between mainloop and server tick rate
     */
    @Overwrite
    private static int tickQuery(CommandSourceStack source) {
        ServerTickRateManager serverTickManager = source.getServer().tickRateManager();
        if (serverTickManager.isSprinting()) {
            source.sendSuccess(() -> Component.translatable("commands.tick.status.sprinting"), false);
        }
        else {
            if (serverTickManager.isFrozen()) {
                source.sendSuccess(() -> Component.translatable("commands.tick.status.frozen"), false);
            } else if (serverTickManager.nanosecondsPerTick() < source.getServer().getAverageTickTimeNanos()) {
                source.sendSuccess(() -> Component.translatable("commands.tick.status.lagging"), false);
            } else {
                source.sendSuccess(() -> Component.translatable("commands.tick.status.running"), false);
            }
        }

        source.sendSuccess(() -> Component.literal("Server's target tick rate: " + serverTickManager.tickRate$getServerRate() + " per second (" + nanosToMilisString((long)((double) TimeUtil.NANOSECONDS_PER_SECOND / (double)serverTickManager.tickRate$getServerRate())) + " mspt)"), false);
        source.sendSuccess(() -> Component.literal("Mainloop's target tick rate: " + Math.round(serverTickManager.tickrate()) + " per second (" + nanosToMilisString((long)((double) TimeUtil.NANOSECONDS_PER_SECOND / (double)serverTickManager.tickrate())) + " mspt)"), false);

        long[] ls = Arrays.copyOf(source.getServer().getTickTimesNanos(), source.getServer().getTickTimesNanos().length);
        Arrays.sort(ls);
        String p50 = nanosToMilisString(ls[ls.length / 2]);
        String p95 = nanosToMilisString(ls[(int)((double)ls.length * 0.95)]);
        String p99 = nanosToMilisString(ls[(int)((double)ls.length * 0.99)]);
        float avg = source.getServer().getCurrentSmoothedTickTime();
        source.sendSuccess(() -> Component.literal("Avg: %.1fms P50: %sms P95: %sms P99: %sms, sample: %s".formatted(avg,p50,p95,p99,ls.length)), false);
        return serverTickManager.tickRate$getServerRate();
    }

    @Inject(method = "sprint", at = @At("TAIL"))
    private static void executeSprint(CommandSourceStack source, int ticks, CallbackInfoReturnable<Integer> cir) {
        TickRateEvents.SERVER_SPRINT.invoker().onServerSprint(source.getServer(), ticks);
    }

    @Inject(method = "setFreeze", at = @At("TAIL"))
    private static void executeFreeze(CommandSourceStack source, boolean frozen, CallbackInfoReturnable<Integer> cir) {
        TickRateEvents.SERVER_FREEZE.invoker().onServerFreeze(source.getServer(), frozen);
    }

    @Inject(method = "step", at = @At("TAIL"))
    private static void executeStep(CommandSourceStack source, int ticks, CallbackInfoReturnable<Integer> cir) {
        TickRateEvents.SERVER_STEP.invoker().onServerStep(source.getServer(), ticks);
    }


    @Unique
    private static int executeRate(CommandSourceStack source, List<? extends AttachmentTarget> targets, float rate) {
        if(targets == null) return 0;

        int roundRate = Math.round(rate); // can't actually accept decimals
        ServerTickRateManager tickManager = source.getServer().tickRateManager();
        tickManager.tickRate$setRate(roundRate, targets);

        String targetType;
        switch(targets.getFirst()) {
            case Entity ignored -> {
                targetType = "entities";
                targets.forEach(target -> TickRateEvents.ENTITY_RATE.invoker().onEntityRate((Entity) target, roundRate==-1 ? 0 : roundRate));
            }
            case LevelChunk ignored -> {
                targetType = "chunks";
                targets.forEach(target -> TickRateEvents.CHUNK_RATE.invoker().onChunkRate((LevelChunk) target, roundRate==-1 ? 0 : roundRate));
            }
            default -> throw new IllegalArgumentException("Unknown target type: " + targets.getFirst());
        }

        if(roundRate != -1) {
            source.sendSuccess(() -> Component.nullToEmpty("Set tick rate of " + targets.size() + " " + targetType + " to " + roundRate + " TPS."), false);
            return roundRate;
        }
        else {
            source.sendSuccess(() -> Component.literal("Reset the target rate of " + targets.size() + " " + targetType), false);
            return 0;
        }
    }

    @Unique
    private static int executeQuery(CommandSourceStack source, List<? extends AttachmentTarget> targets) {
        if(targets == null) return 0;

        ServerTickRateManager tickManager = source.getServer().tickRateManager();
        int firstRate;
        String targetType;
        StringBuilder sb = new StringBuilder();
        switch(targets.getFirst()) {
            case Entity first -> {
                targetType = "entities";
                firstRate = tickManager.tickRate$getEntityRate(first);
                targets.forEach(e -> {
                    Entity entity = (Entity) e;
                    sb.append(entity.getType().getDescription().getString()).append(" ").append(entity.getScoreboardName()).append(" - ").append(tickManager.tickRate$getEntityRate(entity)).append(" TPS").append("\n");
                });
            }
            case LevelChunk first -> {
                targetType = "chunks";
                firstRate = tickManager.tickRate$getChunkRate(first);
                targets.forEach(chunk -> {
                    LevelChunk levelChunk = (LevelChunk) chunk;
                    sb.append("Chunk ").append(levelChunk.getPos().toString()).append(" - ").append(tickManager.tickRate$getChunkRate(levelChunk)).append(" TPS").append("\n");
                });
            }
            default -> throw new IllegalArgumentException("Unknown target type: " + targets.getFirst());
        }

        sb.insert(0, "The tick rates of the specified " + targetType + " are as follows:\n");
        sb.deleteCharAt(sb.length()-1); // to remove last \n
        source.sendSuccess(() -> Component.nullToEmpty(sb.toString()), false);
        return firstRate;
    }

    @Unique
    private static int executeFreeze(CommandSourceStack source, List<? extends AttachmentTarget> targets, boolean frozen) {
        if(targets == null) return 0;

        ServerTickRateManager tickManager = source.getServer().tickRateManager();
        tickManager.tickRate$setFrozen(frozen, targets);

        String targetType;
        switch(targets.getFirst()) {
            case Entity ignored -> {
                targetType = "entities";
                targets.forEach(entity -> TickRateEvents.ENTITY_FREEZE.invoker().onEntityFreeze((Entity) entity, frozen));
            }
            case LevelChunk ignored -> {
                targetType = "chunks";
                targets.forEach(chunk -> TickRateEvents.CHUNK_FREEZE.invoker().onChunkFreeze((LevelChunk) chunk, frozen));
            }
            default -> throw new IllegalArgumentException("Unknown target type: " + targets.getFirst());
        }

        source.sendSuccess(() -> Component.literal(targets.size() + " " + targetType + " have been " + (frozen ? "frozen." : "unfrozen.")), false);
        return 1;
    }

    @Unique
    private static int executeStep(CommandSourceStack source, List<? extends AttachmentTarget> targets, int steps) {
        if(targets == null) return 0;

        ServerTickRateManager tickManager = source.getServer().tickRateManager();
        boolean success = tickManager.tickRate$step(steps, targets);

        String targetType;
        switch(targets.getFirst()) {
            case Entity ignored -> {
                targetType = "entities";
                if(success && steps != 0) targets.forEach(entity -> TickRateEvents.ENTITY_STEP.invoker().onEntityStep((Entity) entity, steps));
            }
            case LevelChunk ignored -> {
                targetType = "chunks";
                if(success && steps != 0) targets.forEach(chunk -> TickRateEvents.CHUNK_STEP.invoker().onChunkStep((LevelChunk) chunk, steps));
            }
            default -> throw new IllegalArgumentException("Unknown target type: " + targets.getFirst());
        }

        if(success) {
            if(steps != 0) source.sendSuccess(() -> Component.literal(targets.size() + " " + targetType + " will step " + steps + " ticks."), false);
            else source.sendSuccess(() -> Component.literal(targets.size() + " " + targetType + " have stopped stepping."), false);
        }
        else source.sendSuccess(() -> Component.literal("All of the specified " + targetType + " must be frozen first and cannot be sprinting!").withColor(CommonColors.SOFT_RED), false);
        return success ? 1 : 0;
    }

    @Unique
    private static int executeSprint(CommandSourceStack source, List<? extends AttachmentTarget> targets, int ticks) {
        if(targets == null) return 0;

        ServerTickRateManager tickManager = source.getServer().tickRateManager();
        boolean success = tickManager.tickRate$sprint(ticks, targets);

        String targetType;
        switch(targets.getFirst()) {
            case Entity ignored -> {
                targetType = "entities";
                if(success && ticks != 0) targets.forEach(entity -> TickRateEvents.ENTITY_SPRINT.invoker().onEntitySprint((Entity) entity, ticks));
            }
            case LevelChunk ignored -> {
                targetType = "chunks";
                if(success && ticks != 0) targets.forEach(chunk -> TickRateEvents.CHUNK_SPRINT.invoker().onChunkSprint((LevelChunk) chunk, ticks));
            }
            default -> throw new IllegalArgumentException("Unknown target type: " + targets.getFirst());
        }

        if(success) {
            if(ticks != 0) source.sendSuccess(() -> Component.literal(targets.size() + " " + targetType + " will sprint " + ticks + " ticks."), false);
            else source.sendSuccess(() -> Component.literal(targets.size() + " " + targetType + " have stopped sprinting."), false);
        }
        else source.sendSuccess(() -> Component.literal("All of the specified " + targetType + " must not be stepping!").withColor(CommonColors.SOFT_RED), false);
        return success ? 1 : 0;
    }



    @Unique
    // returns NULL if any of the entities cannot be the command's target
    private static List<? extends Entity> entityCheck(Collection<? extends Entity> entities, CommandSourceStack source) {
        return (List<? extends Entity>) entities;
    }

    @Unique
    // returns NULL if any of the chunks cannot be the command's target (meaning they are unloaded)
    private static List<LevelChunk> chunkCheck(List<ChunkPos> chunks, CommandSourceStack source) {
        List<LevelChunk> levelChunks = new ArrayList<>();
        boolean match = chunks.stream().anyMatch(chunkPos -> {
            LevelChunk levelChunk = (LevelChunk) source.getLevel().getChunk(chunkPos.x,chunkPos.z,ChunkStatus.FULL,false);
            levelChunks.add(levelChunk);
            return levelChunk==null || levelChunk.getFullStatus() == FullChunkStatus.INACCESSIBLE;
        });

        if(match) {
            source.sendSuccess(() -> Component.literal("Some of the specified chunks are not loaded!").withColor(CommonColors.SOFT_RED), false);
            return null;
        }
        return levelChunks;
    }

    /**
     * @param depth number of steps back up the command tree to get to the node right before the chunkOperations
     */
    @Unique
    private static List<ChunkPos> getChunks(CommandContext<CommandSourceStack> context, int depth) throws CommandSyntaxException {
        // CommandContext#getArgument is not used because it throws an Exception when not found, which is not great for performance
        String lastNode = context.getNodes().get(context.getNodes().size() - depth - 1).getNode().getName();
        return switch(lastNode) {
            case "from" -> {
                ColumnPos from = ColumnPosArgument.getColumnPos(context, "from");
                if (from.x() < -30000000 || from.z() < -30000000 || from.x() >= 30000000 || from.z() >= 30000000)
                    throw BlockPosArgument.ERROR_OUT_OF_WORLD.create();
                yield List.of(from.toChunkPos());
            }
            case "to" -> {
                // logic taken from ForceLoadCommand :)
                ColumnPos from = ColumnPosArgument.getColumnPos(context, "from");
                ColumnPos to = ColumnPosArgument.getColumnPos(context, "to");
                int minX = Math.min(from.x(), to.x());
                int minZ = Math.min(from.z(), to.z());
                int maxX = Math.max(from.x(), to.x());
                int maxZ = Math.max(from.z(), to.z());
                if (minX < -30000000 || minZ < -30000000 || maxX >= 30000000 || maxZ >= 30000000)
                    throw BlockPosArgument.ERROR_OUT_OF_WORLD.create();

                int chunkMinX = SectionPos.blockToSectionCoord(minX);
                int chunkMinZ = SectionPos.blockToSectionCoord(minZ);
                int chunkMaxX = SectionPos.blockToSectionCoord(maxX);
                int chunkMaxZ = SectionPos.blockToSectionCoord(maxZ);
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
                ColumnPos circleCentre = ColumnPosArgument.getColumnPos(context, "from");
                float radius = FloatArgumentType.getFloat(context, "radius");
                float minX = circleCentre.x() - radius;
                float minZ = circleCentre.z() - radius;
                float maxX = circleCentre.x() + radius;
                float maxZ = circleCentre.z() + radius;
                if (minX < -30000000 || minZ < -30000000 || maxX >= 30000000 || maxZ >= 30000000)
                    throw BlockPosArgument.ERROR_OUT_OF_WORLD.create();

                int chunkMinX = SectionPos.posToSectionCoord(minX);
                int chunkMinZ = SectionPos.posToSectionCoord(minZ);
                int chunkMaxX = SectionPos.posToSectionCoord(maxX);
                int chunkMaxZ = SectionPos.posToSectionCoord(maxZ);
                List<ChunkPos> chunks = new ArrayList<>();
                for(int chunkX = chunkMinX; chunkX <= chunkMaxX; chunkX++) {
                    for(int chunkZ = chunkMinZ; chunkZ <= chunkMaxZ; chunkZ++) {
                        // https://www.geeksforgeeks.org/check-if-any-point-overlaps-the-given-circle-and-rectangle/
                        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                        SectionPos chunkSectionPos = SectionPos.of(chunkPos,0);
                        int X1 = chunkSectionPos.minBlockX();
                        int X2 = chunkSectionPos.maxBlockX();
                        int Z1 = chunkSectionPos.minBlockZ();
                        int Z2 = chunkSectionPos.maxBlockZ();
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
