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
        if(MinecraftClient.getInstance().isPaused()) return new TickDeltaInfo(1.0f,0,0);
        if(entity instanceof PlayerEntity && serverState.frozen()) return TickDeltaInfo.ofServer(true);
        if(serverState.frozen() || serverState.sprinting() || serverState.stepping()) return TickDeltaInfo.ofServer(false);
        if(entity.hasVehicle()) return getEntityTickDelta(entity.getRootVehicle()); // all passengers will follow TPS of the root entity

        TickState state = entities.get(entity.getUuidAsString());
        if(state == null) return getChunkTickDelta(entity.getWorld(), entity.getChunkPos().toLong());
        if(state.sprinting()) return renderTickCounter.tickRate$getSpecificTickDelta(1000.0f / 20.0f,entity.getUuidAsString()); // animate at max 20 TPS
        if(state.frozen() && !state.stepping()) return new TickDeltaInfo(1.0f,0,0);
        if(state.rate() == -1.0f) return getChunkTickDelta(entity.getWorld(), entity.getChunkPos().toLong());
        return renderTickCounter.tickRate$getSpecificTickDelta(1000.0f / state.rate(), entity.getUuidAsString());
    }

    public static TickDeltaInfo getChunkTickDelta(World world, long chunkPos) {
        RenderTickCounter renderTickCounter = MinecraftClient.getInstance().getRenderTickCounter();
        if(!serverHasMod) return TickDeltaInfo.ofServer(false);
        if(MinecraftClient.getInstance().isPaused()) return new TickDeltaInfo(1.0f,0,0);
        if(serverState.frozen() || serverState.sprinting() || serverState.stepping()) return TickDeltaInfo.ofServer(false);

        String key = world.getRegistryKey().getValue() + "-" + chunkPos;
        TickState state = chunks.get(key);
        if(state == null) return TickDeltaInfo.ofServer(false);
        if(state.sprinting()) return renderTickCounter.tickRate$getSpecificTickDelta(1000.0f / 20.0f,key); // animate at max 20 TPS
        if(state.frozen() && !state.stepping()) return new TickDeltaInfo(1.0f,0,0);
        if(state.rate() == -1.0f) return TickDeltaInfo.ofServer(false);
        return renderTickCounter.tickRate$getSpecificTickDelta(1000.0f / state.rate(),key);
    }

    public static TickState getEntityState(Entity entity) {
        if(entity.hasVehicle()) return getEntityState(entity.getRootVehicle());
        TickState state = entities.get(entity.getUuidAsString());
        if(state == null) return getChunkState(entity.getWorld(), entity.getChunkPos().toLong());
        if(state.rate() == -1.0f)
            return new TickState(getChunkState(entity.getWorld(), entity.getChunkPos().toLong()).rate(), state.frozen(), state.stepping(), state.sprinting());
        else return state;
    }

    public static TickState getChunkState(World world, long chunkPos) {
        if(!serverHasMod) return getServerState();
        TickState state = chunks.get(world.getRegistryKey().getValue() + "-" + chunkPos);
        if(state == null) return serverState;
        if(state.rate() == -1.0f)
            return new TickState(serverState.rate(), state.frozen(), state.stepping(), state.sprinting());
        else return state;
    }

    public static TickState getServerState() {
        if(!serverHasMod) {
            TickManager tickManager = MinecraftClient.getInstance().world.getTickManager();
            return new TickState(tickManager.getTickRate(),tickManager.isFrozen(),tickManager.isStepping(),false); // Client does not have any sprint info
        }
        return serverState;
    }

}
