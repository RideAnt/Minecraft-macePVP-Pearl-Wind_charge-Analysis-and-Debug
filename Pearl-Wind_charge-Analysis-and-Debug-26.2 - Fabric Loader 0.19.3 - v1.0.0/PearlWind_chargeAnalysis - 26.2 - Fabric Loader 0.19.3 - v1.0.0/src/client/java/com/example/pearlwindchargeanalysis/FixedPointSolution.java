package com.example.pearlwindchargeanalysis;

import net.minecraft.world.phys.Vec3;

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
        Vec3 encounterWorldPos
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
                Vec3.ZERO
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
