package com.thaumicwonders.alchemy;

import com.leclowndu93150.thaumaturge.api.capability.IPlayerKnowledge;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeAccess;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeType;
import com.leclowndu93150.thaumaturge.api.research.IResearchCategory;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Lethe Water: drinking it (32 ticks) forgets one unspent theory. A research category is picked at random, weighted
 * by the drinker's theory knowledge in it, and one whole theory point is removed there; the drinker then gains 25
 * experience. With no theory knowledge it does nothing. Completed research and observations are never touched, as
 * the original's research text says. 20 tick cooldown; the bottle comes back.
 */
public class LetheWaterItem extends Item {
    public LetheWaterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return stack;
        }
        if (player instanceof ServerPlayer sp) {
            IPlayerKnowledge knowledge = KnowledgeAccess.of(sp);
            ResourceKey<IResearchCategory> category = pick(sp, knowledge);
            if (category == null) {
                sp.sendOverlayMessage(Component.translatable("event.lethe_water.not_found").withStyle(ChatFormatting.DARK_PURPLE));
            } else if (knowledge.addKnowledge(KnowledgeType.THEORY, category, -KnowledgeType.THEORY.progression())) {
                knowledge.sync(sp);
                sp.giveExperiencePoints(25);
                sp.sendOverlayMessage(Component.translatable("event.lethe_water.forgot").withStyle(ChatFormatting.DARK_PURPLE));
            }
            if (level.getRandom().nextBoolean()) {
                level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            sp.getCooldowns().addCooldown(stack, 20);
        }
        if (player.getAbilities().instabuild) {
            return stack;
        }
        stack.shrink(1);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (stack.isEmpty()) {
            return bottle;
        }
        if (!player.getInventory().add(bottle)) {
            player.drop(bottle, false);
        }
        return stack;
    }

    private static ResourceKey<IResearchCategory> pick(ServerPlayer player, IPlayerKnowledge knowledge) {
        List<ResourceKey<IResearchCategory>> keys = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int total = 0;
        for (ResourceKey<IResearchCategory> key : player.level().registryAccess().lookupOrThrow(IResearchCategory.REGISTRY_KEY).listElementIds().toList()) {
            int weight = knowledge.knowledge(KnowledgeType.THEORY, key);
            if (weight > 0) {
                keys.add(key);
                weights.add(weight);
                total += weight;
            }
        }
        if (total <= 0) {
            return null;
        }
        int roll = player.getRandom().nextInt(total);
        for (int i = 0; i < keys.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) {
                return keys.get(i);
            }
        }
        return null;
    }
}
