package io.github.dennisochulor.tickrate;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;

public record TickDeltaInfo(float tickDelta, int i, float lastFrameDuration) {

    // returns the server's current TickDeltaInfo
    public static TickDeltaInfo ofServer() {
        RenderTickCounter renderTickCounter = MinecraftClient.getInstance().getRenderTickCounter();
        TickRateRenderTickCounter tickRateRenderTickCounter = (TickRateRenderTickCounter) renderTickCounter;
        return new TickDeltaInfo(renderTickCounter.getTickDelta(false), tickRateRenderTickCounter.tickRate$getI(), renderTickCounter.getLastFrameDuration());
    }

}