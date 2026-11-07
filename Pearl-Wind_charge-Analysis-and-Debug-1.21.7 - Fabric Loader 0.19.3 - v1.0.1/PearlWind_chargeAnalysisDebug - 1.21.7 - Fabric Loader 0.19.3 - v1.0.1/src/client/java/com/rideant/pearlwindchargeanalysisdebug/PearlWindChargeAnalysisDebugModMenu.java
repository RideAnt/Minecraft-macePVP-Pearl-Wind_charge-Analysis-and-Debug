package com.rideant.pearlwindchargeanalysisdebug;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class PearlWindChargeAnalysisDebugModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PearlWindChargeAnalysisDebugModMenu::createConfigScreen;
    }

    private static Screen createConfigScreen(Screen parent) {
        ModConfig.Config config = ModConfig.get();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable(
                        "title.pearlwind_chargeanalysis_debug.config"
                ));

        ConfigEntryBuilder entries = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(
                Text.translatable(
                        "category.pearlwind_chargeanalysis_debug.general"
                )
        );

        general.addEntry(entries.startBooleanToggle(
                        Text.translatable(
                                "option.pearlwind_chargeanalysis_debug.debug_auto_aim"
                        ),
                        config.debugAutoAim
                )
                .setDefaultValue(false)
                .setSaveConsumer(value -> config.debugAutoAim = value)
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Text.translatable(
                                "option.pearlwind_chargeanalysis_debug.remove_random_offset"
                        ),
                        config.removeRandomOffset
                )
                .setDefaultValue(false)
                .setSaveConsumer(value -> config.removeRandomOffset = value)
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Text.translatable(
                                "option.pearlwind_chargeanalysis_debug.debug_log"
                        ),
                        config.debugLog
                )
                .setDefaultValue(false)
                .setSaveConsumer(value -> config.debugLog = value)
                .build());

        builder.setSavingRunnable(ModConfig::save);
        return builder.build();
    }
}
