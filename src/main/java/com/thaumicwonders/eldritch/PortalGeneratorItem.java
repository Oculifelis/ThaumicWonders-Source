package com.thaumicwonders.eldritch;

import net.minecraft.world.item.Item;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Portal Generator item. Used on a Portal Anchor it splits one generator off the stack, linked to that anchor (its
 * position and dimension, in the {@code thaumaturgicwonders:portal_link} component), and hands it over. The tooltip says
 * where a linked generator leads.
 */
public class PortalGeneratorItem extends BlockItem {
    public PortalGeneratorItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(EldritchContent.PORTAL_ANCHOR.get())) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (!level.isClientSide() && player != null) {
            ItemStack linked = stack.copyWithCount(1);
            linked.set(EldritchContent.PORTAL_LINK.get(), GlobalPos.of(level.dimension(), pos.immutable()));
            level.playSound(null, pos, TTSounds.JAR.get(), SoundSource.BLOCKS, 1.0F, 2.0F);
            stack.shrink(1);
            if (!player.getInventory().add(linked)) {
                player.drop(linked, false);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltipList, TooltipFlag flag) {
        java.util.function.Consumer<Component> tooltip = tooltipList::add;
        super.appendHoverText(stack, context, tooltipList, flag);
        GlobalPos link = stack.get(EldritchContent.PORTAL_LINK.get());
        if (link != null) {
            BlockPos p = link.pos();
            tooltip.accept(Component.translatable(getDescriptionId() + ".tooltip.linkedto", p.getX(), p.getY(), p.getZ(),
                    link.dimension().location().toString()).withStyle(ChatFormatting.GRAY));
        }
    }
}
