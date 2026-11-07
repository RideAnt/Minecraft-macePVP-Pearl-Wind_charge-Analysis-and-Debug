package com.example.pearlwindchargeanalysis;

import net.minecraft.util.math.Vec3d;

/**
 * Local coordinate frame attached to one pearl launch.
 *
 * x' points along the pearl's actual initial horizontal velocity.
 * y' is Minecraft world up.
 * z' = x' x y', so the pearl trajectory lies in the x'y' plane.
 */
public record PearlCoordinateFrame(
        Vec3d origin,
        Vec3d axisX,
        Vec3d axisY,
        Vec3d axisZ
) {
    private static final double EPSILON = 1.0E-10;
    private static final Vec3d WORLD_UP = new Vec3d(0.0, 1.0, 0.0);

    public static PearlCoordinateFrame create(
            Vec3d origin,
            Vec3d initialVelocity,
            Vec3d fallbackHorizontalDirection
    ) {
        Vec3d horizontal = new Vec3d(initialVelocity.x, 0.0, initialVelocity.z);

        if (horizontal.lengthSquared() < EPSILON) {
            horizontal = new Vec3d(
                    fallbackHorizontalDirection.x,
                    0.0,
                    fallbackHorizontalDirection.z
            );
        }

        if (horizontal.lengthSquared() < EPSILON) {
            horizontal = new Vec3d(0.0, 0.0, 1.0);
        }

        Vec3d x = horizontal.normalize();
        Vec3d y = WORLD_UP;
        Vec3d z = x.crossProduct(y);

        if (z.lengthSquared() < EPSILON) {
            z = new Vec3d(-1.0, 0.0, 0.0);
        } else {
            z = z.normalize();
        }

        return new PearlCoordinateFrame(origin, x, y, z);
    }

    public Vec3d toWorldPosition(Vec3d localPosition) {
        return origin
                .add(axisX.multiply(localPosition.x))
                .add(axisY.multiply(localPosition.y))
                .add(axisZ.multiply(localPosition.z));
    }

    public Vec3d toLocalVector(Vec3d worldVector) {
        return new Vec3d(
                worldVector.dotProduct(axisX),
                worldVector.dotProduct(axisY),
                worldVector.dotProduct(axisZ)
        );
    }

    public Vec3d toLocalPosition(Vec3d worldPosition) {
        return toLocalVector(worldPosition.subtract(origin));
    }
}
