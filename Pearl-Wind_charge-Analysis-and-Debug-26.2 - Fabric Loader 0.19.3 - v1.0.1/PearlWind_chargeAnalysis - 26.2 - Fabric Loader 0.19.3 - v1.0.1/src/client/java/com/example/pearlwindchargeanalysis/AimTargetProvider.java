package com.example.pearlwindchargeanalysis;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.Vec3;

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
        Minecraft client = Minecraft.getInstance();
        if (!ModConfig.get().enabled
                || client.player == null
                || client.level == null
                || index < 1) {
            return null;
        }

        PearlTrack track = PearlWindChargeAnalysisClient.TRACKER.getTrackByIndex(index);
        if (track == null) {
            return null;
        }

        ThrownEnderpearl pearl = PearlWindChargeAnalysisClient.TRACKER
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

        float tickProgress = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
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
                Mth.clamp(-solution.betaDeg(), -90.0, 90.0)
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
        Minecraft client = Minecraft.getInstance();
        if (!ModConfig.get().enabled
                || client.player == null
                || client.level == null
                || index < 1) {
            return null;
        }

        PearlTrack track = PearlWindChargeAnalysisClient.TRACKER
                .getTrackByIndex(index);
        if (track == null || PearlWindChargeAnalysisClient.TRACKER
                .getPearlEntity(client, track) == null) {
            return null;
        }

        float tickProgress = client.getDeltaTracker()
                .getGameTimeDeltaPartialTick(false);
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
        Vec3 launchPosition = FixedPoint3DSolver
                .getWindChargeLaunchPosition(client.player);
        Vec3 inheritedVelocity = FixedPoint3DSolver
                .getInheritedPlayerVelocity(
                        client.player,
                        PearlWindChargeAnalysisClient.getAnalysisMode()
                );

        Vec3 encounterPosition = solution.isValid()
                ? solution.encounterWorldPos()
                : new Vec3(Double.NaN, Double.NaN, Double.NaN);
        Vec3 localDirectional = new Vec3(
                Double.NaN,
                Double.NaN,
                Double.NaN
        );
        double residual = Double.NaN;
        if (solution.isValid()) {
            Vec3 directional = encounterPosition
                    .subtract(launchPosition)
                    .subtract(inheritedVelocity.scale(solution.windFlightTick()));
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
