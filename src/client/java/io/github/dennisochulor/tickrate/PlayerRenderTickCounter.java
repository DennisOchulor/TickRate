package io.github.dennisochulor.tickrate;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;

/** Returns the player's current render tick counter values or the server's if {@link TickRateClientManager#serverHasMod()} is false */
public class PlayerRenderTickCounter implements RenderTickCounter {

    @Override
    public float getDynamicDeltaTicks() {
        if(TickRateClientManager.serverHasMod())
            return TickRateClientManager.getEntityTickProgress(MinecraftClient.getInstance().player).dynamicDeltaTicks();
        else return MinecraftClient.getInstance().getRenderTickCounter().getDynamicDeltaTicks();
    }

    @Override
    public float getTickProgress(boolean ignoreFreeze) {
        if(TickRateClientManager.serverHasMod()) {
            if(MinecraftClient.getInstance().world.getTickManager().isFrozen())
                return MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(ignoreFreeze);
            else
                return TickRateClientManager.getEntityTickProgress(MinecraftClient.getInstance().player).tickProgress();
        }
        else return MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(ignoreFreeze);
    }

    @Override
    public float getFixedDeltaTicks() {
        return MinecraftClient.getInstance().getRenderTickCounter().getFixedDeltaTicks();
    }

}
