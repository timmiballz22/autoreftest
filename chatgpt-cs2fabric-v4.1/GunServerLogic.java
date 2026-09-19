package dev.cs2fabric;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Single-player, integrated-server authoritative gun logic. */
public final class GunServerLogic {
    private GunServerLogic() {}

    private static final Map<WeaponKey, Long> LAST_SHOT_NANOS = new ConcurrentHashMap<>();
    private static final Map<WeaponKey, ReloadState> RELOADS = new ConcurrentHashMap<>();

    public static void requestFire(ServerPlayer player, boolean scoped) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)) return;

        GunSpec spec = gun.spec();
        WeaponKey key = new WeaponKey(player.getUUID(), spec.id());
        if (RELOADS.containsKey(key)) return;

        if (stack.getDamageValue() >= spec.magazineSize()) {
            requestReload(player);
            return;
        }

        long now = System.nanoTime();
        long last = LAST_SHOT_NANOS.getOrDefault(key, Long.MIN_VALUE / 4);
        if (now - last < spec.shotIntervalNanos()) return;
        LAST_SHOT_NANOS.put(key, now);

        stack.setDamageValue(Math.min(spec.magazineSize(), stack.getDamageValue() + 1));

        ServerLevel level = (ServerLevel) player.level();
        SoundEvent shotSound = spec.pistol() ? ModSounds.PISTOL_SHOT : ModSounds.RIFLE_SHOT;
        playDouble(level, player.getX(), player.getEyeY(), player.getZ(),
                shotSound, 1.0F, 1.0F);

        Vec3 eye = player.getEyePosition();
        float spread = scoped && spec.hasScope() ? spec.scopedSpreadDegrees() : spec.hipSpreadDegrees();
        Map<LivingEntity, Float> accumulatedDamage = new HashMap<>();

        int pellets = Math.max(1, spec.pellets());
        Vec3 exactDirection = player.getViewVector(1.0F).normalize();
        ShotResult exactShot = traceShot(player, eye, exactDirection, spec.range());
        for (int pellet = 0; pellet < pellets; pellet++) {
            boolean crosshairOnHitbox = pellet == 0 && exactShot.entityHit() != null;
            Vec3 direction = crosshairOnHitbox ? exactDirection : applySpread(exactDirection, spread);
            ShotResult shot = crosshairOnHitbox ? exactShot : traceShot(player, eye, direction, spec.range());

            renderTracer(level, eye.add(direction.scale(0.65D)), shot.impact(), pellets > 1 ? 2.5D : 1.35D);

            if (shot.entityHit() == null) continue;
            Entity entity = shot.entityHit().getEntity();
            if (!(entity instanceof LivingEntity living)) continue;

            float damage = damageForHitgroup(spec, living, shot.entityHit().getLocation());
            accumulatedDamage.merge(living, damage, Float::sum);
        }

        DamageSource bulletDamage = bulletDamageSource(level, player);
        for (Map.Entry<LivingEntity, Float> entry : accumulatedDamage.entrySet()) {
            entry.getKey().hurtServer(level, bulletDamage, entry.getValue());
        }
    }

    private static ShotResult traceShot(ServerPlayer player, Vec3 eye, Vec3 direction, double range) {
        Vec3 end = eye.add(direction.scale(range));
        HitResult blockHit = player.level().clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));
        double maxDistanceSq = range * range;
        Vec3 impact = end;
        if (blockHit.getType() != HitResult.Type.MISS) {
            impact = blockHit.getLocation();
            maxDistanceSq = eye.distanceToSqr(impact);
        }

        AABB searchBox = player.getBoundingBox().expandTowards(direction.scale(range)).inflate(1.35D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player, eye, end, searchBox,
                entity -> entity != player && entity instanceof LivingEntity && entity.isPickable(),
                maxDistanceSq
        );
        if (entityHit != null && eye.distanceToSqr(entityHit.getLocation()) <= maxDistanceSq) {
            impact = entityHit.getLocation();
        } else {
            entityHit = null;
        }
        return new ShotResult(impact, entityHit);
    }

    private static float damageForHitgroup(GunSpec spec, LivingEntity living, Vec3 hitLocation) {
        AABB box = living.getBoundingBox();
        double relative = (hitLocation.y - box.minY) / Math.max(0.001D, box.getYsize());
        float damage;
        if (relative >= 0.78D) damage = spec.damage() * spec.headMultiplier();
        else if (relative < 0.34D) damage = spec.damage() * 0.75F;
        else if (relative < 0.58D) damage = spec.damage() * 1.25F;
        else damage = spec.damage();

        if ("awp".equals(spec.id())) return Math.max(20.0F, damage);
        return damage;
    }

    private static DamageSource bulletDamageSource(ServerLevel level, ServerPlayer player) {
        return new DamageSource(
                level.registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                        .get(ModDamageTypes.BULLET.identifier())
                        .orElseThrow(),
                player
        );
    }

    private static void renderTracer(ServerLevel level, Vec3 start, Vec3 end, double desiredSpacing) {
        Vec3 delta = end.subtract(start);
        double distance = delta.length();
        if (distance < 0.05D) return;
        int steps = Math.max(2, Math.min(48, (int)Math.ceil(distance / desiredSpacing)));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double)steps;
            Vec3 p = start.add(delta.scale(t));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        level.sendParticles(ParticleTypes.CRIT, end.x, end.y, end.z, 2, 0.025D, 0.025D, 0.025D, 0.0D);
    }

    public static void requestReload(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)) return;
        if (stack.getDamageValue() <= 0) return;

        GunSpec spec = gun.spec();
        WeaponKey key = new WeaponKey(player.getUUID(), spec.id());
        if (RELOADS.containsKey(key)) return;
        if (!hasAmmoMagazine(player)) return;

        long finish = player.level().getGameTime() + spec.reloadTicks();
        RELOADS.put(key, new ReloadState(finish));

        ServerLevel level = (ServerLevel) player.level();
        playDouble(level, player.getX(), player.getY(), player.getZ(),
                ModSounds.RELOAD, 1.0F, spec.pistol() ? 1.06F : 0.96F);
    }

    public static void tick(MinecraftServer server) {
        if (RELOADS.isEmpty()) return;

        for (Map.Entry<WeaponKey, ReloadState> entry : RELOADS.entrySet()) {
            WeaponKey key = entry.getKey();
            ReloadState state = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(key.playerId());
            if (player == null) {
                RELOADS.remove(key, state);
                continue;
            }

            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof GunItem gun) || !gun.spec().id().equals(key.weaponId())) {
                RELOADS.remove(key, state);
                continue;
            }
            if (player.level().getGameTime() < state.finishGameTime()) continue;

            if (consumeAmmoMagazine(player)) stack.setDamageValue(0);
            RELOADS.remove(key, state);
        }
    }

    public static int countAmmoMagazines(Player player) {
        if (player == null) return 0;
        int total = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(ModItems.AMMO)) total += stack.getCount();
        }
        return total;
    }

    public static boolean hasAmmoMagazine(Player player) {
        return player != null && (player.getAbilities().instabuild || countAmmoMagazines(player) > 0);
    }

    private static boolean consumeAmmoMagazine(ServerPlayer player) {
        if (player.getAbilities().instabuild) return true;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(ModItems.AMMO) || stack.isEmpty()) continue;
            stack.shrink(1);
            inventory.setChanged();
            return true;
        }
        return false;
    }

    public static boolean isReloading(UUID playerId, String weaponId) {
        return RELOADS.containsKey(new WeaponKey(playerId, weaponId));
    }

    public static void resetRuntimeState() {
        LAST_SHOT_NANOS.clear();
        RELOADS.clear();
    }

    private static void playDouble(ServerLevel level, double x, double y, double z,
                                   SoundEvent sound, float volume, float pitch) {
        level.playSound(null, x, y, z, sound, SoundSource.PLAYERS, volume, pitch);
        level.playSound(null, x, y, z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static Vec3 applySpread(Vec3 forward, float degrees) {
        if (degrees <= 0.0001F) return forward;
        Vec3 right = forward.cross(Vec3.Y_AXIS);
        if (right.lengthSqr() < 1.0E-8D) right = Vec3.X_AXIS;
        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double cone = Math.tan(Math.toRadians(degrees));
        double radius = Math.sqrt(random.nextDouble()) * cone;
        double angle = random.nextDouble() * Math.PI * 2.0D;
        return forward.add(right.scale(Math.cos(angle) * radius))
                .add(up.scale(Math.sin(angle) * radius)).normalize();
    }

    private record WeaponKey(UUID playerId, String weaponId) {}
    private record ReloadState(long finishGameTime) {}
    private record ShotResult(Vec3 impact, EntityHitResult entityHit) {}
}
