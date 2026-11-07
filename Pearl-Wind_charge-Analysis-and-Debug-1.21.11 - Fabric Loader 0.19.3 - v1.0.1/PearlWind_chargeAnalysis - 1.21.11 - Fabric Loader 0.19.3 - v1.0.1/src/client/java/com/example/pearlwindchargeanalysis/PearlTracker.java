package com.example.pearlwindchargeanalysis;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Tracks every local-player pearl and, when enabled, the earliest currently
 * flying pearl for each other player.
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
        boolean includeOtherPlayers = ModConfig.get().interceptOtherPlayers;
        Map<UUID, OtherPearlCandidate> earliestOtherPearls =
                new LinkedHashMap<>();

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof EnderPearlEntity pearl)) {
                continue;
            }

            Entity ownerEntity = pearl.getOwner();
            if (!(ownerEntity instanceof PlayerEntity owner)) {
                continue;
            }

            boolean ownedByLocalPlayer = owner.getUuid().equals(
                    client.player.getUuid()
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
                    owner.getUuid()
            );
            if (current == null || isEarlier(candidate.pearl, current.pearl)) {
                earliestOtherPearls.put(owner.getUuid(), candidate);
            }
        }

        Set<UUID> selectedOtherPearlUuids = new HashSet<>();
        if (includeOtherPlayers) {
            for (OtherPearlCandidate candidate : earliestOtherPearls.values()) {
                selectedOtherPearlUuids.add(candidate.pearl.getUuid());
            }
        }

        removeUnselectedOtherPearls(selectedOtherPearlUuids);

        for (OtherPearlCandidate candidate : earliestOtherPearls.values()) {
            trackIfAbsent(client, candidate.pearl, candidate.owner, false);
        }
    }

    private void removeGonePearls(MinecraftClient client) {
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
            MinecraftClient client,
            EnderPearlEntity pearl,
            PlayerEntity owner,
            boolean ownedByLocalPlayer
    ) {
        UUID uuid = pearl.getUuid();
        if (flyingPearls.containsKey(uuid)) {
            return;
        }

        Vec3d velocity = pearl.getVelocity();
        double actualAlpha = AngleCalculator.getVelocityElevationDeg(velocity);
        Vec3d pearlStartPos = new Vec3d(pearl.getX(), pearl.getY(), pearl.getZ());

        /*
         * For a local pearl this is the local throw point. For another
         * player's pearl it is the local analysis-start point, so fixed-point
         * mode still means "the interceptor stayed near where tracking began".
         */
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
                owner.getUuid(),
                owner.getName().getString(),
                ownedByLocalPlayer,
                actualAlpha,
                pearlStartPos,
                throwerStartPos,
                velocity,
                pearl.age,
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
            EnderPearlEntity candidate,
            EnderPearlEntity current
    ) {
        if (candidate.age != current.age) {
            return candidate.age > current.age;
        }

        return candidate.getId() < current.getId();
    }

    private record OtherPearlCandidate(
            EnderPearlEntity pearl,
            PlayerEntity owner
    ) {
    }
}

