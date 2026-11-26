package com.thaumicwonders.devices;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Meaty Orb: 250 each of Victus (relative west), Aqua (east) and Alienis (back) start fifteen seconds of raw meat
 * falling from the top of the world within 32 blocks. Each use also adds 10 flux to the aura.
 */
public class MeatyOrbBlockEntity extends OrbBlockEntity {
    public static final int CAPACITY = 250;
    public static final int DURATION = 300;

    private int activeCounter;

    public MeatyOrbBlockEntity(BlockPos pos, BlockState state) {
        super(DevicesContent.MEATY_ORB_BE.get(), pos, state, List.of(TTAspects.VICTUS, TTAspects.AQUA, TTAspects.ALIENIS),
                List.of("lifeEssentia", "waterEssentia", "eldritchEssentia"), CAPACITY);
    }

    @Override
    public void act(ServerPlayer player, int choice) {
        ServerLevel level = (ServerLevel) this.level;
        if (!has(0, CAPACITY) || !has(1, CAPACITY) || !has(2, CAPACITY)) {
            status(player, "event.meaty_orb.unfueled");
            return;
        }
        for (int tank = 0; tank < 3; tank++) {
            amounts[tank] -= CAPACITY;
        }
        sync();
        AuraHelper.polluteAura(level, worldPosition, 10, true);
        activeCounter = DURATION;
        Component used = Component.translatable("event.meaty_orb.used").withStyle(ChatFormatting.DARK_PURPLE);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(worldPosition.getCenter()) < 32 * 32) {
                p.sendOverlayMessage(used);
            }
        }
        MeteorbBlockEntity.skyBolt(level, worldPosition, aspect(2).value().color());
    }

    @Override
    protected void activeTick() {
        if (activeCounter <= 0) {
            return;
        }
        activeCounter--;
        ServerLevel level = (ServerLevel) this.level;
        RandomSource random = level.getRandom();
        double x = worldPosition.getX() + 0.5 + 32.0 * (random.nextDouble() - random.nextDouble());
        double z = worldPosition.getZ() + 0.5 + 32.0 * (random.nextDouble() - random.nextDouble());
        ItemEntity meat = new ItemEntity(level, x, level.getMaxY(), z, new ItemStack(randomMeat(random)));
        level.addFreshEntity(meat);
    }

    /** The original's weighted meat table. */
    private static net.minecraft.world.item.Item randomMeat(RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 30) {
            return Items.BEEF;
        } else if (roll < 55) {
            return Items.PORKCHOP;
        } else if (roll < 75) {
            return Items.CHICKEN;
        } else if (roll < 90) {
            return Items.MUTTON;
        }
        return Items.RABBIT;
    }
}
