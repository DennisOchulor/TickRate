package io.github.dennisochulor.tickrate.mixin;

import io.github.dennisochulor.tickrate.TickRateS2CUpdatePayload;
import io.github.dennisochulor.tickrate.injected_interface.TickRateTickManager;
import io.github.dennisochulor.tickrate.TickState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
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

    @Unique private float nominalTickRate = 20.0f;
    @Unique private int ticks = 0;
    @Unique private final Map<String,Float> entities = new HashMap<>(); // uuid -> tickRate
    @Unique private final Map<String,Float> chunks = new HashMap<>(); // world-longChunkPos -> tickRate
    @Unique private final Map<String,Float> unloadedEntities = new HashMap<>(); // uuid -> tickRate
    @Unique private final Map<String,Float> unloadedChunks = new HashMap<>(); // world-longChunkPos -> tickRate
    @Unique private final Map<String,Boolean> chunksTicked = new HashMap<>(); // world-longChunkPos -> hasTickedThisMainloopTick, needed to ensure ChunkTickState is only updated ONCE per mainloop tick
    @Unique private final Map<String,Integer> steps = new HashMap<>(); // uuid/world-longChunkPos -> steps, if steps==0, then it's frozen
    @Unique private final Map<String,Integer> sprinting = new HashMap<>(); // uuid/world-longChunkPos -> sprintTicks
    @Unique private final Set<ServerPlayerEntity> playersWithMod = new HashSet<>(); // stores players that have this mod client-side
    @Unique private int sprintAvgTicksPerSecond = -1;
    @Unique private File datafile;

    @Shadow public abstract void setTickRate(float tickRate);
    @Shadow @Final private MinecraftServer server;
    @Shadow private long scheduledSprintTicks;

    @Inject(method = "step", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerTickManager;sendStepPacket()V"))
    public void serverTickManager$step(int ticks, CallbackInfoReturnable<Boolean> cir) {
        this.stepTicks++; // for some reason, the first tick is always skipped. so artificially add one :P
        setTickRate(nominalTickRate);
    }

    @Inject(method = "stopStepping", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerTickManager;sendStepPacket()V"))
    public void stopStepping(CallbackInfoReturnable<Boolean> cir) {
        updateFastestTicker();
    }

    @Inject(method = "finishSprinting", at = @At("TAIL"))
    public void finishSprinting(CallbackInfo ci) {
        tickRate$sendUpdatePacket(); // tell client to stop sprinting
    }

    /**
     * @author Ninjaking312
     * @reason individual sprint or server sprint are both just sprint to code that doesn't know the difference
     */
    @Overwrite
    public boolean isSprinting() {
        return tickRate$isServerSprint() || tickRate$isIndividualSprint();
    }

    @Override
    public void step() {
        this.shouldTick = !this.frozen || this.stepTicks > 0;
        if (this.stepTicks > 0) {
            this.stepTicks--;
            if(this.stepTicks == 0) {
                updateFastestTicker();
                tickRate$sendUpdatePacket(); // tell client to stop stepping
            }
        }
    }

    public void tickRate$serverStarted() {
        datafile = server.isDedicated() ? server.getRunDirectory().resolve("world/data/TickRateData.nbt").toFile() : server.getRunDirectory().resolve("saves/" + server.getSaveProperties().getLevelName() + "/data/TickRateData.nbt").toFile();
        if(datafile.exists()) {
            try {
                NbtCompound nbt = NbtIo.read(datafile.toPath());
                nominalTickRate = nbt.getFloat("nominalTickRate");
                NbtOps.INSTANCE.getMap(nbt.get("entities")).getOrThrow().entries().forEach(pair -> {
                    String key = NbtOps.INSTANCE.getStringValue(pair.getFirst()).getOrThrow();
                    float value = NbtOps.INSTANCE.getNumberValue(pair.getSecond()).getOrThrow().floatValue();
                    unloadedEntities.put(key,value);
                });
                NbtOps.INSTANCE.getMap(nbt.get("chunks")).getOrThrow().entries().forEach(pair -> {
                    String key = NbtOps.INSTANCE.getStringValue(pair.getFirst()).getOrThrow();
                    float value = NbtOps.INSTANCE.getNumberValue(pair.getSecond()).getOrThrow().floatValue();
                    unloadedChunks.put(key,value);
                });
                updateFastestTicker();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void tickRate$saveData() {
        NbtCompound nbt = new NbtCompound();
        nbt.putFloat("nominalTickRate",nominalTickRate);
        var entitiesNbt = NbtOps.INSTANCE.mapBuilder();
        entities.forEach((k,v) -> entitiesNbt.add(k,NbtOps.INSTANCE.createFloat(v)));
        unloadedEntities.forEach((k,v) -> entitiesNbt.add(k,NbtOps.INSTANCE.createFloat(v)));
        var chunksNbt = NbtOps.INSTANCE.mapBuilder();
        chunks.forEach((k,v) -> chunksNbt.add(k,NbtOps.INSTANCE.createFloat(v)));
        unloadedChunks.forEach((k,v) -> chunksNbt.add(k,NbtOps.INSTANCE.createFloat(v)));
        nbt.put("entities", entitiesNbt.build(NbtOps.INSTANCE.empty()).getOrThrow());
        nbt.put("chunks", chunksNbt.build(NbtOps.INSTANCE.empty()).getOrThrow());
        try {
            NbtIo.write(nbt,datafile.toPath());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void tickRate$addPlayerWithMod(ServerPlayerEntity player) {
        playersWithMod.add(player);
    }

    public void tickRate$removePlayerWithMod(ServerPlayerEntity player) {
        playersWithMod.remove(player);
    }

    public boolean tickRate$hasClientMod(ServerPlayerEntity player) {
        return playersWithMod.contains(player);
    }

    public void tickRate$sendUpdatePacket() {
        TickState server = tickRate$getServerTickState();
        Map<String,TickState> entities1 = new HashMap<>();
        Map<String,TickState> chunks1 = new HashMap<>();
        this.entities.keySet().forEach(key -> entities1.put(key,getEntityTickState(key)));
        this.chunks.keySet().forEach(key -> chunks1.put(key,getChunkTickState(key)));
        this.steps.keySet().forEach(key -> { // for frozen stuff that has no specific rate
            if(key.contains(":")) chunks1.putIfAbsent(key, getChunkTickState(key)); // only chunk keys have : in them
            else entities1.putIfAbsent(key, getEntityTickState(key));
        });
        this.sprinting.keySet().forEach(key -> { // for sprinting stuff that has no specific rate
            if(key.contains(":")) chunks1.putIfAbsent(key, getChunkTickState(key)); // only chunk keys have : in them
            else entities1.putIfAbsent(key, getEntityTickState(key));
        });
        TickRateS2CUpdatePayload payload = new TickRateS2CUpdatePayload(server,entities1,chunks1);
        playersWithMod.forEach(player -> ServerPlayNetworking.send(player, payload));
    }

    public boolean tickRate$shouldTickEntity(Entity entity) {
        if(tickRate$isServerSprint()) return true;
        if(isFrozen()) {
            if(entity instanceof ServerPlayerEntity) return true;
            return isStepping();
        }
        //todo playersWithoutMod thingy

        if(sprinting.computeIfPresent(entity.getUuidAsString(), (k,v) -> {
            if(v == 0) return null;
            return --v;
        }) != null) return true;

        if(steps.getOrDefault(entity.getUuidAsString(),-1) == 0) return false;

        Float tickRate = entities.get(entity.getUuidAsString());
        boolean shouldTick;
        if(tickRate != null)
            shouldTick = internalShouldTick(tickRate);
        else
            shouldTick = tickRate$shouldTickChunk(entity.getWorld(),entity.getChunkPos().toLong());

        steps.computeIfPresent(entity.getUuidAsString(),(k,v) -> {
            if(v > 0 && shouldTick) return --v;
            return v;
        });

        return shouldTick;
    }

    public boolean tickRate$shouldTickChunk(World world, long chunkPos) {
        String key = world.getRegistryKey().getValue() + "-" + chunkPos;
        if(chunksTicked.get(key) != null) return chunksTicked.get(key);
        if(tickRate$isServerSprint()) return true;
        if(isFrozen()) return isStepping();

        if(sprinting.computeIfPresent(key, (k,v) -> {
            if(v == 0) return null;
            return --v;
        }) != null)
        {
            chunksTicked.put(key,true);
            return true;
        }

        if(steps.getOrDefault(key,-1) == 0) return false;

        Float tickRate = chunks.get(key);
        boolean shouldTick;
        if(tickRate == null) // follow nominal rate
            shouldTick = tickRate$shouldTickServer();
        else
            shouldTick = internalShouldTick(tickRate);

        steps.computeIfPresent(key,(k,v) -> {
            if(v > 0 && shouldTick) return --v;
            return v;
        });

        chunksTicked.put(key,shouldTick);
        return shouldTick;
    }

    public boolean tickRate$shouldTickServer() {
        if(tickRate$isServerSprint()) return true;
        if(isFrozen()) return isStepping();
        return internalShouldTick(nominalTickRate);
    }

    public void tickRate$updateChunkLoad(World world, long chunkPos, boolean loaded) {
        String key = world.getRegistryKey().getValue() + "-" + chunkPos;
        if(loaded) {
            Float rate = unloadedChunks.get(key);
            if(rate == null) return;
            unloadedChunks.remove(key);
            chunks.put(key,rate);
            if(rate > tickRate) updateFastestTicker();
        }
        else {
            Float rate = chunks.get(key);
            sprinting.remove(key); // just remove sprint if it unloads
            if(rate == null) return;
            chunks.remove(key);
            unloadedChunks.put(key,rate);
            if(rate == tickRate) updateFastestTicker();
        }
    }

    public void tickRate$updateEntityLoad(Entity entity, boolean loaded) {
        String key = entity.getUuidAsString();
        if(loaded) {
            Float rate = unloadedEntities.get(key);
            if(rate == null) return;
            unloadedEntities.remove(key);
            entities.put(key,rate);
            if(rate > tickRate) updateFastestTicker();
        }
        else {
            Float rate = entities.get(key);
            sprinting.remove(key); // just remove sprint if it unloads
            if(rate == null) return;

            Entity.RemovalReason reason = entity.getRemovalReason();
            if(reason != null) {
                switch (reason) {
                    case KILLED,DISCARDED -> tickRate$removeEntity(entity,!entity.isPlayer(),true,true);
                    case UNLOADED_TO_CHUNK,UNLOADED_WITH_PLAYER -> {
                        tickRate$removeEntity(entity,true,false,true);
                        unloadedEntities.put(key,rate);
                    }
                    case CHANGED_DIMENSION -> {} // NO-OP
                }
            }
            else {
                tickRate$removeEntity(entity,true,false,true); // removed for no reason?? wtf
                unloadedEntities.put(key,rate); // just have to save even if that's not the correct thing to do
            }
            if(rate == tickRate) updateFastestTicker();
        }
    }

    public void tickRate$setServerRate(float rate) {
        nominalTickRate = rate;
        updateFastestTicker();
    }

    public float tickRate$getServerRate() {
        return nominalTickRate;
    }

    public TickState tickRate$getServerTickState() {
        return new TickState(tickRate$getServerRate(),isFrozen(),isStepping(),tickRate$isServerSprint());
    }

    public void tickRate$ticked() {
        if(tickRate$isIndividualSprint()) {
            ticks++;
            if(ticks > sprintAvgTicksPerSecond) {
                ticks = 1;
                sprintAvgTicksPerSecond = (int) (TimeHelper.SECOND_IN_NANOS / server.getAverageNanosPerTick());
                tickRate$sendUpdatePacket();
            }
        }
        else {
            sprintAvgTicksPerSecond = -1;
            ticks++;
            if(ticks > tickRate) {
                ticks = 1;
                tickRate$sendUpdatePacket();
            }
        }
        chunksTicked.clear();
    }

    public boolean tickRate$isIndividualSprint() {
        return !sprinting.isEmpty();
    }

    public boolean tickRate$isServerSprint() {
        return scheduledSprintTicks > 0L;
    }

    public void tickRate$removeEntity(Entity entity, boolean rate, boolean steps, boolean sprint) {
        String uuid = entity.getUuidAsString();
        if(rate) entities.remove(uuid);
        if(steps) this.steps.remove(uuid);
        if(sprint) sprinting.remove(uuid);
    }

    public void tickRate$setEntityRate(float rate, Collection<? extends Entity> entities) {
        if(rate == 0) entities.forEach(e -> this.entities.remove(e.getUuidAsString()));
        else entities.forEach(e -> this.entities.put(e.getUuidAsString(), rate));
        updateFastestTicker();
    }

    public float tickRate$getEntityRate(Entity entity) {
        if(entity.hasVehicle()) return tickRate$getEntityRate(entity.getRootVehicle()); //passengers follow tick rate of root vehicle
        Float rate = entities.get(entity.getUuidAsString());
        if(rate != null) return rate;
        rate = chunks.get(entity.getWorld().getRegistryKey().getValue() + "-" + ChunkPos.toLong(entity.getBlockPos()));
        if(rate != null) return rate;
        return nominalTickRate;
    }

    public void tickRate$setEntityFrozen(boolean frozen, Collection<? extends Entity> entities) {
        if(frozen) {
            entities.forEach(e -> steps.put(e.getUuidAsString(),0));
            entities.forEach(e -> { // if sprinting, stop the sprint
               if(sprinting.containsKey(e.getUuidAsString())) sprinting.put(e.getUuidAsString(),0);
            });
        }
        else {
            entities.forEach(e -> steps.remove(e.getUuidAsString()));
        }
    }

    public boolean tickRate$stepEntity(int steps, Collection<? extends Entity> entities) {
        if(entities.stream().anyMatch(e -> !this.steps.containsKey(e.getUuidAsString()) || this.sprinting.containsKey(e.getUuidAsString()))) {
            return false; // some are not frozen or are sprinting, error
        }
        if(steps == 0) entities.forEach(e -> this.steps.put(e.getUuidAsString(), 0));
        else entities.forEach(e -> this.steps.put(e.getUuidAsString(), steps));
        return true;
    }

    public boolean tickRate$sprintEntity(int ticks, Collection<? extends Entity> entities) {
        if(entities.stream().anyMatch(e -> this.steps.getOrDefault(e.getUuidAsString(),-1) > 0)) {
            return false; // some are stepping, error
        }
        if(ticks == 0) entities.forEach(e -> this.sprinting.remove(e.getUuidAsString()));
        else entities.forEach(e -> this.sprinting.put(e.getUuidAsString(), ticks));
        return true;
    }

    public TickState tickRate$getEntityTickState(Entity entity) {
        String key = entity.getUuidAsString();
        return getEntityTickState(key);
    }

    public void tickRate$setChunkRate(float rate, World world, List<ChunkPos> chunks) {
        if(rate == 0) {
            chunks.forEach(chunkPos -> {
                String key = world.getRegistryKey().getValue() + "-" + chunkPos.toLong();
                this.chunks.remove(key);
            });
        }
        else {
            chunks.forEach(chunkPos -> this.chunks.put(world.getRegistryKey().getValue() + "-" + chunkPos.toLong(), rate));
        }
        updateFastestTicker();
    }

    public float tickRate$getChunkRate(World world, long chunkPos) {
        String key = world.getRegistryKey().getValue() + "-" + chunkPos;
        Float rate = chunks.get(key);
        if(rate != null) return rate;
        return nominalTickRate;
    }

    public void tickRate$setChunkFrozen(boolean frozen, World world, List<ChunkPos> chunks) {
        if(frozen) {
            chunks.forEach(chunkPos -> {
                String key = world.getRegistryKey().getValue() + "-" + chunkPos.toLong();
                steps.put(key,0);
                if(sprinting.containsKey(key)) sprinting.put(key,0); // if sprinting, stop the sprint
            });
        }
        else {
            chunks.forEach(chunkPos -> steps.remove(world.getRegistryKey().getValue() + "-" + chunkPos.toLong()));
        }
    }

    public boolean tickRate$stepChunk(int steps, World world, List<ChunkPos> chunks) {
        boolean error = chunks.stream().anyMatch(chunkPos -> {
            String key = world.getRegistryKey().getValue() + "-" + chunkPos.toLong();
            return !this.steps.containsKey(key) || this.sprinting.containsKey(key);
        });
        if(error) return false; // some are not frozen or are sprinting, error

        if(steps == 0) chunks.forEach(chunkPos -> this.steps.put(world.getRegistryKey().getValue() + "-" + chunkPos.toLong(), 0));
        else chunks.forEach(chunkPos -> this.steps.put(world.getRegistryKey().getValue() + "-" + chunkPos.toLong(), steps));
        return true;
    }

    public boolean tickRate$sprintChunk(int ticks, World world, List<ChunkPos> chunks) {
        if(chunks.stream().anyMatch(chunkPos -> this.steps.getOrDefault(world.getRegistryKey().getValue() + "-" + chunkPos.toLong(),-1) > 0))
            return false; // some are stepping, error

        if(ticks == 0) chunks.forEach(chunkPos -> this.sprinting.remove(world.getRegistryKey().getValue() + "-" + chunkPos.toLong()));
        else chunks.forEach(chunkPos -> this.sprinting.put(world.getRegistryKey().getValue() + "-" + chunkPos.toLong(), ticks));
        return true;
    }

    public TickState tickRate$getChunkTickState(World world, long chunkPos) {
        String key = world.getRegistryKey().getValue() + "-" + chunkPos;
        return getChunkTickState(key);
    }


    // PRIVATE METHODS

    @Unique
    private boolean internalShouldTick(float tickRate) {
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
        float fastest = 1.0f;
        fastest = Math.max(fastest, nominalTickRate);
        for(float rate : entities.values())
            fastest = Math.max(fastest,rate);
        for(float rate : chunks.values())
            fastest = Math.max(fastest,rate);
        if(fastest != tickRate) {
            setTickRate(fastest);
            ticks = 1; // reset it
        }
    }

    @Unique
    private TickState getChunkTickState(String key) {
        float rate = chunks.getOrDefault(key, -1.0f);
        boolean frozen = steps.containsKey(key);
        boolean stepping = frozen && steps.get(key) != 0;
        boolean sprinting = this.sprinting.containsKey(key);
        return new TickState(rate,frozen,stepping,sprinting);
    }

    @Unique
    private TickState getEntityTickState(String key) {
        float rate = entities.getOrDefault(key, -1.0f);
        boolean frozen = steps.containsKey(key);
        boolean stepping = frozen && steps.get(key) != 0;
        boolean sprinting = this.sprinting.containsKey(key);
        return new TickState(rate,frozen,stepping,sprinting);
    }


}
