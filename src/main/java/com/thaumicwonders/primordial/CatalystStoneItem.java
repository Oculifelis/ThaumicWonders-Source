package com.thaumicwonders.primordial;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A catalyst stone for the Catalyzation Chamber (the original's ICatalystStone): 64 uses (durability 63), a 1-in-X
 * chance per operation to release one flux, and the aspect whose colour its sparkles take. What it turns items into
 * is data: the {@code thaumaturgicwonders:catalyzation} recipes naming it.
 */
public class CatalystStoneItem extends Item {
    public CatalystStoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 10;
    }
}
