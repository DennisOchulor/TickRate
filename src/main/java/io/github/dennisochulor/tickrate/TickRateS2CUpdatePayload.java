package io.github.dennisochulor.tickrate;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public record TickRateS2CUpdatePayload(TickState server, Map<Integer,TickState> entities, Map<Long,TickState> chunks) implements CustomPayload {

    public static final Id<TickRateS2CUpdatePayload> ID = new Id<>(Identifier.of(TickRate.MOD_ID,"update"));
    public static final PacketCodec<RegistryByteBuf, TickRateS2CUpdatePayload> CODEC = PacketCodec.tuple(TickState.PACKET_CODEC, TickRateS2CUpdatePayload::server,
        PacketCodecs.map(HashMap::new,PacketCodecs.INTEGER,TickState.PACKET_CODEC), TickRateS2CUpdatePayload::entities,
        PacketCodecs.map(HashMap::new,PacketCodecs.LONG,TickState.PACKET_CODEC), TickRateS2CUpdatePayload::chunks,
        TickRateS2CUpdatePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

}
