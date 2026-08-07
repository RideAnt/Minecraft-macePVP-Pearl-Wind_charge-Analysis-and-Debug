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

        double geometric = (1.0 - Math.pow(AIR_DRAG, ticks)) / (1.0 - AIR_DRAG);
        double horizontalFactor = AIR_DRAG * geometric;
        double vertical = AIR_DRAG * (initialVelocity.y
                + AIR_DRAG * GRAVITY / (1.0 - AIR_DRAG)) * geometric
                - AIR_DRAG * GRAVITY * ticks / (1.0 - AIR_DRAG);
        return new Vec3(initialVelocity.x * horizontalFactor, vertical,
                initialVelocity.z * horizontalFactor);
    }
}
