package io.github.dennisochulor.tickrate.mixin.sound;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

    @Shadow @Final private MinecraftServer server;

    /**
     * Entity pitch change is handled by EntityMixin already
     */
    @ModifyVariable(method = "playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/sound/SoundCategory;FFJ)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    public float playSound(float pitch, @Local(argsOnly = true) PlayerEntity player, @Local(argsOnly = true, ordinal = 0) double x, @Local(argsOnly = true, ordinal = 1) double y, @Local(argsOnly = true, ordinal = 2) double z, @Local(argsOnly = true) SoundCategory category, @Local(argsOnly = true) RegistryEntry<SoundEvent> event) {
        return switch(category) {
            case MASTER,MUSIC,RECORDS,VOICE,NEUTRAL,HOSTILE -> pitch;
            case PLAYERS -> {
                if(player != null) yield pitch * (server.getTickManager().tickRate$getEntityRate(player) / 20.0F);
                else yield pitch * (server.getTickManager().tickRate$getChunkRate((World)(Object)this, ChunkPos.toLong(new BlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z)))) / 20.0F);
            }
            case WEATHER -> pitch * (server.getTickManager().tickRate$getServerRate() / 20.0F);
            case BLOCKS, AMBIENT -> pitch * (server.getTickManager().tickRate$getChunkRate((World)(Object)this, ChunkPos.toLong(new BlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z)))) / 20.0F);
        };
    }

    @ModifyVariable(method = "playSoundFromEntity", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    public float playSoundFromEntity(float pitch, @Local(argsOnly = true) Entity entity) { // never called apparently
        return pitch * (server.getTickManager().tickRate$getEntityRate(entity) / 20.0F);
    }

}
