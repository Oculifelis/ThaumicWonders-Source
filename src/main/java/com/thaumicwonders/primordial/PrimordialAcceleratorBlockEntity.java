package com.thaumicwonders.primordial;

import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.thaumicwonders.devices.DeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Primordial Accelerator, as the original's TilePrimordialAccelerator. While it has a redstone signal and holds a
 * primordial pearl with at least two uses left (damage below 7), it consumes the pearl and fires it along its facing:
 * up to 10 tunnels facing the same way, then a terminus facing the same way, which throws out primordial grains, one
 * set per use the pearl had left: 1 grain, a second with chance (10 + tunnels)/20, a third with chance tunnels/20.
 * A wrong-facing part, an 11th tunnel or any other block in the way makes a terrain-damaging explosion of strength 4;
 * an empty space ends the shot harmlessly, and the pearl is lost. Hoppers can put the pearl in from any side.
 */
public class PrimordialAcceleratorBlockEntity extends DeviceBlockEntity implements WorldlyContainer, MenuProvider {
    public static final int MAX_TUNNELS = 10;
    private static final int[] SLOTS = {0};

    private ItemStack pearl = ItemStack.EMPTY;

    final Container socket = new SocketContainer(() -> pearl, s -> {
        pearl = s;
        setChanged();
    }, PrimordialContent::isUsablePearl, this);

    public PrimordialAcceleratorBlockEntity(BlockPos pos, BlockState state) {
        super(PrimordialContent.ACCELERATOR_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PrimordialAcceleratorBlockEntity be) {
        if (!(level instanceof ServerLevel server) || !state.is(PrimordialContent.PRIMORDIAL_ACCELERATOR.get())) {
            return;
        }
        boolean powered = !state.getValue(PrimordialAcceleratorBlock.ENABLED);
        if (powered && PrimordialContent.isUsablePearl(be.pearl)) {
            ItemStack shot = be.pearl;
            be.pearl = ItemStack.EMPTY;
            be.setChanged();
            RandomSource r = server.getRandom();
            server.playSound(null, pos, TTSounds.ZAP.get(), SoundSource.BLOCKS, 1.0F, (r.nextFloat() - r.nextFloat()) * 0.2F + 1.0F);
            server.playSound(null, pos, TTSounds.WIND.get(), SoundSource.BLOCKS, 1.0F, (r.nextFloat() - r.nextFloat()) * 0.2F + 1.0F);
            be.fire(server, state.getValue(PrimordialAcceleratorBlock.FACING), shot);
        }
    }

    private void fire(ServerLevel level, Direction facing, ItemStack shot) {
        int tunnels = 0;
        BlockPos cur = worldPosition;
        for (int step = 0; step < 20; step++) {
            cur = cur.relative(facing);
            BlockState state = level.getBlockState(cur);
            if (state.is(PrimordialContent.ACCELERATOR_TERMINUS.get())) {
                if (state.getValue(AcceleratorPartBlock.FACING) == facing) {
                    completeReaction(level, cur, tunnels, shot);
                } else {
                    explode(level, cur);
                }
                return;
            }
            if (state.is(PrimordialContent.ACCELERATOR_TUNNEL.get())) {
                if (state.getValue(AcceleratorPartBlock.FACING) == facing && tunnels < MAX_TUNNELS) {
                    tunnels++;
                    continue;
                }
                explode(level, cur);
                return;
            }
            if (state.isAir()) {
                return;
            }
            explode(level, cur);
            return;
        }
        explode(level, cur);
    }

    private static void explode(ServerLevel level, BlockPos pos) {
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4.0F, Level.ExplosionInteraction.BLOCK);
    }

    private static void completeReaction(ServerLevel level, BlockPos terminus, int tunnels, ItemStack shot) {
        int uses = Mth.clamp(PrimordialContent.PEARL_USES - shot.getDamageValue(), 0, PrimordialContent.PEARL_USES);
        RandomSource r = level.getRandom();
        for (int i = 0; i < uses; i++) {
            ejectGrain(level, terminus);
            if (r.nextInt(20) < 10 + tunnels) {
                ejectGrain(level, terminus);
            }
            if (r.nextInt(20) < tunnels) {
                ejectGrain(level, terminus);
            }
        }
    }

    private static void ejectGrain(ServerLevel level, BlockPos terminus) {
        RandomSource r = level.getRandom();
        ItemEntity item = new ItemEntity(level, terminus.getX() + 0.5, terminus.getY() + 1.0, terminus.getZ() + 0.5,
                new ItemStack(PrimordialContent.PRIMORDIAL_GRAIN.get()), r.nextGaussian() * 0.1, 0.3, r.nextGaussian() * 0.1);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide() && !pearl.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, pearl);
            pearl = ItemStack.EMPTY;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!pearl.isEmpty()) {
            out.store("Pearl", ItemStack.CODEC, pearl);
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        pearl = in.read("Pearl", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.thaumaturgicwonders.primordial_accelerator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return SocketMenu.accelerator(id, inventory, socket);
    }

    // ---- container: the pearl socket, open on every side as the original's sided inventory -----------------------

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return pearl.isEmpty() && PrimordialContent.isUsablePearl(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return pearl.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return socket.getItem(0);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return socket.removeItem(0, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return socket.removeItemNoUpdate(0);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        socket.setItem(0, stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return PrimordialContent.isUsablePearl(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        pearl = ItemStack.EMPTY;
    }
}
