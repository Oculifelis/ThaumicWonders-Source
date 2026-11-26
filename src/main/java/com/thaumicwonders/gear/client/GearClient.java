package com.thaumicwonders.gear.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.thaumicwonders.ThaumicWonders;
import com.thaumicwonders.gear.FlyingCarpetEntity;
import com.thaumicwonders.gear.GearContent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Client-only parts of the gear group: carpet and arrow renderers, the Void Fortress armour model, the carpet keys,
 * the Bow of Bone's pull, the HUD gauges and the goggles' Curios rendering.
 */
public final class GearClient {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(ThaumicWonders.id("main"));
    public static final KeyMapping CARPET_FORWARD = new KeyMapping("key.thaumaturgicwonders.carpet_forward", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_W, CATEGORY);
    public static final KeyMapping CARPET_BACKWARD = new KeyMapping("key.thaumaturgicwonders.carpet_backward", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_S, CATEGORY);

    private static final ModelLayerLocation VF_HEAD = new ModelLayerLocation(ThaumicWonders.id("void_fortress_armor"), "head");
    private static final ModelLayerLocation VF_CHEST = new ModelLayerLocation(ThaumicWonders.id("void_fortress_armor"), "chest");
    private static final ModelLayerLocation VF_LEGS = new ModelLayerLocation(ThaumicWonders.id("void_fortress_armor"), "legs");
    private static final Identifier VF_TEXTURE = ThaumicWonders.id("textures/entity/armor/void_fortress_armor.png");
    private static final Identifier GOGGLES_TEXTURE = ThaumicWonders.id("textures/entity/armor/night_vision_goggles.png");

    private GearClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(GearClient::layers);
        modBus.addListener(GearClient::renderers);
        modBus.addListener(GearClient::keys);
        modBus.addListener(GearClient::itemProperties);
        modBus.addListener(GearClient::guiLayers);
        modBus.addListener(GearClient::clientExtensions);
        modBus.addListener(GearClient::clientSetup);
        NeoForge.EVENT_BUS.addListener(GearClient::clientTick);
    }

    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FlyingCarpetRenderer.LAYER, FlyingCarpetRenderer.Model::create);
        event.registerLayerDefinition(VF_HEAD, () -> VoidFortressArmorModel.create(VoidFortressArmorModel.Piece.HEAD));
        event.registerLayerDefinition(VF_CHEST, () -> VoidFortressArmorModel.create(VoidFortressArmorModel.Piece.CHEST));
        event.registerLayerDefinition(VF_LEGS, () -> VoidFortressArmorModel.create(VoidFortressArmorModel.Piece.LEGS));
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GearContent.FLYING_CARPET_ENTITY.get(), FlyingCarpetRenderer::new);
        event.registerEntityRenderer(GearContent.PRIMAL_ARROW_ENTITY.get(), PrimalArrowRenderer::new);
    }

    private static void keys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(CARPET_FORWARD);
        event.register(CARPET_BACKWARD);
    }

    private static void itemProperties(RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(ThaumicWonders.id("pull"), BowPullProperty.CODEC);
    }

    private static void guiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, ThaumicWonders.id("gear_hud"), new GearHud());
    }

    private static void clientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new VoidFortressExtension(), GearContent.VOID_FORTRESS_HELM.get(), GearContent.VOID_FORTRESS_CHEST.get(),
                GearContent.VOID_FORTRESS_LEGS.get());
        event.registerItem(new IClientItemExtensions() {
            @Override
            public Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type, EquipmentClientInfo.Layer layer, Identifier fallback) {
                return GOGGLES_TEXTURE;
            }
        }, GearContent.NIGHT_VISION_GOGGLES.get());
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        ICurioRenderer.register(GearContent.NIGHT_VISION_GOGGLES.get(), GogglesCurioRenderer::new);
    }

    /** Feeds the carpet keys to the carpet the player is piloting (the carpet moves on the pilot's side). */
    private static void clientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() instanceof FlyingCarpetEntity carpet) {
            carpet.updateInputs(CARPET_FORWARD.isDown(), CARPET_BACKWARD.isDown());
        }
    }

    private static final class VoidFortressExtension implements IClientItemExtensions {
        private HumanoidModel<HumanoidRenderState> head;
        private HumanoidModel<HumanoidRenderState> chest;
        private HumanoidModel<HumanoidRenderState> legs;

        @Override
        public Model getHumanoidArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
            if (stack.is(GearContent.VOID_FORTRESS_HELM.get())) {
                if (this.head == null) {
                    this.head = bake(VF_HEAD);
                }
                return this.head;
            }
            if (stack.is(GearContent.VOID_FORTRESS_LEGS.get())) {
                if (this.legs == null) {
                    this.legs = bake(VF_LEGS);
                }
                return this.legs;
            }
            if (this.chest == null) {
                this.chest = bake(VF_CHEST);
            }
            return this.chest;
        }

        @Override
        public Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type, EquipmentClientInfo.Layer layer, Identifier fallback) {
            return VF_TEXTURE;
        }

        private static HumanoidModel<HumanoidRenderState> bake(ModelLayerLocation layer) {
            return new VoidFortressArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(layer));
        }
    }
}
