package io.github.dennisochulor.tickrate.client;

import static io.github.dennisochulor.tickrate.TickRateAttachments.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.github.dennisochulor.tickrate.TickState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class TickRateClientManager {

    private TickRateClientManager() {}

    private static boolean serverHasMod = false;
    private static final Map<Integer, DeltaTrackerInfo> entityCache = new HashMap<>();
    private static final Map<Long, DeltaTrackerInfo> chunkCache = new HashMap<>();

    // called in DeltaTracker$TimerMixin
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

    public static DeltaTrackerInfo getEntityDeltaTrackerInfo(Entity entity) {
        DeltaTrackerInfo info = entityCache.get(entity.getId());
        if(info != null) return info;

        Minecraft minecraft = Minecraft.getInstance();
        DeltaTracker deltaTracker = minecraft.getDeltaTracker();
        TickState serverState = getServerState();

        if(!serverHasMod) info = DeltaTrackerInfo.ofServer(false);
        else if(minecraft.isPaused()) info = DeltaTrackerInfo.NO_ANIMATE;
        else if(entity instanceof Player && serverState.frozen()) info = DeltaTrackerInfo.ofServer(true); // tick freeze doesn't affect players
        else if(entity.isPassenger()) info = getEntityDeltaTrackerInfo(entity.getRootVehicle());
        else {
            // client's own player OR entities where client player is a passenger can go above 20TPS limit
            boolean cappedAt20TPS = !(entity==minecraft.player) && !entity.hasPassenger(Objects.requireNonNull(minecraft.player));
            TickState state = getEntityState(entity); // this also handles passenger entities
            if(state.sprinting()) // animate at max 20 TPS but for client player we don't know the TPS, so just say 100 :P
                info = cappedAt20TPS ? deltaTracker.tickRate$getDeltaTrackerInfo(20) : deltaTracker.tickRate$getClientPlayerDeltaTrackerInfo(100);
            else if(state.frozen() && !state.stepping()) info = DeltaTrackerInfo.NO_ANIMATE;
            else if(!cappedAt20TPS) info = deltaTracker.tickRate$getClientPlayerDeltaTrackerInfo(state.rate());
            else info = deltaTracker.tickRate$getDeltaTrackerInfo(state.rate());
        }

        entityCache.put(entity.getId(), info);
        return info;
    }

    public static DeltaTrackerInfo getChunkDeltaTrackerInfo(ChunkPos chunkPos) {
        DeltaTrackerInfo info = chunkCache.get(chunkPos.pack());
        if(info != null) return info;

        DeltaTracker deltaTracker = Minecraft.getInstance().getDeltaTracker();
        if(!serverHasMod) info = DeltaTrackerInfo.ofServer(false);
        else if(Minecraft.getInstance().isPaused()) info = DeltaTrackerInfo.NO_ANIMATE;
        else {
            TickState state = getChunkState(chunkPos);
            if(state.sprinting()) info = deltaTracker.tickRate$getDeltaTrackerInfo(20); // animate at max 20 TPS
            else if(state.frozen() && !state.stepping()) info = DeltaTrackerInfo.NO_ANIMATE;
            else info = deltaTracker.tickRate$getDeltaTrackerInfo(state.rate());
        }

        chunkCache.put(chunkPos.pack(), info);
        return info;
    }

    public static TickState getEntityState(Entity entity) {
        if(entity.isPassenger()) return getEntityState(entity.getRootVehicle()); // all passengers will follow TPS of the root entity
        TickState state = entity.getAttached(TICK_STATE);
        if(state == null) return getChunkState(entity.chunkPosition());

        int rate = state.rate();
        TickState serverState = getServerState();
        if(rate == -1) rate = getChunkState(entity.chunkPosition()).rate();
        if(serverState.frozen() || serverState.sprinting() || serverState.stepping())
            return Objects.requireNonNull(serverState.withRate(serverState.stepping() ? serverState.rate() : rate));
        return Objects.requireNonNull(state.withRate(rate));
    }

    /**
     * Level is assumed to be the {@link Minecraft#level}
     */
    public static TickState getChunkState(ChunkPos chunkPos) {
        if(!serverHasMod) return getServerState();
        TickState state = Objects.requireNonNull(Minecraft.getInstance().level).getChunk(chunkPos.x(), chunkPos.z()).getAttached(TICK_STATE);
        if(state == null) return getServerState();

        int rate = state.rate();
        TickState serverState = getServerState();
        if(state.rate() == -1) rate = serverState.rate();
        if(serverState.frozen() || serverState.sprinting() || serverState.stepping())
            return Objects.requireNonNull(serverState.withRate(serverState.stepping() ? serverState.rate() : rate));
        return Objects.requireNonNull(state.withRate(rate));
    }

    public static TickState getServerState() {
        Level level = Objects.requireNonNull(Minecraft.getInstance().level);
        if(!serverHasMod) {
            TickRateManager tickManager = level.tickRateManager();
            return new TickState((int) tickManager.tickrate(),tickManager.isFrozen(),tickManager.isSteppingForward(),false); // Client does not have any sprint info
        }
        return level.getAttachedOrThrow(TICK_STATE_SERVER);
    }

}
