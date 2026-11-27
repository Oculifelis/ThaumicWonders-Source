package com.thaumicwonders.primordial;

import com.thaumicwonders.data.DeviceRules;
import com.thaumicwonders.data.DeviceRules.Catalyst;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.content.effect.Effects;
import com.leclowndu93150.thaumaturge.content.essentia.BellowsHelper;
import com.leclowndu93150.thaumaturge.content.particle.VisSparkleParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Catalyzation Chamber, as the original's TileCatalyzationChamber. Items that the equipped catalyst stone can refine
 * are taken in; every operation (140 ticks, 80 while the vis boost lasts, less 20/38/54/68 for one to four bellows
 * two blocks away, at least 10) turns one of them into the stone's output, wears the stone by one use and, one time in
 * the stone's flux chance, puts one flux into the aura beyond the opening. The boost: whenever it has run out the
 * chamber drains up to 20 vis from the aura, and each operation spends one. Items that cannot be refined are thrown out.
 */
public class CatalyzationChamberBlockEntity extends ChamberBlockEntity implements MenuProvider {
    private static final Direction[] BELLOWS_SIDES = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private ItemStack equippedStone = ItemStack.EMPTY;

    /** The stone socket the screen shows (the original's SlotCatalyzationStone, backed by the equipped stone). */
    final Container socket = new SocketContainer(() -> equippedStone, s -> {
        equippedStone = s;
        sync();
    }, PrimordialContent::isCatalystStone, this);

    public CatalyzationChamberBlockEntity(BlockPos pos, BlockState state) {
        super(PrimordialContent.CATALYZATION_CHAMBER_BE.get(), pos, state);
    }

    public ItemStack equippedStone() {
        return equippedStone;
    }

    private @Nullable Catalyst stone() {
        return equippedStone.isEmpty() ? null : equippedStone.typeHolder().getData(DeviceRules.CATALYST);
    }

    @Override
    protected boolean canRefine(ItemStack stack) {
        return level instanceof ServerLevel server && stone() != null && !CatalyzationRecipe.refine(server, equippedStone, stack).isEmpty();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CatalyzationChamberBlockEntity be) {
        if (level instanceof ServerLevel server) {
            be.serverTick(server);
        }
    }

    private void serverTick(ServerLevel server) {
        boolean refined = false;
        if (refineTime > 0) {
            refineTime--;
            refined = true;
        }
        if (maxRefineTime <= 0) {
            maxRefineTime = calcRefineTime(server);
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
                Catalyst stone = stone();
                ItemStack result = stone == null ? ItemStack.EMPTY : CatalyzationRecipe.refine(server, equippedStone, stack);
                if (result.isEmpty()) {
                    // As the original: with no stone (or a rejected item) the whole stack comes out unchanged.
                    result = stack.copy();
                } else {
                    equippedStone.hurtAndBreak(1, server, null, item -> {
                    });
                    if (equippedStone.isEmpty()) {
                        equippedStone = ItemStack.EMPTY;
                    }
                }
                if (speedyTime > 0) {
                    speedyTime--;
                }
                eject(result.copy());
                playEffects(server, stone);
                if (stone != null && server.getRandom().nextInt(stone.fluxChance()) == 0) {
                    AuraHelper.polluteAura(server, worldPosition.relative(outputSide()), 1.0F, true);
                }
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
                    maxRefineTime = refineTime = calcRefineTime(server);
                    break;
                }
                eject(stack);
                stacks.set(slot, ItemStack.EMPTY);
                setChanged();
            }
        }
    }

    private int calcRefineTime(ServerLevel server) {
        int n = Math.min(4, BellowsHelper.countBellows(server, worldPosition, BELLOWS_SIDES, 2));
        int bonus = n > 0 ? (20 - (n - 1)) * n : 0;
        return Math.max(10, (speedyTime > 0 ? 80 : 140) - bonus);
    }

    /** Five sparkles in the stone's colour from the chamber out through the opening, and the lava pop. */
    private void playEffects(ServerLevel server, @Nullable Catalyst stone) {
        if (stone != null) {
            Vec3 from = Vec3.atCenterOf(worldPosition);
            Vec3 to = Vec3.atCenterOf(worldPosition.relative(outputSide(), 2));
            int color = stone.sparkle().value().color();
            for (int i = 0; i < 5; i++) {
                Effects.spawn(server, new VisSparkleParticleOptions(to.x, to.y, to.z, color), from.x, from.y, from.z);
            }
        }
        popSound();
    }

    @Override
    public void spill() {
        super.spill();
        if (level != null && !equippedStone.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, equippedStone);
            equippedStone = ItemStack.EMPTY;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!equippedStone.isEmpty()) {
            out.store("EquippedStone", ItemStack.CODEC, equippedStone);
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        equippedStone = in.read("EquippedStone", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.thaumaturgicwonders.catalyzation_chamber");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return SocketMenu.catalyzation(id, inventory, socket);
    }
}
