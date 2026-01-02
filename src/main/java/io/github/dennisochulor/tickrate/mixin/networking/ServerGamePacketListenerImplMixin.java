package io.github.dennisochulor.tickrate.mixin.networking;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.dennisochulor.tickrate.TickRateHelloPayload;
import io.github.dennisochulor.tickrate.TickState;
import io.github.dennisochulor.tickrate.injected_interface.TickRateServerGamePacketListenerImpl;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundTickingStatePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin implements TickRateServerGamePacketListenerImpl {

    @Shadow public ServerPlayer player;
    @Shadow public abstract void teleport(double x, double y, double z, float yaw, float pitch);
    @Shadow public abstract void ackBlockChangesUpTo(int sequence);

    @Unique private boolean hasClientMod;
    // Only used if hasClientMod == false
    @Unique private boolean wasFrozen = false;
    @Unique @Nullable private ClientboundTickingStatePacket prevPacket = null;


    @Override
    public boolean tickRate$hasClientMod() {
        return hasClientMod;
    }


    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        hasClientMod = ServerPlayNetworking.canSend((ServerGamePacketListenerImpl) (Object) this, TickRateHelloPayload.ID);
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;tickPlayer()Z"))
    private boolean tick(ServerGamePacketListenerImpl handler, Operation<Boolean> original) {
        ServerTickRateManager tickManager = player.level().getServer().tickRateManager();

        // For players without the mod client-side, allow some degree of TPS control
        // Regardless of whether this player should tick or not, send update packet if applicable
        if (!hasClientMod) {
            TickState state = tickManager.tickRate$getEntityTickStateDeep(player);
            ClientboundTickingStatePacket newPacket = new ClientboundTickingStatePacket(state.sprinting() ? 100 : state.rate(), tickManager.isFrozen());
            if (!newPacket.equals(prevPacket)) {
                handler.send(newPacket);
                prevPacket = newPacket;
            }

            if ( (!wasFrozen && state.frozen()) && !(state.stepping() || state.sprinting()) ) {
                wasFrozen = true;
            }
            else if ( (wasFrozen && !state.frozen()) || (wasFrozen && (state.stepping() || state.sprinting())) ) {
                wasFrozen = false;
            }
        }


        // call tickMovement() according to player's TPS
        // the rest of the handler should still tick even if shouldTickEntity is false
        if (tickManager.tickRate$shouldTickEntity(player)) return original.call(handler);
        else return false;
    }



    // Mimic player freeze for clients without the mod

    @Inject(method = {"handlePlayerInput","handleInteract","handleUseItem"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER), cancellable = true)
    private void handlePlayerGeneralAction(CallbackInfo ci) {
        if (!hasClientMod && wasFrozen) {
            player.resetLastActionTime();
            ci.cancel();
        }
    }

    @Inject(method = "handleUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER), cancellable = true)
    private void handleUseItemOn$interactBlock(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        // for placing blocks
        if (!hasClientMod && wasFrozen) {
            ServerGamePacketListenerImpl handler = ((ServerGamePacketListenerImpl)(Object)this);
            BlockPos pos = packet.getHitResult().getBlockPos();
            ackBlockChangesUpTo(packet.getSequence()); // sequence is checked in handler.tick(), so must update it else client won't process block updates
            player.resetLastActionTime(); // to avoid potential erroneous idle timeout
            handler.send(new ClientboundBlockUpdatePacket(player.level(), pos));
            handler.send(new ClientboundBlockUpdatePacket(player.level(), pos.relative(packet.getHitResult().getDirection())));
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER), cancellable = true)
    private void handlePlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        // for breaking blocks
        if (!hasClientMod && wasFrozen) {
            ackBlockChangesUpTo(packet.getSequence());
            player.resetLastActionTime();
            ((ServerGamePacketListenerImpl)(Object)this).send(new ClientboundBlockUpdatePacket(player.level(), packet.getPos()));
            ci.cancel();
        }
    }

    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER), cancellable = true)
    private void handleMovePlayer(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (!hasClientMod && wasFrozen) {
            teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            ci.cancel();
        }
    }

}
