package com.example.pearlwindchargeanalysis;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only entrypoint.
 *
 * Press O to switch between fixed-point and moving-point analysis.
 */
public final class PearlWindChargeAnalysisClient implements ClientModInitializer {
    public static final String MOD_ID = "pearlwind_chargeanalysis";
    public static final PearlTracker TRACKER = new PearlTracker();

    private static final int MODE_NOTICE_DURATION_TICKS = 60;
    private static KeyMapping toggleModeKey;

    private static AnalysisMode analysisMode = AnalysisMode.FIXED_POINT;
    private static int modeNoticeTicks = 0;
    private static String modeNoticeText = "";

    @Override
    public void onInitializeClient() {
        // Load and sanitize configuration before registering callbacks.
        ModConfig.get();
        analysisMode = ModConfig.loadMode();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "category"));

        toggleModeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pearlwind_chargeanalysis.toggle_mode",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_BACKSLASH,
                category,
                0
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfig.get().enabled) {
                TRACKER.clear();
                modeNoticeTicks = 0;
                return;
            }

            TRACKER.tick(client);
            handleKeyInput(client);
            tickModeNotice();
        });

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "angle_hud"),
                HudRenderer::render
        );
    }

    public static AnalysisMode getAnalysisMode() {
        return analysisMode;
    }

    public static int getModeNoticeTicks() {
        return modeNoticeTicks;
    }

    public static String getModeNoticeText() {
        return modeNoticeText;
    }

    public static void tickModeNotice() {
        if (modeNoticeTicks > 0) {
            modeNoticeTicks--;
        }
    }

    /**
     * Kept for the Mod Menu save callback.
     */
    public static void refreshModeFromConfig() {
        analysisMode = ModConfig.loadMode();
        FixedPoint3DSolver.clearCache();
    }

    private static void handleKeyInput(Minecraft client) {
        while (toggleModeKey.consumeClick()) {
            analysisMode = analysisMode.next();
            ModConfig.saveMode(analysisMode);
            FixedPoint3DSolver.clearCache();
            modeNoticeText = net.minecraft.client.resources.language.I18n.get(
                    "hud.pearlwind_chargeanalysis.mode",
                    net.minecraft.client.resources.language.I18n.get(
                            "mode.pearlwind_chargeanalysis." + analysisMode.configValue()));
            modeNoticeTicks = MODE_NOTICE_DURATION_TICKS;
        }
    }
}
