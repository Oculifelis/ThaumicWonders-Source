package com.thaumicwonders.gear;

import com.leclowndu93150.thaumaturge.api.items.RechargeAccess;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Night-Vision Goggles: a helmet or a Curios head item. While worn they keep Night Vision up, spending one vis every
 * 360 ticks (100 vis last 30 minutes).
 */
public class NightVisionGogglesItem extends ArmorItem implements ICurioItem {
    public static final int VIS_CAPACITY = 100;
    public static final net.minecraft.resources.ResourceLocation TEXTURE =
            com.thaumicwonders.ThaumicWonders.id("textures/entity/armor/night_vision_goggles.png");
    public static final int ENERGY_PER_VIS = (20 * 60 * 30) / VIS_CAPACITY;

    public NightVisionGogglesItem(Properties props) {
        super(GearContent.GOGGLES, ArmorItem.Type.HELMET, props);
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level anyLevel, Entity entity, int slotId, boolean selected) {
        if (!(anyLevel instanceof ServerLevel level)) {
            return;
        }
        EquipmentSlot slot = GearContent.wornSlot(entity, stack);
        if (slot == EquipmentSlot.HEAD && entity instanceof LivingEntity living && living.getItemBySlot(slot) == stack) {
            wornTick(stack, living);
        }
    }

    @Override
    public net.minecraft.resources.ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
            net.minecraft.world.item.ArmorMaterial.Layer layer, boolean inner) {
        return TEXTURE;
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        if (!context.entity().level().isClientSide()) {
            wornTick(stack, context.entity());
        }
    }

    private static void wornTick(ItemStack stack, LivingEntity wearer) {
        consumeEnergy(stack, wearer, ENERGY_PER_VIS);
        if (hasEnergy(stack)) {
            wearer.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false));
        }
    }

    /** The original's energy counter: count down, and take one vis when it runs out. Shared with the charm. */
    static void consumeEnergy(ItemStack stack, LivingEntity wearer, int perVis) {
        int energy = stack.getOrDefault(TTDataComponents.ENERGY.get(), 0);
        if (energy > 0) {
            energy--;
        } else if (RechargeAccess.consumeCharge(stack, wearer, 1)) {
            energy = perVis;
        }
        stack.set(TTDataComponents.ENERGY.get(), energy);
    }

    static boolean hasEnergy(ItemStack stack) {
        return stack.getOrDefault(TTDataComponents.ENERGY.get(), 0) > 0 || RechargeAccess.getCharge(stack) > 0;
    }
}
