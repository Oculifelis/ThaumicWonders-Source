package com.thaumicwonders.gear;

import static com.thaumicwonders.TWRegistry.COMPONENTS;
import static com.thaumicwonders.TWRegistry.ENTITIES;
import static com.thaumicwonders.TWRegistry.RECIPE_SERIALIZERS;

import com.leclowndu93150.thaumaturge.api.items.ChargeDisplay;
import com.leclowndu93150.thaumaturge.api.items.ChargeProfile;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.leclowndu93150.thaumaturge.registry.TTItemTags;
import com.mojang.serialization.Codec;
import com.thaumicwonders.TWRegistry;
import com.thaumicwonders.ThaumicWonders;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The gear group (see docs/COVERAGE.md): Primal Destroyer, Flying Carpet, Void Fortress armour, Night-Vision Goggles,
 * Cleansing Charm, Bow of Bone and the six Primal Arrows.
 */
public final class GearContent {
    private GearContent() {
    }

    // ---- components ---------------------------------------------------------------------------------------------
    /** The Primal Destroyer's hunger, 0..600 (the original's NBT "hunger"). */
    public static final Supplier<DataComponentType<Integer>> HUNGER = COMPONENTS.registerComponentType("hunger",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    /** The Cleansing Charm's progress towards removing one warp, 0..72000 (the original's NBT "progress"). */
    public static final Supplier<DataComponentType<Integer>> PROGRESS = COMPONENTS.registerComponentType("progress",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    /** The Flying Carpet's colour (the original's display.color; absent means red). */
    public static final Supplier<DataComponentType<DyeColor>> DYE = COMPONENTS.registerComponentType("dye",
            b -> b.persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC));

    public static final ResourceKey<DamageType> DESTROYER_HUNGER = ResourceKey.create(Registries.DAMAGE_TYPE, ThaumicWonders.id("primal_destroyer_hunger"));

    // ---- materials ----------------------------------------------------------------------------------------------
    /** VOIDFLAME: harvest level 4, 200 uses, efficiency 8, damage 8, enchantability 20, repaired with void metal. */
    public static final ToolMaterial VOIDFLAME = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 200, 8.0F, 8.0F, 20, TTItemTags.INGOTS_VOID_METAL);
    public static final ResourceKey<EquipmentAsset> VOID_FORTRESS_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, ThaumicWonders.id("void_fortress"));
    public static final ResourceKey<EquipmentAsset> GOGGLES_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, ThaumicWonders.id("night_vision_goggles"));
    /** VOID_FORTRESS: durability factor 50, reduction {boots 4, legs 7, chest 9, helm 4}, enchantability 15, toughness 3. */
    public static final ArmorMaterial VOID_FORTRESS = new ArmorMaterial(50,
            Map.of(ArmorType.BOOTS, 4, ArmorType.LEGGINGS, 7, ArmorType.CHESTPLATE, 9, ArmorType.HELMET, 4), 15,
            SoundEvents.ARMOR_EQUIP_IRON, 3.0F, 0.0F, TTItemTags.INGOTS_VOID_METAL, VOID_FORTRESS_ASSET);
    /** Thaumcraft's ARMORMAT_SPECIAL (the Goggles of Revealing's): 1 armour, enchantability 25; brass repairs it. */
    public static final ArmorMaterial GOGGLES = new ArmorMaterial(25,
            Map.of(ArmorType.BOOTS, 1, ArmorType.LEGGINGS, 1, ArmorType.CHESTPLATE, 1, ArmorType.HELMET, 1), 25,
            SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, TagKey.create(Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "ingots/brass")),
            GOGGLES_ASSET);

    // ---- items --------------------------------------------------------------------------------------------------
    public static final DeferredItem<PrimalDestroyerItem> PRIMAL_DESTROYER = TWRegistry.item("primal_destroyer", PrimalDestroyerItem::new,
            p -> p.sword(VOIDFLAME, 3.0F, -2.4F).rarity(Rarity.EPIC).component(TTDataComponents.STACK_WARP.get(), 3)
                    .component(HUNGER.get(), 0));
    public static final DeferredItem<FlyingCarpetItem> FLYING_CARPET = TWRegistry.item("flying_carpet", FlyingCarpetItem::new,
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON)
                    .component(TTDataComponents.RECHARGEABLE.get(), new ChargeProfile(FlyingCarpetItem.CAPACITY, ChargeDisplay.ALWAYS)));
    public static final DeferredItem<VoidFortressArmorItem> VOID_FORTRESS_HELM = voidFortress("void_fortress_helm", ArmorType.HELMET);
    public static final DeferredItem<VoidFortressArmorItem> VOID_FORTRESS_CHEST = voidFortress("void_fortress_chest", ArmorType.CHESTPLATE);
    public static final DeferredItem<VoidFortressArmorItem> VOID_FORTRESS_LEGS = voidFortress("void_fortress_legs", ArmorType.LEGGINGS);
    public static final DeferredItem<NightVisionGogglesItem> NIGHT_VISION_GOGGLES = TWRegistry.item("night_vision_goggles", NightVisionGogglesItem::new,
            p -> p.humanoidArmor(GOGGLES, ArmorType.HELMET).durability(350)
                    .component(TTDataComponents.RECHARGEABLE.get(), new ChargeProfile(NightVisionGogglesItem.VIS_CAPACITY, ChargeDisplay.ALWAYS)));
    public static final DeferredItem<CleansingCharmItem> CLEANSING_CHARM = TWRegistry.item("cleansing_charm", CleansingCharmItem::new,
            p -> p.stacksTo(1).rarity(Rarity.RARE)
                    .component(TTDataComponents.RECHARGEABLE.get(), new ChargeProfile(CleansingCharmItem.VIS_CAPACITY, ChargeDisplay.ALWAYS)));
    public static final DeferredItem<BoneBowItem> BONE_BOW = TWRegistry.item("bone_bow", BoneBowItem::new,
            p -> p.durability(512).repairable(net.minecraft.world.item.Items.BONE).enchantable(1)
                    .component(TTDataComponents.RECHARGEABLE.get(), new ChargeProfile(BoneBowItem.VIS_CAPACITY, ChargeDisplay.ALWAYS)));
    public static final Map<PrimalArrowEntity.Kind, DeferredItem<PrimalArrowItem>> PRIMAL_ARROWS = arrows();

    // ---- entities -----------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<FlyingCarpetEntity>> FLYING_CARPET_ENTITY = ENTITIES.registerEntityType(
            "flying_carpet", FlyingCarpetEntity::new, MobCategory.MISC,
            b -> b.noLootTable().sized(1.0F, 0.0625F).passengerAttachments(0.1F).clientTrackingRange(4).updateInterval(1));
    public static final DeferredHolder<EntityType<?>, EntityType<PrimalArrowEntity>> PRIMAL_ARROW_ENTITY = ENTITIES.registerEntityType(
            "primal_arrow", PrimalArrowEntity::new, MobCategory.MISC,
            b -> b.noLootTable().sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20));

    // ---- recipes ------------------------------------------------------------------------------------------------
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CarpetDyeRecipe>> FLYING_CARPET_DYES =
            RECIPE_SERIALIZERS.register("flying_carpet_dyes", () -> CarpetDyeRecipe.SERIALIZER);

    private static DeferredItem<VoidFortressArmorItem> voidFortress(String name, ArmorType type) {
        return TWRegistry.item(name, VoidFortressArmorItem::new, p -> {
            Item.Properties props = p.humanoidArmor(VOID_FORTRESS, type).rarity(Rarity.EPIC).component(TTDataComponents.STACK_WARP.get(), 3);
            // The helm carries a pair of Goggles of Revealing in its mask.
            return type == ArmorType.HELMET ? props.component(TTDataComponents.GOGGLES_UPGRADE.get(), Unit.INSTANCE) : props;
        });
    }

    private static Map<PrimalArrowEntity.Kind, DeferredItem<PrimalArrowItem>> arrows() {
        Map<PrimalArrowEntity.Kind, DeferredItem<PrimalArrowItem>> map = new EnumMap<>(PrimalArrowEntity.Kind.class);
        for (PrimalArrowEntity.Kind kind : PrimalArrowEntity.Kind.values()) {
            map.put(kind, TWRegistry.item("primal_arrow_" + kind.id(), p -> new PrimalArrowItem(p, kind), p -> p));
        }
        return map;
    }

    public static Item arrowItem(PrimalArrowEntity.Kind kind) {
        return PRIMAL_ARROWS.get(kind).get();
    }

    public static boolean isVoidFortress(net.minecraft.world.item.ItemStack stack) {
        return stack.getItem() instanceof VoidFortressArmorItem;
    }

    /** How many Void Fortress pieces an entity wears over head, chest and legs. */
    public static int setCount(net.minecraft.world.entity.LivingEntity entity) {
        int n = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS}) {
            if (isVoidFortress(entity.getItemBySlot(slot))) {
                n++;
            }
        }
        return n;
    }

    public static void init(IEventBus modBus) {
        // The original's creative tab showed the Flying Carpet.
        TWRegistry.setTabIcon(FLYING_CARPET);
        modBus.addListener(GearContent::commonSetup);
        NeoForge.EVENT_BUS.register(GearEvents.class);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        // BehaviorDispensePrimalArrow: a dispenser fires the arrow with the vanilla projectile defaults.
        event.enqueueWork(() -> PRIMAL_ARROWS.values().forEach(DispenserBlock::registerProjectileBehavior));
    }
}
