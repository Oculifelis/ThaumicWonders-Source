package com.thaumicwonders.gear.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Night-Vision Goggles in the Curios head slot: drawn as the worn goggles (the armour layer on the head), where the
 * original drew a flat bauble sprite on the face.
 */
public class GogglesCurioRenderer implements ICurioRenderer {
    private HumanoidModel<HumanoidRenderState> model;

    @Override
    public <S extends LivingEntityRenderState, M extends EntityModel<? super S>> void render(ItemStack stack, SlotContext slotContext,
            PoseStack pose, SubmitNodeCollector collector, int light, S state, RenderLayerParent<S, M> parent,
            EntityRendererProvider.Context context, float yRotation, float xRotation) {
        if (!(state instanceof HumanoidRenderState humanoid)) {
            return;
        }
        if (this.model == null) {
            this.model = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_ARMOR.head()));
        }
        this.model.setupAnim(humanoid);
        var equipment = stack.get(DataComponents.EQUIPPABLE);
        if (equipment == null || equipment.assetId().isEmpty()) return;
        context.getEquipmentRenderer().renderLayers(EquipmentClientInfo.LayerType.HUMANOID, equipment.assetId().get(), this.model, humanoid,
                stack, pose, collector, light, humanoid.outlineColor);
    }
}
