package com.thaumicwonders.alchemy;

import static com.thaumicwonders.TWRegistry.BLOCK_ENTITIES;
import static com.thaumicwonders.TWRegistry.ENTITIES;
import static com.thaumicwonders.TWRegistry.FLUIDS;
import static com.thaumicwonders.TWRegistry.FLUID_TYPES;
import static com.thaumicwonders.TWRegistry.ITEMS;
import static com.thaumicwonders.TWRegistry.RECIPE_SERIALIZERS;

import com.leclowndu93150.thaumaturge.api.aspect.AspectCapabilities;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCapabilities;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.thaumicwonders.ThaumicWonders;
import com.thaumicwonders.TWRegistry;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** The alchemy group (see docs/COVERAGE.md rows 2-13). */
public final class AlchemyContent {
    private AlchemyContent() {
    }

    // The original's BlockTileTW: hardness 2, resistance 20 (modern 12); BlockTW for the capacitor: 1.5 / 2.
    private static Block.Properties device(Block.Properties p) {
        return p.mapColor(MapColor.STONE).strength(2.0F, 12.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
    }

    // ---- Everburning Urn ------------------------------------------------------------------------------------------
    public static final DeferredBlock<EverburningUrnBlock> EVERBURNING_URN = TWRegistry.block("everburning_urn",
            EverburningUrnBlock::new, p -> device(p).noOcclusion());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EverburningUrnBlockEntity>> EVERBURNING_URN_BE =
            BLOCK_ENTITIES.register("everburning_urn", () -> new BlockEntityType<>(EverburningUrnBlockEntity::new, EVERBURNING_URN.get()));

    // ---- Creative Essentia Jar ------------------------------------------------------------------------------------
    public static final DeferredBlock<CreativeEssentiaJarBlock> CREATIVE_ESSENTIA_JAR = TWRegistry.block("creative_essentia_jar",
            CreativeEssentiaJarBlock::new, p -> p.mapColor(MapColor.NONE).strength(0.3F).sound(SoundType.GLASS).noOcclusion(),
            CreativeEssentiaJarItem::new, p -> p.rarity(Rarity.EPIC));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeEssentiaJarBlockEntity>> CREATIVE_ESSENTIA_JAR_BE =
            BLOCK_ENTITIES.register("creative_essentia_jar", () -> new BlockEntityType<>(CreativeEssentiaJarBlockEntity::new, CREATIVE_ESSENTIA_JAR.get()));

    // ---- Hexamite -------------------------------------------------------------------------------------------------
    public static final DeferredBlock<HexamiteBlock> HEXAMITE = TWRegistry.block("hexamite", HexamiteBlock::new,
            p -> p.mapColor(MapColor.FIRE).instabreak().sound(SoundType.GRASS).ignitedByLava().isRedstoneConductor((s, l, q) -> false));
    public static final DeferredHolder<EntityType<?>, EntityType<HexamitePrimed>> HEXAMITE_PRIMED = ENTITIES.registerEntityType(
            "hexamite_primed", HexamitePrimed::new, MobCategory.MISC,
            b -> b.noLootTable().fireImmune().sized(0.98F, 0.98F).eyeHeight(0.15F).clientTrackingRange(10).updateInterval(10));

    // ---- Flux Capacitor and Flux Distiller ------------------------------------------------------------------------
    /** The original drew the capacitor at a lightmap floor of 180 per charge; here each charge gives one light level. */
    public static final DeferredBlock<FluxCapacitorBlock> FLUX_CAPACITOR = TWRegistry.block("flux_capacitor", FluxCapacitorBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(1.5F, 1.2F).sound(SoundType.STONE).requiresCorrectToolForDrops()
                    .randomTicks().lightLevel(s -> s.getValue(FluxCapacitorBlock.CHARGE)),
            (block, props) -> new FluxChargeBlockItem(block, FluxCapacitorBlock.CHARGE, props), p -> p);
    public static final DeferredBlock<FluxDistillerBlock> FLUX_DISTILLER = TWRegistry.block("flux_distiller", FluxDistillerBlock::new,
            p -> device(p).noOcclusion(),
            (block, props) -> new FluxChargeBlockItem(block, FluxCapacitorBlock.CHARGE, props), p -> p);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluxDistillerBlockEntity>> FLUX_DISTILLER_BE =
            BLOCK_ENTITIES.register("flux_distiller", () -> new BlockEntityType<>(FluxDistillerBlockEntity::new, FLUX_DISTILLER.get()));

    // ---- Disjunction Cloth ----------------------------------------------------------------------------------------
    public static final DeferredItem<Item> DISJUNCTION_CLOTH = TWRegistry.item("disjunction_cloth", Item::new,
            p -> p.durability(50).rarity(Rarity.UNCOMMON));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DisenchantRecipe>> DISENCHANT =
            RECIPE_SERIALIZERS.register("disenchant", () -> DisenchantRecipe.SERIALIZER);

    // ---- Panacea --------------------------------------------------------------------------------------------------
    private static final FoodProperties PANACEA_FOOD = new FoodProperties.Builder().nutrition(6).saturationModifier(1.8F).alwaysEdible().build();
    public static final DeferredItem<PanaceaItem> PANACEA = TWRegistry.item("panacea", p -> new PanaceaItem(false, p),
            p -> p.rarity(Rarity.RARE).food(PANACEA_FOOD, Consumables.DEFAULT_FOOD));
    public static final DeferredItem<PanaceaItem> ENCHANTED_PANACEA = TWRegistry.item("enchanted_panacea", p -> new PanaceaItem(true, p),
            p -> p.rarity(Rarity.EPIC).food(PANACEA_FOOD, Consumables.DEFAULT_FOOD).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

    // ---- Lethe Water ----------------------------------------------------------------------------------------------
    public static final DeferredItem<LetheWaterItem> LETHE_WATER = TWRegistry.item("lethe_water", LetheWaterItem::new,
            p -> p.rarity(Rarity.UNCOMMON).stacksTo(16).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

    // ---- Alkahest Vat ---------------------------------------------------------------------------------------------
    public static final DeferredBlock<AlkahestVatBlock> ALKAHEST_VAT = TWRegistry.block("alkahest_vat", AlkahestVatBlock::new,
            p -> device(p).noOcclusion().lightLevel(s -> 6));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlkahestVatBlockEntity>> ALKAHEST_VAT_BE =
            BLOCK_ENTITIES.register("alkahest_vat", () -> new BlockEntityType<>(AlkahestVatBlockEntity::new, ALKAHEST_VAT.get()));

    // ---- Quicksilver fluid ----------------------------------------------------------------------------------------
    public static final DeferredHolder<FluidType, FluidType> QUICKSILVER_TYPE = FLUID_TYPES.register("quicksilver",
            () -> new FluidType(FluidType.Properties.create().descriptionId("block.thaumaturgicwonders.fluid_quicksilver")
                    .viscosity(1560).density(13593).canSwim(false).canDrown(true).fallDistanceModifier(0.0F)));
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, QuicksilverFluid.Source> QUICKSILVER =
            FLUIDS.register("quicksilver", QuicksilverFluid.Source::new);
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, QuicksilverFluid.Flowing> FLOWING_QUICKSILVER =
            FLUIDS.register("flowing_quicksilver", QuicksilverFluid.Flowing::new);
    public static final DeferredBlock<LiquidBlock> QUICKSILVER_BLOCK = TWRegistry.blockNoItem("fluid_quicksilver",
            p -> new LiquidBlock(QUICKSILVER.get(), p),
            p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).replaceable().noCollision().strength(100.0F).pushReaction(PushReaction.DESTROY)
                    .noLootTable().liquid().sound(SoundType.EMPTY));
    public static final DeferredItem<BucketItem> QUICKSILVER_BUCKET = TWRegistry.item("quicksilver_bucket",
            p -> new BucketItem(QUICKSILVER.get(), p), p -> p.craftRemainder(Items.BUCKET).stacksTo(1));

    // ---- Mystic Gardening -----------------------------------------------------------------------------------------
    public static final TagKey<Block> SHIMMERLEAF_SOIL = soil("shimmerleaf_soil");
    public static final TagKey<Block> CINDERPEARL_SOIL = soil("cinderpearl_soil");
    public static final TagKey<Block> VISHROOM_SOIL = soil("vishroom_soil");

    private static TagKey<Block> soil(String name) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ThaumicWonders.MODID, name));
    }

    private static Block.Properties crop(Block.Properties p, MapColor color) {
        return p.mapColor(color).noCollision().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY);
    }

    public static final DeferredItem<BlockItem> SHIMMERLEAF_SEED = seed("shimmerleaf_seed", () -> AlchemyContent.SHIMMERLEAF_CROP.get());
    public static final DeferredItem<BlockItem> CINDERPEARL_SEED = seed("cinderpearl_seed", () -> AlchemyContent.CINDERPEARL_CROP.get());
    public static final DeferredItem<BlockItem> VISHROOM_SPORE = seed("vishroom_spore", () -> AlchemyContent.VISHROOM_CROP.get());

    public static final DeferredBlock<MysticCropBlock> SHIMMERLEAF_CROP = TWRegistry.blockNoItem("shimmerleaf_crop",
            p -> new MysticCropBlock(SHIMMERLEAF_SEED, TTBlocks.PLANT_SHIMMERLEAF, SHIMMERLEAF_SOIL, false, p),
            p -> crop(p, MapColor.PLANT).offsetType(BlockBehaviour.OffsetType.XZ));
    public static final DeferredBlock<MysticCropBlock> CINDERPEARL_CROP = TWRegistry.blockNoItem("cinderpearl_crop",
            p -> new MysticCropBlock(CINDERPEARL_SEED, TTBlocks.PLANT_CINDERPEARL, CINDERPEARL_SOIL, false, p),
            p -> crop(p, MapColor.COLOR_ORANGE).offsetType(BlockBehaviour.OffsetType.XZ));
    public static final DeferredBlock<MysticCropBlock> VISHROOM_CROP = TWRegistry.blockNoItem("vishroom_crop",
            p -> new MysticCropBlock(VISHROOM_SPORE, TTBlocks.PLANT_VISHROOM, VISHROOM_SOIL, true, p),
            p -> crop(p, MapColor.COLOR_PURPLE));

    private static DeferredItem<BlockItem> seed(String name, java.util.function.Supplier<? extends Block> crop) {
        DeferredItem<BlockItem> item = ITEMS.registerItem(name, p -> new BlockItem(crop.get(), p), p -> p.useItemDescriptionPrefix());
        TWRegistry.tab(item);
        return item;
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(AlchemyContent::capabilities);
        NeoForge.EVENT_BUS.addListener(FluxExplosion::onDetonate);
        NeoForge.EVENT_BUS.addListener(AlchemyContent::onCrafted);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(EssentiaCapabilities.TRANSPORT, CREATIVE_ESSENTIA_JAR_BE.get(), (be, side) -> be);
        event.registerBlockEntity(EssentiaCapabilities.STORAGE, CREATIVE_ESSENTIA_JAR_BE.get(), (be, side) -> be.storage(side));
        event.registerBlockEntity(AspectCapabilities.CONTAINER, CREATIVE_ESSENTIA_JAR_BE.get(), (be, side) -> be);
        event.registerBlockEntity(EssentiaCapabilities.TRANSPORT, FLUX_DISTILLER_BE.get(), (be, side) -> be);
        event.registerBlockEntity(AspectCapabilities.CONTAINER, FLUX_DISTILLER_BE.get(), (be, side) -> be);
        // The urn only gives lava out of its top, as the original's fluid handler did.
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, EVERBURNING_URN_BE.get(),
                (be, side) -> side == null || side == Direction.UP ? be.tank() : null);
    }

    /** Disenchanting with the Disjunction Cloth releases 5 vis per enchantment level at the crafter. */
    private static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        ItemStack target = DisenchantRecipe.target(event.getInventory());
        if (target == null) {
            return;
        }
        ItemEnchantments enchantments = target.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        int levels = 0;
        for (var entry : enchantments.entrySet()) {
            levels += entry.getIntValue();
        }
        if (levels > 0) {
            AuraHelper.addVis(player.level(), player.blockPosition(), levels * 5.0F);
        }
    }
}
