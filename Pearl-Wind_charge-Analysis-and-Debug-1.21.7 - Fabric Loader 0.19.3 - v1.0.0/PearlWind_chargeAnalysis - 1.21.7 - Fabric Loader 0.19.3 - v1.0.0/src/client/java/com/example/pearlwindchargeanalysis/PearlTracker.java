package com.example.pearlwindchargeanalysis;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks ender pearls thrown by the local player.
 */
public final class PearlTracker {
    private final Map<UUID, PearlTrack> flyingPearls = new LinkedHashMap<>();

    public void tick(MinecraftClient client) {
        if (client.world == null || client.player == null) {
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
    public double getAgeTicks(MinecraftClient client, PearlTrack track, float tickProgress) {
        Entity entity = getPearlEntity(client, track);
        if (entity == null) {
            return -1.0;
        }

        double renderFraction = 0.0;
        if (client.world != null && !client.world.getTickManager().isFrozen()) {
            renderFraction = Math.max(0.0, Math.min(1.0, tickProgress));
        }

        return Math.max(0.0, entity.age + renderFraction);
    }

    public EnderPearlEntity getPearlEntity(MinecraftClient client, PearlTrack track) {
        if (client.world == null) {
            return null;
        }

        Entity entity = client.world.getEntityById(track.entityId());
        if (!(entity instanceof EnderPearlEntity pearl)) {
            return null;
        }

        return pearl.getUuid().equals(track.uuid()) ? pearl : null;
    }

    private void scanForNewPearls(MinecraftClient client) {
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof EnderPearlEntity pearl)) {
                continue;
            }

            if (!isOwnedByClientPlayer(pearl, client)) {
                continue;
            }

            UUID uuid = pearl.getUuid();
            if (flyingPearls.containsKey(uuid)) {
                continue;
            }

            Vec3d velocity = pearl.getVelocity();
            double actualAlpha = AngleCalculator.getVelocityElevationDeg(velocity);
            Vec3d pearlStartPos = new Vec3d(pearl.getX(), pearl.getY(), pearl.getZ());
            Vec3d throwerStartPos = new Vec3d(
                    client.player.getX(),
                    client.player.getY(),
                    client.player.getZ()
            );
            Vec3d fallbackLook = AngleCalculator.getHorizontalLookVector(client.player);
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

    private void removeGonePearls(MinecraftClient client) {
        flyingPearls.entrySet().removeIf(
                entry -> getPearlEntity(client, entry.getValue()) == null
        );
    }

    private static boolean isOwnedByClientPlayer(
            EnderPearlEntity pearl,
            MinecraftClient client
    ) {
        if (client.player == null) {
            return false;
        }

        Entity owner = pearl.getOwner();
        return owner != null && owner.getUuid().equals(client.player.getUuid());
    }
}
