package com.thaumicwonders.primordial;

import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.casters.IInteractWithCaster;
import com.leclowndu93150.thaumaturge.content.effect.Effects;
import com.leclowndu93150.thaumaturge.content.entity.EntityFluxRift;
import com.leclowndu93150.thaumaturge.content.infusion.BlockPillar;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.thaumicwonders.ThaumicWonders;
import com.thaumicwonders.devices.DeviceBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.thaumicwonders.util.ValueInput;
import com.thaumicwonders.util.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Coalescence Matrix, as the original's TileCoalescenceMatrix. On its platform (void metal floor with an arcane brick
 * rim one block down, eight arcane pillars around it) it drains flux rifts within 16 blocks that it can see, every
 * second: progress sqrt(size) per rift, the rift losing drained/15 stability and, one time in 33, one size. Every 50
 * progress is one charge, up to 10. Whenever it cannot make progress (no rift, broken platform, or already full) it
 * loses one progress per second, and with it charge. Fully charged, using any caster on it sets it off: arcs from the
 * pillars, explosions at the matrix and at the pillars, and an Avatar of Corruption in its place.
 */
public class CoalescenceMatrixBlockEntity extends DeviceBlockEntity implements IInteractWithCaster {
    public static final int PROGRESS_PER_CHARGE = 50;

    private int progress;

    public CoalescenceMatrixBlockEntity(BlockPos pos, BlockState state) {
        super(PrimordialContent.COALESCENCE_MATRIX_BE.get(), pos, state);
    }

    private int charge() {
        return getBlockState().getValue(CoalescenceMatrixBlock.CHARGE);
    }

    private void setCharge(int charge) {
        if (level != null && charge != charge()) {
            level.setBlock(worldPosition, getBlockState().setValue(CoalescenceMatrixBlock.CHARGE, charge), Block.UPDATE_ALL);
        }
    }

    private void incrementProgress(int amount) {
        progress += amount;
        if (progress >= PROGRESS_PER_CHARGE && charge() < CoalescenceMatrixBlock.MAX_CHARGE) {
            setCharge(charge() + 1);
            progress -= PROGRESS_PER_CHARGE;
        }
        progress = Math.max(0, Math.min(PROGRESS_PER_CHARGE, progress));
        setChanged();
    }

    private void decrementProgress(int amount) {
        progress -= amount;
        if (progress < 0 && charge() > 0) {
            setCharge(charge() - 1);
            progress += PROGRESS_PER_CHARGE;
        }
        progress = Math.max(0, Math.min(PROGRESS_PER_CHARGE, progress));
        setChanged();
    }

    private boolean isProgressFull() {
        return charge() >= CoalescenceMatrixBlock.MAX_CHARGE && progress >= PROGRESS_PER_CHARGE;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CoalescenceMatrixBlockEntity be) {
        be.tickCounter++;
        if (level instanceof ServerLevel server && be.tickCounter % 20 == 0) {
            List<EntityFluxRift> rifts = be.isValidPlacement(server) && !be.isProgressFull() ? be.validRifts(server) : List.of();
            if (!rifts.isEmpty()) {
                be.drainRifts(server, rifts);
            } else {
                be.decrementProgress(1);
            }
        }
    }

    private Vec3 top() {
        return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5);
    }

    private void drainRifts(ServerLevel level, List<EntityFluxRift> rifts) {
        boolean found = false;
        for (EntityFluxRift rift : rifts) {
            double drained = Math.sqrt(rift.currentSize());
            incrementProgress((int) drained);
            rift.adjustStability(-(float) (drained / 15.0));
            if (level.getRandom().nextInt(33) == 0) {
                rift.resize(rift.currentSize() - 1);
            }
            if (drained >= 1.0) {
                found = true;
            }
            if (found && tickCounter % 40 == 0) {
                Effects.voidStream(level, rift.position()).to(top()).seed(tickCounter).scale(0.04F).send();
            }
        }
    }

    private List<EntityFluxRift> validRifts(ServerLevel level) {
        List<EntityFluxRift> result = new ArrayList<>();
        Vec3 top = top();
        for (EntityFluxRift rift : level.getEntitiesOfClass(EntityFluxRift.class, new AABB(worldPosition).inflate(16.0))) {
            if (!rift.isAlive() || rift.currentSize() <= 1 || rift.distanceToSqr(Vec3.atCenterOf(worldPosition)) > 16.0 * 16.0) {
                continue;
            }
            Vec3 from = top.add(rift.position().subtract(top).normalize());
            HitResult hit = level.clip(new ClipContext(from, rift.position(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, rift));
            if (hit.getType() == HitResult.Type.MISS) {
                result.add(rift);
            }
        }
        return result;
    }

    /** The coalescence platform around the matrix (the original's isValidPlacement). */
    private boolean isValidPlacement(Level level) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -3; z <= 3; z++) {
                if (!level.getBlockState(worldPosition.offset(x, -1, z)).is(TTBlocks.METAL_VOID_BLOCK.get())) {
                    return false;
                }
            }
        }
        for (int x : new int[] {-3, 3}) {
            for (int z = -2; z <= 2; z++) {
                if (!level.getBlockState(worldPosition.offset(x, -1, z)).is(TTBlocks.METAL_VOID_BLOCK.get())) {
                    return false;
                }
            }
        }
        for (int i = -2; i <= 2; i++) {
            if (!brick(level, i, -4) || !brick(level, i, 4) || !brick(level, -4, i) || !brick(level, 4, i)) {
                return false;
            }
        }
        if (!brick(level, -3, -3) || !brick(level, 3, -3) || !brick(level, -3, 3) || !brick(level, 3, 3)) {
            return false;
        }
        for (BlockPos p : pillars(0)) {
            if (!(level.getBlockState(p).getBlock() instanceof BlockPillar)) {
                return false;
            }
        }
        return true;
    }

    private boolean brick(Level level, int x, int z) {
        return level.getBlockState(worldPosition.offset(x, -1, z)).is(TTBlocks.STONE_ARCANE_BRICK.get());
    }

    private List<BlockPos> pillars(int dy) {
        List<BlockPos> list = new ArrayList<>();
        for (int a : new int[] {-4, 4}) {
            for (int b : new int[] {-2, 2}) {
                list.add(worldPosition.offset(a, dy, b));
                list.add(worldPosition.offset(b, dy, a));
            }
        }
        return list;
    }

    @Override
    public boolean onCasterRightClick(Level level, ItemStack caster, Player player, BlockPos pos, Direction side, InteractionHand hand) {
        if (charge() < CoalescenceMatrixBlock.MAX_CHARGE || !(level instanceof ServerLevel server)) {
            return true;
        }
        List<BlockPos> pillars = pillars(1);
        int color = Aspects.resolve(server, TTAspects.VITIUM).value().color();
        Vec3 here = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        for (BlockPos p : pillars) {
            Effects.arcBolt(server, new Vec3(p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5)).to(here).color(color).width(0.6F).send();
        }
        server.playSound(null, pos, TTSounds.ZAP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        server.removeBlock(pos, false);
        server.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2.0F, Level.ExplosionInteraction.BLOCK);
        for (BlockPos p : pillars) {
            server.explode(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2.0F, Level.ExplosionInteraction.BLOCK);
        }
        BuiltInRegistries.ENTITY_TYPE.getOptional(ThaumicWonders.id("corruption_avatar")).ifPresentOrElse(type -> {
            Entity avatar = type.create(server);
            if (avatar != null) {
                avatar.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, server.getRandom().nextFloat() * 360.0F, 0.0F);
                server.addFreshEntity(avatar);
            }
        }, () -> ThaumicWonders.LOG.warn("Coalescence Matrix at {}: no thaumaturgicwonders:corruption_avatar entity registered", pos));
        return true;
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag outTag, net.minecraft.core.HolderLookup.Provider outReg) {
        ValueOutput out = new ValueOutput(outTag, outReg);
        super.saveAdditional(out.tag(), out.registries());
        out.putShort("progress", (short) progress);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag inTag, net.minecraft.core.HolderLookup.Provider inReg) {
        ValueInput in = new ValueInput(inTag, inReg);
        super.loadAdditional(in.tag(), in.registries());
        progress = in.getShortOr("progress", (short) 0);
    }
}
