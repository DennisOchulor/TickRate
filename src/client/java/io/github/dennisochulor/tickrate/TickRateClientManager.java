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

    public static float getEntityTickDelta(float defaultTickDelta, Entity entity) {
        if(!serverHasMod) return defaultTickDelta;
        if(serverState.frozen() || serverState.sprinting() || serverState.stepping()) return defaultTickDelta;

        TickRateRenderTickCounter renderTickCounter = (TickRateRenderTickCounter) MinecraftClient.getInstance().getRenderTickCounter();
        TickState state = entities.get(entity.getUuidAsString());
        if(state == null) return getChunkTickDelta(defaultTickDelta, entity.getWorld(), entity.getChunkPos().toLong());
        if(state.frozen()) return 1.0f;
        if(state.sprinting()) return renderTickCounter.tickRate$getSpecificTickDelta(1000.0f / 20.0f); // animate at max 20 TPS
        if(state.rate() == -1.0f) return getChunkTickDelta(defaultTickDelta, entity.getWorld(), entity.getChunkPos().toLong());
        return renderTickCounter.tickRate$getSpecificTickDelta(1000.0f / state.rate());
    }

    public static float getChunkTickDelta(float defaultTickDelta, World world, long chunkPos) {
        if(!serverHasMod) return defaultTickDelta;
        if(serverState.frozen() || serverState.sprinting() || serverState.stepping()) return defaultTickDelta;

        TickRateRenderTickCounter renderTickCounter = (TickRateRenderTickCounter) MinecraftClient.getInstance().getRenderTickCounter();
        TickState state = chunks.get(world.getRegistryKey().getValue() + "-" + chunkPos);
        if(state == null) return defaultTickDelta;
        if(state.frozen()) return 1.0f;
        if(state.sprinting()) return renderTickCounter.tickRate$getSpecificTickDelta(1000.0f / 20.0f); // animate at max 20 TPS
        if(state.rate() == -1.0f) return defaultTickDelta;
        return renderTickCounter.tickRate$getSpecificTickDelta(1000.0f / state.rate());
    }

}
