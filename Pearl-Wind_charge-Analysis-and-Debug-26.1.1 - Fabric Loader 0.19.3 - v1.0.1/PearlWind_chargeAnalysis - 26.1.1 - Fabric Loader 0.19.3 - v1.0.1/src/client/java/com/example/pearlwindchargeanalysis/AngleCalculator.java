package com.example.pearlwindchargeanalysis;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Shared angle and vector helpers.
 *
 * Minecraft pitch:
 * looking up = negative, horizontal = zero, looking down = positive.
 * This mod displays elevation, therefore elevation = -pitch.
 */
public final class AngleCalculator {
    private static final double EPSILON = 1.0E-9;

    private AngleCalculator() {
    }

    public static double getLookElevationDeg(Entity entity) {
        return -Mth.wrapDegrees(entity.getXRot());
    }

    public static double getVelocityElevationDeg(Vec3 velocity) {
        double horizontal = Math.sqrt(
                velocity.x * velocity.x + velocity.z * velocity.z
        );

        if (horizontal < 1.0E-7) {
            return velocity.y >= 0.0 ? 90.0 : -90.0;
        }

        return Math.toDegrees(Math.atan2(velocity.y, horizontal));
    }

    public static boolean isBetaInsideRange(
            double betaDeg,
            double minBetaDeg,
            double maxBetaDeg
    ) {
        double min = Math.min(minBetaDeg, maxBetaDeg);
        double max = Math.max(minBetaDeg, maxBetaDeg);

        return betaDeg >= min - EPSILON
                && betaDeg <= max + EPSILON;
    }

    public static double distance3d(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static Vec3 getHorizontalLookVector(Entity entity) {
        double yaw = Math.toRadians(entity.getYRot());
        return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
    }

    public static Vec3 getDirectionFromGameAnglesDeg(
            double elevationDeg,
            double gameYawDeg
    ) {
        double elevation = Math.toRadians(elevationDeg);
        double yaw = Math.toRadians(gameYawDeg);
        double horizontal = Math.cos(elevation);

        return new Vec3(
                -Math.sin(yaw) * horizontal,
                Math.sin(elevation),
                Math.cos(yaw) * horizontal
        );
    }

    public static double getYawToHorizontalVectorDeg(Vec3 vector) {
        return normalizeGameYawDeg(
                Math.toDegrees(Math.atan2(-vector.x, vector.z))
        );
    }

    /**
     * Minecraft horizontal yaw measured from world +Z, normalized to [0, 360).
     */
    public static double normalizeGameYawDeg(double yawDeg) {
        double normalized = yawDeg % 360.0;
        if (normalized < 0.0) {
            normalized += 360.0;
        }

        return normalized >= 360.0 ? 0.0 : normalized;
    }

    public static double getGameYawDeg(Entity entity) {
        return normalizeGameYawDeg(entity.getYRot());
    }

    public static double signedHorizontalYawDeltaDeg(
            Entity entity,
            Vec3 targetVector
    ) {
        double targetYaw = getYawToHorizontalVectorDeg(targetVector);
        return Mth.wrapDegrees(targetYaw - entity.getYRot());
    }
}
