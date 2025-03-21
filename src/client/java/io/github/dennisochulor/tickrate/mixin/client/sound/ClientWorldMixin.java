package io.github.dennisochulor.tickrate.mixin.client.sound;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.dennisochulor.tickrate.TickRate;
import io.github.dennisochulor.tickrate.TickRateClientManager;
import io.github.dennisochulor.tickrate.TickState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {

    @Shadow @Final private MinecraftClient client;

    /**
     * Entity pitch change is handled by EntityMixin (common side) already
     */
    @ModifyArg(method = "playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/sound/SoundCategory;FFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;playSound(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZJ)V"), index = 6)
    public float playSoundWithPlayer(float pitch, @Local(argsOnly = true, ordinal = 0) double x, @Local(argsOnly = true, ordinal = 1) double y, @Local(argsOnly = true, ordinal = 2) double z, @Local(argsOnly = true) SoundCategory category, @Local(argsOnly = true) RegistryEntry<SoundEvent> event) {
        return switch(category) {
            case MASTER,MUSIC,RECORDS,VOICE,NEUTRAL,HOSTILE -> pitch;
            case PLAYERS -> {
                TickState state = TickRateClientManager.getEntityState(client.player);
                if(state.sprinting()) yield TickRate.MAX_SOUND_PITCH;
                else yield pitch * (state.rate() / 20.0F);
            }
            case WEATHER -> {
                TickState state = TickRateClientManager.getServerState();
                if(state.sprinting()) yield TickRate.MAX_SOUND_PITCH;
                else yield pitch * (state.rate() / 20.0F);
            }
            case BLOCKS, AMBIENT -> {
                TickState state = TickRateClientManager.getChunkState(ChunkPos.toLong(new BlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z))));
                if(state.sprinting()) yield TickRate.MAX_SOUND_PITCH;
                else yield pitch * (state.rate() / 20.0F);
            }
        };
    }

    @ModifyVariable(method = "playSoundFromEntity*", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    public float playSoundFromEntity(float pitch, @Local(argsOnly = true) Entity entity) { // never called apparently
        TickState state = TickRateClientManager.getEntityState(entity);
        if(state.sprinting()) return TickRate.MAX_SOUND_PITCH;
        else return pitch * (state.rate() / 20.0F);
    }

    @ModifyVariable(method = "playSound(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZ)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    public float playSound(float pitch, @Local(argsOnly = true, ordinal = 0) double x, @Local(argsOnly = true, ordinal = 1) double y, @Local(argsOnly = true, ordinal = 2) double z, @Local(argsOnly = true) SoundCategory category, @Local(argsOnly = true) SoundEvent event) {
        return switch(category) {
            case MASTER,MUSIC,RECORDS,VOICE,NEUTRAL,HOSTILE -> pitch;
            case PLAYERS -> {
                TickState state = TickRateClientManager.getEntityState(client.player);
                if(state.sprinting()) yield TickRate.MAX_SOUND_PITCH;
                else yield pitch * (state.rate() / 20.0F);
            }
            case WEATHER -> {
                TickState state = TickRateClientManager.getServerState();
                if(state.sprinting()) yield TickRate.MAX_SOUND_PITCH;
                else yield pitch * (state.rate() / 20.0F);
            }
            case BLOCKS, AMBIENT -> {
                TickState state = TickRateClientManager.getChunkState(ChunkPos.toLong(new BlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z))));
                if(state.sprinting()) yield TickRate.MAX_SOUND_PITCH;
                else yield pitch * (state.rate() / 20.0F);
            }
        };
    }

}
