package io.github.dennisochulor.tickrate.mixin.networking;

import io.github.dennisochulor.tickrate.TickRateHelloPayload;
import io.github.dennisochulor.tickrate.TickState;
import io.github.dennisochulor.tickrate.injected_interface.TickRateServerPlayNetworkHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.network.packet.s2c.play.UpdateTickRateS2CPacket;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin implements TickRateServerPlayNetworkHandler {

    @Shadow public ServerPlayerEntity player;

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

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tick$head(CallbackInfo ci) {

        ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler) (Object) this;
        ServerTickManager tickManager = player.getEntityWorld().getServer().getTickManager();

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
                EntityAttributeInstance attribute = player.getAttributes().getCustomInstance(EntityAttributes.MOVEMENT_SPEED);
                attribute.setBaseValue(0);

                attribute = player.getAttributes().getCustomInstance(EntityAttributes.JUMP_STRENGTH);
                attribute.setBaseValue(0);

                wasFrozen = true;
            }
            else if ( (wasFrozen && !state.frozen()) || (wasFrozen && (state.stepping() || state.sprinting())) ) {
                player.getAttributes().getCustomInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(0.1);
                player.getAttributes().getCustomInstance(EntityAttributes.JUMP_STRENGTH).setBaseValue(0.42);

                wasFrozen = false;
            }
        }


        // tick handler according to player's TPS
        if(!tickManager.tickRate$shouldTickEntity(player)) ci.cancel();
    }

    // Mimic player freeze for clients without the mod
    @Inject(method = {"onPlayerMove","onPlayerInput","onPlayerInteractBlock","onPlayerInteractEntity","onPlayerInteractItem","onPlayerAction"}, at = @At("HEAD"), cancellable = true)
    private void onPlayerAction(CallbackInfo ci) {
        if (!hasClientMod && wasFrozen) ci.cancel();
    }

}
