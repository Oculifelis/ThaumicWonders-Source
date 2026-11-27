package com.thaumicwonders.eldritch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thaumicwonders.ThaumicWonders;
import com.thaumicwonders.eldritch.CorruptionAvatar;
import com.thaumicwonders.eldritch.FluxFireball;
import com.thaumicwonders.eldritch.VoidPortal;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/** The eldritch group's entity renderers. */
final class EldritchRenderers {
    private EldritchRenderers() {
    }

    // ---- Void Portal: Thaumaturge's animated eldritch portal on a camera-facing quad, wobbling when unstable ----

    static final class VoidPortalRenderer extends EntityRenderer<VoidPortal> {
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumaturge", "textures/misc/eldritch_portal.png");
        /** Thaumaturge's strip has 32 square frames (the original's TC6 strip had 16); one frame per 50 ms as the original. */
        private static final int FRAMES = 32;

        VoidPortalRenderer(EntityRendererProvider.Context context) {
            super(context);
            this.shadowRadius = 0.0F;
        }

        @Override
        public ResourceLocation getTextureLocation(VoidPortal portal) {
            return TEXTURE;
        }

        @Override
        protected int getBlockLightLevel(VoidPortal portal, BlockPos pos) {
            return 15;
        }

        @Override
        public void render(VoidPortal portal, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            float age = portal.tickCount + partialTick;
            float stability = portal.generatorStability();
            float scaley = 1.4F;
            float scale = Math.min(50.0F, age) / 50.0F * 1.25F;
            float m = (1.0F - Mth.clamp((stability + 100.0F) / 100.0F, 0.0F, 1.0F)) / 3.0F;
            float bobY = Mth.sin(age / (5.0F - 12.0F * m)) * m + m;
            float bobXZ = Mth.sin(age / (6.0F - 15.0F * m)) * m + m;
            float alpha = Mth.clamp(1.0F - bobY, 0.0F, 1.0F);
            scaley -= bobY / 4.0F;
            scale -= bobXZ / 3.0F;
            int frame = FRAMES - 1 - (int) ((System.nanoTime() / 50_000_000L) % FRAMES);
            float u0 = frame / (float) FRAMES;
            float u1 = u0 + 1.0F / FRAMES;
            int color = FastColor.ARGB32.color(Math.round(alpha * 255.0F), 255, 255, 255);
            pose.pushPose();
            pose.translate(0.0F, portal.getBbHeight() / 2.0F, 0.0F);
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            PoseStack.Pose p = pose.last();
            VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
            vertex(buffer, p, -scale, -scaley, u1, 1.0F, color);
            vertex(buffer, p, scale, -scaley, u0, 1.0F, color);
            vertex(buffer, p, scale, scaley, u0, 0.0F, color);
            vertex(buffer, p, -scale, scaley, u1, 0.0F, color);
            pose.popPose();
            super.render(portal, yaw, partialTick, pose, buffers, light);
        }

        private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float u, float v, int color) {
            buffer.addVertex(pose, x, y, 0.0F).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
        }
    }

    // ---- Flux Fireball: the vanilla dragon fireball sprite, camera-facing, scale 2 ----

    static final class FluxFireballRenderer extends EntityRenderer<FluxFireball> {
        private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/enderdragon/dragon_fireball.png");

        FluxFireballRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public ResourceLocation getTextureLocation(FluxFireball fireball) {
            return TEXTURE;
        }

        @Override
        protected int getBlockLightLevel(FluxFireball fireball, BlockPos pos) {
            return 15;
        }

        @Override
        public void render(FluxFireball fireball, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            pose.pushPose();
            pose.scale(2.0F, 2.0F, 2.0F);
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            PoseStack.Pose p = pose.last();
            VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
            vertex(buffer, p, light, -0.5F, -0.25F, 0.0F, 1.0F);
            vertex(buffer, p, light, 0.5F, -0.25F, 1.0F, 1.0F);
            vertex(buffer, p, light, 0.5F, 0.75F, 1.0F, 0.0F);
            vertex(buffer, p, light, -0.5F, 0.75F, 0.0F, 0.0F);
            pose.popPose();
            super.render(fireball, yaw, partialTick, pose, buffers, light);
        }

        private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, int light, float x, float y, float u, float v) {
            buffer.addVertex(pose, x, y, 0.0F).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(pose, 0.0F, 1.0F, 0.0F);
        }
    }

    // ---- Avatar of Corruption: the biped model on the original's texture, scaled 1.15 ----

    static final class AvatarRenderer extends HumanoidMobRenderer<CorruptionAvatar, HumanoidModel<CorruptionAvatar>> {
        private static final ResourceLocation TEXTURE = ThaumicWonders.id("textures/entity/monsters/corruption_avatar.png");

        AvatarRenderer(EntityRendererProvider.Context context) {
            super(context, new HumanoidModel<>(context.bakeLayer(EldritchClient.AVATAR_LAYER)), 0.5F);
        }

        @Override
        public ResourceLocation getTextureLocation(CorruptionAvatar avatar) {
            return TEXTURE;
        }

        @Override
        protected void scale(CorruptionAvatar avatar, PoseStack pose, float partialTick) {
            pose.scale(1.15F, 1.15F, 1.15F);
        }
    }
}
