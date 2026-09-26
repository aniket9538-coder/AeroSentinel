package com.aerosentinel.spatial;

import com.aerosentinel.util.H3Utils;
import com.uber.h3core.util.LatLng;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class H3ServiceTest {

    private H3Service h3Service;

    // Real Pune Shivajinagar coordinates
    private static final double PUNE_LAT = 18.5314;
    private static final double PUNE_LNG = 73.8446;

    // Real Mumbai Kurla coordinates
    private static final double MUMBAI_LAT = 19.0863;
    private static final double MUMBAI_LNG = 72.8888;

    // Real Delhi RK Puram coordinates
    private static final double DELHI_LAT = 28.563262;
    private static final double DELHI_LNG = 77.186937;

    @BeforeEach
    void setUp() {
        h3Service = new H3Service(8);
    }

    @Test
    @DisplayName("1. Valid coordinate conversion produces 15-character hex index")
    void testValidCoordinateConversion() {
        String h3 = h3Service.coordinatesToH3(PUNE_LAT, PUNE_LNG);
        assertThat(h3).isNotNull();
        assertThat(h3).matches("^[0-9a-f]{15}$");
        assertThat(h3Service.validateH3Index(h3)).isTrue();
    }

    @Test
    @DisplayName("2. Deterministic conversion: same coordinates and resolution always yield exact same H3 index")
    void testDeterministicConversion() {
        String h3First = h3Service.coordinatesToH3(PUNE_LAT, PUNE_LNG);
        String h3Second = h3Service.coordinatesToH3(PUNE_LAT, PUNE_LNG);
        String h3Third = h3Service.coordinatesToH3(PUNE_LAT, PUNE_LNG);

        assertThat(h3First).isEqualTo(h3Second);
        assertThat(h3Second).isEqualTo(h3Third);

        // Also test H3Utils static utility
        String utilsH3 = H3Utils.coordinatesToH3(PUNE_LAT, PUNE_LNG);
        assertThat(utilsH3).isEqualTo(h3First);
    }

    @Test
    @DisplayName("3. Different coordinates produce distinct spatial cells")
    void testDifferentCoordinatesProduceDistinctCells() {
        String puneH3 = h3Service.coordinatesToH3(PUNE_LAT, PUNE_LNG);
        String mumbaiH3 = h3Service.coordinatesToH3(MUMBAI_LAT, MUMBAI_LNG);
        String delhiH3 = h3Service.coordinatesToH3(DELHI_LAT, DELHI_LNG);

        assertThat(puneH3).isNotEqualTo(mumbaiH3);
        assertThat(mumbaiH3).isNotEqualTo(delhiH3);
        assertThat(puneH3).isNotEqualTo(delhiH3);
    }

    @Test
    @DisplayName("4. Invalid latitude rejected (< -90, > 90, NaN, Infinite)")
    void testInvalidLatitudeRejected() {
        assertThatThrownBy(() -> h3Service.coordinatesToH3(-90.1, 73.85))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid latitude");

        assertThatThrownBy(() -> h3Service.coordinatesToH3(90.1, 73.85))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid latitude");

        assertThatThrownBy(() -> h3Service.coordinatesToH3(Double.NaN, 73.85))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid latitude");

        assertThatThrownBy(() -> h3Service.coordinatesToH3(Double.POSITIVE_INFINITY, 73.85))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid latitude");
    }

    @Test
    @DisplayName("5. Invalid longitude rejected (< -180, > 180, NaN, Infinite)")
    void testInvalidLongitudeRejected() {
        assertThatThrownBy(() -> h3Service.coordinatesToH3(18.53, -180.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid longitude");

        assertThatThrownBy(() -> h3Service.coordinatesToH3(18.53, 180.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid longitude");

        assertThatThrownBy(() -> h3Service.coordinatesToH3(18.53, Double.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid longitude");

        assertThatThrownBy(() -> h3Service.coordinatesToH3(18.53, Double.NEGATIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid longitude");
    }

    @Test
    @DisplayName("6. Configured resolution is used and can be queried")
    void testConfiguredResolution() {
        assertThat(h3Service.getResolution()).isEqualTo(8);

        String h3 = h3Service.coordinatesToH3(PUNE_LAT, PUNE_LNG);
        assertThat(h3Service.getCellResolution(h3)).isEqualTo(8);

        H3Service res7Service = new H3Service(7);
        String h3Res7 = res7Service.coordinatesToH3(PUNE_LAT, PUNE_LNG);
        assertThat(res7Service.getCellResolution(h3Res7)).isEqualTo(7);
        assertThat(h3Res7).isNotEqualTo(h3);
    }

    @Test
    @DisplayName("7. H3 index validation correctly validates valid vs invalid strings")
    void testH3IndexValidation() {
        String validH3 = h3Service.coordinatesToH3(PUNE_LAT, PUNE_LNG);
        assertThat(h3Service.validateH3Index(validH3)).isTrue();
        assertThat(H3Utils.isValidH3Index(validH3)).isTrue();

        assertThat(h3Service.validateH3Index(null)).isFalse();
        assertThat(h3Service.validateH3Index("")).isFalse();
        assertThat(h3Service.validateH3Index("   ")).isFalse();
        assertThat(h3Service.validateH3Index("invalid-h3-index")).isFalse();
        assertThat(h3Service.validateH3Index("8860145a33fffff-fake")).isFalse();
        assertThat(h3Service.validateH3Index("123456789012345")).isFalse();
    }

    @Test
    @DisplayName("8. Center generation produces valid centroid near original coordinates")
    void testCenterGeneration() {
        String h3 = h3Service.coordinatesToH3(PUNE_LAT, PUNE_LNG);
        LatLng center = h3Service.h3ToCenter(h3);

        assertThat(center).isNotNull();
        // At resolution 8 (~461m radius), centroid must be within 0.01 degrees of station
        assertThat(center.lat).isCloseTo(PUNE_LAT, org.assertj.core.data.Offset.offset(0.01));
        assertThat(center.lng).isCloseTo(PUNE_LNG, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    @DisplayName("9. Boundary generation produces 6 vertices for hexagon")
    void testBoundaryGeneration() {
        String h3 = h3Service.coordinatesToH3(PUNE_LAT, PUNE_LNG);
        List<LatLng> boundary = h3Service.h3ToBoundary(h3);

        assertThat(boundary).isNotNull();
        assertThat(boundary).hasSize(6); // Hexagon has 6 vertices
        for (LatLng vertex : boundary) {
            assertThat(vertex.lat).isBetween(-90.0, 90.0);
            assertThat(vertex.lng).isBetween(-180.0, 180.0);
        }
    }

    @Test
    @DisplayName("10. Boundary is non-empty and correctly ordered around perimeter")
    void testBoundaryNonEmpty() {
        String h3 = h3Service.coordinatesToH3(DELHI_LAT, DELHI_LNG);
        List<LatLng> vertices = h3Service.h3ToBoundary(h3);

        assertThat(vertices).isNotEmpty();
        assertThat(vertices.size()).isGreaterThanOrEqualTo(6);

        // Calculate surface area (resolution 8 hexagon should be ~0.737 km²)
        double areaKm2 = h3Service.calculateCellArea(h3);
        assertThat(areaKm2).isBetween(0.5, 1.0);
    }

    @Test
    @DisplayName("11. Malformed H3 rejected with IllegalArgumentException in all accessor methods")
    void testMalformedH3Rejected() {
        assertThatThrownBy(() -> h3Service.h3ToCenter("not-a-cell"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid H3 index");

        assertThatThrownBy(() -> h3Service.h3ToBoundary("not-a-cell"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid H3 index");

        assertThatThrownBy(() -> h3Service.h3ToBoundaryWkt("not-a-cell"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid H3 index");

        assertThatThrownBy(() -> h3Service.getCellResolution("not-a-cell"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid H3 index");

        assertThatThrownBy(() -> h3Service.calculateCellArea("not-a-cell"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid H3 index");
    }

    @Test
    @DisplayName("12. Boundary WKT is closed PostGIS POLYGON format: POLYGON((lng lat, ...))")
    void testBoundaryWktFormat() {
        String h3 = h3Service.coordinatesToH3(PUNE_LAT, PUNE_LNG);
        String wkt = h3Service.h3ToBoundaryWkt(h3);

        assertThat(wkt).startsWith("POLYGON((");
        assertThat(wkt).endsWith("))");

        // First and last coordinates must match to close polygon ring
        String inner = wkt.substring("POLYGON((".length(), wkt.length() - "))".length());
        String[] points = inner.split(", ");
        assertThat(points.length).isEqualTo(7); // 6 vertices + 1 closing vertex
        assertThat(points[0]).isEqualTo(points[points.length - 1]);
    }
}
