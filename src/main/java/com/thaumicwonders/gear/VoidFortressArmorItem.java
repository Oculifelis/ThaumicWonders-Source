package com.thaumicwonders.gear;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Void Fortress armour (helm, chest, legs). Self-repairing while worn, 3 warp each, the helm works as Goggles of
 * Revealing. The set bonus, warpshell toughness, fire/explosion/magic ratios and the helm's life-leech live in
 * {@link GearEvents}.
 */
public class VoidFortressArmorItem extends Item {
    public VoidFortressArmorItem(Properties props) {
        super(props);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (slot != null && slot.isArmor() && stack.isDamaged() && entity.tickCount % 20 == 0
                && entity instanceof LivingEntity living && living.getItemBySlot(slot) == stack) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("enchantment.special.warpshell").withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
