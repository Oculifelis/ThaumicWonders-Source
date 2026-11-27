package com.thaumicwonders.devices;

import com.thaumicwonders.data.EssentiaTanks;
import com.thaumicwonders.data.EssentiaTanks.Tank;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.effect.Effects;
import com.leclowndu93150.thaumaturge.content.entity.EntityFluxRift;
import com.leclowndu93150.thaumaturge.content.particle.VisSparkleParticleOptions;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Two rippers ten blocks apart, facing each other, both enabled and holding at least 50 vitium, pour their fuel into
 * the point midway between them: an existing rift within 2 blocks grows, otherwise a new Thaumaturge flux rift
 * opens there (unless some other rift lies within 32 blocks, in which case the fuel is spent for nothing, as in the
 * original).
 */
public class DimensionalRipperBlockEntity extends EssentiaDeviceBlockEntity {
    public static final int DISTANCE = 10;
    private static final int RED = 0xFFFF0000;

    private final EssentiaTanks stored = new EssentiaTanks();

    public DimensionalRipperBlockEntity(BlockPos pos, BlockState state) {
        super(DevicesContent.DIMENSIONAL_RIPPER_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DimensionalRipperBlockEntity be) {
        if (++be.tickCounter % 5 == 0) {
            if (be.stored.room(be.fuel()) > 0) {
                be.fill(List.of(be.facing().getOpposite()), true);
            }
            be.checkForActivation((ServerLevel) level);
        }
    }

    private Direction facing() {
        return getBlockState().getValue(DimensionalRipperBlock.FACING);
    }

    private boolean enabled() {
        return getBlockState().getValue(DimensionalRipperBlock.ENABLED);
    }

    private @Nullable Tank fuel() {
        return EssentiaTanks.Profile.of(getBlockState()).slot(0, 1);
    }

    private void checkForActivation(ServerLevel level) {
        Direction facing = facing();
        BlockPos otherPos = worldPosition.relative(facing, DISTANCE);
        if (!(level.getBlockEntity(otherPos) instanceof DimensionalRipperBlockEntity other) || other.facing() != facing.getOpposite()) {
            return;
        }
        Tank tank = fuel();
        Tank otherTank = other.fuel();
        if (tank == null || otherTank == null) return;
        // The link sparkle (the original's block event 5): a vis sparkle in vitium's colour from here to the partner.
        Vec3 here = Vec3.atCenterOf(worldPosition);
        Vec3 there = Vec3.atCenterOf(otherPos);
        Effects.spawn(level, new VisSparkleParticleOptions(there.x, there.y, there.z, tank.aspect().value().color()), here.x, here.y, here.z);
        if (!enabled() || !other.enabled() || !stored.funded(tank) || !other.stored.funded(otherTank)) {
            return;
        }
        // Java int division, as the original (it rounds towards zero for negative coordinates).
        BlockPos target = new BlockPos((worldPosition.getX() + otherPos.getX()) / 2, (worldPosition.getY() + otherPos.getY()) / 2,
                (worldPosition.getZ() + otherPos.getZ()) / 2);
        int fuel = Math.min(stored.amount(tank.key()), other.stored.amount(otherTank.key()));
        stored.drain(tank, fuel);
        other.stored.drain(otherTank, fuel);
        sync();
        other.sync();
        Vec3 targetCentre = Vec3.atCenterOf(target);
        Effects.drillBeam(level, beamStart(worldPosition, facing)).to(targetCentre).color(RED).type(1).send();
        Effects.drillBeam(level, beamStart(otherPos, other.facing())).to(targetCentre).color(RED).type(1).send();
        level.playSound(null, worldPosition, TTSounds.ZAP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, otherPos, TTSounds.ZAP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        createRift(level, target, fuel);
    }

    private static Vec3 beamStart(BlockPos pos, Direction facing) {
        return Vec3.atCenterOf(pos).add(facing.getStepX() * 0.5, facing.getStepY() * 0.5, facing.getStepZ() * 0.5);
    }

    private static void createRift(ServerLevel level, BlockPos target, int fuel) {
        Vec3 centre = Vec3.atCenterOf(target);
        List<EntityFluxRift> close = riftsWithin(level, centre, 2.0);
        if (!close.isEmpty()) {
            EntityFluxRift rift = close.getFirst();
            int oldSize = rift.currentSize();
            double oldFuel = oldSize * oldSize / 3.0;
            int newSize = (int) Math.sqrt((oldFuel + 2.0 * fuel) * 3.0);
            rift.resize(newSize);
            rift.adjustStability(-(newSize - oldSize));
        } else if (riftsWithin(level, centre, 32.0).isEmpty()) {
            EntityFluxRift rift = new EntityFluxRift(TTEntities.FLUX_RIFT.get(), level);
            rift.reseed(level.getRandom().nextInt());
            rift.snapTo(centre.x, centre.y, centre.z, level.getRandom().nextInt(360), 0.0F);
            if (level.addFreshEntity(rift)) {
                rift.resize((int) Math.sqrt(2.0 * fuel * 3.0));
            }
        }
    }

    private static List<EntityFluxRift> riftsWithin(ServerLevel level, Vec3 centre, double range) {
        return level.getEntitiesOfClass(EntityFluxRift.class, new AABB(centre, centre).inflate(range),
                rift -> rift.position().distanceToSqr(centre) <= range * range);
    }

    // ---- essentia --------------------------------------------------------------------------------------------

    @Override
    protected boolean acceptsFace(Direction face) {
        return face == facing().getOpposite();
    }

    @Override
    protected @Nullable Holder<IAspect> wantedOn(Direction face) {
        Tank tank = fuel();
        return tank == null ? null : tank.aspect();
    }

    @Override
    protected int roomFor(Direction face) {
        return stored.room(fuel());
    }

    @Override
    protected int store(Holder<IAspect> aspect, int count, Direction face) {
        Tank tank = fuel();
        return tank != null && aspect.is(tank.key()) ? count - stored.add(tank, count) : count;
    }

    @Override
    public @Nullable Holder<IAspect> getEssentiaType(@Nullable Direction side) {
        return wantedOn(facing().getOpposite());
    }

    @Override
    public int getEssentiaAmount(@Nullable Direction side) {
        return amount();
    }

    public int amount() {
        Tank tank = fuel();
        return tank == null ? 0 : stored.amount(tank.key());
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        stored.save(out);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        stored.load(in, () -> stored.importAmount(TTAspects.VITIUM, in.getShortOr("essentia", (short) 0)));
    }
}
