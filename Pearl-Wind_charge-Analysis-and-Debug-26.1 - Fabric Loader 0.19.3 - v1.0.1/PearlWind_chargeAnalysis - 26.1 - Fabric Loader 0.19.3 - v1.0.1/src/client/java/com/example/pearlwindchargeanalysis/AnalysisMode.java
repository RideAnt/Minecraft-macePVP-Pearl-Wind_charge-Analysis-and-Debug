package com.example.pearlwindchargeanalysis;

/**
 * Available pearl / wind-charge analysis modes.
 */
public enum AnalysisMode {
    FIXED_POINT("定点解析", "fixed_point"),
    MOVING_POINT("动点解析", "moving_point");

    private final String displayName;
    private final String configValue;

    AnalysisMode(String displayName, String configValue) {
        this.displayName = displayName;
        this.configValue = configValue;
    }

    public String displayName() {
        return displayName;
    }

    public String configValue() {
        return configValue;
    }

    public AnalysisMode next() {
        return this == FIXED_POINT ? MOVING_POINT : FIXED_POINT;
    }

    public static AnalysisMode fromConfigValue(String value) {
        if (MOVING_POINT.configValue.equalsIgnoreCase(value)) {
            return MOVING_POINT;
        }

        return FIXED_POINT;
    }
}
