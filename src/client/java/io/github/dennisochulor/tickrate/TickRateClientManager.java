package io.github.dennisochulor.tickrate;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

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

    public static TickDeltaInfo getEntityTickDelta(float defaultTickDelta, Entity entity) {
        TickRateRenderTickCounter renderTickCounter = (TickRateRenderTickCounter) MinecraftClient.getInstance().getRenderTickCounter();
        if(!serverHasMod) return new TickDeltaInfo(defaultTickDelta,renderTickCounter.tickRate$getI());
        if(serverState.frozen() || serverState.sprinting() || serverState.stepping()) return new TickDeltaInfo(defaultTickDelta,renderTickCounter.tickRate$getI());

        TickState state = entities.get(entity.getUuidAsString());
        if(state == null) return getChunkTickDelta(defaultTickDelta, entity.getWorld(), entity.getChunkPos().toLong());
        if(state.frozen() && !state.stepping()) return new TickDeltaInfo(1.0f,0);
        if(state.sprinting()) return renderTickCounter.tickRate$getSpecificTickDelta(1000.0f / 20.0f,entity.getUuidAsString()); // animate at max 20 TPS
        if(state.rate() == -1.0f) return getChunkTickDelta(defaultTickDelta, entity.getWorld(), entity.getChunkPos().toLong());
        return renderTickCounter.tickRate$getSpecificTickDelta(1000.0f / state.rate(), entity.getUuidAsString());
    }

    public static TickDeltaInfo getChunkTickDelta(float defaultTickDelta, World world, long chunkPos) {
        TickRateRenderTickCounter renderTickCounter = (TickRateRenderTickCounter) MinecraftClient.getInstance().getRenderTickCounter();
        if(!serverHasMod) return new TickDeltaInfo(defaultTickDelta,renderTickCounter.tickRate$getI());
        if(serverState.frozen() || serverState.sprinting() || serverState.stepping()) return new TickDeltaInfo(defaultTickDelta,renderTickCounter.tickRate$getI());

        String key = world.getRegistryKey().getValue() + "-" + chunkPos;
        TickState state = chunks.get(key);
        if(state == null) return new TickDeltaInfo(defaultTickDelta,renderTickCounter.tickRate$getI());
        if(state.frozen() && !state.stepping()) return new TickDeltaInfo(1.0f,0);
        if(state.sprinting()) return renderTickCounter.tickRate$getSpecificTickDelta(1000.0f / 20.0f,key); // animate at max 20 TPS
        if(state.rate() == -1.0f) return new TickDeltaInfo(defaultTickDelta,renderTickCounter.tickRate$getI());
        return renderTickCounter.tickRate$getSpecificTickDelta(1000.0f / state.rate(),key);
    }

}