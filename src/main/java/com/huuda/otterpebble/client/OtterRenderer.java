package com.huuda.otterpebble.client;

import com.huuda.otterpebble.entity.OtterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class OtterRenderer extends GeoEntityRenderer<OtterEntity> {
    /** Pup scale; the head is enlarged separately in {@link OtterModel}. */
    private static final float PUP_SCALE = 0.55F;

    public OtterRenderer(EntityRendererProvider.Context context) {
        super(context, new OtterModel());
        this.shadowRadius = 0.4F;
        addRenderLayer(new OtterMouthItemLayer(this));
    }

    @Override
    public void preRender(PoseStack poseStack, OtterEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        if (!isReRender && animatable.isBaby()) {
            poseStack.scale(PUP_SCALE, PUP_SCALE, PUP_SCALE);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, red, green, blue, alpha);
    }
}
