package com.thaumicwonders.devices;

import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Shared essentia handling of the Meteorb and the Meaty Orb: three tanks fed through the faces to the left, right and
 * back of the control panel (relative WEST, EAST and SOUTH; the panel and the top and bottom take nothing).
 */
public abstract class OrbBlockEntity extends EssentiaDeviceBlockEntity {
    /** Relative faces in tank order. */
    private static final List<Direction> TANK_FACES = List.of(Direction.WEST, Direction.EAST, Direction.SOUTH);

    private final List<ResourceKey<IAspect>> aspects;
    private final List<String> keys;
    private final int capacity;
    protected final int[] amounts = new int[3];

    protected OrbBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, List<ResourceKey<IAspect>> aspects,
            List<String> keys, int capacity) {
        super(type, pos, state);
        this.aspects = aspects;
        this.keys = keys;
        this.capacity = capacity;
    }

    void serverTick() {
        if (++tickCounter % 5 == 0) {
            fill(Direction.Plane.HORIZONTAL, false);
        }
        activeTick();
    }

    protected void activeTick() {
    }

    /** The face relative to a panel facing north: rotate the facing to NORTH k times, then the face k times. */
    private Direction relative(Direction absolute) {
        if (absolute.getAxis().isVertical()) {
            return absolute;
        }
        Direction facing = getBlockState().getValue(OrbBlock.FACING);
        Direction result = absolute;
        while (facing != Direction.NORTH) {
            facing = facing.getClockWise();
            result = result.getClockWise();
        }
        return result;
    }

    private int tankOf(@Nullable Direction absolute) {
        return absolute == null ? -1 : TANK_FACES.indexOf(relative(absolute));
    }

    protected Holder<IAspect> aspect(int tank) {
        return Aspects.resolve(level, aspects.get(tank));
    }

    protected boolean has(int tank, int amount) {
        return amounts[tank] >= amount;
    }

    /** Reports an action's result to the player as the original's purple status message. */
    protected static void status(ServerPlayer player, String key) {
        player.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE));
    }

    /** Runs the orb's action chosen on its screen. */
    public abstract void act(ServerPlayer player, int choice);

    @Override
    protected boolean acceptsFace(Direction face) {
        return tankOf(face) >= 0;
    }

    @Override
    protected @Nullable Holder<IAspect> wantedOn(Direction face) {
        int tank = tankOf(face);
        return tank < 0 || level == null ? null : aspect(tank);
    }

    @Override
    protected int roomFor(Direction face) {
        int tank = tankOf(face);
        return tank < 0 ? 0 : capacity - amounts[tank];
    }

    @Override
    protected int store(Holder<IAspect> aspect, int count, Direction face) {
        for (int tank = 0; tank < 3; tank++) {
            if (aspect.is(aspects.get(tank))) {
                int added = Math.min(count, capacity - amounts[tank]);
                amounts[tank] += added;
                return count - added;
            }
        }
        return count;
    }

    @Override
    public @Nullable Holder<IAspect> getEssentiaType(@Nullable Direction side) {
        int tank = tankOf(side);
        return tank < 0 || level == null ? null : aspect(tank);
    }

    @Override
    public int getEssentiaAmount(@Nullable Direction side) {
        int tank = tankOf(side);
        return tank < 0 ? 0 : amounts[tank];
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        for (int tank = 0; tank < 3; tank++) {
            out.putShort(keys.get(tank), (short) amounts[tank]);
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        for (int tank = 0; tank < 3; tank++) {
            amounts[tank] = in.getShortOr(keys.get(tank), (short) 0);
        }
    }
}
