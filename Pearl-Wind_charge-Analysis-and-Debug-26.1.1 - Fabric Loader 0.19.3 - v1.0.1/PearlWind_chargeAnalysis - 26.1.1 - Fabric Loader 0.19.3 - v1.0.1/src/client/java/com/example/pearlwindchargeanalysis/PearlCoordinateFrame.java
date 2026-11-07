package com.example.pearlwindchargeanalysis;

import net.minecraft.world.phys.Vec3;

/**
 * Local coordinate frame attached to one pearl launch.
 *
 * x' points along the pearl's actual initial horizontal velocity.
 * y' is Minecraft world up.
 * z' = x' x y', so the pearl trajectory lies in the x'y' plane.
 */
public record PearlCoordinateFrame(
        Vec3 origin,
        Vec3 axisX,
        Vec3 axisY,
        Vec3 axisZ
) {
    private static final double EPSILON = 1.0E-10;
    private static final Vec3 WORLD_UP = new Vec3(0.0, 1.0, 0.0);

    public static PearlCoordinateFrame create(
            Vec3 origin,
            Vec3 initialVelocity,
            Vec3 fallbackHorizontalDirection
    ) {
        Vec3 horizontal = new Vec3(initialVelocity.x, 0.0, initialVelocity.z);

        if (horizontal.lengthSqr() < EPSILON) {
            horizontal = new Vec3(
                    fallbackHorizontalDirection.x,
                    0.0,
                    fallbackHorizontalDirection.z
            );
        }

        if (horizontal.lengthSqr() < EPSILON) {
            horizontal = new Vec3(0.0, 0.0, 1.0);
        }

        Vec3 x = horizontal.normalize();
        Vec3 y = WORLD_UP;
        Vec3 z = x.cross(y);

        if (z.lengthSqr() < EPSILON) {
            z = new Vec3(-1.0, 0.0, 0.0);
        } else {
            z = z.normalize();
        }

        return new PearlCoordinateFrame(origin, x, y, z);
    }

    public Vec3 toWorldPosition(Vec3 localPosition) {
        return origin
                .add(axisX.scale(localPosition.x))
                .add(axisY.scale(localPosition.y))
                .add(axisZ.scale(localPosition.z));
    }

    public Vec3 toLocalVector(Vec3 worldVector) {
        return new Vec3(
                worldVector.dot(axisX),
                worldVector.dot(axisY),
                worldVector.dot(axisZ)
        );
    }

    public Vec3 toLocalPosition(Vec3 worldPosition) {
        return toLocalVector(worldPosition.subtract(origin));
    }
}
