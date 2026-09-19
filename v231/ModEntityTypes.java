package dev.ashenthefox.skibidi;

import dev.ashenthefox.skibidi.entity.GToiletEntity;
import dev.ashenthefox.skibidi.entity.SkibidiEntity;
import dev.ashenthefox.skibidi.entity.SkibidiKingEntity;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntityTypes {
    public static final Identifier SKIBIDI_ID = SkibidiToiletMod.id("skibidi");
    public static final Identifier SKIBIDI_KING_ID = SkibidiToiletMod.id("skibidi_king");
    public static final Identifier G_TOILET_ID = SkibidiToiletMod.id("g_toilet");
    public static final Identifier G_TOILET_2_ID = SkibidiToiletMod.id("g_toilet_2");
    public static final Identifier G_TOILET_3_ID = SkibidiToiletMod.id("g_toilet_3");

    public static final ResourceKey<EntityType<?>> SKIBIDI_KEY = key(SKIBIDI_ID);
    public static final ResourceKey<EntityType<?>> SKIBIDI_KING_KEY = key(SKIBIDI_KING_ID);
    public static final ResourceKey<EntityType<?>> G_TOILET_KEY = key(G_TOILET_ID);
    public static final ResourceKey<EntityType<?>> G_TOILET_2_KEY = key(G_TOILET_2_ID);
    public static final ResourceKey<EntityType<?>> G_TOILET_3_KEY = key(G_TOILET_3_ID);

    public static final EntityType<SkibidiEntity> SKIBIDI = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        SKIBIDI_KEY,
        EntityType.Builder.of(SkibidiEntity::new, MobCategory.MONSTER)
            .sized(0.90F, 2.00F)
            .eyeHeight(1.68F)
            .clientTrackingRange(10)
            .build(SKIBIDI_KEY)
    );

    public static final EntityType<SkibidiKingEntity> SKIBIDI_KING = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        SKIBIDI_KING_KEY,
        EntityType.Builder.of(SkibidiKingEntity::new, MobCategory.MONSTER)
            .sized(1.35F, 3.25F)
            .eyeHeight(2.45F)
            .clientTrackingRange(12)
            .build(SKIBIDI_KING_KEY)
    );

    public static final EntityType<GToiletEntity> G_TOILET = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        G_TOILET_KEY,
        EntityType.Builder.of(GToiletEntity::new, MobCategory.MONSTER)
            .sized(1.60F, 3.85F)
            .eyeHeight(3.05F)
            .clientTrackingRange(12)
            .build(G_TOILET_KEY)
    );

    public static final EntityType<GToiletEntity> G_TOILET_2 = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        G_TOILET_2_KEY,
        EntityType.Builder.of(GToiletEntity::new, MobCategory.MONSTER)
            .sized(1.82F, 4.20F)
            .eyeHeight(3.30F)
            .clientTrackingRange(12)
            .build(G_TOILET_2_KEY)
    );

    public static final EntityType<GToiletEntity> G_TOILET_3 = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        G_TOILET_3_KEY,
        EntityType.Builder.of(GToiletEntity::new, MobCategory.MONSTER)
            .sized(2.05F, 4.70F)
            .eyeHeight(3.65F)
            .clientTrackingRange(14)
            .build(G_TOILET_3_KEY)
    );

    private ModEntityTypes() {
    }

    private static ResourceKey<EntityType<?>> key(Identifier id) {
        return ResourceKey.create(Registries.ENTITY_TYPE, id);
    }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(
            SKIBIDI,
            SkibidiEntity.createAttributes(20.0D, 4.0D, 0.0D, 0.0D, 35.0D, 0.26D)
        );
        FabricDefaultAttributeRegistry.register(SKIBIDI_KING, SkibidiKingEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(
            G_TOILET,
            GToiletEntity.createAttributes(450.0D, 24.0D, 14.0D, 6.0D, 0.90D, 72.0D, 0.24D)
        );
        FabricDefaultAttributeRegistry.register(
            G_TOILET_2,
            GToiletEntity.createAttributes(650.0D, 28.0D, 18.0D, 8.0D, 0.95D, 80.0D, 0.26D)
        );
        FabricDefaultAttributeRegistry.register(
            G_TOILET_3,
            GToiletEntity.createAttributes(900.0D, 34.0D, 22.0D, 10.0D, 1.00D, 88.0D, 0.28D)
        );

        SpawnPlacements.register(
            SKIBIDI,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Mob::checkMobSpawnRules
        );

        BiomeModifications.addSpawn(
            BiomeSelectors.foundInOverworld(),
            MobCategory.MONSTER,
            SKIBIDI,
            95,
            1,
            3
        );
    }
}
