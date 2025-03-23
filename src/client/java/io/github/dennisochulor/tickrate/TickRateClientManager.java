package io.github.dennisochulor.tickrate;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.tick.TickManager;

import java.util.HashMap;
import java.util.Map;

public class TickRateClientManager {

    private TickRateClientManager() {}

    private static boolean serverHasMod = false;
    private static TickState serverState;
    private static final Map<Integer,TickState> entities = new HashMap<>(); // only has entities for current client world
    private static final Map<Long,TickState> chunks = new HashMap<>(); // only has chunks for current client world
    private static final Map<Integer, TickProgressInfo> entityCache = new HashMap<>();
    private static final Map<Long, TickProgressInfo> chunkCache = new HashMap<>();

    public static void update(TickRateS2CUpdatePayload payload) {
        serverState = payload.server();
        entities.clear();
        chunks.clear();
        entities.putAll(payload.entities());
        chunks.putAll(payload.chunks());
    }

    // called in RenderTickCounterDynamicMixin
    public static void clearCache() {
        entityCache.clear();
        chunkCache.clear();
    }

    public static void setServerHasMod(boolean serverHasMod) {
        TickRateClientManager.serverHasMod = serverHasMod;
        if(!serverHasMod) {
            serverState = null;
            entities.clear();
            chunks.clear();
        }
    }

    public static boolean serverHasMod() {
        return serverHasMod;
    }

    public static float getMillisPerServerTick() {
        return 1000.0f / serverState.rate();
    }

    public static TickProgressInfo getEntityTickProgress(Entity entity) {
        TickProgressInfo info = entityCache.get(entity.getId());
        if(info != null) return info;

        RenderTickCounter renderTickCounter = MinecraftClient.getInstance().getRenderTickCounter();
        if(!serverHasMod) info = TickProgressInfo.ofServer(false);
        else if(MinecraftClient.getInstance().isPaused()) info = TickProgressInfo.NO_ANIMATE;
        else if(entity instanceof PlayerEntity && serverState.frozen()) info = TickProgressInfo.ofServer(true); // tick freeze doesn't affect players
        else if(entity.hasVehicle()) info = getEntityTickProgress(entity.getRootVehicle());
        else {
            // client's own player OR entities where client player is a passenger can go above 20TPS limit
            boolean cappedAt20TPS = !(entity==MinecraftClient.getInstance().player) && !entity.hasPassenger(MinecraftClient.getInstance().player);
            TickState state = getEntityState(entity); // this also handles passenger entities
            if(state.sprinting()) // animate at max 20 TPS but for client player we don't know the TPS, so just say 100 :P
                info = cappedAt20TPS ? renderTickCounter.tickRate$getSpecificTickProgressInfo(20) : renderTickCounter.tickRate$getClientPlayerTickProgressInfo(100);
            else if(state.frozen() && !state.stepping()) info = TickProgressInfo.NO_ANIMATE;
            else if(!cappedAt20TPS) info = renderTickCounter.tickRate$getClientPlayerTickProgressInfo((int) state.rate());
            else info = renderTickCounter.tickRate$getSpecificTickProgressInfo((int) state.rate());
        }

        entityCache.put(entity.getId(), info);
        return info;
    }

    public static TickProgressInfo getChunkTickProgress(long chunkPos) {
        TickProgressInfo info = chunkCache.get(chunkPos);
        if(info != null) return info;

        RenderTickCounter renderTickCounter = MinecraftClient.getInstance().getRenderTickCounter();
        if(!serverHasMod) info = TickProgressInfo.ofServer(false);
        else if(MinecraftClient.getInstance().isPaused()) info = TickProgressInfo.NO_ANIMATE;
        else {
            TickState state = getChunkState(chunkPos);
            if(state.sprinting()) info = renderTickCounter.tickRate$getSpecificTickProgressInfo(20); // animate at max 20 TPS
            else if(state.frozen() && !state.stepping()) info = TickProgressInfo.NO_ANIMATE;
            else info = renderTickCounter.tickRate$getSpecificTickProgressInfo((int) state.rate());
        }

        chunkCache.put(chunkPos, info);
        return info;
    }

    public static TickState getEntityState(Entity entity) {
        if(entity.hasVehicle()) return getEntityState(entity.getRootVehicle()); // all passengers will follow TPS of the root entity
        TickState state = entities.get(entity.getId());
        if(state == null) return getChunkState(entity.getChunkPos().toLong());

        float rate = state.rate();
        if(rate == -1.0f) rate = getChunkState(entity.getChunkPos().toLong()).rate();
        if(serverState.frozen() || serverState.sprinting() || serverState.stepping())
            return new TickState(serverState.stepping() ? serverState.rate() : rate,serverState.frozen(),serverState.stepping(),serverState.sprinting());
        return new TickState(rate,state.frozen(),state.stepping(),state.sprinting());
    }

    /**
     * World is assumed to be the {@link MinecraftClient#world}
     */
    public static TickState getChunkState(long chunkPos) {
        if(!serverHasMod) return getServerState();
        TickState state = chunks.get(chunkPos);
        if(state == null) return serverState;

        float rate = state.rate();
        if(state.rate() == -1.0f) rate = serverState.rate();
        if(serverState.frozen() || serverState.sprinting() || serverState.stepping())
            return new TickState(serverState.stepping() ? serverState.rate() : rate,serverState.frozen(),serverState.stepping(),serverState.sprinting());
        return new TickState(rate,state.frozen(),state.stepping(),state.sprinting());
    }

    public static TickState getServerState() {
        if(!serverHasMod) {
            TickManager tickManager = MinecraftClient.getInstance().world.getTickManager();
            return new TickState(tickManager.getTickRate(),tickManager.isFrozen(),tickManager.isStepping(),false); // Client does not have any sprint info
        }
        return serverState;
    }

}
