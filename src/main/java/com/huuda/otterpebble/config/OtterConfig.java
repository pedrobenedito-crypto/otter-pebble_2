package com.huuda.otterpebble.config;

import com.huuda.otterpebble.entity.OtterVariant;
import net.minecraft.util.RandomSource;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Numerical tuning is listed as an open team decision in the GDD ("World &amp; feel"), so every value lives here.
 * File: config/otterpebble-common.toml
 */
public final class OtterConfig {
    public enum PaletteMode { RANDOM, WARM_CHESTNUT, DEEP_BROWN, GRAY_BROWN }

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.EnumValue<PaletteMode> PALETTE;

    public static final ForgeConfigSpec.IntValue CALM_TICKS;
    public static final ForgeConfigSpec.IntValue INSPECT_TICKS;
    public static final ForgeConfigSpec.IntValue CURIOUS_RANGE;

    public static final ForgeConfigSpec.IntValue FOLLOW_START_DISTANCE;
    public static final ForgeConfigSpec.IntValue TELEPORT_DISTANCE;

    public static final ForgeConfigSpec.BooleanValue RESCUE_ENABLED;
    public static final ForgeConfigSpec.IntValue RESCUE_RANGE;
    public static final ForgeConfigSpec.IntValue RECENT_DROP_SECONDS;
    public static final ForgeConfigSpec.IntValue MAX_CARRY_SECONDS;

    public static final ForgeConfigSpec.IntValue GAZE_TICKS;
    public static final ForgeConfigSpec.IntValue CHIRP_COOLDOWN_SECONDS;

    public static final ForgeConfigSpec.BooleanValue BELLY_SLIDE;
    public static final ForgeConfigSpec.BooleanValue POST_SWIM_GROOM;
    public static final ForgeConfigSpec.BooleanValue LEARNING_BY_WATCHING;
    public static final ForgeConfigSpec.BooleanValue PEBBLE_PLAY;
    public static final ForgeConfigSpec.BooleanValue PAIRED_FLOAT;

    public static final ForgeConfigSpec.BooleanValue FISHING_SUPPORT;
    public static final ForgeConfigSpec.IntValue FISHING_RANGE;
    public static final ForgeConfigSpec.DoubleValue FISHING_CHANCE;
    public static final ForgeConfigSpec.IntValue FISHING_COOLDOWN_SECONDS;

    public static final ForgeConfigSpec.BooleanValue DROWNED_SPLASH;
    public static final ForgeConfigSpec.IntValue DROWNED_SPLASH_COOLDOWN_SECONDS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Look of the otters").push("general");
        PALETTE = b.comment("Palette options from the concept (cosmetic only, no biome, rarity or gameplay bonus).",
                        "RANDOM picks one per family.")
                .defineEnum("palette", PaletteMode.RANDOM);
        b.pop();

        b.comment("Bond: Approach -> Inspect -> Accept. Only an unbonded pup can bond.").push("bond");
        CALM_TICKS = b.comment("Ticks a player must stay calm nearby (holding a fish) before the pup becomes curious.")
                .defineInRange("calmTicksBeforeCurious", 40, 5, 1200);
        INSPECT_TICKS = b.comment("Ticks the pup inspects the offer before accepting it.")
                .defineInRange("inspectTicks", 50, 10, 600);
        CURIOUS_RANGE = b.comment("Distance (blocks) at which a calm player with a fish catches the pup's attention.")
                .defineInRange("curiousRange", 8, 3, 24);
        b.pop();

        b.comment("Follow / Wait orders").push("orders");
        FOLLOW_START_DISTANCE = b.comment("Distance (blocks) at which a following otter walks or swims after its owner.")
                .defineInRange("followStartDistance", 6, 2, 32);
        TELEPORT_DISTANCE = b.comment("Distance (blocks) at which a following otter catches up next to its owner. 0 disables.")
                .defineInRange("teleportDistance", 14, 0, 128);
        b.pop();

        b.comment("Main utility: the tool comes back").push("rescue");
        RESCUE_ENABLED = b.define("enabled", true);
        RESCUE_RANGE = b.comment("Search radius (blocks) around the otter for a recently dropped tool.")
                .defineInRange("searchRange", 16, 4, 48);
        RECENT_DROP_SECONDS = b.comment("How long after the owner's drop the tool still counts as a recent drop.")
                .defineInRange("recentDropSeconds", 60, 5, 3600);
        MAX_CARRY_SECONDS = b.comment("If the tool cannot be delivered in this time, it is settled as one safe drop.")
                .defineInRange("maxCarrySeconds", 120, 10, 3600);
        b.pop();

        b.comment("Chirp & answer: look at your otter with an empty main hand").push("greeting");
        GAZE_TICKS = b.defineInRange("gazeTicks", 20, 1, 200);
        CHIRP_COOLDOWN_SECONDS = b.defineInRange("chirpCooldownSeconds", 10, 1, 600);
        b.pop();

        b.comment("Approved personality behaviours and the requested paired float").push("behaviours");
        BELLY_SLIDE = b.define("bellySlide", true);
        POST_SWIM_GROOM = b.define("postSwimGroom", true);
        LEARNING_BY_WATCHING = b.define("learningByWatching", true);
        PEBBLE_PLAY = b.define("pebblePlay", true);
        PAIRED_FLOAT = b.comment("Requested social behaviour: two otters float holding one forepaw each.")
                .define("pairedFloat", true);
        b.pop();

        b.comment("Appendix D - future options. Off by default: the GDD asks for separate approval",
                "and anti-stacking tests before activation.").push("future_options");
        FISHING_SUPPORT = b.comment("One extra ordinary fish after a rod catch. The original result and its odds are kept.")
                .define("fishingSupport", false);
        FISHING_RANGE = b.defineInRange("fishingRangeBlocks", 10, 1, 64);
        FISHING_CHANCE = b.defineInRange("fishingChance", 0.25D, 0.0D, 1.0D);
        FISHING_COOLDOWN_SECONDS = b.defineInRange("fishingCooldownSeconds", 90, 1, 3600);
        DROWNED_SPLASH = b.comment("One brief pushback on a Drowned after it hits the owner. No damage, pursuit or combat command.")
                .define("drownedEscapeSplash", false);
        DROWNED_SPLASH_COOLDOWN_SECONDS = b.defineInRange("drownedSplashCooldownSeconds", 60, 1, 3600);
        b.pop();

        SPEC = b.build();
    }

    public static OtterVariant pickVariant(RandomSource random) {
        return switch (PALETTE.get()) {
            case WARM_CHESTNUT -> OtterVariant.WARM_CHESTNUT;
            case DEEP_BROWN -> OtterVariant.DEEP_BROWN;
            case GRAY_BROWN -> OtterVariant.GRAY_BROWN;
            default -> OtterVariant.values()[random.nextInt(OtterVariant.values().length)];
        };
    }

    private OtterConfig() {
    }
}
