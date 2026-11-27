package com.thaumicwonders.devices;

import com.thaumicwonders.data.EssentiaTanks;
import com.thaumicwonders.data.EssentiaTanks.Tank;
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

    private final List<ResourceKey<IAspect>> legacyAspects;
    private final List<String> keys;
    protected final EssentiaTanks stored = new EssentiaTanks();

    protected OrbBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, List<ResourceKey<IAspect>> aspects,
            List<String> keys) {
        super(type, pos, state);
        this.legacyAspects = aspects;
        this.keys = keys;
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

    protected @Nullable Tank tank(int slot) {
        return EssentiaTanks.Profile.of(getBlockState()).slot(slot, TANK_FACES.size());
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
        Tank tank = tank(tankOf(face));
        return tank == null ? null : tank.aspect();
    }

    @Override
    protected int roomFor(Direction face) {
        return stored.room(tank(tankOf(face)));
    }

    @Override
    protected int store(Holder<IAspect> aspect, int count, Direction face) {
        Tank tank = tank(tankOf(face));
        return tank != null && aspect.is(tank.key()) ? count - stored.add(tank, count) : count;
    }

    @Override
    public @Nullable Holder<IAspect> getEssentiaType(@Nullable Direction side) {
        Tank tank = tank(tankOf(side));
        return tank == null ? null : tank.aspect();
    }

    @Override
    public int getEssentiaAmount(@Nullable Direction side) {
        Tank tank = tank(tankOf(side));
        return tank == null ? 0 : stored.amount(tank.key());
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        stored.save(out);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        stored.load(in, () -> {
            for (int i = 0; i < legacyAspects.size(); i++) {
                stored.importAmount(legacyAspects.get(i), in.getShortOr(keys.get(i), (short) 0));
            }
        });
    }
}
