package com.thaumicwonders.alchemy;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * thaumaturgicwonders:disenchant. Exactly one Disjunction Cloth and exactly one enchanted item (enchanted books keep
 * theirs in stored_enchantments and do not count), nothing else: the result is the item with its enchantments
 * removed (repair cost and everything else kept). The cloth comes back one point more worn, and breaks after its
 * 51st use. Crafting it releases 5 vis per enchantment level into the aura ({@link AlchemyContent}).
 */
public class DisenchantRecipe extends CustomRecipe {
    public static final RecipeSerializer<DisenchantRecipe> SERIALIZER = new SimpleCraftingRecipeSerializer<>(DisenchantRecipe::new);

    public DisenchantRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    private static boolean isCloth(ItemStack stack) {
        return stack.is(AlchemyContent.DISJUNCTION_CLOTH.get());
    }

    /** The enchanted item of a matching grid, or null. Works on both the recipe input and the live craft container. */
    static @Nullable ItemStack target(int size, java.util.function.IntFunction<ItemStack> slot) {
        ItemStack cloth = null;
        ItemStack target = null;
        for (int i = 0; i < size; i++) {
            ItemStack stack = slot.apply(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (isCloth(stack) && cloth == null) {
                cloth = stack;
            } else if (!isCloth(stack) && stack.isEnchanted() && target == null) {
                target = stack;
            } else {
                return null;
            }
        }
        return cloth != null ? target : null;
    }

    static @Nullable ItemStack target(Container container) {
        return target(container.getContainerSize(), container::getItem);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return target(input.size(), input::getItem) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack target = target(input.size(), input::getItem);
        if (target == null) {
            return ItemStack.EMPTY;
        }
        ItemStack out = target.copyWithCount(1);
        out.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        return out;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (isCloth(stack)) {
                int damage = stack.getDamageValue() + 1;
                if (damage <= stack.getMaxDamage()) {
                    ItemStack worn = stack.copyWithCount(1);
                    worn.setDamageValue(damage);
                    remaining.set(i, worn);
                }
            }
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
