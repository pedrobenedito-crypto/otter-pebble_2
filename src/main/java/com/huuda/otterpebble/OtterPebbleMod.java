package com.huuda.otterpebble;

import com.huuda.otterpebble.config.OtterConfig;
import com.huuda.otterpebble.registry.ModEntities;
import com.huuda.otterpebble.registry.ModItems;
import com.huuda.otterpebble.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.geckolib.GeckoLib;

/**
 * Otter &amp; the Pebble: a river companion for Minecraft (HUUDA concept, GDD v04).
 */
@Mod(OtterPebbleMod.MODID)
public final class OtterPebbleMod {
    public static final String MODID = "otterpebble";

    // The classic context getters still work on every Forge 47 build. The newer constructor parameter
    // (FMLJavaModLoadingContext) only exists from Forge 47.3.10, so the classic form keeps older 1.20.1 setups working.
    @SuppressWarnings("removal")
    public OtterPebbleMod() {
        // GeckoLib asks dependent mods to call this from their constructor; it is idempotent.
        GeckoLib.initialize();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITY_TYPES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModSounds.SOUND_EVENTS.register(modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, OtterConfig.SPEC);
    }

    // Kept on purpose: OptiFine for 1.20.1 removes Forge's ResourceLocation.fromNamespaceAndPath,
    // so mods that call it crash with NoSuchMethodError when OptiFine is installed.
    @SuppressWarnings("removal")
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
