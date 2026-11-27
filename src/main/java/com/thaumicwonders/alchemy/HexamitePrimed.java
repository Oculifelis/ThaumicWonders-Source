package com.thaumicwonders.alchemy;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Lit Hexamite: vanilla primed TNT (fuse 80, movement, smoke, the swelling white-flash render) that ends in a
 * strength 6 incendiary flux explosion which drains vis but does not pollute.
 */
public class HexamitePrimed extends PrimedTnt {
    private @Nullable LivingEntity igniter;

    public HexamitePrimed(EntityType<? extends HexamitePrimed> type, Level level) {
        super(type, level);
        setBlockState(AlchemyContent.HEXAMITE.get().defaultBlockState());
    }

    public static HexamitePrimed create(Level level, double x, double y, double z, @Nullable LivingEntity igniter) {
        HexamitePrimed primed = new HexamitePrimed(AlchemyContent.HEXAMITE_PRIMED.get(), level);
        primed.setPos(x, y, z);
        double angle = level.getRandom().nextDouble() * Math.PI * 2;
        primed.setDeltaMovement(-Math.sin(angle) * 0.02, 0.2, -Math.cos(angle) * 0.02);
        primed.setFuse(80);
        primed.xo = x;
        primed.yo = y;
        primed.zo = z;
        primed.igniter = igniter;
        return primed;
    }

    @Override
    public @Nullable LivingEntity getOwner() {
        return igniter;
    }

    @Override
    protected void explode() {
        if (level() instanceof ServerLevel server) {
            FluxExplosion.create(server, this, getX(), getY() + getBbHeight() / 16.0, getZ(), 6.0F, true, true, false);
        }
    }
}
