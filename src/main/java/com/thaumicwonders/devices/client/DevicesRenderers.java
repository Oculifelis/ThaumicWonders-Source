package com.thaumicwonders.devices.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thaumicwonders.client.EndCube;
import com.thaumicwonders.devices.DimensionalRipperBlock;
import com.thaumicwonders.devices.DimensionalRipperBlockEntity;
import com.thaumicwonders.devices.OreDivinerBlockEntity;
import com.thaumicwonders.devices.VoidBeaconBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/** The devices' block entity renderers. */
final class DevicesRenderers {
    private DevicesRenderers() {
    }

    // ---- Dimensional Ripper: a small end-portal cube floating in front of the emitter while enabled ----

    static final class RipperRenderer implements BlockEntityRenderer<DimensionalRipperBlockEntity> {
        RipperRenderer(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(DimensionalRipperBlockEntity be, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            var block = be.getBlockState();
            if (!block.getValue(DimensionalRipperBlock.ENABLED)) {
                return;
            }
            Direction facing = block.getValue(DimensionalRipperBlock.FACING);
            pose.pushPose();
            pose.translate(0.5F, 0.5F, 0.5F);
            pose.mulPose(facing.getRotation());
            pose.translate(0.0F, 0.375F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            pose.translate(-0.5F, -0.5F, -0.5F);
            EndCube.draw(pose, buffers);
            pose.popPose();
        }
    }

    // ---- Ore Diviner: the searched-for ore turning slowly inside the glass ----

    static final class OreDivinerRenderer implements BlockEntityRenderer<OreDivinerBlockEntity> {
        OreDivinerRenderer(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(OreDivinerBlockEntity be, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            if (!be.active() || be.getLevel() == null) {
                return;
            }
            ItemStack stack = be.searchStack();
            if (stack.isEmpty()) {
                return;
            }
            float angle = (be.getLevel().getGameTime() % 360L + partialTicks) % 360.0F;
            pose.pushPose();
            pose.translate(0.5F, 0.0625F + 0.375F * 0.75F, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(angle));
            pose.scale(0.75F, 0.75F, 0.75F);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers,
                    be.getLevel(), (int) be.getBlockPos().asLong());
            pose.popPose();
        }
    }

    // ---- Void Beacon: a beacon beam in the Alienis colour ----

    static final class VoidBeaconRenderer implements BlockEntityRenderer<VoidBeaconBlockEntity> {
        VoidBeaconRenderer(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(VoidBeaconBlockEntity be, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            int height = be.beamHeight();
            float scale = be.beamScale(partialTicks);
            if (height <= 0 || scale <= 0.0F) {
                return;
            }
            long time = be.getLevel() == null ? 0L : be.getLevel().getGameTime();
            pose.pushPose();
            pose.translate(0.0F, 0.19F, 0.0F);
            BeaconRenderer.renderBeaconBeam(pose, buffers, BeaconRenderer.BEAM_LOCATION, partialTicks, 1.0F, time, 0, height,
                    FastColor.ARGB32.opaque(be.beamColor()), 0.2F * scale, 0.25F * scale);
            pose.popPose();
        }

        @Override
        public boolean shouldRenderOffScreen(VoidBeaconBlockEntity be) {
            return true;
        }

        @Override
        public int getViewDistance() {
            return 256;
        }

        @Override
        public AABB getRenderBoundingBox(VoidBeaconBlockEntity be) {
            var pos = be.getBlockPos();
            return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + Mth.clamp(be.beamHeight() + 1, 1, 4096), pos.getZ() + 1.0);
        }
    }
}
