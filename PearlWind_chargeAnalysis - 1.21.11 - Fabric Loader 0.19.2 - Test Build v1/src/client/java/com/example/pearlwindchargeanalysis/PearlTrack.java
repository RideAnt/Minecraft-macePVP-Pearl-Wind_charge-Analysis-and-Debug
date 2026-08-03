package com.example.pearlwindchargeanalysis;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/**
 * A single thrown ender pearl captured when it first appears on the client.
 */
public record PearlTrack(
        UUID uuid,
        int entityId,
        double actualAlphaDeg,
        Vec3d startPos,
        Vec3d throwerStartPos,
        Vec3d startVelocity,
        PearlCoordinateFrame coordinateFrame
) {
    public double distanceFromThrowerStart(Entity entity) {
        Vec3d entityPos = new Vec3d(
                entity.getX(),
                entity.getY(),
                entity.getZ()
        );
        return throwerStartPos.distanceTo(entityPos);
    }
}
