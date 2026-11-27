package com.thaumicwonders.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

/** Hexamite: TNT in every respect except that it primes into {@link HexamitePrimed}. */
public class HexamiteBlock extends TntBlock {
    public HexamiteBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction face, @Nullable LivingEntity igniter) {
        if (level.isClientSide()) {
            return;
        }
        HexamitePrimed primed = HexamitePrimed.create(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, igniter);
        level.addFreshEntity(primed);
        level.playSound(null, primed.getX(), primed.getY(), primed.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (level.isClientSide()) {
            return;
        }
        HexamitePrimed primed = HexamitePrimed.create(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, explosion.getIndirectSourceEntity());
        int fuse = primed.getFuse();
        primed.setFuse(level.getRandom().nextInt(fuse / 4) + fuse / 8);
        level.addFreshEntity(primed);
    }
}
