package com.thaumicwonders.client;

import net.neoforged.bus.api.IEventBus;

/** Client-only setup: each content group keeps its own client class. */
public final class TWClient {
    private TWClient() {
    }

    public static void init(IEventBus modBus) {
        com.thaumicwonders.alchemy.client.AlchemyClient.init(modBus);
        com.thaumicwonders.devices.client.DevicesClient.init(modBus);
        com.thaumicwonders.primordial.client.PrimordialClient.init(modBus);
        com.thaumicwonders.gear.client.GearClient.init(modBus);
        com.thaumicwonders.eldritch.client.EldritchClient.init(modBus);
    }
}
