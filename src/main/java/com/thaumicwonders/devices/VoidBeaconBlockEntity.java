package com.thaumicwonders.devices;

import com.leclowndu93150.thaumaturge.api.aspect.AspectIndexAccess;
import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.effect.Effects;
import com.leclowndu93150.thaumaturge.content.entity.EntityFluxRift;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * Void Beacon, as the original: holds up to 100 of one essentia (taken through its four sides), needs a clear view of
 * the sky (no block of light opacity 15 above it, bedrock excepted), drains flux rifts within 16 blocks that it can see
 * for progress, and every 200 progress conjures an item whose aspects include the stored essentia, weighted by how
 * much of it the item holds. It costs 20 essentia per item, halved per complete pyramid layer underneath (1 at four
 * layers). The candidate items are the item tag {@code thaumaturgicwonders:void_beacon_conjurable} and the base blocks are
 * the block tag {@code thaumaturgicwonders:void_beacon_base}, so packs can change both.
 */
public class VoidBeaconBlockEntity extends EssentiaDeviceBlockEntity {
    public static final int CAPACITY = 100;
    public static final int PROGRESS_REQUIRED = 200;
    public static final TagKey<Item> CONJURABLE = TagKey.create(Registries.ITEM, com.thaumicwonders.ThaumicWonders.id("void_beacon_conjurable"));
    public static final TagKey<Block> BASE = TagKey.create(Registries.BLOCK, com.thaumicwonders.ThaumicWonders.id("void_beacon_base"));

    /** Per aspect: the conjurable items holding it and how much. Rebuilt after every tag reload. */
    private static @Nullable Map<ResourceKey<IAspect>, List<Weighted>> registry;

    private record Weighted(Item item, int weight) {
    }

    private @Nullable ResourceKey<IAspect> essentiaType;
    private int essentiaAmount;
    private boolean validPlacement;
    private int levels = -1;
    private int progress;
    /** Height of the beam (both sides). */
    private int beamHeight;
    /** Client only: the beam's fade-in. */
    private float beamScale;

    public VoidBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(DevicesContent.VOID_BEACON_BE.get(), pos, state);
    }

    public static void clearRegistry() {
        registry = null;
    }

    public int essentiaAmount() {
        return essentiaAmount;
    }

    public int beamHeight() {
        return validPlacement && getBlockState().getValue(VoidBeaconBlock.ENABLED) ? beamHeight : 0;
    }

    /** Client: the beam's width scale, growing in over 40 ticks. */
    public float beamScale(float partialTicks) {
        return Math.min(1.0F, beamScale + 0.025F * partialTicks);
    }

    public void clearEssentia() {
        essentiaType = null;
        essentiaAmount = 0;
        sync();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VoidBeaconBlockEntity be) {
        be.tickCounter++;
        if (level.isClientSide()) {
            be.beamScale = be.beamHeight() > 0 ? Math.min(1.0F, be.beamScale + 0.025F) : 0.0F;
        }
        if (!level.isClientSide() && be.tickCounter % 5 == 0) {
            be.fill(Direction.Plane.HORIZONTAL, false);
        }
        if (be.tickCounter % 80 == 0 || be.tickCounter == 1) {
            be.updateBeam(level);
            be.updateLevels(level);
        }
        if (level instanceof ServerLevel server && be.tickCounter % 20 == 0) {
            if (be.canMakeProgress()) {
                be.drainRifts(server);
            }
            while (be.canConjureItem(server)) {
                be.eject(server, be.conjure(server));
                be.progress -= PROGRESS_REQUIRED;
                be.essentiaAmount -= be.requiredEssentia();
                if (be.essentiaAmount <= 0) {
                    be.essentiaAmount = 0;
                    be.essentiaType = null;
                }
                be.sync();
            }
        }
    }

    private boolean enabled() {
        return getBlockState().getValue(VoidBeaconBlock.ENABLED);
    }

    private boolean hasEnoughEssentia() {
        int cost = requiredEssentia();
        return essentiaType != null && levels >= 0 && cost > 0 && essentiaAmount >= cost;
    }

    private boolean canMakeProgress() {
        return validPlacement && enabled() && hasEnoughEssentia() && progress < PROGRESS_REQUIRED;
    }

    private boolean canConjureItem(ServerLevel level) {
        return validPlacement && enabled() && hasEnoughEssentia() && progress >= PROGRESS_REQUIRED && canEject(level);
    }

    public int requiredEssentia() {
        List<Integer> costs = getBlockState().typeHolder().getData(com.thaumicwonders.data.DeviceRules.BEACON_COST);
        return costs == null ? 0 : costs.get(Math.clamp(levels, 0, 4));
    }

    private void updateBeam(Level level) {
        validPlacement = true;
        beamHeight = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = worldPosition.getY() + 1; y < level.getMaxY() + 1; y++) {
            pos.set(worldPosition.getX(), y, worldPosition.getZ());
            BlockState state = level.getBlockState(pos);
            if (state.getLightDampening() >= 15 && !state.is(Blocks.BEDROCK)) {
                validPlacement = false;
                beamHeight = 0;
                return;
            }
            beamHeight++;
        }
    }

    private void updateLevels(Level level) {
        levels = 0;
        if (!validPlacement) {
            return;
        }
        for (int dy = 1; dy <= 4 && levelComplete(level, dy); dy++) {
            levels++;
        }
    }

    private boolean levelComplete(Level level, int dy) {
        int y = worldPosition.getY() - dy;
        for (int x = worldPosition.getX() - dy; x <= worldPosition.getX() + dy; x++) {
            for (int z = worldPosition.getZ() - dy; z <= worldPosition.getZ() + dy; z++) {
                if (!level.getBlockState(new BlockPos(x, y, z)).is(BASE)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void drainRifts(ServerLevel level) {
        boolean found = false;
        Vec3 top = new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5);
        for (EntityFluxRift rift : validRifts(level, top)) {
            double drained = Math.sqrt(rift.currentSize());
            progress += (int) drained;
            rift.adjustStability(-(float) (drained / 15.0));
            if (level.getRandom().nextInt(33) == 0) {
                rift.resize(rift.currentSize() - 1);
            }
            if (drained >= 1.0) {
                found = true;
            }
            if (tickCounter % 40 == 0) {
                Effects.voidStream(level, rift.position()).to(top).seed(tickCounter).scale(0.04F).send();
            }
        }
        if (found) {
            sync();
        }
    }

    private List<EntityFluxRift> validRifts(ServerLevel level, Vec3 top) {
        List<EntityFluxRift> result = new ArrayList<>();
        for (EntityFluxRift rift : level.getEntitiesOfClass(EntityFluxRift.class, new AABB(worldPosition).inflate(16.0))) {
            if (!rift.isAlive() || rift.currentSize() <= 1 || rift.distanceToSqr(Vec3.atCenterOf(worldPosition)) > 16.0 * 16.0) {
                continue;
            }
            Vec3 from = top.add(rift.position().subtract(top).normalize());
            HitResult hit = level.clip(new ClipContext(from, rift.position(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, rift));
            if (hit.getType() == HitResult.Type.MISS) {
                result.add(rift);
            }
        }
        return result;
    }

    private ItemStack conjure(ServerLevel level) {
        if (essentiaType == null) {
            return ItemStack.EMPTY;
        }
        List<Weighted> list = registry(level).get(essentiaType);
        if (list == null || list.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int total = 0;
        for (Weighted w : list) {
            total += w.weight();
        }
        int roll = level.getRandom().nextInt(total);
        for (Weighted w : list) {
            roll -= w.weight();
            if (roll < 0) {
                return new ItemStack(w.item());
            }
        }
        return ItemStack.EMPTY;
    }

    private static Map<ResourceKey<IAspect>, List<Weighted>> registry(ServerLevel level) {
        Map<ResourceKey<IAspect>, List<Weighted>> map = registry;
        if (map == null) {
            map = new HashMap<>();
            for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(CONJURABLE)) {
                for (var instance : AspectIndexAccess.of(item.value()).entries()) {
                    var key = instance.aspect().unwrapKey();
                    if (key.isPresent() && instance.amount() > 0) {
                        map.computeIfAbsent(key.get(), k -> new ArrayList<>()).add(new Weighted(item.value(), instance.amount()));
                    }
                }
            }
            registry = map;
        }
        return map;
    }

    private boolean canEject(ServerLevel level) {
        for (Direction face : Direction.Plane.HORIZONTAL) {
            ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, worldPosition.relative(face), face.getOpposite());
            if (handler != null) {
                for (int slot = 0; slot < handler.size(); slot++) {
                    if (handler.getResource(slot).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void eject(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        int left = stack.getCount();
        ItemResource resource = ItemResource.of(stack);
        for (Direction face : Direction.Plane.HORIZONTAL) {
            ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, worldPosition.relative(face), face.getOpposite());
            if (handler == null) {
                continue;
            }
            try (Transaction tx = Transaction.openRoot()) {
                left -= handler.insert(resource, left, tx);
                tx.commit();
            }
            if (left <= 0) {
                return;
            }
        }
        // The original lost an item that found no room; drop it on top of the beacon instead.
        Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, stack.copyWithCount(left));
    }

    // ---- essentia --------------------------------------------------------------------------------------------

    private @Nullable Holder<IAspect> type() {
        return essentiaType == null || level == null ? null : Aspects.resolve(level, essentiaType);
    }

    @Override
    protected boolean acceptsFace(Direction face) {
        return face.getAxis().isHorizontal();
    }

    @Override
    protected @Nullable Holder<IAspect> wantedOn(Direction face) {
        return type();
    }

    @Override
    protected int roomFor(Direction face) {
        return CAPACITY - essentiaAmount;
    }

    @Override
    protected int store(Holder<IAspect> aspect, int count, Direction face) {
        if (essentiaType != null && !aspect.is(essentiaType)) {
            return count;
        }
        int added = Math.min(count, CAPACITY - essentiaAmount);
        if (added > 0) {
            essentiaType = aspect.unwrapKey().orElse(null);
            essentiaAmount += added;
        }
        return count - added;
    }

    @Override
    public @Nullable Holder<IAspect> getEssentiaType(@Nullable Direction side) {
        return type();
    }

    @Override
    public int getEssentiaAmount(@Nullable Direction side) {
        return essentiaAmount;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (essentiaType != null) {
            out.store("essentiaType", ResourceKey.codec(IAspect.REGISTRY_KEY), essentiaType);
            out.putShort("essentiaAmount", (short) essentiaAmount);
        }
        out.putShort("levels", (short) levels);
        out.putShort("progress", (short) progress);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        essentiaType = in.read("essentiaType", ResourceKey.codec(IAspect.REGISTRY_KEY)).orElse(null);
        essentiaAmount = essentiaType == null ? 0 : in.getShortOr("essentiaAmount", (short) 0);
        levels = in.getShortOr("levels", (short) -1);
        progress = in.getShortOr("progress", (short) 0);
    }

    /** Eldritch (Alienis) colour, used by the beam renderer. */
    public int beamColor() {
        return level == null ? 0x805080 : Aspects.resolve(level, TTAspects.ALIENIS).value().color();
    }
}
