package com.huuda.otterpebble.entity.ai;

import com.huuda.otterpebble.entity.BondState;
import com.huuda.otterpebble.entity.OtterAction;
import com.huuda.otterpebble.entity.OtterEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/** Family movement: an unbonded pup stays near the closest adult otter. */
public class OtterFollowFamilyGoal extends Goal {
    private final OtterEntity pup;
    private final double speed;
    @Nullable
    private OtterEntity adult;
    private int repathTicks;

    public OtterFollowFamilyGoal(OtterEntity pup, double speed) {
        this.pup = pup;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!pup.isBaby() || pup.isTame() || pup.getBondState() != BondState.UNBONDED || pup.getAction() != OtterAction.NONE) {
            return false;
        }
        List<OtterEntity> adults = pup.level().getEntitiesOfClass(OtterEntity.class, pup.getBoundingBox().inflate(12.0D, 4.0D, 12.0D),
                o -> o != pup && o.isAlive() && !o.isBaby());
        OtterEntity closest = null;
        double best = Double.MAX_VALUE;
        for (OtterEntity o : adults) {
            double d = pup.distanceToSqr(o);
            if (d < best) {
                best = d;
                closest = o;
            }
        }
        if (closest == null || best < 5.0D * 5.0D) {
            return false;
        }
        adult = closest;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (adult == null || !adult.isAlive() || pup.getBondState() != BondState.UNBONDED) {
            return false;
        }
        double d = pup.distanceToSqr(adult);
        return d > 3.0D * 3.0D && d < 16.0D * 16.0D;
    }

    @Override
    public void start() {
        repathTicks = 0;
    }

    @Override
    public void stop() {
        adult = null;
        pup.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (adult != null && --repathTicks <= 0) {
            repathTicks = adjustedTickDelay(10);
            pup.getNavigation().moveTo(adult, speed);
        }
    }
}
