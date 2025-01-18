package io.github.dennisochulor.tickrate.mixin.client;

import io.github.dennisochulor.tickrate.injected_interface.TickRateParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Particle.class)
public class ParticleMixin implements TickRateParticle {

    @Shadow protected double x;
    @Shadow protected double y;
    @Shadow protected double z;
    @Shadow @Final protected ClientWorld world;

    @Override
    public BlockPos tickRate$getBlockPos() {
        return new BlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
    }

    @Override
    public World tickRate$getWorld() {
        return world;
    }
}
