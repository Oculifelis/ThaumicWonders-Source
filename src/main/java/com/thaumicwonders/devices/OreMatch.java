package com.thaumicwonders.devices;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The original's ore-dictionary checks, on tags: an ore is anything in a {@code c:ores/<name>} tag, and two ores match
 * when they share such a tag (item tags for the held stack, block tags for blocks in the world).
 */
public final class OreMatch {
    private static final String[][] COLORS = {
            {"iron", "D8AF93"}, {"coal", "101010"}, {"redstone", "FF0000"}, {"gold", "FCEE4B"}, {"lapis", "1445BC"},
            {"diamond", "5DECF5"}, {"emerald", "17DD62"}, {"quartz", "E5DED5"}, {"silver", "DAD9FD"}, {"lead", "6B6B6B"},
            {"tin", "EFEFFB"}, {"copper", "FD9C55"}, {"amber", "FDB325"}, {"cinnabar", "9B0508"}};

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
        return state.isAir() || state.is(Blocks.BEDROCK) ? Set.of() : names(state.tags());
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
        for (String name : names(state)) {
            String n = name.toLowerCase(Locale.ROOT);
            for (String[] entry : COLORS) {
                if (n.contains(entry[0])) {
                    return Integer.parseInt(entry[1], 16);
                }
            }
        }
        return 0xC0C0C0;
    }
}
