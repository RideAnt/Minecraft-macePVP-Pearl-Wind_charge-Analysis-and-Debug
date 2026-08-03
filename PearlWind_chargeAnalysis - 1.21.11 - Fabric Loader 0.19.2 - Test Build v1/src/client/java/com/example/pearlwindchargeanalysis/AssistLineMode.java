package com.example.pearlwindchargeanalysis;

import net.minecraft.text.Text;

import java.util.Locale;

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

    public Text displayName() {
        return Text.translatable(
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
