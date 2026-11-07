package com.example.pearlwindchargeanalysis;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Read-only integration point for optional companion tools.
 */
public final class AimTargetProvider {
    private static final double FIXED_POINT_MAX_THROWER_DRIFT_BLOCKS = 0.80;

    private AimTargetProvider() {
    }

    /**
     * Returns target game yaw and pitch for a tracked pearl index, or null when no valid target
     * is currently available. This method never changes player state.
     */
    public static double[] resolve(int index) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ModConfig.get().enabled
                || client.player == null
                || client.world == null
                || index < 1) {
            return null;
        }

        PearlTrack track = PearlWindChargeAnalysisClient.TRACKER.getTrackByIndex(index);
        if (track == null) {
            return null;
        }

        EnderPearlEntity pearl = PearlWindChargeAnalysisClient.TRACKER
                .getPearlEntity(client, track);
        if (pearl == null) {
            return null;
        }

        if (PearlWindChargeAnalysisClient.getAnalysisMode()
                == AnalysisMode.FIXED_POINT
                && track.horizontalDistanceFromThrowerStart(client.player)
                > FIXED_POINT_MAX_THROWER_DRIFT_BLOCKS) {
            return null;
        }

        float tickProgress = client.getRenderTickCounter().getTickProgress(false);
        double clientAgeTick = PearlWindChargeAnalysisClient.TRACKER.getAgeTicks(
                client,
                track,
                tickProgress
        );
        if (clientAgeTick < 0.0) {
            return null;
        }

        double effectiveAgeTick = NetworkTimingCompensation
                .getEffectivePearlAgeTicks(client, clientAgeTick);
        FixedPointSolution solution = FixedPoint3DSolver.solve(
                client,
                track,
                effectiveAgeTick
        );
        if (!solution.isValid()) {
            return null;
        }

        ModConfig.Config config = ModConfig.get();
        if (!AngleCalculator.isBetaInsideRange(
                solution.betaDeg(),
                config.bestBetaMin,
                config.bestBetaMax
        )) {
            return null;
        }

        return new double[]{
                solution.targetWorldYawDeg(),
                MathHelper.clamp(-solution.betaDeg(), -90.0, 90.0)
        };
    }

    /**
     * Read-only diagnostics for the optional 26.2 debug companion.
     *
     * Layout: status, clientAge, effectiveAge, anchorAge, encounterAge,
     * windMoves, launch xyz, encounter xyz, local delta u/y/w, residual,
     * horizontal thrower drift, inherited xyz, beta and world yaw.
     */
    public static double[] resolveDiagnostics(int index) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ModConfig.get().enabled
                || client.player == null
                || client.world == null
                || index < 1) {
            return null;
        }

        PearlTrack track = PearlWindChargeAnalysisClient.TRACKER
                .getTrackByIndex(index);
        if (track == null || PearlWindChargeAnalysisClient.TRACKER
                .getPearlEntity(client, track) == null) {
            return null;
        }

        float tickProgress = client.getRenderTickCounter()
                .getTickProgress(false);
        double clientAgeTick = PearlWindChargeAnalysisClient.TRACKER
                .getAgeTicks(client, track, tickProgress);
        if (clientAgeTick < 0.0) {
            return null;
        }

        double effectiveAgeTick = NetworkTimingCompensation
                .getEffectivePearlAgeTicks(client, clientAgeTick);
        FixedPointSolution solution = FixedPoint3DSolver.solve(
                client,
                track,
                effectiveAgeTick
        );
        Vec3d launchPosition = FixedPoint3DSolver
                .getWindChargeLaunchPosition(client.player);
        Vec3d inheritedVelocity = FixedPoint3DSolver
                .getInheritedPlayerVelocity(
                        client.player,
                        PearlWindChargeAnalysisClient.getAnalysisMode()
                );

        Vec3d encounterPosition = solution.isValid()
                ? solution.encounterWorldPos()
                : new Vec3d(Double.NaN, Double.NaN, Double.NaN);
        Vec3d localDirectional = new Vec3d(
                Double.NaN,
                Double.NaN,
                Double.NaN
        );
        double residual = Double.NaN;
        if (solution.isValid()) {
            Vec3d directional = encounterPosition
                    .subtract(launchPosition)
                    .subtract(inheritedVelocity.multiply(solution.windFlightTick()));
            localDirectional = track.coordinateFrame().toLocalVector(directional);
            residual = directional.length()
                    - FixedPoint3DSolver.getTheoreticalWindChargeSpeed()
                    * solution.windFlightTick();
        }

        return new double[]{
                solution.status().ordinal(),
                clientAgeTick,
                effectiveAgeTick,
                track.anchorAgeTick(),
                solution.encounterTick(),
                solution.windFlightTick(),
                launchPosition.x,
                launchPosition.y,
                launchPosition.z,
                encounterPosition.x,
                encounterPosition.y,
                encounterPosition.z,
                localDirectional.x,
                localDirectional.y,
                localDirectional.z,
                residual,
                track.horizontalDistanceFromThrowerStart(client.player),
                inheritedVelocity.x,
                inheritedVelocity.y,
                inheritedVelocity.z,
                solution.betaDeg(),
                solution.targetWorldYawDeg()
        };
    }
}

