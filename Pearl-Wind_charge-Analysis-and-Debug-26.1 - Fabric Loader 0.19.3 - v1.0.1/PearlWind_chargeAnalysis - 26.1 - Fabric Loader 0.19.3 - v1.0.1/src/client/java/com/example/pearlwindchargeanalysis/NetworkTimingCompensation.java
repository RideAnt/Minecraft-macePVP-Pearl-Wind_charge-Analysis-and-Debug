package com.example.pearlwindchargeanalysis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

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

    public static double getEffectivePearlAgeTicks(Minecraft client, double clientAgeTick) {
        if (!Double.isFinite(clientAgeTick)
                || client == null
                || client.player == null
                || client.isLocalServer()
                || !ModConfig.get().serverTimingCompensation) {
            return clientAgeTick;
        }

        ClientPacketListener networkHandler = client.getConnection();
        if (networkHandler == null) {
            return clientAgeTick;
        }

        PlayerInfo entry = networkHandler.getPlayerInfo(client.player.getUUID());
        if (entry == null) {
            return clientAgeTick;
        }

        int latencyMs = Math.max(0, entry.getLatency());
        return clientAgeTick + latencyMs / MILLIS_PER_TICK;
    }
}
