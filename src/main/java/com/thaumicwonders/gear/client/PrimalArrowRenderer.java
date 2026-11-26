package com.thaumicwonders.gear.client;

import com.thaumicwonders.ThaumicWonders;
import com.thaumicwonders.gear.PrimalArrowEntity;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

/** The vanilla arrow model with each primal's own texture. */
public class PrimalArrowRenderer extends ArrowRenderer<PrimalArrowEntity, PrimalArrowRenderer.State> {
    private static final Map<PrimalArrowEntity.Kind, Identifier> TEXTURES = new EnumMap<>(PrimalArrowEntity.Kind.class);

    static {
        for (PrimalArrowEntity.Kind kind : PrimalArrowEntity.Kind.values()) {
            TEXTURES.put(kind, ThaumicWonders.id("textures/entity/primal_arrow_" + kind.id() + ".png"));
        }
    }

    public static class State extends ArrowRenderState {
        public PrimalArrowEntity.Kind kind = PrimalArrowEntity.Kind.AIR;
    }

    public PrimalArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PrimalArrowEntity arrow, State state, float partialTick) {
        super.extractRenderState(arrow, state, partialTick);
        state.kind = arrow.kind();
    }

    @Override
    protected Identifier getTextureLocation(State state) {
        return TEXTURES.get(state.kind);
    }
}
