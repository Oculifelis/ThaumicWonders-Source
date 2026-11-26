package com.thaumicwonders.primordial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * {@code thaumaturgicwonders:catalyzation}: what a catalyst stone in the Catalyzation Chamber turns one item into (the
 * original hardcoded these per stone with ore-dictionary names). {@code stone} is the catalyst, {@code input} the
 * item thrown in, and the output is either a fixed {@code result} or the first item of {@code result_tag} (the
 * original's "first ore-dictionary entry", used where no fixed item exists, e.g. tin ingots). One item goes in per
 * operation, whatever the output count.
 */
public final class CatalyzationRecipe implements Recipe<CatalyzationRecipe.Input> {
    public static final MapCodec<CatalyzationRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("stone").forGetter(r -> r.stone),
            Ingredient.CODEC.fieldOf("input").forGetter(r -> r.input),
            ItemStackTemplate.CODEC.optionalFieldOf("result").forGetter(r -> r.result),
            TagKey.codec(Registries.ITEM).optionalFieldOf("result_tag").forGetter(r -> r.resultTag),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(r -> r.count)).apply(i, CatalyzationRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CatalyzationRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.stone,
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.input,
            ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC), r -> r.result,
            ByteBufCodecs.optional(net.minecraft.resources.Identifier.STREAM_CODEC.map(id -> TagKey.create(Registries.ITEM, id), TagKey::location)), r -> r.resultTag,
            ByteBufCodecs.VAR_INT, r -> r.count,
            CatalyzationRecipe::new);

    public static final RecipeSerializer<CatalyzationRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    /** The stone and the item to refine. */
    public record Input(ItemStack stone, ItemStack item) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index == 0 ? stone : item;
        }

        @Override
        public int size() {
            return 2;
        }
    }

    private final Ingredient stone;
    private final Ingredient input;
    private final Optional<ItemStackTemplate> result;
    private final Optional<TagKey<Item>> resultTag;
    private final int count;

    public CatalyzationRecipe(Ingredient stone, Ingredient input, Optional<ItemStackTemplate> result, Optional<TagKey<Item>> resultTag, int count) {
        this.stone = stone;
        this.input = input;
        this.result = result;
        this.resultTag = resultTag;
        this.count = count;
    }

    public Ingredient stone() {
        return stone;
    }

    public Ingredient input() {
        return input;
    }

    /** The output for this recipe, empty when its tag has no items. */
    public ItemStack output() {
        if (result.isPresent()) {
            return result.get().create();
        }
        if (resultTag.isPresent()) {
            for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(resultTag.get())) {
                return new ItemStack(item, count);
            }
        }
        return ItemStack.EMPTY;
    }

    /** The output of a stone for an item, or empty when that stone rejects it. */
    public static ItemStack refine(ServerLevel level, ItemStack stone, ItemStack item) {
        if (stone.isEmpty() || item.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return level.recipeAccess().getRecipeFor(PrimordialContent.CATALYZATION_TYPE.get(), new Input(stone, item.copyWithCount(1)), level)
                .map(h -> h.value().output()).orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean matches(Input in, Level level) {
        return stone.test(in.stone()) && input.test(in.item()) && !output().isEmpty();
    }

    @Override
    public ItemStack assemble(Input in) {
        return output();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<CatalyzationRecipe> getSerializer() {
        return PrimordialContent.CATALYZATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<CatalyzationRecipe> getType() {
        return PrimordialContent.CATALYZATION_TYPE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
