package com.thaumicwonders.devices;

import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.api.items.RechargeAccess;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Timewinder: a clock that holds 100 vis (Thaumaturge's rechargeable charge) and spends 25 to skip forward to the next
 * sunrise or to the rise of a chosen moon phase, adding 10 flux at the user and a one-minute cooldown.
 */
public class TimewinderItem extends Item {
    public static final int COST = 25;
    public static final int CAPACITY = 100;
    public static final int COOLDOWN = 1200;

    public TimewinderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!DevicesContent.isSurfaceWorld(level)) {
            if (level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("event.timewinder.offworld").withStyle(ChatFormatting.DARK_PURPLE));
            }
            return InteractionResult.PASS;
        }
        if (RechargeAccess.getCharge(player.getItemInHand(hand)) < COST) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            DevicesContent.clientHooks().openTimewinder();
        }
        return InteractionResult.SUCCESS;
    }

    static @Nullable ItemStack held(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof TimewinderItem) {
                return stack;
            }
        }
        return null;
    }

    /** The server side of a button press: 0..7 the rise of that moon phase, 8 the next sunrise. */
    static void act(ServerPlayer player, int targetPhase) {
        ItemStack stack = held(player);
        ServerLevel level = player.level();
        if (stack == null || targetPhase < 0 || targetPhase > 8 || player.getCooldowns().isOnCooldown(stack)
                || !DevicesContent.isSurfaceWorld(level)) {
            return;
        }
        Optional<Holder<WorldClock>> clock = level.dimensionType().defaultClock();
        if (clock.isEmpty() || !RechargeAccess.consumeCharge(stack, player, COST)) {
            return;
        }
        long now = level.clockManager().getTotalTicks(clock.get());
        long dayStart = now - Math.floorMod(now, 24000L);
        long target;
        if (targetPhase == 8) {
            target = dayStart + 24000L;
        } else {
            int currentPhase = (int) Math.floorMod(now / 24000L, 8L);
            int days;
            if (targetPhase != currentPhase) {
                days = (targetPhase - currentPhase + 8) % 8;
            } else if (Math.floorMod(now, 24000L) <= 13500L) {
                days = 0;
            } else {
                days = 8;
            }
            target = dayStart + days * 24000L + 13500L;
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        level.clockManager().setTotalTicks(clock.get(), target);
        AuraHelper.polluteAura(level, player.blockPosition(), 10.0F, true);
        Component used = Component.translatable("event.timewinder.used").withStyle(ChatFormatting.DARK_PURPLE);
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            p.sendOverlayMessage(used);
        }
    }
}
