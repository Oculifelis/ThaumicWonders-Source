package com.thaumicwonders.eldritch;

import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.api.items.IGogglesReadout;
import com.leclowndu93150.thaumaturge.content.device.BlockEntityStabilizer;
import com.leclowndu93150.thaumaturge.content.effect.Effects;
import com.leclowndu93150.thaumaturge.content.entity.EntityFluxRift;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.thaumicwonders.devices.DeviceBlockEntity;
import java.text.DecimalFormat;
import java.util.List;
import com.thaumicwonders.data.EncounterRules;
import com.thaumicwonders.data.EncounterRules.Portal;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Portal Generator, as the original's TilePortalGenerator. It keeps its portal's link and a stability from -100 to 100.
 * Every 6 seconds while its portal is open, Thaumaturge stabilizers within 8 blocks spend charge to raise it (0.75 per
 * charge, until very stable) and every open portal within 16 blocks lowers it (0.04 x portals squared). Every 30
 * seconds an unstable portal (below 0) may misbehave, with a chance of |stability| in 1000: a flux burst, invaders
 * from the void, or (very unstable) the portal is subverted into a Crimson Cult portal or collapses into a flux rift.
 * With revealing goggles it shows its stability level and the last gain and loss.
 */
public class PortalGeneratorBlockEntity extends DeviceBlockEntity implements IGogglesReadout {
    private static final DecimalFormat FORMAT = new DecimalFormat("#######.##");

    private @Nullable GlobalPos link;
    private float stability;
    private float lastStabilityIncrease;
    private float lastStabilityDecrease;
    private int sparkCounter;
    private int ticksExisted;
    private @Nullable Boolean lastEnabled;

    public PortalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(EldritchContent.PORTAL_GENERATOR_BE.get(), pos, state);
    }

    // ---- stability ------------------------------------------------------------------------------------------

    public enum Stability {
        VERY_STABLE("very_stable", ChatFormatting.GREEN), STABLE("stable", ChatFormatting.DARK_GREEN),
        UNSTABLE("unstable", ChatFormatting.GOLD), VERY_UNSTABLE("very_unstable", ChatFormatting.RED);

        final String key;
        final ChatFormatting color;

        Stability(String key, ChatFormatting color) {
            this.key = key;
            this.color = color;
        }
    }

    public static Stability level(float stability) {
        if (stability >= 50.0F) {
            return Stability.VERY_STABLE;
        }
        if (stability >= 0.0F) {
            return Stability.STABLE;
        }
        return stability >= -25.0F ? Stability.UNSTABLE : Stability.VERY_UNSTABLE;
    }

    public float stability() {
        return stability;
    }

    private void setStability(float value) {
        stability = Math.max(-100.0F, Math.min(100.0F, value));
    }

    public void setLink(GlobalPos link) {
        this.link = link;
        sync();
    }

    public @Nullable GlobalPos link() {
        return link;
    }

    // ---- portal ---------------------------------------------------------------------------------------------

    public @Nullable VoidPortal activePortal() {
        if (level == null) {
            return null;
        }
        List<VoidPortal> portals = level.getEntitiesOfClass(VoidPortal.class, new AABB(worldPosition.above()));
        return portals.isEmpty() ? null : portals.getFirst();
    }

    public void spawnPortal() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        VoidPortal portal = new VoidPortal(EldritchContent.VOID_PORTAL.get(), server);
        BlockPos up = worldPosition.above();
        portal.snapTo(up.getX() + 0.5, up.getY(), up.getZ() + 0.5, 0.0F, 0.0F);
        portal.setLink(link);
        server.addFreshEntity(portal);
    }

    public void despawnPortal(boolean sound) {
        VoidPortal portal = activePortal();
        if (portal == null) {
            return;
        }
        portal.discard();
        if (sound && level != null) {
            RandomSource r = level.getRandom();
            level.playSound(null, worldPosition, TTSounds.SHOCK.get(), SoundSource.BLOCKS, 1.0F, (r.nextFloat() - r.nextFloat()) * 0.2F + 1.0F);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) {
            despawnPortal(true);
        }
    }

    // ---- tick -----------------------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, PortalGeneratorBlockEntity be) {
        ServerLevel server = (ServerLevel) level;
        be.sparkCounter++;
        be.ticksExisted++;
        if (be.sparkCounter >= 20) {
            be.flash(server);
            be.sparkCounter -= 20 + server.getRandom().nextInt(80);
        }
        boolean enabled = state.getValue(PortalGeneratorBlock.ENABLED);
        if (be.lastEnabled == null) {
            be.lastEnabled = enabled;
        }
        VoidPortal portal = be.activePortal();
        if (enabled != be.lastEnabled) {
            if (enabled && portal == null) {
                be.spawnPortal();
            } else if (!enabled && portal != null) {
                be.despawnPortal(true);
            }
            portal = be.activePortal();
        }
        be.lastEnabled = enabled;

        if (be.ticksExisted % 120 == 0) {
            float before = be.stability;
            float increase = be.lastStabilityIncrease;
            float decrease = be.lastStabilityDecrease;
            be.lastStabilityIncrease = 0.0F;
            be.lastStabilityDecrease = 0.0F;
            if (portal != null) {
                be.stabilize(server);
                int n = server.getEntitiesOfClass(VoidPortal.class, new AABB(pos.above()).inflate(16.0)).size();
                if (n > 0) {
                    be.lastStabilityDecrease = 0.04F * n * n;
                    be.setStability(be.stability - be.lastStabilityDecrease);
                }
            }
            if (before != be.stability || increase != be.lastStabilityIncrease || decrease != be.lastStabilityDecrease) {
                be.sync();
            }
        }
        if (portal != null && be.ticksExisted % 600 == 0 && be.stability < 0.0F
                && server.getRandom().nextInt(1000) < Math.abs(be.stability)) {
            be.instabilityEvent(server);
        }
    }

    /** The flux-coloured lightning flash over the generator, every 1 to 5 seconds. */
    private void flash(ServerLevel level) {
        RandomSource r = level.getRandom();
        BlockPos src = worldPosition.above();
        if (r.nextInt() % 2 == 0) {
            src = src.south();
        }
        if (r.nextInt() % 2 == 0) {
            src = src.east();
        }
        int c = Aspects.resolve(level, TTAspects.VITIUM).value().color();
        Effects.lightningFlash(level, new net.minecraft.world.phys.Vec3(src.getX(), src.getY(), src.getZ()))
                .color(((c >> 16) & 0xFF) / 255.0F, ((c >> 8) & 0xFF) / 255.0F, (c & 0xFF) / 255.0F).alpha(1.0F).scale(2.5F).send();
    }

    /** Stabilizers within 8 blocks; as the original, the first one found empty ends the whole scan. */
    private void stabilize(ServerLevel level) {
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-8, -8, -8), worldPosition.offset(8, 8, 8))) {
            if (level.getBlockEntity(p) instanceof BlockEntityStabilizer stabilizer) {
                while (level(stability) != Stability.VERY_STABLE && stabilizer.mitigate(1)) {
                    setStability(stability + 0.75F);
                    lastStabilityIncrease += 0.75F;
                }
                if (stabilizer.getEnergy() == 0) {
                    return;
                }
            }
        }
    }

    // ---- instability ----------------------------------------------------------------------------------------

    private void instabilityEvent(ServerLevel level) {
        Portal rule = getBlockState().typeHolder().getData(EncounterRules.PORTAL);
        if (rule == null) return;
        rule.event(stability, level.getRandom()).ifPresent(event -> {
            switch (event) {
                case FLUX -> AuraHelper.polluteAura(level, worldPosition, (float) Math.sqrt(Math.abs(stability)), true);
                case INVADERS -> rule.invader(stability, level.getRandom()).ifPresent(invader -> spawnInvaders(level, invader));
                case SUBVERSION -> rule.subversion(stability, level.getRandom()).ifPresent(action -> subvert(level, action));
            }
        });
    }

    private void spawnInvaders(ServerLevel level, EncounterRules.Invader invader) {
        int count = invader.count(stability);
        RandomSource r = level.getRandom();
        BlockPos portalPos = worldPosition.above();
        boolean spawned = false;
        for (int i = 0; i < count; i++) {
            if (!(invader.entity().create(level, EntitySpawnReason.EVENT) instanceof Mob mob)) {
                continue;
            }
            mob.snapTo(portalPos.getX() + r.nextGaussian() * 3.0, portalPos.getY() + r.nextGaussian() * 3.0,
                    portalPos.getZ() + r.nextGaussian() * 3.0, 0.0F, 0.0F);
            if (mob.checkSpawnRules(level, EntitySpawnReason.EVENT) && mob.checkSpawnObstruction(level)) {
                net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.EVENT, null);
                spawned |= level.addFreshEntity(mob);
            }
        }
        if (spawned) {
            EldritchContent.tellNearby(level, worldPosition, 16.0, "event.void_portal.invader");
        }
    }

    private void subvert(ServerLevel level, EncounterRules.Subversion action) {
        RandomSource r = level.getRandom();
        if (action == EncounterRules.Subversion.CRIMSON_PORTAL) {
            Mob portal = TTEntities.CULTIST_PORTAL_LESSER.get().create(level, EntitySpawnReason.EVENT);
            if (portal == null) {
                return;
            }
            portal.snapTo(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, 0.0F, 0.0F);
            despawnPortal(false);
            net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(portal, level, level.getCurrentDifficultyAt(worldPosition), EntitySpawnReason.EVENT, null);
            level.addFreshEntity(portal);
            EldritchContent.tellNearby(level, worldPosition, 16.0, "event.void_portal.subvert.lesser_crimson");
        } else {
            EntityFluxRift rift = new EntityFluxRift(TTEntities.FLUX_RIFT.get(), level);
            rift.reseed(r.nextInt());
            rift.snapTo(worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5, r.nextInt(360), 0.0F);
            despawnPortal(false);
            if (level.addFreshEntity(rift)) {
                rift.resize((int) Math.sqrt(2.0 * Math.abs(stability) * 3.0));
            }
            EldritchContent.tellNearby(level, worldPosition, 16.0, "event.void_portal.subvert.flux_rift");
        }
    }

    // ---- goggles --------------------------------------------------------------------------------------------

    @Override
    public List<Component> readout() {
        Stability s = level(stability);
        return List.of(
                Component.translatable("gui.thaumaturge.infusion.stability." + s.key).withStyle(s.color, ChatFormatting.BOLD),
                Component.translatable("gui.thaumaturgicwonders.stability.gain", FORMAT.format(lastStabilityIncrease))
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC),
                Component.translatable("gui.thaumaturgicwonders.stability.loss", FORMAT.format(lastStabilityDecrease))
                        .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
    }

    // ---- save -----------------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.storeNullable("link", GlobalPos.CODEC, link);
        out.putFloat("stability", stability);
        out.putFloat("lastStabilityIncrease", lastStabilityIncrease);
        out.putFloat("lastStabilityDecrease", lastStabilityDecrease);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        link = in.read("link", GlobalPos.CODEC).orElse(null);
        stability = in.getFloatOr("stability", 0.0F);
        lastStabilityIncrease = in.getFloatOr("lastStabilityIncrease", 0.0F);
        lastStabilityDecrease = in.getFloatOr("lastStabilityDecrease", 0.0F);
    }
}
