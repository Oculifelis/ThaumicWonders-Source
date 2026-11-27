package com.thaumicwonders;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The shared registers and helpers. Each content group declares its own entries with these helpers; the creative
 * tab lists everything in registration order (the order the groups load in {@link ThaumicWonders}).
 */
public final class TWRegistry {
    private static final String MODID = ThaumicWonders.MODID;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, MODID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);
    public static final DeferredRegister<net.minecraft.world.item.ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    private static final List<Supplier<? extends Item>> TAB_ITEMS = new ArrayList<>();
    /** The tab icon; the original used the Flying Carpet, which the gear group sets once it registers it. */
    private static Supplier<? extends Item> icon = () -> Items.BOOK;

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.thaumaturgicwonders"))
            .icon(() -> new ItemStack(icon.get()))
            .displayItems((params, out) -> TAB_ITEMS.forEach(i -> out.accept(i.get())))
            .build());

    private TWRegistry() {
    }

    public static void setTabIcon(Supplier<? extends Item> item) {
        icon = item;
    }

    /** Adds an item to the tab (for items registered without these helpers). */
    public static <I extends Item> Supplier<I> tab(Supplier<I> item) {
        TAB_ITEMS.add(item);
        return item;
    }

    /** A block with a plain block item in the tab. */
    public static <B extends Block> DeferredBlock<B> block(String name, Function<Block.Properties, B> factory, UnaryOperator<Block.Properties> props) {
        DeferredBlock<B> block = BLOCKS.register(name, () -> factory.apply(props.apply(Block.Properties.of())));
        TAB_ITEMS.add(ITEMS.registerSimpleBlockItem(block));
        return block;
    }

    /** A block with its own block item class, in the tab. */
    public static <B extends Block, I extends BlockItem> DeferredBlock<B> block(String name, Function<Block.Properties, B> factory,
            UnaryOperator<Block.Properties> props, BlockItemFactory<I> itemFactory, UnaryOperator<Item.Properties> itemProps) {
        DeferredBlock<B> block = BLOCKS.register(name, () -> factory.apply(props.apply(Block.Properties.of())));
        TAB_ITEMS.add(ITEMS.register(name, () -> itemFactory.create(block.get(), itemProps.apply(new Item.Properties()))));
        return block;
    }

    /** A block with no item (multiblock parts, placeholders, crops whose item is a seed). */
    public static <B extends Block> DeferredBlock<B> blockNoItem(String name, Function<Block.Properties, B> factory, UnaryOperator<Block.Properties> props) {
        return BLOCKS.register(name, () -> factory.apply(props.apply(Block.Properties.of())));
    }

    /** A block whose block item stays out of the creative tab (the original's tab-less multiblock cores). */
    public static <B extends Block> DeferredBlock<B> blockHidden(String name, Function<Block.Properties, B> factory, UnaryOperator<Block.Properties> props) {
        DeferredBlock<B> block = BLOCKS.register(name, () -> factory.apply(props.apply(Block.Properties.of())));
        ITEMS.registerSimpleBlockItem(block);
        return block;
    }

    public static <I extends Item> DeferredItem<I> item(String name, Function<Item.Properties, I> factory, UnaryOperator<Item.Properties> props) {
        DeferredItem<I> item = ITEMS.register(name, () -> factory.apply(props.apply(new Item.Properties())));
        TAB_ITEMS.add(item);
        return item;
    }

    public static DeferredItem<Item> item(String name) {
        return item(name, Item::new, p -> p);
    }

    /** An entity type built from the given builder settings. */
    public static <E extends net.minecraft.world.entity.Entity> DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<E>> entity(
            String name, net.minecraft.world.entity.EntityType.EntityFactory<E> factory, net.minecraft.world.entity.MobCategory category,
            UnaryOperator<net.minecraft.world.entity.EntityType.Builder<E>> builder) {
        return ENTITIES.register(name, () -> builder.apply(net.minecraft.world.entity.EntityType.Builder.of(factory, category)).build(MODID + ":" + name));
    }

    @FunctionalInterface
    public interface BlockItemFactory<I extends BlockItem> {
        I create(Block block, Item.Properties props);
    }

    static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ARMOR_MATERIALS.register(bus);
        ITEMS.register(bus);
        COMPONENTS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ENTITIES.register(bus);
        MENUS.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        RECIPE_TYPES.register(bus);
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
        EFFECTS.register(bus);
        ATTACHMENTS.register(bus);
        TABS.register(bus);
    }
}
