package com.thaumicwonders.validation;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.thaumicwonders.ThaumicWonders;
import com.thaumicwonders.data.DeviceLoot;
import com.thaumicwonders.data.DeviceRules;
import com.thaumicwonders.data.EncounterRules;
import com.thaumicwonders.data.EquipmentRules;
import com.thaumicwonders.devices.*;
import com.thaumicwonders.eldritch.EldritchContent;
import com.thaumicwonders.gear.*;
import com.thaumicwonders.primordial.*;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

final class PackRulesSelfTest {
    private static final BlockPos POS = new BlockPos(3, 200, 3);

    private PackRulesSelfTest() {
    }

    static void run(ServerLevel level, boolean override, BiConsumer<String, Runnable> check) {
        check.accept("chamber fuel, inputs and legacy order", () -> chamber(level, override));
        check.accept("orb fuel, action costs and legacy identities", () -> orbs(level, override));
        check.accept("research engines and ripper", () -> engines(level, override));
        check.accept("beacon tiers and chamber catalysts", () -> devices(level, override));
        check.accept("chamber and meaty orb loot", () -> loot(level, override));
        check.accept("diviner targets and range", () -> diviner(level, override));
        check.accept("portal events, invaders and subversions", () -> portal(override));
        check.accept("avatar seed limits", () -> seeds(level, override));
        check.accept("primal arrow hit and damage", () -> arrows(level, override));
        check.accept("armor, sword, set membership and bonuses", () -> equipment(level, override));
    }

    private static void require(boolean ok, String detail) {
        if (!ok) throw new AssertionError(detail);
    }

    private static Holder<IAspect> aspect(ServerLevel level, ResourceKey<IAspect> key) {
        return level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).getOrThrow(key);
    }

    private static void load(ServerLevel level, BlockEntity be, CompoundTag tag) {
        be.setLevel(level);
        be.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
    }

    private static void chamber(ServerLevel level, boolean override) {
        var state = PrimordialContent.ACCRETION_CHAMBER.get().defaultBlockState();
        var chamber = new AccretionChamberBlockEntity(POS, state);
        CompoundTag old = new CompoundTag();
        old.putIntArray("essentia", new int[] {2, 3, 4, 5, 6, 7});
        load(level, chamber, old);
        require(chamber.amount(TTAspects.TERRA) == 7 && chamber.amount(TTAspects.AER) == 2, "positional save was relabelled");
        CompoundTag saved = chamber.saveCustomOnly(level.registryAccess());
        require(saved.contains("stored_aspects") && !saved.contains("essentia"), "chamber did not write named storage");
        load(level, chamber, saved);
        require(chamber.amount(TTAspects.AQUA) == 3, "inactive aspect was lost");
        var fuel = override ? List.of(TTAspects.TERRA, TTAspects.AER)
                : List.of(TTAspects.AER, TTAspects.AQUA, TTAspects.IGNIS, TTAspects.ORDO, TTAspects.PERDITIO, TTAspects.TERRA);
        for (var key : fuel) {
            require(key.equals(chamber.currentSuction()), "wrong suction order for " + key);
            int capacity = override ? (key == TTAspects.TERRA ? 9 : 3) : 125;
            int room = capacity - chamber.amount(key);
            require(chamber.roomFor(key) == room && chamber.add(aspect(level, key), room + 1) == 1, "incorrect capacity for " + key);
        }
        require(chamber.isEssentiaFull() && chamber.currentSuction() == null, "chamber never reached full");
        require(chamber.canPlaceItemThroughFace(0, new ItemStack(Items.FEATHER), Direction.UP) == override, "feather input");
        require(chamber.canPlaceItemThroughFace(0, new ItemStack(PrimordialContent.PRIMORDIAL_GRAIN.get()), Direction.UP) != override, "grain input");
    }

    private static void orbs(ServerLevel level, boolean override) {
        var meaty = new MeatyOrbBlockEntity(POS, DevicesContent.MEATY_ORB.get().defaultBlockState());
        CompoundTag old = new CompoundTag();
        old.putShort("lifeEssentia", (short) 13);
        old.putShort("waterEssentia", (short) 5);
        old.putShort("eldritchEssentia", (short) 8);
        load(level, meaty, old);
        load(level, meaty, meaty.saveCustomOnly(level.registryAccess()));
        require(meaty.getEssentiaAmount(Direction.WEST) == (override ? 0 : 13) && meaty.getEssentiaAmount(Direction.EAST) == 5
                && meaty.getEssentiaAmount(Direction.SOUTH) == (override ? 13 : 8), "meaty orb relabelled stored fuel");
        var wrong = aspect(level, TTAspects.ORDO);
        require(meaty.addEssentia(wrong, 1, Direction.WEST) == 0 && meaty.getSuctionAmount(Direction.UP) == 0, "invalid inlet accepted fuel");
        for (Direction side : List.of(Direction.WEST, Direction.EAST, Direction.SOUTH)) {
            var requested = meaty.getSuctionType(side);
            require(requested != null, "missing orb inlet");
            meaty.addEssentia(requested, 1000, side);
        }
        meaty.act(FakePlayerFactory.getMinecraft(level), 0);
        require(meaty.getEssentiaAmount(Direction.WEST) == (override ? 5 : 0) && meaty.getEssentiaAmount(Direction.SOUTH) == (override ? 9 : 0),
                "meaty orb action cost");

        var weather = new MeteorbBlockEntity(POS, DevicesContent.METEORB.get().defaultBlockState());
        weather.setLevel(level);
        var west = aspect(level, override ? TTAspects.IGNIS : TTAspects.AER);
        require(weather.addEssentia(west, 99, Direction.WEST, true) == (override ? 7 : 50)
                && weather.getEssentiaAmount(Direction.WEST) == 0, "weather simulation or capacity");
        weather.addEssentia(west, 99, Direction.WEST);
        weather.act(FakePlayerFactory.getMinecraft(level), 0);
        require(weather.getEssentiaAmount(Direction.WEST) == (override ? 5 : 0), "weather activation cost");
    }

    private static void engines(ServerLevel level, boolean override) {
        for (var block : List.of(DevicesContent.INSPIRATION_ENGINE.get(), DevicesContent.MADNESS_ENGINE.get())) {
            var prior = level.getBlockState(POS);
            try {
                level.setBlock(POS, block.defaultBlockState(), 2);
                var engine = (ResearchEngineBlockEntity) level.getBlockEntity(POS);
                boolean madness = block == DevicesContent.MADNESS_ENGINE.get();
                var key = override ? TTAspects.AQUA : (madness ? TTAspects.ALIENIS : TTAspects.COGNITIO);
                int cost = override ? 2 : (madness ? 10 : 5);
                require(engine.addEssentia(aspect(level, key), cost - 1, Direction.WEST) == cost - 1 && !engine.isFueled(), "premature funding");
                engine.addEssentia(aspect(level, key), 1, Direction.WEST);
                require(engine.isFueled() && block.aspectSaveChance(level, POS, block.defaultBlockState()) == (override ? .75F : (madness ? .35F : .25F)),
                        "funded engine research discount");
            } finally {
                level.setBlock(POS, prior, 2);
            }
        }
        var ripper = new DimensionalRipperBlockEntity(POS, DevicesContent.DIMENSIONAL_RIPPER.get().defaultBlockState());
        CompoundTag old = new CompoundTag();
        old.putShort("essentia", (short) 8);
        load(level, ripper, old);
        require(ripper.amount() == (override ? 0 : 8), "ripper relabelled legacy Vitium");
        Direction inlet = ripper.getBlockState().getValue(DimensionalRipperBlock.FACING).getOpposite();
        require(ripper.addEssentia(aspect(level, override ? TTAspects.TERRA : TTAspects.VITIUM), 1000, inlet) == (override ? 12 : 242),
                "ripper fuel or capacity");
    }

    private static void devices(ServerLevel level, boolean override) {
        var beacon = new VoidBeaconBlockEntity(POS, DevicesContent.VOID_BEACON.get().defaultBlockState());
        int[] costs = override ? new int[]{1, 3, 8, 13, 21} : new int[]{20, 10, 5, 2, 1};
        for (int i = 0; i < costs.length; i++) {
            CompoundTag old = new CompoundTag();
            old.putShort("levels", (short) i);
            load(level, beacon, old);
            require(beacon.requiredEssentia() == costs[i], "beacon tier " + i);
        }
        var stick = new ItemStack(Items.STICK);
        require(PrimordialContent.isCatalystStone(stick) == override, "third-party socket item");
        var catalyst = (override ? stick : new ItemStack(PrimordialContent.ALCHEMIST_STONE.get())).typeHolder().getData(DeviceRules.CATALYST);
        require(catalyst != null && catalyst.fluxChance() == (override ? 2 : 50) && catalyst.sparkle().is(override ? TTAspects.AQUA : TTAspects.ORDO),
                "catalyst flux roll or aspect");
    }

    private static void loot(ServerLevel level, boolean override) {
        var meats = Set.of(Items.BEEF, Items.PORKCHOP, Items.CHICKEN, Items.MUTTON, Items.RABBIT);
        for (int i = 0; i < 32; i++) {
            var pearl = DeviceLoot.roll(level, POS, DeviceLoot.ACCRETION);
            var meat = DeviceLoot.roll(level, POS, DeviceLoot.MEATY_ORB);
            require(pearl.size() == 1 && pearl.getFirst().getCount() == 1, "missing chamber loot");
            require(override ? pearl.getFirst().is(Items.DIAMOND) : pearl.getFirst().is(TTItems.PRIMORDIAL_PEARL.get())
                    && Set.of(5, 6, 7).contains(pearl.getFirst().getDamageValue()), "wrong chamber product");
            require(meat.size() == 1 && (override ? meat.getFirst().is(Items.CARROT) : meats.contains(meat.getFirst().getItem())), "wrong orb loot");
        }
    }

    private static void diviner(ServerLevel level, boolean override) {
        var iron = net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState();
        var coal = net.minecraft.world.level.block.Blocks.COAL_ORE.defaultBlockState();
        require(OreMatch.color(iron) == (override ? 42 : 0xD8AF93), "ore color");
        require(OreMatch.names(coal).isEmpty() == override, "ignored ore block");
        var rule = DeviceRules.Diviner.of(new ItemStack(DevicesContent.STRUCTURE_DIVINER.get()));
        var id = ThaumicWonders.id(override ? "qa/diviner" : "structure_diviner/village");
        require(rule.radius() == (override ? 19 : 100) && rule.targets().size() == (override ? 9 : 8)
                && rule.targets().getFirst().tag().equals(id), "diviner search range, order or page count");
        require(rule.allows(id) && !rule.allows(ThaumicWonders.id("missing")), "server accepted an unknown selection");
        require(level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(TagKey.create(Registries.STRUCTURE, id)).orElseThrow().size() > 0,
                "selected structure tag is empty");
    }

    private static void portal(boolean override) {
        var rule = EldritchContent.PORTAL_GENERATOR.get().builtInRegistryHolder().getData(EncounterRules.PORTAL);
        var random = RandomSource.create(42L);
        require(rule != null && rule.event(0, random).isEmpty(), "stable portal emitted an event");
        if (override) {
            require(rule.event(-5, random).isEmpty() && rule.event(-6, random).orElseThrow() == EncounterRules.Event.INVADERS, "strict threshold");
            var invader = rule.invader(-6, random).orElseThrow();
            require(invader.entity() == EntityType.CHICKEN && invader.count(-6) == 4, "custom invader or count");
            require(rule.subversion(-1, random).orElseThrow() == EncounterRules.Subversion.FLUX_RIFT, "custom subversion");
        } else {
            require(rule.events().unwrap().stream().map(e -> e.weight()).toList().equals(List.of(60, 35, 5)), "original event weights");
            require(rule.invaders().unwrap().stream().map(e -> e.weight()).toList().equals(List.of(40, 30, 20, 7, 3)), "original invader weights");
            require(rule.subversions().unwrap().stream().map(e -> e.weight()).toList().equals(List.of(75, 25)), "original subversion weights");
        }
    }

    private static void seeds(ServerLevel level, boolean override) {
        var avatar = EldritchContent.CORRUPTION_AVATAR.get().create(level, EntitySpawnReason.COMMAND);
        require(avatar != null, "missing avatar");
        avatar.applyRiftEffects(2);
        var buff = avatar.getEffect(override ? MobEffects.SPEED : MobEffects.STRENGTH);
        require(buff != null && buff.getAmplifier() == 5 && buff.getDuration() == (override ? 45 : 200), "rift buff scaling");
        require(avatar.hasEffect(MobEffects.HASTE) != override, "rift effect selection");
        var easy = avatar.seedRule(Difficulty.EASY);
        require(easy != null && easy.limit() == (override ? 3 : 1) && easy.primeRolls() == (override ? 9 : 0), "avatar difficulty rule");
        var hard = avatar.seedRule(Difficulty.HARD);
        require(override ? hard == null : hard != null && hard.limit() == 4 && hard.primeRolls() == 2, "removed or default hard rule");
    }

    private static void arrows(ServerLevel level, boolean override) {
        var kind = PrimalArrowEntity.Kind.EARTH;
        var stack = new ItemStack(GearContent.arrowItem(kind));
        var arrow = new PrimalArrowEntity(level, 0, 200, 0, stack, kind);
        var saved = net.minecraft.world.level.storage.TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        arrow.saveWithoutId(saved);
        require(saved.buildResult().getDoubleOr("damage", -1) == (override ? 8 : 3) && arrow.profile().pierceArmor() == override
                && arrow.profile().infinityRoll() == (override ? 1 : 3), "fired arrow profile");
        var cow = EntityType.COW.create(level, EntitySpawnReason.COMMAND);
        require(cow != null, "missing effect target");
        arrow.profile().applyEffects(cow, arrow);
        require(cow.hasEffect(MobEffects.GLOWING) == override, "arrow effect did not follow the pack");
        cow.removeAllEffects();
        ItemStack sword = new ItemStack(GearContent.PRIMAL_DESTROYER.get());
        sword.set(GearContent.HUNGER.get(), 39);
        GearContent.PRIMAL_DESTROYER.get().hurtEnemy(sword, cow, FakePlayerFactory.getMinecraft(level));
        require(PrimalDestroyerItem.hunger(sword) == (override ? 36 : 0) && cow.hasEffect(MobEffects.GLOWING) == override
                && cow.hasEffect(MobEffects.WEAKNESS) != override, "sword on-hit hunger and effects");
    }

    private static void equipment(ServerLevel level, boolean override) {
        var helm = new ItemStack(GearContent.VOID_FORTRESS_HELM.get());
        var goggles = new ItemStack(GearContent.NIGHT_VISION_GOGGLES.get());
        var sword = new ItemStack(GearContent.PRIMAL_DESTROYER.get());
        require(helm.getMaxDamage() == ArmorType.HELMET.getDurability(override ? 9 : 50)
                && goggles.getMaxDamage() == (override ? 77 : 350) && sword.getMaxDamage() == (override ? 123 : 200), "equipment durability");
        require(helm.get(DataComponents.ATTRIBUTE_MODIFIERS).compute(Attributes.ARMOR, 0, EquipmentSlot.HEAD) == (override ? 13 : 4),
                "equipped armor defense");
        require(sword.get(DataComponents.ATTRIBUTE_MODIFIERS).compute(Attributes.ATTACK_DAMAGE, 0, EquipmentSlot.MAINHAND) == (override ? 15 : 11),
                "native sword attributes");
        require(GearContent.isVoidFortress(new ItemStack(Items.IRON_HELMET)) == override && GearContent.isVoidFortress(helm), "set membership tag");
        var bonus = EquipmentRules.fortress(level.registryAccess()).bonus(1);
        require(bonus.armor() == (override ? 3 : 0) && bonus.toughness() == (override ? 4 : 0), "one-piece set bonus");
    }
}
