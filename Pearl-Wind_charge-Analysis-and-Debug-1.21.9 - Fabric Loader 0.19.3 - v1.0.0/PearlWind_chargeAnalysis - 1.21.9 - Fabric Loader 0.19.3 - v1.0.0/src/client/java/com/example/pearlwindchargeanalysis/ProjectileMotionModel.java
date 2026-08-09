package com.example.pearlwindchargeanalysis;

import net.minecraft.util.math.Vec3d;

/**
 * Discrete ender-pearl motion used by Minecraft 1.21.11.
 *
 * <p>For each tick, the projectile applies gravity, applies air drag, then
 * moves by the updated velocity. The initial velocity is the launch velocity
 * captured when the local player's pearl first appears on the client.</p>
 */
public final class ProjectileMotionModel {
    public static final double AIR_DRAG = 0.99;
    public static final double GRAVITY = 0.03;

    private static final double EPSILON = 1.0E-9;

    private ProjectileMotionModel() {
    }

    /**
     * Returns the displacement after {@code ticks} projectile updates from its
     * launch position.
     */
    public static Vec3d displacementAfterTicks(Vec3d initialVelocity, double ticks) {
        if (initialVelocity == null || !Double.isFinite(ticks) || ticks <= 0.0) {
            return Vec3d.ZERO;
        }

        double geometric = geometricSum(ticks);
        double horizontalFactor = AIR_DRAG * geometric;
        double vertical = AIR_DRAG * (
                initialVelocity.y + AIR_DRAG * GRAVITY / (1.0 - AIR_DRAG)
        ) * geometric - AIR_DRAG * GRAVITY * ticks / (1.0 - AIR_DRAG);

        return new Vec3d(
                initialVelocity.x * horizontalFactor,
                vertical,
                initialVelocity.z * horizontalFactor
        );
    }

    private static double geometricSum(double ticks) {
        if (Math.abs(1.0 - AIR_DRAG) < EPSILON) {
            return ticks;
        }

        return (1.0 - Math.pow(AIR_DRAG, ticks)) / (1.0 - AIR_DRAG);
    }
}
