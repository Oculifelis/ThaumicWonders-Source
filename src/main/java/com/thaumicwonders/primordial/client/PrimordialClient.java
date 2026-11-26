package com.thaumicwonders.primordial.client;

import com.thaumicwonders.primordial.PrimordialContent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only parts of the primordial group: the two socket screens. */
public final class PrimordialClient {
    private PrimordialClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterMenuScreensEvent event) -> {
            event.register(PrimordialContent.CATALYZATION_MENU.get(), SocketScreen::catalyzation);
            event.register(PrimordialContent.ACCELERATOR_MENU.get(), SocketScreen::accelerator);
        });
    }
}
