package com.thaumicwonders.alchemy;

import com.leclowndu93150.thaumaturge.api.aspect.AspectIndexAccess;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.api.damagesource.TTDamageSources;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Alkahest Vat: a crucible of universal solvent. Items that fall in are destroyed and release
 * {@code count x 0.01 x sqrt(total aspects)} vis into the aura; living things inside take 1 dissolve damage every
 * 10th contact (the counter is shared by every vat, as in the original). Using an item on the top face dissolves
 * one of it.
 */
public class AlkahestVatBlock extends BaseEntityBlock {
    public static final MapCodec<AlkahestVatBlock> CODEC = simpleCodec(AlkahestVatBlock::new);
    /** Crucible shape: 5-pixel base plus four 2-pixel walls. */
    private static final VoxelShape SHAPE = Shapes.join(Shapes.block(), Block.box(2, 5, 2, 14, 16, 14), BooleanOp.ONLY_FIRST);
    private static int delay;

    public AlkahestVatBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlkahestVatBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? createTickerHelper(type, AlchemyContent.ALKAHEST_VAT_BE.get(), AlkahestVatBlockEntity::clientTick) : null;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean intersects) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (entity instanceof ItemEntity item) {
            releaseVis(level, pos, item.getItem());
            item.discard();
            hiss(level, pos);
        } else if (++delay >= 10) {
            delay = 0;
            if (entity instanceof LivingEntity living) {
                living.hurtServer(server, TTDamageSources.dissolve(level), 1.0F);
                hiss(level, pos);
            }
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || hit.getDirection() != Direction.UP) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            releaseVis(level, pos, stack.copyWithCount(1));
            stack.shrink(1);
            hiss(level, pos);
        }
        return InteractionResult.SUCCESS;
    }

    private static void releaseVis(Level level, BlockPos pos, ItemStack stack) {
        float vis = (float) (stack.getCount() * 0.01 * Math.sqrt(AspectIndexAccess.of(stack.copyWithCount(1)).totalAmount()));
        if (vis > 0) {
            AuraHelper.addVis(level, pos, vis);
        }
    }

    private static void hiss(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 2.0F + level.getRandom().nextFloat() * 0.4F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0) {
            level.playLocalSound(pos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.1F + random.nextFloat() * 0.1F, 1.2F + random.nextFloat() * 0.2F, false);
        }
    }
}
