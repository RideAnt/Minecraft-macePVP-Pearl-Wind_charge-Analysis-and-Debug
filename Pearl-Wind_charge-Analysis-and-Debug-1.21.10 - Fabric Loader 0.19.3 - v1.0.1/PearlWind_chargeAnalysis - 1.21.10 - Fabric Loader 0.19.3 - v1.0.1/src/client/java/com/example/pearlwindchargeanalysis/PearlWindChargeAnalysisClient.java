package com.example.pearlwindchargeanalysis;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
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
    private static KeyBinding toggleModeKey;

    private static AnalysisMode analysisMode = AnalysisMode.FIXED_POINT;
    private static int modeNoticeTicks = 0;
    private static String modeNoticeText = "";

    @Override
    public void onInitializeClient() {
        // Load and sanitize configuration before registering callbacks.
        ModConfig.get();
        analysisMode = ModConfig.loadMode();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "category"));

        toggleModeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pearlwind_chargeanalysis.toggle_mode",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_BACKSLASH,
                category
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
                Identifier.of(MOD_ID, "angle_hud"),
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

    private static void handleKeyInput(MinecraftClient client) {
        while (toggleModeKey.wasPressed()) {
            analysisMode = analysisMode.next();
            ModConfig.saveMode(analysisMode);
            FixedPoint3DSolver.clearCache();
            modeNoticeText = net.minecraft.client.resource.language.I18n.translate("hud.pearlwind_chargeanalysis.mode", net.minecraft.client.resource.language.I18n.translate("mode.pearlwind_chargeanalysis." + analysisMode.configValue()));
            modeNoticeTicks = MODE_NOTICE_DURATION_TICKS;
        }
    }
}
