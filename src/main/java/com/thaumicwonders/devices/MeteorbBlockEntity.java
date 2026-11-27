package com.thaumicwonders.devices;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.effect.Effects;
import com.thaumicwonders.data.EssentiaTanks.Tank;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Meteorb: 50 Aer clears the sky, 50 Aqua brings rain, 50 Potentia a thunderstorm, each for 300..899 seconds.
 * Tanks of 50 (relative west Aer, east Aqua, back Potentia).
 */
public class MeteorbBlockEntity extends OrbBlockEntity {
    public MeteorbBlockEntity(BlockPos pos, BlockState state) {
        super(DevicesContent.METEORB_BE.get(), pos, state, List.of(TTAspects.AER, TTAspects.AQUA, TTAspects.POTENTIA),
                List.of("airEssentia", "waterEssentia", "energyEssentia"));
    }

    @Override
    public void act(ServerPlayer player, int target) {
        if (target < 0 || target > 2) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level;
        Tank fuel = tank(target);
        if (fuel == null || !stored.funded(fuel)) {
            status(player, "event.meteorb.unfueled");
            return;
        }
        stored.spend(fuel);
        sync();
        int duration = (300 + level.getRandom().nextInt(600)) * 20;
        switch (target) {
            case 0 -> ((net.minecraft.server.level.ServerLevel) level).setWeatherParameters(duration, 0, false, false);
            case 1 -> ((net.minecraft.server.level.ServerLevel) level).setWeatherParameters(0, duration, true, false);
            default -> ((net.minecraft.server.level.ServerLevel) level).setWeatherParameters(0, duration, true, true);
        }
        Component used = Component.translatable("event.meteorb.used").withStyle(ChatFormatting.DARK_PURPLE);
        for (ServerPlayer p : level.players()) {
            p.displayClientMessage(used, true);
        }
        skyBolt(level, worldPosition, fuel.aspect().value().color());
    }

    /** The original's PacketMeteorbFx: an arc bolt from the orb straight up to the top of the world. */
    static void skyBolt(ServerLevel level, BlockPos pos, int color) {
        Vec3 from = Vec3.atCenterOf(pos);
        Vec3 to = new Vec3(pos.getX() + 0.5, level.getMaxBuildHeight() - 1 + 0.5, pos.getZ() + 0.5);
        Effects.arcBolt(level, from).to(to).color(color).width(0.6F).send();
    }
}
