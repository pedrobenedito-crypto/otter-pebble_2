package com.huuda.otterpebble.client;

import com.huuda.otterpebble.OtterPebbleMod;
import com.huuda.otterpebble.entity.OtterEntity;
import com.huuda.otterpebble.entity.OtterVariant;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * Model and animation files made for Blockbench (GeckoLib format): geo/entity/otter.geo.json and
 * animations/entity/otter.animation.json. One silhouette, two ages: pups use the same model with a larger head.
 */
public class OtterModel extends DefaultedEntityGeoModel<OtterEntity> {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[OtterVariant.values().length];

    static {
        for (OtterVariant variant : OtterVariant.values()) {
            TEXTURES[variant.ordinal()] = OtterPebbleMod.id("textures/entity/otter/" + variant.textureName() + ".png");
        }
    }

    public OtterModel() {
        // head turning is applied below, on top of the animated head pose
        super(OtterPebbleMod.id("otter"), false);
    }

    @Override
    public ResourceLocation getTextureResource(OtterEntity otter) {
        return TEXTURES[otter.getVariant().ordinal()];
    }

    @Override
    public void setCustomAnimations(OtterEntity otter, long instanceId, AnimationState<OtterEntity> animationState) {
        super.setCustomAnimations(otter, instanceId, animationState);

        // Bones are shared between all otters, so every value below is written on every frame.
        CoreGeoBone pebble = getAnimationProcessor().getBone("pebble");
        if (pebble != null) {
            pebble.setHidden(!otter.isPebbleVisible());
        }

        CoreGeoBone head = getAnimationProcessor().getBone("head");
        if (head != null) {
            float scale = otter.isBaby() ? 1.35F : 1.0F;
            head.setScaleX(scale);
            head.setScaleY(scale);
            head.setScaleZ(scale);
            EntityModelData data = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            boolean floatingOnBack = otter.isInWater() && !animationState.isMoving();
            if (data != null && otter.allowsHeadLook() && !floatingOnBack) {
                head.setRotX(head.getRotX() + data.headPitch() * Mth.DEG_TO_RAD * 0.8F);
                head.setRotY(head.getRotY() + Mth.clamp(data.netHeadYaw(), -60.0F, 60.0F) * Mth.DEG_TO_RAD);
            }
        }

        CoreGeoBone root = getAnimationProcessor().getBone("root");
        if (root != null && otter.isInWater() && otter.allowsHeadLook()) {
            float pitch = Mth.lerp(animationState.getPartialTick(), otter.swimPitchO, otter.swimPitch);
            root.setRotX(root.getRotX() - pitch * Mth.DEG_TO_RAD);
        }
    }
}
