package io.github.dennisochulor.tickrate.mixin.networking;

import io.github.dennisochulor.tickrate.TickRateHelloPayload;
import io.github.dennisochulor.tickrate.TickState;
import io.github.dennisochulor.tickrate.injected_interface.TickRateServerPlayNetworkHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateTickRateS2CPacket;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin implements TickRateServerPlayNetworkHandler {

    @Shadow public ServerPlayerEntity player;
    @Shadow public abstract void requestTeleport(double x, double y, double z, float yaw, float pitch);
    @Shadow public abstract void updateSequence(int sequence);

    @Unique private boolean hasClientMod;
    // Only used if hasClientMod == false
    @Unique private boolean wasFrozen = false;
    @Unique private UpdateTickRateS2CPacket prevPacket = null;


    @Override
    public boolean tickRate$hasClientMod() {
        return hasClientMod;
    }


    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        hasClientMod = ServerPlayNetworking.canSend((ServerPlayNetworkHandler) (Object) this, TickRateHelloPayload.ID);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;syncWithPlayerPosition()V"), cancellable = true)
    private void tick(CallbackInfo ci) {
        ServerTickManager tickManager = player.getEntityWorld().getServer().getTickManager();
        ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler) (Object) this;

        // For players without the mod client-side, allow some degree of TPS control
        // Regardless of whether this player should tick or not, send update packet if applicable
        if (!hasClientMod) {
            TickState state = tickManager.tickRate$getEntityTickStateDeep(player);
            UpdateTickRateS2CPacket newPacket = new UpdateTickRateS2CPacket(state.sprinting() ? 100 : state.rate(), tickManager.isFrozen());
            if (!newPacket.equals(prevPacket)) {
                handler.sendPacket(newPacket);
                prevPacket = newPacket;
            }

            if ( (!wasFrozen && state.frozen()) && !(state.stepping() || state.sprinting()) ) {
                wasFrozen = true;
            }
            else if ( (wasFrozen && !state.frozen()) || (wasFrozen && (state.stepping() || state.sprinting())) ) {
                wasFrozen = false;
            }
        }


        // call tickMovement() according to player's TPS, tickMovement() starts at syncWithPlayerPosition() and ends before baseTick()
        // the rest of the handler should still tick even if shouldTickEntity is false, though only the sequencing stuff is being acccounted for now.
        if (!tickManager.tickRate$shouldTickEntity(player)) ci.cancel();
    }



    // Mimic player freeze for clients without the mod

    @Inject(method = {"onPlayerInput","onPlayerInteractEntity","onPlayerInteractItem"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
            shift = At.Shift.AFTER), cancellable = true)
    private void onPlayerGeneralAction(CallbackInfo ci) {
        if (!hasClientMod && wasFrozen) {
            player.updateLastActionTime();
            ci.cancel();
        }
    }

    @Inject(method = "onPlayerInteractBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
            shift = At.Shift.AFTER), cancellable = true)
    private void onPlayerInteractBlock(PlayerInteractBlockC2SPacket packet, CallbackInfo ci) {
        // for placing blocks
        if (!hasClientMod && wasFrozen) {
            ServerPlayNetworkHandler handler = ((ServerPlayNetworkHandler)(Object)this);
            BlockPos pos = packet.getBlockHitResult().getBlockPos();
            updateSequence(packet.getSequence()); // sequence is checked in handler.tick(), so must update it else client won't process block updates
            player.updateLastActionTime(); // to avoid potential erroneous idle timeout
            handler.sendPacket(new BlockUpdateS2CPacket(player.getEntityWorld(), pos));
            handler.sendPacket(new BlockUpdateS2CPacket(player.getEntityWorld(), pos.offset(packet.getBlockHitResult().getSide())));
            ci.cancel();
        }
    }

    @Inject(method = "onPlayerAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
            shift = At.Shift.AFTER), cancellable = true)
    private void onPlayerAction(PlayerActionC2SPacket packet, CallbackInfo ci) {
        // for breaking blocks
        if (!hasClientMod && wasFrozen) {
            updateSequence(packet.getSequence());
            player.updateLastActionTime();
            ((ServerPlayNetworkHandler)(Object)this).sendPacket(new BlockUpdateS2CPacket(player.getEntityWorld(), packet.getPos()));
            ci.cancel();
        }
    }

    @Inject(method = "onPlayerMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
            shift = At.Shift.AFTER), cancellable = true)
    private void onPlayerMove(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if (!hasClientMod && wasFrozen) {
            requestTeleport(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
            ci.cancel();
        }
    }

}
