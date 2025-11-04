package io.github.dennisochulor.tickrate;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;

/** Returns the player's current DeltaTracker values or the server's if {@link TickRateClientManager#serverHasMod()} is false */
public class PlayerDeltaTracker implements DeltaTracker {

    @Override
    public float getGameTimeDeltaTicks() {
        if(TickRateClientManager.serverHasMod())
            return TickRateClientManager.getEntityDeltaTrackerInfo(Minecraft.getInstance().player).deltaTicks();
        else return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaTicks();
    }

    @Override
    public float getGameTimeDeltaPartialTick(boolean ignoreFreeze) {
        if(TickRateClientManager.serverHasMod()) {
            if(Minecraft.getInstance().level.tickRateManager().isFrozen())
                return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(ignoreFreeze);
            else
                return TickRateClientManager.getEntityDeltaTrackerInfo(Minecraft.getInstance().player).partialTick();
        }
        else return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(ignoreFreeze);
    }

    @Override
    public float getRealtimeDeltaTicks() {
        return Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks();
    }

}
