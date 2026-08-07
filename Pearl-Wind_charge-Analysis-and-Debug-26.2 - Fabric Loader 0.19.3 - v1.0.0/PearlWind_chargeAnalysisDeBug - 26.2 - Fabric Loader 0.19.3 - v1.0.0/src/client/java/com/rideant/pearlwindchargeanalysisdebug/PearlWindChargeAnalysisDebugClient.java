package com.rideant.pearlwindchargeanalysisdebug;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class PearlWindChargeAnalysisDebugClient
        implements ClientModInitializer {
    private static final int NOTICE_DURATION_TICKS = 20;

    private static KeyMapping toggleDebugLockKey;
    private static int lockedPearlIndex;
    private static int noticeTicks;
    private static String noticeText = "";

    @Override
    public void onInitializeClient() {
        ModConfig.get();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(PearlWindChargeAnalysisDebug.MOD_ID, "category")
        );
        toggleDebugLockKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pearlwind_chargeanalysis_debug.toggle_debug_lock",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                category,
                0
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            handleKeyInput(client);
            updateAutomaticLock(client);
            if (noticeTicks > 0) {
                noticeTicks--;
            }
        });

        LevelRenderEvents.START_MAIN.register(context ->
                DebugAimController.renderFrame(
                        Minecraft.getInstance(),
                        lockedPearlIndex
                )
        );

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(
                        PearlWindChargeAnalysisDebug.MOD_ID,
                        "debug_notice"
                ),
                DebugNoticeHud::render
        );
    }

    public static int getNoticeTicks() {
        return noticeTicks;
    }

    public static String getNoticeText() {
        return noticeText;
    }

    private static void handleKeyInput(Minecraft client) {
        while (toggleDebugLockKey.consumeClick()) {
            if (!ModConfig.get().debugAutoAim) {
                lockedPearlIndex = 0;
                showNotice("调试追踪未启用");
                continue;
            }

            if (client.player == null
                    || client.level == null
                    || client.getSingleplayerServer() == null) {
                lockedPearlIndex = 0;
                showNotice("不可锁定");
                continue;
            }

            boolean p1 = DebugAimController.isPearlLockable(1);
            boolean p2 = DebugAimController.isPearlLockable(2);
            if (!p1 && !p2) {
                lockedPearlIndex = 0;
                showNotice("不可锁定");
                continue;
            }

            if (lockedPearlIndex == 1) {
                lockedPearlIndex = p2 ? 2 : 1;
            } else if (lockedPearlIndex == 2) {
                lockedPearlIndex = p1 ? 1 : 2;
            } else {
                lockedPearlIndex = p1 ? 1 : 2;
            }
            showNotice("锁定 P" + lockedPearlIndex);
        }
    }

    private static void updateAutomaticLock(Minecraft client) {
        if (!DebugAimController.canOperate(client)) {
            lockedPearlIndex = 0;
            return;
        }

        boolean p1 = DebugAimController.isPearlLockable(1);
        boolean p2 = DebugAimController.isPearlLockable(2);
        if (!p1 && !p2) {
            lockedPearlIndex = 0;
            return;
        }

        if ((lockedPearlIndex == 1 && p1)
                || (lockedPearlIndex == 2 && p2)) {
            return;
        }
        lockedPearlIndex = p1 ? 1 : 2;
    }

    private static void showNotice(String text) {
        noticeText = text;
        noticeTicks = NOTICE_DURATION_TICKS;
    }
}
