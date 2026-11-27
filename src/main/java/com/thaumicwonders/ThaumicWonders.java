package com.thaumicwonders;

import com.mojang.logging.LogUtils;
import com.thaumicwonders.alchemy.AlchemyContent;
import com.thaumicwonders.devices.DevicesContent;
import com.thaumicwonders.eldritch.EldritchContent;
import com.thaumicwonders.gear.GearContent;
import com.thaumicwonders.primordial.PrimordialContent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Thaumic Wonders (daedalus4096, Thaumcraft 6), ported to Thaumaturge on NeoForge 26.1.2. Content is split into five
 * groups, each a package with its own {@code *Content} class holding its registrations and listeners (see
 * docs/COVERAGE.md for which feature lives where).
 */
@Mod(ThaumicWonders.MODID)
public final class ThaumicWonders {
    public static final String MODID = "thaumaturgicwonders";
    public static final Logger LOG = LogUtils.getLogger();

    public ThaumicWonders(IEventBus modBus, ModContainer container, Dist dist) {
        // The groups add to the shared registers from their static fields, so they load before the registers attach.
        AlchemyContent.init(modBus);
        DevicesContent.init(modBus);
        PrimordialContent.init(modBus);
        GearContent.init(modBus);
        EldritchContent.init(modBus);
        modBus.addListener(com.thaumicwonders.data.DeviceRules::register);
        TWRegistry.register(modBus);
        if (dist == Dist.CLIENT) {
            com.thaumicwonders.client.TWClient.init(modBus);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
