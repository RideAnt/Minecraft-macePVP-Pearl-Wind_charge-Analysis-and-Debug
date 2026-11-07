package com.rideant.pearlwindchargeanalysisdebug;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PearlWindChargeAnalysisDebugModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PearlWindChargeAnalysisDebugModMenu::createConfigScreen;
    }

    private static Screen createConfigScreen(Screen parent) {
        ModConfig.Config config = ModConfig.get();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable(
                        "title.pearlwind_chargeanalysis_debug.config"
                ));

        ConfigEntryBuilder entries = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(
                Component.translatable(
                        "category.pearlwind_chargeanalysis_debug.general"
                )
        );

        general.addEntry(entries.startBooleanToggle(
                        Component.translatable(
                                "option.pearlwind_chargeanalysis_debug.debug_auto_aim"
                        ),
                        config.debugAutoAim
                )
                .setDefaultValue(false)
                .setSaveConsumer(value -> config.debugAutoAim = value)
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.translatable(
                                "option.pearlwind_chargeanalysis_debug.remove_random_offset"
                        ),
                        config.removeRandomOffset
                )
                .setDefaultValue(false)
                .setSaveConsumer(value -> config.removeRandomOffset = value)
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.translatable(
                                "option.pearlwind_chargeanalysis_debug.debug_log"
                        ),
                        config.debugLog
                )
                .setDefaultValue(false)
                .setSaveConsumer(value -> config.debugLog = value)
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.translatable(
                                "option.pearlwind_chargeanalysis_debug.trajectory_trace"
                        ),
                        config.trajectoryTrace
                )
                .setDefaultValue(false)
                .setSaveConsumer(value -> config.trajectoryTrace = value)
                .build());

        builder.setSavingRunnable(ModConfig::save);
        return builder.build();
    }
}
