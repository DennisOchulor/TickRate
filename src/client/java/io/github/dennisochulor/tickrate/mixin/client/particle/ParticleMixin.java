package io.github.dennisochulor.tickrate.mixin.client.particle;

import io.github.dennisochulor.tickrate.injected_interface.TickRateParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Particle.class)
public class ParticleMixin implements TickRateParticle {

    @Shadow protected double x;
    @Shadow protected double y;
    @Shadow protected double z;
    @Shadow @Final protected ClientLevel level;

    @Override
    public BlockPos tickRate$getBlockPos() {
        return new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z));
    }

    @Override
    public Level tickRate$getLevel() {
        return level;
    }
}
