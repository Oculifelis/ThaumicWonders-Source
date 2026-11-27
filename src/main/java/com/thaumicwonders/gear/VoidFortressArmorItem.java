package com.thaumicwonders.gear;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Void Fortress armour (helm, chest, legs). Self-repairing while worn, 3 warp each, the helm works as Goggles of
 * Revealing. The set bonus, warpshell toughness, fire/explosion/magic ratios and the helm's life-leech live in
 * {@link GearEvents}.
 */
public class VoidFortressArmorItem extends ArmorItem {
    private static final net.minecraft.resources.ResourceLocation TEXTURE =
            com.thaumicwonders.ThaumicWonders.id("textures/entity/armor/void_fortress_armor.png");

    public VoidFortressArmorItem(Properties props, ArmorItem.Type type) {
        super(GearContent.VOID_FORTRESS, type, props);
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level anyLevel, Entity entity, int slotId, boolean selected) {
        if (!(anyLevel instanceof ServerLevel level)) {
            return;
        }
        EquipmentSlot slot = GearContent.wornSlot(entity, stack);
        if (slot != null && slot.isArmor() && stack.isDamaged() && entity.tickCount % 20 == 0
                && entity instanceof LivingEntity living && living.getItemBySlot(slot) == stack) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    @Override
    public net.minecraft.resources.ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
            net.minecraft.world.item.ArmorMaterial.Layer layer, boolean inner) {
        return TEXTURE;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltipList, TooltipFlag flag) {
        java.util.function.Consumer<Component> tooltip = tooltipList::add;
        tooltip.accept(Component.translatable("enchantment.special.warpshell").withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltipList, flag);
    }
}
