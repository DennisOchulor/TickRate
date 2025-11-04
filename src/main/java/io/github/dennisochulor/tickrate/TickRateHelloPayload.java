package io.github.dennisochulor.tickrate;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TickRateHelloPayload(String version) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TickRateHelloPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TickRate.MOD_ID,"hello"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TickRateHelloPayload> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, TickRateHelloPayload::version, TickRateHelloPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public TickRateHelloPayload() {
        this(FabricLoader.getInstance().getModContainer(TickRate.MOD_ID).get().getMetadata().getVersion().getFriendlyString());
    }

}
