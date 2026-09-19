package dev.ashenthefox.skibidi.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.ashenthefox.skibidi.SkibidiToiletMod;
import dev.ashenthefox.skibidi.entity.AbstractSkibidiEntity;
import dev.ashenthefox.skibidi.entity.GToiletEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class SkibidiRenderer<T extends AbstractSkibidiEntity>
    extends MobRenderer<T, SkibidiRenderState, EmptySkibidiModel<SkibidiRenderState>> {

    private static final Identifier BASE_TEXTURE =
        SkibidiToiletMod.id("textures/entity/skibidi_toilet.png");
    private static final Identifier FALLBACK_TEXTURE =
        SkibidiToiletMod.id("textures/entity/skibidi_head.png");
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

    public SkibidiRenderer(
        EntityRendererProvider.Context context,
        float modelScale,
        float shadowRadius,
        SkibidiVariant variant
    ) {
        super(context, new EmptySkibidiModel<>(), shadowRadius);
        this.modelScale = modelScale;
        this.variant = variant;

        // Intentionally DO NOT install SkibidiMeshLayer here.
        // Rendering the glTF mesh as a LivingEntity layer applies vanilla
        // ModelPart transforms that the uploaded mesh does not use.
    }

    @Override
    public SkibidiRenderState createRenderState() {
        return new SkibidiRenderState();
    }

    @Override
    public void extractRenderState(T entity, SkibidiRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.laserFiring = entity instanceof GToiletEntity gToilet && gToilet.isLaserFiring();
    }

    @Override
    public Identifier getTextureLocation(SkibidiRenderState state) {
        // The EmptySkibidiModel has no geometry, but MobRenderer still asks
        // for a valid texture while processing normal LivingEntity behavior.
        return FALLBACK_TEXTURE;
    }

    @Override
    public void submit(
        SkibidiRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState camera
    ) {
        if (!state.isInvisible) {
            poseStack.pushPose();

            // Use only the entity's world/body transform. The packed glTF is
            // already Y-up with its feet at Y=0, so it must not be pushed
            // through vanilla ModelPart's (-X,-Y,+Z)/-1.501 convention.
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));

            if (state.deathTime > 0.0F) {
                float fall = (state.deathTime - 1.0F) / 20.0F * 1.6F;
                fall = Mth.sqrt(fall);
                if (fall > 1.0F) {
                    fall = 1.0F;
                }
                poseStack.mulPose(Axis.ZP.rotationDegrees(fall * 90.0F));
            }

            float walking = Mth.clamp(state.walkAnimationSpeed, 0.0F, 1.0F);
            float bob = Mth.sin(state.ageInTicks * (variant == SkibidiVariant.KING ? 0.10F : 0.14F))
                * (variant == SkibidiVariant.KING ? 0.012F : 0.007F)
                + Mth.sin(state.walkAnimationPos * 0.65F) * walking * 0.010F;

            poseStack.translate(0.0F, bob, 0.0F);

            int light = state.lightCoords;
            int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);

            // Keep the original supplied head rigidly connected to the toilet.
            // This prevents the previous visible seam/neck tearing.
            float headYaw = 0.0F;
            float headPitch = 0.0F;

            submitGroup(
                submitNodeCollector, poseStack, BASE_RENDER,
                SkibidiMesh.TOILET, light, overlay, 0.0F, 0.0F, false
            );
            submitGroup(
                submitNodeCollector, poseStack, BASE_RENDER,
                SkibidiMesh.HEAD_BASE, light, overlay, headYaw, headPitch, true
            );

            if (variant == SkibidiVariant.G_TOILET_2 || variant == SkibidiVariant.G_TOILET_3) {
                submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    METAL_RENDER,
                    (pose, buffer) -> SkibidiAttachments.renderMetal(
                        variant, pose, buffer, light, overlay, modelScale, headYaw, headPitch
                    )
                );
                submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    DARK_RENDER,
                    (pose, buffer) -> SkibidiAttachments.renderDark(
                        variant, pose, buffer, light, overlay, modelScale, headYaw, headPitch
                    )
                );
            }

            if (variant.hasGlowingEyes()) {
                submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    YELLOW_RENDER,
                    (pose, buffer) -> SkibidiAttachments.renderYellow(
                        variant,
                        state.laserFiring,
                        pose,
                        buffer,
                        FULL_BRIGHT,
                        overlay,
                        modelScale,
                        headYaw,
                        headPitch
                    )
                );
            }

            poseStack.popPose();
        }

        // Empty model => no duplicate body is drawn here. We still retain
        // vanilla LivingEntity handling such as name tags, leashes and shadow.
        super.submit(state, poseStack, submitNodeCollector, camera);
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
