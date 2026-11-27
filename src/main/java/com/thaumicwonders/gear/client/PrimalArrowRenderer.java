package com.thaumicwonders.gear.client;

import com.thaumicwonders.ThaumicWonders;
import com.thaumicwonders.gear.PrimalArrowEntity;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** The vanilla arrow model with each primal's own texture. */
public class PrimalArrowRenderer extends ArrowRenderer<PrimalArrowEntity> {
    private static final Map<PrimalArrowEntity.Kind, ResourceLocation> TEXTURES = new EnumMap<>(PrimalArrowEntity.Kind.class);

    static {
        for (PrimalArrowEntity.Kind kind : PrimalArrowEntity.Kind.values()) {
            TEXTURES.put(kind, ThaumicWonders.id("textures/entity/primal_arrow_" + kind.id() + ".png"));
        }
    }

    public PrimalArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(PrimalArrowEntity arrow) {
        return TEXTURES.get(arrow.kind());
    }
}
