package io.github.dennisochulor.tickrate;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

public class TickRateAttachments {
    private TickRateAttachments() {}

    public static void init() {}

    public static final AttachmentType<TickState> TICK_STATE = AttachmentRegistry.create(Identifier.of(TickRate.MOD_ID, "tick_state"),
            builder ->
                    builder.persistent(TickState.CODEC)
                            .syncWith(TickState.PACKET_CODEC, AttachmentSyncPredicate.all())
                            .copyOnDeath()
    );

    public static final AttachmentType<TickState> TICK_STATE_SERVER = AttachmentRegistry.create(Identifier.of(TickRate.MOD_ID, "tick_state_server"),
            builder ->
                    builder.persistent(TickState.CODEC)
                            .syncWith(TickState.PACKET_CODEC, AttachmentSyncPredicate.all())
    );

    public static final AttachmentType<Integer> STEP_TICKS = AttachmentRegistry.create(Identifier.of(TickRate.MOD_ID, "step_ticks"),
            builder ->
                    builder.persistent(Codecs.NON_NEGATIVE_INT)
                            .copyOnDeath()
    );

    public static final AttachmentType<Integer> SPRINT_TICKS = AttachmentRegistry.create(Identifier.of(TickRate.MOD_ID, "sprint_ticks"),
            builder ->
                    builder.persistent(Codecs.NON_NEGATIVE_INT)
                            .copyOnDeath()
    );

}
