package com.thaumicwonders.gear.client;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

/** The laid-out Flying Carpet: ModelFlyingCarpet's single 28x16x1 slab, textured by the carpet's colour. */
public class FlyingCarpetRenderer extends EntityRenderer<FlyingCarpetEntity, FlyingCarpetRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ThaumicWonders.id("flying_carpet"), "main");
    private static final Map<DyeColor, Identifier> TEXTURES = new EnumMap<>(DyeColor.class);
    private static final Identifier RED = texture(DyeColor.RED);

    static {
        for (DyeColor color : DyeColor.values()) {
            TEXTURES.put(color, texture(color));
        }
    }

    private static Identifier texture(DyeColor color) {
        // The original named light grey "silver".
        String name = color == DyeColor.LIGHT_GRAY ? "silver" : color.getSerializedName();
        return ThaumicWonders.id("textures/entity/flying_carpet_" + name + ".png");
    }

    public static class State extends EntityRenderState {
        public float yRot;
        public Identifier texture = RED;
    }

    public static class Model extends EntityModel<State> {
        public Model(ModelPart root) {
            super(root);
        }

        public static LayerDefinition create() {
            MeshDefinition mesh = new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("carpet_base", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -9.0F, -3.0F, 28.0F, 16.0F, 1.0F),
                    PartPose.offsetAndRotation(0.0F, 3.0F, 1.0F, Mth.HALF_PI, 0.0F, 0.0F));
            return LayerDefinition.create(mesh, 128, 64);
        }
    }

    private final Model model;

    public FlyingCarpetRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new Model(context.bakeLayer(LAYER));
        this.shadowRadius = 0.5F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FlyingCarpetEntity carpet, State state, float partialTick) {
        super.extractRenderState(carpet, state, partialTick);
        state.yRot = carpet.getYRot(partialTick);
        DyeColor color = carpet.getDyeColor();
        state.texture = color == null ? RED : TEXTURES.get(color);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.0F, 0.375F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F - state.yRot));
        pose.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModel(this.model, state, pose, state.texture, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
