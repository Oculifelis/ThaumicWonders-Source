package com.thaumicwonders.alchemy;

import net.minecraft.world.item.Item;
import com.leclowndu93150.thaumaturge.content.essentia.jar.JarItem;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** The jar's item: "Creative only" plus the original's four tooltip lines. */
public class CreativeEssentiaJarItem extends JarItem {
    public CreativeEssentiaJarItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltipList, TooltipFlag flag) {
        java.util.function.Consumer<Component> tooltip = tooltipList::add;
        super.appendHoverText(stack, context, tooltipList, flag);
        tooltip.accept(Component.translatable("item.creative_only").withStyle(ChatFormatting.DARK_PURPLE));
        for (int i = 1; i <= 4; i++) {
            tooltip.accept(Component.translatable("block.thaumaturgicwonders.creative_essentia_jar.tooltip." + i));
        }
    }
}
