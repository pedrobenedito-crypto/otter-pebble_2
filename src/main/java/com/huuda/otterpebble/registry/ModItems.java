package com.huuda.otterpebble.registry;

import com.huuda.otterpebble.OtterPebbleMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, OtterPebbleMod.MODID);

    /** Chestnut body with cream belly, like the concept art. */
    public static final RegistryObject<Item> OTTER_SPAWN_EGG = ITEMS.register("otter_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.OTTER, 0x8E5435, 0xEAD0AC, new Item.Properties()));

    private ModItems() {
    }
}
