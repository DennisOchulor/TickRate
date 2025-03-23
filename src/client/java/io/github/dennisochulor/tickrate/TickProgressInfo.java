package io.github.dennisochulor.tickrate;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;

public record TickProgressInfo(float tickProgress, int i, float dynamicDeltaTicks) {

    public static final TickProgressInfo NO_ANIMATE = new TickProgressInfo(1.0f,0,0);

    // returns the server's current TickProgressInfo
    public static TickProgressInfo ofServer(boolean ignoreFreeze) {
        RenderTickCounter renderTickCounter = MinecraftClient.getInstance().getRenderTickCounter();
        return new TickProgressInfo(renderTickCounter.getTickProgress(ignoreFreeze), renderTickCounter.tickRate$getI(), renderTickCounter.getDynamicDeltaTicks());
    }

}