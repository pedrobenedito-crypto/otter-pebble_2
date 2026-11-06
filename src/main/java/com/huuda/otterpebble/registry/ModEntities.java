package com.huuda.otterpebble.registry;

import com.huuda.otterpebble.OtterPebbleMod;
import com.huuda.otterpebble.entity.OtterEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, OtterPebbleMod.MODID);

    public static final RegistryObject<EntityType<OtterEntity>> OTTER = ENTITY_TYPES.register("otter",
            () -> EntityType.Builder.of(OtterEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.55F)
                    .clientTrackingRange(10)
                    .build(OtterPebbleMod.id("otter").toString()));

    private ModEntities() {
    }
}
