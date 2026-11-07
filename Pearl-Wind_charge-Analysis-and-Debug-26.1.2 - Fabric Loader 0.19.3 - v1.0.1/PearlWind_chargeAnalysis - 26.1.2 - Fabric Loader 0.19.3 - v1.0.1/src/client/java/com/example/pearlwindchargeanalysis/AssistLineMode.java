package com.example.pearlwindchargeanalysis;

import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum AssistLineMode {
    INDEPENDENT_AXES("independent_axes"),
    PERSPECTIVE("perspective");

    private final String configValue;

    AssistLineMode(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    public Component displayName() {
        return Component.translatable(
                "option.pearlwind_chargeanalysis.assist_line_mode."
                        + configValue
        );
    }

    public static AssistLineMode fromConfigValue(String value) {
        if (value == null) {
            return INDEPENDENT_AXES;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (AssistLineMode mode : values()) {
            if (mode.configValue.equals(normalized)) {
                return mode;
            }
        }

        return INDEPENDENT_AXES;
    }
}
