package com.thaumicwonders.gear;

import com.leclowndu93150.thaumaturge.api.capability.IPlayerKnowledge;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeAccess;
import com.thaumicwonders.ThaumicWonders;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The Primal Destroyer: a Voidflame sword that repairs itself, saps and burns what it hits, and hungers while carried.
 * Every second it is in the hotbar or offhand its hunger rises by one; at 600 it bites its wielder (12 damage of its
 * own type, Weakness and Hunger) and resets. Hits feed it 120, kills 600.
 */
public class PrimalDestroyerItem extends Item {
    public static final int MAX_HUNGER = 600;
    private static final int HIT_FEED = 120;
    private static final int KILL_FEED = 600;
    private static final float HUNGER_DAMAGE = 12.0F;

    public PrimalDestroyerItem(Properties props) {
        super(props);
    }

    public static int hunger(ItemStack stack) {
        return stack.getOrDefault(GearContent.HUNGER.get(), 0);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof ServerPlayer player) || entity.tickCount % 20 != 0) {
            return;
        }
        // Self-repairing, like the original's Voidflame material.
        if (stack.isDamaged()) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
        if (!isEquipped(player, stack)) {
            return;
        }
        int hunger = hunger(stack);
        if (hunger >= MAX_HUNGER) {
            player.sendOverlayMessage(Component.translatable("event.primal_destroyer.hunger_full").withStyle(ChatFormatting.DARK_PURPLE));
            player.hurtServer(level, level.damageSources().source(GearContent.DESTROYER_HUNGER), HUNGER_DAMAGE);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120));
            stack.set(GearContent.HUNGER.get(), 0);
            // The research flag that reveals the Primal Destroyer's addendum.
            IPlayerKnowledge knowledge = KnowledgeAccess.of(player);
            if (knowledge != null && knowledge.addResearch(ThaumicWonders.id("f_thevoidhungers"))) {
                knowledge.sync(player);
            }
        } else {
            stack.set(GearContent.HUNGER.get(), hunger + 1);
        }
    }

    /** The sword counts as wielded in the hotbar or the offhand. */
    private static boolean isEquipped(Player player, ItemStack stack) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            if (inv.getItem(i) == stack) {
                return true;
            }
        }
        return player.getOffhandItem() == stack;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (attacker.level().isClientSide()) {
            return;
        }
        // Sapping Greater and Voidflame.
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60));
        target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120));
        target.igniteForSeconds(3.0F);
        int feed = target.isDeadOrDying() ? KILL_FEED : HIT_FEED;
        stack.set(GearContent.HUNGER.get(), Math.max(0, hunger(stack) - feed));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        // Hunger ticking up must not bob the held sword every second.
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("enchantment.special.sapgreat").withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.translatable("enchantment.special.voidflame").withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
