package com.thaumicwonders.gear;

import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * The Cleansing Charm (Curios charm slot). While its wearer has ordinary warp and it has energy, it works one hour
 * (72000 ticks) to remove one point of it, releasing 0.1 flux every 3 seconds. 200 vis run it for an hour.
 */
public class CleansingCharmItem extends Item implements ICurioItem {
    public static final int VIS_CAPACITY = 200;
    public static final int MAX_PROGRESS = 20 * 60 * 60;
    public static final int ENERGY_PER_VIS = (MAX_PROGRESS / VIS_CAPACITY) - 1;

    public CleansingCharmItem(Properties props) {
        super(props);
    }

    public static int progress(ItemStack stack) {
        return stack.getOrDefault(GearContent.PROGRESS.get(), 0);
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        if (!(context.entity() instanceof ServerPlayer player)) {
            return;
        }
        if (WarpHelper.getWarp(player).get(WarpType.NORMAL) <= 0) {
            return;
        }
        if (NightVisionGogglesItem.hasEnergy(stack)) {
            int progress = progress(stack) + 1;
            if (progress % 60 == 0) {
                AuraHelper.polluteAura(player.level(), player.blockPosition(), 0.1F, true);
            }
            if (progress >= MAX_PROGRESS) {
                WarpHelper.addWarp(player, -1, WarpType.NORMAL);
                progress = 0;
            }
            stack.set(GearContent.PROGRESS.get(), progress);
        }
        NightVisionGogglesItem.consumeEnergy(stack, player, ENERGY_PER_VIS);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltipList, TooltipFlag flag) {
        java.util.function.Consumer<Component> tooltip = tooltipList::add;
        int percent = (int) (100.0 * progress(stack) / MAX_PROGRESS);
        tooltip.accept(Component.translatable("item.thaumaturgicwonders.cleansing_charm.tooltip.progress", percent));
        super.appendHoverText(stack, context, tooltipList, flag);
    }
}
