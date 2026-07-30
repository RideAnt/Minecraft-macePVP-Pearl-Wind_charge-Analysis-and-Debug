package com.rideant.pearlwindchargeanalysisdebug;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;

public final class DebugAimController {
    private DebugAimController() {
    }

    public static boolean isPearlLockable(int index) {
        return MainAnalysisBridge.resolveAimTarget(index) != null;
    }

    public static void renderFrame(Minecraft client, int lockedPearlIndex) {
        if (!canOperate(client) || lockedPearlIndex == 0) {
            logState(client, lockedPearlIndex, null);
            return;
        }

        double[] target = MainAnalysisBridge.resolveAimTarget(lockedPearlIndex);
        logState(client, lockedPearlIndex, target);
        if (target == null) {
            return;
        }

        double targetYaw = target[0];
        double targetPitch = Mth.clamp(target[1], -90.0, 90.0);
        client.player.yRotO = (float) targetYaw;
        client.player.xRotO = (float) targetPitch;
        client.player.setYRot((float) targetYaw);
        client.player.setXRot((float) targetPitch);
    }

    public static boolean canOperate(Minecraft client) {
        return ModConfig.get().debugAutoAim
                && client != null
                && client.player != null
                && client.level != null
                && client.isInSingleplayer()
                && (
                client.player.getMainHandItem().is(Items.WIND_CHARGE)
                        || client.player.getOffhandItem().is(Items.WIND_CHARGE)
        );
    }

    private static int frameCounter;

    /**
     * Throttled diagnostic of the auto-aim state. Logs roughly 3 times per
     * second, only while holding a wind charge and with debugLog enabled.
     */
    private static void logState(
            Minecraft client,
            int lockedPearlIndex,
            double[] target
    ) {
        if (!ModConfig.get().debugLog) {
            return;
        }
        if (++frameCounter % 20 != 0) {
            return;
        }
        if (client == null || client.player == null) {
            return;
        }
        boolean holding = client.player.getMainHandItem().is(Items.WIND_CHARGE)
                || client.player.getOffhandItem().is(Items.WIND_CHARGE);
        if (!holding) {
            return;
        }
        PearlWindChargeAnalysisDebug.LOGGER.info(
                "auto-aim: lock={} target={}",
                lockedPearlIndex,
                target == null
                        ? "null"
                        : String.format(
                                Locale.ROOT,
                                "yaw=%.2f pitch=%.2f",
                                target[0],
                                target[1]
                        )
        );
    }
}
