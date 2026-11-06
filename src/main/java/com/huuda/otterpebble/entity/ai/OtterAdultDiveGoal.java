package com.huuda.otterpebble.entity.ai;

import com.huuda.otterpebble.config.OtterConfig;
import com.huuda.otterpebble.entity.OtterAction;
import com.huuda.otterpebble.entity.OtterEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/** An adult's confident dive from the bank. Nearby pups watch it (learning by watching). */
public class OtterAdultDiveGoal extends Goal {
    private final OtterEntity otter;
    @Nullable
    private Direction direction;
    private int ticks;

    public OtterAdultDiveGoal(OtterEntity otter) {
        this.otter = otter;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!OtterConfig.LEARNING_BY_WATCHING.get() || otter.isBaby() || otter.getAction() != OtterAction.NONE || !otter.onGround()
                || otter.isInWater() || otter.isOrderedToWait() || otter.isCarrying() || otter.isRescueActive() || !otter.isDiveReady()) {
            return false;
        }
        boolean pupsNearby = !nearbyPups().isEmpty();
        if (otter.getRandom().nextInt(reducedTickDelay(pupsNearby ? 120 : 600)) != 0) {
            return false;
        }
        WaterHelper.Entry entry = WaterHelper.findEntry(otter, 2, 2);
        if (entry == null) {
            return false;
        }
        direction = entry.direction();
        return true;
    }

    private List<OtterEntity> nearbyPups() {
        return otter.level().getEntitiesOfClass(OtterEntity.class, otter.getBoundingBox().inflate(10.0D, 4.0D, 10.0D),
                o -> o != otter && o.isAlive() && o.isBaby());
    }

    @Override
    public boolean canContinueToUse() {
        return ticks < 20 && otter.getAction() == OtterAction.DIVE;
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
            otter.setDeltaMovement(direction.getStepX() * 0.45D, 0.42D, direction.getStepZ() * 0.45D);
        }
        otter.setAction(OtterAction.DIVE);
        for (OtterEntity pup : nearbyPups()) {
            pup.watchAdultDive(otter);
        }
    }

    @Override
    public void tick() {
        ticks++;
    }

    @Override
    public void stop() {
        direction = null;
        otter.startDiveCooldown();
        if (otter.getAction() == OtterAction.DIVE) {
            otter.setAction(OtterAction.NONE);
        }
    }
}
