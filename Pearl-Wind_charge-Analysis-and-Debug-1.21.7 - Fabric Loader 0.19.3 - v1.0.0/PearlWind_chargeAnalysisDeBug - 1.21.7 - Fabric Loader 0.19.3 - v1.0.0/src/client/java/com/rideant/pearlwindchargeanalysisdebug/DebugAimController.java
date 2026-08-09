package com.rideant.pearlwindchargeanalysisdebug;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;

public final class DebugAimController {
    private DebugAimController() {
    }

    public static boolean isPearlLockable(int index) {
        return MainAnalysisBridge.resolveAimTarget(index) != null;
    }

    public static void renderFrame(MinecraftClient client, int lockedPearlIndex) {
        if (!canOperate(client) || lockedPearlIndex == 0) {
            return;
        }

        double[] target = MainAnalysisBridge.resolveAimTarget(lockedPearlIndex);
        if (target == null) {
            return;
        }

        double targetYaw = target[0];
        double targetPitch = MathHelper.clamp(target[1], -90.0, 90.0);
        client.player.lastYaw = (float) targetYaw;
        client.player.lastPitch = (float) targetPitch;
        client.player.setYaw((float) targetYaw);
        client.player.setPitch((float) targetPitch);
    }

    public static boolean canOperate(MinecraftClient client) {
        return ModConfig.get().debugAutoAim
                && client != null
                && client.player != null
                && client.world != null
                && client.isInSingleplayer()
                && (
                client.player.getMainHandStack().isOf(Items.WIND_CHARGE)
                        || client.player.getOffHandStack().isOf(Items.WIND_CHARGE)
        );
    }
}
