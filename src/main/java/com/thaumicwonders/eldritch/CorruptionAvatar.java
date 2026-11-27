package com.thaumicwonders.eldritch;

import com.thaumicwonders.data.EncounterRules;
import com.thaumicwonders.data.EncounterRules.Seeds;
import org.jspecify.annotations.Nullable;
import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.api.entity.IFluxImmune;
import com.leclowndu93150.thaumaturge.api.entity.ThaumaturgeEntityTypeTags;
import com.leclowndu93150.thaumaturge.content.effect.Effects;
import com.leclowndu93150.thaumaturge.content.entity.AbstractTaintSeed;
import com.leclowndu93150.thaumaturge.content.entity.EntityFluxRift;
import com.leclowndu93150.thaumaturge.content.entity.ai.LongRangeAttackGoal;
import com.leclowndu93150.thaumaturge.content.entity.boss.EntityThaumaturgeBoss;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.thaumicwonders.alchemy.FluxExplosion;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Avatar of Corruption, as the original's EntityCorruptionAvatar: a Thaumaturge boss (boss bar, follow range 40,
 * knockback resistance 0.95, slow regeneration, aggro and player scaling, as TC6's EntityThaumcraftBoss) with 300
 * health, 8 attack, 6 armour and speed 0.32. It throws Flux Fireballs from 4 to 24 blocks and fights hand to hand
 * closer in. It hunts every creature that is neither eldritch nor tainted (players only once they hit it, as the
 * original). Around it: 1 flux every 5 ticks; regeneration from the local flux; vis exhaustion and flux taint for
 * everything within 8 blocks; taint seeds sown every 10 seconds; and every 10 seconds each flux rift within 16 blocks
 * empowers it with Strength and Haste. Caught in a wall it bursts out in a strength 7 flux explosion. It is immune to
 * drowning and to its own fireballs and explosions.
 */
public class CorruptionAvatar extends EntityThaumaturgeBoss implements RangedAttackMob, IFluxImmune {
    private int seedCooldown;
    private boolean suffocating;

    public CorruptionAvatar(EntityType<? extends CorruptionAvatar> type, Level level) {
        super(type, level);
        this.xpReward = 50;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes().add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.ARMOR, 6.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new LongRangeAttackGoal(this, 0.8, 4.0, 30, 40, 24.0F));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 0.8, false));
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Mob, not LivingEntity: as the original (EntityLiving), players are never picked on sight.
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 0, false, false, (e, level) -> !isHorror(e)));
    }

    /** The original's IEldritchMob / ITaintedMob test: Thaumaturge's eldritch and tainted entity tags. */
    static boolean isHorror(Entity e) {
        return e.is(ThaumaturgeEntityTypeTags.ELDRITCH) || e.is(ThaumaturgeEntityTypeTags.TAINTED)
                || e instanceof CorruptionAvatar;
    }

    @Override
    public boolean considersEntityAsAlly(Entity other) {
        return isHorror(other) || super.considersEntityAsAlly(other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !isHorror(target) && super.canAttack(target);
    }

    // ---- ranged attack --------------------------------------------------------------------------------------

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (!hasLineOfSight(target)) {
            return;
        }
        swing(InteractionHand.MAIN_HAND);
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        double dx = target.getX() - getX();
        double dy = target.getY(0.5) - getY(0.5);
        double dz = target.getZ() - getZ();
        FluxFireball fireball = new FluxFireball(level(), this, new Vec3(dx, dy, dz));
        fireball.setPos(getX(), getY(0.5), getZ());
        level().addFreshEntity(fireball);
        playSound(TTSounds.EGATTACK.get(), 1.0F, 1.0F + random.nextFloat() * 0.1F);
    }

    // ---- server behaviour -----------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (tickCount % 5 == 0) {
            AuraHelper.polluteAura(level, blockPosition().above(), 1.0F, true);
        }
        if (tickCount % 40 == 0) {
            float flux = Math.min(100.0F, AuraHelper.getFlux(level, blockPosition()));
            int amp = (int) (0.5 * Math.sqrt(flux));
            addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, amp, false, false));
        }
        if (tickCount % 20 == 0) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(8.0),
                    e -> e != this && e.isAlive() && e.distanceTo(this) <= 8.0F)) {
                e.addEffect(new MobEffectInstance(TTMobEffects.INFECTIOUS_VIS_EXHAUST, 100, 3));
                e.addEffect(new MobEffectInstance(TTMobEffects.FLUX_TAINT, 100, 0));
            }
        }
        if (++seedCooldown >= 200) {
            sowSeed(level);
        }
        if (tickCount % 200 == 0) {
            drawOnRifts(level);
        }
        if (suffocating && tickCount % 20 == 0) {
            FluxExplosion.create(level, this, getX(), getY(), getZ(), 7.0F, false, true, true);
            suffocating = false;
        }
    }

    private int vitiumColor(ServerLevel level) {
        return Aspects.resolve(level, TTAspects.VITIUM).value().color();
    }

    /** Taint seeds: at most 1 / 2 / 4 within 16 blocks by difficulty, primes 0% / 10% / 20% of the time. */
    private void sowSeed(ServerLevel level) {
        var rule = seedRule(level.getDifficulty());
        if (rule == null || rule.limit() == 0) return;
        List<AbstractTaintSeed> seeds = level.getEntitiesOfClass(AbstractTaintSeed.class, getBoundingBox().inflate(16.0));
        if (seeds.size() >= rule.limit()) {
            return;
        }
        RandomSource r = getRandom();
        boolean prime = r.nextInt(10) < rule.primeRolls();
        Mob seed = (prime ? TTEntities.TAINT_SEED_PRIME.get() : TTEntities.TAINT_SEED.get()).create(level, EntitySpawnReason.MOB_SUMMONED);
        if (seed == null) {
            return;
        }
        seed.snapTo((int) (getX() + r.nextGaussian() * 5.0) + 0.5, (int) (getY() + r.nextGaussian() * 5.0),
                (int) (getZ() + r.nextGaussian() * 5.0) + 0.5, r.nextInt(360), 0.0F);
        if (seed.checkSpawnObstruction(level) && level.addFreshEntity(seed)) {
            getLookControl().setLookAt(seed, 30.0F, 30.0F);
            Effects.boltStrike(level, getEyePosition()).to(seed.getEyePosition()).color(vitiumColor(level)).width(0.6F).send();
            playSound(TTSounds.ZAP.get(), 1.0F, 1.0F);
            seedCooldown = 0;
        }
    }

    public @Nullable Seeds seedRule(net.minecraft.world.Difficulty difficulty) {
        var limits = getType().builtInRegistryHolder().getData(EncounterRules.SEEDS);
        return limits == null ? null : limits.get(difficulty);
    }

    /** Flux rifts within 16 blocks: Strength 3+n and Haste n for 10 seconds, and an arc from each rift. */
    private void drawOnRifts(ServerLevel level) {
        List<EntityFluxRift> rifts = level.getEntitiesOfClass(EntityFluxRift.class, getBoundingBox().inflate(16.0));
        int n = rifts.size();
        if (n == 0) {
            return;
        }
        applyRiftEffects(n);
        int color = vitiumColor(level);
        for (EntityFluxRift rift : rifts) {
            Effects.boltStrike(level, rift.position()).to(getEyePosition()).color(color).width(0.6F).send();
            rift.playSound(TTSounds.ZAP.get(), 1.0F, 1.0F);
        }
        EldritchContent.tellNearby(level, blockPosition(), 32.0, "event.corruption_avatar.empower");
    }

    public void applyRiftEffects(int count) {
        var effects = getType().builtInRegistryHolder().getData(EncounterRules.RIFT_EFFECTS);
        if (count > 0 && effects != null) effects.forEach(effect -> addEffect(effect.forRifts(count)));
    }

    // ---- damage ---------------------------------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypes.DROWN) || source.getEntity() instanceof CorruptionAvatar) {
            return false;
        }
        if (source.is(DamageTypes.IN_WALL)) {
            suffocating = true;
        }
        return super.hurtServer(level, source, amount);
    }

    // ---- sounds ---------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return TTSounds.EGIDLE.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return TTSounds.EGDEATH.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 500;
    }

    // ---- save -----------------------------------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("seedCooldown", seedCooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        seedCooldown = in.getIntOr("seedCooldown", 0);
    }
}
