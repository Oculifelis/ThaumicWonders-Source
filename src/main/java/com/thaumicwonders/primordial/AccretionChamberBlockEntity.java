package com.thaumicwonders.primordial;

import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.content.effect.Effects;
import com.leclowndu93150.thaumaturge.content.particle.VisSparkleParticleOptions;
import com.thaumicwonders.ThaumicWonders;
import com.thaumicwonders.data.DeviceLoot;
import com.thaumicwonders.data.EssentiaTanks;
import com.thaumicwonders.data.EssentiaTanks.Tank;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.thaumicwonders.util.ValueInput;
import com.thaumicwonders.util.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Primordial Accretion Chamber, as the original's TilePrimordialAccretionChamber: 125 of each primal essentia (taken
 * through its six side ports, one aspect at a time in alphabetical order) and one Primordial Grain make a Primordial
 * Pearl: 10% with 3 uses left (damage 5), 20% with 2 (damage 6), 70% with 1 (damage 7). An operation takes 140 ticks,
 * 80 while the vis boost lasts (the same 20-vis drain as the Catalyzation Chamber), and only counts down while the
 * essentia is full; grains thrown in while it is not full are thrown back out.
 */
public class AccretionChamberBlockEntity extends ChamberBlockEntity {
    private static final TagKey<Item> INPUTS = TagKey.create(Registries.ITEM, ThaumicWonders.id("accretion_inputs"));
    /** Positional format in worlds written before fuel profiles were configurable. */
    private static final List<ResourceKey<IAspect>> LEGACY_ORDER = List.of(TTAspects.AER, TTAspects.AQUA, TTAspects.IGNIS,
            TTAspects.ORDO, TTAspects.PERDITIO, TTAspects.TERRA);
    private final EssentiaTanks stored = new EssentiaTanks();

    public AccretionChamberBlockEntity(BlockPos pos, BlockState state) {
        super(PrimordialContent.ACCRETION_CHAMBER_BE.get(), pos, state);
    }

    private List<Tank> tanks() {
        return EssentiaTanks.Profile.of(getBlockState()).tanks();
    }

    public boolean isEssentiaFull() {
        List<Tank> tanks = tanks();
        return !tanks.isEmpty() && tanks.stream().allMatch(stored::funded);
    }

    public @Nullable ResourceKey<IAspect> currentSuction() {
        for (Tank tank : tanks()) {
            if (stored.room(tank) > 0) return tank.key();
        }
        return null;
    }

    private @Nullable Tank tankFor(ResourceKey<IAspect> aspect) {
        for (Tank tank : tanks()) {
            if (tank.key().equals(aspect)) return tank;
        }
        return null;
    }

    public int roomFor(ResourceKey<IAspect> aspect) {
        return stored.room(tankFor(aspect));
    }

    public int add(Holder<IAspect> aspect, int amount) {
        Tank tank = aspect.unwrapKey().map(this::tankFor).orElse(null);
        int added = stored.add(tank, amount);
        if (added > 0) sync();
        return amount - added;
    }

    public @Nullable ResourceKey<IAspect> largest() {
        ResourceKey<IAspect> best = null;
        int largest = 0;
        for (Tank tank : tanks()) {
            int amount = stored.amount(tank.key());
            if (amount > largest) {
                largest = amount;
                best = tank.key();
            }
        }
        return best;
    }

    public int amount(@Nullable ResourceKey<IAspect> aspect) {
        return aspect == null ? 0 : stored.amount(aspect);
    }

    @Override
    protected boolean canRefine(ItemStack stack) {
        return stack.is(INPUTS) && isEssentiaFull();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AccretionChamberBlockEntity be) {
        if (level instanceof ServerLevel server) {
            be.serverTick(server);
        }
    }

    private void serverTick(ServerLevel server) {
        boolean refined = false;
        boolean full = isEssentiaFull();
        if (refineTime > 0 && full) {
            refineTime--;
            refined = true;
        }
        if (maxRefineTime <= 0) {
            maxRefineTime = calcRefineTime();
        }
        if (refineTime > maxRefineTime) {
            refineTime = maxRefineTime;
        }
        if (refineTime <= 0 && refined) {
            for (int slot = 0; slot < SIZE; slot++) {
                ItemStack stack = stacks.get(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                if (!stack.is(INPUTS)) {
                    eject(stack.copy());
                    stacks.set(slot, ItemStack.EMPTY);
                    sync();
                    continue;
                }
                speedyTime--;
                tanks().forEach(stored::spend);
                DeviceLoot.roll(server, worldPosition, DeviceLoot.ACCRETION).forEach(this::eject);
                Vec3 from = Vec3.atCenterOf(worldPosition);
                Vec3 to = Vec3.atCenterOf(worldPosition.relative(outputSide(), 2));
                int color = Aspects.resolve(server, TTAspects.PRAECANTATIO).value().color();
                for (int i = 0; i < 5; i++) {
                    Effects.spawn(server, new VisSparkleParticleOptions(to.x, to.y, to.z, color), from.x, from.y, from.z);
                }
                popSound();
                stack.shrink(1);
                if (stack.isEmpty()) {
                    stacks.set(slot, ItemStack.EMPTY);
                }
                sync();
                break;
            }
        }
        if (speedyTime <= 0) {
            speedyTime = (int) AuraHelper.drainVis(server, worldPosition, 20.0F, false);
        }
        if (refineTime == 0 && !refined) {
            for (int slot = 0; slot < SIZE; slot++) {
                ItemStack stack = stacks.get(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                if (canRefine(stack)) {
                    maxRefineTime = refineTime = calcRefineTime();
                    break;
                }
                eject(stack);
                stacks.set(slot, ItemStack.EMPTY);
                setChanged();
            }
        }
    }

    private int calcRefineTime() {
        return speedyTime > 0 ? 80 : 140;
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag outTag, net.minecraft.core.HolderLookup.Provider outReg) {
        ValueOutput out = new ValueOutput(outTag, outReg);
        super.saveAdditional(out.tag(), out.registries());
        stored.save(out);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag inTag, net.minecraft.core.HolderLookup.Provider inReg) {
        ValueInput in = new ValueInput(inTag, inReg);
        super.loadAdditional(in.tag(), in.registries());
        stored.load(in, () -> {
            int[] saved = in.getIntArray("essentia").orElse(new int[0]);
            for (int i = 0; i < Math.min(saved.length, LEGACY_ORDER.size()); i++) {
                stored.importAmount(LEGACY_ORDER.get(i), saved[i]);
            }
        });
    }
}
