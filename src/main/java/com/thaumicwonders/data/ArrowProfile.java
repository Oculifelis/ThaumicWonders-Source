package com.thaumicwonders.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumicwonders.ThaumicWonders;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import org.jetbrains.annotations.Nullable;

public record ArrowProfile(double damage, boolean pierceArmor, boolean fireDamage, boolean extinguish,
        float igniteSeconds, float burningSeconds, Optional<Float> waterInertia, int infinityRoll, List<MobEffectInstance> effects) {
    private static final Codec<Float> SECONDS = Codec.floatRange(0, Float.MAX_VALUE);
    private static final Codec<ArrowProfile> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("damage_multiplier", 1.0).forGetter(ArrowProfile::damage),
            Codec.BOOL.optionalFieldOf("pierce_armor", false).forGetter(ArrowProfile::pierceArmor),
            Codec.BOOL.optionalFieldOf("fire_damage", false).forGetter(ArrowProfile::fireDamage),
            Codec.BOOL.optionalFieldOf("extinguish", false).forGetter(ArrowProfile::extinguish),
            SECONDS.optionalFieldOf("ignite_seconds", 0.0F).forGetter(ArrowProfile::igniteSeconds),
            SECONDS.optionalFieldOf("burning_ignite_seconds", 0.0F).forGetter(ArrowProfile::burningSeconds),
            Codec.floatRange(0, 1).optionalFieldOf("water_inertia").forGetter(ArrowProfile::waterInertia),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("infinity_roll", 0).forGetter(ArrowProfile::infinityRoll),
            MobEffectInstance.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(ArrowProfile::effects)
    ).apply(i, ArrowProfile::new));
    public static final DataMapType<Item, ArrowProfile> TYPE = DataMapType.builder(
            ThaumicWonders.id("primal_arrow"), Registries.ITEM, CODEC).synced(CODEC, false).build();
    private static final ArrowProfile PLAIN = new ArrowProfile(1, false, false, false, 0, 0, Optional.empty(), 0, List.of());

    public static ArrowProfile of(Item item) {
        ArrowProfile rule = item.builtInRegistryHolder().getData(TYPE);
        return rule == null ? PLAIN : rule;
    }

    public void applyEffects(LivingEntity target, @Nullable Entity source) {
        for (MobEffectInstance effect : effects) target.addEffect(new MobEffectInstance(effect), source);
    }
}
