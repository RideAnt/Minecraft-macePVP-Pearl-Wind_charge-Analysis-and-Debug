package com.rideant.pearlwindchargeanalysisdebug;

import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
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
                "PearlWind_chargeAnalysisDebug loaded; random-offset removal is {}",
                ModConfig.get().removeRandomOffset ? "enabled" : "disabled"
        );
    }

    public static boolean shouldRemoveRandomOffset(ProjectileEntity projectile) {
        if (!ModConfig.get().removeRandomOffset || projectile == null) {
            return false;
        }

        World world = projectile.getWorld();
        if (world == null || world.isClient()) {
            return false;
        }

        MinecraftServer server = world.getServer();
        return server != null && !server.isDedicated();
    }

    public static void logApplied(
            ProjectileEntity projectile,
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
}
