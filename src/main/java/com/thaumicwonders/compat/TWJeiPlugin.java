package com.thaumicwonders.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.thaumicwonders.ThaumicWonders;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.recipe.types.IRecipeType;
import com.thaumicwonders.compat.jei.JeiKit;
import com.thaumicwonders.primordial.PrimordialContent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * JEI: an information page for every item that comes from the world or a device rather than a recipe. The list is
 * {@code assets/thaumaturgicwonders/jei_info.json}, written by tools/gen.py with the text under {@code jei.thaumaturgicwonders.info.*}.
 * Arcane, crucible and infusion recipes are Thaumaturge's own types, which its plugin shows. Loaded only with JEI.
 */
@JeiPlugin
public final class TWJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return ThaumicWonders.id("jei");
    }

    static final IRecipeType<JeiKit.Conversion> CATALYZATION = JeiKit.type(ThaumicWonders.id("catalyzation"));

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new JeiKit.ConversionCategory(registration.getJeiHelpers().getGuiHelper(), CATALYZATION,
                Component.translatable("jei.thaumaturgicwonders.catalyzation"), PrimordialContent.ALCHEMIST_STONE.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        JeiKit.stations(registration, CATALYZATION, PrimordialContent.CATALYZATION_CHAMBER.get());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(CATALYZATION, JeiKit.recipes(PrimordialContent.CATALYZATION_TYPE.get()).stream()
                .map(h -> h.value()).filter(r -> !r.output().isEmpty())
                .map(r -> JeiKit.Conversion.builder().in(stacks(r.input())).in(stacks(r.stone()))
                        .station(PrimordialContent.CATALYZATION_CHAMBER.get()).out(r.output()).build())
                .toList());
        for (String name : infoNames()) {
            BuiltInRegistries.ITEM.getOptional(ThaumicWonders.id(name)).ifPresent(item ->
                    registration.addIngredientInfo(item, Component.translatable("jei.thaumaturgicwonders.info." + name)));
        }
    }

    private static List<ItemStack> stacks(Ingredient ingredient) {
        return ingredient.items().map(ItemStack::new).toList();
    }

    private static String[] infoNames() {
        try (InputStream in = TWJeiPlugin.class.getResourceAsStream("/assets/thaumaturgicwonders/jei_info.json")) {
            if (in == null) {
                return new String[0];
            }
            JsonArray array = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
            String[] out = new String[array.size()];
            for (int i = 0; i < out.length; i++) {
                out[i] = array.get(i).getAsString();
            }
            return out;
        } catch (Exception e) {
            ThaumicWonders.LOG.warn("Could not read the JEI info list", e);
            return new String[0];
        }
    }
}
