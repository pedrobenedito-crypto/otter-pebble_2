package com.huuda.otterpebble.client;

import com.huuda.otterpebble.entity.OtterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/** Mouth carry: the rescued tool is drawn crosswise in the otter's mouth (the "item_mouth" bone). */
public class OtterMouthItemLayer extends BlockAndItemGeoLayer<OtterEntity> {
    private static final String MOUTH_BONE = "item_mouth";

    public OtterMouthItemLayer(GeoRenderer<OtterEntity> renderer) {
        super(renderer);
    }

    @Override
    protected ItemStack getStackForBone(GeoBone bone, OtterEntity otter) {
        if (!MOUTH_BONE.equals(bone.getName())) {
            return null;
        }
        ItemStack stack = otter.getCarriedForRender();
        return stack.isEmpty() ? null : stack;
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, OtterEntity otter) {
        return ItemDisplayContext.GROUND;
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, OtterEntity otter,
                                      MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        // handle across the mouth: the item sprite's diagonal turned level
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
        poseStack.scale(0.9F, 0.9F, 0.9F);
        super.renderStackForBone(poseStack, bone, stack, otter, bufferSource, partialTick, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
