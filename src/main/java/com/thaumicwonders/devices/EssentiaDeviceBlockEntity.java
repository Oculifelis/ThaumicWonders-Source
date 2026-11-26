package com.thaumicwonders.devices;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCapabilities;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaTransport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A device that only takes essentia in (the original's "common essentia fill" pattern). Every fill it pulls one
 * essentia per allowed face from a neighbouring tube or jar whose suction is below its own; suction is 128 while the
 * face still has room and 0 once full. Spilled essentia (should a neighbour give more than fits) pollutes the aura.
 */
public abstract class EssentiaDeviceBlockEntity extends DeviceBlockEntity implements IEssentiaTransport {
    public static final int SUCTION = 128;

    protected EssentiaDeviceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Whether tubes may connect on this (absolute) face. */
    protected abstract boolean acceptsFace(Direction face);

    /** The aspect this face takes, or null for "any" (only meaningful while {@link #roomFor} is positive). */
    protected abstract @Nullable Holder<IAspect> wantedOn(Direction face);

    /** How much more of that face's essentia fits. */
    protected abstract int roomFor(Direction face);

    /** Stores essentia that came in through a face; returns the leftover that did not fit. */
    protected abstract int store(Holder<IAspect> aspect, int amount, Direction face);

    /** Pulls essentia through every allowed face; with {@code stopWhenFull} it stops once a face reports no room. */
    protected void fill(Iterable<Direction> faces, boolean stopWhenFull) {
        if (level == null) {
            return;
        }
        boolean changed = false;
        for (Direction face : faces) {
            if (!acceptsFace(face)) {
                continue;
            }
            if (roomFor(face) <= 0) {
                if (stopWhenFull) {
                    break;
                }
                continue;
            }
            int suction = getSuctionAmount(face);
            Direction opposite = face.getOpposite();
            IEssentiaTransport other = level.getCapability(EssentiaCapabilities.TRANSPORT, worldPosition.relative(face), opposite);
            if (other == null || other == this || !other.canOutputTo(opposite) || other.getEssentiaAmount(opposite) <= 0
                    || suction <= other.getSuctionAmount(opposite) || suction < other.getMinimumSuction()) {
                continue;
            }
            Holder<IAspect> type = other.getEssentiaType(opposite);
            Holder<IAspect> wanted = wantedOn(face);
            if (type == null || (wanted != null && !type.equals(wanted))) {
                continue;
            }
            int taken = other.takeEssentia(type, 1, opposite);
            if (taken <= 0) {
                continue;
            }
            int leftover = store(type, taken, face);
            if (leftover > 0) {
                AuraHelper.polluteAura(level, worldPosition, leftover, true);
            }
            changed = true;
        }
        if (changed) {
            sync();
        }
    }

    @Override
    public boolean isConnectable(Direction side) {
        return acceptsFace(side);
    }

    @Override
    public boolean canInputFrom(Direction side) {
        return acceptsFace(side);
    }

    @Override
    public boolean canOutputTo(Direction side) {
        return false;
    }

    @Override
    public void setSuction(@Nullable Holder<IAspect> type, int amount) {
    }

    @Override
    public @Nullable Holder<IAspect> getSuctionType(@Nullable Direction side) {
        return side == null || !acceptsFace(side) ? null : wantedOn(side);
    }

    @Override
    public int getSuctionAmount(@Nullable Direction side) {
        return side != null && acceptsFace(side) && roomFor(side) > 0 ? SUCTION : 0;
    }

    @Override
    public int takeEssentia(Holder<IAspect> type, int amount, Direction side) {
        return 0;
    }

    @Override
    public int addEssentia(Holder<IAspect> type, int amount, Direction side) {
        return addEssentia(type, amount, side, false);
    }

    @Override
    public int addEssentia(Holder<IAspect> type, int amount, Direction side, boolean simulate) {
        if (!acceptsFace(side) || amount <= 0) {
            return 0;
        }
        Holder<IAspect> wanted = wantedOn(side);
        if (wanted != null && !wanted.equals(type)) {
            return 0;
        }
        int accepted = Math.min(amount, roomFor(side));
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            store(type, accepted, side);
            sync();
        }
        return accepted;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }
}
