package io.github.dennisochulor.tickrate.client.mixin.compat.emf;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.dennisochulor.tickrate.client.TickRateClientManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import traben.entity_model_features.models.animation.EMFAnimationEntityContext;
import traben.entity_model_features.utils.EMFEntity;

/**
 * EMF compat,
 * see <a href="https://github.com/Traben-0/Entity_Model_Features/blob/216bcd4b0ebbfac00e837eb0f4fb37f9206d1bbf/src/main/java/traben/entity_model_features/models/animation/EMFAnimationEntityContext.java#L1373">here</a>
 *
 * <p>The class is deprecated and will be removed as per the comment:
 * "todomove most func into EMFRenderState where appropriate"
 *
 * <p>Until that refactor is done, not much I can do to avoid using deprecated stuff :(
 */
@SuppressWarnings("deprecation")
@Mixin(EMFAnimationEntityContext.class)
abstract class EMFAnimationEntityContextMixin {
    @Shadow
    private static EMFEntity emfEntity() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @ModifyReturnValue(method = "getTickDelta", at = @At("RETURN"))
    private static float getTickDelta(float original) {
        EMFEntity emfEntity = emfEntity();

        if (emfEntity instanceof BlockEntity blockEntity) {
            return TickRateClientManager.getChunkDeltaTrackerInfo(ChunkPos.containing(blockEntity.getBlockPos())).partialTick();
        }
        else if (emfEntity instanceof Entity entity) {
            return TickRateClientManager.getEntityDeltaTrackerInfo(entity).partialTick();
        }

        return original;
    }
}
