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
                && client.getSingleplayerServer() != null
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

        double[] diagnostics = lockedPearlIndex > 0
                ? MainAnalysisBridge.resolveModelDiagnostics(lockedPearlIndex)
                : null;
        if (diagnostics != null) {
            logModelState(lockedPearlIndex, target, diagnostics);
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

    private static void logModelState(
            int lockedPearlIndex,
            double[] target,
            double[] values
    ) {
        int statusOrdinal = (int) values[0];
        String status = switch (statusOrdinal) {
            case 0 -> "VALID";
            case 1 -> "NO_SOLUTION";
            case 2 -> "NON_AIR_PATH";
            case 3 -> "BLOCKED_PATH";
            default -> "UNKNOWN_" + statusOrdinal;
        };
        String targetText = target == null
                ? "null"
                : String.format(
                        Locale.ROOT,
                        "yaw=%.3f,pitch=%.3f",
                        target[0],
                        target[1]
                );

        PearlWindChargeAnalysisDebug.LOGGER.info(String.format(
                Locale.ROOT,
                "PWMODEL lock=P%d target={%s} status=%s clientAge=%.3f effectiveA=%.3f anchorA=%.3f encounterT=%.3f windMoves=%.3f launch=(%.6f,%.6f,%.6f) encounter=(%.6f,%.6f,%.6f) localDelta=(u=%.6f,y=%.6f,w=%.6f) residual=%+.9f driftXZ=%.6f inherited=(%.6f,%.6f,%.6f) beta=%.6f yaw=%.6f",
                lockedPearlIndex,
                targetText,
                status,
                values[1],
                values[2],
                values[3],
                values[4],
                values[5],
                values[6],
                values[7],
                values[8],
                values[9],
                values[10],
                values[11],
                values[12],
                values[13],
                values[14],
                values[15],
                values[16],
                values[17],
                values[18],
                values[19],
                values[20],
                values[21]
        ));
    }
}
