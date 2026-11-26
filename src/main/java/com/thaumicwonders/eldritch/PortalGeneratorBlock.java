package com.thaumicwonders.eldritch;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

/**
 * Portal Generator: ENABLED while it gets no redstone signal. Placed from a linked item with three free blocks above,
 * it opens its Void Portal at once; switching the signal off and on closes and reopens it. Breaking it (or blowing it
 * up) closes the portal.
 */
public class PortalGeneratorBlock extends BaseEntityBlock {
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    public static final MapCodec<PortalGeneratorBlock> CODEC = simpleCodec(PortalGeneratorBlock::new);

    public PortalGeneratorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ENABLED, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ENABLED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(ENABLED, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean moved) {
        updateEnabled(state, level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        updateEnabled(state, level, pos);
    }

    private static void updateEnabled(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        boolean shouldBe = !level.hasNeighborSignal(pos);
        if (state.getValue(ENABLED) != shouldBe) {
            level.setBlock(pos, state.setValue(ENABLED, shouldBe), Block.UPDATE_ALL);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        GlobalPos link = stack.get(EldritchContent.PORTAL_LINK.get());
        if (level.isClientSide() || link == null) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof PortalGeneratorBlockEntity generator) {
            // The original only kept the link when it could open the portal at once; here the link is always kept,
            // so a generator placed under something still works once the space is cleared and it is re-powered.
            generator.setLink(link);
            if (level.isEmptyBlock(pos.above()) && level.isEmptyBlock(pos.above(2)) && level.isEmptyBlock(pos.above(3))) {
                generator.spawnPortal();
            }
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortalGeneratorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, EldritchContent.PORTAL_GENERATOR_BE.get(), PortalGeneratorBlockEntity::serverTick);
    }
}
