package com.example.pearlwindchargeanalysis;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.Vec3;

/**
 * Tracks every local-player pearl and, when enabled, the earliest currently
 * flying pearl for each other player.
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
        boolean includeOtherPlayers = ModConfig.get().interceptOtherPlayers;
        Map<UUID, OtherPearlCandidate> earliestOtherPearls =
                new LinkedHashMap<>();

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof ThrownEnderpearl pearl)) {
                continue;
            }

            Entity ownerEntity = pearl.getOwner();
            if (!(ownerEntity instanceof Player owner)) {
                continue;
            }

            boolean ownedByLocalPlayer = owner.getUUID().equals(
                    client.player.getUUID()
            );
            if (ownedByLocalPlayer) {
                trackIfAbsent(client, pearl, owner, true);
                continue;
            }

            if (!includeOtherPlayers) {
                continue;
            }

            OtherPearlCandidate candidate = new OtherPearlCandidate(
                    pearl,
                    owner
            );
            OtherPearlCandidate current = earliestOtherPearls.get(
                    owner.getUUID()
            );
            if (current == null || isEarlier(candidate.pearl, current.pearl)) {
                earliestOtherPearls.put(owner.getUUID(), candidate);
            }
        }

        Set<UUID> selectedOtherPearlUuids = new HashSet<>();
        if (includeOtherPlayers) {
            for (OtherPearlCandidate candidate : earliestOtherPearls.values()) {
                selectedOtherPearlUuids.add(candidate.pearl.getUUID());
            }
        }

        removeUnselectedOtherPearls(selectedOtherPearlUuids);

        for (OtherPearlCandidate candidate : earliestOtherPearls.values()) {
            trackIfAbsent(client, candidate.pearl, candidate.owner, false);
        }
    }

    private void removeGonePearls(Minecraft client) {
        Iterator<Map.Entry<UUID, PearlTrack>> iterator =
                flyingPearls.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PearlTrack> entry = iterator.next();
            if (getPearlEntity(client, entry.getValue()) == null) {
                FixedPoint3DSolver.removeCache(entry.getKey());
                iterator.remove();
            }
        }
    }

    private void trackIfAbsent(
            Minecraft client,
            ThrownEnderpearl pearl,
            Player owner,
            boolean ownedByLocalPlayer
    ) {
        UUID uuid = pearl.getUUID();
        if (flyingPearls.containsKey(uuid)) {
            return;
        }

        Vec3 velocity = pearl.getDeltaMovement();
        double actualAlpha = AngleCalculator.getVelocityElevationDeg(velocity);
        Vec3 pearlStartPos = new Vec3(pearl.getX(), pearl.getY(), pearl.getZ());

        /*
         * For a local pearl this is the local throw point. For another
         * player's pearl it is the local analysis-start point, so fixed-point
         * mode still means "the interceptor stayed near where tracking began".
         */
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
                owner.getUUID(),
                owner.getName().getString(),
                ownedByLocalPlayer,
                actualAlpha,
                pearlStartPos,
                throwerStartPos,
                velocity,
                pearl.tickCount,
                frame
        ));
    }

    private void removeUnselectedOtherPearls(Set<UUID> selectedUuids) {
        Iterator<Map.Entry<UUID, PearlTrack>> iterator =
                flyingPearls.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PearlTrack> entry = iterator.next();
            PearlTrack track = entry.getValue();
            if (!track.ownedByLocalPlayer()
                    && !selectedUuids.contains(track.uuid())) {
                FixedPoint3DSolver.removeCache(entry.getKey());
                iterator.remove();
            }
        }
    }

    /** Older client age wins; entity id is the stable tie-breaker. */
    private static boolean isEarlier(
            ThrownEnderpearl candidate,
            ThrownEnderpearl current
    ) {
        if (candidate.tickCount != current.tickCount) {
            return candidate.tickCount > current.tickCount;
        }

        return candidate.getId() < current.getId();
    }

    private record OtherPearlCandidate(
            ThrownEnderpearl pearl,
            Player owner
    ) {
    }
}
