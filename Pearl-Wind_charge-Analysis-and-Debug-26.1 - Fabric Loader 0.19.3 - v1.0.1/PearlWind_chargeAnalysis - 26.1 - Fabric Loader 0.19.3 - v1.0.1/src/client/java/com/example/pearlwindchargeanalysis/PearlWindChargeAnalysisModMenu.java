package com.example.pearlwindchargeanalysis;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Mod Menu integration with a Cloth Config screen.
 */
public final class PearlWindChargeAnalysisModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PearlWindChargeAnalysisModMenu::createConfigScreen;
    }

    private static Screen createConfigScreen(Screen parent) {
        ModConfig.Config config = ModConfig.get();
        config.sanitize();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(
                        Component.translatable(
                                "title.pearlwind_chargeanalysis.config"
                        )
                );

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(
                Component.translatable(
                        "category.pearlwind_chargeanalysis.general"
                )
        );

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Component.translatable(
                                "option.pearlwind_chargeanalysis.enabled"
                        ),
                        config.enabled
                )
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.enabled = value)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Component.translatable(
                                "option.pearlwind_chargeanalysis.show_assist_hud"
                        ),
                        config.showAssistHud
                )
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.showAssistHud = value)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Component.translatable(
                                "option.pearlwind_chargeanalysis.intercept_other_players"
                        ),
                        config.interceptOtherPlayers
                )
                .setDefaultValue(false)
                .setSaveConsumer(
                        value -> config.interceptOtherPlayers = value
                )
                .build());

        general.addEntry(entryBuilder
                .startEnumSelector(
                        Component.translatable(
                                "option.pearlwind_chargeanalysis.assist_line_mode"
                        ),
                        AssistLineMode.class,
                        config.assistLineMode
                )
                .setDefaultValue(AssistLineMode.INDEPENDENT_AXES)
                .setEnumNameProvider(
                        value -> ((AssistLineMode) value).displayName()
                )
                .setSaveConsumer(value -> config.assistLineMode = value)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Component.translatable(
                                "option.pearlwind_chargeanalysis.validate_path_collisions"
                        ),
                        config.validatePathCollisions
                )
                .setDefaultValue(false)
                .setSaveConsumer(
                        value -> config.validatePathCollisions = value
                )
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Component.translatable(
                                "option.pearlwind_chargeanalysis.server_timing_compensation"
                        ),
                        config.serverTimingCompensation
                )
                .setDefaultValue(false)
                .setSaveConsumer(
                        value -> config.serverTimingCompensation = value
                )
                .build());

        general.addEntry(entryBuilder
                .startIntSlider(
                        Component.translatable(
                                "option.pearlwind_chargeanalysis.best_beta_min"
                        ),
                        (int) Math.round(config.bestBetaMin),
                        -90,
                        90
                )
                .setDefaultValue(-90)
                .setSaveConsumer(value -> config.bestBetaMin = value)
                .build());

        general.addEntry(entryBuilder
                .startIntSlider(
                        Component.translatable(
                                "option.pearlwind_chargeanalysis.best_beta_max"
                        ),
                        (int) Math.round(config.bestBetaMax),
                        -90,
                        90
                )
                .setDefaultValue(90)
                .setSaveConsumer(value -> config.bestBetaMax = value)
                .build());

        general.addEntry(entryBuilder
                .startIntSlider(
                        Component.translatable(
                                "option.pearlwind_chargeanalysis.vertical_aim_max"
                        ),
                        (int) Math.round(config.verticalAimMax),
                        0,
                        180
                )
                .setDefaultValue(30)
                .setSaveConsumer(value -> config.verticalAimMax = value)
                .build());

        general.addEntry(entryBuilder
                .startIntSlider(
                        Component.translatable(
                                "option.pearlwind_chargeanalysis.horizontal_aim_max"
                        ),
                        (int) Math.round(config.horizontalAimMax),
                        0,
                        180
                )
                .setDefaultValue(30)
                .setSaveConsumer(value -> config.horizontalAimMax = value)
                .build());

        builder.setSavingRunnable(() -> {
            config.sanitize();
            ModConfig.save();
            PearlWindChargeAnalysisClient.refreshModeFromConfig();
        });

        return builder.build();
    }
}
