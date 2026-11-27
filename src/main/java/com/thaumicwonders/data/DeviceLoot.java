package com.thaumicwonders.data;

import com.thaumicwonders.ThaumicWonders;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public final class DeviceLoot {
    public static final ResourceKey<LootTable> ACCRETION = ResourceKey.create(Registries.LOOT_TABLE, ThaumicWonders.id("gameplay/accretion"));
    public static final ResourceKey<LootTable> MEATY_ORB = ResourceKey.create(Registries.LOOT_TABLE, ThaumicWonders.id("gameplay/meaty_orb"));

    private DeviceLoot() {
    }

    public static List<ItemStack> roll(ServerLevel level, BlockPos pos, ResourceKey<LootTable> key) {
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, pos.getCenter())
                .create(LootContextParamSets.CHEST);
        return level.getServer().reloadableRegistries().getLootTable(key).getRandomItems(params, level.getRandom());
    }
}
