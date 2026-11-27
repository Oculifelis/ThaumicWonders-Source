package com.thaumicwonders.devices.client;

import com.thaumicwonders.devices.DevicesContent;
import com.thaumicwonders.devices.DevicesNetwork;
import com.thaumicwonders.devices.OrbBlock;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client-only parts of the devices group: renderers and the selector screens. */
public final class DevicesClient {
    private DevicesClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(DevicesClient::registerRenderers);
        DevicesContent.setClientHooks(new DevicesContent.ClientHooks() {
            @Override
            public void openOrb(OrbBlock.Kind kind, BlockPos pos) {
                SelectorScreen screen = switch (kind) {
                    case METEORB -> new SelectorScreen(Component.translatable("block.thaumaturgicwonders.meteorb"), "meteorb",
                            List.of(new SelectorScreen.Button(2, 24, 0, 144, 120), new SelectorScreen.Button(1, 0, 0, 120, 120),
                                    new SelectorScreen.Button(0, -24, 0, 96, 120)),
                            id -> PacketDistributor.sendToServer(new DevicesNetwork.OrbAction(pos, id)));
                    case MEATY_ORB -> new SelectorScreen(Component.translatable("block.thaumaturgicwonders.meaty_orb"), "meaty_orb",
                            List.of(new SelectorScreen.Button(0, 0, 0, 120, 120)),
                            id -> PacketDistributor.sendToServer(new DevicesNetwork.OrbAction(pos, id)));
                };
                Minecraft.getInstance().setScreen(screen);
            }

            @Override
            public void openTimewinder() {
                Minecraft.getInstance().setScreen(new SelectorScreen(Component.translatable("item.thaumaturgicwonders.timewinder"), "timewinder",
                        SelectorScreen.radial(), id -> PacketDistributor.sendToServer(new DevicesNetwork.TimewinderAction(id))));
            }

            @Override
            public void openStructureDiviner(com.thaumicwonders.data.DeviceRules.Diviner rule) {
                Minecraft.getInstance().setScreen(new DivinerScreen(rule, 0));
            }
        });
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(DevicesContent.DIMENSIONAL_RIPPER_BE.get(), DevicesRenderers.RipperRenderer::new);
        event.registerBlockEntityRenderer(DevicesContent.ORE_DIVINER_BE.get(), DevicesRenderers.OreDivinerRenderer::new);
        event.registerBlockEntityRenderer(DevicesContent.VOID_BEACON_BE.get(), DevicesRenderers.VoidBeaconRenderer::new);
    }
}
