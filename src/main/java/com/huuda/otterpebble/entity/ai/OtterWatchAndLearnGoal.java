package com.huuda.otterpebble.entity.ai;

import com.huuda.otterpebble.entity.OtterAction;
import com.huuda.otterpebble.entity.OtterEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Optional;

/**
 * Learning by watching: the pup watches the adult, then tries a smaller, awkward dive
 * along its own safe route. The imitation is game fiction: no stats change.
 */
public class OtterWatchAndLearnGoal extends Goal {
    private enum Phase { WATCH, APPROACH, HOP, DONE }

    private final OtterEntity pup;
    private Phase phase = Phase.DONE;
    private int ticks;
    @Nullable
    private BlockPos bankSpot;

    public OtterWatchAndLearnGoal(OtterEntity pup) {
        this.pup = pup;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return pup.isBaby() && pup.getWatchedAdult() != null && pup.getAction() == OtterAction.NONE && !pup.isCarrying()
                && !pup.isInWater();
    }

    @Override
    public boolean canContinueToUse() {
        return phase != Phase.DONE && pup.isAlive();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        phase = Phase.WATCH;
        ticks = 0;
        bankSpot = null;
        pup.getNavigation().stop();
        pup.setAction(OtterAction.WATCH);
    }

    @Override
    public void tick() {
        ticks++;
        switch (phase) {
            case WATCH -> tickWatch();
            case APPROACH -> tickApproach();
            case HOP -> {
                if (ticks >= 24 || (pup.isInWater() && ticks > 6)) {
                    phase = Phase.DONE;
                }
            }
            default -> {
            }
        }
    }

    private void tickWatch() {
        OtterEntity adult = pup.getWatchedAdult();
        if (adult != null && adult.isAlive()) {
            pup.getLookControl().setLookAt(adult, 30.0F, 30.0F);
        }
        if (ticks < 40) {
            return;
        }
        pup.setAction(OtterAction.NONE);
        if (pup.isOrderedToWait()) {
            phase = Phase.DONE; // Wait blocks travel: the pup only watches
            return;
        }
        Optional<BlockPos> spot = WaterHelper.findNearestBankSpot(pup, 8);
        if (spot.isEmpty()) {
            phase = Phase.DONE;
            return;
        }
        Path path = pup.getNavigation().createPath(spot.get(), 0);
        if (path == null || !path.canReach()) {
            phase = Phase.DONE;
            return;
        }
        bankSpot = spot.get();
        pup.getNavigation().moveTo(path, 1.0D);
        phase = Phase.APPROACH;
        ticks = 0;
    }

    private void tickApproach() {
        if (bankSpot == null || ticks > 200) {
            phase = Phase.DONE;
            return;
        }
        double distance = pup.blockPosition().distSqr(bankSpot);
        if (distance > 1.5D && !pup.getNavigation().isDone()) {
            return;
        }
        if (distance > 4.0D) {
            phase = Phase.DONE; // the route was lost: no hop from an unchecked spot
            return;
        }
        Direction toWater = WaterHelper.waterDirectionFrom(pup.level(), pup.blockPosition());
        if (toWater == null) {
            toWater = WaterHelper.waterDirectionFrom(pup.level(), bankSpot);
        }
        if (toWater == null) {
            phase = Phase.DONE;
            return;
        }
        pup.getNavigation().stop();
        pup.faceDirection(toWater);
        pup.setDeltaMovement(toWater.getStepX() * 0.25D, 0.32D, toWater.getStepZ() * 0.25D);
        pup.setAction(OtterAction.AWKWARD_DIVE);
        phase = Phase.HOP;
        ticks = 0;
    }

    @Override
    public void stop() {
        phase = Phase.DONE;
        bankSpot = null;
        pup.clearWatchedAdult();
        OtterAction action = pup.getAction();
        if (action == OtterAction.WATCH || action == OtterAction.AWKWARD_DIVE) {
            pup.setAction(OtterAction.NONE);
        }
    }
}
