package com.example.pearlwindchargeanalysis;

import net.minecraft.util.math.Vec3d;

/**
 * Result produced by the three-dimensional encounter solver.
 */
public record FixedPointSolution(
        Status status,
        double betaDeg,
        double localGammaDeg,
        double targetWorldYawDeg,
        double signedYawDeltaDeg,
        double encounterTick,
        double windFlightTick,
        Vec3d encounterWorldPos
) {
    public enum Status {
        VALID,
        NO_SOLUTION,
        NON_AIR_PATH,
        BLOCKED_PATH
    }

    public static FixedPointSolution invalid(Status status) {
        return new FixedPointSolution(
                status,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Vec3d.ZERO
        );
    }

    public boolean isValid() {
        return status == Status.VALID
                && Double.isFinite(betaDeg)
                && Double.isFinite(localGammaDeg)
                && Double.isFinite(targetWorldYawDeg)
                && Double.isFinite(signedYawDeltaDeg)
                && Double.isFinite(encounterTick)
                && Double.isFinite(windFlightTick);
    }
}
