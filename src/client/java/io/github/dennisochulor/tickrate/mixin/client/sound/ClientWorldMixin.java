package io.github.dennisochulor.tickrate.mixin.client.sound;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.dennisochulor.tickrate.TickRate;
import io.github.dennisochulor.tickrate.TickRateClientManager;
import io.github.dennisochulor.tickrate.TickState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {

    @Shadow @Final private MinecraftClient client;

    /**
     * Entity pitch change is handled by EntityMixin (common side) already
     */
    @ModifyVariable(method = "playSound(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZJ)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    public float playSound(float pitch, @Local(argsOnly = true, ordinal = 0) double x, @Local(argsOnly = true, ordinal = 1) double y, @Local(argsOnly = true, ordinal = 2) double z, @Local(argsOnly = true) SoundCategory category) {
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
                TickState state = TickRateClientManager.getChunkState(new ChunkPos(new BlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z))));
                if(state.sprinting()) yield TickRate.MAX_SOUND_PITCH;
                else yield pitch * (state.rate() / 20.0F);
            }
        };
    }

    /**
     * Entity pitch change is handled by EntityMixin (common side) already
     * Entity is assumed to be MinecraftClient#player
     */
    @ModifyVariable(method = "playSoundClient(Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    public float playSoundClient(float pitch, @Local(argsOnly = true) SoundCategory category) {
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
                TickState state = TickRateClientManager.getChunkState(client.player.getChunkPos());
                if(state.sprinting()) yield TickRate.MAX_SOUND_PITCH;
                else yield pitch * (state.rate() / 20.0F);
            }
        };
    }

    @ModifyVariable(method = "playSoundFromEntity", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    public float playSoundFromEntity(float pitch, @Local(argsOnly = true, ordinal = 1) Entity entity) {
        TickState state = TickRateClientManager.getEntityState(entity);
        if(state.sprinting()) return TickRate.MAX_SOUND_PITCH;
        else return pitch * (state.rate() / 20.0F);
    }

    @ModifyVariable(method = "playSoundFromEntityClient", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    public float playSoundFromEntityClient(float pitch, @Local(argsOnly = true) Entity entity) {
        TickState state = TickRateClientManager.getEntityState(entity);
        if(state.sprinting()) return TickRate.MAX_SOUND_PITCH;
        else return pitch * (state.rate() / 20.0F);
    }

}
