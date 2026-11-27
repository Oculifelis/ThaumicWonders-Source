package com.thaumicwonders.data;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumicwonders.ThaumicWonders;
import com.thaumicwonders.gear.GearContent;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

public final class EquipmentRules {
    private static final Codec<Integer> POSITIVE = Codec.intRange(1, Integer.MAX_VALUE);
    public static final ResourceKey<Registry<Armor>> ARMORS = ResourceKey.createRegistryKey(ThaumicWonders.id("armor_material"));
    public static final ResourceKey<Registry<Sword>> SWORDS = ResourceKey.createRegistryKey(ThaumicWonders.id("sword_material"));

    public record Bonus(double armor, double toughness) {
        private static final Codec<Bonus> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("armor").forGetter(Bonus::armor),
                Codec.DOUBLE.fieldOf("toughness").forGetter(Bonus::toughness)
        ).apply(i, Bonus::new));
        private static final Bonus ZERO = new Bonus(0, 0);
    }

    public record Armor(int durability, Map<ArmorType, Integer> defense, int enchantment, float toughness,
            Identifier sound, Identifier repair, Identifier asset, Optional<Integer> fixedDurability, List<Bonus> setBonus) {
        private static final Codec<Armor> CODEC = RecordCodecBuilder.create(i -> i.group(
                POSITIVE.fieldOf("durability_factor").forGetter(Armor::durability),
                Codec.unboundedMap(ArmorType.CODEC, Codec.intRange(0, Integer.MAX_VALUE)).fieldOf("defense").forGetter(Armor::defense),
                POSITIVE.fieldOf("enchantment_value").forGetter(Armor::enchantment),
                Codec.floatRange(0, Float.MAX_VALUE).fieldOf("toughness").forGetter(Armor::toughness),
                Identifier.CODEC.fieldOf("equip_sound").forGetter(Armor::sound),
                Identifier.CODEC.fieldOf("repair_tag").forGetter(Armor::repair),
                Identifier.CODEC.fieldOf("asset").forGetter(Armor::asset),
                POSITIVE.optionalFieldOf("fixed_durability").forGetter(Armor::fixedDurability),
                Bonus.CODEC.listOf(0, 3).optionalFieldOf("set_bonus", List.of()).forGetter(Armor::setBonus)
        ).apply(i, Armor::new));

        public ArmorMaterial material() {
            return new ArmorMaterial(durability, defense, enchantment, BuiltInRegistries.SOUND_EVENT.get(sound).orElseThrow(),
                    toughness, 0, TagKey.create(Registries.ITEM, repair), ResourceKey.create(EquipmentAssets.ROOT_ID, asset));
        }

        public Bonus bonus(int pieces) {
            return pieces < 1 || pieces > setBonus.size() ? Bonus.ZERO : setBonus.get(pieces - 1);
        }

        void apply(DataComponentMap.Builder builder, HolderLookup.Provider registries, ArmorType slot) {
            ArmorMaterial material = material();
            builder.set(DataComponents.MAX_DAMAGE, fixedDurability.orElse(slot.getDurability(durability)));
            builder.set(DataComponents.ATTRIBUTE_MODIFIERS, material.createAttributes(slot));
            builder.set(DataComponents.ENCHANTABLE, new Enchantable(enchantment));
            builder.set(DataComponents.EQUIPPABLE, Equippable.builder(slot.getSlot()).setEquipSound(material.equipSound()).setAsset(material.assetId()).build());
            builder.set(DataComponents.REPAIRABLE, new Repairable(registries.lookupOrThrow(Registries.ITEM).getOrThrow(material.repairIngredient())));
        }
    }

    public record Sword(Identifier incorrectBlocks, int durability, float speed, float damageBonus, int enchantment,
            Identifier repair, float attackDamage, float attackSpeed) {
        private static final Codec<Sword> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("incorrect_blocks").forGetter(Sword::incorrectBlocks),
                POSITIVE.fieldOf("durability").forGetter(Sword::durability),
                Codec.FLOAT.fieldOf("mining_speed").forGetter(Sword::speed),
                Codec.FLOAT.fieldOf("damage_bonus").forGetter(Sword::damageBonus),
                POSITIVE.fieldOf("enchantment_value").forGetter(Sword::enchantment),
                Identifier.CODEC.fieldOf("repair_tag").forGetter(Sword::repair),
                Codec.FLOAT.fieldOf("attack_damage").forGetter(Sword::attackDamage),
                Codec.FLOAT.fieldOf("attack_speed").forGetter(Sword::attackSpeed)
        ).apply(i, Sword::new));

        public Item.Properties sword(Item.Properties properties) {
            var material = new ToolMaterial(TagKey.create(Registries.BLOCK, incorrectBlocks), durability, speed, damageBonus,
                    enchantment, TagKey.create(Registries.ITEM, repair));
            return properties.sword(material, attackDamage, attackSpeed);
        }

        void apply(DataComponentMap.Builder builder, HolderLookup.Provider registries) {
            builder.set(DataComponents.MAX_DAMAGE, durability);
            builder.set(DataComponents.ENCHANTABLE, new Enchantable(enchantment));
            builder.set(DataComponents.REPAIRABLE, new Repairable(registries.lookupOrThrow(Registries.ITEM).getOrThrow(TagKey.create(Registries.ITEM, repair))));
            builder.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, attackDamage + damageBonus, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND).add(Attributes.ATTACK_SPEED,
                    new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, attackSpeed, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND).build());
        }
    }

    // Item registration precedes world registries. Read the shipped files for that first pass.
    public static final Armor FORTRESS = bundled(ARMORS, "void_fortress", Armor.CODEC);
    public static final Armor GOGGLES = bundled(ARMORS, "goggles", Armor.CODEC);
    public static final Sword VOIDFLAME = bundled(SWORDS, "voidflame", Sword.CODEC);

    private EquipmentRules() {
    }

    private static <T> T bundled(ResourceKey<Registry<T>> key, String id, Codec<T> codec) {
        String path = "/data/" + ThaumicWonders.MODID + "/" + ThaumicWonders.MODID + "/" + key.identifier().getPath() + "/" + id + ".json";
        try (var reader = new InputStreamReader(Objects.requireNonNull(EquipmentRules.class.getResourceAsStream(path), path), StandardCharsets.UTF_8)) {
            return codec.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow();
        } catch (IOException e) {
            throw new IllegalStateException(path, e);
        }
    }

    private static <T> T lookup(HolderLookup.Provider registries, ResourceKey<Registry<T>> key, String id, T initial) {
        return registries.lookup(key).flatMap(table -> table.get(ResourceKey.create(key, ThaumicWonders.id(id))))
                .map(holder -> holder.value()).orElse(initial);
    }

    public static Armor fortress(HolderLookup.Provider registries) {
        return lookup(registries, ARMORS, "void_fortress", FORTRESS);
    }

    public static void registries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(ARMORS, Armor.CODEC, Armor.CODEC);
        event.dataPackRegistry(SWORDS, Sword.CODEC, Sword.CODEC);
    }

    public static void components(ModifyDefaultComponentsEvent event) {
        event.modify(GearContent.VOID_FORTRESS_HELM, (b, r, item) -> fortress(r).apply(b, r, ArmorType.HELMET));
        event.modify(GearContent.VOID_FORTRESS_CHEST, (b, r, item) -> fortress(r).apply(b, r, ArmorType.CHESTPLATE));
        event.modify(GearContent.VOID_FORTRESS_LEGS, (b, r, item) -> fortress(r).apply(b, r, ArmorType.LEGGINGS));
        event.modify(GearContent.NIGHT_VISION_GOGGLES, (b, r, item) -> lookup(r, ARMORS, "goggles", GOGGLES).apply(b, r, ArmorType.HELMET));
        event.modify(GearContent.PRIMAL_DESTROYER, (b, r, item) -> lookup(r, SWORDS, "voidflame", VOIDFLAME).apply(b, r));
    }
}
