package com.thaumicwonders.devices;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import java.util.List;
import com.thaumicwonders.data.DeviceLoot;
import com.thaumicwonders.data.EssentiaTanks.Tank;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Meaty Orb: 250 each of Victus (relative west), Aqua (east) and Alienis (back) start fifteen seconds of raw meat
 * falling from the top of the world within 32 blocks. Each use also adds 10 flux to the aura.
 */
public class MeatyOrbBlockEntity extends OrbBlockEntity {
    public static final int DURATION = 300;

    private int activeCounter;

    public MeatyOrbBlockEntity(BlockPos pos, BlockState state) {
        super(DevicesContent.MEATY_ORB_BE.get(), pos, state, List.of(TTAspects.VICTUS, TTAspects.AQUA, TTAspects.ALIENIS),
                List.of("lifeEssentia", "waterEssentia", "eldritchEssentia"));
    }

    @Override
    public void act(ServerPlayer player, int choice) {
        ServerLevel level = (ServerLevel) this.level;
        Tank left = tank(0);
        Tank right = tank(1);
        Tank back = tank(2);
        if (back == null || !stored.funded(left) || !stored.funded(right) || !stored.funded(back)) {
            status(player, "event.meaty_orb.unfueled");
            return;
        }
        stored.spend(left);
        stored.spend(right);
        stored.spend(back);
        sync();
        AuraHelper.polluteAura(level, worldPosition, 10, true);
        activeCounter = DURATION;
        Component used = Component.translatable("event.meaty_orb.used").withStyle(ChatFormatting.DARK_PURPLE);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(worldPosition.getCenter()) < 32 * 32) {
                p.sendOverlayMessage(used);
            }
        }
        MeteorbBlockEntity.skyBolt(level, worldPosition, back.aspect().value().color());
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
        for (ItemStack drop : DeviceLoot.roll(level, worldPosition, DeviceLoot.MEATY_ORB)) {
            level.addFreshEntity(new ItemEntity(level, x, level.getMaxY(), z, drop));
        }
    }
}
