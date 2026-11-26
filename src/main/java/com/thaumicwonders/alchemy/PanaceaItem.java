package com.thaumicwonders.alchemy;

import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Panacea (6 hunger, saturation 1.8, always edible). Eating it first clears every harmful effect, then gives
 * Regeneration II 100 ticks and Absorption I 2400 ticks. The enchanted one gives Regeneration II 400, Resistance I
 * 6000, Fire Resistance 6000, Absorption IV 2400 and Warp Ward 18000 instead.
 */
public class PanaceaItem extends Item {
    private final boolean enchanted;

    public PanaceaItem(boolean enchanted, Properties properties) {
        super(properties);
        this.enchanted = enchanted;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            List<Holder<MobEffect>> bad = new ArrayList<>();
            for (MobEffectInstance effect : entity.getActiveEffects()) {
                if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    bad.add(effect.getEffect());
                }
            }
            bad.forEach(entity::removeEffect);
            if (enchanted) {
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
                entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 6000, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3));
                entity.addEffect(new MobEffectInstance(TTMobEffects.WARP_WARD, 18000, 0, true, true));
            } else {
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 0));
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
