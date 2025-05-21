package io.github.dennisochulor.tickrate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import org.jetbrains.annotations.Nullable;

/**
 * @param rate If this is -1, then it fallbacks to the next thing.
 */
public record TickState(int rate, boolean frozen, boolean stepping, boolean sprinting) {

    public static final TickState DEFAULT = new TickState(-1, false, false, false);

    public static final PacketCodec<ByteBuf,TickState> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, TickState::rate,
            PacketCodecs.BOOL, TickState::frozen,
            PacketCodecs.BOOL, TickState::stepping,
            PacketCodecs.BOOL, TickState::sprinting,
            TickState::new
    );

    public static final Codec<TickState> CODEC = RecordCodecBuilder.create(tickState ->
        tickState.group(
                Codec.INT.fieldOf("rate").forGetter(TickState::rate),
                Codec.BOOL.fieldOf("frozen").forGetter(TickState::frozen),
                Codec.BOOL.fieldOf("stepping").forGetter(TickState::stepping),
                Codec.BOOL.fieldOf("sprinting").forGetter(TickState::sprinting)
        ).apply(tickState, TickState::new));


    public static TickState ofRate(int rate) {
        return new TickState(rate, false, false, false);
    }

    public static TickState ofFrozen() {
        return new TickState(-1, true, false, false);
    }

    public static TickState ofStepping() {
        return new TickState(-1, false, true, false);
    }

    public static TickState ofSprinting() {
        return new TickState(-1, false, false, true);
    }


    @Nullable
    public TickState withRate(int rate) {
        TickState state = new TickState(rate, this.frozen(), this.stepping(), this.sprinting());
        return state.equals(DEFAULT) ? null : state;
    }

    @Nullable
    public TickState withFrozen(boolean frozen) {
        TickState state = new TickState(this.rate(), frozen, this.stepping(), this.sprinting());
        return state.equals(DEFAULT) ? null : state;
    }

    @Nullable
    public TickState withStepping(boolean stepping) {
        TickState state = new TickState(this.rate(), this.frozen(), stepping, this.sprinting());
        return state.equals(DEFAULT) ? null : state;
    }

    @Nullable
    public TickState withSprinting(boolean sprinting) {
        TickState state = new TickState(this.rate(), this.frozen(), this.stepping(), sprinting);
        return state.equals(DEFAULT) ? null : state;
    }


}
