package com.example.pearlwindchargeanalysis;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.Vec3;

/**
 * Tracks ender pearls thrown by the local player.
 */
public final class PearlTracker {
    private final Map<UUID, PearlTrack> flyingPearls = new LinkedHashMap<>();

    public void tick(Minecraft client) {
        if (client.level == null || client.player == null) {
            flyingPearls.clear();
            FixedPoint3DSolver.clearCache();
            return;
        }

        scanForNewPearls(client);
        removeGonePearls(client);
    }

    public Collection<PearlTrack> getFlyingPearls() {
        return Collections.unmodifiableCollection(flyingPearls.values());
    }

    public PearlTrack getTrackByIndex(int index) {
        if (index < 1) {
            return null;
        }

        int current = 1;
        for (PearlTrack track : flyingPearls.values()) {
            if (current == index) {
                return track;
            }
            current++;
        }

        return null;
    }

    public void clear() {
        flyingPearls.clear();
        FixedPoint3DSolver.clearCache();
    }

    /**
     * Entity.age is the completed game-tick age. Render interpolation is added
     * only while the world is not frozen; otherwise /tick freeze would display
     * a false extra tick.
     */
    public double getAgeTicks(Minecraft client, PearlTrack track, float tickProgress) {
        Entity entity = getPearlEntity(client, track);
        if (entity == null) {
            return -1.0;
        }

        double renderFraction = 0.0;
        if (client.level != null && !client.level.tickRateManager().isFrozen()) {
            renderFraction = Math.max(0.0, Math.min(1.0, tickProgress));
        }

        return Math.max(0.0, entity.tickCount + renderFraction);
    }

    public ThrownEnderpearl getPearlEntity(Minecraft client, PearlTrack track) {
        if (client.level == null) {
            return null;
        }

        Entity entity = client.level.getEntity(track.entityId());
        if (!(entity instanceof ThrownEnderpearl pearl)) {
            return null;
        }

        return pearl.getUUID().equals(track.uuid()) ? pearl : null;
    }

    private void scanForNewPearls(Minecraft client) {
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof ThrownEnderpearl pearl)) {
                continue;
            }

            if (!isOwnedByClientPlayer(pearl, client)) {
                continue;
            }

            UUID uuid = pearl.getUUID();
            if (flyingPearls.containsKey(uuid)) {
                continue;
            }

            Vec3 velocity = pearl.getDeltaMovement();
            double actualAlpha = AngleCalculator.getVelocityElevationDeg(velocity);
            Vec3 pearlStartPos = new Vec3(pearl.getX(), pearl.getY(), pearl.getZ());
            Vec3 throwerStartPos = new Vec3(
                    client.player.getX(),
                    client.player.getY(),
                    client.player.getZ()
            );
            Vec3 fallbackLook = AngleCalculator.getHorizontalLookVector(client.player);
            PearlCoordinateFrame frame = PearlCoordinateFrame.create(
                    pearlStartPos,
                    velocity,
                    fallbackLook
            );

            flyingPearls.put(uuid, new PearlTrack(
                    uuid,
                    pearl.getId(),
                    actualAlpha,
                    pearlStartPos,
                    throwerStartPos,
                    velocity,
                    frame
            ));
        }
    }

    private void removeGonePearls(Minecraft client) {
        flyingPearls.entrySet().removeIf(
                entry -> getPearlEntity(client, entry.getValue()) == null
        );
    }

    private static boolean isOwnedByClientPlayer(
            ThrownEnderpearl pearl,
            Minecraft client
    ) {
        if (client.player == null) {
            return false;
        }

        Entity owner = pearl.getOwner();
        return owner != null && owner.getUUID().equals(client.player.getUUID());
    }
}
