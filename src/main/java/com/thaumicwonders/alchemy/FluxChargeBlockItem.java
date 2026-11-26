package com.thaumicwonders.alchemy;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Block item of the Flux Capacitor and Flux Distiller. The stored charge rides in the vanilla block_state component
 * (copied by the loot table, restored on placement); the tooltip shows it as "Flux: n" in yellow.
 */
public class FluxChargeBlockItem extends BlockItem {
    private final IntegerProperty charge;

    public FluxChargeBlockItem(Block block, IntegerProperty charge, Properties properties) {
        super(block, properties);
        this.charge = charge;
    }

    public int charge(ItemStack stack) {
        BlockItemStateProperties props = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        Integer value = props.get(charge);
        return value == null ? 0 : value;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable(getDescriptionId() + ".tooltip.flux", charge(stack)).withStyle(ChatFormatting.YELLOW));
    }
}
