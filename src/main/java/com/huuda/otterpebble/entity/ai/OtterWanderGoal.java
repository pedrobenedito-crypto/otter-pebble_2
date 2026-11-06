package com.huuda.otterpebble.entity.ai;

import com.huuda.otterpebble.entity.OtterAction;
import com.huuda.otterpebble.entity.OtterEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Ambient wandering along the river: mostly in the water, sometimes up on the bank. Wait blocks it. */
public class OtterWanderGoal extends RandomStrollGoal {
    private final OtterEntity otter;

    public OtterWanderGoal(OtterEntity otter, double speed) {
        super(otter, speed, 100);
        this.otter = otter;
    }

    @Override
    public boolean canUse() {
        return !otter.isOrderedToWait() && otter.getAction() == OtterAction.NONE && !otter.isCarrying()
                && !otter.isRescueActive() && !otter.hasPartner() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !otter.isOrderedToWait() && otter.getAction() == OtterAction.NONE && super.canContinueToUse();
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        if (otter.isInWater()) {
            Vec3 swim = BehaviorUtils.getRandomSwimmablePos(otter, 8, 3);
            if (swim != null && otter.getRandom().nextFloat() < 0.75F) {
                return swim;
            }
            return LandRandomPos.getPos(otter, 8, 4);
        }
        Vec3 land = LandRandomPos.getPos(otter, 8, 4);
        return land != null ? land : DefaultRandomPos.getPos(otter, 8, 4);
    }
}
