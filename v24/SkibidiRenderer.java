package dev.ashenthefox.skibidi.client.render;

import dev.ashenthefox.skibidi.SkibidiToiletMod;
import dev.ashenthefox.skibidi.entity.AbstractSkibidiEntity;
import dev.ashenthefox.skibidi.entity.GToiletEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class SkibidiRenderer<T extends AbstractSkibidiEntity>
    extends MobRenderer<T, SkibidiRenderState, EmptySkibidiModel<SkibidiRenderState>> {

    private static final Identifier FALLBACK_TEXTURE =
        SkibidiToiletMod.id("textures/entity/skibidi_head.png");

    public SkibidiRenderer(
        EntityRendererProvider.Context context,
        float modelScale,
        float shadowRadius,
        SkibidiVariant variant
    ) {
        super(context, new EmptySkibidiModel<>(), shadowRadius);

        // Keep the normal Minecraft LivingEntity transform/pose pipeline.
        // The custom mesh layer only replaces the geometry.
        this.addLayer(new SkibidiMeshLayer<>(this, modelScale, variant));
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
        return FALLBACK_TEXTURE;
    }
}
