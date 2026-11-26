package com.thaumicwonders.primordial;

import java.util.function.Predicate;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Catalyzation Chamber's and the Primordial Accelerator's container (the original's two containers are the same
 * apart from what the socket takes): one socket at (80, 29) holding a single item, the player inventory at (8, 84)
 * and the hotbar at (8, 142). Shift-click moves the socket's item into the inventory, and anything else into the socket.
 */
public class SocketMenu extends AbstractContainerMenu {
    private final Container socket;

    /** Client side. */
    public static SocketMenu catalyzationClient(int id, Inventory inventory) {
        return new SocketMenu(PrimordialContent.CATALYZATION_MENU.get(), id, inventory, new Client(PrimordialContent::isCatalystStone));
    }

    public static SocketMenu acceleratorClient(int id, Inventory inventory) {
        return new SocketMenu(PrimordialContent.ACCELERATOR_MENU.get(), id, inventory, new Client(PrimordialContent::isUsablePearl));
    }

    static SocketMenu catalyzation(int id, Inventory inventory, Container socket) {
        return new SocketMenu(PrimordialContent.CATALYZATION_MENU.get(), id, inventory, socket);
    }

    static SocketMenu accelerator(int id, Inventory inventory, Container socket) {
        return new SocketMenu(PrimordialContent.ACCELERATOR_MENU.get(), id, inventory, socket);
    }

    private SocketMenu(MenuType<?> type, int id, Inventory inventory, Container socket) {
        super(type, id);
        this.socket = socket;
        addSlot(new Socket(socket, 80, 29));
        addStandardInventorySlots(inventory, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return socket.stillValid(player);
    }

    private static final class Socket extends Slot {
        Socket(Container container, int x, int y) {
            super(container, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(0, stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /** The client copy of a socket. */
    private static final class Client extends SimpleContainer {
        private final Predicate<ItemStack> valid;

        Client(Predicate<ItemStack> valid) {
            super(1);
            this.valid = valid;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return valid.test(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
