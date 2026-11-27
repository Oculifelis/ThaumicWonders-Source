package com.thaumicwonders.primordial.client;

import com.thaumicwonders.ThaumicWonders;
import com.thaumicwonders.primordial.SocketMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Catalyzation Chamber's and the Primordial Accelerator's screens: the original's background picture only (the
 * socket frame and the inventory panel are both on it), no labels. The original drew it 175 by 232, cutting off the
 * panel's last column and leaving the window too high; this draws the art's real 176 by 166.
 */
public class SocketScreen extends AbstractContainerScreen<SocketMenu> {
    private final ResourceLocation texture;

    private SocketScreen(SocketMenu menu, Inventory inventory, Component title, String name) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.texture = ThaumicWonders.id("textures/gui/gui_" + name + ".png");
    }

    static SocketScreen catalyzation(SocketMenu menu, Inventory inventory, Component title) {
        return new SocketScreen(menu, inventory, title, "catalyzation_chamber");
    }

    static SocketScreen accelerator(SocketMenu menu, Inventory inventory, Component title) {
        return new SocketScreen(menu, inventory, title, "primordial_accelerator");
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        super.render(g, mouseX, mouseY, partialTicks);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTicks, int mouseX, int mouseY) {
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    }
}
