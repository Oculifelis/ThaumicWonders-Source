package com.thaumicwonders.primordial.client;

import com.thaumicwonders.ThaumicWonders;
import com.thaumicwonders.primordial.SocketMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Catalyzation Chamber's and the Primordial Accelerator's screens: the original's background picture only (the
 * socket frame and the inventory panel are both on it), no labels. The original drew it 175 by 232, cutting off the
 * panel's last column and leaving the window too high; this draws the art's real 176 by 166.
 */
public class SocketScreen extends AbstractContainerScreen<SocketMenu> {
    private final Identifier texture;

    private SocketScreen(SocketMenu menu, Inventory inventory, Component title, String name) {
        super(menu, inventory, title, 176, 166);
        this.texture = ThaumicWonders.id("textures/gui/gui_" + name + ".png");
    }

    static SocketScreen catalyzation(SocketMenu menu, Inventory inventory, Component title) {
        return new SocketScreen(menu, inventory, title, "catalyzation_chamber");
    }

    static SocketScreen accelerator(SocketMenu menu, Inventory inventory, Component title) {
        return new SocketScreen(menu, inventory, title, "primordial_accelerator");
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(g, mouseX, mouseY, partialTicks);
        g.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
    }
}
