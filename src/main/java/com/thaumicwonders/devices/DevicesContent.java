package com.thaumicwonders.devices;

import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCapabilities;
import com.leclowndu93150.thaumaturge.api.items.ChargeDisplay;
import com.leclowndu93150.thaumaturge.api.items.ChargeProfile;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.thaumicwonders.TWRegistry;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The devices group (see docs/COVERAGE.md): Dimensional Ripper, Inspiration and Madness Engines, Meteorb, Meaty Orb,
 * Ore Diviner, Structure Diviner, Void Beacon and Timewinder.
 */
public final class DevicesContent {
    private DevicesContent() {
    }

    public static final DeferredBlock<DimensionalRipperBlock> DIMENSIONAL_RIPPER = TWRegistry.block("dimensional_ripper", DimensionalRipperBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(2.0F, 20.0F).sound(SoundType.METAL).noOcclusion());
    public static final DeferredBlock<ResearchEngineBlock> INSPIRATION_ENGINE = TWRegistry.block("inspiration_engine",
            p -> new ResearchEngineBlock(p, ResearchEngineBlockEntity.Kind.INSPIRATION),
            p -> p.mapColor(MapColor.METAL).strength(2.0F, 20.0F).sound(SoundType.METAL).noOcclusion());
    public static final DeferredBlock<ResearchEngineBlock> MADNESS_ENGINE = TWRegistry.block("madness_engine",
            p -> new ResearchEngineBlock(p, ResearchEngineBlockEntity.Kind.MADNESS),
            p -> p.mapColor(MapColor.METAL).strength(2.0F, 20.0F).sound(SoundType.METAL).noOcclusion());
    public static final DeferredBlock<OrbBlock> METEORB = TWRegistry.block("meteorb",
            p -> new OrbBlock(p, OrbBlock.Kind.METEORB, MeteorbBlockEntity::new),
            p -> p.mapColor(MapColor.STONE).strength(2.0F, 20.0F).sound(SoundType.STONE).noOcclusion());
    public static final DeferredBlock<OrbBlock> MEATY_ORB = TWRegistry.block("meaty_orb",
            p -> new OrbBlock(p, OrbBlock.Kind.MEATY_ORB, MeatyOrbBlockEntity::new),
            p -> p.mapColor(MapColor.COLOR_PINK).strength(0.25F, 2.0F).sound(SoundType.SLIME_BLOCK).noOcclusion());
    public static final DeferredBlock<OreDivinerBlock> ORE_DIVINER = TWRegistry.block("ore_diviner", OreDivinerBlock::new,
            p -> p.mapColor(MapColor.NONE).strength(2.0F, 20.0F).sound(SoundType.GLASS).noOcclusion());
    public static final DeferredBlock<VoidBeaconBlock> VOID_BEACON = TWRegistry.block("void_beacon", VoidBeaconBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(2.0F, 20.0F).sound(SoundType.STONE).noOcclusion());

    public static final DeferredItem<TimewinderItem> TIMEWINDER = TWRegistry.item("timewinder", TimewinderItem::new,
            p -> p.stacksTo(1).rarity(Rarity.RARE)
                    .component(TTDataComponents.RECHARGEABLE.get(), new ChargeProfile(TimewinderItem.CAPACITY, ChargeDisplay.ALWAYS)));
    public static final DeferredItem<StructureDivinerItem> STRUCTURE_DIVINER = TWRegistry.item("structure_diviner", StructureDivinerItem::new,
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final Supplier<BlockEntityType<DimensionalRipperBlockEntity>> DIMENSIONAL_RIPPER_BE = TWRegistry.BLOCK_ENTITIES.register(
            "dimensional_ripper", () -> BlockEntityType.Builder.of(DimensionalRipperBlockEntity::new, DIMENSIONAL_RIPPER.get()).build(null));
    public static final Supplier<BlockEntityType<ResearchEngineBlockEntity>> RESEARCH_ENGINE_BE = TWRegistry.BLOCK_ENTITIES.register(
            "research_engine", () -> BlockEntityType.Builder.of(ResearchEngineBlockEntity::new, INSPIRATION_ENGINE.get(), MADNESS_ENGINE.get()).build(null));
    public static final Supplier<BlockEntityType<MeteorbBlockEntity>> METEORB_BE = TWRegistry.BLOCK_ENTITIES.register(
            "meteorb", () -> BlockEntityType.Builder.of(MeteorbBlockEntity::new, METEORB.get()).build(null));
    public static final Supplier<BlockEntityType<MeatyOrbBlockEntity>> MEATY_ORB_BE = TWRegistry.BLOCK_ENTITIES.register(
            "meaty_orb", () -> BlockEntityType.Builder.of(MeatyOrbBlockEntity::new, MEATY_ORB.get()).build(null));
    public static final Supplier<BlockEntityType<OreDivinerBlockEntity>> ORE_DIVINER_BE = TWRegistry.BLOCK_ENTITIES.register(
            "ore_diviner", () -> BlockEntityType.Builder.of(OreDivinerBlockEntity::new, ORE_DIVINER.get()).build(null));
    public static final Supplier<BlockEntityType<VoidBeaconBlockEntity>> VOID_BEACON_BE = TWRegistry.BLOCK_ENTITIES.register(
            "void_beacon", () -> BlockEntityType.Builder.of(VoidBeaconBlockEntity::new, VOID_BEACON.get()).build(null));

    /** Client-only actions reached from common code (screens); set by the client class. */
    public interface ClientHooks {
        void openOrb(OrbBlock.Kind kind, BlockPos pos);

        void openTimewinder();

        void openStructureDiviner(com.thaumicwonders.data.DeviceRules.Diviner targets);
    }

    private static ClientHooks clientHooks = new ClientHooks() {
        @Override
        public void openOrb(OrbBlock.Kind kind, BlockPos pos) {
        }

        @Override
        public void openTimewinder() {
        }

        @Override
        public void openStructureDiviner(com.thaumicwonders.data.DeviceRules.Diviner targets) {
        }
    };

    public static ClientHooks clientHooks() {
        return clientHooks;
    }

    public static void setClientHooks(ClientHooks hooks) {
        clientHooks = hooks;
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(DevicesContent::registerCapabilities);
        modBus.addListener(DevicesNetwork::register);
        NeoForge.EVENT_BUS.addListener((TagsUpdatedEvent event) -> VoidBeaconBlockEntity.clearRegistry());
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(EssentiaCapabilities.TRANSPORT, DIMENSIONAL_RIPPER_BE.get(), (be, side) -> be);
        event.registerBlockEntity(EssentiaCapabilities.TRANSPORT, RESEARCH_ENGINE_BE.get(), (be, side) -> be);
        event.registerBlockEntity(EssentiaCapabilities.TRANSPORT, METEORB_BE.get(), (be, side) -> be);
        event.registerBlockEntity(EssentiaCapabilities.TRANSPORT, MEATY_ORB_BE.get(), (be, side) -> be);
        event.registerBlockEntity(EssentiaCapabilities.TRANSPORT, VOID_BEACON_BE.get(), (be, side) -> be);
    }

    /** The original's {@code isSurfaceWorld}: a dimension with sky light, no ceiling and a moving sun. */
    public static boolean isSurfaceWorld(Level level) {
        DimensionType type = level.dimensionType();
        return type.hasSkyLight() && !type.hasCeiling() && !type.hasFixedTime();
    }

    /** ENABLED follows "no redstone signal", as the original's enabled blocks. */
    static void updateEnabled(BlockState state, Level level, BlockPos pos, BooleanProperty enabled) {
        if (level.isClientSide()) {
            return;
        }
        boolean shouldBe = !level.hasNeighborSignal(pos);
        if (state.getValue(enabled) != shouldBe) {
            level.setBlock(pos, state.setValue(enabled, shouldBe), Block.UPDATE_ALL);
        }
    }
}
