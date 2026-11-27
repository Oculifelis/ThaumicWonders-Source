package com.thaumicwonders.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumicwonders.ThaumicWonders;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

public record DestroyerProfile(int hungerLimit, int hitFeed, int killFeed, float biteDamage, float fireSeconds,
        List<MobEffectInstance> biteEffects, List<MobEffectInstance> hitEffects) {
    private static final Codec<Integer> NONNEGATIVE = Codec.intRange(0, Integer.MAX_VALUE);
    private static final Codec<Float> AMOUNT = Codec.floatRange(0, Float.MAX_VALUE);
    private static final Codec<DestroyerProfile> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("hunger_limit").forGetter(DestroyerProfile::hungerLimit),
            NONNEGATIVE.fieldOf("hit_feed").forGetter(DestroyerProfile::hitFeed),
            NONNEGATIVE.fieldOf("kill_feed").forGetter(DestroyerProfile::killFeed),
            AMOUNT.fieldOf("bite_damage").forGetter(DestroyerProfile::biteDamage),
            AMOUNT.fieldOf("fire_seconds").forGetter(DestroyerProfile::fireSeconds),
            MobEffectInstance.CODEC.listOf().fieldOf("bite_effects").forGetter(DestroyerProfile::biteEffects),
            MobEffectInstance.CODEC.listOf().fieldOf("hit_effects").forGetter(DestroyerProfile::hitEffects)
    ).apply(i, DestroyerProfile::new));
    public static final DataMapType<Item, DestroyerProfile> TYPE = DataMapType.builder(
            ThaumicWonders.id("primal_destroyer"), Registries.ITEM, CODEC).synced(CODEC, false).build();

    public static @org.jetbrains.annotations.Nullable DestroyerProfile of(ItemStack stack) {
        return stack.getItemHolder().getData(TYPE);
    }

    public void afterHit(ItemStack stack, LivingEntity target) {
        hitEffects.forEach(effect -> target.addEffect(new MobEffectInstance(effect)));
        if (fireSeconds > 0) target.igniteForSeconds(fireSeconds);
        int feed = target.isDeadOrDying() ? killFeed : hitFeed;
        var hunger = com.thaumicwonders.gear.GearContent.HUNGER.get();
        stack.set(hunger, Math.max(0, stack.getOrDefault(hunger, 0) - feed));
    }
}
