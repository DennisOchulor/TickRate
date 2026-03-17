package io.github.dennisochulor.tickrate.client.mixin.tick;

import io.github.dennisochulor.tickrate.client.TickRateClientManager;
import net.minecraft.world.TickRateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TickRateManager.class)
public abstract class TickRateManagerMixin {

    @Shadow protected boolean runGameElements;

    /**
     * @author Ninjaking312
     * @reason server override nonsense
     */
    @Overwrite
    public boolean runsNormally() { // if not override, then just true
        if (!TickRateClientManager.serverHasMod()) return runGameElements;
        else return !TickRateClientManager.isServerOverride() || runGameElements;
    }

}
