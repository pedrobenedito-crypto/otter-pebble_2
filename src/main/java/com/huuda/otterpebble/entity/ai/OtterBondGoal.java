package com.huuda.otterpebble.entity.ai;

import com.huuda.otterpebble.entity.BondState;
import com.huuda.otterpebble.entity.OtterEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Curious: the pup watches the calm player from a little distance.
 * Inspecting: the pup comes close and sniffs the offered fish (the commit itself happens in the entity tick).
 */
public class OtterBondGoal extends Goal {
    private final OtterEntity otter;
    private int repathTicks;

    public OtterBondGoal(OtterEntity otter) {
        this.otter = otter;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        BondState state = otter.getBondState();
        return otter.isBaby() && !otter.isTame() && (state == BondState.CURIOUS || state == BondState.INSPECTING)
                && otter.getBondFocusPlayer() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        otter.getNavigation().stop();
        repathTicks = 0;
    }

    @Override
    public void stop() {
        otter.getNavigation().stop();
    }

    @Override
    public void tick() {
        Player player = otter.getBondFocusPlayer();
        if (player == null) {
            return;
        }
        otter.getLookControl().setLookAt(player, 30.0F, 30.0F);
        if (--repathTicks > 0) {
            return;
        }
        repathTicks = 10;
        double distance = otter.distanceToSqr(player);
        if (otter.getBondState() == BondState.INSPECTING) {
            if (distance > 1.6D * 1.6D) {
                otter.getNavigation().moveTo(player, 0.6D);
            } else {
                otter.getNavigation().stop();
            }
        } else if (distance < 2.5D * 2.5D) {
            // keep a respectful distance while curious
            Vec3 away = otter.position().subtract(player.position()).normalize().scale(2.0D).add(otter.position());
            otter.getNavigation().moveTo(away.x, away.y, away.z, 0.7D);
        } else {
            otter.getNavigation().stop();
        }
    }
}
