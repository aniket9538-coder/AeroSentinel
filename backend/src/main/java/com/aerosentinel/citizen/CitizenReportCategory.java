package com.aerosentinel.citizen;

public enum CitizenReportCategory {
    SMOKE,
    BURNING,
    DUST,
    ODOR,
    OTHER;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        try {
            CitizenReportCategory.valueOf(value.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static CitizenReportCategory fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        return CitizenReportCategory.valueOf(value.trim().toUpperCase());
    }
}