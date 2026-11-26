package com.thaumicwonders.eldritch;

import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.thaumicwonders.ThaumicWonders;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Void Portal a Portal Generator opens above itself, as the original's EntityVoidPortal. Using it takes the player
 * to the linked Portal Anchor, in any dimension, if the anchor is still there. Both ends gain 5 flux. An unstable
 * generator throws the arrival off target: up to |stability|/2 blocks on each horizontal axis down to -25, and
 * 12.5 + (|stability|/2 - 12.5)^2 blocks below that (1418 at -100); the height is never changed (so, as in the
 * original, a bad landing can be inside terrain).
 */
public class VoidPortal extends Entity {
    private @Nullable GlobalPos link;
    private int cooldownTicks;
    private int soundTime;

    public VoidPortal(EntityType<? extends VoidPortal> type, Level level) {
        super(type, level);
    }

    public void setLink(@Nullable GlobalPos link) {
        this.link = link;
    }

    /** The stability of the generator under the portal (-100..100), 0 without one. Read on both sides. */
    public float generatorStability() {
        return level().getBlockEntity(blockPosition().below()) instanceof PortalGeneratorBlockEntity generator ? generator.stability() : 0.0F;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }
        if (!level().isClientSide() && random.nextInt(1000) < soundTime++) {
            soundTime = -540;
            playSound(TTSounds.MONOLITH.get(), 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player instanceof ServerPlayer sp && level() instanceof ServerLevel here && cooldownTicks <= 0) {
            cooldownTicks = 3;
            travel(here, sp);
        }
        return InteractionResult.SUCCESS;
    }

    private void travel(ServerLevel here, ServerPlayer player) {
        if (link == null) {
            player.sendOverlayMessage(Component.translatable("event.void_portal.no_anchor").withStyle(ChatFormatting.DARK_PURPLE));
            return;
        }
        ServerLevel there = here.getServer().getLevel(link.dimension());
        if (there == null) {
            player.sendOverlayMessage(Component.translatable("event.void_portal.no_world").withStyle(ChatFormatting.DARK_PURPLE));
            ThaumicWonders.LOG.error("Void portal at {} links to missing dimension {}", blockPosition(), link.dimension().identifier());
            return;
        }
        BlockPos anchor = link.pos();
        if (!there.getBlockState(anchor).is(EldritchContent.PORTAL_ANCHOR.get())) {
            player.sendOverlayMessage(Component.translatable("event.void_portal.no_anchor").withStyle(ChatFormatting.DARK_PURPLE));
            return;
        }
        float s = generatorStability();
        int max = 0;
        if (s < -25.0F) {
            double d = 0.5 * Math.abs(s) - 12.5;
            max = (int) (12.5 + d * d);
        } else if (s < 0.0F) {
            max = (int) (0.5 * Math.abs(s));
        }
        RandomSource r = here.getRandom();
        int dx = max > 0 ? r.nextInt(max) - r.nextInt(max) : 0;
        int dz = max > 0 ? r.nextInt(max) - r.nextInt(max) : 0;
        BlockPos target = anchor.offset(dx, 0, dz);

        AuraHelper.polluteAura(here, blockPosition(), 5.0F, true);
        boolean sameWorld = there == here;
        player.teleport(new TeleportTransition(there, new Vec3(target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5), Vec3.ZERO,
                player.getYRot(), player.getXRot(), Set.<Relative>of(), TeleportTransition.DO_NOTHING));
        if (sameWorld) {
            there.playSound(null, target, SoundEvents.PORTAL_TRAVEL, SoundSource.NEUTRAL, 0.25F, (r.nextFloat() - r.nextFloat()) * 0.2F + 1.0F);
        }
        AuraHelper.polluteAura(there, target.above(), 5.0F, true);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.storeNullable("link", GlobalPos.CODEC, link);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        link = in.read("link", GlobalPos.CODEC).orElse(null);
    }
}
