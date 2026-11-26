package com.thaumicwonders.gear;

import com.leclowndu93150.thaumaturge.api.items.RechargeAccess;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;

/**
 * The Bow of Bone. Unpowered it draws in 20 ticks like a normal bow and shoots at 3x draw power; with vis in it, it
 * draws in 10 ticks, looses itself once fully drawn, shoots at 4x and spends one vis per shot.
 */
public class BoneBowItem extends BowItem {
    public static final int VIS_CAPACITY = 200;
    public static final int POWERED_CHARGE_TIME = 10;
    public static final int UNPOWERED_CHARGE_TIME = 20;

    public BoneBowItem(Properties props) {
        super(props);
    }

    public static boolean isPowered(ItemStack stack) {
        return RechargeAccess.getCharge(stack) > 0;
    }

    public static float chargeTime(ItemStack stack) {
        return isPowered(stack) ? POWERED_CHARGE_TIME : UNPOWERED_CHARGE_TIME;
    }

    public static float arrowVelocity(ItemStack stack, int ticks) {
        float f = ticks / chargeTime(stack);
        f = (f * f + f * 2.0F) / 3.0F;
        return Math.min(f, 1.0F);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        int ticks = this.getUseDuration(stack, entity) - remaining;
        if (ticks >= POWERED_CHARGE_TIME && isPowered(stack)) {
            entity.releaseUsingItem();
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        ItemStack ammo = player.getProjectile(stack);
        if (ammo.isEmpty()) {
            return false;
        }
        int ticks = this.getUseDuration(stack, entity) - timeLeft;
        ticks = EventHooks.onArrowLoose(stack, level, player, ticks, true);
        if (ticks < 0) {
            return false;
        }
        boolean powered = isPowered(stack);
        float power = arrowVelocity(stack, ticks);
        if (power < 0.1F) {
            return false;
        }
        List<ItemStack> projectiles = draw(stack, ammo, player);
        if (level instanceof ServerLevel server && !projectiles.isEmpty()) {
            this.shoot(server, player, player.getUsedItemHand(), stack, projectiles, power * (powered ? 4.0F : 3.0F), 1.0F, power == 1.0F, null);
            RechargeAccess.consumeCharge(stack, player, 1);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F,
                1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        // Spending vis must not bob the bow.
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }
}
