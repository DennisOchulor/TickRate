package io.github.dennisochulor.tickrate.mixin.client.compat.emf;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.dennisochulor.tickrate.TickRateClientManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import traben.entity_model_features.models.animation.math.EMFMath;
import traben.entity_model_features.utils.EMFEntity;

/**
 * EMF compat,
 * see <a href="https://github.com/Traben-0/Entity_Model_Features/blob/02034eb0f102040b16900be7c900eff88da89e9e/src/main/java/traben/entity_model_features/models/animation/math/EMFMath.java#L591">here</a>
 */
@Mixin(EMFMath.class)
abstract class EMFMathMixin {
    @Shadow
    private static @Nullable EMFEntity emfEntity() { return null; }

    @ModifyReturnValue(method = "getTickDelta", at = @At("RETURN"))
    private static float getTickDelta(float original) {
        EMFEntity emfEntity = emfEntity();

        if (emfEntity instanceof BlockEntity blockEntity) {
            return TickRateClientManager.getChunkTickDelta(new ChunkPos(blockEntity.getPos())).tickDelta();
        }
        else if (emfEntity instanceof Entity entity) {
            return TickRateClientManager.getEntityTickDelta(entity).tickDelta();
        }

        return original;
    }
}
