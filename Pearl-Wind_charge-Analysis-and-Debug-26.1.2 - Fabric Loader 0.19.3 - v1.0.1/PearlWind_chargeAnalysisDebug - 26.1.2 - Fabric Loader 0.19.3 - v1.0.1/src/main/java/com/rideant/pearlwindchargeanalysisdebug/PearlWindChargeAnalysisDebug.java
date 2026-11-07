package com.rideant.pearlwindchargeanalysisdebug;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional singleplayer debug companion for PearlWind_chargeAnalysis.
 */
public final class PearlWindChargeAnalysisDebug implements ModInitializer {
    public static final String MOD_ID = "pearlwind_chargeanalysis_debug";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModConfig.get();
        LOGGER.info(
                "PearlWind_chargeAnalysisDebug loaded; random-offset removal is {}, trajectory trace is {}",
                ModConfig.get().removeRandomOffset ? "enabled" : "disabled",
                ModConfig.get().trajectoryTrace ? "enabled" : "disabled"
        );
        ServerTickEvents.END_SERVER_TICK.register(
                ProjectileTraceLogger::onEndServerTick
        );
    }

    public static boolean shouldRemoveRandomOffset(Projectile projectile) {
        if (!ModConfig.get().removeRandomOffset || projectile == null) {
            return false;
        }

        Level world = projectile.level();
        if (world == null || world.isClientSide()) {
            return false;
        }

        MinecraftServer server = world.getServer();
        return server != null && !server.isDedicatedServer();
    }

    public static void logApplied(
            Projectile projectile,
            float originalUncertainty
    ) {
        if (!ModConfig.get().debugLog) {
            return;
        }

        LOGGER.info(
                "Removed projectile random offset: type={}, uncertainty={}",
                projectile.getType(),
                originalUncertainty
        );
    }

    public static boolean shouldLogShootDirection() {
        return ModConfig.get().debugLog;
    }

    public static void logShootDirection(
            Projectile projectile,
            double x,
            double y,
            double z
    ) {
        double length = Math.sqrt(x * x + y * y + z * z);
        String direction;
        if (length < 1.0E-6) {
            direction = "(zero vector)";
        } else {
            double yaw = Math.toDegrees(Math.atan2(-x, z));
            double pitch = Math.toDegrees(-Math.asin(y / length));
            direction = String.format(
                    java.util.Locale.ROOT,
                    "yaw=%.2f pitch=%.2f vec=(%.3f,%.3f,%.3f)",
                    yaw,
                    pitch,
                    x,
                    y,
                    z
            );
        }

        LOGGER.info(
                "Projectile shoot direction: type={}, {}",
                projectile.getType(),
                direction
        );
    }
}
