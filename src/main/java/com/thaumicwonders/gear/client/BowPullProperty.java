package com.thaumicwonders.gear.client;

import com.thaumicwonders.gear.BoneBowItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * thaumaturgicwonders:pull, the Bow of Bone's draw: ticks drawn over its draw time (10 ticks with vis, 20 without), so the
 * pulling models keep pace with the faster powered draw. thaumaturgicwonders:pulling is 1 while it is drawn.
 */
public final class BowPullProperty implements ClampedItemPropertyFunction {
    public static final BowPullProperty PULL = new BowPullProperty();
    public static final ClampedItemPropertyFunction PULLING = (stack, level, living, seed) ->
            living != null && living.isUsingItem() && living.getUseItem() == stack ? 1.0F : 0.0F;

    private BowPullProperty() {
    }

    @Override
    public float unclampedCall(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity living, int seed) {
        if (living == null || !living.isUsingItem() || living.getUseItem() != stack) {
            return 0.0F;
        }
        return living.getTicksUsingItem() / BoneBowItem.chargeTime(stack);
    }
}
