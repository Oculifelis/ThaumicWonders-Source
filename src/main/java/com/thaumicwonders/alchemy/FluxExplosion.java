package com.thaumicwonders.alchemy;

import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.jspecify.annotations.Nullable;

/**
 * The original's FluxExplosion: a vanilla explosion that also works on the aura. Every affected air position drains
 * 1 vis; every affected solid block pollutes 1 flux when {@code causesFlux}; every living thing caught in it gets
 * Infectious Vis Exhaustion III for 1200 ticks. Used by Hexamite here and by the Flux Fireball (eldritch group).
 */
public final class FluxExplosion {
    /** causesFlux of the flux explosion currently running on this thread (the Detonate event fires inside explode). */
    private static final ThreadLocal<Boolean> CURRENT = new ThreadLocal<>();

    private FluxExplosion() {
    }

    public static void create(ServerLevel level, @Nullable Entity source, double x, double y, double z, float strength,
                              boolean flaming, boolean damagesTerrain, boolean causesFlux) {
        Boolean outer = CURRENT.get();
        CURRENT.set(causesFlux);
        try {
            level.explode(source, x, y, z, strength, flaming, damagesTerrain ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
        } finally {
            if (outer == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(outer);
            }
        }
    }

    static void onDetonate(ExplosionEvent.Detonate event) {
        Boolean causesFlux = CURRENT.get();
        if (causesFlux == null) {
            return;
        }
        Level level = event.getLevel();
        for (BlockPos pos : event.getAffectedBlocks()) {
            if (level.getBlockState(pos).isAir()) {
                AuraHelper.drainVis(level, pos, 1.0F, false);
            } else if (causesFlux) {
                AuraHelper.polluteAura(level, pos, 1.0F, true);
            }
        }
        for (Entity entity : event.getAffectedEntities()) {
            if (entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(TTMobEffects.INFECTIOUS_VIS_EXHAUST, 1200, 2));
            }
        }
    }
}
