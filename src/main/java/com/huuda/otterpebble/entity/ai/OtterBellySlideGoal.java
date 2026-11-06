package com.huuda.otterpebble.entity.ai;

import com.huuda.otterpebble.config.OtterConfig;
import com.huuda.otterpebble.entity.OtterAction;
import com.huuda.otterpebble.entity.OtterEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/** A short belly slide: only on a clear bank with a safe landing, ending in an ordinary water entry. */
public class OtterBellySlideGoal extends Goal {
    private final OtterEntity otter;
    @Nullable
    private Direction direction;
    private int ticks;

    public OtterBellySlideGoal(OtterEntity otter) {
        this.otter = otter;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!OtterConfig.BELLY_SLIDE.get() || otter.getAction() != OtterAction.NONE || !otter.onGround() || otter.isInWater()
                || otter.isOrderedToWait() || otter.isCarrying() || otter.isRescueActive() || !otter.isSlideReady()
                || otter.getRandom().nextInt(reducedTickDelay(150)) != 0) {
            return false;
        }
        WaterHelper.Entry entry = WaterHelper.findEntry(otter, 3, 1);
        if (entry == null) {
            return false;
        }
        direction = entry.direction();
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return direction != null && ticks < 30 && !otter.isInWater() && otter.getAction() == OtterAction.SLIDE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        ticks = 0;
        otter.getNavigation().stop();
        if (direction != null) {
            otter.faceDirection(direction);
        }
        otter.setAction(OtterAction.SLIDE);
    }

    @Override
    public void tick() {
        ticks++;
        if (direction == null) {
            return;
        }
        otter.faceDirection(direction);
        Vec3 motion = otter.getDeltaMovement();
        otter.setDeltaMovement(direction.getStepX() * 0.28D, motion.y, direction.getStepZ() * 0.28D);
    }

    @Override
    public void stop() {
        direction = null;
        otter.startSlideCooldown();
        if (otter.getAction() == OtterAction.SLIDE) {
            otter.setAction(OtterAction.NONE);
        }
    }
}
