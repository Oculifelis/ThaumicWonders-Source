package com.thaumicwonders.devices.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thaumicwonders.devices.DimensionalRipperBlock;
import com.thaumicwonders.devices.DimensionalRipperBlockEntity;
import com.thaumicwonders.devices.OreDivinerBlockEntity;
import com.thaumicwonders.devices.VoidBeaconBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The devices' block entity renderers. */
final class DevicesRenderers {
    private DevicesRenderers() {
    }

    // ---- Dimensional Ripper: a small end-portal cube floating in front of the emitter while enabled ----

    static final class RipperState extends BlockEntityRenderState {
        @Nullable Direction facing;
    }

    static final class RipperRenderer implements BlockEntityRenderer<DimensionalRipperBlockEntity, RipperState> {
        RipperRenderer(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public RipperState createRenderState() {
            return new RipperState();
        }

        @Override
        public void extractRenderState(DimensionalRipperBlockEntity be, RipperState state, float partialTicks, Vec3 camera,
                ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
            BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, crumbling);
            var block = be.getBlockState();
            state.facing = block.getValue(DimensionalRipperBlock.ENABLED) ? block.getValue(DimensionalRipperBlock.FACING) : null;
        }

        @Override
        public void submit(RipperState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            if (state.facing == null) {
                return;
            }
            pose.pushPose();
            pose.translate(0.5F, 0.5F, 0.5F);
            pose.mulPose(state.facing.getRotation());
            pose.translate(0.0F, 0.375F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            pose.translate(-0.5F, -0.5F, -0.5F);
            AbstractEndPortalRenderer.submitSpecial(RenderTypes.endPortal(), pose, collector);
            pose.popPose();
        }
    }

    // ---- Ore Diviner: the searched-for ore turning slowly inside the glass ----

    static final class DivinerState extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float angle;
    }

    static final class OreDivinerRenderer implements BlockEntityRenderer<OreDivinerBlockEntity, DivinerState> {
        private final ItemModelResolver items;

        OreDivinerRenderer(BlockEntityRendererProvider.Context context) {
            this.items = context.itemModelResolver();
        }

        @Override
        public DivinerState createRenderState() {
            return new DivinerState();
        }

        @Override
        public void extractRenderState(OreDivinerBlockEntity be, DivinerState state, float partialTicks, Vec3 camera,
                ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
            BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, crumbling);
            state.item.clear();
            if (be.active() && be.getLevel() != null) {
                items.updateForTopItem(state.item, be.searchStack(), ItemDisplayContext.FIXED, be.getLevel(), null, (int) be.getBlockPos().asLong());
                state.angle = (be.getLevel().getGameTime() % 360L + partialTicks) % 360.0F;
            }
        }

        @Override
        public void submit(DivinerState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            if (state.item.isEmpty()) {
                return;
            }
            pose.pushPose();
            pose.translate(0.5F, 0.0625F + 0.375F * 0.75F, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(state.angle));
            pose.scale(0.75F, 0.75F, 0.75F);
            state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    // ---- Void Beacon: a beacon beam in the Alienis colour ----

    static final class BeamState extends BlockEntityRenderState {
        int height;
        int color;
        float scale;
        float animationTime;
        float radiusScale;
    }

    static final class VoidBeaconRenderer implements BlockEntityRenderer<VoidBeaconBlockEntity, BeamState> {
        VoidBeaconRenderer(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public BeamState createRenderState() {
            return new BeamState();
        }

        @Override
        public void extractRenderState(VoidBeaconBlockEntity be, BeamState state, float partialTicks, Vec3 camera,
                ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
            BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, crumbling);
            state.height = be.beamHeight();
            state.color = ARGB.opaque(be.beamColor());
            state.scale = be.beamScale(partialTicks);
            long time = be.getLevel() == null ? 0L : be.getLevel().getGameTime();
            state.animationTime = Math.floorMod(time, 40L) + partialTicks;
            double dx = camera.x - (be.getBlockPos().getX() + 0.5);
            double dz = camera.z - (be.getBlockPos().getZ() + 0.5);
            state.radiusScale = Math.max(1.0F, (float) Math.sqrt(dx * dx + dz * dz) / 96.0F);
        }

        @Override
        public void submit(BeamState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            if (state.height <= 0 || state.scale <= 0.0F) {
                return;
            }
            pose.pushPose();
            pose.translate(0.0F, 0.19F, 0.0F);
            BeaconRenderer.submitBeaconBeam(pose, collector, BeaconRenderer.BEAM_LOCATION, state.radiusScale, state.animationTime, 0, state.height,
                    state.color, 0.2F * state.scale, 0.25F * state.scale);
            pose.popPose();
        }

        @Override
        public boolean shouldRenderOffScreen() {
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
