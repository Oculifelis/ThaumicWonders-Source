package com.thaumicwonders.alchemy;

import com.leclowndu93150.thaumaturge.content.essentia.jar.AbstractBlockEntityJar;
import com.leclowndu93150.thaumaturge.content.essentia.jar.AbstractBlockJar;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Creative Essentia Jar block: Thaumaturge's jar with an infinite block entity. */
public class CreativeEssentiaJarBlock extends AbstractBlockJar {
    public static final MapCodec<CreativeEssentiaJarBlock> CODEC = simpleCodec(CreativeEssentiaJarBlock::new);

    public CreativeEssentiaJarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CreativeEssentiaJarBlock> codec() {
        return CODEC;
    }

    /** The base jar creates and ticks this type (block entity, fill-from-above). */
    @Override
    public BlockEntityType<? extends AbstractBlockEntityJar> jarType() {
        return AlchemyContent.CREATIVE_ESSENTIA_JAR_BE.get();
    }
}
