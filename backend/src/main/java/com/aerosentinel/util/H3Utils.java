package com.aerosentinel.util;

import com.uber.h3core.AreaUnit;
import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Utility helper providing static access to native Uber H3 spatial calculations.
 * All fake/mock H3 generators have been completely removed.
 */
public class H3Utils {

    private static final Logger log = LoggerFactory.getLogger(H3Utils.class);

    public static final int MACRO_RESOLUTION = 7;         // ~1.2 km edge length
    public static final int NEIGHBORHOOD_RESOLUTION = 8;  // ~461 m edge length (AeroSentinel default)
    public static final int MICRO_RESOLUTION = 9;         // ~174 m edge length

    private static final H3Core H3_CORE;

    static {
        try {
            H3_CORE = H3Core.newInstance();
        } catch (IOException e) {
            log.error("Fatal error: Failed to initialize Uber H3Core native library", e);
            throw new ExceptionInInitializerError(e);
        }
    }

    public static H3Core getH3Core() {
        return H3_CORE;
    }

    public static int getResolution() {
        return NEIGHBORHOOD_RESOLUTION;
    }

    /**
     * Validates that latitude is within standard WGS84 range [-90.0, 90.0].
     */
    public static void validateLatitude(double latitude) {
        if (Double.isNaN(latitude) || Double.isInfinite(latitude) || latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Invalid latitude: " + latitude + ". Must be between -90.0 and 90.0.");
        }
    }

    /**
     * Validates that longitude is within standard WGS84 range [-180.0, 180.0].
     */
    public static void validateLongitude(double longitude) {
        if (Double.isNaN(longitude) || Double.isInfinite(longitude) || longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Invalid longitude: " + longitude + ". Must be between -180.0 and 180.0.");
        }
    }

    /**
     * Validates both latitude and longitude.
     */
    public static void validateCoordinates(double latitude, double longitude) {
        validateLatitude(latitude);
        validateLongitude(longitude);
    }

    /**
     * Validates whether string is a syntactically and mathematically valid H3 index.
     */
    public static boolean validateH3Index(String h3Index) {
        return isValidH3Index(h3Index);
    }

    public static boolean isValidH3Index(String h3Index) {
        if (h3Index == null || h3Index.isBlank()) {
            return false;
        }
        try {
            return H3_CORE.isValidCell(h3Index.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Converts coordinates to H3 index using default resolution (8).
     */
    public static String coordinatesToH3(double latitude, double longitude) {
        return coordinatesToH3(latitude, longitude, NEIGHBORHOOD_RESOLUTION);
    }

    /**
     * Converts coordinates to H3 index with an explicit resolution.
     */
    public static String coordinatesToH3(double latitude, double longitude, int resolution) {
        validateCoordinates(latitude, longitude);
        if (resolution < 0 || resolution > 15) {
            throw new IllegalArgumentException("H3 resolution must be between 0 and 15. Received: " + resolution);
        }
        return H3_CORE.latLngToCellAddress(latitude, longitude, resolution);
    }

    /**
     * Computes the centroid coordinates of an H3 cell.
     */
    public static LatLng h3ToCenter(String h3Index) {
        if (!isValidH3Index(h3Index)) {
            throw new IllegalArgumentException("Invalid H3 index: " + h3Index);
        }
        return H3_CORE.cellToLatLng(h3Index.trim());
    }

    /**
     * Computes the polygon boundary vertices of an H3 cell.
     */
    public static List<LatLng> h3ToBoundary(String h3Index) {
        if (!isValidH3Index(h3Index)) {
            throw new IllegalArgumentException("Invalid H3 index: " + h3Index);
        }
        return H3_CORE.cellToBoundary(h3Index.trim());
    }

    /**
     * Generates a standard Well-Known Text (WKT) closed polygon representation for PostGIS.
     */
    public static String h3ToBoundaryWkt(String h3Index) {
        List<LatLng> vertices = h3ToBoundary(h3Index);
        if (vertices.isEmpty()) {
            throw new IllegalArgumentException("H3 index produced no boundary vertices: " + h3Index);
        }
        StringBuilder sb = new StringBuilder("POLYGON((");
        for (LatLng p : vertices) {
            sb.append(p.lng).append(" ").append(p.lat).append(", ");
        }
        LatLng first = vertices.get(0);
        sb.append(first.lng).append(" ").append(first.lat).append("))");
        return sb.toString();
    }

    /**
     * Calculates the approximate surface area of the cell in square kilometers.
     */
    public static double calculateCellArea(String h3Index) {
        if (!isValidH3Index(h3Index)) {
            throw new IllegalArgumentException("Invalid H3 index: " + h3Index);
        }
        return H3_CORE.cellArea(h3Index.trim(), AreaUnit.km2);
    }
}
