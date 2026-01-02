package io.github.dennisochulor.tickrate.mixin.sound;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.dennisochulor.tickrate.TickRate;
import io.github.dennisochulor.tickrate.TickState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Shadow @Final private MinecraftServer server;

    /**
     * Entity pitch change is handled by EntityMixin already
     */
    @ModifyVariable(method = "playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V", at = @At("HEAD"), argsOnly = true, name = "pitch")
    public float playSound(float pitch, @Local(argsOnly = true) @Nullable Entity entity,
                           @Local(argsOnly = true, ordinal = 0) double x, @Local(argsOnly = true, ordinal = 1) double y, @Local(argsOnly = true, ordinal = 2) double z,
                           @Local(argsOnly = true) SoundSource category) {
        ServerTickRateManager tickManager = server.tickRateManager();
        return switch(category) {
            case MASTER,MUSIC,UI,RECORDS,VOICE,NEUTRAL,HOSTILE -> pitch;
            case PLAYERS -> {
                TickState state;
                if(entity != null) // possibly handles player sounds
                    state = tickManager.tickRate$getEntityTickStateDeep(entity);
                else
                    state = tickManager.tickRate$getChunkTickStateDeep((Level) (Object) this, new ChunkPos(new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z))));
                if(state.sprinting()) yield TickRate.MAX_SOUND_PITCH;
                else yield pitch * (state.rate() / 20.0F);
            }
            case WEATHER -> {
                TickState state = tickManager.tickRate$getServerTickState();
                if(state.sprinting()) yield TickRate.MAX_SOUND_PITCH;
                else yield pitch * (state.rate() / 20.0F);
            }
            case BLOCKS, AMBIENT -> {
                TickState state = tickManager.tickRate$getChunkTickStateDeep((Level)(Object)this, new ChunkPos(new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z))));
                if(state.sprinting()) yield TickRate.MAX_SOUND_PITCH;
                else yield pitch * (state.rate() / 20.0F);
            }
        };
    }

    @ModifyVariable(method = "playSeededSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    public float playSeededSound$Entity(float pitch, @Local(argsOnly = true, ordinal = 1) Entity entity) { // never called apparently
        TickState state = server.tickRateManager().tickRate$getEntityTickStateDeep(entity);
        if(state.sprinting()) return TickRate.MAX_SOUND_PITCH;
        else return pitch * (state.rate() / 20.0F);
    }

}
