package com.thaumicwonders.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * Liquid quicksilver (the original's fluid_quicksilver: viscosity 1560, density 13593, quanta 4). It spreads four
 * blocks and poisons anything living in it for 100 ticks.
 */
public abstract class QuicksilverFluid extends BaseFlowingFluid {
    protected QuicksilverFluid(Properties properties) {
        super(properties);
    }

    static Properties properties() {
        return new Properties(AlchemyContent.QUICKSILVER_TYPE, AlchemyContent.QUICKSILVER, AlchemyContent.FLOWING_QUICKSILVER)
                .block(AlchemyContent.QUICKSILVER_BLOCK).bucket(AlchemyContent.QUICKSILVER_BUCKET)
                // Forge's classic fluid block: quanta 4 spreads 4 blocks, tick rate = viscosity / 200.
                .levelDecreasePerBlock(2).slopeFindDistance(2).tickRate(1560 / 200).explosionResistance(100.0F);
    }

    @Override
    protected void entityInside(Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects) {
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
        }
    }

    public static class Source extends QuicksilverFluid {
        public Source() {
            super(properties());
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }
    }

    public static class Flowing extends QuicksilverFluid {
        public Flowing() {
            super(properties());
            registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }
    }
}
