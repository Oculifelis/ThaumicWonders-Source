package com.thaumicwonders.devices;

import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.particle.SparkleParticleOptions;
import com.leclowndu93150.thaumaturge.content.particle.VisSparkleParticleOptions;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Ore Diviner's search item (synced to every client for the renderer and the activity sparkles) and, on the
 * client of the player who started the search, the found target that gets pinged every 44 ticks.
 */
public class OreDivinerBlockEntity extends DeviceBlockEntity {
    public static final int SCAN_RANGE = 20;

    private ItemStack searchStack = ItemStack.EMPTY;
    /** Client only: this client's target. */
    private @Nullable BlockPos target;
    private int counter = -1;

    public OreDivinerBlockEntity(BlockPos pos, BlockState state) {
        super(DevicesContent.ORE_DIVINER_BE.get(), pos, state);
    }

    public ItemStack searchStack() {
        return searchStack;
    }

    public boolean active() {
        return !searchStack.isEmpty();
    }

    public void setSearchStack(ItemStack stack) {
        searchStack = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        sync();
    }

    public void setTarget(@Nullable BlockPos newTarget) {
        target = newTarget;
        counter = 0;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // As the original: a search does not survive the diviner being unloaded.
        if (level != null && !level.isClientSide() && !searchStack.isEmpty()) {
            searchStack = ItemStack.EMPTY;
            setChanged();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, OreDivinerBlockEntity be) {
        RandomSource random = level.getRandom();
        if (be.active() && be.target != null && be.counter % 44 == 0) {
            BlockPos t = be.target;
            int color = OreMatch.color(level.getBlockState(t));
            level.addParticle(new SparkleParticleOptions(color, 3.0F, 0, 0.0F, 0.0F, 44, false), t.getX() + 0.5, t.getY() + 0.5, t.getZ() + 0.5,
                    0, 0, 0);
        }
        if (be.active() && be.counter % 5 == 0) {
            int color = Aspects.resolve(level, TTAspects.PRAECANTATIO).value().color();
            level.addParticle(new VisSparkleParticleOptions(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, color),
                    pos.getX() + 0.5 + random.nextInt(3) - random.nextInt(3), pos.getY() + 0.5 + random.nextInt(3),
                    pos.getZ() + 0.5 + random.nextInt(3) - random.nextInt(3), 0, 0, 0);
        }
        be.counter++;
    }

    /** Runs a search on the client that received it: cube shells of radius 1..20, nearest shell first. */
    public static void search(Level level, Player player, BlockPos origin, ItemStack stack) {
        if (!(level.getBlockEntity(origin) instanceof OreDivinerBlockEntity diviner) || !OreMatch.isOre(stack)) {
            return;
        }
        Set<String> wanted = OreMatch.names(stack);
        BlockPos found = null;
        for (int r = 1; r <= SCAN_RANGE && found == null; r++) {
            found = searchShell(level, origin, wanted, r);
        }
        if (found == null) {
            player.sendOverlayMessage(Component.translatable("event.ore_diviner.not_found").withStyle(ChatFormatting.DARK_PURPLE));
        } else {
            BlockPos d = found.subtract(origin);
            Direction dir = Direction.getApproximateNearest(d.getX(), d.getY(), d.getZ());
            player.sendOverlayMessage(Component.translatable("event.ore_diviner.found." + dir.getName()).withStyle(ChatFormatting.DARK_PURPLE));
        }
        diviner.setTarget(found);
    }

    private static @Nullable BlockPos searchShell(Level level, BlockPos origin, Set<String> wanted, int r) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int a = -r; a <= r; a++) {
            for (int b = -r; b <= r; b++) {
                int[][] offsets = {{r, a, b}, {-r, a, b}, {a, r, b}, {a, -r, b}, {a, b, r}, {a, b, -r}};
                for (int[] o : offsets) {
                    pos.setWithOffset(origin, o[0], o[1], o[2]);
                    if (OreMatch.matches(level.getBlockState(pos), wanted)) {
                        return pos.immutable();
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!searchStack.isEmpty()) {
            out.store("searchStack", ItemStack.CODEC, searchStack);
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        searchStack = in.read("searchStack", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }
}
