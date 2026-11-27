package com.thaumicwonders.client;

import com.thaumicwonders.ThaumicWonders;
import com.thaumicwonders.alchemy.AlchemyContent;
import com.thaumicwonders.devices.DevicesContent;
import com.thaumicwonders.gear.GearContent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Item model predicates for 1.21.1, which has no item model definitions: the structure diviner's needle, the timewinder's
 * dial, the flying carpet's dye and the flux tanks' charge, each read by overrides in models/item.
 */
public final class TWItemProperties {
    private TWItemProperties() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(TWItemProperties::clientSetup);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(DevicesContent.STRUCTURE_DIVINER.get(), ThaumicWonders.id("angle"),
                    new CompassItemPropertyFunction((level, stack, entity) -> {
                        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
                        return tracker == null ? null : tracker.target().orElse(null);
                    }));
            ItemProperties.register(DevicesContent.TIMEWINDER.get(), ThaumicWonders.id("time"), TWItemProperties::time);
            ItemProperties.register(GearContent.FLYING_CARPET.get(), ThaumicWonders.id("dye"), (stack, level, living, seed) -> {
                DyeColor dye = stack.get(GearContent.DYE.get());
                return dye == null ? -1.0F : dye.getId() / 16.0F;
            });
            ClampedItemPropertyFunction charge = (stack, level, living, seed) -> charge(stack) / 10.0F;
            ItemProperties.register(AlchemyContent.FLUX_CAPACITOR.get().asItem(), ThaumicWonders.id("charge"), charge);
            ItemProperties.register(AlchemyContent.FLUX_DISTILLER.get().asItem(), ThaumicWonders.id("charge"), charge);
        });
    }

    /** Time of day in the overworld, a spinning dial anywhere else. */
    private static float time(ItemStack stack, @Nullable ClientLevel clientLevel, @Nullable LivingEntity living, int seed) {
        Entity holder = living != null ? living : stack.getEntityRepresentation();
        Level level = clientLevel != null ? clientLevel : holder != null ? holder.level() : null;
        if (level == null) {
            return 0.0F;
        }
        if (level.dimension() == Level.OVERWORLD) {
            return level.getTimeOfDay(1.0F);
        }
        return (float) ((level.getGameTime() * 0.37 + seed) % 64) / 64.0F;
    }

    private static int charge(ItemStack stack) {
        BlockItemStateProperties props = stack.get(DataComponents.BLOCK_STATE);
        if (props == null) {
            return 0;
        }
        String value = props.properties().get("charge");
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
