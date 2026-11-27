package com.thaumicwonders.primordial;

import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.thaumicwonders.devices.EssentiaDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The Primordial Accretion Chamber's placeholder blocks (the original's TilePrimordialAccretionChamberPlaceholder).
 * The six advanced-alchemical-construct placeholders level with the chamber on its left and right are essentia
 * ports: each takes the essentia the chamber currently asks for through its outward face, one per 5 ticks, with
 * suction 128. Everything else about the essentia is the chamber's.
 */
public class AccretionPortBlockEntity extends EssentiaDeviceBlockEntity {
    private @Nullable BlockPos central;

    public AccretionPortBlockEntity(BlockPos pos, BlockState state) {
        super(PrimordialContent.ACCRETION_PORT_BE.get(), pos, state);
    }

    /** The chamber this placeholder belongs to (the first one in the 3x3x3 around it). */
    public @Nullable AccretionChamberBlockEntity chamber() {
        if (level == null) {
            return null;
        }
        if (central != null && level.getBlockEntity(central) instanceof AccretionChamberBlockEntity c) {
            return c;
        }
        central = null;
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-1, -1, -1), worldPosition.offset(1, 1, 1))) {
            if (level.getBlockEntity(p) instanceof AccretionChamberBlockEntity c) {
                central = p.immutable();
                return c;
            }
        }
        return null;
    }

    private boolean isPort(Direction face) {
        AccretionChamberBlockEntity c = chamber();
        if (c == null || !face.getAxis().isHorizontal()) {
            return false;
        }
        Direction facing = c.getBlockState().getValue(ChamberBlock.FACING);
        for (int offset = -1; offset <= 1; offset++) {
            BlockPos along = c.getBlockPos().relative(facing, offset);
            if (face == facing.getCounterClockWise() && worldPosition.equals(along.relative(facing.getCounterClockWise()))) {
                return true;
            }
            if (face == facing.getClockWise() && worldPosition.equals(along.relative(facing.getClockWise()))) {
                return true;
            }
        }
        return false;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AccretionPortBlockEntity be) {
        if (level instanceof ServerLevel && ++be.tickCounter % 5 == 0) {
            AccretionChamberBlockEntity c = be.chamber();
            if (c != null && c.currentSuction() != null) {
                be.fill(java.util.List.of(Direction.values()), false);
            }
        }
    }

    private @Nullable Holder<IAspect> holder(@Nullable ResourceKey<IAspect> key) {
        return key == null || level == null ? null : Aspects.resolve(level, key);
    }

    @Override
    protected boolean acceptsFace(Direction face) {
        return isPort(face);
    }

    @Override
    protected @Nullable Holder<IAspect> wantedOn(Direction face) {
        AccretionChamberBlockEntity c = chamber();
        return c == null ? null : holder(c.currentSuction());
    }

    @Override
    protected int roomFor(Direction face) {
        AccretionChamberBlockEntity c = chamber();
        ResourceKey<IAspect> want = c == null ? null : c.currentSuction();
        return want == null ? 0 : c.roomFor(want);
    }

    @Override
    protected int store(Holder<IAspect> aspect, int amount, Direction face) {
        AccretionChamberBlockEntity c = chamber();
        return c == null ? amount : c.add(aspect, amount);
    }

    @Override
    public @Nullable Holder<IAspect> getEssentiaType(@Nullable Direction side) {
        AccretionChamberBlockEntity c = chamber();
        return c == null ? null : holder(c.largest());
    }

    @Override
    public int getEssentiaAmount(@Nullable Direction side) {
        AccretionChamberBlockEntity c = chamber();
        return c == null ? 0 : c.amount(c.largest());
    }
}
