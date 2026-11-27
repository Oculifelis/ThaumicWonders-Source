package com.thaumicwonders.devices;

import com.leclowndu93150.thaumaturge.api.research.IResearchTableAid;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Inspiration Engine and Madness Engine. Thaumaturge has no theorycraft cards, so an engine is a research table aid:
 * placed directly on top of either half of a research table and fuelled with essentia, it gives the table its chance
 * that placing or combining an aspect costs nothing (the original card's "1 inspiration and a bonus draw"), and it
 * spends one dose of fuel per research note finished at that table for the original's research gain (see
 * {@link ResearchEngineBlockEntity}).
 */
public class ResearchEngineBlock extends BaseEntityBlock implements IResearchTableAid {
    private final ResearchEngineBlockEntity.Kind kind;

    public ResearchEngineBlock(Properties properties, ResearchEngineBlockEntity.Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public ResearchEngineBlockEntity.Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MapCodec.unit(this);
    }

    @Override
    public float aspectSaveChance(Level level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof ResearchEngineBlockEntity engine && engine.isFueled()
                ? com.thaumicwonders.data.EssentiaTanks.Profile.of(state).saveChance() : 0.0F;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResearchEngineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, DevicesContent.RESEARCH_ENGINE_BE.get(), ResearchEngineBlockEntity::serverTick);
    }
}
