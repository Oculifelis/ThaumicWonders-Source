package com.thaumicwonders.alchemy;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Mystic Gardening crop: an ordinary 8-age crop that, the moment it reaches full age (growth or bone meal), turns
 * into the mature Thaumaturge plant. Its soil is a block tag (data); vishroom spores also take any solid top face,
 * as the original's cave plant type did.
 */
public class MysticCropBlock extends CropBlock {
    private static final VoxelShape SHAPE = Block.box(4.8, 0, 4.8, 11.2, 9.6, 11.2);

    private final Supplier<? extends ItemLike> seed;
    private final Supplier<? extends Block> mature;
    private final TagKey<Block> soil;
    private final boolean anySolidTop;
    private final MapCodec<MysticCropBlock> codec = MapCodec.unit(this);

    public MysticCropBlock(Supplier<? extends ItemLike> seed, Supplier<? extends Block> mature, TagKey<Block> soil, boolean anySolidTop, Properties properties) {
        super(properties);
        this.seed = seed;
        this.mature = mature;
        this.soil = soil;
        this.anySolidTop = anySolidTop;
    }

    @Override
    public MapCodec<? extends CropBlock> codec() {
        return codec;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seed.get();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(soil) || (anySolidTop && state.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        mature(level, pos);
    }

    @Override
    public void growCrops(Level level, BlockPos pos, BlockState state) {
        super.growCrops(level, pos, state);
        mature(level, pos);
    }

    private void mature(Level level, BlockPos pos) {
        BlockState now = level.getBlockState(pos);
        if (now.is(this) && isMaxAge(now)) {
            level.setBlock(pos, mature.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
