package com.huuda.otterpebble.registry;

import com.huuda.otterpebble.OtterPebbleMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Data-driven item lists (editable with a datapack). */
public final class ModTags {
    /** Fish a pup accepts as the bond offer (the GDD's "pebble" decision: fish). */
    public static final TagKey<Item> BOND_OFFERINGS = TagKey.create(Registries.ITEM, OtterPebbleMod.id("bond_offerings"));
    /** Food a bonded otter accepts as care (heals; speeds up a pup's growth). */
    public static final TagKey<Item> CARE_FOOD = TagKey.create(Registries.ITEM, OtterPebbleMod.id("care_food"));
    /** Tools the otter may rescue from the water. */
    public static final TagKey<Item> RESCUABLE_TOOLS = TagKey.create(Registries.ITEM, OtterPebbleMod.id("rescuable_tools"));

    private ModTags() {
    }
}
