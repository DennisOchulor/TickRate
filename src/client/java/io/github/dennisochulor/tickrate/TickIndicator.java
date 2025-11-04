package io.github.dennisochulor.tickrate;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class TickIndicator {

    private TickIndicator() {}

    private static boolean enabled = false;

    public static boolean toggle() {
        enabled = !enabled;
        return enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void tick() {
        if(!TickIndicator.isEnabled() || !TickRateClientManager.serverHasMod()) return;
        Minecraft client = Minecraft.getInstance();
        TickState chunkState = TickRateClientManager.getChunkState(client.player.chunkPosition());
        int chunkRate = chunkState.rate();
        TickState entityState = client.crosshairPickEntity!=null ? TickRateClientManager.getEntityState(client.crosshairPickEntity) : null;
        int entityRate = (entityState!=null ? entityState.rate() : 0);

        String chunkStateStr = "";
        String entityStateStr = "";
        if(chunkState.sprinting())
            chunkStateStr = " (Sprinting)";
        else if(chunkState.stepping())
            chunkStateStr = " (Stepping)";
        else if(chunkState.frozen())
            chunkStateStr = " (Frozen)";

        if(entityState != null) {
            if(entityState.sprinting())
                entityStateStr = " (Sprinting)";
            else if(entityState.stepping())
                entityStateStr = " (Stepping)";
            else if(entityState.frozen())
                entityStateStr = " (Frozen)";
        }

        client.player.displayClientMessage(Component.literal("Entity: " + entityRate + " TPS" + entityStateStr + "       Chunk: " + chunkRate + " TPS" + chunkStateStr), true);
    }

}
