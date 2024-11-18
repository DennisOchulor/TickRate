package io.github.dennisochulor.tickrate.mixin;

import io.github.dennisochulor.tickrate.TickRateTickManager;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickManager;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.tick.TickManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Mixin(ServerTickManager.class)
public abstract class ServerTickManagerMixin extends TickManager implements TickRateTickManager {

    @Unique private float nominalTickRate = 20.0f;
    @Unique private int ticks = 0;
    @Unique private final Map<String,Float> entities = new HashMap<>(); // uuid -> tickRate
    @Unique private final Map<String,Float> chunks = new HashMap<>(); // world-longChunkPos -> tickRate
    @Unique private final Map<String,Integer> steps = new HashMap<>(); // uuid/world-longChunkPos -> steps, if steps==0, then it's frozen
    @Unique private final Map<String,Integer> sprinting = new HashMap<>(); // uuid/world-longChunkPos -> sprintTicks
    @Unique private int sprintAvgTicksPerSecond = -1;
    @Shadow @Final private MinecraftServer server;
    @Unique private File datafile;

    @Shadow public abstract void setTickRate(float tickRate);

    @Shadow public abstract boolean isSprinting();

    @Shadow public abstract boolean stopStepping();

    @Inject(method = "<init>(Lnet/minecraft/server/MinecraftServer;)V", at = @At("TAIL"))
    public void ServerTickManager(MinecraftServer server, CallbackInfo ci) {
        datafile = server.isDedicated() ? server.getRunDirectory().resolve("world/data/TickRateData.nbt").toFile() : server.getRunDirectory().resolve("saves/" + server.getSaveProperties().getLevelName() + "/data/TickRateData.nbt").toFile();
    }

    @Inject(method = "step", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerTickManager;sendStepPacket()V"))
    public void serverTickManager$step(int ticks, CallbackInfoReturnable<Boolean> cir) {
        setTickRate(nominalTickRate);
    }

    @Inject(method = "stopStepping", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerTickManager;sendStepPacket()V"))
    public void stopStepping(CallbackInfoReturnable<Boolean> cir) {
        updateFastestTicker();
    }

    @Override
    public void step() {
        this.shouldTick = !this.frozen || this.stepTicks > 0;
        if (this.stepTicks > 0) {
            if(this.stepTicks == 1) stopStepping();
            else this.stepTicks--;
        }
    }

    public void tickRate$serverStarted() {
        if(datafile.exists()) {
            try {
                NbtCompound nbt = NbtIo.read(datafile.toPath());
                nominalTickRate = nbt.getFloat("nominalTickRate");
                NbtOps.INSTANCE.getMap(nbt.get("entities")).getOrThrow().entries().forEach(pair -> {
                    entities.put(NbtOps.INSTANCE.getStringValue(pair.getFirst()).getOrThrow(),NbtOps.INSTANCE.getNumberValue(pair.getSecond()).getOrThrow().floatValue());
                });
                NbtOps.INSTANCE.getMap(nbt.get("chunks")).getOrThrow().entries().forEach(pair -> {
                    chunks.put(NbtOps.INSTANCE.getStringValue(pair.getFirst()).getOrThrow(), NbtOps.INSTANCE.getNumberValue(pair.getSecond()).getOrThrow().floatValue());
                });
                updateFastestTicker();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void tickRate$serverStopped() {
        NbtCompound nbt = new NbtCompound();
        nbt.putFloat("nominalTickRate",nominalTickRate);
        var entitiesNbt = NbtOps.INSTANCE.mapBuilder();
        entities.forEach((k,v) -> entitiesNbt.add(k,NbtOps.INSTANCE.createFloat(v)));
        var chunksNbt = NbtOps.INSTANCE.mapBuilder();
        chunks.forEach((k,v) -> chunksNbt.add(k,NbtOps.INSTANCE.createFloat(v)));
        nbt.put("entities", entitiesNbt.build(NbtOps.INSTANCE.empty()).getOrThrow());
        nbt.put("chunks", chunksNbt.build(NbtOps.INSTANCE.empty()).getOrThrow());
        try {
            NbtIo.write(nbt,datafile.toPath());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean tickRate$shouldTickEntity(Entity entity) {
        if(isSprinting()) return true;
        if(isFrozen()) return isStepping();

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
        if(isSprinting()) return true;
        if(isFrozen()) return isStepping();

        String key = world.getRegistryKey().getValue() + "-" + chunkPos;
        if(sprinting.computeIfPresent(key, (k,v) -> {
            if(v == 0) return null;
            return --v;
        }) != null) return true;

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

        return shouldTick;
    }

    public boolean tickRate$shouldTickServer() {
        if(isSprinting()) return true;
        if(isFrozen()) return isStepping();
        return internalShouldTick(nominalTickRate);
    }

    public void tickRate$setServerRate(float rate) {
        nominalTickRate = rate;
        updateFastestTicker();
    }

    public float tickRate$getServerRate() {
        return nominalTickRate;
    }

    public void tickRate$ticked() {
        if(!sprinting.isEmpty()) {
            ticks++;
            if(ticks > sprintAvgTicksPerSecond) {
                ticks = 1;
                sprintAvgTicksPerSecond = (int) (TimeHelper.SECOND_IN_NANOS / server.getAverageNanosPerTick());
            }
        }
        else {
            sprintAvgTicksPerSecond = -1;
            ticks++;
            if(ticks > tickRate) ticks = 1;
        }
    }

    public void tickRate$setEntityRate(float rate, Collection<? extends Entity> entities) {
        if(rate == 0) entities.forEach(e -> this.entities.remove(e.getUuidAsString()));
        else entities.forEach(e -> this.entities.put(e.getUuidAsString(), rate));
        updateFastestTicker();
    }

    public float tickRate$getEntityRate(Entity entity) {
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
        if(entities.stream().anyMatch(e -> !this.steps.containsKey(e.getUuidAsString()))) {
            return false; // some are not frozen, error
        }
        entities.forEach(e -> this.steps.put(e.getUuidAsString(), steps));
        return true;
    }

    public boolean tickRate$sprintEntity(int ticks, Collection<? extends Entity> entities) {
        if(entities.stream().anyMatch(e -> this.steps.containsKey(e.getUuidAsString()))) {
            return false; // some are frozen, error
        }
        entities.forEach(e -> this.sprinting.put(e.getUuidAsString(), ticks));
        return true;
    }

    public void tickRate$setChunkRate(float rate, World world, long chunkPos) {
        if(rate == 0) chunks.remove(world.getRegistryKey().getValue() + "-" + chunkPos);
        else chunks.put(world.getRegistryKey().getValue() + "-" + chunkPos, rate);
        updateFastestTicker();
    }

    public float tickRate$getChunkRate(World world, long chunkPos) {
        Float rate = chunks.get(world.getRegistryKey().getValue() + "-" + chunkPos);
        if(rate != null) return rate;
        return nominalTickRate;
    }

    public void tickRate$setChunkFrozen(boolean frozen, World world, long chunkPos) {
        String key = world.getRegistryKey().getValue() + "-" + chunkPos;
        if(frozen) {
            steps.put(key,0);
            if(sprinting.containsKey(key)) sprinting.put(key,0); // if sprinting, stop the sprint
        }
        else {
            steps.remove(key);
        }
    }

    public boolean tickRate$stepChunk(int steps, World world, long chunkPos) {
        String key = world.getRegistryKey().getValue() + "-" + chunkPos;
        if(!this.steps.containsKey(key)) return false; // not frozen, cannot step
        this.steps.put(key,steps);
        return true;
    }

    public boolean tickRate$sprintChunk(int ticks, World world, long chunkPos) {
        String key = world.getRegistryKey().getValue() + "-" + chunkPos;
        if(this.steps.containsKey(key)) return false; // frozen, cannot sprint
        this.sprinting.put(key,ticks);
        return true;
    }


    // PRIVATE METHODS

    @Unique
    private boolean internalShouldTick(float tickRate) {
        // attempt to evenly space out the exact number of ticks
        float fastestTickRate = sprintAvgTicksPerSecond==-1 ? this.tickRate : sprintAvgTicksPerSecond;

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


}
