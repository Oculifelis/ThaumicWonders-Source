package com.thaumicwonders.gear;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** One of the six Primal Arrows (the original's primal_arrow metadata variants, now separate items). */
public class PrimalArrowItem extends ArrowItem {
    private final PrimalArrowEntity.Kind kind;

    public PrimalArrowItem(Properties props, PrimalArrowEntity.Kind kind) {
        super(props);
        this.kind = kind;
    }

    public PrimalArrowEntity.Kind kind() {
        return this.kind;
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, @Nullable ItemStack weapon) {
        return new PrimalArrowEntity(level, shooter, ammo.copyWithCount(1), weapon, this.kind);
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        // The dispenser behaviour (BehaviorDispensePrimalArrow).
        return new PrimalArrowEntity(level, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1), this.kind);
    }

    @Override
    public boolean isInfinite(ItemStack ammo, ItemStack bow, LivingEntity living) {
        // Infinity only spares a primal arrow one time in three.
        var infinity = living.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.INFINITY);
        if (infinity.isEmpty() || EnchantmentHelper.getItemEnchantmentLevel(infinity.get(), bow) <= 0) {
            return false;
        }
        return living.getRandom().nextInt(3) == 0;
    }
}
