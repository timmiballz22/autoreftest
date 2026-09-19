package dev.ashenthefox.skibidi.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ashenthefox.skibidi.SkibidiToiletMod;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class SkibidiMeshLayer<
    S extends LivingEntityRenderState,
    M extends EntityModel<? super S>
> extends RenderLayer<S, M> {

    private static final Identifier BASE_TEXTURE =
        SkibidiToiletMod.id("textures/entity/skibidi_toilet.png");
    private static final Identifier YELLOW_TEXTURE =
        SkibidiToiletMod.id("textures/entity/solid_yellow.png");
    private static final Identifier METAL_TEXTURE =
        SkibidiToiletMod.id("textures/entity/solid_metal.png");
    private static final Identifier DARK_TEXTURE =
        SkibidiToiletMod.id("textures/entity/solid_dark.png");

    private static final RenderType BASE_RENDER = RenderTypes.entityCutoutNoCull(BASE_TEXTURE);
    private static final RenderType YELLOW_RENDER = RenderTypes.entityCutoutNoCull(YELLOW_TEXTURE);
    private static final RenderType METAL_RENDER = RenderTypes.entityCutoutNoCull(METAL_TEXTURE);
    private static final RenderType DARK_RENDER = RenderTypes.entityCutoutNoCull(DARK_TEXTURE);
    private static final int FULL_BRIGHT = 0x00F000F0;

    private final float modelScale;
    private final SkibidiVariant variant;

    public SkibidiMeshLayer(
        RenderLayerParent<S, M> parent,
        float modelScale,
        SkibidiVariant variant
    ) {
        super(parent);
        this.modelScale = modelScale;
        this.variant = variant;
    }

    @Override
    public void submit(
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int light,
        S state,
        float relativeHeadYaw,
        float headPitch
    ) {
        if (state.isInvisible) {
            return;
        }

        float walking = Mth.clamp(state.walkAnimationSpeed, 0.0F, 1.0F);
        float bob = Mth.sin(state.ageInTicks * (variant == SkibidiVariant.KING ? 0.10F : 0.14F))
            * (variant == SkibidiVariant.KING ? 0.012F : 0.007F)
            + Mth.sin(state.walkAnimationPos * 0.65F) * walking * 0.010F;

        // Preserve the exact uploaded model's rigid head/toilet alignment.
        // Independently rotating the flattened head subset caused visible tearing and gaps.
        float yaw = 0.0F;
        float pitch = 0.0F;
        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);

        poseStack.pushPose();
        poseStack.translate(0.0F, -bob, 0.0F);

        submitGroup(submitNodeCollector, poseStack, BASE_RENDER, SkibidiMesh.TOILET,
            light, overlay, 0.0F, 0.0F, false);
        submitGroup(submitNodeCollector, poseStack, BASE_RENDER, SkibidiMesh.HEAD_BASE,
            light, overlay, yaw, pitch, true);

        if (variant == SkibidiVariant.G_TOILET_2 || variant == SkibidiVariant.G_TOILET_3) {
            submitNodeCollector.submitCustomGeometry(
                poseStack,
                METAL_RENDER,
                (pose, buffer) -> SkibidiAttachments.renderMetal(
                    variant, pose, buffer, light, overlay, modelScale, yaw, pitch
                )
            );
            submitNodeCollector.submitCustomGeometry(
                poseStack,
                DARK_RENDER,
                (pose, buffer) -> SkibidiAttachments.renderDark(
                    variant, pose, buffer, light, overlay, modelScale, yaw, pitch
                )
            );
        }

        // G-Toilet 1/2/3 all receive full-bright yellow eyes.
        if (variant.hasGlowingEyes()) {
            submitNodeCollector.submitCustomGeometry(
                poseStack,
                YELLOW_RENDER,
                (pose, buffer) -> SkibidiAttachments.renderYellow(
                    variant, pose, buffer, FULL_BRIGHT, overlay, modelScale, yaw, pitch
                )
            );
        }

        poseStack.popPose();
    }

    private void submitGroup(
        SubmitNodeCollector collector,
        PoseStack stack,
        RenderType type,
        int group,
        int light,
        int overlay,
        float yaw,
        float pitch,
        boolean head
    ) {
        collector.submitCustomGeometry(
            stack,
            type,
            (pose, buffer) -> SkibidiMesh.renderGroup(
                group, pose, buffer, light, overlay, modelScale, yaw, pitch, head
            )
        );
    }
}
