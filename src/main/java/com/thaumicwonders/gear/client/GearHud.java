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
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The original's HudManager: a hunger gauge for each held Primal Destroyer and a vis gauge while riding a Flying
 * Carpet, stacked down the top-left corner under whichever of Thaumaturge's gauges are showing.
 */
public final class GearHud implements LayeredDraw.Layer {
    private static final ResourceLocation HUD = ThaumicWonders.id("textures/gui/hud.png");
    private static final int GAUGE_HEIGHT = 77;
    private static final int DESTROYER_COLOR = 0xFF800080;
    private static final int CARPET_COLOR = 0xFFFF80FF;

    private final AuraHudOverlay aura = new AuraHudOverlay();
    private final SanityHudOverlay sanity = new SanityHudOverlay();

    @Override
    public void render(GuiGraphics graphics, DeltaTracker delta) {
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

    private static void gauge(GuiGraphics graphics, int y, int value, int max, int color, int frameU) {
        graphics.blit(HUD, 1, y + 1, 152, 0, 20, 78, 256, 256);
        if (value > 0) {
            int gap = (int) ((float) (max - value) / max * 48.0F);
            gap = Math.max(0, Math.min(48, gap));
            RenderSystem.setShaderColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, ((color >>> 24) & 0xFF) / 255.0F);
            graphics.blit(HUD, 7, y + 23 + gap, 200, gap, 8, 48 - gap, 256, 256);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        graphics.blit(HUD, 1, y + 1, frameU, 0, 20, 78, 256, 256);
    }
}
