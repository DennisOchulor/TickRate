package io.github.dennisochulor.tickrate;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;

import java.util.Objects;

/** Returns the player's current DeltaTracker values or the server's if {@link TickRateClientManager#serverHasMod()} is false */
public class PlayerDeltaTracker implements DeltaTracker {

    @Override
    public float getGameTimeDeltaTicks() {
        Minecraft minecraft = Minecraft.getInstance();
        if(TickRateClientManager.serverHasMod())
            return TickRateClientManager.getEntityDeltaTrackerInfo(Objects.requireNonNull(minecraft.player)).deltaTicks();
        else return minecraft.getDeltaTracker().getGameTimeDeltaTicks();
    }

    @Override
    public float getGameTimeDeltaPartialTick(boolean ignoreFreeze) {
        Minecraft minecraft = Minecraft.getInstance();
        if(TickRateClientManager.serverHasMod()) {
            if(Objects.requireNonNull(minecraft.level).tickRateManager().isFrozen())
                return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(ignoreFreeze);
            else
                return TickRateClientManager.getEntityDeltaTrackerInfo(Objects.requireNonNull(minecraft.player)).partialTick();
        }
        else return minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(ignoreFreeze);
    }

    @Override
    public float getRealtimeDeltaTicks() {
        return Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks();
    }

}
