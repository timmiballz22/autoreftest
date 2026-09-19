package dev.ashenthefox.skibidi;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class ModDamageTypes {
    public static final ResourceKey<DamageType> G_TOILET_LASER =
        ResourceKey.create(Registries.DAMAGE_TYPE, SkibidiToiletMod.id("g_toilet_laser"));

    private ModDamageTypes() {
    }

    public static DamageSource gToiletLaser(Level level, Entity attacker) {
        Holder.Reference<DamageType> type = level.registryAccess()
            .lookupOrThrow(Registries.DAMAGE_TYPE)
            .getOrThrow(G_TOILET_LASER);
        return new DamageSource(type, attacker, attacker);
    }
}
