package io.github.dennisochulor.tickrate;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;

public record TickDeltaInfo(float tickDelta, int i, float lastFrameDuration) {

    public static final TickDeltaInfo NO_ANIMATE = new TickDeltaInfo(1.0f,0,0);

    // returns the server's current TickDeltaInfo
    public static TickDeltaInfo ofServer(boolean ignoreFreeze) {
        RenderTickCounter renderTickCounter = MinecraftClient.getInstance().getRenderTickCounter();
        return new TickDeltaInfo(renderTickCounter.getTickDelta(ignoreFreeze), renderTickCounter.tickRate$getI(), renderTickCounter.getLastFrameDuration());
    }

}