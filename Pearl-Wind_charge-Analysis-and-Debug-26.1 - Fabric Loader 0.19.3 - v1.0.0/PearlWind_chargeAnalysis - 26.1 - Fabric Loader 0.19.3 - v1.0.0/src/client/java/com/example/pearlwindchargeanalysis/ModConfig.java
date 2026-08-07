package com.example.pearlwindchargeanalysis;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Client config persisted in
 * .minecraft/config/pearlwind_chargeanalysis.properties.
 */
public final class ModConfig {
    private static final String FILE_NAME =
            "pearlwind_chargeanalysis.properties";

    private static final String MODE_KEY = "analysisMode";
    private static final String ENABLED_KEY = "enabled";
    private static final String SHOW_ASSIST_HUD_KEY = "showAssistHud";
    private static final String ASSIST_LINE_MODE_KEY = "assistLineMode";
    private static final String BEST_BETA_MIN_KEY = "bestBetaMin";
    private static final String BEST_BETA_MAX_KEY = "bestBetaMax";
    private static final String VERTICAL_AIM_MAX_KEY = "verticalAimMax";
    private static final String HORIZONTAL_AIM_MAX_KEY = "horizontalAimMax";
    private static final String SERVER_TIMING_COMPENSATION_KEY =
            "serverTimingCompensation";
    private static final String VALIDATE_PATH_COLLISIONS_KEY =
            "validatePathCollisions";

    private static final double DEFAULT_BEST_BETA_MIN = -90.0;
    private static final double DEFAULT_BEST_BETA_MAX = 90.0;
    private static final double DEFAULT_VERTICAL_AIM_MAX = 30.0;
    private static final double DEFAULT_HORIZONTAL_AIM_MAX = 30.0;

    private static Config CONFIG;

    private ModConfig() {
    }

    public static Config get() {
        if (CONFIG == null) {
            CONFIG = load();
        }

        return CONFIG;
    }

    public static AnalysisMode loadMode() {
        return get().analysisMode;
    }

    public static void saveMode(AnalysisMode mode) {
        Config config = get();
        config.analysisMode = mode == null
                ? AnalysisMode.FIXED_POINT
                : mode;
        save();
    }

    public static void save() {
        Config config = get();
        config.sanitize();

        Path path = getConfigPath();
        Properties properties = new Properties();

        properties.setProperty(
                MODE_KEY,
                config.analysisMode.configValue()
        );
        properties.setProperty(
                ENABLED_KEY,
                Boolean.toString(config.enabled)
        );
        properties.setProperty(
                SHOW_ASSIST_HUD_KEY,
                Boolean.toString(config.showAssistHud)
        );
        properties.setProperty(
                ASSIST_LINE_MODE_KEY,
                config.assistLineMode.configValue()
        );
        properties.setProperty(
                BEST_BETA_MIN_KEY,
                Double.toString(config.bestBetaMin)
        );
        properties.setProperty(
                BEST_BETA_MAX_KEY,
                Double.toString(config.bestBetaMax)
        );
        properties.setProperty(
                VERTICAL_AIM_MAX_KEY,
                Double.toString(config.verticalAimMax)
        );
        properties.setProperty(
                HORIZONTAL_AIM_MAX_KEY,
                Double.toString(config.horizontalAimMax)
        );
        properties.setProperty(
                SERVER_TIMING_COMPENSATION_KEY,
                Boolean.toString(config.serverTimingCompensation)
        );
        properties.setProperty(
                VALIDATE_PATH_COLLISIONS_KEY,
                Boolean.toString(config.validatePathCollisions)
        );

        try {
            Files.createDirectories(path.getParent());

            try (Writer writer = Files.newBufferedWriter(path)) {
                properties.store(
                        writer,
                        "PearlWind_chargeAnalysis client settings"
                );
            }
        } catch (IOException ignored) {
            // A config write failure must not crash the client.
        }
    }

    private static Config load() {
        Config config = new Config();
        Path path = getConfigPath();

        if (!Files.exists(path)) {
            config.sanitize();
            return config;
        }

        Properties properties = new Properties();

        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);

            config.analysisMode =
                    AnalysisMode.fromConfigValue(
                            properties.getProperty(MODE_KEY)
                    );
            config.enabled = parseBoolean(
                    properties.getProperty(ENABLED_KEY),
                    true
            );
            config.showAssistHud = parseBoolean(
                    properties.getProperty(SHOW_ASSIST_HUD_KEY),
                    true
            );
            config.assistLineMode = AssistLineMode.fromConfigValue(
                    properties.getProperty(ASSIST_LINE_MODE_KEY)
            );
            config.bestBetaMin = parseDouble(
                    properties.getProperty(BEST_BETA_MIN_KEY),
                    DEFAULT_BEST_BETA_MIN
            );
            config.bestBetaMax = parseDouble(
                    properties.getProperty(BEST_BETA_MAX_KEY),
                    DEFAULT_BEST_BETA_MAX
            );
            config.verticalAimMax = parseDouble(
                    properties.getProperty(VERTICAL_AIM_MAX_KEY),
                    DEFAULT_VERTICAL_AIM_MAX
            );
            config.horizontalAimMax = parseDouble(
                    properties.getProperty(HORIZONTAL_AIM_MAX_KEY),
                    DEFAULT_HORIZONTAL_AIM_MAX
            );
            config.serverTimingCompensation = parseBoolean(
                    properties.getProperty(SERVER_TIMING_COMPENSATION_KEY),
                    false
            );
            config.validatePathCollisions = parseBoolean(
                    properties.getProperty(VALIDATE_PATH_COLLISIONS_KEY),
                    false
            );
        } catch (IOException ignored) {
            // Use defaults.
        }

        config.sanitize();
        return config;
    }

    private static boolean parseBoolean(
            String value,
            boolean fallback
    ) {
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private static double parseDouble(
            String value,
            double fallback
    ) {
        if (value == null) {
            return fallback;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve(FILE_NAME);
    }

    public static final class Config {
        public boolean enabled = true;
        public boolean showAssistHud = true;
        public AssistLineMode assistLineMode = AssistLineMode.INDEPENDENT_AXES;
        public double bestBetaMin = DEFAULT_BEST_BETA_MIN;
        public double bestBetaMax = DEFAULT_BEST_BETA_MAX;
        public double verticalAimMax = DEFAULT_VERTICAL_AIM_MAX;
        public double horizontalAimMax = DEFAULT_HORIZONTAL_AIM_MAX;
        public boolean serverTimingCompensation = false;
        public boolean validatePathCollisions = false;
        public AnalysisMode analysisMode = AnalysisMode.FIXED_POINT;

        public void sanitize() {
            if (analysisMode == null) {
                analysisMode = AnalysisMode.FIXED_POINT;
            }

            if (assistLineMode == null) {
                assistLineMode = AssistLineMode.INDEPENDENT_AXES;
            }

            bestBetaMin = clamp(bestBetaMin, -90.0, 90.0);
            bestBetaMax = clamp(bestBetaMax, -90.0, 90.0);

            if (bestBetaMin > bestBetaMax) {
                double temp = bestBetaMin;
                bestBetaMin = bestBetaMax;
                bestBetaMax = temp;
            }

            verticalAimMax = clamp(verticalAimMax, 0.0, 180.0);
            horizontalAimMax = clamp(horizontalAimMax, 0.0, 180.0);
        }

        private static double clamp(
                double value,
                double min,
                double max
        ) {
            if (!Double.isFinite(value)) {
                return min;
            }

            return Math.max(min, Math.min(max, value));
        }
    }
}
