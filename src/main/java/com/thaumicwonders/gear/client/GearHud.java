package com.thaumicwonders.gear.client;

import com.leclowndu93150.thaumaturge.client.hud.AuraHudOverlay;
import com.leclowndu93150.thaumaturge.client.hud.CasterHudOverlay;
import com.leclowndu93150.thaumaturge.client.hud.SanityHudOverlay;
import com.thaumicwonders.ThaumicWonders;
import com.thaumicwonders.gear.FlyingCarpetEntity;
import com.thaumicwonders.gear.FlyingCarpetItem;
import com.thaumicwonders.gear.GearContent;
import com.thaumicwonders.gear.PrimalDestroyerItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.GuiLayer;

/**
 * The original's HudManager: a hunger gauge for each held Primal Destroyer and a vis gauge while riding a Flying
 * Carpet, stacked down the top-left corner under whichever of Thaumaturge's gauges are showing.
 */
public final class GearHud implements GuiLayer {
    private static final Identifier HUD = ThaumicWonders.id("textures/gui/hud.png");
    private static final int GAUGE_HEIGHT = 77;
    private static final int DESTROYER_COLOR = 0xFF800080;
    private static final int CARPET_COLOR = 0xFFFF80FF;

    private final AuraHudOverlay aura = new AuraHudOverlay();
    private final SanityHudOverlay sanity = new SanityHudOverlay();

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || mc.screen != null) {
            return;
        }
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean mainDestroyer = main.is(GearContent.PRIMAL_DESTROYER.get());
        boolean offDestroyer = off.is(GearContent.PRIMAL_DESTROYER.get());
        FlyingCarpetEntity carpet = player.getVehicle() instanceof FlyingCarpetEntity c ? c : null;
        if (!mainDestroyer && !offDestroyer && carpet == null) {
            return;
        }
        int y = this.thaumaturgeOffset(mc, player);
        if (mainDestroyer) {
            gauge(graphics, y, PrimalDestroyerItem.hunger(main), destroyerLimit(main), DESTROYER_COLOR, 128);
            y += GAUGE_HEIGHT;
        }
        if (offDestroyer) {
            gauge(graphics, y, PrimalDestroyerItem.hunger(off), destroyerLimit(off), DESTROYER_COLOR, 128);
            y += GAUGE_HEIGHT;
        }
        if (carpet != null) {
            gauge(graphics, y, carpet.getVisCharge(), FlyingCarpetItem.CAPACITY, CARPET_COLOR, 176);
        }
    }

    /** Where the original moved down for the caster dial, thaumometer and sanity checker, this follows Thaumaturge's own gauges. */
    private int thaumaturgeOffset(Minecraft mc, LocalPlayer player) {
        int y = 0;
        try {
            if (CasterHudOverlay.isVisible(mc)) {
                y += CasterHudOverlay.STACK_HEIGHT;
            }
            if (this.aura.visible(mc, player)) {
                y += this.aura.height();
            }
            if (this.sanity.visible(mc, player)) {
                y += this.sanity.height();
            }
        } catch (RuntimeException ignored) {
            // A layout change in Thaumaturge only moves our gauges; never break the HUD over it.
        }
        return y;
    }

    private static int destroyerLimit(ItemStack stack) {
        var profile = com.thaumicwonders.data.DestroyerProfile.of(stack);
        return profile == null ? 1 : profile.hungerLimit();
    }

    private static void gauge(GuiGraphicsExtractor graphics, int y, int value, int max, int color, int frameU) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, HUD, 1, y + 1, 152, 0, 20, 78, 256, 256);
        if (value > 0) {
            int gap = (int) ((float) (max - value) / max * 48.0F);
            gap = Math.max(0, Math.min(48, gap));
            graphics.blit(RenderPipelines.GUI_TEXTURED, HUD, 7, y + 23 + gap, 200, gap, 8, 48 - gap, 256, 256, color);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, HUD, 1, y + 1, frameU, 0, 20, 78, 256, 256);
    }
}
