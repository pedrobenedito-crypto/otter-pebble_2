package com.huuda.otterpebble.entity.ai;

import com.huuda.otterpebble.config.OtterConfig;
import com.huuda.otterpebble.entity.OtterEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;

import java.util.EnumSet;

/** Follow order: walk or swim after the owner; catch up next to them when left far behind. Owner absent: nothing changes. */
public class OtterFollowOwnerGoal extends Goal {
    private static final double STOP_DISTANCE = 2.5D;

    private final OtterEntity otter;
    private final double speed;
    private Player owner;
    private int repathTicks;

    public OtterFollowOwnerGoal(OtterEntity otter, double speed) {
        this.otter = otter;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!otter.isTame() || otter.isWaiting() || otter.isRescueActive() || otter.isLeashed() || otter.isPassenger()) {
            return false;
        }
        Player player = otter.getOwnerPlayer();
        if (player == null) {
            return false;
        }
        double start = OtterConfig.FOLLOW_START_DISTANCE.get();
        if (otter.distanceToSqr(player) < start * start) {
            return false;
        }
        this.owner = player;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return owner != null && owner.isAlive() && !otter.isWaiting() && !otter.isRescueActive()
                && otter.distanceToSqr(owner) > STOP_DISTANCE * STOP_DISTANCE && otter.getOwnerPlayer() == owner;
    }

    @Override
    public void start() {
        repathTicks = 0;
        otter.releasePair();
    }

    @Override
    public void stop() {
        owner = null;
        otter.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (owner == null) {
            return;
        }
        otter.getLookControl().setLookAt(owner, 10.0F, otter.getMaxHeadXRot());
        if (--repathTicks > 0) {
            return;
        }
        repathTicks = adjustedTickDelay(10);
        int teleport = OtterConfig.TELEPORT_DISTANCE.get();
        if (teleport > 0 && otter.distanceToSqr(owner) >= (double) teleport * teleport) {
            tryCatchUp();
        } else {
            otter.getNavigation().moveTo(owner, speed);
        }
    }

    private void tryCatchUp() {
        BlockPos center = owner.blockPosition();
        for (int i = 0; i < 10; i++) {
            int dx = otter.getRandom().nextInt(7) - 3;
            int dy = otter.getRandom().nextInt(3) - 1;
            int dz = otter.getRandom().nextInt(7) - 3;
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
                continue;
            }
            BlockPos pos = center.offset(dx, dy, dz);
            if (canStandAt(otter.level(), pos)) {
                otter.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, otter.getYRot(), otter.getXRot());
                otter.getNavigation().stop();
                return;
            }
        }
    }

    private boolean canStandAt(LevelReader level, BlockPos pos) {
        boolean land = WaterHelper.isStandable(level, pos);
        boolean water = WaterHelper.isWater(level, pos) && WaterHelper.isPassable(level, pos.above());
        return (land || water) && WaterHelper.isAreaSafe(level, pos, 1)
                && level.noCollision(otter, otter.getBoundingBox().move(pos.getX() + 0.5D - otter.getX(),
                pos.getY() - otter.getY(), pos.getZ() + 0.5D - otter.getZ()));
    }
}
