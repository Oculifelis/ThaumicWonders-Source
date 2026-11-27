package com.thaumicwonders.gear.client;

import com.thaumicwonders.gear.GearContent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The Void Fortress armour (ModelVoidFortressArmor's geometry on a 128x64 texture). One layer per piece: the helm's
 * horned helmet on the head, the chest's plates on body and arms, the legs' belt and leg panels. Ornaments appear as
 * the set fills up: some at two pieces, more at three. The head is drawn 1.01x so hats do not poke through.
 */
public class VoidFortressArmorModel extends HumanoidModel<LivingEntity> {
    public enum Piece { HEAD, CHEST, LEGS }

    private record Part(Piece piece, String parent, String name, int u, int v, boolean mirror, float x, float y, float z,
            int w, int h, int d, float rx, float ry, float rz) {
    }

    private static final List<Part> PARTS = new ArrayList<>();
    /** Ornaments hidden until two pieces are worn. */
    private static final Set<String> NEED_TWO = Set.of("Book", "SideHornR1", "SideHornR2", "SideHornL1", "SideHornL2",
            "ShoulderplateRTop", "ShoulderplateRMid", "ShoulderplateR1", "ShoulderOrnamentR", "ShoulderOrnamentL",
            "ShoulderplateLTop", "ShoulderplateLMid", "ShoulderplateL1", "SidePanelR2", "SidePanelL2");
    /** Ornaments hidden until the full set is worn. */
    private static final Set<String> NEED_THREE = Set.of("Scroll", "FrontHornR1", "FrontHornR2", "FrontHornL1", "FrontHornL2",
            "ShoulderplateR2", "ShoulderplateL2", "SidePanelR3", "SidePanelL3", "HelmCordR", "HelmCordL", "ChestCordR",
            "ChestCordL", "BeltCord", "ArmBladeR1", "ArmBladeR2", "ArmBladeL1", "ArmBladeL2");

    static {
        part(Piece.HEAD, "head", "Helmet", 41, 8, false, -4.5F, -9.0F, -4.5F, 9, 4, 9, 0.0F, 0.0F, 0.0F);
        part(Piece.HEAD, "head", "HelmetR", 21, 13, false, -6.5F, -3.0F, -4.5F, 1, 5, 9, 0.0F, 0.0F, 0.5235988F);
        part(Piece.HEAD, "head", "HelmetL", 21, 13, true, 5.5F, -3.0F, -4.5F, 1, 5, 9, 0.0F, 0.0F, -0.5235988F);
        part(Piece.HEAD, "head", "HelmetB", 41, 21, false, -4.5F, -3.0F, 5.5F, 9, 5, 1, 0.5235988F, 0.0F, 0.0F);
        part(Piece.HEAD, "head", "Visor", 21, 0, false, -4.5F, -6.0F, -6.5F, 9, 1, 2, 0.0F, 0.0F, 0.0F);
        part(Piece.HEAD, "head", "Mask", 52, 2, false, -4.5F, -5.0F, -4.6F, 9, 5, 1, 0.0F, 0.0F, 0.0F);
        part(Piece.HEAD, "head", "FrontHornR1", 30, 6, false, -3.5F, -8.0F, -7.0F, 2, 2, 2, -0.1745329F, 0.0F, 0.0F);
        part(Piece.HEAD, "head", "FrontHornR2", 39, 6, false, -3.0F, -7.0F, -10.5F, 1, 1, 3, -0.2617994F, 0.0F, 0.0F);
        part(Piece.HEAD, "head", "FrontHornL1", 30, 6, true, 1.5F, -8.0F, -7.0F, 2, 2, 2, -0.1745329F, 0.0F, 0.0F);
        part(Piece.HEAD, "head", "FrontHornL2", 39, 6, true, 2.0F, -7.0F, -10.5F, 1, 1, 3, -0.2617994F, 0.0F, 0.0F);
        part(Piece.HEAD, "head", "SideHornR1", 30, 6, false, -8.5F, -7.2F, -1.0F, 2, 2, 2, 0.0F, 0.0F, 0.3490659F);
        part(Piece.HEAD, "head", "SideHornR2", 39, 11, false, -12.3F, -5.2F, -0.5F, 3, 1, 1, 0.0F, 0.0F, 0.5235988F);
        part(Piece.HEAD, "head", "SideHornL1", 30, 6, true, 6.5F, -7.2F, -1.0F, 2, 2, 2, 0.0F, 0.0F, -0.3490659F);
        part(Piece.HEAD, "head", "SideHornL2", 39, 11, true, 9.3F, -5.2F, -0.5F, 3, 1, 1, 0.0F, 0.0F, -0.5235988F);
        part(Piece.HEAD, "head", "HelmCordR", 25, 6, false, -4.5F, -5.0F, -5.6F, 1, 6, 1, 0.0F, 0.0F, 0.0F);
        part(Piece.HEAD, "head", "HelmCordL", 25, 6, true, 3.5F, -5.0F, -5.6F, 1, 6, 1, 0.0F, 0.0F, 0.0F);
        part(Piece.LEGS, "body", "MBelt", 56, 55, false, -4.0F, 8.0F, -3.0F, 8, 4, 1, 0.0F, 0.0F, 0.0F);
        part(Piece.LEGS, "body", "MBeltL", 76, 44, false, 4.0F, 8.0F, -3.0F, 1, 3, 6, 0.0F, 0.0F, 0.0F);
        part(Piece.LEGS, "body", "MBeltR", 76, 44, false, -5.0F, 8.0F, -3.0F, 1, 3, 6, 0.0F, 0.0F, 0.0F);
        part(Piece.CHEST, "body", "Chestplate", 56, 45, false, -4.0F, 1.0F, -4.0F, 8, 7, 2, 0.0F, 0.0F, 0.0F);
        part(Piece.CHEST, "body", "Scroll", 34, 27, false, -2.0F, 9.5F, 4.0F, 8, 3, 3, 0.0F, 0.0F, 0.1919862F);
        part(Piece.CHEST, "body", "Backplate", 36, 45, false, -4.0F, 1.0F, 2.0F, 8, 11, 2, 0.0F, 0.0F, 0.0F);
        part(Piece.CHEST, "body", "Book", 100, 8, false, 1.0F, -0.3F, 4.0F, 5, 7, 2, 0.0F, 0.0F, 0.7679449F);
        part(Piece.CHEST, "body", "ChestCordR", 20, 6, false, -3.5F, 1.0F, -5.0F, 1, 4, 1, 0.0F, 0.0F, 0.0F);
        part(Piece.CHEST, "body", "ChestCordL", 20, 6, true, 2.5F, 1.0F, -5.0F, 1, 4, 1, 0.0F, 0.0F, 0.0F);
        part(Piece.CHEST, "right_arm", "ShoulderR", 56, 35, false, -3.5F, -2.5F, -2.5F, 5, 5, 5, 0.0F, 0.0F, 0.0F);
        part(Piece.CHEST, "right_arm", "ShoulderOrnamentR", 79, 13, false, -2.0F, -1.0F, -3.0F, 2, 2, 1, 0.0F, 0.0F, 0.0F);
        part(Piece.CHEST, "right_arm", "GauntletR", 100, 26, false, -3.5F, 3.5F, -2.5F, 2, 6, 5, 0.0F, 0.0F, 0.0F);
        part(Piece.CHEST, "right_arm", "GauntletStrapR1", 84, 31, false, -1.5F, 3.5F, -2.5F, 3, 1, 5, 0.0F, 0.0F, 0.0F);
        part(Piece.CHEST, "right_arm", "GauntletStrapR2", 84, 31, false, -1.5F, 6.5F, -2.5F, 3, 1, 5, 0.0F, 0.0F, 0.0F);
        part(Piece.CHEST, "right_arm", "ShoulderplateRTop", 110, 37, false, -5.5F, -2.5F, -3.5F, 2, 1, 7, 0.0F, 0.0F, 0.3490659F);
        part(Piece.CHEST, "right_arm", "ShoulderplateRMid", 110, 37, false, -5.5F, 2.5F, -3.5F, 2, 1, 7, 0.0F, 0.0F, 0.3490659F);
        part(Piece.CHEST, "right_arm", "ShoulderplateR1", 110, 45, false, -4.5F, -1.5F, -3.5F, 1, 4, 7, 0.0F, 0.0F, 0.3490659F);
        part(Piece.CHEST, "right_arm", "ShoulderplateR2", 94, 45, false, -3.5F, 1.5F, -3.5F, 1, 5, 7, 0.0F, 0.0F, 0.3490659F);
        part(Piece.CHEST, "right_arm", "ArmBladeR1", 79, 17, false, -4.5F, 6.5F, -0.5F, 1, 3, 1, 0.0F, 0.0F, 0.0F);
        part(Piece.CHEST, "right_arm", "ArmBladeR2", 79, 17, false, -5.5F, 5.5F, -0.5F, 1, 3, 1, 0.0F, 0.0F, 0.0F);
        part(Piece.CHEST, "left_arm", "ShoulderL", 56, 35, true, -1.5F, -2.5F, -2.5F, 5, 5, 5, 0.0F, 0.0F, 0.0F);
        part(Piece.CHEST, "left_arm", "ShoulderOrnamentL", 79, 13, true, 0.0F, -1.0F, -3.0F, 2, 2, 1, 0.0F, 0.0F, 0.0F);
        part(Piece.CHEST, "left_arm", "GauntletL", 114, 26, false, 1.5F, 3.5F, -2.5F, 2, 6, 5, 0.0F, 0.0F, 0.0F);
        part(Piece.CHEST, "left_arm", "GauntletStrapL1", 84, 31, true, -1.5F, 3.5F, -2.5F, 3, 1, 5, 0.0F, 0.0F, 0.0F);
        part(Piece.CHEST, "left_arm", "GauntletStrapL2", 84, 31, true, -1.5F, 6.5F, -2.5F, 3, 1, 5, 0.0F, 0.0F, 0.0F);
        part(Piece.CHEST, "left_arm", "ShoulderplateLTop", 110, 37, true, 3.5F, -2.5F, -3.5F, 2, 1, 7, 0.0F, 0.0F, -0.3490659F);
        part(Piece.CHEST, "left_arm", "ShoulderplateLMid", 110, 37, true, 3.5F, 2.5F, -3.5F, 2, 1, 7, 0.0F, 0.0F, -0.3490659F);
        part(Piece.CHEST, "left_arm", "ShoulderplateL1", 110, 45, true, 3.5F, -1.5F, -3.5F, 1, 4, 7, 0.0F, 0.0F, -0.3490659F);
        part(Piece.CHEST, "left_arm", "ShoulderplateL2", 94, 45, true, 2.5F, 1.5F, -3.5F, 1, 5, 7, 0.0F, 0.0F, -0.3490659F);
        part(Piece.CHEST, "left_arm", "ArmBladeL1", 79, 17, false, 3.5F, 6.5F, -0.5F, 1, 3, 1, 0.0F, 0.0F, 0.0F);
        part(Piece.CHEST, "left_arm", "ArmBladeL2", 79, 17, false, 4.5F, 5.5F, -0.5F, 1, 3, 1, 0.0F, 0.0F, 0.0F);
        part(Piece.LEGS, "right_leg", "LegPanelR1", 0, 51, false, -1.0F, 0.5F, -3.5F, 3, 4, 1, -0.4363323F, 0.0F, 0.0F);
        part(Piece.LEGS, "right_leg", "LegPanelR2", 0, 51, false, -1.0F, 3.5F, -2.5F, 3, 4, 1, -0.4363323F, 0.0F, 0.0F);
        part(Piece.LEGS, "right_leg", "LegPanelR3", 0, 56, false, -1.0F, 6.5F, -1.5F, 3, 3, 1, -0.4363323F, 0.0F, 0.0F);
        part(Piece.LEGS, "right_leg", "LegPanelR4", 0, 43, false, -3.0F, 0.5F, -3.5F, 2, 3, 1, -0.4363323F, 0.0F, 0.0F);
        part(Piece.LEGS, "right_leg", "LegPanelR5", 0, 43, false, -3.0F, 2.5F, -2.5F, 2, 3, 1, -0.4363323F, 0.0F, 0.0F);
        part(Piece.LEGS, "right_leg", "LegPanelR6", 0, 43, false, -3.0F, 4.5F, -1.5F, 2, 3, 1, -0.4363323F, 0.0F, 0.0F);
        part(Piece.LEGS, "right_leg", "SidePanelR1", 0, 22, false, -2.5F, 0.5F, -2.5F, 1, 4, 5, 0.0F, 0.0F, 0.4363323F);
        part(Piece.LEGS, "right_leg", "SidePanelR2", 0, 31, false, -1.5F, 3.5F, -2.5F, 1, 3, 5, 0.0F, 0.0F, 0.4363323F);
        part(Piece.LEGS, "right_leg", "SidePanelR3", 0, 31, false, -0.5F, 5.5F, -2.5F, 1, 3, 5, 0.0F, 0.0F, 0.4363323F);
        part(Piece.LEGS, "right_leg", "BackPanelR1", 0, 18, false, -3.0F, 0.5F, 2.5F, 5, 3, 1, 0.4363323F, 0.0F, 0.0F);
        part(Piece.LEGS, "right_leg", "BackPanelR2", 0, 18, false, -3.0F, 2.5F, 1.5F, 5, 3, 1, 0.4363323F, 0.0F, 0.0F);
        part(Piece.LEGS, "right_leg", "BackPanelR3", 0, 18, false, -3.0F, 4.5F, 0.5F, 5, 3, 1, 0.4363323F, 0.0F, 0.0F);
        part(Piece.LEGS, "right_leg", "BeltCord", 15, 6, false, -1.0F, -2.5F, -4.5F, 1, 7, 1, -0.4363323F, 0.0F, 0.0F);
        part(Piece.LEGS, "left_leg", "LegPanelL1", 0, 51, true, -2.0F, 0.5F, -3.5F, 3, 4, 1, -0.4363323F, 0.0F, 0.0F);
        part(Piece.LEGS, "left_leg", "LegPanelL2", 0, 51, true, -2.0F, 3.5F, -2.5F, 3, 4, 1, -0.4363323F, 0.0F, 0.0F);
        part(Piece.LEGS, "left_leg", "LegPanelL3", 0, 56, true, -2.0F, 6.5F, -1.5F, 3, 3, 1, -0.4363323F, 0.0F, 0.0F);
        part(Piece.LEGS, "left_leg", "LegPanelL4", 0, 43, true, 1.0F, 0.5F, -3.5F, 2, 3, 1, -0.4363323F, 0.0F, 0.0F);
        part(Piece.LEGS, "left_leg", "LegPanelL5", 0, 43, true, 1.0F, 2.5F, -2.5F, 2, 3, 1, -0.4363323F, 0.0F, 0.0F);
        part(Piece.LEGS, "left_leg", "LegPanelL6", 0, 43, true, 1.0F, 4.5F, -1.5F, 2, 3, 1, -0.4363323F, 0.0F, 0.0F);
        part(Piece.LEGS, "left_leg", "SidePanelL1", 0, 22, true, 1.5F, 0.5F, -2.5F, 1, 4, 5, 0.0F, 0.0F, -0.4363323F);
        part(Piece.LEGS, "left_leg", "SidePanelL2", 0, 31, true, 0.5F, 3.5F, -2.5F, 1, 3, 5, 0.0F, 0.0F, -0.4363323F);
        part(Piece.LEGS, "left_leg", "SidePanelL3", 0, 31, true, -0.5F, 5.5F, -2.5F, 1, 3, 5, 0.0F, 0.0F, -0.4363323F);
        part(Piece.LEGS, "left_leg", "BackPanelL1", 0, 18, true, -2.0F, 0.5F, 2.5F, 5, 3, 1, 0.4363323F, 0.0F, 0.0F);
        part(Piece.LEGS, "left_leg", "BackPanelL2", 0, 18, true, -2.0F, 2.5F, 1.5F, 5, 3, 1, 0.4363323F, 0.0F, 0.0F);
        part(Piece.LEGS, "left_leg", "BackPanelL3", 0, 18, true, -2.0F, 4.5F, 0.5F, 5, 3, 1, 0.4363323F, 0.0F, 0.0F);
    }

    private static void part(Piece piece, String parent, String name, int u, int v, boolean mirror, float x, float y, float z,
            int w, int h, int d, float rx, float ry, float rz) {
        PARTS.add(new Part(piece, parent, name, u, v, mirror, x, y, z, w, h, d, rx, ry, rz));
    }

    private final List<ModelPart> needTwo = new ArrayList<>();
    private final List<ModelPart> needThree = new ArrayList<>();

    public VoidFortressArmorModel(ModelPart root) {
        super(root);
        for (Part part : PARTS) {
            ModelPart parent = root.getChild(part.parent());
            if (!parent.hasChild(part.name())) {
                continue;
            }
            ModelPart child = parent.getChild(part.name());
            if (NEED_TWO.contains(part.name())) {
                this.needTwo.add(child);
            } else if (NEED_THREE.contains(part.name())) {
                this.needThree.add(child);
            }
        }
    }

    public static LayerDefinition create(Piece piece) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        // 1.21.1's HumanoidModel looks the hat up on the root, not under the head.
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
        for (Part part : PARTS) {
            if (part.piece() != piece) {
                continue;
            }
            CubeListBuilder cubes = CubeListBuilder.create().texOffs(part.u(), part.v());
            if (part.mirror()) {
                cubes = cubes.mirror();
            }
            cubes.addBox(part.x(), part.y(), part.z(), part.w(), part.h(), part.d());
            root.getChild(part.parent()).addOrReplaceChild(part.name(), cubes,
                    PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, part.rx(), part.ry(), part.rz()));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** Shows the ornaments for the wearer's set size and sizes the helm so hats do not poke through. */
    public void prepare(LivingEntity wearer) {
        int set = GearContent.setCount(wearer);
        this.needTwo.forEach(p -> p.visible = set >= 2);
        this.needThree.forEach(p -> p.visible = set >= 3);
        this.head.xScale = this.head.yScale = this.head.zScale = 1.01F;
    }
}
