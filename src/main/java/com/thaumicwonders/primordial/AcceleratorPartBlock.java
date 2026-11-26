package com.thaumicwonders.primordial;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Primordial Accelerator Tunnel and Terminus: plain blocks with a horizontal facing, which points back at the placer
 * (the same way when sneaking), as the original's IBlockFacingHorizontal. The reaction only runs through parts
 * facing the same way as the accelerator.
 */
public class AcceleratorPartBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<AcceleratorPartBlock> CODEC = simpleCodec(AcceleratorPartBlock::new);

    public AcceleratorPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, placementFacing(context));
    }

    static net.minecraft.core.Direction placementFacing(BlockPlaceContext context) {
        boolean sneaking = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        return sneaking ? context.getHorizontalDirection() : context.getHorizontalDirection().getOpposite();
    }
}
