package com.thaumicwonders.alchemy;

import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

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

    public FluidStacksResourceHandler tank() {
        return tank;
    }

    public int lava() {
        return tank.getAmountAsInt(0);
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
            urn.tank.set(0, FluidResource.of(Fluids.LAVA), filled);
            urn.setChanged();
            if (filled >= CAPACITY) {
                urn.sync();
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("Tank"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("Tank").ifPresent(tank::deserialize);
    }

    /** Lava only; nothing can be poured in from outside. Emptying it from full syncs the client (for the flames). */
    private final class Tank extends FluidStacksResourceHandler {
        Tank() {
            super(1, CAPACITY);
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.getFluid().isSame(Fluids.LAVA);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        protected void onContentsChanged(int index, FluidStack previous) {
            super.onContentsChanged(index, previous);
            if (previous.getAmount() >= CAPACITY && lava() < CAPACITY) {
                sync();
            } else {
                setChanged();
            }
        }
    }
}
