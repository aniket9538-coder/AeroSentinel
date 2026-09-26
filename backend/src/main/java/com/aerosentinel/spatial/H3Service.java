package com.aerosentinel.spatial;

import com.uber.h3core.AreaUnit;
import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Authoritative backend spatial service providing deterministic Uber H3 indexing,
 * coordinate validation, center coordinates, and polygon boundary generation.
 */
@Service
public class H3Service {

    private static final Logger log = LoggerFactory.getLogger(H3Service.class);

    private final H3Core h3Core;
    private final int resolution;

    public H3Service(@Value("${app.spatial.h3.resolution:8}") int resolution) {
        if (resolution < 0 || resolution > 15) {
            throw new IllegalArgumentException("H3 resolution must be between 0 and 15. Received: " + resolution);
        }
        this.resolution = resolution;
        try {
            this.h3Core = H3Core.newInstance();
            log.info("Initialized Uber H3Core native spatial engine with configured default resolution={}", resolution);
        } catch (IOException e) {
            log.error("Fatal: failed to initialize native Uber H3Core library", e);
            throw new IllegalStateException("Failed to initialize native Uber H3Core library", e);
        }
    }

    /**
     * Validates that latitude is within standard WGS84 range [-90.0, 90.0].
     */
    public void validateLatitude(double latitude) {
        if (Double.isNaN(latitude) || Double.isInfinite(latitude) || latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Invalid latitude: " + latitude + ". Must be between -90.0 and 90.0.");
        }
    }

    /**
     * Validates that longitude is within standard WGS84 range [-180.0, 180.0].
     */
    public void validateLongitude(double longitude) {
        if (Double.isNaN(longitude) || Double.isInfinite(longitude) || longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Invalid longitude: " + longitude + ". Must be between -180.0 and 180.0.");
        }
    }

    /**
     * Validates both latitude and longitude.
     */
    public void validateCoordinates(double latitude, double longitude) {
        validateLatitude(latitude);
        validateLongitude(longitude);
    }

    /**
     * Validates whether the given string is a syntactically and mathematically valid H3 cell index.
     * Safely catches NumberFormatException / IllegalArgumentException from underlying H3 hex parser.
     */
    public boolean validateH3Index(String h3Index) {
        if (h3Index == null || h3Index.isBlank()) {
            return false;
        }
        try {
            return h3Core.isValidCell(h3Index.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Converts WGS84 coordinates (latitude, longitude) to an H3 index using configured resolution.
     * Deterministic: same coordinates always produce the exact same H3 index.
     */
    public String coordinatesToH3(double latitude, double longitude) {
        return coordinatesToH3(latitude, longitude, this.resolution);
    }

    /**
     * Converts WGS84 coordinates to an H3 index using an explicit resolution.
     */
    public String coordinatesToH3(double latitude, double longitude, int customResolution) {
        validateCoordinates(latitude, longitude);
        if (customResolution < 0 || customResolution > 15) {
            throw new IllegalArgumentException("H3 resolution must be between 0 and 15. Received: " + customResolution);
        }
        return h3Core.latLngToCellAddress(latitude, longitude, customResolution);
    }

    /**
     * Computes the center coordinates (centroid) of the specified H3 cell.
     */
    public LatLng h3ToCenter(String h3Index) {
        if (!validateH3Index(h3Index)) {
            throw new IllegalArgumentException("Invalid H3 index: " + h3Index);
        }
        return h3Core.cellToLatLng(h3Index.trim());
    }

    /**
     * Computes the polygon boundary vertices of the specified H3 cell.
     * Returns vertices ordered around the hexagon perimeter.
     */
    public List<LatLng> h3ToBoundary(String h3Index) {
        if (!validateH3Index(h3Index)) {
            throw new IllegalArgumentException("Invalid H3 index: " + h3Index);
        }
        return h3Core.cellToBoundary(h3Index.trim());
    }

    /**
     * Generates a standard Well-Known Text (WKT) closed polygon representation for PostGIS:
     * POLYGON((lng1 lat1, lng2 lat2, ..., lngN latN, lng1 lat1))
     */
    public String h3ToBoundaryWkt(String h3Index) {
        List<LatLng> vertices = h3ToBoundary(h3Index);
        if (vertices.isEmpty()) {
            throw new IllegalArgumentException("H3 index produced no boundary vertices: " + h3Index);
        }
        StringBuilder sb = new StringBuilder("POLYGON((");
        for (LatLng p : vertices) {
            sb.append(p.lng).append(" ").append(p.lat).append(", ");
        }
        // Close polygon ring with the initial vertex
        LatLng first = vertices.get(0);
        sb.append(first.lng).append(" ").append(first.lat).append("))");
        return sb.toString();
    }

    /**
     * Returns the resolution of a given H3 cell index.
     */
    public int getCellResolution(String h3Index) {
        if (!validateH3Index(h3Index)) {
            throw new IllegalArgumentException("Invalid H3 index: " + h3Index);
        }
        return h3Core.getResolution(h3Index.trim());
    }

    /**
     * Calculates the approximate surface area of the cell in square kilometers.
     */
    public double calculateCellArea(String h3Index) {
        if (!validateH3Index(h3Index)) {
            throw new IllegalArgumentException("Invalid H3 index: " + h3Index);
        }
        return h3Core.cellArea(h3Index.trim(), AreaUnit.km2);
    }

    /**
     * Returns the centrally configured default resolution.
     */
    public int getResolution() {
        return this.resolution;
    }

    /**
     * Exposes the underlying H3Core native instance for advanced operations if required.
     */
    public H3Core getH3Core() {
        return this.h3Core;
    }
}
