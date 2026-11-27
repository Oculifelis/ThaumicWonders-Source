package com.thaumicwonders.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumicwonders.ThaumicWonders;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

public final class EncounterRules {
    public enum Event implements StringRepresentable {
        FLUX, INVADERS, SUBVERSION;

        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    public enum Subversion implements StringRepresentable {
        CRIMSON_PORTAL, FLUX_RIFT;

        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    public record Action<T>(T action, int belowStability) {
        static <T> MapCodec<Action<T>> codec(Codec<T> type) {
            return RecordCodecBuilder.mapCodec(i -> i.group(
                    type.fieldOf("action").forGetter(Action::action),
                    Codec.intRange(-100, 100).fieldOf("below_stability").forGetter(Action::belowStability)
            ).apply(i, Action::new));
        }
    }

    public record Invader(EntityType<?> entity, int belowStability, int stabilityPerExtra) {
        static final MapCodec<Invader> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(Invader::entity),
                Codec.intRange(-100, 100).fieldOf("below_stability").forGetter(Invader::belowStability),
                Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("stability_per_extra", 0).forGetter(Invader::stabilityPerExtra)
        ).apply(i, Invader::new));

        public int count(float stability) {
            return stabilityPerExtra == 0 ? 1 : 1 + (int) (Math.abs(stability) / stabilityPerExtra);
        }
    }

    public record Portal(WeightedList<Action<Event>> events, WeightedList<Invader> invaders,
            WeightedList<Action<Subversion>> subversions) {
        static final Codec<Portal> CODEC = RecordCodecBuilder.create(i -> i.group(
                WeightedList.codec(Action.codec(StringRepresentable.fromEnum(Event::values))).fieldOf("events").forGetter(Portal::events),
                WeightedList.codec(Invader.CODEC).fieldOf("invaders").forGetter(Portal::invaders),
                WeightedList.codec(Action.codec(StringRepresentable.fromEnum(Subversion::values))).fieldOf("subversions").forGetter(Portal::subversions)
        ).apply(i, Portal::new));

        // Draw first, then check stability. Ineligible results consume their original share of rolls.
        public Optional<Event> event(float stability, RandomSource random) {
            return events.getRandom(random).filter(e -> stability < e.belowStability()).map(Action::action);
        }

        public Optional<Invader> invader(float stability, RandomSource random) {
            return invaders.getRandom(random).filter(e -> stability < e.belowStability());
        }

        public Optional<Subversion> subversion(float stability, RandomSource random) {
            return subversions.getRandom(random).filter(e -> stability < e.belowStability()).map(Action::action);
        }
    }

    public record Seeds(int limit, int primeRolls) {
        static final Codec<Seeds> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("limit").forGetter(Seeds::limit),
                Codec.intRange(0, 10).fieldOf("prime_rolls").forGetter(Seeds::primeRolls)
        ).apply(i, Seeds::new));
    }

    public record RiftEffect(Holder<MobEffect> effect, int ticks, int baseAmplifier, int perRift) {
        private static final Codec<RiftEffect> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(RiftEffect::effect),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("ticks").forGetter(RiftEffect::ticks),
                Codec.intRange(0, 255).fieldOf("base_amplifier").forGetter(RiftEffect::baseAmplifier),
                Codec.intRange(0, 255).fieldOf("per_rift").forGetter(RiftEffect::perRift)
        ).apply(i, RiftEffect::new));

        public MobEffectInstance forRifts(int count) {
            return new MobEffectInstance(effect, ticks, Math.clamp((long) baseAmplifier + (long) perRift * count, 0, 255));
        }
    }

    public static final DataMapType<EntityType<?>, List<RiftEffect>> RIFT_EFFECTS = DataMapType.builder(
            ThaumicWonders.id("rift_effects"), Registries.ENTITY_TYPE, RiftEffect.CODEC.listOf()).build();

    public static final DataMapType<Block, Portal> PORTAL = DataMapType.builder(
            ThaumicWonders.id("portal_encounters"), Registries.BLOCK, Portal.CODEC).build();
    public static final DataMapType<EntityType<?>, Map<Difficulty, Seeds>> SEEDS = DataMapType.builder(
            ThaumicWonders.id("taint_seeds"), Registries.ENTITY_TYPE, Codec.unboundedMap(Difficulty.CODEC, Seeds.CODEC)).build();

    private EncounterRules() {
    }
}
