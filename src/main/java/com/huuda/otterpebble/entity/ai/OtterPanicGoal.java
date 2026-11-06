package com.huuda.otterpebble.entity.ai;

import com.huuda.otterpebble.entity.OtterEntity;
import net.minecraft.world.entity.ai.goal.PanicGoal;

/** Immediate danger (Appendix B, priority 02): hurt, burning, freezing or startled by a sprinting player. */
public class OtterPanicGoal extends PanicGoal {
    private final OtterEntity otter;

    public OtterPanicGoal(OtterEntity otter, double speed) {
        super(otter, speed);
        this.otter = otter;
    }

    @Override
    protected boolean shouldPanic() {
        return super.shouldPanic() || otter.isStartled();
    }

    @Override
    public void start() {
        otter.onDanger();
        super.start();
    }
}
