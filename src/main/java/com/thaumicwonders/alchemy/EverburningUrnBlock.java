package com.thaumicwonders.alchemy;

import net.minecraft.world.ItemInteractionResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

/** Everburning Urn block: buckets draw its lava; a full urn smokes and flickers. */
public class EverburningUrnBlock extends BaseEntityBlock {
    public static final MapCodec<EverburningUrnBlock> CODEC = simpleCodec(EverburningUrnBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public EverburningUrnBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EverburningUrnBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, AlchemyContent.EVERBURNING_URN_BE.get(), EverburningUrnBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof EverburningUrnBlockEntity urn)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        // Any face works with a bucket, as in the original; pipes only reach the tank from the top.
        if (FluidUtil.interactWithFluidHandler(player, hand, urn.tank())) {
            urn.sync();
            level.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 0.33F,
                    1.0F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.3F);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof EverburningUrnBlockEntity urn && urn.isFull()) {
            double x = pos.getX() + 0.5 + (random.nextFloat() - random.nextFloat()) * 0.1;
            double y = pos.getY() + 1.1;
            double z = pos.getZ() + 0.5 + (random.nextFloat() - random.nextFloat()) * 0.1;
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
            if (random.nextFloat() < 0.1F) {
                level.playLocalSound(pos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            }
        }
    }
}
