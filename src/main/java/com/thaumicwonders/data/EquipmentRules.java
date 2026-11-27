package com.thaumicwonders.data;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumicwonders.ThaumicWonders;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Gear materials as data. Item registration precedes world registries, and 1.21.1 cannot rebuild item defaults after
 * a reload, so armour and sword stats come from the shipped files at startup. The datapack registries still drive what
 * is looked up while playing: the Void Fortress set bonus and the defense its fire, explosion and magic ratios use.
 */
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

    public record Armor(int durability, Map<ArmorItem.Type, Integer> defense, int enchantment, float toughness,
            ResourceLocation sound, ResourceLocation repair, ResourceLocation asset, Optional<Integer> fixedDurability, List<Bonus> setBonus) {
        private static final Codec<Armor> CODEC = RecordCodecBuilder.create(i -> i.group(
                POSITIVE.fieldOf("durability_factor").forGetter(Armor::durability),
                Codec.unboundedMap(ArmorItem.Type.CODEC, Codec.intRange(0, Integer.MAX_VALUE)).fieldOf("defense").forGetter(Armor::defense),
                POSITIVE.fieldOf("enchantment_value").forGetter(Armor::enchantment),
                Codec.floatRange(0, Float.MAX_VALUE).fieldOf("toughness").forGetter(Armor::toughness),
                ResourceLocation.CODEC.fieldOf("equip_sound").forGetter(Armor::sound),
                ResourceLocation.CODEC.fieldOf("repair_tag").forGetter(Armor::repair),
                ResourceLocation.CODEC.fieldOf("asset").forGetter(Armor::asset),
                POSITIVE.optionalFieldOf("fixed_durability").forGetter(Armor::fixedDurability),
                Bonus.CODEC.listOf(0, 3).optionalFieldOf("set_bonus", List.of()).forGetter(Armor::setBonus)
        ).apply(i, Armor::new));

        /** A 1.21.1 armour material for registration; BODY falls back to the chestplate value. */
        public ArmorMaterial material() {
            Map<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
            for (ArmorItem.Type type : ArmorItem.Type.values()) {
                int fallback = type == ArmorItem.Type.BODY ? defense.getOrDefault(ArmorItem.Type.CHESTPLATE, 0) : 0;
                map.put(type, defense.getOrDefault(type, fallback));
            }
            TagKey<Item> repairTag = TagKey.create(Registries.ITEM, repair);
            return new ArmorMaterial(map, enchantment, BuiltInRegistries.SOUND_EVENT.getHolder(sound).orElseThrow(),
                    () -> Ingredient.of(repairTag), List.of(new ArmorMaterial.Layer(asset)), toughness, 0.0F);
        }

        public int durability(ArmorItem.Type type) {
            return fixedDurability.orElseGet(() -> type.getDurability(durability));
        }

        public int defense(ArmorItem.Type type) {
            return defense.getOrDefault(type, 0);
        }

        public Bonus bonus(int pieces) {
            return pieces < 1 || pieces > setBonus.size() ? Bonus.ZERO : setBonus.get(pieces - 1);
        }
    }

    public record Sword(ResourceLocation incorrectBlocks, int durability, float speed, float damageBonus, int enchantment,
            ResourceLocation repair, float attackDamage, float attackSpeed) {
        private static final Codec<Sword> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("incorrect_blocks").forGetter(Sword::incorrectBlocks),
                POSITIVE.fieldOf("durability").forGetter(Sword::durability),
                Codec.FLOAT.fieldOf("mining_speed").forGetter(Sword::speed),
                Codec.FLOAT.fieldOf("damage_bonus").forGetter(Sword::damageBonus),
                POSITIVE.fieldOf("enchantment_value").forGetter(Sword::enchantment),
                ResourceLocation.CODEC.fieldOf("repair_tag").forGetter(Sword::repair),
                Codec.FLOAT.fieldOf("attack_damage").forGetter(Sword::attackDamage),
                Codec.FLOAT.fieldOf("attack_speed").forGetter(Sword::attackSpeed)
        ).apply(i, Sword::new));

        public Tier tier() {
            TagKey<Item> repairTag = TagKey.create(Registries.ITEM, repair);
            return new SimpleTier(TagKey.create(Registries.BLOCK, incorrectBlocks), durability, speed, damageBonus, enchantment,
                    () -> Ingredient.of(repairTag));
        }

        public Item.Properties sword(Item.Properties properties) {
            return properties.durability(durability).attributes(ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, attackDamage + damageBonus, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND).add(Attributes.ATTACK_SPEED,
                    new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, attackSpeed, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND).build());
        }
    }

    public static final Armor FORTRESS = bundled(ARMORS, "void_fortress", Armor.CODEC);
    public static final Armor GOGGLES = bundled(ARMORS, "goggles", Armor.CODEC);
    public static final Sword VOIDFLAME = bundled(SWORDS, "voidflame", Sword.CODEC);

    private EquipmentRules() {
    }

    private static <T> T bundled(ResourceKey<Registry<T>> key, String id, Codec<T> codec) {
        String path = "/data/" + ThaumicWonders.MODID + "/" + ThaumicWonders.MODID + "/" + key.location().getPath() + "/" + id + ".json";
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
}
