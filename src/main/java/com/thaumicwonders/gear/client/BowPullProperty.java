package com.thaumicwonders.gear.client;

import com.mojang.serialization.MapCodec;
import com.thaumicwonders.gear.BoneBowItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * thaumaturgicwonders:pull, the Bow of Bone's draw: ticks drawn over its draw time (10 ticks with vis, 20 without), so the
 * pulling models keep pace with the faster powered draw.
 */
public record BowPullProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<BowPullProperty> CODEC = MapCodec.unit(new BowPullProperty());

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        LivingEntity living = owner == null ? null : owner.asLivingEntity();
        if (living == null || !living.isUsingItem() || living.getUseItem() != stack) {
            return 0.0F;
        }
        return living.getTicksUsingItem() / BoneBowItem.chargeTime(stack);
    }

    @Override
    public MapCodec<BowPullProperty> type() {
        return CODEC;
    }
}
