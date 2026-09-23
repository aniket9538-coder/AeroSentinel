package com.aerosentinel.util;

public class H3Utils {

    /**
     * Standard H3 edge lengths and resolution approximations.
     */
    public static final int MACRO_RESOLUTION = 7;     // ~1.2 km edge length
    public static final int NEIGHBORHOOD_RESOLUTION = 8; // ~461 m edge length
    public static final int MICRO_RESOLUTION = 9;     // ~174 m edge length

    /**
     * Fallback deterministic H3 index generator for offline simulation/sample data
     * when native C/JNI libraries are not bundled.
     */
    public static String coordinatesToMockH3(double latitude, double longitude, int resolution) {
        long latPart = Math.round((latitude + 90.0) * 1000);
        long lngPart = Math.round((longitude + 180.0) * 1000);
        return String.format("8%x%07x%05x", resolution, latPart, lngPart);
    }

    public static boolean isValidH3Index(String h3Index) {
        return h3Index != null && h3Index.matches("^[0-9a-fA-F]{15}$");
    }
}
