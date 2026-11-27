package com.thaumicwonders.primordial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;

/**
 * {@code thaumaturgicwonders:tag_smelting}: a furnace recipe whose output is the first item of an item tag (the original
 * smelted its tin, silver and lead clusters into "the first {@code ingotTin}" of the ore dictionary). It only matches
 * while the tag has an item, so packs without that metal simply cannot smelt it.
 */
public class TagSmeltingRecipe extends SmeltingRecipe {
    public static final MapCodec<TagSmeltingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(TagSmeltingRecipe::input),
            TagKey.codec(Registries.ITEM).fieldOf("result_tag").forGetter(r -> r.resultTag),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(r -> r.count),
            Codec.FLOAT.optionalFieldOf("experience", 0.0F).forGetter(AbstractCookingRecipe::getExperience),
            Codec.INT.optionalFieldOf("cookingtime", 200).forGetter(AbstractCookingRecipe::getCookingTime)).apply(i, TagSmeltingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TagSmeltingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, TagSmeltingRecipe::input,
            ResourceLocation.STREAM_CODEC.map(id -> TagKey.create(Registries.ITEM, id), TagKey::location), r -> r.resultTag,
            ByteBufCodecs.VAR_INT, r -> r.count,
            ByteBufCodecs.FLOAT, AbstractCookingRecipe::getExperience,
            ByteBufCodecs.VAR_INT, AbstractCookingRecipe::getCookingTime,
            TagSmeltingRecipe::new);

    public static final RecipeSerializer<TagSmeltingRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<TagSmeltingRecipe> codec() {
            return MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TagSmeltingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };

    private final TagKey<Item> resultTag;
    private final int count;

    public TagSmeltingRecipe(Ingredient ingredient, TagKey<Item> resultTag, int count, float experience, int cookingTime) {
        // The template is never handed out: assemble() reads the tag.
        super("", CookingBookCategory.MISC, ingredient, new ItemStack(Items.BARRIER), experience, cookingTime);
        this.resultTag = resultTag;
        this.count = count;
    }

    public Ingredient input() {
        return ingredient;
    }

    private ItemStack output() {
        for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(resultTag)) {
            return new ItemStack(item, count);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean matches(SingleRecipeInput in, Level level) {
        return super.matches(in, level) && !output().isEmpty();
    }

    @Override
    public ItemStack assemble(SingleRecipeInput in, HolderLookup.Provider registries) {
        return output();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return output();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return PrimordialContent.TAG_SMELTING_SERIALIZER.get();
    }
}
