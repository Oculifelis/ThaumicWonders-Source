package com.thaumicwonders.alchemy;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.content.essentia.jar.AbstractBlockEntityJar;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Creative Essentia Jar: a Thaumaturge jar (labels, phials, braces, tubes, suction, dumping all inherited) whose
 * contents never run out. As in the original, a take of its aspect always succeeds and never deducts.
 */
public class CreativeEssentiaJarBlockEntity extends AbstractBlockEntityJar {
    public CreativeEssentiaJarBlockEntity(BlockPos pos, BlockState state) {
        super(AlchemyContent.CREATIVE_ESSENTIA_JAR_BE.get(), pos, state);
    }

    @Override
    protected boolean doTakeFromContainer(ResourceKey<IAspect> key, int requested) {
        return key != null && key.equals(aspectKey());
    }

    /** Transactional extraction (tubes, the infusion matrix) commits through here; extraction never lowers the amount. */
    @Override
    public void setStorageContents(ResourceKey<IAspect> key, int amount) {
        ResourceKey<IAspect> current = aspectKey();
        if (current != null && amount() > 0 && (key == null || key.equals(current)) && amount < amount()) {
            return;
        }
        super.setStorageContents(key, amount);
    }
}
