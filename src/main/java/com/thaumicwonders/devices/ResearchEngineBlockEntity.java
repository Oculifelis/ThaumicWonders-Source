package com.thaumicwonders.devices;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.research.IResearchEntry;
import com.leclowndu93150.thaumaturge.api.research.pool.AspectPoolAccess;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.leclowndu93150.thaumaturge.content.research.note.ResearchNoteData;
import com.leclowndu93150.thaumaturge.content.research.table.BlockEntityResearchTable;
import com.leclowndu93150.thaumaturge.content.research.table.BlockResearchTable;
import com.leclowndu93150.thaumaturge.content.research.table.MenuResearchTable;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The fuel tank and the "card" of an Inspiration or Madness Engine.
 *
 * <p>Fuel as in the original: Cognitio (cost 5, capacity 25) or Alienis (cost 10, capacity 50), taken from tubes or
 * jars on every face but the top, one essentia per face every 5 ticks.
 *
 * <p>Research gain (the original's Induced Inspiration / Induced Madness cards, which needed a fuelled engine near the
 * table and paid one cost per activation): whenever a research note on the table below is finished, the engine pays
 * one cost and the player working at that table gains aspect-pool points.
 * <ul>
 * <li>Inspiration: the original added {@code 5 + highest/3} progress to the category with the highest total. Here the
 * player's current focus is the note's research entry: of its note aspects, the one the player holds most of gets
 * {@code 5 + min(held, 100)/3} points (TC6 category totals never passed 100, hence the cap).</li>
 * <li>Madness: the original added 20..25 Eldritch progress; here 20..25 Alienis points, plus 1 normal and 5 temporary
 * warp, as the original.</li>
 * </ul>
 */
public class ResearchEngineBlockEntity extends EssentiaDeviceBlockEntity {
    public enum Kind {
        INSPIRATION(TTAspects.COGNITIO, 5, 25, 0.25F, "card.induced_inspiration.text"),
        MADNESS(TTAspects.ALIENIS, 10, 50, 0.35F, "card.induced_madness.text");

        private final ResourceKey<IAspect> aspect;
        private final int cost;
        private final int capacity;
        private final float saveChance;
        private final String text;

        Kind(ResourceKey<IAspect> aspect, int cost, int capacity, float saveChance, String text) {
            this.aspect = aspect;
            this.cost = cost;
            this.capacity = capacity;
            this.saveChance = saveChance;
            this.text = text;
        }

        public ResourceKey<IAspect> aspect() {
            return aspect;
        }

        public int cost() {
            return cost;
        }

        public int capacity() {
            return capacity;
        }

        /** The table's chance that an aspect placement is free while this engine is fuelled (two halves cap at 0.5). */
        public float saveChance() {
            return saveChance;
        }
    }

    private static final List<Direction> FACES = List.of(Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);

    private int amount;
    /** The note on the table below that was last seen unfinished ({@code entry#index}). */
    private @Nullable String watchedNote;

    public ResearchEngineBlockEntity(BlockPos pos, BlockState state) {
        super(DevicesContent.RESEARCH_ENGINE_BE.get(), pos, state);
    }

    public Kind kind() {
        return getBlockState().getBlock() instanceof ResearchEngineBlock engine ? engine.kind() : Kind.INSPIRATION;
    }

    public boolean isFueled() {
        return amount >= kind().cost();
    }

    private boolean deductCost() {
        if (!isFueled()) {
            return false;
        }
        amount -= kind().cost();
        sync();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResearchEngineBlockEntity be) {
        if (++be.tickCounter % 5 == 0 && be.amount < be.kind().capacity()) {
            be.fill(FACES, true);
        }
        be.watchTable((ServerLevel) level);
    }

    private void watchTable(ServerLevel level) {
        BlockPos below = worldPosition.below();
        BlockState tableState = level.getBlockState(below);
        if (!(tableState.getBlock() instanceof BlockResearchTable)) {
            watchedNote = null;
            return;
        }
        BlockPos main = BlockResearchTable.mainPos(tableState, below);
        if (!(level.getBlockEntity(main) instanceof BlockEntityResearchTable table)) {
            watchedNote = null;
            return;
        }
        ResearchNoteData note = table.noteData();
        if (note == null) {
            watchedNote = null;
            return;
        }
        String key = note.entry() + "#" + note.index();
        if (!note.complete()) {
            watchedNote = key;
        } else if (key.equals(watchedNote)) {
            watchedNote = null;
            ServerPlayer researcher = researcherAt(level, main);
            if (researcher != null && deductCost()) {
                activate(level, researcher, note);
            }
        }
    }

    private static @Nullable ServerPlayer researcherAt(ServerLevel level, BlockPos tablePos) {
        for (ServerPlayer player : level.players()) {
            if (player.containerMenu instanceof MenuResearchTable menu && tablePos.equals(menu.tablePos())) {
                return player;
            }
        }
        return null;
    }

    private void activate(ServerLevel level, ServerPlayer player, ResearchNoteData note) {
        Kind kind = kind();
        if (kind == Kind.MADNESS) {
            int points = Mth.nextInt(level.getRandom(), 20, 25);
            AspectPoolAccess.grant(player, Aspects.resolve(level, TTAspects.ALIENIS), points);
            WarpHelper.addWarp(player, 1, WarpType.NORMAL);
            WarpHelper.addWarp(player, 5, WarpType.TEMPORARY);
            player.sendSystemMessage(Component.translatable(kind.text));
            return;
        }
        Holder<IAspect> focus = null;
        int held = -1;
        Optional<? extends Holder<IResearchEntry>> entry = level.registryAccess().lookupOrThrow(IResearchEntry.REGISTRY_KEY)
                .get(ResourceKey.create(IResearchEntry.REGISTRY_KEY, note.entry()));
        if (entry.isPresent()) {
            for (AspectInstance instance : entry.get().value().noteAspects().entries()) {
                int amountHeld = AspectPoolAccess.amount(player, instance.aspect());
                if (amountHeld > held) {
                    held = amountHeld;
                    focus = instance.aspect();
                }
            }
        }
        if (focus == null) {
            // A note with no note aspects: fall back to the engine's own Cognitio.
            focus = Aspects.resolve(level, TTAspects.COGNITIO);
            held = AspectPoolAccess.amount(player, focus);
        }
        int points = 5 + Math.min(held, 100) / 3;
        AspectPoolAccess.grant(player, focus, points);
        player.sendSystemMessage(Component.translatable(kind.text, points, aspectName(focus)));
    }

    private static Component aspectName(Holder<IAspect> aspect) {
        return aspect.unwrapKey().map(k -> (Component) Component.translatable("aspect." + k.identifier().getNamespace() + "." + k.identifier().getPath()))
                .orElse(Component.literal(aspect.value().tag()));
    }

    // ---- essentia --------------------------------------------------------------------------------------------

    @Override
    protected boolean acceptsFace(Direction face) {
        return face != Direction.UP;
    }

    @Override
    protected @Nullable Holder<IAspect> wantedOn(Direction face) {
        return level == null ? null : Aspects.resolve(level, kind().aspect());
    }

    @Override
    protected int roomFor(Direction face) {
        return kind().capacity() - amount;
    }

    @Override
    protected int store(Holder<IAspect> aspect, int count, Direction face) {
        int added = Math.min(count, kind().capacity() - amount);
        amount += added;
        return count - added;
    }

    @Override
    public @Nullable Holder<IAspect> getEssentiaType(@Nullable Direction side) {
        return wantedOn(Direction.DOWN);
    }

    @Override
    public int getEssentiaAmount(@Nullable Direction side) {
        return amount;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putShort("essentia", (short) amount);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        amount = in.getShortOr("essentia", (short) 0);
    }
}
