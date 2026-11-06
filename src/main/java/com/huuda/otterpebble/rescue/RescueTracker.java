package com.huuda.otterpebble.rescue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Provenance and exclusive claims for dropped tools, stored on the item entity's persistent data
 * (saved with the item, so it survives reloads).
 *
 * <ul>
 *     <li>Notice: only items tossed by the owner (Forge {@code ItemTossEvent}) are tagged as owner drops.</li>
 *     <li>Claim: one otter reserves one item; a claim expires unless the claiming otter refreshes it.</li>
 *     <li>Settled items (returned or dropped by an otter) are never claimed again.</li>
 * </ul>
 */
public final class RescueTracker {
    public static final String TAG_DROPPER = "otterpebble_dropper";
    public static final String TAG_DROP_TIME = "otterpebble_drop_time";
    public static final String TAG_CLAIM = "otterpebble_claim";
    public static final String TAG_CLAIM_TIME = "otterpebble_claim_time";
    public static final String TAG_SETTLED = "otterpebble_settled";

    /** A claim that has not been refreshed for this many ticks is considered abandoned. */
    private static final long CLAIM_TIMEOUT = 40L;

    public static void markOwnerDrop(ItemEntity item, Player dropper) {
        CompoundTag data = item.getPersistentData();
        data.putUUID(TAG_DROPPER, dropper.getUUID());
        data.putLong(TAG_DROP_TIME, dropper.level().getGameTime());
    }

    public static boolean isRecentDropOf(ItemEntity item, UUID owner, long now, long windowTicks) {
        CompoundTag data = item.getPersistentData();
        if (data.getBoolean(TAG_SETTLED) || !data.hasUUID(TAG_DROPPER)) {
            return false;
        }
        return owner.equals(data.getUUID(TAG_DROPPER)) && now - data.getLong(TAG_DROP_TIME) <= windowTicks;
    }

    public static long dropTimeOf(ItemEntity item) {
        return item.getPersistentData().getLong(TAG_DROP_TIME);
    }

    public static boolean isClaimedByOther(ItemEntity item, UUID otter, long now) {
        CompoundTag data = item.getPersistentData();
        if (!data.hasUUID(TAG_CLAIM)) {
            return false;
        }
        boolean fresh = now - data.getLong(TAG_CLAIM_TIME) <= CLAIM_TIMEOUT;
        return fresh && !otter.equals(data.getUUID(TAG_CLAIM));
    }

    public static boolean isClaimedBy(ItemEntity item, UUID otter) {
        CompoundTag data = item.getPersistentData();
        return data.hasUUID(TAG_CLAIM) && otter.equals(data.getUUID(TAG_CLAIM));
    }

    public static boolean tryClaim(ItemEntity item, UUID otter, long now) {
        if (isClaimedByOther(item, otter, now)) {
            return false;
        }
        CompoundTag data = item.getPersistentData();
        data.putUUID(TAG_CLAIM, otter);
        data.putLong(TAG_CLAIM_TIME, now);
        return true;
    }

    public static void refreshClaim(ItemEntity item, UUID otter, long now) {
        if (isClaimedBy(item, otter)) {
            item.getPersistentData().putLong(TAG_CLAIM_TIME, now);
        }
    }

    public static void releaseClaim(ItemEntity item, UUID otter) {
        if (isClaimedBy(item, otter)) {
            CompoundTag data = item.getPersistentData();
            data.remove(TAG_CLAIM);
            data.remove(TAG_CLAIM_TIME);
        }
    }

    /** Settled drops keep no rescue provenance, so they are never rescued again (no loops). */
    public static void markSettled(ItemEntity item) {
        CompoundTag data = item.getPersistentData();
        data.remove(TAG_DROPPER);
        data.remove(TAG_DROP_TIME);
        data.remove(TAG_CLAIM);
        data.remove(TAG_CLAIM_TIME);
        data.putBoolean(TAG_SETTLED, true);
    }

    private RescueTracker() {
    }
}
