package com.example.pearlwindchargeanalysis;

import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * A single thrown ender pearl captured when it first appears on the client.
 */
public record PearlTrack(
        UUID uuid,
        int entityId,
        double actualAlphaDeg,
        Vec3 startPos,
        Vec3 throwerStartPos,
        Vec3 startVelocity,
        PearlCoordinateFrame coordinateFrame
) {
    public double distanceFromThrowerStart(Entity entity) {
        Vec3 entityPos = new Vec3(
                entity.getX(),
                entity.getY(),
                entity.getZ()
        );
        return throwerStartPos.distanceTo(entityPos);
    }
}
