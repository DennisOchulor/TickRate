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
import net.minecraft.text.Text;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

@Mixin(TickCommand.class)
public class TickCommandMixin {

    @Shadow private static final float MAX_TICK_RATE = 10000.0F;
    @Shadow private static final String DEFAULT_TICK_RATE_STRING = String.valueOf(20);

    @Shadow private static int executeRate(ServerCommandSource source, float rate) { return 0; }
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
                        .requires(source -> source.hasPermissionLevel(3))
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
                                                        .executes(context -> executeEntityQuery(context.getSource(), EntityArgumentType.getEntity(context, "entity")))))
                                        .then(CommandManager.literal("rate")
                                                .then(CommandManager.argument("entities", EntityArgumentType.entities())
                                                        .then(CommandManager.literal("reset").executes(context -> executeEntityRate(context.getSource(), EntityArgumentType.getEntities(context, "entities"), 0.0f)))
                                                        .then(CommandManager.argument("rate", FloatArgumentType.floatArg(1.0F, 10000.0F))
                                                                .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{DEFAULT_TICK_RATE_STRING,"reset"}, suggestionsBuilder))
                                                                .executes(context -> executeEntityRate(context.getSource(), EntityArgumentType.getEntities(context, "entities"), FloatArgumentType.getFloat(context, "rate")))))))
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
                                                                .executes(context -> executeChunkRate(context.getSource(), BlockPosArgumentType.getBlockPos(context, "blockPos"), FloatArgumentType.getFloat(context, "rate"))))))
                        )
        );
    }

    @Redirect(method = "executeRate", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerTickManager;setTickRate(F)V"))
    private static void executeRate$setTickRate(ServerTickManager instance, float tickRate) {
        TickRateTickManager tickManager = (TickRateTickManager) instance;
        tickManager.tickRate$setServerRate(tickRate);
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
        String string4 = format(ls[ls.length / 2]);
        String string5 = format(ls[(int)((double)ls.length * 0.95)]);
        String string6 = format(ls[(int)((double)ls.length * 0.99)]);
        source.sendFeedback(() -> Text.translatable("commands.tick.query.percentiles", string4, string5, string6, ls.length), false);
        return (int)f;
    }

    @Unique
    private static int executeChunkRate(ServerCommandSource source, BlockPos blockPos, float rate) {
        TickRateTickManager tickManager = (TickRateTickManager) source.getServer().getTickManager();
        tickManager.tickRate$setChunkRate(rate, source.getWorld(), ChunkPos.toLong(blockPos));
        String string = String.format(Locale.ROOT, "%.1f", rate);
        if(rate != 0) {
            source.sendFeedback(() -> Text.of("Successfully set target rate of the specified chunk to " + string), false);
            return (int) rate;
        }
        else {
            source.sendFeedback(() -> Text.literal("Reset the target rate of the specified chunk according to the server's tick rate."), false);
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
    private static int executeEntityRate(ServerCommandSource source, Collection<? extends Entity> entities, float rate) {
        TickRateTickManager tickManager = (TickRateTickManager) source.getServer().getTickManager();
        tickManager.tickRate$setEntityRate(rate, entities);
        String string = String.format(Locale.ROOT, "%.1f", rate);
        if(rate != 0) {
            source.sendFeedback(() -> Text.of("Successfully set target rate of the specified entities to " + string), false);
            return (int) rate;
        }
        else {
            source.sendFeedback(() -> Text.literal("Reset the target rate of the specified entities according to the server's tick rate."), false);
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

}
