package com.thaumicwonders.alchemy;

import com.leclowndu93150.thaumaturge.content.particle.BubbleParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The vat's block entity only makes the surface froth, one bubble a tick (Thaumaturge's crucible froth). */
public class AlkahestVatBlockEntity extends BlockEntity {
    public AlkahestVatBlockEntity(BlockPos pos, BlockState state) {
        super(AlchemyContent.ALKAHEST_VAT_BE.get(), pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, AlkahestVatBlockEntity vat) {
        RandomSource random = level.getRandom();
        BubbleParticleOptions froth = new BubbleParticleOptions(ARGB.colorFromFloat(1.0F, 0.5F, 0.5F, 0.7F), 1.0F,
                random.nextFloat() * 0.2F + 0.2F, 4 + random.nextInt(3), 0.1F, false);
        level.addParticle(froth, pos.getX() + 0.2 + random.nextFloat() * 0.6, pos.getY() + 0.8, pos.getZ() + 0.2 + random.nextFloat() * 0.6, 0, 0, 0);
    }
}
