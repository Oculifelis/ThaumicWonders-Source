package com.thaumicwonders.primordial;

import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * A catalyst stone for the Catalyzation Chamber (the original's ICatalystStone): 64 uses (durability 63), a 1-in-X
 * chance per operation to release one flux, and the aspect whose colour its sparkles take. What it turns items into
 * is data: the {@code thaumaturgicwonders:catalyzation} recipes naming it.
 */
public class CatalystStoneItem extends Item {
    private final int fluxChance;
    private final ResourceKey<IAspect> sparkle;

    public CatalystStoneItem(Properties properties, int fluxChance, ResourceKey<IAspect> sparkle) {
        super(properties);
        this.fluxChance = fluxChance;
        this.sparkle = sparkle;
    }

    /** One flux every X operations on average. */
    public int fluxChance() {
        return fluxChance;
    }

    public int sparkleColor(Level level) {
        return Aspects.resolve(level, sparkle).value().color();
    }
}
