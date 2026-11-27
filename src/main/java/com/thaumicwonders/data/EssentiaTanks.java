package com.thaumicwonders.data;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumicwonders.ThaumicWonders;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import org.jspecify.annotations.Nullable;

/** Fuel is stored by aspect so a pack can reorder or remove tanks without changing their contents. */
public final class EssentiaTanks {
    private static final Codec<Integer> POSITIVE = Codec.intRange(1, Integer.MAX_VALUE);
    private static final Codec<Map<ResourceKey<IAspect>, Integer>> AMOUNTS =
            Codec.unboundedMap(ResourceKey.codec(IAspect.REGISTRY_KEY), Codec.intRange(0, Integer.MAX_VALUE));

    public record Tank(Holder<IAspect> aspect, int capacity, int cost) {
        static final Codec<Tank> CODEC = RecordCodecBuilder.<Tank>create(i -> i.group(
                RegistryFixedCodec.create(IAspect.REGISTRY_KEY).fieldOf("aspect").forGetter(Tank::aspect),
                POSITIVE.fieldOf("capacity").forGetter(Tank::capacity),
                POSITIVE.fieldOf("cost").forGetter(Tank::cost)
        ).apply(i, Tank::new)).validate(t -> t.cost <= t.capacity ? DataResult.success(t)
                : DataResult.error(() -> "Tank cost exceeds capacity"));

        public ResourceKey<IAspect> key() {
            return aspect.unwrapKey().orElseThrow();
        }
    }

    public record Profile(List<Tank> tanks, float saveChance) {
        static final Codec<Profile> CODEC = RecordCodecBuilder.<Profile>create(i -> i.group(
                Tank.CODEC.listOf(1, 256).fieldOf("tanks").forGetter(Profile::tanks),
                Codec.floatRange(0, 1).optionalFieldOf("save_chance", 0.0F).forGetter(Profile::saveChance)
        ).apply(i, Profile::new)).validate(p -> p.tanks.stream().map(Tank::key).distinct().count() == p.tanks.size()
                ? DataResult.success(p) : DataResult.error(() -> "Each tank must have a distinct aspect"));

        public static final Profile EMPTY = new Profile(List.of(), 0);

        public static Profile of(BlockState state) {
            Profile profile = state.typeHolder().getData(TYPE);
            return profile == null ? EMPTY : profile;
        }

        /** The number of tanks is fixed for devices with one inlet per action. */
        public @Nullable Tank slot(int index, int requiredSize) {
            return tanks.size() == requiredSize && index >= 0 && index < requiredSize ? tanks.get(index) : null;
        }
    }

    public static final DataMapType<Block, Profile> TYPE = DataMapType.builder(
            ThaumicWonders.id("essentia_tanks"), Registries.BLOCK, Profile.CODEC).synced(Profile.CODEC, false).build();

    private final Map<ResourceKey<IAspect>, Integer> amounts = new HashMap<>();

    public int amount(ResourceKey<IAspect> key) {
        return amounts.getOrDefault(key, 0);
    }

    public int room(@Nullable Tank tank) {
        return tank == null ? 0 : Math.max(0, tank.capacity() - amount(tank.key()));
    }

    public int add(@Nullable Tank tank, int requested) {
        int added = Math.min(Math.max(0, requested), room(tank));
        if (tank != null && added > 0) {
            amounts.merge(tank.key(), added, Integer::sum);
        }
        return added;
    }

    public boolean funded(@Nullable Tank tank) {
        return tank != null && amount(tank.key()) >= tank.cost();
    }

    public void spend(Tank tank) {
        drain(tank, tank.cost());
    }

    public void drain(Tank tank, int used) {
        int left = amount(tank.key()) - used;
        if (left > 0) amounts.put(tank.key(), left);
        else amounts.remove(tank.key());
    }

    public void save(ValueOutput out) {
        out.store("stored_aspects", AMOUNTS, amounts);
    }

    public void load(ValueInput in, Runnable readLegacy) {
        amounts.clear();
        in.read("stored_aspects", AMOUNTS).ifPresentOrElse(amounts::putAll, readLegacy);
    }

    public void importAmount(ResourceKey<IAspect> originalAspect, int originalAmount) {
        if (originalAmount > 0) amounts.put(originalAspect, originalAmount);
    }
}
