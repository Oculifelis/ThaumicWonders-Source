package com.thaumicwonders.gear;

import com.leclowndu93150.thaumaturge.api.items.RechargeAccess;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Flying Carpet item: holds 240 vis. Used on the top of a block it lays the carpet there; used on a water
 * cauldron it washes the dye out (one level of water); used on a Recharge Pedestal it does nothing, so the pedestal
 * can take it.
 */
public class FlyingCarpetItem extends Item {
    public static final int CAPACITY = 240;

    public FlyingCarpetItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (state.is(Blocks.WATER_CAULDRON)) {
            if (state.getValue(LayeredCauldronBlock.LEVEL) <= 0 || !stack.has(GearContent.DYE.get())) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                stack.remove(GearContent.DYE.get());
                LayeredCauldronBlock.lowerFillLevel(state, level, pos);
            }
            return InteractionResult.SUCCESS;
        }
        if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals("thaumaturge:recharge_pedestal")
                || context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            Vec3 at = context.getClickLocation();
            FlyingCarpetEntity carpet = GearContent.FLYING_CARPET_ENTITY.get().create(level);
            if (carpet == null) {
                return InteractionResult.FAIL;
            }
            carpet.moveTo(at.x, at.y, at.z, player != null ? player.getYRot() : 0.0F, 0.0F);
            carpet.setVisCharge(RechargeAccess.getCharge(stack));
            carpet.setEnergy(stack.getOrDefault(TTDataComponents.ENERGY.get(), 0));
            carpet.setDyeColor(stack.get(GearContent.DYE.get()));
            level.addFreshEntity(carpet);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
