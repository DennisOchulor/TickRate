package io.github.dennisochulor.tickrate.mixin;

import io.github.dennisochulor.tickrate.TickRateTickManager;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickManager;
import net.minecraft.util.math.BlockPos;
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
    @Shadow @Final private MinecraftServer server;
    @Unique private File datafile;

    @Shadow public abstract void setTickRate(float tickRate);

    @Inject(method = "<init>(Lnet/minecraft/server/MinecraftServer;)V", at = @At("TAIL"))
    public void ServerTickManager(MinecraftServer server, CallbackInfo ci) {
        datafile = server.isDedicated() ? server.getRunDirectory().resolve("world/data/TickRateData.nbt").toFile() : server.getRunDirectory().resolve("saves/" + server.getSaveProperties().getLevelName() + "/data/TickRateData.nbt").toFile();
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
        Float tickRate = entities.get(entity.getUuidAsString());
        if(tickRate != null)
            return internalShouldTick(tickRate);
        else
            return tickRate$shouldTickChunk(entity.getWorld().getRegistryKey(),entity.getBlockPos());
    }

    public boolean tickRate$shouldTickChunk(RegistryKey<World> registryKey, BlockPos pos) {
        Float tickRate = chunks.get(registryKey.getRegistry().toString() + "-" + ChunkPos.toLong(pos));
        if(tickRate == null) // follow nominal rate
            return tickRate$shouldTickServer();
        else
            return internalShouldTick(tickRate);
    }

    public boolean tickRate$shouldTickServer() {
        return internalShouldTick(nominalTickRate);
    }

    public void tickRate$ticked() {
        ticks++;
        if(ticks > tickRate) ticks = 1;
    }

    public void tickRate$setEntityRate(float rate, Collection<? extends Entity> entities) {
        entities.forEach(e -> this.entities.put(e.getUuidAsString(), rate));
        updateFastestTicker();
    }

    public float tickRate$getEntityRate(Entity entity) {
        Float rate = entities.get(entity.getUuidAsString());
        if(rate != null) return rate;
        rate = chunks.get(entity.getWorld().getRegistryKey().getRegistry().toString() + "-" + ChunkPos.toLong(entity.getBlockPos()));
        if(rate != null) return rate;
        return nominalTickRate;
    }

    public void tickRate$setChunkRate(float rate, RegistryKey<World> registryKey, BlockPos pos) {
        chunks.put(registryKey.getRegistry().toString() + "-" + ChunkPos.toLong(pos), rate);
        updateFastestTicker();
    }

    public float tickRate$getChunkRate(RegistryKey<World> registryKey, BlockPos pos) {
        Float rate = chunks.get(registryKey.getRegistry().toString() + "-" + ChunkPos.toLong(pos));
        if(rate != null) return rate;
        return nominalTickRate;
    }


    // PRIVATE METHODS

    @Unique
    private boolean internalShouldTick(float tickRate) {
        double d = (this.tickRate-1)/(tickRate+1);
        if(tickRate == this.tickRate) return true;
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
        float fastest = tickRate;
        for(float rate : entities.values())
            fastest = Math.max(fastest,rate);
        for(float rate : chunks.values())
            fastest = Math.max(fastest,rate);
        if(fastest != tickRate) {
            setTickRate(fastest);
        }
    }


}
