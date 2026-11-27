package com.thaumicwonders.data;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumicwonders.ThaumicWonders;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

public final class DeviceRules {
    public record Catalyst(int fluxChance, Holder<IAspect> sparkle) {
        static final Codec<Catalyst> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("flux_roll").forGetter(Catalyst::fluxChance),
                RegistryFixedCodec.create(IAspect.REGISTRY_KEY).fieldOf("sparkle").forGetter(Catalyst::sparkle)
        ).apply(i, Catalyst::new));
    }

    public record Target(Identifier tag, Component label) {
        static final Codec<Target> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("tag").forGetter(Target::tag),
                ComponentSerialization.CODEC.fieldOf("label").forGetter(Target::label)
        ).apply(i, Target::new));
    }

    public record Diviner(int radius, List<Target> targets) {
        static final Codec<Diviner> CODEC = RecordCodecBuilder.<Diviner>create(i -> i.group(
                Codec.intRange(1, 1024).fieldOf("search_radius").forGetter(Diviner::radius),
                Target.CODEC.listOf().fieldOf("targets").forGetter(Diviner::targets)
        ).apply(i, Diviner::new)).validate(p -> p.targets.stream().map(Target::tag).distinct().count() == p.targets.size()
                ? DataResult.success(p) : DataResult.error(() -> "Duplicate structure tag"));
        private static final Diviner EMPTY = new Diviner(1, List.of());

        public static Diviner of(ItemStack stack) {
            Diviner rule = stack.typeHolder().getData(DIVINER);
            return rule == null ? EMPTY : rule;
        }

        public boolean allows(Identifier tag) {
            return targets.stream().anyMatch(t -> t.tag.equals(tag));
        }
    }

    public record OreColor(String keyword, int rgb) {
        static final Codec<OreColor> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("keyword").forGetter(OreColor::keyword),
                Codec.intRange(0, 0xFFFFFF).fieldOf("rgb").forGetter(OreColor::rgb)
        ).apply(i, OreColor::new));
    }

    public record Palette(int fallback, List<OreColor> colors) {
        private static final Codec<Palette> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 0xFFFFFF).fieldOf("fallback").forGetter(Palette::fallback),
                OreColor.CODEC.listOf().fieldOf("colors").forGetter(Palette::colors)
        ).apply(i, Palette::new));
    }

    public static final DataMapType<Block, Palette> ORE_PALETTE = DataMapType.builder(
            ThaumicWonders.id("ore_diviner_palette"), Registries.BLOCK, Palette.CODEC).synced(Palette.CODEC, false).build();

    private static final Codec<List<Integer>> BEACON_CODEC = Codec.intRange(1, 100).listOf(5, 5);
    public static final DataMapType<Block, List<Integer>> BEACON_COST = DataMapType.builder(
            ThaumicWonders.id("beacon_cost"), Registries.BLOCK, BEACON_CODEC).synced(BEACON_CODEC, false).build();
    public static final DataMapType<Item, Catalyst> CATALYST = DataMapType.builder(
            ThaumicWonders.id("catalyst"), Registries.ITEM, Catalyst.CODEC).synced(Catalyst.CODEC, false).build();
    public static final DataMapType<Item, Diviner> DIVINER = DataMapType.builder(
            ThaumicWonders.id("structure_diviner"), Registries.ITEM, Diviner.CODEC).synced(Diviner.CODEC, false).build();

    private DeviceRules() {
    }

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(EssentiaTanks.TYPE);
        event.register(EncounterRules.PORTAL);
        event.register(EncounterRules.SEEDS);
        event.register(EncounterRules.RIFT_EFFECTS);
        event.register(ArrowProfile.TYPE);
        event.register(DestroyerProfile.TYPE);
        event.register(ORE_PALETTE);
        event.register(BEACON_COST);
        event.register(CATALYST);
        event.register(DIVINER);
    }
}
