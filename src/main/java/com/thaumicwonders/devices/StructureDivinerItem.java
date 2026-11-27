package com.thaumicwonders.devices;

import net.minecraft.world.InteractionResultHolder;
import com.thaumicwonders.data.DeviceRules.Diviner;
import net.minecraft.resources.ResourceLocation;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/**
 * Structure Diviner: picks one of eight structure kinds on its screen and points at the nearest one in the current
 * dimension. The target is the vanilla lodestone-tracker component (untracked), which the compass-style item model reads,
 * instead of the original's NBT position. Each kind is a structure tag, {@code thaumaturgicwonders:structure_diviner/<kind>}.
 */
public class StructureDivinerItem extends Item {
    public StructureDivinerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            DevicesContent.clientHooks().openStructureDiviner(Diviner.of(player.getItemInHand(hand)));
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    private static @Nullable ItemStack held(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof StructureDivinerItem) {
                return stack;
            }
        }
        return null;
    }

    /** Resolve the selected tag against the current pack after any intervening reload. */
    static void act(ServerPlayer player, Optional<ResourceLocation> target) {
        ItemStack stack = held(player);
        if (stack == null) {
            return;
        }
        if (target.isEmpty()) {
            stack.remove(DataComponents.LODESTONE_TRACKER);
            return;
        }
        Diviner rule = Diviner.of(stack);
        if (!rule.allows(target.get())) return;
        ServerLevel level = (ServerLevel) player.level();
        TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, target.get());
        BlockPos found = level.findNearestMapStructure(tag, player.blockPosition(), rule.radius(), false);
        if (found == null) {
            stack.remove(DataComponents.LODESTONE_TRACKER);
            message(player, "event.structure_diviner.not_found");
            return;
        }
        stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), found)), false));
        double dx = found.getX() - player.getX();
        double dz = found.getZ() - player.getZ();
        double distanceSq = dx * dx + dz * dz;
        String key;
        if (distanceSq < 10.0 * 10.0) {
            key = "event.structure_diviner.found.here";
        } else if (distanceSq < 100.0 * 100.0) {
            key = "event.structure_diviner.found.near";
        } else if (distanceSq < 1000.0 * 1000.0) {
            key = "event.structure_diviner.found.far";
        } else if (distanceSq < 10000.0 * 10000.0) {
            key = "event.structure_diviner.found.very_far";
        } else {
            key = "event.structure_diviner.found.extreme";
        }
        message(player, key);
    }

    private static void message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE), true);
    }
}
