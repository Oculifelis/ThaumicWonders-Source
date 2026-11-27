package com.thaumicwonders.devices;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import com.thaumicwonders.ThaumicWonders;
import com.thaumicwonders.data.DeviceRules;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The original's ore-dictionary checks, on tags: an ore is anything in a {@code c:ores/<name>} tag, and two ores match
 * when they share such a tag (item tags for the held stack, block tags for blocks in the world).
 */
public final class OreMatch {
    private static final TagKey<Block> IGNORED = TagKey.create(Registries.BLOCK, ThaumicWonders.id("ore_diviner_ignored"));

    private OreMatch() {
    }

    private static Set<String> names(Stream<? extends TagKey<?>> tags) {
        return tags.map(TagKey::location)
                .filter(id -> id.getNamespace().equals("c") && id.getPath().startsWith("ores/"))
                .map(id -> id.getPath().substring(5))
                .collect(Collectors.toSet());
    }

    public static Set<String> names(ItemStack stack) {
        return stack.isEmpty() ? Set.of() : names(stack.tags());
    }

    public static Set<String> names(BlockState state) {
        return state.isAir() || state.is(IGNORED) ? Set.of() : names(state.tags());
    }

    public static boolean isOre(ItemStack stack) {
        return !names(stack).isEmpty();
    }

    public static boolean matches(BlockState state, Set<String> wanted) {
        if (wanted.isEmpty()) {
            return false;
        }
        for (String name : names(state)) {
            if (wanted.contains(name)) {
                return true;
            }
        }
        return false;
    }

    /** The original's ore colours, matched by keyword against the tag name. */
    public static int color(BlockState state) {
        var palette = DevicesContent.ORE_DIVINER.get().builtInRegistryHolder().getData(DeviceRules.ORE_PALETTE);
        if (palette == null) return 0xC0C0C0;
        for (String name : names(state)) {
            String n = name.toLowerCase(Locale.ROOT);
            for (var entry : palette.colors()) {
                if (n.contains(entry.keyword().toLowerCase(Locale.ROOT))) {
                    return entry.rgb();
                }
            }
        }
        return palette.fallback();
    }
}
