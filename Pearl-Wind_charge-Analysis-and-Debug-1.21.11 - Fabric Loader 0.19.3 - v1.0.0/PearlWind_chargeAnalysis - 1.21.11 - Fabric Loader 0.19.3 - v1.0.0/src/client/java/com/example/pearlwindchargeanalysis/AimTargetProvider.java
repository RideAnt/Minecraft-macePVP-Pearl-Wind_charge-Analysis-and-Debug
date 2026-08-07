package com.example.pearlwindchargeanalysis;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Read-only integration point for optional companion tools.
 */
public final class AimTargetProvider {
    private static final double FIXED_POINT_MAX_THROWER_DRIFT_BLOCKS = 0.80;

    private AimTargetProvider() {
    }

    /**
     * Returns target game yaw and pitch for P1/P2, or null when no valid target
     * is currently available. This method never changes player state.
     */
    public static double[] resolve(int index) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ModConfig.get().enabled
                || client.player == null
                || client.world == null
                || index < 1
                || index > 2) {
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
                && track.distanceFromThrowerStart(client.player)
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
}
