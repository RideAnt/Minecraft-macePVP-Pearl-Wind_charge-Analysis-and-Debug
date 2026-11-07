package com.rideant.pearlwindchargeanalysisdebug;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side, single-line-per-entity tick tracing for model verification.
 * Spawn states are inserted into the same history map as tick states, so age
 * zero is emitted once and the first line can describe P0 -> P1 or W0 -> W1.
 */
public final class ProjectileTraceLogger {
    private static final Map<UUID, TraceState> PREVIOUS = new HashMap<>();

    private ProjectileTraceLogger() {
    }

    public static void onProjectileLaunched(Projectile projectile) {
        if (!shouldTrace(projectile)) {
            return;
        }

        MinecraftServer server = projectile.level().getServer();
        int serverTick = server == null ? -1 : server.getTickCount();
        TraceState state = TraceState.capture(projectile, serverTick);
        if (PREVIOUS.putIfAbsent(projectile.getUUID(), state) != null) {
            return;
        }

        PearlWindChargeAnalysisDebug.LOGGER.info(String.format(
                Locale.ROOT,
                "PWTRACE event=SPAWN serverTick=%d %s ownerPos=%s ownerKnown=%s",
                serverTick,
                state.describe(),
                formatVec(state.ownerPosition),
                formatVec(state.ownerKnownMovement)
        ));
    }

    public static void onEndServerTick(MinecraftServer server) {
        if (!ModConfig.get().trajectoryTrace || server.isDedicatedServer()) {
            PREVIOUS.clear();
            return;
        }

        int serverTick = server.getTickCount();
        Map<UUID, TraceState> current = new HashMap<>();
        Set<UUID> seen = new HashSet<>();

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof Projectile projectile)
                        || !isSupported(projectile)
                        || !(projectile.getOwner() instanceof Player)) {
                    continue;
                }

                UUID uuid = projectile.getUUID();
                TraceState state = TraceState.capture(projectile, serverTick);
                TraceState previous = PREVIOUS.get(uuid);
                if (previous == null) {
                    PearlWindChargeAnalysisDebug.LOGGER.info(String.format(
                            Locale.ROOT,
                            "PWTRACE event=OBSERVE serverTick=%d %s ownerPos=%s ownerKnown=%s",
                            serverTick,
                            state.describe(),
                            formatVec(state.ownerPosition),
                            formatVec(state.ownerKnownMovement)
                    ));
                } else if (previous.age != state.age
                        || previous.position.distanceToSqr(state.position) > 0.0) {
                    logTick(previous, state);
                }

                current.put(uuid, state);
                seen.add(uuid);
            }
        }

        for (Map.Entry<UUID, TraceState> entry : PREVIOUS.entrySet()) {
            if (seen.contains(entry.getKey())) {
                continue;
            }
            TraceState state = entry.getValue();
            PearlWindChargeAnalysisDebug.LOGGER.info(String.format(
                    Locale.ROOT,
                    "PWTRACE event=REMOVE serverTick=%d last={%s}",
                    serverTick,
                    state.describe()
            ));
        }

        PREVIOUS.clear();
        PREVIOUS.putAll(current);
    }

    public static void onHit(Projectile projectile, HitResult hitResult) {
        if (!shouldTraceType(projectile)) {
            return;
        }

        String target = "none";
        if (hitResult instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            target = String.format(
                    Locale.ROOT,
                    "%s#%d age=%d pos=%s",
                    entity.getType(),
                    entity.getId(),
                    entity.tickCount,
                    formatVec(entity.position())
            );
        }

        PearlWindChargeAnalysisDebug.LOGGER.info(String.format(
                Locale.ROOT,
                "PWTRACE event=HIT source=%s#%d age=%d hitType=%s hitPos=%s target={%s}",
                kind(projectile),
                projectile.getId(),
                projectile.tickCount,
                hitResult.getType(),
                formatVec(hitResult.getLocation()),
                target
        ));
    }

    public static void onDeflection(
            Projectile projectile,
            ProjectileDeflection deflection,
            Entity deflectingEntity,
            boolean byAttack,
            boolean applied,
            Vec3 velocityBefore,
            double accelerationBefore
    ) {
        if (!shouldTraceType(projectile)) {
            return;
        }

        String deflector = deflectingEntity == null
                ? "none"
                : deflectingEntity.getType() + "#" + deflectingEntity.getId();
        PearlWindChargeAnalysisDebug.LOGGER.info(String.format(
                Locale.ROOT,
                "PWTRACE event=DEFLECT target=%s#%d age=%d type=%s byAttack=%s applied=%s deflector=%s velocityBefore=%s velocityAfter=%s accelerationBefore=%.6f accelerationAfter=%.6f",
                kind(projectile),
                projectile.getId(),
                projectile.tickCount,
                deflection,
                byAttack,
                applied,
                deflector,
                formatVec(velocityBefore),
                formatVec(projectile.getDeltaMovement()),
                accelerationBefore,
                accelerationPower(projectile)
        ));
    }

    public static double accelerationPower(Projectile projectile) {
        return projectile instanceof AbstractHurtingProjectile hurting
                ? hurting.accelerationPower
                : Double.NaN;
    }

    private static void logTick(TraceState previous, TraceState current) {
        String phase = "pearl".equals(current.kind)
                ? String.format(
                        Locale.ROOT,
                        "sweep=P%d->P%d",
                        previous.age,
                        current.age
                )
                : "moves=W" + current.age;
        PearlWindChargeAnalysisDebug.LOGGER.info(String.format(
                Locale.ROOT,
                "PWTRACE event=TICK serverTick=%d %s %s from=%s to=%s velocityBefore=%s velocityAfter=%s acceleration=%.6f ownerKnown=%s",
                current.serverTick,
                phase,
                current.identity(),
                formatVec(previous.position),
                formatVec(current.position),
                formatVec(previous.velocity),
                formatVec(current.velocity),
                current.accelerationPower,
                formatVec(current.ownerKnownMovement)
        ));
    }

    private static boolean shouldTrace(Projectile projectile) {
        return shouldTraceType(projectile)
                && projectile.getOwner() instanceof Player;
    }

    private static boolean shouldTraceType(Projectile projectile) {
        return ModConfig.get().trajectoryTrace
                && projectile != null
                && projectile.level() != null
                && !projectile.level().isClientSide()
                && isSupported(projectile);
    }

    private static boolean isSupported(Projectile projectile) {
        return projectile instanceof ThrownEnderpearl
                || projectile instanceof WindCharge;
    }

    private static String kind(Projectile projectile) {
        return projectile instanceof ThrownEnderpearl ? "pearl" : "wind";
    }

    private static String formatVec(Vec3 value) {
        if (value == null) {
            return "none";
        }
        return String.format(
                Locale.ROOT,
                "(%.6f,%.6f,%.6f)",
                value.x,
                value.y,
                value.z
        );
    }

    private record TraceState(
            int serverTick,
            String dimension,
            String kind,
            int entityId,
            UUID uuid,
            int age,
            Vec3 position,
            Vec3 velocity,
            double accelerationPower,
            Vec3 ownerPosition,
            Vec3 ownerKnownMovement
    ) {
        private static TraceState capture(
                Projectile projectile,
                int serverTick
        ) {
            Entity owner = projectile.getOwner();
            return new TraceState(
                    serverTick,
                    projectile.level().dimension().toString(),
                    ProjectileTraceLogger.kind(projectile),
                    projectile.getId(),
                    projectile.getUUID(),
                    projectile.tickCount,
                    projectile.position(),
                    projectile.getDeltaMovement(),
                    ProjectileTraceLogger.accelerationPower(projectile),
                    owner == null ? null : owner.position(),
                    owner == null ? null : owner.getKnownMovement()
            );
        }

        private String identity() {
            return String.format(
                    Locale.ROOT,
                    "kind=%s id=%d uuid=%s age=%d dimension=%s",
                    kind,
                    entityId,
                    uuid,
                    age,
                    dimension
            );
        }

        private String describe() {
            return String.format(
                    Locale.ROOT,
                    "%s pos=%s velocity=%s acceleration=%.6f",
                    identity(),
                    formatVec(position),
                    formatVec(velocity),
                    accelerationPower
            );
        }
    }
}
