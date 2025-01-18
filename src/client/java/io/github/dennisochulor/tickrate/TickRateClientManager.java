package io.github.dennisochulor.tickrate;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.world.tick.TickManager;

import java.util.HashMap;
import java.util.Map;

public class TickRateClientManager {

    private TickRateClientManager() {}

    private static boolean serverHasMod = false;
    private static TickState serverState;
    private static final Map<String,TickState> entities = new HashMap<>();
    private static final Map<String,TickState> chunks = new HashMap<>();

    public static void update(TickRateS2CUpdatePayload payload) {
        serverState = payload.server();
        entities.clear();
        chunks.clear();
        entities.putAll(payload.entities());
        chunks.putAll(payload.chunks());
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

    public static TickDeltaInfo getEntityTickDelta(Entity entity) {
        RenderTickCounter renderTickCounter = MinecraftClient.getInstance().getRenderTickCounter();
        if(!serverHasMod) return TickDeltaInfo.ofServer(false);
        if(MinecraftClient.getInstance().isPaused()) return TickDeltaInfo.NO_ANIMATE;
        if(entity instanceof PlayerEntity && serverState.frozen()) return TickDeltaInfo.ofServer(true); // tick freeze doesn't affect players
        if(entity.hasVehicle()) return getEntityTickDelta(entity.getRootVehicle());

        // client's own player OR entities where client player is a passenger can go above 20TPS limit
        boolean cappedAt20TPS = !(entity==MinecraftClient.getInstance().player) && !entity.hasPassenger(MinecraftClient.getInstance().player);
        TickState state = getEntityState(entity); // this also handles passenger entities
        if(state.sprinting()) // animate at max 20 TPS but for client player we don't know the TPS, so just say 100 :P
            return cappedAt20TPS ? renderTickCounter.tickRate$getSpecificTickDeltaInfo(20) : renderTickCounter.tickRate$getClientPlayerTickDeltaInfo(100);
        if(state.frozen() && !state.stepping()) return TickDeltaInfo.NO_ANIMATE;
        if(!cappedAt20TPS) return renderTickCounter.tickRate$getClientPlayerTickDeltaInfo((int) state.rate());
        return renderTickCounter.tickRate$getSpecificTickDeltaInfo((int) state.rate());
    }

    public static TickDeltaInfo getChunkTickDelta(World world, long chunkPos) {
        RenderTickCounter renderTickCounter = MinecraftClient.getInstance().getRenderTickCounter();
        if(!serverHasMod) return TickDeltaInfo.ofServer(false);
        if(MinecraftClient.getInstance().isPaused()) return TickDeltaInfo.NO_ANIMATE;

        TickState state = getChunkState(world, chunkPos);
        if(state.sprinting()) return renderTickCounter.tickRate$getSpecificTickDeltaInfo(20); // animate at max 20 TPS
        if(state.frozen() && !state.stepping()) return TickDeltaInfo.NO_ANIMATE;
        return renderTickCounter.tickRate$getSpecificTickDeltaInfo((int) state.rate());
    }

    public static TickState getEntityState(Entity entity) {
        if(entity.hasVehicle()) return getEntityState(entity.getRootVehicle()); // all passengers will follow TPS of the root entity
        TickState state = entities.get(entity.getUuidAsString());
        if(state == null) return getChunkState(entity.getWorld(), entity.getChunkPos().toLong());

        float rate = state.rate();
        if(rate == -1.0f) rate = getChunkState(entity.getWorld(), entity.getChunkPos().toLong()).rate();
        if(serverState.frozen() || serverState.sprinting() || serverState.stepping())
            return new TickState(serverState.stepping() ? serverState.rate() : rate,serverState.frozen(),serverState.stepping(),serverState.sprinting());
        return new TickState(rate,state.frozen(),state.stepping(),state.sprinting());
    }

    public static TickState getChunkState(World world, long chunkPos) {
        if(!serverHasMod) return getServerState();
        TickState state = chunks.get(world.getRegistryKey().getValue() + "-" + chunkPos);
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
