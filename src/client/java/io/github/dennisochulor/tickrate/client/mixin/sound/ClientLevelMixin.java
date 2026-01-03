package io.github.dennisochulor.tickrate.client.mixin.sound;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.dennisochulor.tickrate.TickRate;
import io.github.dennisochulor.tickrate.client.TickRateClientManager;
import io.github.dennisochulor.tickrate.TickState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.util.Objects;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Shadow @Final private Minecraft minecraft;

    /**
     * Entity pitch change is handled by EntityMixin (common side) already
     */
    @ModifyVariable(method = "playSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZJ)V",
            at = @At("HEAD"), argsOnly = true, name = "pitch")
    public float playSound(float pitch, @Local(argsOnly = true, name = "x") double x,
                           @Local(argsOnly = true, name = "y") double y,
                           @Local(argsOnly = true, name = "z") double z,
                           @Local(argsOnly = true, name = "source") SoundSource source)
    {
        Objects.requireNonNull(minecraft.player);

        return switch(source) {
            case MASTER,MUSIC,UI,RECORDS,VOICE,NEUTRAL,HOSTILE -> pitch;
            case PLAYERS -> {
                TickState state = TickRateClientManager.getEntityState(minecraft.player);
                if(state.sprinting()) yield TickRate.MAX_SOUND_PITCH;
                else yield pitch * (state.rate() / 20.0F);
            }
            case WEATHER -> {
                TickState state = TickRateClientManager.getServerState();
                if(state.sprinting()) yield TickRate.MAX_SOUND_PITCH;
                else yield pitch * (state.rate() / 20.0F);
            }
            case BLOCKS, AMBIENT -> {
                TickState state = TickRateClientManager.getChunkState(new ChunkPos(new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z))));
                if(state.sprinting()) yield TickRate.MAX_SOUND_PITCH;
                else yield pitch * (state.rate() / 20.0F);
            }
        };
    }

    /**
     * Entity pitch change is handled by EntityMixin (common side) already
     * Entity is assumed to be Minecraft#player
     */
    @ModifyVariable(method = "playPlayerSound(Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
            at = @At("HEAD"), argsOnly = true, name = "pitch")
    public float playPlayerSound$Client(float pitch, @Local(argsOnly = true, name = "source") SoundSource source) {
        Objects.requireNonNull(minecraft.player);

        return switch(source) {
            case MASTER,MUSIC,UI,RECORDS,VOICE,NEUTRAL,HOSTILE -> pitch;
            case PLAYERS -> {
                TickState state = TickRateClientManager.getEntityState(minecraft.player);
                if(state.sprinting()) yield TickRate.MAX_SOUND_PITCH;
                else yield pitch * (state.rate() / 20.0F);
            }
            case WEATHER -> {
                TickState state = TickRateClientManager.getServerState();
                if(state.sprinting()) yield TickRate.MAX_SOUND_PITCH;
                else yield pitch * (state.rate() / 20.0F);
            }
            case BLOCKS, AMBIENT -> {
                TickState state = TickRateClientManager.getChunkState(minecraft.player.chunkPosition());
                if(state.sprinting()) yield TickRate.MAX_SOUND_PITCH;
                else yield pitch * (state.rate() / 20.0F);
            }
        };
    }

    @ModifyVariable(method = "playSeededSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"), argsOnly = true, name = "pitch")
    public float playSeededSound$Entity(float pitch, @Local(argsOnly = true, name = "sourceEntity") Entity sourceEntity) {
        TickState state = TickRateClientManager.getEntityState(sourceEntity);
        if(state.sprinting()) return TickRate.MAX_SOUND_PITCH;
        else return pitch * (state.rate() / 20.0F);
    }

    @ModifyVariable(method = "playLocalSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
            at = @At("HEAD"), argsOnly = true, name = "pitch")
    public float playLocalSound$Entity(float pitch, @Local(argsOnly = true, name = "sourceEntity") Entity sourceEntity) {
        TickState state = TickRateClientManager.getEntityState(sourceEntity);
        if(state.sprinting()) return TickRate.MAX_SOUND_PITCH;
        else return pitch * (state.rate() / 20.0F);
    }

}
