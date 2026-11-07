package com.example.pearlwindchargeanalysis;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

/**
 * Three-dimensional pearl / wind-charge encounter solver.
 *
 * The pearl is expressed in a local coordinate frame whose x' axis follows the
 * pearl's actual initial horizontal velocity and whose y' axis is world up.
 *
 * In fixed-point mode, the corrected two-point equation is evaluated in the
 * pearl's local (u, y, w) frame:
 *
 *   sqrt(DeltaU(t)^2 + DeltaY(t)^2 + DeltaW^2)
 *       = windChargeSpeed * (t - effectivePearlAge - 1)
 *
 * DeltaU and DeltaY use the exact gravity-then-drag closed form. DeltaW is
 * retained as a constant instead of assuming the launch point lies perfectly
 * in the pearl trajectory plane. The one-tick phase term is required because
 * the older pearl performs its collision sweep before the newly spawned wind
 * charge moves.
 *
 * In both modes, the player's launch velocity inherited by the wind charge is
 * subtracted before solving the same directional-speed equation. Fixed-point
 * mode still differs by enforcing the horizontal launch-area limit. The
 * earliest positive encounter root is always selected.
 */
public final class FixedPoint3DSolver {
    private static final double WIND_CHARGE_SPEED = 1.5;
    private static final double WIND_CHARGE_LAUNCH_PHASE_TICK = 1.0;

    private static final double MAX_ENCOUNTER_TICK = 400.0;
    private static final double ROOT_SCAN_STEP_TICK = 0.20;
    private static final int ROOT_BISECTION_ITERATIONS = 36;
    private static final double ROOT_DISTANCE_TOLERANCE = 1.0E-5;
    private static final double PATH_SAMPLE_SPACING_BLOCK = 0.25;
    private static final double EPSILON = 1.0E-8;

    /*
     * HUD text, assist lines and companion tools may request the same solution
     * in one render frame. Entries are removed with their active pearl tracks,
     * so the cache supports any number of currently tracked owners without
     * retaining completed projectiles.
     */
    private static final Map<UUID, CacheEntry> CACHE = new LinkedHashMap<>();

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

        Vec3d launchPosition = getWindChargeLaunchPosition(client.player);
        AnalysisMode mode = PearlWindChargeAnalysisClient.getAnalysisMode();
        Vec3d inheritedPlayerVelocity =
                getInheritedPlayerVelocity(client.player, mode);
        boolean collisionValidation = ModConfig.get().validatePathCollisions;

        CacheEntry cached = CACHE.get(track.uuid());
        if (cached != null
                && Math.abs(cached.effectiveAgeTick - effectivePearlAgeTick)
                <= 1.0E-7
                && cached.launchPosition.squaredDistanceTo(launchPosition) <= 1.0E-12
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
                launchPosition,
                inheritedPlayerVelocity
        );

        CACHE.put(
                track.uuid(),
                new CacheEntry(
                        effectivePearlAgeTick,
                        launchPosition,
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
            Vec3d launchPosition,
            Vec3d inheritedPlayerVelocity
    ) {
        TwoPointModel model = TwoPointModel.create(
                track,
                launchPosition,
                inheritedPlayerVelocity
        );
        double startTick = effectivePearlAgeTick
                + WIND_CHARGE_LAUNCH_PHASE_TICK;

        double previousTick = startTick;
        double previousValue = encounterFunction(
                model,
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
                    model,
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
                                model,
                                effectivePearlAgeTick,
                                previousTick,
                                tick
                        );

                if (!Double.isFinite(encounterTick)
                        || encounterTick < effectivePearlAgeTick
                                + WIND_CHARGE_LAUNCH_PHASE_TICK - EPSILON) {
                    previousTick = tick;
                    previousValue = value;
                    continue;
                }

                return buildAndValidateSolution(
                        client,
                        track,
                        launchPosition,
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

    /** Exact spawn position used by WindChargeItem in Minecraft 26.2. */
    public static Vec3d getWindChargeLaunchPosition(PlayerEntity player) {
        return new Vec3d(player.getX(), player.getEyeY(), player.getZ());
    }

    /**
     * Compatibility alias for companion code compiled against the previous
     * public helper. It now returns the real wind-charge launch position.
     */
    public static Vec3d getPlayerHeadCenter(PlayerEntity player) {
        return getWindChargeLaunchPosition(player);
    }

    /**
     * Vanilla projectile launch inheritance: horizontal player velocity is
     * always added; vertical velocity is added only while airborne. The mode
     * parameter is retained for binary/source compatibility; both analysis
     * modes must model the same vanilla launch physics.
     */
    public static Vec3d getInheritedPlayerVelocity(
            PlayerEntity player,
            AnalysisMode mode
    ) {
        if (player == null) {
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
        double elapsedTick = tick - track.anchorAgeTick();
        if (!Double.isFinite(elapsedTick) || elapsedTick <= 0.0) {
            return track.startPos();
        }

        Vec3d initialVelocity = track.startVelocity();
        double horizontalSpeed = Math.sqrt(
                initialVelocity.x * initialVelocity.x
                        + initialVelocity.z * initialVelocity.z
        );
        Vec3d localDisplacement = ProjectileMotionModel.displacementAfterTicks(
                new Vec3d(horizontalSpeed, initialVelocity.y, 0.0),
                elapsedTick
        );
        return track.coordinateFrame().toWorldPosition(localDisplacement);
    }

    private static double encounterFunction(
            TwoPointModel model,
            double effectivePearlAgeTick,
            double encounterTick
    ) {
        double windFlightTick = encounterTick
                - effectivePearlAgeTick
                - WIND_CHARGE_LAUNCH_PHASE_TICK;
        if (windFlightTick < -EPSILON) {
            return Double.NaN;
        }

        double requiredDirectionalDistance = model.requiredDirectionalDistance(
                encounterTick,
                Math.max(0.0, windFlightTick)
        );
        double windDirectionalReach = WIND_CHARGE_SPEED * windFlightTick;
        return requiredDirectionalDistance - windDirectionalReach;
    }

    private static double bisectRoot(
            TwoPointModel model,
            double effectivePearlAgeTick,
            double low,
            double high
    ) {
        double fLow = encounterFunction(
                model,
                effectivePearlAgeTick,
                low
        );
        double fHigh = encounterFunction(
                model,
                effectivePearlAgeTick,
                high
        );

        if (!Double.isFinite(fLow) || !Double.isFinite(fHigh)) {
            return Double.NaN;
        }

        for (int i = 0; i < ROOT_BISECTION_ITERATIONS; i++) {
            double mid = (low + high) * 0.5;
            double fMid = encounterFunction(
                    model,
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
            Vec3d launchPosition,
            Vec3d inheritedPlayerVelocity,
            double effectivePearlAgeTick,
            double encounterTick
    ) {
        double windFlightTick = encounterTick
                - effectivePearlAgeTick
                - WIND_CHARGE_LAUNCH_PHASE_TICK;
        Vec3d encounterPos = getPearlWorldPosition(track, encounterTick);
        Vec3d aimVector = encounterPos
                .subtract(launchPosition)
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
                launchPosition,
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
            Vec3d launchPosition,
            double effectivePearlAgeTick,
            FixedPointSolution solution
    ) {
        ClientWorld world = client.world;

        if (!isAirAt(world, launchPosition)
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

        if (!isAirAlongSegment(world, launchPosition, solution.encounterWorldPos())) {
            return FixedPointSolution.Status.NON_AIR_PATH;
        }

        if (ModConfig.get().validatePathCollisions
                && hitsBlock(
                        world,
                        client.player,
                        launchPosition,
                        solution.encounterWorldPos()
                )) {
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

    public static void removeCache(UUID pearlUuid) {
        if (pearlUuid != null) {
            CACHE.remove(pearlUuid);
        }
    }

    private record CacheEntry(
            double effectiveAgeTick,
            Vec3d launchPosition,
            Vec3d inheritedPlayerVelocity,
            AnalysisMode mode,
            boolean collisionValidation,
            FixedPointSolution solution
    ) {
    }

    /**
     * The PDF's two-point model, with the off-plane DeltaW term retained. The
     * anchor is the first observed pearl state, not an assumed age-zero state.
     */
    private record TwoPointModel(
            PearlTrack track,
            double deltaU0,
            double deltaY0,
            double deltaW0,
            double pearlHorizontalVelocity,
            double pearlVerticalVelocity,
            double inheritedU,
            double inheritedY,
            double inheritedW
    ) {
        private static TwoPointModel create(
                PearlTrack track,
                Vec3d launchPosition,
                Vec3d inheritedPlayerVelocity
        ) {
            Vec3d localOffset = track.coordinateFrame().toLocalVector(
                    track.startPos().subtract(launchPosition)
            );
            Vec3d localInherited = track.coordinateFrame().toLocalVector(
                    inheritedPlayerVelocity
            );
            Vec3d initialVelocity = track.startVelocity();
            double horizontalVelocity = Math.hypot(
                    initialVelocity.x,
                    initialVelocity.z
            );

            return new TwoPointModel(
                    track,
                    localOffset.x,
                    localOffset.y,
                    localOffset.z,
                    horizontalVelocity,
                    initialVelocity.y,
                    localInherited.x,
                    localInherited.y,
                    localInherited.z
            );
        }

        private double requiredDirectionalDistance(
                double encounterTick,
                double windFlightTick
        ) {
            double elapsedTick = encounterTick - track.anchorAgeTick();
            if (!Double.isFinite(elapsedTick) || elapsedTick < 0.0) {
                return Double.NaN;
            }

            double displacementFactor =
                    ProjectileMotionModel.displacementFactorAfterTicks(
                            elapsedTick
                    );
            double gravityDisplacement =
                    ProjectileMotionModel.gravityDisplacementAfterTicks(
                            elapsedTick
                    );

            double deltaU = deltaU0
                    + displacementFactor * pearlHorizontalVelocity
                    - inheritedU * windFlightTick;
            double deltaY = deltaY0
                    + displacementFactor * pearlVerticalVelocity
                    + gravityDisplacement
                    - inheritedY * windFlightTick;
            double deltaW = deltaW0 - inheritedW * windFlightTick;

            return Math.sqrt(
                    deltaU * deltaU
                            + deltaY * deltaY
                            + deltaW * deltaW
            );
        }
    }
}

