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
        UUID ownerUuid,
        String ownerName,
        boolean ownedByLocalPlayer,
        double actualAlphaDeg,
        Vec3 startPos,
        Vec3 throwerStartPos,
        Vec3 startVelocity,
        double anchorAgeTick,
        PearlCoordinateFrame coordinateFrame
) {
    public String displayLabel(int index) {
        String base = "P" + index;
        if (ownedByLocalPlayer || ownerName == null || ownerName.isBlank()) {
            return base;
        }

        return base + "[" + ownerName + "]";
    }

    /**
     * Fixed-point mode is invalidated only by horizontal movement away from
     * the throw point. Jumping or falling in place must not disable it.
     */
    public double horizontalDistanceFromThrowerStart(Entity entity) {
        double dx = entity.getX() - throwerStartPos.x;
        double dz = entity.getZ() - throwerStartPos.z;
        return Math.hypot(dx, dz);
    }
}
