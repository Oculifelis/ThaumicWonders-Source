package com.thaumicwonders.devices.client;

import com.thaumicwonders.ThaumicWonders;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The original's selector screens (Timewinder, Structure Diviner, Meteorb, Meaty Orb): a 256x256 picture in the middle
 * of the screen and 16x16 buttons cut from a second texture, bright while hovered with the label to their right.
 * Pressing one sends its id and closes the screen.
 */
public class SelectorScreen extends Screen {
    record Button(int id, int dx, int dy, int u, int v) {
    }

    private final ResourceLocation background;
    private final ResourceLocation buttons;
    private final String labelPrefix;
    private final List<Button> layout;
    private final IntConsumer onPress;

    SelectorScreen(Component title, String name, List<Button> layout, IntConsumer onPress) {
        super(title);
        this.background = ThaumicWonders.id("textures/gui/gui_" + name + "_background.png");
        this.buttons = ThaumicWonders.id("textures/gui/gui_" + name + ".png");
        this.labelPrefix = "thaumaturgicwonders.gui." + name + ".";
        this.layout = new ArrayList<>(layout);
        this.onPress = onPress;
    }

    /** The eight-around-one layout of the Timewinder and the Structure Diviner (the centre is id 8). */
    static List<Button> radial() {
        return List.of(new Button(0, 0, -32, 120, 88), new Button(1, 23, -23, 143, 97), new Button(2, 32, 0, 152, 120),
                new Button(3, 23, 23, 143, 143), new Button(4, 0, 32, 120, 152), new Button(8, 0, 0, 120, 120),
                new Button(5, -23, 23, 97, 143), new Button(6, -32, 0, 88, 120), new Button(7, -23, -23, 97, 97));
    }

    protected Component label(Button button) {
        return Component.translatable(labelPrefix + button.id());
    }

    private int baseX() {
        return (width - 16) / 2;
    }

    private int baseY() {
        return (height - 16) / 2;
    }

    private boolean over(Button b, double mx, double my) {
        int x = baseX() + b.dx();
        int y = baseY() + b.dy();
        return mx >= x && my >= y && mx < x + 16 && my < y + 16;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        super.render(g, mouseX, mouseY, partialTicks);
        g.blit(background, (width - 256) / 2, (height - 256) / 2, 0.0F, 0.0F, 256, 256, 256, 256);
        Button hovered = null;
        for (Button b : layout) {
            boolean on = over(b, mouseX, mouseY);
            int x = baseX() + b.dx();
            int y = baseY() + b.dy();
            float shade = on ? 1.0F : 0.8F;
            g.setColor(shade, shade, shade, 1.0F);
            g.blit(buttons, x, y, b.u(), b.v(), 16, 16, 256, 256);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            if (on) {
                hovered = b;
            }
        }
        if (hovered != null) {
            g.drawString(font, label(hovered), baseX() + hovered.dx() + 19, baseY() + hovered.dy() + 4, 0xFFFFFFFF);
        }

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Last drawn is on top, so test in reverse.
            for (int i = layout.size() - 1; i >= 0; i--) {
                Button b = layout.get(i);
                if (over(b, mouseX, mouseY)) {
                    onPress.accept(b.id());
                    onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
