package com.huuda.otterpebble.entity.ai;

import com.huuda.otterpebble.config.OtterConfig;
import com.huuda.otterpebble.entity.OtterEntity;
import com.huuda.otterpebble.registry.ModTags;
import com.huuda.otterpebble.rescue.RescueTracker;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * Proposed rescue contract: Notice -> Claim &amp; dive -> Carry -> Deliver.
 * <ul>
 *     <li>Notice: a recent owner drop in safe, loaded water (provenance from the toss event).</li>
 *     <li>Claim &amp; dive: exclusive claim and a valid route to the item and back to the owner.</li>
 *     <li>Carry: mouth carry, same item (type, count, condition, metadata, remaining lifetime).</li>
 *     <li>Deliver: one safe pickup; a full inventory leaves one safe world drop.</li>
 * </ul>
 * No extra loot, no lifetime reset, no container access and no other players' items.
 */
public class OtterRescueGoal extends Goal {
    private enum Phase { DIVE, RETURN }

    private final OtterEntity otter;
    @Nullable
    private ItemEntity target;
    private Phase phase = Phase.DIVE;
    private int searchCooldown;
    private int repathTicks;
    private int phaseTicks;

    public OtterRescueGoal(OtterEntity otter) {
        this.otter = otter;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (otter.isCarrying()) {
            return !otter.isOrderedToWait() && otter.getOwnerPlayer() != null; // resume a pending delivery
        }
        if (!otter.canStartRescue()) {
            return false;
        }
        if (--searchCooldown > 0) {
            return false;
        }
        searchCooldown = 10;
        Player owner = otter.getOwnerPlayer();
        if (owner == null) {
            return false;
        }
        ItemEntity candidate = findCandidate(owner);
        if (candidate == null || !hasValidRoute(candidate, owner)) {
            return false;
        }
        if (!RescueTracker.tryClaim(candidate, otter.getUUID(), otter.level().getGameTime())) {
            return false;
        }
        target = candidate;
        return true;
    }

    @Nullable
    private ItemEntity findCandidate(Player owner) {
        Level level = otter.level();
        long now = level.getGameTime();
        long window = OtterConfig.RECENT_DROP_SECONDS.get() * 20L;
        double range = OtterConfig.RESCUE_RANGE.get();
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, otter.getBoundingBox().inflate(range, 8.0D, range),
                item -> item.isAlive()
                        && item.getItem().is(ModTags.RESCUABLE_TOOLS)
                        && RescueTracker.isRecentDropOf(item, owner.getUUID(), now, window)
                        && !RescueTracker.isClaimedByOther(item, otter.getUUID(), now)
                        && isSafeLoadedWater(item));
        ItemEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ItemEntity item : items) {
            double d = otter.distanceToSqr(item);
            if (d < bestDistance) {
                bestDistance = d;
                best = item;
            }
        }
        return best;
    }

    /** Safe, loaded water: the item is in water, its chunk is loaded, and no lava or hostile mob is next to it. */
    private boolean isSafeLoadedWater(ItemEntity item) {
        Level level = otter.level();
        if (!item.isInWater() || item.isInLava() || !level.isLoaded(item.blockPosition())) {
            return false;
        }
        if (!WaterHelper.isAreaSafe(level, item.blockPosition(), 2)) {
            return false;
        }
        return level.getEntitiesOfClass(Monster.class, item.getBoundingBox().inflate(6.0D)).isEmpty();
    }

    private boolean hasValidRoute(ItemEntity item, Player owner) {
        Path toItem = otter.getNavigation().createPath(item, 1);
        if (toItem == null || !toItem.canReach()) {
            return false;
        }
        Path toOwner = otter.getNavigation().createPath(owner, 2);
        return toOwner != null;
    }

    @Override
    public void start() {
        phase = otter.isCarrying() ? Phase.RETURN : Phase.DIVE;
        phaseTicks = 0;
        repathTicks = 0;
        otter.setRescueActive(true);
        otter.setDiveActive(phase == Phase.DIVE);
    }

    @Override
    public boolean canContinueToUse() {
        if (otter.isOrderedToWait() || !otter.isAlive()) {
            return false;
        }
        if (phase == Phase.DIVE) {
            return target != null && target.isAlive() && RescueTracker.isClaimedBy(target, otter.getUUID()) && phaseTicks < 400;
        }
        return otter.isCarrying() && otter.getOwnerPlayer() != null && phaseTicks < 1200;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        phaseTicks++;
        if (phase == Phase.DIVE) {
            tickDive();
        } else {
            tickReturn();
        }
    }

    private void tickDive() {
        if (target == null || !target.isAlive()) {
            return; // concurrent pickup by the owner: nothing to carry, the goal ends
        }
        RescueTracker.refreshClaim(target, otter.getUUID(), otter.level().getGameTime());
        otter.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (--repathTicks <= 0) {
            repathTicks = 10;
            otter.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), 1.3D);
        }
        if (otter.distanceToSqr(target) < 1.3D * 1.3D) {
            ItemEntity item = target;
            if (otter.pickUpRescue(item)) {
                target = null;
                phase = Phase.RETURN;
                phaseTicks = 0;
                repathTicks = 0;
                otter.setDiveActive(false);
            }
        }
    }

    private void tickReturn() {
        Player owner = otter.getOwnerPlayer();
        if (owner == null) {
            return;
        }
        otter.getLookControl().setLookAt(owner, 30.0F, 30.0F);
        if (otter.distanceToSqr(owner) < 2.2D * 2.2D) {
            otter.deliverCarried(owner);
            otter.getNavigation().stop();
            return;
        }
        if (--repathTicks <= 0) {
            repathTicks = 10;
            otter.getNavigation().moveTo(owner, 1.2D);
        }
    }

    @Override
    public void stop() {
        if (target != null) {
            RescueTracker.releaseClaim(target, otter.getUUID());
            target = null;
        }
        otter.getNavigation().stop();
        otter.setRescueActive(false);
        otter.setDiveActive(false);
        // A delivery that timed out with the owner present is settled once (no endless carrying).
        if (phase == Phase.RETURN && otter.isCarrying() && phaseTicks >= 1200) {
            otter.settleCarried(true);
        }
    }
}
