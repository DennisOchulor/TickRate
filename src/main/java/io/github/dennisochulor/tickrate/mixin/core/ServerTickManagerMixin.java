package io.github.dennisochulor.tickrate.mixin.core;

import static io.github.dennisochulor.tickrate.TickRateAttachments.*;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.dennisochulor.tickrate.TickRate;
import io.github.dennisochulor.tickrate.injected_interface.TickRateTickManager;
import io.github.dennisochulor.tickrate.TickState;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.tick.TickManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Mixin(ServerTickManager.class)
public abstract class ServerTickManagerMixin extends TickManager implements TickRateTickManager {

    @Unique private int ticks = 0;
    @Unique private final Map<String,Boolean> ticked = new HashMap<>(); // someIdentifierString -> cachedShouldTick, needed to ensure TickState is only updated ONCE per mainloop tick
    @Unique private final SortedMap<Integer,Integer> tickers = new TreeMap<>(Comparator.reverseOrder()); // tickRate -> numberOfTickers, for fastest ticker tracking
    @Unique private final Set<UUID> playersWithMod = new HashSet<>(); // stores players that have this mod client-side
    @Unique private int sprintAvgTicksPerSecond = -1;
    @Unique private int numberOfIndividualSprints = 0;

    // migration stuff
    @Unique private File datafile;
    @Unique private final Map<String,Float> migrationData = new HashMap<>();
    @Unique private float nominalTickRateMigration = -1.0f;

    @Shadow public abstract void setTickRate(float tickRate);
    @Shadow @Final private MinecraftServer server;
    @Shadow private long scheduledSprintTicks;

    @Inject(method = "step", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerTickManager;sendStepPacket()V"))
    public void serverTickManager$step(int ticks, CallbackInfoReturnable<Boolean> cir) { // for server step start
        this.stepTicks++; // for some reason, the first tick is always skipped. so artificially add one :P
        TickState state = server.getOverworld().getAttached(TICK_STATE_SERVER);
        setTickRate(state.rate());
        TickState newState = state.withStepping(true);
        server.getWorlds().forEach(serverWorld -> serverWorld.setAttached(TICK_STATE_SERVER, newState));
    }

    @Inject(method = "stopStepping", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerTickManager;sendStepPacket()V"))
    public void stopStepping(CallbackInfoReturnable<Boolean> cir) { // for server step manual stop
        TickState newState = server.getOverworld().getAttached(TICK_STATE_SERVER).withStepping(false);
        server.getWorlds().forEach(serverWorld -> serverWorld.setAttached(TICK_STATE_SERVER, newState));
        updateFastestTicker();
    }

    @Override
    public void step() {
        this.shouldTick = !this.frozen || this.stepTicks > 0;
        if (this.stepTicks > 0) {
            this.stepTicks--;
            if(this.stepTicks == 0) { // for natural server step end
                updateFastestTicker();
                TickState newState = server.getOverworld().getAttached(TICK_STATE_SERVER).withStepping(false);
                server.getWorlds().forEach(serverWorld -> serverWorld.setAttached(TICK_STATE_SERVER, newState));            }
        }
    }

    @Inject(method = "startSprint", at = @At("TAIL"))
    public void startSprint(int ticks, CallbackInfoReturnable<Boolean> cir) { // for server sprint start
        TickState newState = server.getOverworld().getAttached(TICK_STATE_SERVER).withSprinting(true);
        server.getWorlds().forEach(serverWorld -> serverWorld.setAttached(TICK_STATE_SERVER, newState));
    }

    @Inject(method = "finishSprinting", at = @At("TAIL"))
    public void finishSprinting(CallbackInfo ci) { // for server sprint both manual/natural stop
        TickState newState = server.getOverworld().getAttached(TICK_STATE_SERVER).withSprinting(false);
        server.getWorlds().forEach(serverWorld -> serverWorld.setAttached(TICK_STATE_SERVER, newState));
    }

    @Inject(method = "setFrozen", at = @At("TAIL"))
    public void setFrozen(CallbackInfo ci, @Local(argsOnly = true) boolean frozen) { // for server (un)freeze
        TickState newState = server.getOverworld().getAttached(TICK_STATE_SERVER).withFrozen(frozen);
        server.getWorlds().forEach(serverWorld -> serverWorld.setAttached(TICK_STATE_SERVER, newState));
    }

    /**
     * @author Ninjaking312
     * @reason individual sprint or server sprint are both just sprint to code that doesn't know the difference
     */
    @Overwrite
    public boolean isSprinting() {
        return tickRate$isServerSprint() || tickRate$isIndividualSprint();
    }

    public void tickRate$serverStarting() {
        datafile = server.getSavePath(WorldSavePath.ROOT).resolve("data/TickRateData.nbt").toFile();
        if(datafile.exists()) {
            try {
                NbtCompound nbt = NbtIo.read(datafile.toPath());
                nominalTickRateMigration = nbt.getFloat("nominalTickRate").orElse(-1.0f);
                NbtOps.INSTANCE.getMap(nbt.get("entities")).getOrThrow().entries().forEach(pair -> {
                    String key = NbtOps.INSTANCE.getStringValue(pair.getFirst()).getOrThrow();
                    float value = NbtOps.INSTANCE.getNumberValue(pair.getSecond()).getOrThrow().floatValue();
                    migrationData.put(key,value);
                });
                NbtOps.INSTANCE.getMap(nbt.get("chunks")).ifSuccess(nbtElementMapLike -> {
                    nbtElementMapLike.entries().forEach(pair -> {
                        String key = NbtOps.INSTANCE.getStringValue(pair.getFirst()).getOrThrow();
                        float value = NbtOps.INSTANCE.getNumberValue(pair.getSecond()).getOrThrow().floatValue();
                        migrationData.put(key,value);
                    });
                });
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void tickRate$serverStarted() {
        // Have to attach TICK_STATE_SERVER to ALL worlds so client can sync it regardless of what world they are in :>
        TickState serverState = server.getOverworld().getAttachedOrCreate(TICK_STATE_SERVER);
        if(nominalTickRateMigration != -1.0f) serverState = serverState.withRate((int) nominalTickRateMigration);

        final TickState finalServerState = serverState;
        server.getWorlds().forEach(serverWorld -> serverWorld.setAttached(TICK_STATE_SERVER, finalServerState));
        updateTickersMap(finalServerState.rate(), 1);
    }

    public void tickRate$saveData() {
        if(!migrationData.isEmpty()) {
            NbtCompound nbt = new NbtCompound();
            var entitiesNbt = NbtOps.INSTANCE.mapBuilder();
            migrationData.forEach((k,v) -> entitiesNbt.add(k, NbtOps.INSTANCE.createFloat(v)));
            nbt.put("entities", entitiesNbt.build(NbtOps.INSTANCE.empty()).getOrThrow()); // chunks go in here too
            try {
                NbtIo.write(nbt,datafile.toPath());
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            if(datafile.exists()) datafile.delete();
        }
    }

    public void tickRate$addPlayerWithMod(ServerPlayerEntity player) {
        playersWithMod.add(player.getUuid());
    }

    public void tickRate$removePlayerWithMod(ServerPlayerEntity player) {
        playersWithMod.remove(player.getUuid());
    }

    public boolean tickRate$hasClientMod(ServerPlayerEntity player) {
        return playersWithMod.contains(player.getUuid());
    }


    public boolean tickRate$shouldTickEntity(Entity entity) {
        // check the ticked cache
        String key = entity.getUuidAsString();
        Boolean cachedShouldTick = ticked.get(key);
        if(cachedShouldTick != null) return cachedShouldTick;

        // check server overrides
        if(tickRate$isServerSprint()) return true;
        if(isFrozen()) {
            if(entity instanceof ServerPlayerEntity) return true;
            return isStepping();
        }


        TickState tickState = entity.getAttachedOrElse(TICK_STATE, TickState.DEFAULT);
        boolean shouldTick;

        if(tickState.sprinting()) {
            int sprintTicks = entity.getAttached(SPRINT_TICKS);
            if(sprintTicks == 0) {
                entity.removeAttached(SPRINT_TICKS);
                entity.modifyAttached(TICK_STATE, tickState1 -> tickState1.withSprinting(false));
                numberOfIndividualSprints--;
                shouldTick = false;
            }
            else {
                entity.modifyAttached(SPRINT_TICKS, sprintTicks1 -> --sprintTicks1);
                shouldTick = true;
            }
        }

        else if(tickState.frozen() && !tickState.stepping()) {
            shouldTick = false;
        }

        else { // stepping OR just regular ticking
            if(tickState.rate() != -1) shouldTick = internalShouldTick(tickState.rate());
            else shouldTick = tickRate$shouldTickChunk(entity.getWorld(), entity.getChunkPos());

            if(shouldTick && tickState.stepping()) {
                int stepTicks = entity.getAttached(STEP_TICKS);
                if(stepTicks == 0) {
                    entity.removeAttached(STEP_TICKS);
                    entity.modifyAttached(TICK_STATE, tickState1 -> tickState1.withStepping(false));
                }
                else {
                    entity.modifyAttached(STEP_TICKS, stepTicks1 -> --stepTicks1);
                }
            }
        }

        ticked.put(key, shouldTick);
        return shouldTick;
    }

    public boolean tickRate$shouldTickChunk(World world, ChunkPos chunkPos) {
        // check the ticked cache
        String key = world.getRegistryKey().getValue() + "-" + chunkPos.toLong();
        Boolean cachedShouldTick = ticked.get(key);
        if(cachedShouldTick != null) return cachedShouldTick;
        else {
            WorldChunk worldChunk = (WorldChunk) world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false);
            if(worldChunk == null) { // chunk is not fully loaded and accessible yet, so just don't tick it.
                ticked.put(key, false);
                return false;
            }
            else return tickRate$shouldTickChunk(worldChunk);
        }
    }

    public boolean tickRate$shouldTickChunk(WorldChunk chunk) {
        // check the ticked cache
        String key = chunk.getWorld().getRegistryKey().getValue() + "-" + chunk.getPos().toLong();
        Boolean cachedShouldTick = ticked.get(key);
        if(cachedShouldTick != null) return cachedShouldTick;

        // check server overrides
        if(tickRate$isServerSprint()) return true;
        if(isFrozen()) return isStepping();


        TickState tickState = chunk.getAttachedOrElse(TICK_STATE, TickState.DEFAULT);
        boolean shouldTick;

        if(tickState.sprinting()) {
            int sprintTicks = chunk.getAttached(SPRINT_TICKS);
            if(sprintTicks == 0) {
                chunk.removeAttached(SPRINT_TICKS);
                chunk.modifyAttached(TICK_STATE, tickState1 -> tickState1.withSprinting(false));
                numberOfIndividualSprints--;
                shouldTick = false;
            }
            else {
                chunk.modifyAttached(SPRINT_TICKS, sprintTicks1 -> --sprintTicks1);
                shouldTick = true;
            }
        }

        else if(tickState.frozen() && !tickState.stepping()) {
            shouldTick = false;
        }

        else { // stepping OR just regular ticking
            if(tickState.rate() != -1) shouldTick = internalShouldTick(tickState.rate());
            else shouldTick = tickRate$shouldTickServer();

            if(shouldTick && tickState.stepping()) {
                int stepTicks = chunk.getAttached(STEP_TICKS);
                if(stepTicks == 0) {
                    chunk.removeAttached(STEP_TICKS);
                    chunk.modifyAttached(TICK_STATE, tickState1 -> tickState1.withStepping(false));
                }
                else {
                    chunk.modifyAttached(STEP_TICKS, stepTicks1 -> --stepTicks1);
                }
            }
        }

        ticked.put(key, shouldTick);
        return shouldTick;
    }

    public boolean tickRate$shouldTickServer() {
        Boolean cachedShouldTick = ticked.get("server");
        if(cachedShouldTick != null) return cachedShouldTick;

        boolean shouldTick;
        if(tickRate$isServerSprint()) shouldTick = true;
        else if(isFrozen()) shouldTick = isStepping();
        else shouldTick = internalShouldTick(tickRate$getServerRate());

        ticked.put("server", shouldTick);
        return shouldTick;
    }

    public void tickRate$updateLoad(AttachmentTarget attachmentTarget, boolean loaded) {
        migrate(attachmentTarget);

        TickState tickState = attachmentTarget.getAttached(TICK_STATE);
        if(tickState == null) return;

        if(loaded) {
            if(tickState.sprinting()) numberOfIndividualSprints++;
            if(tickState.rate() != -1) updateTickersMap(tickState.rate(), 1);
            if(tickState.rate() > tickRate) updateFastestTicker();
        }
        else {
            if(tickState.sprinting()) numberOfIndividualSprints--;
            if(tickState.rate() != -1) updateTickersMap(tickState.rate(), -1);
            if(tickState.rate() == tickRate) updateFastestTicker();
        }
    }

    public void tickRate$setServerRate(int rate) {
        TickState prevState = server.getOverworld().getAttached(TICK_STATE_SERVER);
        TickState newState = prevState.withRate(rate);
        server.getWorlds().forEach(serverWorld -> serverWorld.setAttached(TICK_STATE_SERVER, newState));
        updateTickersMap(prevState.rate(), -1);
        updateTickersMap(rate, 1);
        updateFastestTicker();
    }

    public int tickRate$getServerRate() {
        return tickRate$getServerTickState().rate();
    }

    public TickState tickRate$getServerTickState() {
        return server.getOverworld().getAttached(TICK_STATE_SERVER);
    }

    public void tickRate$ticked() {
        if(tickRate$isIndividualSprint()) {
            ticks++;
            if(ticks > sprintAvgTicksPerSecond) {
                ticks = 1;
                sprintAvgTicksPerSecond = (int) (TimeHelper.SECOND_IN_NANOS / server.getAverageNanosPerTick());
            }
        }
        else {
            sprintAvgTicksPerSecond = -1;
            ticks++;
            if(ticks > tickRate) {
                ticks = 1;
            }
        }
        ticked.clear();
    }

    public boolean tickRate$isIndividualSprint() {
        return numberOfIndividualSprints > 0;
    }

    public boolean tickRate$isServerSprint() {
        return scheduledSprintTicks > 0L;
    }


    // rate == -1 for reset
    public void tickRate$setRate(int rate, Collection<? extends AttachmentTarget> targets) {
        targets.forEach(target -> target.modifyAttached(TICK_STATE, tickState -> {
            tickState = tickState==null ? TickState.DEFAULT : tickState;
            if(tickState.rate() != -1) updateTickersMap(tickState.rate(), -1);
            return tickState.withRate(rate);
        }));
        updateTickersMap(rate, targets.size());
        updateFastestTicker();
    }

    public void tickRate$setFrozen(boolean frozen, Collection<? extends AttachmentTarget> targets) {
        targets.forEach(target -> target.modifyAttached(TICK_STATE, tickState -> {
            tickState = tickState==null ? TickState.DEFAULT : tickState;
            tickState = tickState.withFrozen(frozen);
            if(tickState != null && frozen) tickState = tickState.withSprinting(false); // stop any sprints
            return tickState;
        }));
    }

    // steps == 0 for stop
    public boolean tickRate$step(int steps, Collection<? extends AttachmentTarget> targets) {
        boolean canStep = targets.stream().allMatch(target -> {
            TickState tickState = target.getAttachedOrElse(TICK_STATE, TickState.DEFAULT);
            return tickState.frozen() && !tickState.sprinting(); // must be frozen AND cannot be sprinting
        });

        if(canStep) {
            targets.forEach(target -> {
                target.modifyAttached(TICK_STATE, tickState -> {
                    tickState = tickState==null ? TickState.DEFAULT : tickState;
                    return tickState.withStepping(steps > 0);
                });
                target.setAttached(STEP_TICKS, steps > 0 ? steps : null);
            });
        }

        return canStep;
    }

    // ticks == 0 for stop
    public boolean tickRate$sprint(int ticks, Collection<? extends AttachmentTarget> targets) {
        // cannot be stepping
        boolean canSprint = targets.stream().noneMatch(target -> target.getAttachedOrElse(TICK_STATE, TickState.DEFAULT).stepping());

        if(canSprint) {
            targets.forEach(target -> {
                target.modifyAttached(TICK_STATE, tickState -> {
                    tickState = tickState==null ? TickState.DEFAULT : tickState;

                    if(tickState.sprinting() && ticks == 0) numberOfIndividualSprints--;
                    else if(!tickState.sprinting() && ticks > 0) numberOfIndividualSprints++;

                    return tickState.withSprinting(ticks > 0);
                });
                target.setAttached(SPRINT_TICKS, ticks > 0 ? ticks : null);
            });
        }

        return canSprint;
    }

    // get tick rates specialised methods

    public int tickRate$getEntityRate(Entity entity) {
        if(isStepping()) return tickRate$getServerRate(); // server step override
        if(entity.hasVehicle()) return tickRate$getEntityRate(entity.getRootVehicle()); //passengers follow tick rate of root vehicle

        int rate = entity.getAttachedOrElse(TICK_STATE, TickState.DEFAULT).rate();
        if(rate != -1) return rate;

        ChunkPos chunkPos = entity.getChunkPos();
        return tickRate$getChunkRate((WorldChunk) entity.getWorld().getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false));
    }

    public int tickRate$getChunkRate(WorldChunk chunk) {
        if(isStepping()) return tickRate$getServerRate(); // server step override

        int rate = chunk.getAttachedOrElse(TICK_STATE, TickState.DEFAULT).rate();
        if(rate != -1) return rate;
        return tickRate$getServerRate();
    }


    // get tick state specialised methods

    public TickState tickRate$getEntityTickStateShallow(Entity entity) {
        return entity.getAttachedOrElse(TICK_STATE, TickState.DEFAULT);
    }

    public TickState tickRate$getEntityTickStateDeep(Entity entity) {
        if(entity.hasVehicle()) return tickRate$getEntityTickStateDeep(entity.getRootVehicle()); // all passengers will follow TPS of the root entity
        TickState state = tickRate$getEntityTickStateShallow(entity);
        TickState serverState = tickRate$getServerTickState();

        if(state.rate() == -1) {
            TickState chunkState = tickRate$getChunkTickStateDeep(entity.getWorld(), entity.getChunkPos());
            if(state.equals(TickState.DEFAULT)) state = chunkState;
            else state = state.withRate(chunkState.rate());
        }
        if(serverState.frozen() || serverState.sprinting() || serverState.stepping())
            state = serverState.withRate(serverState.stepping() ? serverState.rate() : state.rate());

        return state;
    }

    public TickState tickRate$getChunkTickStateShallow(World world, ChunkPos chunkPos) {
        // try get the correct TickState as soon as it is available.
        Chunk chunk = world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_STARTS, false);
        return chunk==null ? TickState.DEFAULT : chunk.getAttachedOrElse(TICK_STATE, TickState.DEFAULT);
    }

    public TickState tickRate$getChunkTickStateDeep(World world, ChunkPos chunkPos) {
        TickState state = tickRate$getChunkTickStateShallow(world, chunkPos);
        int rate = state.rate();
        TickState serverState = tickRate$getServerTickState();

        if(state.rate() == -1) rate = serverState.rate();
        if(serverState.frozen() || serverState.sprinting() || serverState.stepping())
            return serverState.withRate(serverState.stepping() ? serverState.rate() : rate);
        return state.withRate(rate);
    }


    // PRIVATE METHODS

    @Unique
    private boolean internalShouldTick(int tickRate) {
        // attempt to evenly space out the exact number of ticks
        float fastestTickRate = tickRate$isIndividualSprint() ? sprintAvgTicksPerSecond : this.tickRate;

        double d = (fastestTickRate-1)/(tickRate+1);
        if(tickRate == fastestTickRate) return true;
        if(ticks == 1) return Math.ceil(1+(1*d)) == 1;

        double eventsToTick = (ticks-1)/d;
        if(eventsToTick >= tickRate) return Math.ceil(1+(tickRate*d)) == ticks;
        double floorEventToTick = Math.floor(eventsToTick);
        double ceilEventToTick = Math.ceil(eventsToTick);
        if(Math.ceil(1+(floorEventToTick*d)) == ticks) return true;
        return Math.ceil(1+(ceilEventToTick*d)) == ticks;
    }

    @Unique
    private void updateFastestTicker() {
        if(isStepping()) return;

        int fastest = tickers.firstKey();
        //TickRate.LOGGER.warn("fastest:{}, tickRate:{}", fastest, tickRate);
        if(fastest != tickRate) {
            setTickRate(fastest);
            ticks = 1; // reset it
        }
    }

    @Unique
    private void updateTickersMap(int rate, int change) {
        if(change > 0) tickers.merge(rate, change, Integer::sum);
        else if (change < 0) {
            tickers.compute(rate, (k,v) -> {
                if(v == null) throw new IllegalStateException("When removing rate from tickers map, " + rate + " TPS already 0");
                v += change;
                if(v < 0) throw new IllegalStateException("When removing rate from tickers map, " + rate + " TPS deducted to below 0 (" + v + ")");
                return v==0 ? null : v;
            });
        }
        else throw new IllegalArgumentException("change must not be 0");
    }

    @Unique
    private void migrate(AttachmentTarget target) {
        if(migrationData.isEmpty()) return;

        String key = switch (target) {
            case Entity e -> e.getUuidAsString();
            case WorldChunk worldChunk -> worldChunk.getWorld().getRegistryKey().getValue() + "-" + worldChunk.getPos().toLong();
            default -> "";
        };

        Float tickRate = migrationData.remove(key);
        if(tickRate != null) target.setAttached(TICK_STATE, TickState.ofRate(tickRate.intValue()));
    }

}
