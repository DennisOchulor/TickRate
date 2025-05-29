package io.github.dennisochulor.tickrate;

import static io.github.dennisochulor.tickrate.TickRateAttachments.*;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.tick.TickManager;

import java.util.HashMap;
import java.util.Map;

public class TickRateClientManager {

    private TickRateClientManager() {}

    private static boolean serverHasMod = false;
    private static final Map<Integer, TickDeltaInfo> entityCache = new HashMap<>();
    private static final Map<Long, TickDeltaInfo> chunkCache = new HashMap<>();

    // called in RenderTickCounterDynamicMixin
    public static void clearCache() {
        entityCache.clear();
        chunkCache.clear();
    }

    public static void setServerHasMod(boolean serverHasMod) {
        TickRateClientManager.serverHasMod = serverHasMod;
    }

    public static boolean serverHasMod() {
        return serverHasMod;
    }

    public static float getMillisPerServerTick() {
        return 1000.0f / getServerState().rate();
    }

    public static TickDeltaInfo getEntityTickDelta(Entity entity) {
        TickDeltaInfo info = entityCache.get(entity.getId());
        if(info != null) return info;

        RenderTickCounter renderTickCounter = MinecraftClient.getInstance().getRenderTickCounter();
        TickState serverState = getServerState();

        if(!serverHasMod) info = TickDeltaInfo.ofServer(false);
        else if(MinecraftClient.getInstance().isPaused()) info = TickDeltaInfo.NO_ANIMATE;
        else if(entity instanceof PlayerEntity && serverState.frozen()) info = TickDeltaInfo.ofServer(true); // tick freeze doesn't affect players
        else if(entity.hasVehicle()) info = getEntityTickDelta(entity.getRootVehicle());
        else {
            // client's own player OR entities where client player is a passenger can go above 20TPS limit
            boolean cappedAt20TPS = !(entity==MinecraftClient.getInstance().player) && !entity.hasPassenger(MinecraftClient.getInstance().player);
            TickState state = getEntityState(entity); // this also handles passenger entities
            if(state.sprinting()) // animate at max 20 TPS but for client player we don't know the TPS, so just say 100 :P
                info = cappedAt20TPS ? renderTickCounter.tickRate$getSpecificTickDeltaInfo(20) : renderTickCounter.tickRate$getClientPlayerTickDeltaInfo(100);
            else if(state.frozen() && !state.stepping()) info = TickDeltaInfo.NO_ANIMATE;
            else if(!cappedAt20TPS) info = renderTickCounter.tickRate$getClientPlayerTickDeltaInfo(state.rate());
            else info = renderTickCounter.tickRate$getSpecificTickDeltaInfo(state.rate());
        }

        entityCache.put(entity.getId(), info);
        return info;
    }

    public static TickDeltaInfo getChunkTickDelta(ChunkPos chunkPos) {
        TickDeltaInfo info = chunkCache.get(chunkPos.toLong());
        if(info != null) return info;

        RenderTickCounter renderTickCounter = MinecraftClient.getInstance().getRenderTickCounter();
        if(!serverHasMod) info = TickDeltaInfo.ofServer(false);
        else if(MinecraftClient.getInstance().isPaused()) info = TickDeltaInfo.NO_ANIMATE;
        else {
            TickState state = getChunkState(chunkPos);
            if(state.sprinting()) info = renderTickCounter.tickRate$getSpecificTickDeltaInfo(20); // animate at max 20 TPS
            else if(state.frozen() && !state.stepping()) info = TickDeltaInfo.NO_ANIMATE;
            else info = renderTickCounter.tickRate$getSpecificTickDeltaInfo(state.rate());
        }

        chunkCache.put(chunkPos.toLong(), info);
        return info;
    }

    public static TickState getEntityState(Entity entity) {
        if(entity.hasVehicle()) return getEntityState(entity.getRootVehicle()); // all passengers will follow TPS of the root entity
        TickState state = entity.getAttached(TICK_STATE);
        if(state == null) return getChunkState(entity.getChunkPos());

        int rate = state.rate();
        TickState serverState = getServerState();
        if(rate == -1) rate = getChunkState(entity.getChunkPos()).rate();
        if(serverState.frozen() || serverState.sprinting() || serverState.stepping())
            return serverState.withRate(serverState.stepping() ? serverState.rate() : rate);
        return state.withRate(rate);
    }

    /**
     * World is assumed to be the {@link MinecraftClient#world}
     */
    public static TickState getChunkState(ChunkPos chunkPos) {
        if(!serverHasMod) return getServerState();
        TickState state = MinecraftClient.getInstance().world.getChunk(chunkPos.x, chunkPos.z).getAttached(TICK_STATE);
        if(state == null) return getServerState();

        int rate = state.rate();
        TickState serverState = getServerState();
        if(state.rate() == -1) rate = serverState.rate();
        if(serverState.frozen() || serverState.sprinting() || serverState.stepping())
            return serverState.withRate(serverState.stepping() ? serverState.rate() : rate);
        return state.withRate(rate);
    }

    public static TickState getServerState() {
        if(!serverHasMod) {
            TickManager tickManager = MinecraftClient.getInstance().world.getTickManager();
            return new TickState((int) tickManager.getTickRate(),tickManager.isFrozen(),tickManager.isStepping(),false); // Client does not have any sprint info
        }
        return MinecraftClient.getInstance().world.getAttached(TICK_STATE_SERVER);
    }

}
