package io.github.dennisochulor.tickrate.mixin.sound;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public class EntityMixin {

    @Shadow private World world;

    @ModifyVariable(method = "playSound", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    public float playSound(float pitch) {
        if(world.isClient()) return pitch;

        Entity entity = (Entity) (Object) this;
        return pitch * (entity.getServer().getTickManager().tickRate$getEntityRate(entity) / 20.0F);
    }

}
