package com.thaumicwonders.gear.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thaumicwonders.gear.NightVisionGogglesItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Night-Vision Goggles in the Curios head slot: drawn as the worn goggles (the armour layer on the head), where the
 * original drew a flat bauble sprite on the face.
 */
public class GogglesCurioRenderer implements ICurioRenderer {
    private HumanoidModel<LivingEntity> model;

    @Override
    @SuppressWarnings("unchecked")
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack pose,
            RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount,
            float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(parent.getModel() instanceof HumanoidModel<?> humanoid)) {
            return;
        }
        if (this.model == null) {
            this.model = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
        }
        ((HumanoidModel<T>) humanoid).copyPropertiesTo((HumanoidModel<T>) (HumanoidModel<?>) this.model);
        this.model.setAllVisible(false);
        this.model.head.visible = true;
        this.model.hat.visible = true;
        this.model.renderToBuffer(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(NightVisionGogglesItem.TEXTURE)), light,
                OverlayTexture.NO_OVERLAY);
    }
}
