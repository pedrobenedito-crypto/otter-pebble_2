package com.huuda.otterpebble.entity.ai;

import com.huuda.otterpebble.entity.OtterEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Wait: stay at the safe Wait anchor and walk back to it after any displacement (legal state). */
public class OtterWaitAnchorGoal extends Goal {
    private final OtterEntity otter;
    private int repathTicks;
    private int stuckTicks;

    public OtterWaitAnchorGoal(OtterEntity otter) {
        this.otter = otter;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    private double distanceToAnchorSqr() {
        BlockPos anchor = otter.getWaitAnchor();
        return anchor == null ? 0.0D : otter.distanceToSqr(Vec3.atBottomCenterOf(anchor));
    }

    @Override
    public boolean canUse() {
        return otter.isOrderedToWait() && otter.getWaitAnchor() != null && distanceToAnchorSqr() > 1.5D * 1.5D;
    }

    @Override
    public boolean canContinueToUse() {
        return otter.isOrderedToWait() && otter.getWaitAnchor() != null && distanceToAnchorSqr() > 0.6D * 0.6D
                && stuckTicks < 200;
    }

    @Override
    public void start() {
        repathTicks = 0;
        stuckTicks = 0;
    }

    @Override
    public void tick() {
        BlockPos anchor = otter.getWaitAnchor();
        if (anchor == null) {
            return;
        }
        stuckTicks++;
        if (--repathTicks <= 0) {
            repathTicks = 20;
            otter.getNavigation().moveTo(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D, 1.0D);
        }
    }

    @Override
    public void stop() {
        otter.getNavigation().stop();
        if (stuckTicks >= 200) {
            // Unreachable anchor: wait at the nearest legal spot instead of getting stuck.
            otter.setWaitAnchor(otter.findSafeAnchor(otter.blockPosition()));
        }
    }
}
