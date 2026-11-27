package com.thaumicwonders.alchemy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/**
 * Fluid still textures that repeat every 4 blocks instead of every block. Next to
 * {@code <still>.png} put 16 sprites {@code <still>_<x>_<z>.png} (x, z = 0..3, cut from one seamless 64x64 surface and
 * animated in step); each block then shows the sprite for its position. Without them the plain still texture is used.
 */
public final class FluidTextures {
    private FluidTextures() {
    }

    /** Client fluid textures with the 4-block still repeat, untinted. */
    public static IClientFluidTypeExtensions extensions(ResourceLocation still, ResourceLocation flow) {
        ResourceLocation[] parts = new ResourceLocation[16];
        for (int i = 0; i < 16; i++) {
            parts[i] = still.withPath(still.getPath() + "_" + (i >> 2) + "_" + (i & 3));
        }
        return new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return still;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flow;
            }

            @Override
            public ResourceLocation getStillTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                ResourceLocation part = parts[(Math.floorMod(pos.getX(), 4) << 2) | Math.floorMod(pos.getZ(), 4)];
                TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(part);
                return sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation()) ? still : part;
            }

            @Override
            public int getTintColor() {
                return 0xFFFFFFFF;
            }
        };
    }
}
