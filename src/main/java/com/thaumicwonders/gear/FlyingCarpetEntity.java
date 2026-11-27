package com.thaumicwonders.gear;

import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.thaumicwonders.util.ValueInput;
import com.thaumicwonders.util.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The laid-out Flying Carpet. The pilot steers with the carpet keys (forward and back) toward where they look; each
 * vis in the carpet buys 30 seconds of flight (30 energy, one spent every second while ridden). Without energy it
 * falls. Sneak-using it rolls it back up into an item, keeping its vis, energy and colour.
 */
public class FlyingCarpetEntity extends Entity {
    private static final EntityDataAccessor<Integer> VIS_CHARGE = SynchedEntityData.defineId(FlyingCarpetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ENERGY = SynchedEntityData.defineId(FlyingCarpetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DYE_COLOR = SynchedEntityData.defineId(FlyingCarpetEntity.class, EntityDataSerializers.INT);
    public static final int ENERGY_PER_VIS = 30;
    private static final float FORWARD = 0.03F;
    private static final float BACKWARD = 0.005F;
    private static final double MOMENTUM = 0.9;

    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;
    private boolean forwardInputDown;
    private boolean backInputDown;

    public FlyingCarpetEntity(EntityType<? extends FlyingCarpetEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(VIS_CHARGE, 0);
        builder.define(ENERGY, 0);
        builder.define(DYE_COLOR, -1);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag inputTag) {
        ValueInput input = new ValueInput(inputTag, registryAccess());
        this.setVisCharge(input.getIntOr("vis", 0));
        this.setEnergy(input.getIntOr("energy", 0));
        this.entityData.set(DYE_COLOR, input.getIntOr("color", -1));
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag outputTag) {
        ValueOutput output = new ValueOutput(outputTag, registryAccess());
        output.putInt("vis", this.getVisCharge());
        output.putInt("energy", this.getEnergy());
        output.putInt("color", this.entityData.get(DYE_COLOR));
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        this.lerpSteps = 10;
    }

    @Override
    public double lerpTargetX() {
        return this.lerpSteps > 0 ? this.lerpX : this.getX();
    }

    @Override
    public double lerpTargetY() {
        return this.lerpSteps > 0 ? this.lerpY : this.getY();
    }

    @Override
    public double lerpTargetZ() {
        return this.lerpSteps > 0 ? this.lerpZ : this.getZ();
    }

    @Override
    public float lerpTargetXRot() {
        return this.lerpSteps > 0 ? (float) this.lerpXRot : this.getXRot();
    }

    @Override
    public float lerpTargetYRot() {
        return this.lerpSteps > 0 ? (float) this.lerpYRot : this.getYRot();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isControlledByLocalInstance()) {
            this.lerpSteps = 0;
            this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
        } else if (this.lerpSteps > 0) {
            this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
            this.lerpSteps--;
        }
        boolean ridden = this.isVehicle();
        if (ridden && !this.level().isClientSide() && this.tickCount % 20 == 0) {
            this.consumeEnergy();
            this.setNoGravity(this.hasEnergy());
        }
        if (ridden && this.isControlledByLocalInstance()) {
            Vec3 motion = this.getDeltaMovement().scale(MOMENTUM);
            if (!this.isNoGravity()) {
                motion = motion.add(0.0, -0.04, 0.0);
            }
            this.setDeltaMovement(motion);
            if (this.level().isClientSide() && this.hasEnergy()) {
                this.controlCarpet();
            }
            this.move(MoverType.SELF, this.getDeltaMovement());
        } else {
            this.setDeltaMovement(Vec3.ZERO);
            if (this.level().isClientSide()) {
                this.updateInputs(false, false);
            }
        }
        this.resetFallDistance();
        this.checkInsideBlocks();
    }

    private void consumeEnergy() {
        int energy = this.getEnergy();
        if (energy > 0) {
            energy--;
        } else {
            int vis = this.getVisCharge();
            if (vis > 0) {
                energy = ENERGY_PER_VIS;
                this.setVisCharge(vis - 1);
            }
        }
        this.setEnergy(energy);
    }

    public boolean hasEnergy() {
        return this.getEnergy() > 0 || this.getVisCharge() > 0;
    }

    private void controlCarpet() {
        LivingEntity pilot = this.getControllingPassenger();
        if (pilot == null) {
            return;
        }
        this.yRotO = this.getYRot();
        this.setYRot(pilot.getYRot());
        float f = 0.0F;
        if (this.forwardInputDown) {
            f += FORWARD;
        }
        if (this.backInputDown) {
            f -= BACKWARD;
        }
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        this.setDeltaMovement(this.getDeltaMovement().add(
                Mth.sin(-yaw) * f,
                Mth.sin(-pilot.getXRot() * Mth.DEG_TO_RAD) * f,
                Mth.cos(yaw) * f));
    }

    public void updateInputs(boolean forwardDown, boolean backDown) {
        this.forwardInputDown = forwardDown;
        this.backInputDown = backDown;
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof LivingEntity living ? living : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty();
    }

    /** Keeps the rider facing within 105 degrees of the carpet, as the original did. */
    private void clampRotation(Entity rider) {
        rider.setYBodyRot(this.getYRot());
        float delta = Mth.wrapDegrees(rider.getYRot() - this.getYRot());
        float clamped = Mth.clamp(delta, -105.0F, 105.0F);
        rider.yRotO += clamped - delta;
        rider.setYRot(rider.getYRot() + clamped - delta);
        rider.setYHeadRot(rider.getYRot());
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        this.clampRotation(passenger);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level() instanceof ServerLevel server) {
            if (player.isShiftKeyDown()) {
                this.spawnAtLocation(this.toItem(), 0.0F);
                this.discard();
            } else if (!this.isVehicle()) {
                player.startRiding(this);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** The carpet rolled up: same colour, vis and energy. */
    public ItemStack toItem() {
        ItemStack stack = new ItemStack(GearContent.FLYING_CARPET.get());
        DyeColor color = this.getDyeColor();
        if (color != null) {
            stack.set(GearContent.DYE.get(), color);
        }
        stack.set(TTDataComponents.CHARGE.get(), this.getVisCharge());
        stack.set(TTDataComponents.ENERGY.get(), this.getEnergy());
        return stack;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(GearContent.FLYING_CARPET.get());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // The original carpet could not be broken by attacks; it is picked up by sneak-using it.
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        // Shields the carpet and its rider from fall damage.
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return (other.canBeCollidedWith() || other.isPushable()) && !this.isPassengerOfSameVehicle(other);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    public void setVisCharge(int vis) {
        this.entityData.set(VIS_CHARGE, vis);
    }

    public int getVisCharge() {
        return this.entityData.get(VIS_CHARGE);
    }

    public void setEnergy(int energy) {
        this.entityData.set(ENERGY, energy);
    }

    public int getEnergy() {
        return this.entityData.get(ENERGY);
    }

    public void setDyeColor(@Nullable DyeColor color) {
        this.entityData.set(DYE_COLOR, color == null ? -1 : color.getId());
    }

    public @Nullable DyeColor getDyeColor() {
        int id = this.entityData.get(DYE_COLOR);
        return id < 0 ? null : DyeColor.byId(id);
    }
}
