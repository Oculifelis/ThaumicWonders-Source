package com.thaumicwonders.primordial;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The centre of the Catalyzation Chamber or the Primordial Accretion Chamber (made by the dust trigger from a
 * quicksilver source; its model draws the whole 3x3x3 shell). Things standing in it are nudged to the middle; every
 * 10 ticks items float up a little and, once on the bottom, go into the chamber, and living things are poisoned for
 * 5 seconds. Breaking it (or any of its placeholders) turns the placeholders back into their blocks, puts iron bars
 * back into an empty opening, and leaves a quicksilver source in the middle.
 */
public class ChamberBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape COLLISION = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);

    public enum Kind {
        CATALYZATION, ACCRETION
    }

    private final Kind kind;

    public ChamberBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MapCodec.unit(this);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        double dx = nudge(entity.getX() - pos.getX());
        double dz = nudge(entity.getZ() - pos.getZ());
        if (dx != 0.0 || dz != 0.0) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(dx, 0.0, dz));
        }
        if (!(level instanceof ServerLevel server) || entity.tickCount % 10 != 0) {
            return;
        }
        if (entity instanceof ItemEntity item) {
            item.setDeltaMovement(item.getDeltaMovement().x, 0.025, item.getDeltaMovement().z);
            if (item.onGround() && server.getBlockEntity(pos) instanceof ChamberBlockEntity chamber) {
                item.setItem(chamber.addItemsToInventory(item.getItem()));
            }
        } else if (entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
        }
    }

    private static double nudge(double offset) {
        if (offset < 0.3) {
            return 0.0001;
        }
        return offset > 0.7 ? -0.0001 : 0.0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openScreen(level, pos, player);
    }

    /** Opens the stone socket of a Catalyzation Chamber (the Accretion Chamber has no screen). */
    static InteractionResult openScreen(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof CatalyzationChamberBlockEntity chamber)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            player.openMenu(chamber);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        super.destroy(level, pos, state);
        if (!level.isClientSide()) {
            destroyChamber(level, pos, state, pos);
        }
    }

    /** Takes the multiblock apart; {@code broken} is the block being broken, left alone. */
    static void destroyChamber(LevelAccessor level, BlockPos chamberPos, BlockState chamberState, BlockPos broken) {
        for (BlockPos p : BlockPos.betweenClosed(chamberPos.offset(-1, -1, -1), chamberPos.offset(1, 1, 1))) {
            if (p.equals(broken)) {
                continue;
            }
            BlockState current = level.getBlockState(p);
            if (current.getBlock() instanceof PlaceholderBlock placeholder) {
                level.setBlock(p.immutable(), placeholder.original().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        if (chamberState.hasProperty(FACING)) {
            BlockPos opening = chamberPos.relative(chamberState.getValue(FACING).getOpposite());
            if (!opening.equals(broken) && level.getBlockState(opening).isAir()) {
                level.setBlock(opening, Block.updateFromNeighbourShapes(Blocks.IRON_BARS.defaultBlockState(), level, opening), Block.UPDATE_ALL);
            }
        }
        Block quicksilver = BuiltInRegistries.BLOCK.get(com.thaumicwonders.ThaumicWonders.id("fluid_quicksilver"));
        level.setBlock(chamberPos, (quicksilver == Blocks.AIR ? Blocks.WATER : quicksilver).defaultBlockState(), Block.UPDATE_ALL);
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ChamberBlockEntity be) {
            be.onBlockRemoved(pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return kind == Kind.CATALYZATION ? new CatalyzationChamberBlockEntity(pos, state) : new AccretionChamberBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return kind == Kind.CATALYZATION
                ? createTickerHelper(type, PrimordialContent.CATALYZATION_CHAMBER_BE.get(), CatalyzationChamberBlockEntity::tick)
                : createTickerHelper(type, PrimordialContent.ACCRETION_CHAMBER_BE.get(), AccretionChamberBlockEntity::tick);
    }
}
