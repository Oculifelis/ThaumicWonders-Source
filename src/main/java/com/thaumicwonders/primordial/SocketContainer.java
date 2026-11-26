package com.thaumicwonders.primordial;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A one-slot view of a single stack a block entity keeps (the catalyst socket, the accelerator's pearl socket).
 * Holds one item, and only items the predicate allows.
 */
final class SocketContainer implements Container {
    private final Supplier<ItemStack> getter;
    private final Consumer<ItemStack> setter;
    private final Predicate<ItemStack> valid;
    private final BlockEntity owner;

    SocketContainer(Supplier<ItemStack> getter, Consumer<ItemStack> setter, Predicate<ItemStack> valid, BlockEntity owner) {
        this.getter = getter;
        this.setter = setter;
        this.valid = valid;
        this.owner = owner;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return getter.get().isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return getter.get();
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack in = getter.get();
        if (in.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack out = in.split(amount);
        setter.accept(in.isEmpty() ? ItemStack.EMPTY : in);
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack in = getter.get();
        setter.accept(ItemStack.EMPTY);
        return in;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!stack.isEmpty()) {
            stack.limitSize(1);
        }
        setter.accept(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return valid.test(stack);
    }

    @Override
    public void setChanged() {
        owner.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(owner, player);
    }

    @Override
    public void clearContent() {
        setter.accept(ItemStack.EMPTY);
    }
}
