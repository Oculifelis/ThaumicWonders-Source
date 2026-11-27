package com.thaumicwonders.devices.client;

import com.thaumicwonders.data.DeviceRules.Diviner;
import com.thaumicwonders.devices.DevicesNetwork;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

final class DivinerScreen extends SelectorScreen {
    private static final int PAGE_SIZE = 8;
    private final Diviner rule;
    private final int page;

    DivinerScreen(Diviner rule, int page) {
        super(Component.translatable("item.thaumaturgicwonders.structure_diviner"), "structure_diviner",
                radial().stream().filter(b -> b.id() == PAGE_SIZE || page * PAGE_SIZE + b.id() < rule.targets().size()).toList(),
                id -> PacketDistributor.sendToServer(new DevicesNetwork.StructureDivinerAction(
                        id == PAGE_SIZE ? Optional.empty() : Optional.of(rule.targets().get(page * PAGE_SIZE + id).tag()))));
        this.rule = rule;
        this.page = page;
    }

    @Override
    protected Component label(Button button) {
        return button.id() == PAGE_SIZE ? super.label(button) : rule.targets().get(page * PAGE_SIZE + button.id()).label();
    }

    @Override
    protected void init() {
        super.init();
        if (page > 0) pageButton(-32, -1, "spectatorMenu.previous_page");
        if ((page + 1) * PAGE_SIZE < rule.targets().size()) pageButton(4, 1, "spectatorMenu.next_page");
    }

    private void pageButton(int x, int step, String label) {
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal(step < 0 ? "<" : ">"),
                button -> minecraft.setScreen(new DivinerScreen(rule, page + step)))
                .bounds(width / 2 + x, height / 2 + 50, 28, 18)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(label))).build());
    }
}
