package com.thaumicwonders.gear;

import com.leclowndu93150.thaumaturge.api.warp.IPlayerWarp;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.thaumicwonders.ThaumicWonders;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Game-bus handlers for the gear group: the Void Fortress set and its damage rules, and the Primal Arrows' damage. */
public final class GearEvents {
    private static final ResourceLocation SET_ARMOR = ThaumicWonders.id("void_fortress_set_armor");
    private static final ResourceLocation SET_TOUGHNESS = ThaumicWonders.id("void_fortress_set_toughness");
    private static final ResourceLocation WARPSHELL = ThaumicWonders.id("void_fortress_warpshell");

    private GearEvents() {
    }

    /**
     * The original's ISpecialArmor bonuses as attribute modifiers: two pieces add 1 armour and 1 toughness, three a
     * further 1 armour; warpshell adds toughness of permanent warp / 25 (at most 4) while any piece is worn.
     */
    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 10 != 0) {
            return;
        }
        int set = GearContent.setCount(player);
        var bonus = com.thaumicwonders.data.EquipmentRules.fortress(player.level().registryAccess()).bonus(set);
        double warpshell = 0.0;
        if (set > 0) {
            IPlayerWarp warp = WarpHelper.getWarp(player);
            if (warp != null) {
                warpshell = Math.min(100, warp.get(WarpType.PERMANENT)) / 25.0;
            }
        }
        apply(player, Attributes.ARMOR, SET_ARMOR, bonus.armor());
        apply(player, Attributes.ARMOR_TOUGHNESS, SET_TOUGHNESS, bonus.toughness());
        apply(player, Attributes.ARMOR_TOUGHNESS, WARPSHELL, warpshell);
    }

    private static void apply(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (amount == 0.0) {
            instance.removeModifier(id);
        } else {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** The armour points of the Void Fortress pieces worn (helm 4, chest 9, legs 7). */
    private static float voidFortressDefense(LivingEntity entity) {
        float sum = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (GearContent.isVoidFortress(stack)) {
                ArmorItem.Type type = slot == EquipmentSlot.HEAD ? ArmorItem.Type.HELMET : slot == EquipmentSlot.CHEST ? ArmorItem.Type.CHESTPLATE : ArmorItem.Type.LEGGINGS;
                sum += com.thaumicwonders.data.EquipmentRules.fortress(entity.level().registryAccess()).defense(type);
            }
        }
        return sum;
    }

    @SubscribeEvent
    public static void incomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();

        // Primal Arrows: air and order pierce armour; fire arrows do fire damage, which the fireproof shrug off.
        if (source.getDirectEntity() instanceof PrimalArrowEntity arrow) {
            var rule = arrow.profile();
            if (rule.pierceArmor()) {
                event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> 0.0F);
            }
            if (rule.fireDamage() && (target.fireImmune() || target.hasEffect(MobEffects.FIRE_RESISTANCE))) {
                event.setCanceled(true);
                return;
            }
        }

        // Void Fortress: magic is absorbed at defense/35 per piece, fire and explosions at defense/20 (a full set
        // stops them, burning included) in place of plain armour; everything else goes through armour as usual.
        float defense = voidFortressDefense(target);
        if (defense > 0) {
            if (source.is(Tags.DamageTypes.IS_MAGIC)) {
                event.setAmount(event.getAmount() * (1.0F - Math.min(1.0F, defense / 35.0F)));
            } else if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION)) {
                event.setAmount(event.getAmount() * (1.0F - Math.min(1.0F, defense / 20.0F)));
                event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> 0.0F);
            }
        }

        // Void Fortress helm life-leech: a chance of amount/12 to heal its wearer one point per hit dealt.
        if (source.getEntity() instanceof Player leecher && leecher != target
                && GearContent.isVoidFortress(leecher.getItemBySlot(EquipmentSlot.HEAD))
                &&leecher.getRandom().nextFloat() < event.getAmount() / 12.0F) {
            leecher.heal(1.0F);
        }
    }
}
