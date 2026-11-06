package com.huuda.otterpebble.entity.ai;

import com.huuda.otterpebble.entity.OtterEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

/** Walks like a land animal on banks and swims in three dimensions in water. */
public class OtterMoveControl extends MoveControl {
    private static final float SWIM_FACTOR = 0.5F;
    private final OtterEntity otter;

    public OtterMoveControl(OtterEntity otter) {
        super(otter);
        this.otter = otter;
    }

    @Override
    public void tick() {
        if (!otter.isInWater()) {
            super.tick();
            return;
        }
        if (this.operation == MoveControl.Operation.MOVE_TO && !otter.getNavigation().isDone()) {
            double dx = this.wantedX - otter.getX();
            double dy = this.wantedY - otter.getY();
            double dz = this.wantedZ - otter.getZ();
            if (dx * dx + dy * dy + dz * dz < 2.5E-7D) {
                otter.setZza(0.0F);
                return;
            }
            float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
            otter.setYRot(this.rotlerp(otter.getYRot(), yaw, 12.0F));
            otter.yBodyRot = otter.getYRot();
            otter.yHeadRot = otter.getYRot();

            float speed = (float) (this.speedModifier * otter.getAttributeValue(Attributes.MOVEMENT_SPEED)) * SWIM_FACTOR;
            otter.setSpeed(speed);
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            float pitch = (float) Mth.clamp(Mth.atan2(dy, horizontal), -1.2D, 1.2D);
            otter.setZza(Mth.cos(pitch) * speed);
            otter.setYya(Mth.sin(pitch) * speed);

            // Hop out of the water onto a bank instead of pushing against it.
            if (otter.horizontalCollision && dy > -0.3D) {
                otter.setDeltaMovement(otter.getDeltaMovement().add(0.0D, 0.06D, 0.0D));
                if (otter.waterDepthAtFeet() < 0.5D) {
                    otter.getJumpControl().jump();
                }
            }
        } else {
            otter.setSpeed(0.0F);
            otter.setXxa(0.0F);
            otter.setYya(0.0F);
            otter.setZza(0.0F);
        }
    }
}
