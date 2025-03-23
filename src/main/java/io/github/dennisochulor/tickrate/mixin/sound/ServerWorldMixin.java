package io.github.dennisochulor.tickrate.mixin.sound;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.dennisochulor.tickrate.TickRate;
import io.github.dennisochulor.tickrate.TickState;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
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
    @ModifyVariable(method = "playSound", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    public float playSound(float pitch, @Local(argsOnly = true) Entity entity, @Local(argsOnly = true, ordinal = 0) double x, @Local(argsOnly = true, ordinal = 1) double y, @Local(argsOnly = true, ordinal = 2) double z, @Local(argsOnly = true) SoundCategory category) {
        ServerTickManager tickManager = server.getTickManager();
        return switch(category) {
            case MASTER,MUSIC,RECORDS,VOICE,NEUTRAL,HOSTILE -> pitch;
            case PLAYERS -> {
                TickState state;
                if(entity != null) // possibly handles player sounds
                    state = tickManager.tickRate$getEntityTickStateDeep(entity);
                else
                    state = tickManager.tickRate$getChunkTickStateDeep((World) (Object) this, ChunkPos.toLong(new BlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z))));
                if(state.sprinting()) yield TickRate.MAX_SOUND_PITCH;
                else yield pitch * (state.rate() / 20.0F);
            }
            case WEATHER -> {
                TickState state = tickManager.tickRate$getServerTickState();
                if(state.sprinting()) yield TickRate.MAX_SOUND_PITCH;
                else yield pitch * (state.rate() / 20.0F);
            }
            case BLOCKS, AMBIENT -> {
                TickState state = tickManager.tickRate$getChunkTickStateDeep((World)(Object)this, ChunkPos.toLong(new BlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z))));
                if(state.sprinting()) yield TickRate.MAX_SOUND_PITCH;
                else yield pitch * (state.rate() / 20.0F);
            }
        };
    }

    @ModifyVariable(method = "playSoundFromEntity", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    public float playSoundFromEntity(float pitch, @Local(argsOnly = true, ordinal = 1) Entity entity) { // never called apparently
        TickState state = server.getTickManager().tickRate$getEntityTickStateDeep(entity);
        if(state.sprinting()) return TickRate.MAX_SOUND_PITCH;
        else return pitch * (state.rate() / 20.0F);
    }

}
