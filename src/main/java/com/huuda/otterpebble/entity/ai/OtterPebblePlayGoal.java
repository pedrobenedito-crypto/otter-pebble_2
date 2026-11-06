package com.huuda.otterpebble.entity.ai;

import com.huuda.otterpebble.config.OtterConfig;
import com.huuda.otterpebble.entity.OtterAction;
import com.huuda.otterpebble.entity.OtterEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * The chosen pebble: pass, fumble, catch, hold. One visual prop, no spawned item.
 * Pebble play yields to movement or rescue (both run at a higher priority).
 */
public class OtterPebblePlayGoal extends Goal {
    private final OtterEntity otter;
    private int ticks;

    public OtterPebblePlayGoal(OtterEntity otter) {
        this.otter = otter;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!OtterConfig.PEBBLE_PLAY.get() || !otter.hasPebble() || otter.getAction() != OtterAction.NONE || otter.isCarrying()
                || otter.isRescueActive() || !otter.isPebbleReady() || !otter.getNavigation().isDone()
                || otter.getRandom().nextInt(reducedTickDelay(240)) != 0) {
            return false;
        }
        if (!otter.isInWater() && !otter.onGround()) {
            return false;
        }
        Player owner = otter.getOwnerPlayer();
        // While following, only play when the owner is close and not walking away.
        return otter.isOrderedToWait() || owner == null || otter.distanceToSqr(owner) < 5.0D * 5.0D;
    }

    @Override
    public boolean canContinueToUse() {
        return ticks > 0 && otter.getAction() == OtterAction.PEBBLE_PLAY && !otter.isCarrying() && otter.hurtTime == 0
                && otter.getNavigation().isDone();
    }

    @Override
    public void start() {
        ticks = 160 + otter.getRandom().nextInt(80);
        otter.getNavigation().stop();
        otter.setAction(OtterAction.PEBBLE_PLAY);
    }

    @Override
    public void tick() {
        ticks--;
    }

    @Override
    public void stop() {
        otter.startPebbleCooldown();
        if (otter.getAction() == OtterAction.PEBBLE_PLAY) {
            otter.setAction(OtterAction.NONE);
        }
    }
}
