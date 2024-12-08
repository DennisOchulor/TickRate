package io.github.dennisochulor.tickrate;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * @param rate If this is -1.0f, then Chunk delegates to server's nominalTickRate while
 *             Entity delegates to Chunk tick rate.
 */
public record TickState(float rate, boolean frozen, boolean stepping, boolean sprinting) {

    public static final PacketCodec<ByteBuf,TickState> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.FLOAT, TickState::rate,
            PacketCodecs.BOOLEAN, TickState::frozen,
            PacketCodecs.BOOLEAN, TickState::stepping,
            PacketCodecs.BOOLEAN, TickState::sprinting,
            TickState::new
    );

}
