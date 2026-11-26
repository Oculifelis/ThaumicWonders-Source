package com.thaumicwonders.primordial;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.thaumicwonders.ThaumicWonders;
import com.thaumicwonders.TWRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCapabilities;

/**
 * The primordial group (see docs/COVERAGE.md rows 23-28): the Catalyzation Chamber and its catalyst stones, the
 * eldritch clusters, the Primordial Accelerator with its tunnel, terminus and grain, the Primordial Accretion
 * Chamber, and the Coalescence Matrix with its precursor.
 */
public final class PrimordialContent {
    private PrimordialContent() {
    }

    /** Uses a fresh Thaumaturge primordial pearl has (damage 0..7; it is used up at 8). */
    public static final int PEARL_USES = 8;

    // ---- recipe types -----------------------------------------------------------------------------------------
    public static final DeferredHolder<RecipeType<?>, RecipeType<CatalyzationRecipe>> CATALYZATION_TYPE = TWRegistry.RECIPE_TYPES.register(
            "catalyzation", () -> RecipeType.simple(ThaumicWonders.id("catalyzation")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CatalyzationRecipe>> CATALYZATION_SERIALIZER =
            TWRegistry.RECIPE_SERIALIZERS.register("catalyzation", () -> CatalyzationRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TagSmeltingRecipe>> TAG_SMELTING_SERIALIZER =
            TWRegistry.RECIPE_SERIALIZERS.register("tag_smelting", () -> TagSmeltingRecipe.SERIALIZER);

    // ---- items ------------------------------------------------------------------------------------------------
    public static final DeferredItem<CatalystStoneItem> ALCHEMIST_STONE = TWRegistry.item("alchemist_stone",
            p -> new CatalystStoneItem(p, 50, TTAspects.ORDO), p -> p.durability(63).enchantable(10));
    public static final DeferredItem<CatalystStoneItem> TRANSMUTER_STONE = TWRegistry.item("transmuter_stone",
            p -> new CatalystStoneItem(p, 33, TTAspects.PERMUTATIO), p -> p.durability(63).enchantable(10));
    /** The nine eldritch clusters (the original's eldritch_cluster metas 0..8). */
    public static final Map<String, DeferredItem<Item>> ELDRITCH_CLUSTERS = new LinkedHashMap<>();

    static {
        for (String metal : new String[] {"iron", "gold", "copper", "tin", "silver", "lead", "cinnabar", "quartz", "void"}) {
            ELDRITCH_CLUSTERS.put(metal, TWRegistry.item("eldritch_cluster_" + metal));
        }
    }

    public static final DeferredItem<CatalystStoneItem> ALIENIST_STONE = TWRegistry.item("alienist_stone",
            p -> new CatalystStoneItem(p, 10, TTAspects.VITIUM), p -> p.durability(63).enchantable(10).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> PRIMORDIAL_GRAIN = TWRegistry.item("primordial_grain");

    // ---- blocks -----------------------------------------------------------------------------------------------
    public static final DeferredBlock<ChamberBlock> CATALYZATION_CHAMBER = TWRegistry.blockHidden("catalyzation_chamber",
            p -> new ChamberBlock(p, ChamberBlock.Kind.CATALYZATION),
            p -> p.mapColor(MapColor.STONE).strength(2.0F, 20.0F).sound(SoundType.STONE).lightLevel(s -> 13).noOcclusion().noLootTable());
    public static final DeferredBlock<PlaceholderBlock> PLACEHOLDER_ARCANE_STONE = TWRegistry.blockNoItem("placeholder_arcane_stone",
            p -> new PlaceholderBlock(p, TTBlocks.STONE_ARCANE, false), PrimordialContent::stonePlaceholder);
    public static final DeferredBlock<PlaceholderBlock> PLACEHOLDER_OBSIDIAN = TWRegistry.blockNoItem("placeholder_obsidian",
            p -> new PlaceholderBlock(p, () -> Blocks.OBSIDIAN, false), PrimordialContent::stonePlaceholder);

    public static final DeferredBlock<PrimordialAcceleratorBlock> PRIMORDIAL_ACCELERATOR = TWRegistry.block("primordial_accelerator",
            PrimordialAcceleratorBlock::new, p -> p.mapColor(MapColor.METAL).strength(2.0F, 20.0F).sound(SoundType.METAL).noOcclusion());
    public static final DeferredBlock<AcceleratorPartBlock> ACCELERATOR_TUNNEL = TWRegistry.block("primordial_accelerator_tunnel",
            AcceleratorPartBlock::new, p -> p.mapColor(MapColor.METAL).strength(1.5F, 6.0F).sound(SoundType.METAL).noOcclusion());
    public static final DeferredBlock<AcceleratorPartBlock> ACCELERATOR_TERMINUS = TWRegistry.block("primordial_accelerator_terminus",
            AcceleratorPartBlock::new, p -> p.mapColor(MapColor.STONE).strength(1.5F, 6.0F).sound(SoundType.STONE).noOcclusion());

    public static final DeferredBlock<ChamberBlock> ACCRETION_CHAMBER = TWRegistry.blockHidden("primordial_accretion_chamber",
            p -> new ChamberBlock(p, ChamberBlock.Kind.ACCRETION),
            p -> p.mapColor(MapColor.METAL).strength(2.0F, 20.0F).sound(SoundType.METAL).lightLevel(s -> 13).noOcclusion().noLootTable());
    public static final DeferredBlock<PlaceholderBlock> PLACEHOLDER_THAUMIUM_BLOCK = TWRegistry.blockNoItem("placeholder_thaumium_block",
            p -> new PlaceholderBlock(p, TTBlocks.METAL_THAUMIUM_BLOCK, true), PrimordialContent::metalPlaceholder);
    public static final DeferredBlock<PlaceholderBlock> PLACEHOLDER_VOID_METAL_BLOCK = TWRegistry.blockNoItem("placeholder_void_metal_block",
            p -> new PlaceholderBlock(p, TTBlocks.METAL_VOID_BLOCK, true), PrimordialContent::metalPlaceholder);
    public static final DeferredBlock<PlaceholderBlock> PLACEHOLDER_ADV_ALCH_CONSTRUCT = TWRegistry.blockNoItem("placeholder_adv_alch_construct",
            p -> new PlaceholderBlock(p, TTBlocks.ADVANCED_ALCHEMICAL_CONSTRUCT, true), PrimordialContent::metalPlaceholder);

    public static final DeferredBlock<Block> COALESCENCE_MATRIX_PRECURSOR = TWRegistry.block("coalescence_matrix_precursor",
            Block::new, p -> p.mapColor(MapColor.GOLD).strength(1.5F, 6.0F).sound(SoundType.METAL));
    public static final DeferredBlock<CoalescenceMatrixBlock> COALESCENCE_MATRIX = TWRegistry.blockHidden("coalescence_matrix",
            CoalescenceMatrixBlock::new, p -> p.mapColor(MapColor.GOLD).strength(2.0F, 20.0F).sound(SoundType.METAL));

    // ---- block entities ---------------------------------------------------------------------------------------
    public static final Supplier<BlockEntityType<CatalyzationChamberBlockEntity>> CATALYZATION_CHAMBER_BE = TWRegistry.BLOCK_ENTITIES.register(
            "catalyzation_chamber", () -> new BlockEntityType<>(CatalyzationChamberBlockEntity::new, CATALYZATION_CHAMBER.get()));
    public static final Supplier<BlockEntityType<AccretionChamberBlockEntity>> ACCRETION_CHAMBER_BE = TWRegistry.BLOCK_ENTITIES.register(
            "primordial_accretion_chamber", () -> new BlockEntityType<>(AccretionChamberBlockEntity::new, ACCRETION_CHAMBER.get()));
    public static final Supplier<BlockEntityType<AccretionPortBlockEntity>> ACCRETION_PORT_BE = TWRegistry.BLOCK_ENTITIES.register(
            "primordial_accretion_chamber_placeholder", () -> new BlockEntityType<>(AccretionPortBlockEntity::new,
                    PLACEHOLDER_THAUMIUM_BLOCK.get(), PLACEHOLDER_VOID_METAL_BLOCK.get(), PLACEHOLDER_ADV_ALCH_CONSTRUCT.get()));
    public static final Supplier<BlockEntityType<PrimordialAcceleratorBlockEntity>> ACCELERATOR_BE = TWRegistry.BLOCK_ENTITIES.register(
            "primordial_accelerator", () -> new BlockEntityType<>(PrimordialAcceleratorBlockEntity::new, PRIMORDIAL_ACCELERATOR.get()));
    public static final Supplier<BlockEntityType<CoalescenceMatrixBlockEntity>> COALESCENCE_MATRIX_BE = TWRegistry.BLOCK_ENTITIES.register(
            "coalescence_matrix", () -> new BlockEntityType<>(CoalescenceMatrixBlockEntity::new, COALESCENCE_MATRIX.get()));

    // ---- menus ------------------------------------------------------------------------------------------------
    public static final DeferredHolder<MenuType<?>, MenuType<SocketMenu>> CATALYZATION_MENU = TWRegistry.MENUS.register("catalyzation_chamber",
            () -> new MenuType<>(SocketMenu::catalyzationClient, FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<MenuType<?>, MenuType<SocketMenu>> ACCELERATOR_MENU = TWRegistry.MENUS.register("primordial_accelerator",
            () -> new MenuType<>(SocketMenu::acceleratorClient, FeatureFlags.VANILLA_SET));

    private static Block.Properties stonePlaceholder(Block.Properties p) {
        return p.mapColor(MapColor.STONE).strength(2.5F, 10.0F).sound(SoundType.STONE).noOcclusion().pushReaction(
                net.minecraft.world.level.material.PushReaction.BLOCK);
    }

    private static Block.Properties metalPlaceholder(Block.Properties p) {
        return p.mapColor(MapColor.METAL).strength(2.5F, 10.0F).sound(SoundType.METAL).noOcclusion().pushReaction(
                net.minecraft.world.level.material.PushReaction.BLOCK);
    }

    public static boolean isCatalystStone(ItemStack stack) {
        return stack.getItem() instanceof CatalystStoneItem;
    }

    /** A Thaumaturge primordial pearl the accelerator takes: at least two uses left (damage below 7). */
    public static boolean isUsablePearl(ItemStack stack) {
        return stack.is(TTItems.PRIMORDIAL_PEARL.get()) && stack.getDamageValue() < PEARL_USES - 1;
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(PrimordialContent::registerCapabilities);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.OnDatapackSyncEvent e) ->
                e.sendRecipes(CATALYZATION_TYPE.get()));
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, CATALYZATION_CHAMBER_BE.get(),
                (be, side) -> side == null ? null : new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ACCRETION_CHAMBER_BE.get(),
                (be, side) -> side == null ? null : new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ACCELERATOR_BE.get(),
                (be, side) -> new WorldlyContainerWrapper(be, side == null ? net.minecraft.core.Direction.UP : side));
        event.registerBlockEntity(EssentiaCapabilities.TRANSPORT, ACCRETION_PORT_BE.get(), (be, side) -> be);
    }
}
