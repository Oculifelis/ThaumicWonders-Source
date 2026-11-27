package com.thaumicwonders.alchemy;

import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import com.thaumicwonders.util.ValueInput;
import com.thaumicwonders.util.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Everburning Urn: a 1000 mB lava tank that refills itself from the aura. Every 5 ticks, while not full, it drains up
 * to 0.1 vis and turns it into 40 mB of lava per vis (at most 4 mB per 5 ticks; a full tank costs 25 vis). The tank
 * cannot be filled from outside and is reachable only through its top face.
 */
public class EverburningUrnBlockEntity extends AlchemyBlockEntity {
    public static final int CAPACITY = 1000;
    public static final int MAX_PER_FILL = 40;

    private final Tank tank = new Tank();
    private int counter;

    public EverburningUrnBlockEntity(BlockPos pos, BlockState state) {
        super(AlchemyContent.EVERBURNING_URN_BE.get(), pos, state);
    }

    public IFluidHandler tank() {
        return tank;
    }

    public int lava() {
        return tank.getFluidAmount();
    }

    public boolean isFull() {
        return lava() >= CAPACITY;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EverburningUrnBlockEntity urn) {
        urn.counter++;
        if (urn.counter % 5 != 0) {
            return;
        }
        int amount = urn.lava();
        if (amount >= CAPACITY) {
            return;
        }
        float visToDrain = Math.min((CAPACITY - amount) / (float) MAX_PER_FILL, 0.1F);
        float actual = AuraHelper.drainVis(level, pos, visToDrain, false);
        int mbToAdd = (int) (MAX_PER_FILL * actual);
        if (mbToAdd > 0) {
            int filled = Math.min(CAPACITY, amount + mbToAdd);
            urn.tank.setFluid(new FluidStack(Fluids.LAVA, filled));
            urn.setChanged();
            if (filled >= CAPACITY) {
                urn.sync();
            }
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag outputTag, net.minecraft.core.HolderLookup.Provider outputReg) {
        ValueOutput output = new ValueOutput(outputTag, outputReg);
        super.saveAdditional(output.tag(), output.registries());
        output.tag().put("Tank", tank.writeToNBT(output.registries(), new net.minecraft.nbt.CompoundTag()));
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag inputTag, net.minecraft.core.HolderLookup.Provider inputReg) {
        ValueInput input = new ValueInput(inputTag, inputReg);
        super.loadAdditional(input.tag(), input.registries());
        tank.readFromNBT(input.registries(), input.tag().getCompound("Tank"));
    }

    /** Lava only; nothing can be poured in from outside. Emptying it from full syncs the client (for the flames). */
    private final class Tank extends FluidTank {
        Tank() {
            super(CAPACITY, stack -> stack.getFluid().isSame(Fluids.LAVA));
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            int before = getFluidAmount();
            FluidStack out = super.drain(maxDrain, action);
            if (action.execute() && !out.isEmpty() && before >= CAPACITY) {
                sync();
            }
            return out;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(resource, fluid)) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    }
}
