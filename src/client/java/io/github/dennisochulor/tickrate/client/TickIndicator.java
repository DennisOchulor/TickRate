package io.github.dennisochulor.tickrate.client;

import io.github.dennisochulor.tickrate.TickState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Objects;

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

        Minecraft minecraft = Minecraft.getInstance();
        TickState chunkState = TickRateClientManager.getChunkState(Objects.requireNonNull(minecraft.player).chunkPosition());
        int chunkRate = chunkState.rate();
        TickState entityState = minecraft.crosshairPickEntity!=null ? TickRateClientManager.getEntityState(minecraft.crosshairPickEntity) : null;
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

        minecraft.player.displayClientMessage(Component.literal("Entity: " + entityRate + " TPS" + entityStateStr + "       Chunk: " + chunkRate + " TPS" + chunkStateStr), true);
    }

}
