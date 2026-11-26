package com.thaumicwonders.compat.jei;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.client.recipes.TTClientRecipes;
import com.leclowndu93150.thaumaturge.compat.jei.ingredient.AspectIngredientRenderer;
import com.leclowndu93150.thaumaturge.compat.jei.ingredient.AspectIngredientType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

/**
 * Shared JEI pieces for the Thaumaturge addons (same file in each addon, only the package differs). Loaded only with
 * JEI.
 *
 * <ul>
 *   <li>{@link Conversion} + {@link ConversionCategory}: one category class for every "this becomes that" the addon does
 *   outside Thaumaturge's own recipe types: machines, world interactions, rituals, focus effects, drops. Inputs, an
 *   optional station, an aspect cost drawn with Thaumaturge's own aspect slots, outputs with optional chances and a
 *   line of text. Inputs and outputs are real slots, so recipe and usage lookups work both ways.</li>
 *   <li>{@link #infoPages}: an information page for every item of the mod that has a {@code jei.<modid>.info.<item>}
 *   line in the lang file. Adding the line adds the page; no code list to keep in step.</li>
 *   <li>{@link #recipes}: the client copy of a recipe type. The addon must send that type to clients with
 *   {@code OnDatapackSyncEvent#sendRecipes}, or JEI gets nothing.</li>
 * </ul>
 */
public final class JeiKit {
    private JeiKit() {}

    /** One shown conversion. Build with {@link #builder()}. */
    public record Conversion(List<List<ItemStack>> inputs, List<ItemStack> station, AspectList aspects,
                             List<List<ItemStack>> outputs, List<Float> chances, @Nullable Component note) {
        public static Builder builder() {
            return new Builder();
        }
    }

    public static final class Builder {
        private final List<List<ItemStack>> inputs = new ArrayList<>();
        private final List<ItemStack> station = new ArrayList<>();
        private AspectList aspects = AspectList.EMPTY;
        private final List<List<ItemStack>> outputs = new ArrayList<>();
        private final List<Float> chances = new ArrayList<>();
        private @Nullable Component note;

        /** One input slot cycling through the given alternatives. */
        public Builder in(ItemStack... alternatives) {
            List<ItemStack> list = Arrays.stream(alternatives).filter(s -> !s.isEmpty()).toList();
            if (!list.isEmpty()) inputs.add(list);
            return this;
        }

        public Builder in(ItemLike item) {
            return in(new ItemStack(item));
        }

        public Builder in(List<ItemStack> alternatives) {
            return in(alternatives.toArray(ItemStack[]::new));
        }

        /** The block or tool doing the work, shown above the arrow (also a lookup target). */
        public Builder station(ItemStack... stacks) {
            for (ItemStack s : stacks) if (!s.isEmpty()) station.add(s);
            return this;
        }

        public Builder station(ItemLike item) {
            return station(new ItemStack(item));
        }

        public Builder aspects(AspectList list) {
            aspects = list == null ? AspectList.EMPTY : list;
            return this;
        }

        public Builder out(ItemStack... alternatives) {
            return out(1f, alternatives);
        }

        public Builder out(ItemLike item) {
            return out(new ItemStack(item));
        }

        /** An output slot that only comes out {@code chance} of the time (0..1). */
        public Builder out(float chance, ItemStack... alternatives) {
            List<ItemStack> list = Arrays.stream(alternatives).filter(s -> !s.isEmpty()).toList();
            if (!list.isEmpty()) {
                outputs.add(list);
                chances.add(chance);
            }
            return this;
        }

        public Builder note(Component text) {
            note = text;
            return this;
        }

        public Conversion build() {
            return new Conversion(List.copyOf(inputs), List.copyOf(station), aspects, List.copyOf(outputs),
                    List.copyOf(chances), note);
        }
    }

    public static IRecipeType<Conversion> type(Identifier id) {
        return IRecipeType.create(id, Conversion.class);
    }

    /** Fixed-size category: up to 4 inputs, 1 station, 3 outputs, 8 aspects, one line of text. */
    public static final class ConversionCategory extends AbstractRecipeCategory<Conversion> {
        private static final int W = 160;

        public ConversionCategory(IGuiHelper gui, IRecipeType<Conversion> type, Component title, ItemLike icon) {
            this(gui, type, title, new ItemStack(icon));
        }

        public ConversionCategory(IGuiHelper gui, IRecipeType<Conversion> type, Component title, ItemStack icon) {
            super(type, title, gui.createDrawableItemStack(icon), W, 62);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, Conversion recipe, IFocusGroup focuses) {
            int n = Math.min(4, recipe.inputs().size());
            for (int i = 0; i < n; i++) {
                builder.addInputSlot(2 + i * 18, 4).setStandardSlotBackground().addItemStacks(recipe.inputs().get(i));
            }
            if (!recipe.station().isEmpty()) {
                builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 80, 0).addItemStacks(recipe.station());
            }
            int m = Math.min(3, recipe.outputs().size());
            for (int i = 0; i < m; i++) {
                IRecipeSlotBuilder slot = builder.addOutputSlot(104 + i * 18, 4).setOutputSlotBackground()
                        .addItemStacks(recipe.outputs().get(i));
                float chance = recipe.chances().get(i);
                if (chance < 1f) {
                    String pct = chance >= 0.1f ? Math.round(chance * 100) + "%" : String.format("%.1f%%", chance * 100);
                    slot.addRichTooltipCallback((view, tooltip) ->
                            tooltip.add(Component.translatableWithFallback("jei.thaumaturge_addons.chance", "Chance: %s", pct)));
                }
            }
            List<AspectInstance> aspects = recipe.aspects().entries();
            int a = Math.min(8, aspects.size());
            int start = (W - a * 18) / 2;
            for (int i = 0; i < a; i++) {
                builder.addInputSlot(start + i * 18, 26)
                        .setCustomRenderer(AspectIngredientType.INSTANCE, AspectIngredientRenderer.INSTANCE)
                        .add(AspectIngredientType.INSTANCE, aspects.get(i));
            }
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, Conversion recipe, IFocusGroup focuses) {
            builder.addRecipeArrowWidget().setPosition(76, recipe.station().isEmpty() ? 4 : 17);
            if (recipe.note() != null) {
                builder.addText(recipe.note(), W, 18).setPosition(0, 44).setColor(0xFF404040)
                        .setTextAlignment(HorizontalAlignment.CENTER);
            }
        }
    }

    /** Registers each station as a catalyst of the type. */
    public static void stations(IRecipeCatalystRegistration registration, IRecipeType<Conversion> type, ItemLike... items) {
        for (ItemLike item : items) {
            if (item.asItem() != net.minecraft.world.item.Items.AIR) registration.addCraftingStation(type, item);
        }
    }

    /**
     * An information page for every item in {@code modid} whose {@code jei.<modid>.info.<path>} line exists in the
     * loaded language. Returns how many pages were added.
     */
    public static int infoPages(IRecipeRegistration registration, String modid) {
        Language lang = Language.getInstance();
        int count = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals(modid)) continue;
            String key = "jei." + modid + ".info." + id.getPath();
            if (lang.has(key)) {
                registration.addIngredientInfo(item, Component.translatable(key));
                count++;
            }
        }
        return count;
    }

    /** All synced recipes of a type on the client (empty if the type was not sent to clients). */
    public static <I extends net.minecraft.world.item.crafting.RecipeInput, R extends Recipe<I>> List<RecipeHolder<R>> recipes(RecipeType<R> type) {
        var level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        return List.copyOf(TTClientRecipes.getRecipeMapForType(level, type).byType(type));
    }
}
