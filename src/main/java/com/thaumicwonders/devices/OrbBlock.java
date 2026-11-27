package com.thaumicwonders.devices;

import com.mojang.serialization.MapCodec;
import java.util.function.BiFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Meteorb and Meaty Orb: a horizontal-facing device whose control panel faces the placer. Using it opens its
 * screen on the client, in the overworld-like dimensions only (the original's {@code isSurfaceWorld}).
 */
public class OrbBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public enum Kind {
        METEORB("event.meteorb.offworld"),
        MEATY_ORB("event.meaty_orb.offworld");

        private final String offworld;

        Kind(String offworld) {
            this.offworld = offworld;
        }
    }

    private final Kind kind;
    private final BiFunction<BlockPos, BlockState, ? extends OrbBlockEntity> factory;

    public OrbBlock(Properties properties, Kind kind, BiFunction<BlockPos, BlockState, ? extends OrbBlockEntity> factory) {
        super(properties);
        this.kind = kind;
        this.factory = factory;
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
        Direction placer = context.getHorizontalDirection();
        boolean sneaking = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        return defaultBlockState().setValue(FACING, sneaking ? placer : placer.getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            if (DevicesContent.isSurfaceWorld(level)) {
                DevicesContent.clientHooks().openOrb(kind, pos);
            } else {
                player.displayClientMessage(Component.translatable(kind.offworld).withStyle(ChatFormatting.DARK_PURPLE), true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof OrbBlockEntity orb) {
                orb.serverTick();
            }
        };
    }
}
