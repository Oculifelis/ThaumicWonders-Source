package com.thaumicwonders.primordial;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * An invisible shell block of a chamber (the chamber's own model draws the shell). It stands for the block it was
 * made from, which it turns back into (and drops) when the chamber is taken apart; breaking it takes the chamber
 * apart. Catalyzation placeholders open the chamber's screen; the Accretion Chamber's have a block entity, of which
 * the six beside the chamber are essentia ports.
 */
public class PlaceholderBlock extends BaseEntityBlock {
    private final Supplier<? extends Block> original;
    private final boolean port;

    public PlaceholderBlock(Properties properties, Supplier<? extends Block> original, boolean port) {
        super(properties);
        this.original = original;
        this.port = port;
    }

    public Block original() {
        return original.get();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MapCodec.unit(this);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbour, Direction direction) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected int getLightBlock(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    private static @Nullable BlockPos findChamber(BlockGetter level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (level.getBlockState(p).getBlock() instanceof ChamberBlock) {
                return p.immutable();
            }
        }
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos chamber = findChamber(level, pos);
        return chamber == null ? InteractionResult.PASS : ChamberBlock.openScreen(level, chamber, player);
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        super.destroy(level, pos, state);
        if (level.isClientSide()) {
            return;
        }
        BlockPos chamber = findChamber(level, pos);
        if (chamber != null) {
            ChamberBlock.destroyChamber(level, chamber, level.getBlockState(chamber), pos);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return port ? new AccretionPortBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return port && !level.isClientSide() ? createTickerHelper(type, PrimordialContent.ACCRETION_PORT_BE.get(), AccretionPortBlockEntity::tick) : null;
    }
}
