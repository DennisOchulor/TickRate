package io.github.dennisochulor.tickrate;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;

public record TickDeltaInfo(float tickDelta, int i, float lastFrameDuration) {

    // returns the server's current TickDeltaInfo replacing the tickDelta with the one passed-in
    public static TickDeltaInfo ofServer(float tickDelta) {
        RenderTickCounter renderTickCounter = MinecraftClient.getInstance().getRenderTickCounter();
        TickRateRenderTickCounter tickRateRenderTickCounter = (TickRateRenderTickCounter) renderTickCounter;
        return new TickDeltaInfo(tickDelta, tickRateRenderTickCounter.tickRate$getI(), renderTickCounter.getLastFrameDuration());
    }

    // returns the server's current TickDeltaInfo
    public static TickDeltaInfo ofServer() {
        return ofServer(MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false));
    }

}