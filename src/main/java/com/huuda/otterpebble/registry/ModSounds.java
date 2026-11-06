package com.huuda.otterpebble.registry;

import com.huuda.otterpebble.OtterPebbleMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, OtterPebbleMod.MODID);

    public static final RegistryObject<SoundEvent> OTTER_CHIRP = register("entity.otter.chirp");
    public static final RegistryObject<SoundEvent> OTTER_AMBIENT = register("entity.otter.ambient");
    public static final RegistryObject<SoundEvent> OTTER_HURT = register("entity.otter.hurt");
    public static final RegistryObject<SoundEvent> OTTER_DEATH = register("entity.otter.death");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(OtterPebbleMod.id(name)));
    }

    private ModSounds() {
    }
}
