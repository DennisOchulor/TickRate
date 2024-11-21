package io.github.dennisochulor.tickrate;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TickRateHelloPayload(String version) implements CustomPayload {

    public static final CustomPayload.Id<TickRateHelloPayload> ID = new CustomPayload.Id<>(Identifier.of(TickRate.MOD_ID,"hello"));
    public static final PacketCodec<RegistryByteBuf, TickRateHelloPayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING, TickRateHelloPayload::version, TickRateHelloPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public TickRateHelloPayload() {
        this(FabricLoader.getInstance().getModContainer(TickRate.MOD_ID).get().getMetadata().getVersion().getFriendlyString());
    }

}
