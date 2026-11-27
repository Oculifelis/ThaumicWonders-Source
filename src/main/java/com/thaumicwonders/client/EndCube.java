package com.thaumicwonders.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/** A unit cube drawn with the end portal look, as the end gateway draws its faces. */
public final class EndCube {
    private EndCube() {
    }

    public static void draw(PoseStack pose, MultiBufferSource buffers) {
        Matrix4f m = pose.last().pose();
        VertexConsumer c = buffers.getBuffer(RenderType.endPortal());
        face(c, m, 0, 1, 0, 1, 1, 1, 1, 1);
        face(c, m, 0, 1, 1, 0, 0, 0, 0, 0);
        face(c, m, 1, 1, 1, 0, 0, 1, 1, 0);
        face(c, m, 0, 0, 0, 1, 0, 1, 1, 0);
        face(c, m, 0, 1, 0, 0, 0, 0, 1, 1);
        face(c, m, 0, 1, 1, 1, 1, 1, 0, 0);
    }

    private static void face(VertexConsumer c, Matrix4f m, float x0, float x1, float y0, float y1, float z0, float z1, float z2, float z3) {
        c.addVertex(m, x0, y0, z0);
        c.addVertex(m, x1, y0, z1);
        c.addVertex(m, x1, y1, z2);
        c.addVertex(m, x0, y1, z3);
    }
}
