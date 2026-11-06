package com.huuda.otterpebble.entity.ai;

import com.huuda.otterpebble.entity.OtterAction;
import com.huuda.otterpebble.entity.OtterEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * A quiet moment together (requested social behaviour):
 * Join with one mutual partner lock, float with one existing forepaw each, release honouring each latest order.
 * Game proposal: safe, bounded drift. The leader owns the shared anchor; the follower reads it from the leader.
 */
public class OtterPairFloatGoal extends Goal {
    private static final double HALF_GAP = 0.3D;
    private static final double MAX_DRIFT = 3.0D;

    private final OtterEntity otter;
    private int ticks;
    private int duration;
    @Nullable
    private Vec3 start;
    private Vec3 drift = Vec3.ZERO;

    public OtterPairFloatGoal(OtterEntity otter) {
        this.otter = otter;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (otter.hasPartner()) {
            OtterEntity partner = otter.getPartner();
            return partner != null && otter.isPartnerOf(partner) && otter.isInWater() && !otter.isOrderedToWait()
                    && !otter.isCarrying();
        }
        if (!otter.canJoinPair() || otter.getRandom().nextInt(reducedTickDelay(300)) != 0) {
            return false;
        }
        List<OtterEntity> others = otter.level().getEntitiesOfClass(OtterEntity.class, otter.getBoundingBox().inflate(6.0D, 2.0D, 6.0D),
                o -> o != otter && o.isAlive() && o.canJoinPair());
        OtterEntity closest = null;
        double best = Double.MAX_VALUE;
        for (OtterEntity o : others) {
            double d = otter.distanceToSqr(o);
            if (d < best) {
                best = d;
                closest = o;
            }
        }
        return closest != null && otter.tryLockPartner(closest);
    }

    @Override
    public boolean canContinueToUse() {
        OtterEntity partner = otter.getPartner();
        return partner != null && otter.isPartnerOf(partner) && otter.distanceToSqr(partner) < 4.0D * 4.0D
                && otter.isInWater() && !otter.isOrderedToWait() && !otter.isCarrying() && otter.hurtTime == 0
                && (!otter.isPairLeader() || ticks < duration)
                // no stuck pairs: after a short grace period the partner must really be floating too
                && (ticks < 40 || partner.getAction().isPaired());
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        ticks = 0;
        duration = 400 + otter.getRandom().nextInt(400);
        otter.getNavigation().stop();
        OtterEntity partner = otter.getPartner();
        if (otter.isPairLeader() && partner != null) {
            Vec3 anchor = otter.position().add(partner.position()).scale(0.5D);
            start = anchor;
            otter.setPairAnchor(anchor, otter.getYRot());
            double angle = otter.getRandom().nextDouble() * Math.PI * 2.0D;
            drift = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle)).scale(0.008D);
        }
    }

    @Override
    public void tick() {
        ticks++;
        OtterEntity partner = otter.getPartner();
        if (partner == null) {
            return;
        }
        OtterEntity leader = otter.isPairLeader() ? otter : partner;
        if (otter.isPairLeader()) {
            updateDrift();
        }
        Vec3 anchor = leader.getPairAnchor();
        if (anchor == null) {
            return;
        }
        float yaw = leader.getPairYaw();
        // right side of an otter facing 'yaw' (Minecraft yaw 0 faces +Z, its right is -X)
        Vec3 right = new Vec3(-Mth.cos(yaw * Mth.DEG_TO_RAD), 0.0D, -Mth.sin(yaw * Mth.DEG_TO_RAD));
        // the leader floats on the left of the pair, the follower on the right
        Vec3 slot = anchor.add(right.scale(otter.isPairLeader() ? -HALF_GAP : HALF_GAP));
        Vec3 delta = slot.subtract(otter.position());
        Vec3 motion = otter.getDeltaMovement();
        otter.setDeltaMovement(Mth.clamp(delta.x * 0.15D, -0.06D, 0.06D), motion.y, Mth.clamp(delta.z * 0.15D, -0.06D, 0.06D));
        otter.setYRot(yaw);
        otter.yBodyRot = yaw;
        otter.yHeadRot = yaw;
        // The leader's partner is on its right. Lying on its back, the LEFT forepaw is the one on that side.
        otter.setAction(otter.isPairLeader() ? OtterAction.PAIR_LEFT_PAW : OtterAction.PAIR_RIGHT_PAW);
    }

    private void updateDrift() {
        Vec3 anchor = otter.getPairAnchor();
        if (anchor == null || start == null) {
            return;
        }
        Vec3 next = anchor.add(drift);
        BlockPos pos = BlockPos.containing(next.x, next.y, next.z);
        boolean safe = WaterHelper.isWater(otter.level(), pos) && WaterHelper.isAreaSafe(otter.level(), pos, 1)
                && next.distanceToSqr(start) < MAX_DRIFT * MAX_DRIFT;
        if (safe) {
            otter.setPairAnchor(next, otter.getPairYaw());
        } else {
            drift = drift.scale(-1.0D); // bounded drift: turn back
        }
    }

    @Override
    public void stop() {
        start = null;
        // Release: both locks are cleared together, then each otter honours its own latest order.
        otter.releasePair();
        otter.startPairCooldown();
        if (otter.getAction().isPaired()) {
            otter.setAction(OtterAction.NONE);
        }
    }
}
