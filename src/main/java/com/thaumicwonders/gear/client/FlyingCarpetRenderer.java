package com.thaumicwonders.gear.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.thaumicwonders.ThaumicWonders;
import com.thaumicwonders.gear.FlyingCarpetEntity;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

/** The laid-out Flying Carpet: ModelFlyingCarpet's single 28x16x1 slab, textured by the carpet's colour. */
public class FlyingCarpetRenderer extends EntityRenderer<FlyingCarpetEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ThaumicWonders.id("flying_carpet"), "main");
    private static final Map<DyeColor, ResourceLocation> TEXTURES = new EnumMap<>(DyeColor.class);
    private static final ResourceLocation RED = texture(DyeColor.RED);

    static {
        for (DyeColor color : DyeColor.values()) {
            TEXTURES.put(color, texture(color));
        }
    }

    private static ResourceLocation texture(DyeColor color) {
        // The original named light grey "silver".
        String name = color == DyeColor.LIGHT_GRAY ? "silver" : color.getSerializedName();
        return ThaumicWonders.id("textures/entity/flying_carpet_" + name + ".png");
    }

    public static class Model extends EntityModel<FlyingCarpetEntity> {
        private final ModelPart root;

        public Model(ModelPart root) {
            this.root = root;
        }

        public static LayerDefinition create() {
            MeshDefinition mesh = new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("carpet_base", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -9.0F, -3.0F, 28.0F, 16.0F, 1.0F),
                    PartPose.offsetAndRotation(0.0F, 3.0F, 1.0F, Mth.HALF_PI, 0.0F, 0.0F));
            return LayerDefinition.create(mesh, 128, 64);
        }

        @Override
        public void setupAnim(FlyingCarpetEntity carpet, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, int color) {
            this.root.render(pose, consumer, light, overlay, color);
        }
    }

    private final Model model;

    public FlyingCarpetRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new Model(context.bakeLayer(LAYER));
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(FlyingCarpetEntity carpet, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.0F, 0.375F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F - Mth.rotLerp(partialTick, carpet.yRotO, carpet.getYRot())));
        pose.scale(-1.0F, -1.0F, 1.0F);
        this.model.renderToBuffer(pose, buffers.getBuffer(this.model.renderType(this.getTextureLocation(carpet))), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(carpet, entityYaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(FlyingCarpetEntity carpet) {
        DyeColor color = carpet.getDyeColor();
        return color == null ? RED : TEXTURES.get(color);
    }
}
