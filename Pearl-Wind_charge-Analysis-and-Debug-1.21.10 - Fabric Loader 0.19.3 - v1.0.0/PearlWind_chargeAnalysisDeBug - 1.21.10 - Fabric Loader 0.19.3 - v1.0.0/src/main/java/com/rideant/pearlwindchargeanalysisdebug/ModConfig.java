package com.rideant.pearlwindchargeanalysisdebug;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ModConfig {
    private static final String FILE_NAME =
            "pearlwind_chargeanalysis_debug.properties";
    private static final String LEGACY_FILE_NAME =
            "projectileaccuracytest.properties";
    private static final String MAIN_FILE_NAME =
            "pearlwind_chargeanalysis.properties";

    private static final String REMOVE_RANDOM_OFFSET_KEY = "removeRandomOffset";
    private static final String DEBUG_LOG_KEY = "debugLog";
    private static final String DEBUG_AUTO_AIM_KEY = "debugAutoAim";

    private static Config config;

    private ModConfig() {
    }

    public static Config get() {
        if (config == null) {
            config = load();
        }
        return config;
    }

    public static void save() {
        Config current = get();
        Path path = configPath(FILE_NAME);
        Properties properties = new Properties();
        properties.setProperty(
                REMOVE_RANDOM_OFFSET_KEY,
                Boolean.toString(current.removeRandomOffset)
        );
        properties.setProperty(
                DEBUG_LOG_KEY,
                Boolean.toString(current.debugLog)
        );
        properties.setProperty(
                DEBUG_AUTO_AIM_KEY,
                Boolean.toString(current.debugAutoAim)
        );

        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                properties.store(
                        writer,
                        "PearlWind_chargeAnalysisDeBug settings"
                );
            }
        } catch (IOException exception) {
            PearlWindChargeAnalysisDebug.LOGGER.warn(
                    "Unable to save config {}",
                    path,
                    exception
            );
        }
    }

    private static Config load() {
        Path currentPath = configPath(FILE_NAME);
        if (Files.exists(currentPath)) {
            return loadCurrent(currentPath);
        }

        Config migrated = new Config();
        readLegacyProjectileSettings(migrated);
        readLegacyMainDebugSetting(migrated);
        return migrated;
    }

    private static Config loadCurrent(Path path) {
        Config loaded = new Config();
        Properties properties = readProperties(path);
        loaded.removeRandomOffset = parseBoolean(
                properties,
                REMOVE_RANDOM_OFFSET_KEY,
                false
        );
        loaded.debugLog = parseBoolean(properties, DEBUG_LOG_KEY, false);
        loaded.debugAutoAim = parseBoolean(
                properties,
                DEBUG_AUTO_AIM_KEY,
                false
        );
        return loaded;
    }

    private static void readLegacyProjectileSettings(Config target) {
        Properties properties = readProperties(configPath(LEGACY_FILE_NAME));
        target.removeRandomOffset = parseBoolean(
                properties,
                REMOVE_RANDOM_OFFSET_KEY,
                false
        );
        target.debugLog = parseBoolean(properties, DEBUG_LOG_KEY, false);
    }

    private static void readLegacyMainDebugSetting(Config target) {
        Properties properties = readProperties(configPath(MAIN_FILE_NAME));
        target.debugAutoAim = parseBoolean(
                properties,
                DEBUG_AUTO_AIM_KEY,
                false
        );
    }

    private static Properties readProperties(Path path) {
        Properties properties = new Properties();
        if (!Files.exists(path)) {
            return properties;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        } catch (IOException exception) {
            PearlWindChargeAnalysisDebug.LOGGER.warn(
                    "Unable to read config {}",
                    path,
                    exception
            );
        }
        return properties;
    }

    private static boolean parseBoolean(
            Properties properties,
            String key,
            boolean fallback
    ) {
        String value = properties.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private static Path configPath(String fileName) {
        return FabricLoader.getInstance().getConfigDir().resolve(fileName);
    }

    public static final class Config {
        public volatile boolean removeRandomOffset;
        public volatile boolean debugLog;
        public volatile boolean debugAutoAim;
    }
}
