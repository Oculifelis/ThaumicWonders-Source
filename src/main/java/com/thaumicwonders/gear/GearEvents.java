package com.thaumicwonders.gear;

import com.leclowndu93150.thaumaturge.api.warp.IPlayerWarp;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.thaumicwonders.ThaumicWonders;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Game-bus handlers for the gear group: the Void Fortress set and its damage rules, and the Primal Arrows' damage. */
public final class GearEvents {
    private static final Identifier SET_ARMOR = ThaumicWonders.id("void_fortress_set_armor");
    private static final Identifier SET_TOUGHNESS = ThaumicWonders.id("void_fortress_set_toughness");
    private static final Identifier WARPSHELL = ThaumicWonders.id("void_fortress_warpshell");

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
        double armor = set >= 3 ? 2.0 : set >= 2 ? 1.0 : 0.0;
        double toughness = set >= 2 ? 1.0 : 0.0;
        double warpshell = 0.0;
        if (set > 0) {
            IPlayerWarp warp = WarpHelper.getWarp(player);
            if (warp != null) {
                warpshell = Math.min(100, warp.get(WarpType.PERMANENT)) / 25.0;
            }
        }
        apply(player, Attributes.ARMOR, SET_ARMOR, armor);
        apply(player, Attributes.ARMOR_TOUGHNESS, SET_TOUGHNESS, toughness);
        apply(player, Attributes.ARMOR_TOUGHNESS, WARPSHELL, warpshell);
    }

    private static void apply(LivingEntity entity, Holder<Attribute> attribute, Identifier id, double amount) {
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
    private static int voidFortressDefense(LivingEntity entity) {
        int sum = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (GearContent.isVoidFortress(stack)) {
                ArmorType type = slot == EquipmentSlot.HEAD ? ArmorType.HELMET : slot == EquipmentSlot.CHEST ? ArmorType.CHESTPLATE : ArmorType.LEGGINGS;
                sum += GearContent.VOID_FORTRESS.defense().getOrDefault(type, 0);
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
            switch (arrow.kind()) {
                case AIR, ORDER -> event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> 0.0F);
                case FIRE -> {
                    if (target.fireImmune() || target.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                        event.setCanceled(true);
                        return;
                    }
                }
                default -> {
                }
            }
        }

        // Void Fortress: magic is absorbed at defense/35 per piece, fire and explosions at defense/20 (a full set
        // stops them, burning included) in place of plain armour; everything else goes through armour as usual.
        int defense = voidFortressDefense(target);
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
