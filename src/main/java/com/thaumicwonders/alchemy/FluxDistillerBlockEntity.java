package com.thaumicwonders.alchemy;

import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.IAspectContainer;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaTransport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.thaumicwonders.util.ValueInput;
import com.thaumicwonders.util.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Flux Distiller: sits on a Flux Capacitor and, every 20 ticks, moves one charge from the capacitor into itself. At
 * 10 charge it empties the charge into 1 Vitium essentia (it holds at most 1), which tubes draw from its top face.
 */
public class FluxDistillerBlockEntity extends AlchemyBlockEntity implements IEssentiaTransport, IAspectContainer {
    public static final int PROCESS_TIME = 20;
    public static final int MAX_CHARGE = 10;
    public static final int MAX_ESSENTIA = 1;

    private int essentia;
    private int counter;

    public FluxDistillerBlockEntity(BlockPos pos, BlockState state) {
        super(AlchemyContent.FLUX_DISTILLER_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluxDistillerBlockEntity be) {
        if (++be.counter % PROCESS_TIME != 0 || !level.getBlockState(pos.below()).is(AlchemyContent.FLUX_CAPACITOR.get())) {
            return;
        }
        int charge = state.getValue(FluxDistillerBlock.CHARGE);
        if (FluxCapacitorBlock.getCharge(level, pos.below()) > 0 && charge < MAX_CHARGE) {
            FluxCapacitorBlock.decrementCharge(level, pos.below(), 1);
            level.setBlock(pos, state.setValue(FluxDistillerBlock.CHARGE, charge + 1), Block.UPDATE_ALL);
        } else if (charge >= MAX_CHARGE && be.essentia < MAX_ESSENTIA) {
            be.essentia++;
            level.setBlock(pos, state.setValue(FluxDistillerBlock.CHARGE, 0), Block.UPDATE_ALL);
            be.sync();
        }
    }

    private @Nullable Holder<IAspect> vitium() {
        return level == null ? null : level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).get(TTAspects.VITIUM).orElse(null);
    }

    // ---- essentia transport: output only, from the top ---------------------------------------------------------

    @Override
    public boolean isConnectable(Direction face) {
        return face == Direction.UP;
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return false;
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return face == Direction.UP;
    }

    @Override
    public void setSuction(@Nullable Holder<IAspect> type, int amount) {
    }

    @Override
    public @Nullable Holder<IAspect> getSuctionType(@Nullable Direction face) {
        return null;
    }

    @Override
    public int getSuctionAmount(@Nullable Direction face) {
        return 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public int takeEssentia(Holder<IAspect> type, int amount, Direction face) {
        return canOutputTo(face) && drain(type, amount) ? amount : 0;
    }

    @Override
    public int addEssentia(Holder<IAspect> type, int amount, Direction face) {
        return 0;
    }

    @Override
    public @Nullable Holder<IAspect> getEssentiaType(@Nullable Direction face) {
        return vitium();
    }

    @Override
    public int getEssentiaAmount(@Nullable Direction face) {
        return essentia;
    }

    // ---- aspect container --------------------------------------------------------------------------------------

    @Override
    public AspectList getAspects() {
        Holder<IAspect> flux = vitium();
        return flux == null || essentia <= 0 ? AspectList.EMPTY : AspectList.EMPTY.add(flux, essentia);
    }

    @Override
    public void setAspects(AspectList aspects) {
    }

    @Override
    public boolean accepts(Holder<IAspect> type) {
        return false;
    }

    @Override
    public int fill(Holder<IAspect> type, int amount) {
        return 0;
    }

    @Override
    public boolean drain(Holder<IAspect> type, int amount) {
        Holder<IAspect> flux = vitium();
        if (flux == null || !flux.equals(type) || essentia < amount) {
            return false;
        }
        essentia -= amount;
        sync();
        return true;
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag outputTag, net.minecraft.core.HolderLookup.Provider outputReg) {
        ValueOutput output = new ValueOutput(outputTag, outputReg);
        super.saveAdditional(output.tag(), output.registries());
        output.putShort("essentia", (short) essentia);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag inputTag, net.minecraft.core.HolderLookup.Provider inputReg) {
        ValueInput input = new ValueInput(inputTag, inputReg);
        super.loadAdditional(input.tag(), input.registries());
        essentia = input.getShortOr("essentia", (short) 0);
    }
}
