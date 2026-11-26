package com.thaumicwonders.primordial;

import com.thaumicwonders.devices.DeviceBlockEntity;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The shared part of the Catalyzation and Primordial Accretion Chambers (the original's TileTWInventory(32) plus
 * the TC smelter pattern they copy): 32 slots filled only from above, by items thrown into the quicksilver, and
 * results thrown out through the opening (the face opposite the block's FACING).
 */
public abstract class ChamberBlockEntity extends DeviceBlockEntity implements WorldlyContainer {
    public static final int SIZE = 32;
    private static final int[] ALL = IntStream.range(0, SIZE).toArray();
    private static final int[] NONE = new int[0];

    protected final NonNullList<ItemStack> stacks = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    protected int refineTime;
    protected int maxRefineTime;
    protected int speedyTime;

    protected ChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Whether this item can be refined right now. */
    protected abstract boolean canRefine(ItemStack stack);

    /** The opening: results come out here. */
    protected Direction outputSide() {
        return getBlockState().getValue(ChamberBlock.FACING).getOpposite();
    }

    /** An item entity landed in the chamber: keep what fits if it can be refined, throw it back out otherwise. */
    public ItemStack addItemsToInventory(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return stack;
        }
        if (!canRefine(stack)) {
            eject(stack.copy());
            return ItemStack.EMPTY;
        }
        ItemStack rest = insert(stack.copy());
        setChanged();
        return rest;
    }

    private ItemStack insert(ItemStack stack) {
        for (int i = 0; i < SIZE && !stack.isEmpty(); i++) {
            ItemStack in = stacks.get(i);
            if (!in.isEmpty() && ItemStack.isSameItemSameComponents(in, stack)) {
                int move = Math.min(stack.getCount(), in.getMaxStackSize() - in.getCount());
                if (move > 0) {
                    in.grow(move);
                    stack.shrink(move);
                }
            }
        }
        for (int i = 0; i < SIZE && !stack.isEmpty(); i++) {
            if (stacks.get(i).isEmpty()) {
                stacks.set(i, stack.copy());
                stack.setCount(0);
            }
        }
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    /** The original's InventoryUtils.ejectStackAt: into an inventory beyond the opening, else thrown out of it. */
    protected void eject(ItemStack stack) {
        if (!(level instanceof ServerLevel server) || stack.isEmpty()) {
            return;
        }
        Direction side = outputSide();
        int left = stack.getCount();
        ResourceHandler<ItemResource> handler = server.getCapability(Capabilities.Item.BLOCK, worldPosition.relative(side), side.getOpposite());
        if (handler != null) {
            try (Transaction tx = Transaction.openRoot()) {
                left -= handler.insert(ItemResource.of(stack), left, tx);
                tx.commit();
            }
        }
        if (left <= 0) {
            return;
        }
        double x = worldPosition.getX() + 0.5 + side.getStepX();
        double y = worldPosition.getY() + 0.1;
        double z = worldPosition.getZ() + 0.5 + side.getStepZ();
        ItemEntity item = new ItemEntity(server, x, y, z, stack.copyWithCount(left), side.getStepX() * 0.3, 0.0, side.getStepZ() * 0.3);
        item.setDefaultPickUpDelay();
        server.addFreshEntity(item);
    }

    /** Lava pop for every finished operation (the original's block event 4; its sparkles are sent by the caller). */
    protected void popSound() {
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);
        }
    }

    /** Throws everything inside out on the ground (the chamber itself drops nothing). */
    public void spill() {
        if (level == null) {
            return;
        }
        for (int i = 0; i < SIZE; i++) {
            if (!stacks.get(i).isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stacks.get(i));
                stacks.set(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) {
            spill();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, stacks);
        out.putShort("RefineTime", (short) refineTime);
        out.putShort("SpeedyTime", (short) speedyTime);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        for (int i = 0; i < SIZE; i++) {
            stacks.set(i, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(in, stacks);
        refineTime = in.getShortOr("RefineTime", (short) 0);
        speedyTime = in.getShortOr("SpeedyTime", (short) 0);
    }

    // ---- container (hoppers on top, as the original's sided inventory) ----------------------------------------

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? ALL : NONE;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.UP && canRefine(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return stacks.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return stacks.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack out = ContainerHelper.removeItem(stacks, slot, amount);
        setChanged();
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(stacks, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        stacks.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SIZE; i++) {
            stacks.set(i, ItemStack.EMPTY);
        }
    }

    protected static boolean isServer(Level level) {
        return level instanceof ServerLevel;
    }
}
