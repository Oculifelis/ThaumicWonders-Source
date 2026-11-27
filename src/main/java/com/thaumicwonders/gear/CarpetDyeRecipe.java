package com.thaumicwonders.gear;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * thaumaturgicwonders:flying_carpet_dyes. Exactly one Flying Carpet and exactly one dye, nothing else: the carpet takes the
 * dye's colour (vis, energy and everything else kept).
 */
public class CarpetDyeRecipe extends CustomRecipe {
    public static final CarpetDyeRecipe INSTANCE = new CarpetDyeRecipe();
    public static final RecipeSerializer<CarpetDyeRecipe> SERIALIZER = new RecipeSerializer<>() {
        private final MapCodec<CarpetDyeRecipe> codec = MapCodec.unit(INSTANCE);
        private final StreamCodec<RegistryFriendlyByteBuf, CarpetDyeRecipe> stream = StreamCodec.unit(INSTANCE);

        @Override
        public MapCodec<CarpetDyeRecipe> codec() {
            return this.codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CarpetDyeRecipe> streamCodec() {
            return this.stream;
        }
    };

    public CarpetDyeRecipe() {
        super(net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    private record Match(ItemStack carpet, DyeColor color) {
    }

    private static @Nullable Match match(CraftingInput input) {
        ItemStack carpet = null;
        DyeColor color = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(GearContent.FLYING_CARPET.get()) && carpet == null) {
                carpet = stack;
                continue;
            }
            DyeColor dye = DyeColor.getColor(stack);
            if (dye != null && color == null) {
                color = dye;
                continue;
            }
            return null;
        }
        return carpet != null && color != null ? new Match(carpet, color) : null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return match(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, net.minecraft.core.HolderLookup.Provider registries) {
        Match match = match(input);
        if (match == null) {
            return ItemStack.EMPTY;
        }
        ItemStack out = match.carpet().copyWithCount(1);
        out.set(GearContent.DYE.get(), match.color());
        return out;
    }

    @Override
    public RecipeSerializer<CarpetDyeRecipe> getSerializer() {
        return SERIALIZER;
    }
}
