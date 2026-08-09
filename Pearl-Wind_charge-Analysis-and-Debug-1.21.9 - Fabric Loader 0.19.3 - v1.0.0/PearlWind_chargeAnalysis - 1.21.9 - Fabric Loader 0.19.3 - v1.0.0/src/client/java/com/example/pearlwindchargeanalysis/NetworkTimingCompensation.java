package com.example.pearlwindchargeanalysis;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

/**
 * Optional multiplayer timing compensation.
 *
 * The tab-list latency value is treated as a full round-trip delay. When the
 * option is enabled in multiplayer, the fixed-point solver evaluates the
 * pearl at:
 *
 *   effective age = client-observed age + latencyMs / 50
 *
 * Singleplayer always uses the unmodified age.
 */
public final class NetworkTimingCompensation {
    private static final double MILLIS_PER_TICK = 50.0;

    private NetworkTimingCompensation() {
    }

    public static double getEffectivePearlAgeTicks(MinecraftClient client, double clientAgeTick) {
        if (!Double.isFinite(clientAgeTick)
                || client == null
                || client.player == null
                || client.isInSingleplayer()
                || !ModConfig.get().serverTimingCompensation) {
            return clientAgeTick;
        }

        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler == null) {
            return clientAgeTick;
        }

        PlayerListEntry entry = networkHandler.getPlayerListEntry(client.player.getUuid());
        if (entry == null) {
            return clientAgeTick;
        }

        int latencyMs = Math.max(0, entry.getLatency());
        return clientAgeTick + latencyMs / MILLIS_PER_TICK;
    }
}
