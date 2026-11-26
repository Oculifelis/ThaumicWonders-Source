package com.thaumicwonders.alchemy;

import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

/**
 * Flux Capacitor: soaks flux out of the aura and binds it (CHARGE 0..10, also its light level and comparator output).
 * Unpowered, each tick with at least 1 flux in the aura drains 1 flux and adds a charge, then waits 100-199 ticks.
 * Powered, it gives the flux back: 1 per 5 ticks. The charge travels with the item in its block_state component.
 */
public class FluxCapacitorBlock extends Block {
    public static final MapCodec<FluxCapacitorBlock> CODEC = simpleCodec(FluxCapacitorBlock::new);
    public static final int MAX_CHARGE = 10;
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, MAX_CHARGE);

    public FluxCapacitorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CHARGE, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE);
    }

    public static int getCharge(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof FluxCapacitorBlock ? state.getValue(CHARGE) : 0;
    }

    public static void decrementCharge(Level level, BlockPos pos, int amount) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof FluxCapacitorBlock) {
            level.setBlock(pos, state.setValue(CHARGE, Math.max(0, state.getValue(CHARGE) - amount)), Block.UPDATE_ALL);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        work(state, level, pos, random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        work(state, level, pos, random);
    }

    private void work(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int charge = state.getValue(CHARGE);
        if (level.hasNeighborSignal(pos)) {
            if (charge > 0) {
                AuraHelper.polluteAura(level, pos, 1.0F, true);
                level.setBlock(pos, state.setValue(CHARGE, charge - 1), Block.UPDATE_ALL);
                level.scheduleTick(pos, this, 5);
            }
        } else if (charge < MAX_CHARGE && AuraHelper.getFlux(level, pos) >= 1.0F) {
            AuraHelper.drainFlux(level, pos, 1.0F, false);
            level.setBlock(pos, state.setValue(CHARGE, charge + 1), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, 100 + random.nextInt(100));
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide() && level.hasNeighborSignal(pos)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return state.getValue(CHARGE);
    }
}
