package com.thaumicwonders.eldritch.client;

import com.thaumicwonders.ThaumicWonders;
import com.thaumicwonders.eldritch.EldritchContent;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only parts of the eldritch group: the Void Portal, Flux Fireball and Avatar of Corruption renderers. */
public final class EldritchClient {
    /** The original's ModelBiped on a 64x32 texture (left limbs mirror the right ones). */
    public static final ModelLayerLocation AVATAR_LAYER = new ModelLayerLocation(ThaumicWonders.id("corruption_avatar"), "main");

    private EldritchClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(EldritchClient::layers);
        modBus.addListener(EldritchClient::renderers);
    }

    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(AVATAR_LAYER, EldritchClient::avatarLayer);
    }

    static LayerDefinition avatarLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EldritchContent.VOID_PORTAL.get(), EldritchRenderers.VoidPortalRenderer::new);
        event.registerEntityRenderer(EldritchContent.FLUX_FIREBALL.get(), EldritchRenderers.FluxFireballRenderer::new);
        event.registerEntityRenderer(EldritchContent.CORRUPTION_AVATAR.get(), EldritchRenderers.AvatarRenderer::new);
    }
}
