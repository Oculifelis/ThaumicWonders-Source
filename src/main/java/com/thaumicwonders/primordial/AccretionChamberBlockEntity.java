package com.thaumicwonders.primordial;

import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.content.effect.Effects;
import com.leclowndu93150.thaumaturge.content.particle.VisSparkleParticleOptions;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Primordial Accretion Chamber, as the original's TilePrimordialAccretionChamber: 125 of each primal essentia (taken
 * through its six side ports, one aspect at a time in alphabetical order) and one Primordial Grain make a Primordial
 * Pearl: 10% with 3 uses left (damage 5), 20% with 2 (damage 6), 70% with 1 (damage 7). An operation takes 140 ticks,
 * 80 while the vis boost lasts (the same 20-vis drain as the Catalyzation Chamber), and only counts down while the
 * essentia is full; grains thrown in while it is not full are thrown back out.
 */
public class AccretionChamberBlockEntity extends ChamberBlockEntity {
    public static final int CAPACITY = 125;
    /** The original's REQUIRED_FUEL sorted by name: the order the ports ask for essentia in. */
    public static final List<ResourceKey<IAspect>> FUEL = List.of(TTAspects.AER, TTAspects.AQUA, TTAspects.IGNIS, TTAspects.ORDO,
            TTAspects.PERDITIO, TTAspects.TERRA);

    private final int[] essentia = new int[FUEL.size()];

    public AccretionChamberBlockEntity(BlockPos pos, BlockState state) {
        super(PrimordialContent.ACCRETION_CHAMBER_BE.get(), pos, state);
    }

    public boolean isEssentiaFull() {
        for (int amount : essentia) {
            if (amount < CAPACITY) {
                return false;
            }
        }
        return true;
    }

    /** The aspect the ports ask for now (the first one below capacity), or null when full. */
    public @Nullable ResourceKey<IAspect> currentSuction() {
        for (int i = 0; i < FUEL.size(); i++) {
            if (essentia[i] < CAPACITY) {
                return FUEL.get(i);
            }
        }
        return null;
    }

    public int roomFor(ResourceKey<IAspect> aspect) {
        int i = FUEL.indexOf(aspect);
        return i < 0 ? 0 : CAPACITY - essentia[i];
    }

    /** Adds essentia; returns the leftover that did not fit or is not wanted. */
    public int add(Holder<IAspect> aspect, int amount) {
        for (int i = 0; i < FUEL.size(); i++) {
            if (aspect.is(FUEL.get(i))) {
                int added = Math.max(0, Math.min(amount, CAPACITY - essentia[i]));
                essentia[i] += added;
                if (added > 0) {
                    sync();
                }
                return amount - added;
            }
        }
        return amount;
    }

    /** The aspect held most of, for tubes asking what the ports contain. */
    public @Nullable ResourceKey<IAspect> largest() {
        int best = -1;
        for (int i = 0; i < FUEL.size(); i++) {
            if (essentia[i] > 0 && (best < 0 || essentia[i] > essentia[best])) {
                best = i;
            }
        }
        return best < 0 ? null : FUEL.get(best);
    }

    public int amount(@Nullable ResourceKey<IAspect> aspect) {
        int i = aspect == null ? -1 : FUEL.indexOf(aspect);
        return i < 0 ? 0 : essentia[i];
    }

    @Override
    protected boolean canRefine(ItemStack stack) {
        return stack.is(PrimordialContent.PRIMORDIAL_GRAIN.get()) && isEssentiaFull();
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
                ItemStack result = resultFor(server, stack);
                speedyTime--;
                for (int i = 0; i < essentia.length; i++) {
                    essentia[i] -= CAPACITY;
                }
                eject(result.copy());
                Vec3 from = Vec3.atCenterOf(worldPosition);
                Vec3 to = Vec3.atCenterOf(worldPosition.relative(outputSide(), 2));
                int color = Aspects.resolve(server, TTAspects.PRAECANTATIO).value().color();
                for (int i = 0; i < 5; i++) {
                    Effects.spawn(server, new VisSparkleParticleOptions(to.x, to.y, to.z, color), from.x, from.y, from.z);
                }
                popSound();
                stack.shrink(result.getCount());
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

    private static ItemStack resultFor(ServerLevel server, ItemStack stack) {
        if (!stack.is(PrimordialContent.PRIMORDIAL_GRAIN.get())) {
            return stack.copy();
        }
        int r = server.getRandom().nextInt(10);
        ItemStack pearl = new ItemStack(TTItems.PRIMORDIAL_PEARL.get());
        pearl.set(DataComponents.DAMAGE, r == 0 ? 5 : r <= 2 ? 6 : 7);
        return pearl;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putIntArray("essentia", essentia.clone());
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        int[] saved = in.getIntArray("essentia").orElse(new int[0]);
        for (int i = 0; i < essentia.length; i++) {
            essentia[i] = i < saved.length ? saved[i] : 0;
        }
    }
}
