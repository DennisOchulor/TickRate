package io.github.dennisochulor.tickrate;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;

/** Returns the player's current render tick counter values or the server's if {@link TickRateClientManager#serverHasMod()} is false */
public class PlayerRenderTickCounter implements RenderTickCounter {

    @Override
    public float getLastFrameDuration() {
        if(TickRateClientManager.serverHasMod())
            return TickRateClientManager.getEntityTickDelta(MinecraftClient.getInstance().player).lastFrameDuration();
        else return MinecraftClient.getInstance().getRenderTickCounter().getLastFrameDuration();
    }

    @Override
    public float getTickDelta(boolean ignoreFreeze) {
        if(TickRateClientManager.serverHasMod()) {
            if(MinecraftClient.getInstance().world.getTickManager().isFrozen())
                return MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(ignoreFreeze);
            else
                return TickRateClientManager.getEntityTickDelta(MinecraftClient.getInstance().player).tickDelta();
        }
        else return MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(ignoreFreeze);
    }

    @Override
    public float getLastDuration() {
        return MinecraftClient.getInstance().getRenderTickCounter().getLastDuration();
    }

}
