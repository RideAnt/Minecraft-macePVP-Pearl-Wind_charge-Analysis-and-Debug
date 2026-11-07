package com.example.pearlwindchargeanalysis;

import net.minecraft.world.phys.Vec3;

/** Discrete pearl motion for the gravity-then-drag, move-after-update tick order. */
public final class ProjectileMotionModel {
    public static final double AIR_DRAG = 0.99;
    public static final double GRAVITY = 0.03;

    private ProjectileMotionModel() {
    }

    public static Vec3 displacementAfterTicks(Vec3 initialVelocity, double ticks) {
        if (initialVelocity == null || !Double.isFinite(ticks) || ticks <= 0.0) {
            return Vec3.ZERO;
        }

        double displacementFactor = displacementFactorAfterTicks(ticks);
        double gravityDisplacement = gravityDisplacementAfterTicks(ticks);
        return new Vec3(
                initialVelocity.x * displacementFactor,
                initialVelocity.y * displacementFactor + gravityDisplacement,
                initialVelocity.z * displacementFactor
        );
    }

    /** S(t) = q(1-q^t)/(1-q), with q equal to the air-drag factor. */
    public static double displacementFactorAfterTicks(double ticks) {
        if (!Double.isFinite(ticks) || ticks <= 0.0) {
            return 0.0;
        }

        return AIR_DRAG * (1.0 - Math.pow(AIR_DRAG, ticks))
                / (1.0 - AIR_DRAG);
    }

    /** G(t), the accumulated vertical displacement caused by gravity. */
    public static double gravityDisplacementAfterTicks(double ticks) {
        if (!Double.isFinite(ticks) || ticks <= 0.0) {
            return 0.0;
        }

        double displacementFactor = displacementFactorAfterTicks(ticks);
        return AIR_DRAG * GRAVITY * displacementFactor
                / (1.0 - AIR_DRAG)
                - AIR_DRAG * GRAVITY * ticks / (1.0 - AIR_DRAG);
    }
}
