package com.thaumicwonders.gear;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

/**
 * One entity for all six Primal Arrows, told apart by a synced, saved type. Air and order arrows ignore armour and
 * fire arrows do fire damage (both in {@link GearEvents}); earth arrows hit 1.5x as hard, order and entropy 0.8x.
 * Water arrows fly through water undamped and never burn; fire arrows always set their target alight. On a hit,
 * water slows (Slowness V, 10 s), order weakens (Weakness V, 10 s) and entropy withers (Wither I, 5 s). Like the
 * original, they cannot be picked back up outside creative.
 */
public class PrimalArrowEntity extends AbstractArrow {
    private static final EntityDataAccessor<Integer> ARROW_TYPE = SynchedEntityData.defineId(PrimalArrowEntity.class, EntityDataSerializers.INT);
    private static final double BASE_DAMAGE = 2.0;

    public enum Kind implements StringRepresentable {
        AIR(1.0), EARTH(1.5), FIRE(1.0), WATER(1.0), ORDER(0.8), ENTROPY(0.8);

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final double damage;

        Kind(double damage) {
            this.damage = damage;
        }

        public String id() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        @Override
        public String getSerializedName() {
            return this.id();
        }

        public double damage() {
            return this.damage;
        }

        public static Kind byOrdinal(int i) {
            Kind[] all = values();
            return i >= 0 && i < all.length ? all[i] : AIR;
        }
    }

    public PrimalArrowEntity(EntityType<? extends PrimalArrowEntity> type, Level level) {
        super(type, level);
        this.pickup = Pickup.CREATIVE_ONLY;
    }

    public PrimalArrowEntity(Level level, LivingEntity owner, ItemStack ammo, @Nullable ItemStack weapon, Kind kind) {
        super(GearContent.PRIMAL_ARROW_ENTITY.get(), owner, level, ammo, weapon);
        this.setKind(kind);
        this.setBaseDamage(BASE_DAMAGE * kind.damage());
        this.pickup = Pickup.CREATIVE_ONLY;
    }

    public PrimalArrowEntity(Level level, double x, double y, double z, ItemStack ammo, Kind kind) {
        super(GearContent.PRIMAL_ARROW_ENTITY.get(), x, y, z, level, ammo, null);
        this.setKind(kind);
        this.setBaseDamage(BASE_DAMAGE * kind.damage());
        this.pickup = Pickup.CREATIVE_ONLY;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ARROW_TYPE, 0);
    }

    public Kind kind() {
        return Kind.byOrdinal(this.entityData.get(ARROW_TYPE));
    }

    public void setKind(Kind kind) {
        this.entityData.set(ARROW_TYPE, kind.ordinal());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("arrow_type", Kind.CODEC, this.kind());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setKind(input.read("arrow_type", Kind.CODEC).orElse(Kind.AIR));
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(GearContent.arrowItem(this.kind()));
    }

    @Override
    public void tick() {
        // Water arrows cannot burn.
        if (this.kind() == Kind.WATER && this.isOnFire()) {
            this.clearFire();
        }
        super.tick();
    }

    @Override
    protected float getWaterInertia() {
        // Water arrows fly normally underwater.
        return this.kind() == Kind.WATER ? 0.99F : super.getWaterInertia();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (this.kind() == Kind.FIRE && !(hit.getEntity() instanceof EnderMan)) {
            // Fire arrows always light the target, longer if the arrow itself burns.
            hit.getEntity().igniteForSeconds(this.isOnFire() ? 10.0F : 5.0F);
        }
        super.onHitEntity(hit);
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        switch (this.kind()) {
            case WATER -> target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 4), this.getEffectSource());
            case ORDER -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 4), this.getEffectSource());
            case ENTROPY -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100), this.getEffectSource());
            default -> {
            }
        }
    }
}
