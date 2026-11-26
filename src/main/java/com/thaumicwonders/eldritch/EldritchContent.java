package com.thaumicwonders.eldritch;

import static com.thaumicwonders.TWRegistry.BLOCK_ENTITIES;
import static com.thaumicwonders.TWRegistry.COMPONENTS;
import static com.thaumicwonders.TWRegistry.ENTITIES;

import com.thaumicwonders.TWRegistry;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The eldritch group (docs/COVERAGE.md rows 36-37): Portal Anchor, Portal Generator and the Void Portal it opens, and
 * the Avatar of Corruption with its Flux Fireball.
 */
public final class EldritchContent {
    private EldritchContent() {
    }

    // The original's BlockTileTW (IRON, METAL): hardness 2, resistance 20 (modern 12).
    private static Block.Properties device(Block.Properties p) {
        return p.mapColor(MapColor.COLOR_PURPLE).strength(2.0F, 12.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    // ---- components ---------------------------------------------------------------------------------------------
    /** Where a linked Portal Generator item leads (the original's NBT linkX/linkY/linkZ/linkDim). */
    public static final Supplier<DataComponentType<GlobalPos>> PORTAL_LINK = COMPONENTS.registerComponentType("portal_link",
            b -> b.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

    // ---- blocks -------------------------------------------------------------------------------------------------
    /** A marker: a portal only works when this block stands at its link. */
    public static final DeferredBlock<Block> PORTAL_ANCHOR = TWRegistry.block("portal_anchor", Block::new, p -> device(p).noOcclusion());
    public static final DeferredBlock<PortalGeneratorBlock> PORTAL_GENERATOR = TWRegistry.block("portal_generator", PortalGeneratorBlock::new,
            EldritchContent::device, PortalGeneratorItem::new, p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PortalGeneratorBlockEntity>> PORTAL_GENERATOR_BE =
            BLOCK_ENTITIES.register("portal_generator", () -> new BlockEntityType<>(PortalGeneratorBlockEntity::new, PORTAL_GENERATOR.get()));

    // ---- entities -----------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<VoidPortal>> VOID_PORTAL = ENTITIES.registerEntityType("void_portal",
            VoidPortal::new, MobCategory.MISC, b -> b.sized(1.5F, 3.0F).fireImmune().noLootTable().clientTrackingRange(4).updateInterval(20));
    public static final DeferredHolder<EntityType<?>, EntityType<CorruptionAvatar>> CORRUPTION_AVATAR = ENTITIES.registerEntityType(
            "corruption_avatar", CorruptionAvatar::new, MobCategory.MONSTER, b -> b.sized(0.75F, 2.25F).eyeHeight(2.0F).clientTrackingRange(4));
    public static final DeferredHolder<EntityType<?>, EntityType<FluxFireball>> FLUX_FIREBALL = ENTITIES.registerEntityType("flux_fireball",
            FluxFireball::new, MobCategory.MISC, b -> b.sized(1.0F, 1.0F).noLootTable().clientTrackingRange(4).updateInterval(1));

    public static final DeferredItem<SpawnEggItem> CORRUPTION_AVATAR_SPAWN_EGG = TWRegistry.item("corruption_avatar_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(CORRUPTION_AVATAR.get()));

    public static void init(IEventBus modBus) {
        modBus.addListener(EldritchContent::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(CORRUPTION_AVATAR.get(), CorruptionAvatar.createAttributes().build());
    }

    /** The original's PacketLocalizedMessage: a dark purple action-bar message to every player within range. */
    static void tellNearby(ServerLevel level, BlockPos pos, double range, String key) {
        Component message = Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos.getCenter()) <= range * range) {
                player.sendOverlayMessage(message);
            }
        }
    }
}
