package io.github.dennisochulor.tickrate.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;

public record DeltaTrackerInfo(float partialTick, int ticksToDo, float deltaTicks) {

    public static final DeltaTrackerInfo NO_ANIMATE = new DeltaTrackerInfo(1.0f,0,0);

    // returns the server's current DeltaTrackerInfo
    public static DeltaTrackerInfo ofServer(boolean ignoreFreeze) {
        DeltaTracker deltaTracker = Minecraft.getInstance().getDeltaTracker();
        return new DeltaTrackerInfo(deltaTracker.getGameTimeDeltaPartialTick(ignoreFreeze), deltaTracker.tickRate$getTicksToDo(), deltaTracker.getGameTimeDeltaTicks());
    }

}