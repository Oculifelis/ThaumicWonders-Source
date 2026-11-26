package com.thaumicwonders.alchemy.client;

import com.leclowndu93150.thaumaturge.client.render.blockentity.JarRenderer;
import com.thaumicwonders.ThaumicWonders;
import com.thaumicwonders.alchemy.AlchemyContent;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;

/** Client-only parts of the alchemy group: primed hexamite, the creative jar's contents, liquid quicksilver. */
public final class AlchemyClient {
    private AlchemyClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(AlchemyClient::renderers);
        modBus.addListener(AlchemyClient::fluidModels);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AlchemyContent.HEXAMITE_PRIMED.get(), TntRenderer::new);
        event.registerBlockEntityRenderer(AlchemyContent.CREATIVE_ESSENTIA_JAR_BE.get(), JarRenderer::new);
    }

    private static void fluidModels(RegisterFluidModelsEvent event) {
        // Quicksilver-coloured still and flow textures, drawn untinted; the still one repeats every 4 blocks.
        event.register(FluidTextures.model(Identifier.fromNamespaceAndPath(ThaumicWonders.MODID, "block/fluid_quicksilver_still"),
                        Identifier.fromNamespaceAndPath(ThaumicWonders.MODID, "block/fluid_quicksilver_flow")),
                AlchemyContent.QUICKSILVER.get(), AlchemyContent.FLOWING_QUICKSILVER.get());
    }
}
