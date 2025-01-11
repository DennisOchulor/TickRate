package io.github.dennisochulor.tickrate;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

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
        MinecraftClient client = MinecraftClient.getInstance();
        TickState chunkState = TickRateClientManager.getChunkState(client.player.clientWorld, client.player.getChunkPos().toLong());
        int chunkRate = (int) chunkState.rate();
        TickState entityState = client.targetedEntity!=null ? TickRateClientManager.getEntityState(client.targetedEntity) : null;
        int entityRate = (int) (entityState!=null ? entityState.rate() : 0);

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

        client.player.sendMessage(Text.literal("Entity: " + entityRate + " TPS" + entityStateStr + "       Chunk: " + chunkRate + " TPS" + chunkStateStr), true);
    }

}
