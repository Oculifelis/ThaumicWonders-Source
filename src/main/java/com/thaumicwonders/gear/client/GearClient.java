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
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only parts of the gear group: carpet and arrow renderers, the Void Fortress armour model, the carpet keys,
 * the Bow of Bone's pull, the HUD gauges and the goggles' Curios rendering.
 */
public final class GearClient {
    public static final String CATEGORY = "key.category.thaumaturgicwonders.main";
    public static final KeyMapping CARPET_FORWARD = new KeyMapping("key.thaumaturgicwonders.carpet_forward", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_W, CATEGORY);
    public static final KeyMapping CARPET_BACKWARD = new KeyMapping("key.thaumaturgicwonders.carpet_backward", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_S, CATEGORY);

    private static final ModelLayerLocation VF_HEAD = new ModelLayerLocation(ThaumicWonders.id("void_fortress_armor"), "head");
    private static final ModelLayerLocation VF_CHEST = new ModelLayerLocation(ThaumicWonders.id("void_fortress_armor"), "chest");
    private static final ModelLayerLocation VF_LEGS = new ModelLayerLocation(ThaumicWonders.id("void_fortress_armor"), "legs");

    private GearClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(GearClient::layers);
        modBus.addListener(GearClient::renderers);
        modBus.addListener(GearClient::keys);
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
        event.register(CARPET_FORWARD);
        event.register(CARPET_BACKWARD);
    }

    private static void guiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, ThaumicWonders.id("gear_hud"), new GearHud());
    }

    private static void clientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new VoidFortressExtension(), GearContent.VOID_FORTRESS_HELM.get(), GearContent.VOID_FORTRESS_CHEST.get(),
                GearContent.VOID_FORTRESS_LEGS.get());
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        CuriosRendererRegistry.register(GearContent.NIGHT_VISION_GOGGLES.get(), GogglesCurioRenderer::new);
        event.enqueueWork(() -> {
            ItemProperties.register(GearContent.BONE_BOW.get(), ThaumicWonders.id("pull"), BowPullProperty.PULL);
            ItemProperties.register(GearContent.BONE_BOW.get(), ThaumicWonders.id("pulling"), BowPullProperty.PULLING);
        });
    }

    /** Feeds the carpet keys to the carpet the player is piloting (the carpet moves on the pilot's side). */
    private static void clientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() instanceof FlyingCarpetEntity carpet) {
            carpet.updateInputs(CARPET_FORWARD.isDown(), CARPET_BACKWARD.isDown());
        }
    }

    private static final class VoidFortressExtension implements IClientItemExtensions {
        private VoidFortressArmorModel head;
        private VoidFortressArmorModel chest;
        private VoidFortressArmorModel legs;

        @Override
        public Model getGenericArmorModel(LivingEntity wearer, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
            VoidFortressArmorModel model;
            if (stack.is(GearContent.VOID_FORTRESS_HELM.get())) {
                if (this.head == null) {
                    this.head = bake(VF_HEAD);
                }
                model = this.head;
            } else if (stack.is(GearContent.VOID_FORTRESS_LEGS.get())) {
                if (this.legs == null) {
                    this.legs = bake(VF_LEGS);
                }
                model = this.legs;
            } else {
                if (this.chest == null) {
                    this.chest = bake(VF_CHEST);
                }
                model = this.chest;
            }
            ClientHooks.copyModelProperties(original, model);
            model.prepare(wearer);
            return model;
        }

        private static VoidFortressArmorModel bake(ModelLayerLocation layer) {
            return new VoidFortressArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(layer));
        }
    }
}
