package com.huuda.otterpebble.entity.ai;

import com.huuda.otterpebble.config.OtterConfig;
import com.huuda.otterpebble.entity.OtterAction;
import com.huuda.otterpebble.entity.OtterEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Post-swim groom: shake, then rub the face. Follow can interrupt (higher priority goal). */
public class OtterGroomGoal extends Goal {
    private static final int DURATION = 60; // matches animation.otter.groom (3 s)

    private final OtterEntity otter;
    private int ticks;

    public OtterGroomGoal(OtterEntity otter) {
        this.otter = otter;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return OtterConfig.POST_SWIM_GROOM.get() && otter.getAction() == OtterAction.NONE && !otter.isInWater()
                && otter.onGround() && otter.getTicksOutOfWater() > 4 && otter.getTicksOutOfWater() < 60
                && otter.getLastSwimTicks() >= 100 && otter.isGroomReady() && !otter.isCarrying()
                && !otter.isRescueActive();
    }

    @Override
    public boolean canContinueToUse() {
        return ticks > 0 && !otter.isInWater() && otter.getAction() == OtterAction.GROOM;
    }

    @Override
    public void start() {
        ticks = DURATION;
        otter.getNavigation().stop();
        otter.markGroomed();
        otter.startGroomCooldown();
        otter.setAction(OtterAction.GROOM);
        otter.playSound(SoundEvents.WOLF_SHAKE, 0.5F, 1.4F);
    }

    @Override
    public void tick() {
        ticks--;
        if (ticks > DURATION - 20 && ticks % 3 == 0 && otter.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SPLASH, otter.getX(), otter.getY() + otter.getBbHeight() * 0.6D, otter.getZ(),
                    6, otter.getBbWidth() * 0.6D, 0.15D, otter.getBbWidth() * 0.6D, 0.05D);
        }
    }

    @Override
    public void stop() {
        if (otter.getAction() == OtterAction.GROOM) {
            otter.setAction(OtterAction.NONE);
        }
    }
}
