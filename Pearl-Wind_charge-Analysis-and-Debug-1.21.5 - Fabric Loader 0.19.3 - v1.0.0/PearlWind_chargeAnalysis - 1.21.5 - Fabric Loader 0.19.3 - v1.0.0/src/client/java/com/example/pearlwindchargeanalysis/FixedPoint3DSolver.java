package com.example.pearlwindchargeanalysis;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Three-dimensional pearl / wind-charge encounter solver.
 *
 * The pearl is expressed in a local coordinate frame whose x' axis follows the
 * pearl's actual initial horizontal velocity and whose y' axis is world up.
 *
 * Instead of directly dividing by sin(gamma) or using cot(gamma), the code
 * In fixed-point mode, the scalar equation is:
 *
 *   distance(headCenter, pearlPosition(n))
 *       = windChargeSpeed * (n - effectivePearlAge)
 *
 * In moving-point mode, the player's launch velocity inherited by the wind
 * charge is subtracted before solving the same directional-speed equation.
 * The earliest positive encounter root is always selected.
 */
public final class FixedPoint3DSolver {
    private static final double WIND_CHARGE_SPEED = 1.5;

    private static final double MAX_ENCOUNTER_TICK = 400.0;
    private static final double ROOT_SCAN_STEP_TICK = 0.20;
    private static final int ROOT_BISECTION_ITERATIONS = 36;
    private static final double ROOT_DISTANCE_TOLERANCE = 1.0E-5;
    private static final double PATH_SAMPLE_SPACING_BLOCK = 0.25;
    private static final double EPSILON = 1.0E-8;

    /*
     * HUD text, assist lines and companion tools may request the same solution in
     * one render frame. Cache a small number of identical requests so the
     * root scan and optional path checks run only once.
     */
    private static final Map<UUID, CacheEntry> CACHE =
            new LinkedHashMap<>(16, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<UUID, CacheEntry> eldest
                ) {
                    return size() > 16;
                }
            };

    private FixedPoint3DSolver() {
    }

    public static FixedPointSolution solve(
            MinecraftClient client,
            PearlTrack track,
            double effectivePearlAgeTick
    ) {
        if (client == null
                || client.player == null
                || client.world == null
                || track == null
                || !Double.isFinite(effectivePearlAgeTick)
                || effectivePearlAgeTick < 0.0) {
            return FixedPointSolution.invalid(FixedPointSolution.Status.NO_SOLUTION);
        }

        Vec3d headCenter = getPlayerHeadCenter(client.player);
        AnalysisMode mode = PearlWindChargeAnalysisClient.getAnalysisMode();
        Vec3d inheritedPlayerVelocity =
                getInheritedPlayerVelocity(client.player, mode);
        boolean collisionValidation = ModConfig.get().validatePathCollisions;

        CacheEntry cached = CACHE.get(track.uuid());
        if (cached != null
                && Math.abs(cached.effectiveAgeTick - effectivePearlAgeTick)
                <= 1.0E-7
                && cached.headCenter.squaredDistanceTo(headCenter) <= 1.0E-12
                && cached.inheritedPlayerVelocity.squaredDistanceTo(
                        inheritedPlayerVelocity
                ) <= 1.0E-12
                && cached.mode == mode
                && cached.collisionValidation == collisionValidation) {
            return cached.solution;
        }

        FixedPointSolution calculated = solveUncached(
                client,
                track,
                effectivePearlAgeTick,
                headCenter,
                inheritedPlayerVelocity
        );

        CACHE.put(
                track.uuid(),
                new CacheEntry(
                        effectivePearlAgeTick,
                        headCenter,
                        inheritedPlayerVelocity,
                        mode,
                        collisionValidation,
                        calculated
                )
        );

        return calculated;
    }

    private static FixedPointSolution solveUncached(
            MinecraftClient client,
            PearlTrack track,
            double effectivePearlAgeTick,
            Vec3d headCenter,
            Vec3d inheritedPlayerVelocity
    ) {
        double startTick = effectivePearlAgeTick + 1.0E-4;

        double previousTick = startTick;
        double previousValue = encounterFunction(
                track,
                headCenter,
                inheritedPlayerVelocity,
                effectivePearlAgeTick,
                previousTick
        );
        if (!Double.isFinite(previousValue)) {
            return FixedPointSolution.invalid(FixedPointSolution.Status.NO_SOLUTION);
        }

        for (double tick = startTick + ROOT_SCAN_STEP_TICK;
             tick <= MAX_ENCOUNTER_TICK + EPSILON;
             tick += ROOT_SCAN_STEP_TICK) {
            double value = encounterFunction(
                    track,
                    headCenter,
                    inheritedPlayerVelocity,
                    effectivePearlAgeTick,
                    tick
            );
            if (!Double.isFinite(value)) {
                previousTick = tick;
                previousValue = value;
                continue;
            }

            boolean exactEnough = Math.abs(value) <= ROOT_DISTANCE_TOLERANCE;
            boolean signChanged = Double.isFinite(previousValue)
                    && Math.signum(previousValue) != Math.signum(value);

            if (exactEnough || signChanged) {
                double encounterTick = exactEnough
                        ? tick
                        : bisectRoot(
                                track,
                                headCenter,
                                inheritedPlayerVelocity,
                                effectivePearlAgeTick,
                                previousTick,
                                tick
                        );

                if (!Double.isFinite(encounterTick)
                        || encounterTick <= effectivePearlAgeTick + EPSILON) {
                    previousTick = tick;
                    previousValue = value;
                    continue;
                }

                return buildAndValidateSolution(
                        client,
                        track,
                        headCenter,
                        inheritedPlayerVelocity,
                        effectivePearlAgeTick,
                        encounterTick
                );
            }

            previousTick = tick;
            previousValue = value;
        }

        return FixedPointSolution.invalid(FixedPointSolution.Status.NO_SOLUTION);
    }

    public static Vec3d getPlayerHeadCenter(PlayerEntity player) {
        Box box = player.getBoundingBox();
        return new Vec3d(
                (box.minX + box.maxX) * 0.5,
                box.maxY - 0.25,
                (box.minZ + box.maxZ) * 0.5
        );
    }

    /**
     * Vanilla projectile launch inheritance: horizontal player velocity is
     * always added; vertical velocity is added only while airborne.
     */
    public static Vec3d getInheritedPlayerVelocity(
            PlayerEntity player,
            AnalysisMode mode
    ) {
        if (player == null || mode != AnalysisMode.MOVING_POINT) {
            return Vec3d.ZERO;
        }

        Vec3d velocity = player.getVelocity();
        return new Vec3d(
                velocity.x,
                player.isOnGround() ? 0.0 : velocity.y,
                velocity.z
        );
    }

    public static Vec3d getPearlWorldPosition(PearlTrack track, double tick) {
        return track.startPos().add(
                ProjectileMotionModel.displacementAfterTicks(
                        track.startVelocity(),
                        tick
                )
        );
    }

    private static double encounterFunction(
            PearlTrack track,
            Vec3d headCenter,
            Vec3d inheritedPlayerVelocity,
            double effectivePearlAgeTick,
            double encounterTick
    ) {
        if (encounterTick <= effectivePearlAgeTick) {
            return Double.NaN;
        }

        double windFlightTick = encounterTick - effectivePearlAgeTick;
        Vec3d pearlPos = getPearlWorldPosition(track, encounterTick);
        Vec3d directionalDisplacement = pearlPos
                .subtract(headCenter)
                .subtract(inheritedPlayerVelocity.multiply(windFlightTick));
        double requiredDirectionalDistance = directionalDisplacement.length();
        double windDirectionalReach = WIND_CHARGE_SPEED * windFlightTick;
        return requiredDirectionalDistance - windDirectionalReach;
    }

    private static double bisectRoot(
            PearlTrack track,
            Vec3d headCenter,
            Vec3d inheritedPlayerVelocity,
            double effectivePearlAgeTick,
            double low,
            double high
    ) {
        double fLow = encounterFunction(
                track,
                headCenter,
                inheritedPlayerVelocity,
                effectivePearlAgeTick,
                low
        );
        double fHigh = encounterFunction(
                track,
                headCenter,
                inheritedPlayerVelocity,
                effectivePearlAgeTick,
                high
        );

        if (!Double.isFinite(fLow) || !Double.isFinite(fHigh)) {
            return Double.NaN;
        }

        for (int i = 0; i < ROOT_BISECTION_ITERATIONS; i++) {
            double mid = (low + high) * 0.5;
            double fMid = encounterFunction(
                    track,
                    headCenter,
                    inheritedPlayerVelocity,
                    effectivePearlAgeTick,
                    mid
            );

            if (!Double.isFinite(fMid)) {
                return Double.NaN;
            }

            if (Math.abs(fMid) <= ROOT_DISTANCE_TOLERANCE) {
                return mid;
            }

            if (Math.signum(fLow) == Math.signum(fMid)) {
                low = mid;
                fLow = fMid;
            } else {
                high = mid;
                fHigh = fMid;
            }
        }

        return (low + high) * 0.5;
    }

    private static FixedPointSolution buildAndValidateSolution(
            MinecraftClient client,
            PearlTrack track,
            Vec3d headCenter,
            Vec3d inheritedPlayerVelocity,
            double effectivePearlAgeTick,
            double encounterTick
    ) {
        double windFlightTick = encounterTick - effectivePearlAgeTick;
        Vec3d encounterPos = getPearlWorldPosition(track, encounterTick);
        Vec3d aimVector = encounterPos
                .subtract(headCenter)
                .subtract(inheritedPlayerVelocity.multiply(windFlightTick));
        double horizontal = Math.sqrt(
                aimVector.x * aimVector.x + aimVector.z * aimVector.z
        );

        if (aimVector.lengthSquared() < EPSILON) {
            return FixedPointSolution.invalid(FixedPointSolution.Status.NO_SOLUTION);
        }

        double betaDeg = Math.toDegrees(Math.atan2(aimVector.y, horizontal));
        double targetWorldYawDeg = AngleCalculator.getYawToHorizontalVectorDeg(aimVector);
        double signedYawDeltaDeg = MathHelper.wrapDegrees(
                targetWorldYawDeg - client.player.getYaw()
        );

        Vec3d localAim = track.coordinateFrame().toLocalVector(aimVector);
        double localGammaDeg = Math.toDegrees(Math.atan2(localAim.z, localAim.x));

        FixedPointSolution preliminary = new FixedPointSolution(
                FixedPointSolution.Status.VALID,
                betaDeg,
                localGammaDeg,
                targetWorldYawDeg,
                signedYawDeltaDeg,
                encounterTick,
                windFlightTick,
                encounterPos
        );

        FixedPointSolution.Status environmentStatus = validateEnvironmentAndPath(
                client,
                track,
                headCenter,
                effectivePearlAgeTick,
                preliminary
        );

        return environmentStatus == FixedPointSolution.Status.VALID
                ? preliminary
                : FixedPointSolution.invalid(environmentStatus);
    }

    private static FixedPointSolution.Status validateEnvironmentAndPath(
            MinecraftClient client,
            PearlTrack track,
            Vec3d headCenter,
            double effectivePearlAgeTick,
            FixedPointSolution solution
    ) {
        ClientWorld world = client.world;

        if (!isAirAt(world, headCenter)
                || !isAirAt(world, solution.encounterWorldPos())) {
            return FixedPointSolution.Status.NON_AIR_PATH;
        }

        Vec3d previousPearlPos = getPearlWorldPosition(track, effectivePearlAgeTick);
        double pearlTick = effectivePearlAgeTick;

        while (pearlTick < solution.encounterTick() - EPSILON) {
            double nextTick = Math.min(pearlTick + 0.25, solution.encounterTick());
            Vec3d nextPearlPos = getPearlWorldPosition(track, nextTick);

            if (!isAirAlongSegment(world, previousPearlPos, nextPearlPos)) {
                return FixedPointSolution.Status.NON_AIR_PATH;
            }

            if (ModConfig.get().validatePathCollisions
                    && hitsBlock(world, client.player, previousPearlPos, nextPearlPos)) {
                return FixedPointSolution.Status.BLOCKED_PATH;
            }

            previousPearlPos = nextPearlPos;
            pearlTick = nextTick;
        }

        if (!isAirAlongSegment(world, headCenter, solution.encounterWorldPos())) {
            return FixedPointSolution.Status.NON_AIR_PATH;
        }

        if (ModConfig.get().validatePathCollisions
                && hitsBlock(world, client.player, headCenter, solution.encounterWorldPos())) {
            return FixedPointSolution.Status.BLOCKED_PATH;
        }

        return FixedPointSolution.Status.VALID;
    }

    private static boolean isAirAlongSegment(ClientWorld world, Vec3d start, Vec3d end) {
        Vec3d delta = end.subtract(start);
        double distance = delta.length();

        if (distance < EPSILON) {
            return isAirAt(world, start);
        }

        int steps = Math.max(1, (int) Math.ceil(distance / PATH_SAMPLE_SPACING_BLOCK));

        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3d point = start.add(delta.multiply(t));

            if (!isAirAt(world, point)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isAirAt(ClientWorld world, Vec3d point) {
        BlockPos pos = BlockPos.ofFloored(point.x, point.y, point.z);
        return world.getFluidState(pos).isEmpty();
    }

    private static boolean hitsBlock(
            ClientWorld world,
            PlayerEntity player,
            Vec3d start,
            Vec3d end
    ) {
        HitResult result = world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        return result.getType() == HitResult.Type.BLOCK;
    }

    public static double getTheoreticalWindChargeSpeed() {
        return WIND_CHARGE_SPEED;
    }

    public static void clearCache() {
        CACHE.clear();
    }

    private record CacheEntry(
            double effectiveAgeTick,
            Vec3d headCenter,
            Vec3d inheritedPlayerVelocity,
            AnalysisMode mode,
            boolean collisionValidation,
            FixedPointSolution solution
    ) {
    }
}
