package com.thaumicwonders.devices;

import com.thaumicwonders.ThaumicWonders;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The devices' packets. The original's effect and message packets (ripper beams, meteorb bolts, localized messages)
 * are Thaumaturge's server-side effects and overlay messages instead; what is left are the screen buttons and the
 * Ore Diviner's client-side search.
 */
public final class DevicesNetwork {
    /** How far from a device a player may press its screen's buttons. */
    private static final double REACH_SQ = 8.0 * 8.0;

    private DevicesNetwork() {
    }

    /** Timewinder button: 0..7 a moon phase, 8 the next sunrise. */
    public record TimewinderAction(int target) implements CustomPacketPayload {
        public static final Type<TimewinderAction> TYPE = new Type<>(ThaumicWonders.id("timewinder_action"));
        public static final StreamCodec<ByteBuf, TimewinderAction> CODEC = ByteBufCodecs.VAR_INT.map(TimewinderAction::new, TimewinderAction::target);

        @Override
        public Type<TimewinderAction> type() {
            return TYPE;
        }
    }

    /** Structure Diviner selection carries the structure tag, or an empty selection to clear. */
    public record StructureDivinerAction(Optional<Identifier> target) implements CustomPacketPayload {
        public static final Type<StructureDivinerAction> TYPE = new Type<>(ThaumicWonders.id("structure_diviner_action"));
        public static final StreamCodec<ByteBuf, StructureDivinerAction> CODEC = Identifier.STREAM_CODEC.apply(ByteBufCodecs::optional)
                .map(StructureDivinerAction::new, StructureDivinerAction::target);

        @Override
        public Type<StructureDivinerAction> type() {
            return TYPE;
        }
    }

    /** Meteorb or Meaty Orb button at a position. */
    public record OrbAction(BlockPos pos, int choice) implements CustomPacketPayload {
        public static final Type<OrbAction> TYPE = new Type<>(ThaumicWonders.id("orb_action"));
        public static final StreamCodec<ByteBuf, OrbAction> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, OrbAction::pos,
                ByteBufCodecs.VAR_INT, OrbAction::choice, OrbAction::new);

        @Override
        public Type<OrbAction> type() {
            return TYPE;
        }
    }

    /** Server to the player who used an Ore Diviner: search around it for this ore. */
    public record OreDivinerSearch(BlockPos pos, ItemStack stack) implements CustomPacketPayload {
        public static final Type<OreDivinerSearch> TYPE = new Type<>(ThaumicWonders.id("ore_diviner_search"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OreDivinerSearch> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC,
                OreDivinerSearch::pos, ItemStack.STREAM_CODEC, OreDivinerSearch::stack, OreDivinerSearch::new);

        @Override
        public Type<OreDivinerSearch> type() {
            return TYPE;
        }
    }

    /** Server to every player: forget this diviner's target. */
    public record OreDivinerStop(BlockPos pos) implements CustomPacketPayload {
        public static final Type<OreDivinerStop> TYPE = new Type<>(ThaumicWonders.id("ore_diviner_stop"));
        public static final StreamCodec<ByteBuf, OreDivinerStop> CODEC = BlockPos.STREAM_CODEC.map(OreDivinerStop::new, OreDivinerStop::pos);

        @Override
        public Type<OreDivinerStop> type() {
            return TYPE;
        }
    }

    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToServer(TimewinderAction.TYPE, TimewinderAction.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer player) {
                TimewinderItem.act(player, p.target());
            }
        });
        registrar.playToServer(StructureDivinerAction.TYPE, StructureDivinerAction.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer player) {
                StructureDivinerItem.act(player, p.target());
            }
        });
        registrar.playToServer(OrbAction.TYPE, OrbAction.CODEC, DevicesNetwork::handleOrb);
        registrar.playToClient(OreDivinerSearch.TYPE, OreDivinerSearch.CODEC,
                (p, ctx) -> OreDivinerBlockEntity.search(ctx.player().level(), ctx.player(), p.pos(), p.stack()));
        registrar.playToClient(OreDivinerStop.TYPE, OreDivinerStop.CODEC, (p, ctx) -> {
            if (ctx.player().level().getBlockEntity(p.pos()) instanceof OreDivinerBlockEntity diviner) {
                diviner.setTarget(null);
            }
        });
    }

    private static void handleOrb(OrbAction p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player) || player.distanceToSqr(p.pos().getCenter()) > REACH_SQ
                || !DevicesContent.isSurfaceWorld(player.level())) {
            return;
        }
        if (player.level().getBlockEntity(p.pos()) instanceof OrbBlockEntity orb) {
            orb.act(player, p.choice());
        }
    }
}
