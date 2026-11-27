package com.thaumicwonders.eldritch;

import com.thaumicwonders.alchemy.FluxExplosion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Avatar of Corruption's Flux Fireball, as the original's EntityFluxFireball: a vanilla-style fireball that cannot
 * be hit or deflected. A creature it strikes takes its shooter's attack damage (8, plus Strength) as mob damage, or 5
 * magic damage with no shooter; then it bursts in a strength 1 flux explosion that breaks blocks and pollutes the aura.
 */
public class FluxFireball extends AbstractHurtingProjectile {
    public FluxFireball(EntityType<? extends FluxFireball> type, Level level) {
        super(type, level);
    }

    public FluxFireball(Level level, LivingEntity shooter, Vec3 direction) {
        super(EldritchContent.FLUX_FIREBALL.get(), shooter, direction, level);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        Entity target = hit.getEntity();
        if (getOwner() instanceof LivingEntity shooter) {
            DamageSource source = damageSources().mobAttack(shooter);
            if (target.hurt(source, (float) shooter.getAttributeValue(Attributes.ATTACK_DAMAGE)) && target.isAlive()) {
                EnchantmentHelper.doPostAttackEffects(server, target, source);
            }
        } else {
            target.hurt(damageSources().magic(), 5.0F);
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level() instanceof ServerLevel server && !isRemoved()) {
            FluxExplosion.create(server, this, getX(), getY(), getZ(), 1.0F, false, true, true);
            discard();
        }
    }
}
