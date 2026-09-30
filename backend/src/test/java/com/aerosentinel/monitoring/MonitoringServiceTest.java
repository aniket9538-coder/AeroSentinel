package com.aerosentinel.monitoring;

import com.aerosentinel.feature.FeatureEngineeringService;
import com.aerosentinel.monitoring.dto.MonitoringCoverageResponse;
import com.aerosentinel.sensor.MonitoringStation;
import com.aerosentinel.sensor.SensorRepository;
import com.aerosentinel.spatial.H3Service;
import com.uber.h3core.util.LatLng;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {

    @Mock
    private SensorRepository sensorRepository;

    private H3Service h3Service;
    private MonitoringService monitoringService;

    private static final UUID PUNE_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    // Pune Shivajinagar H3 (res 8) - center approx (18.5315, 73.8471)
    private static final String SHIVAJINAGAR_H3 = "88608850e5fffff";

    @BeforeEach
    void setUp() {
        h3Service = new H3Service(8);
        monitoringService = new MonitoringService(sensorRepository, h3Service);
    }

    private MonitoringStation createStation(UUID id, String code, String name, double lat, double lon, String status) {
        MonitoringStation station = new MonitoringStation();
        station.setId(id);
        station.setCityId(PUNE_CITY_ID);
        station.setStationCode(code);
        station.setName(name);
        station.setLatitude(lat);
        station.setLongitude(lon);
        station.setStatus(status);
        return station;
    }

    @Test
    @DisplayName("Case 1: Valid H3 with nearby active station identifies station, distance > 0, distance in KM")
    void testValidH3WithNearbyActiveStation() {
        UUID stationId = UUID.randomUUID();
        // Station ~0.27 km from Shivajinagar H3 centroid
        MonitoringStation station = createStation(stationId, "PUN-001", "Shivajinagar CAAQMS", 18.5314, 73.8446, "ACTIVE");
        when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE")).thenReturn(List.of(station));

        MonitoringCoverageResponse response = monitoringService.getMonitoringCoverage(SHIVAJINAGAR_H3, PUNE_CITY_ID);

        assertThat(response).isNotNull();
        assertThat(response.h3Index()).isEqualTo(SHIVAJINAGAR_H3);
        assertThat(response.latitude()).isNotNull();
        assertThat(response.longitude()).isNotNull();
        assertThat(response.nearestStationId()).isEqualTo(stationId);
        assertThat(response.nearestStationCode()).isEqualTo("PUN-001");
        assertThat(response.nearestStationName()).isEqualTo("Shivajinagar CAAQMS");
        assertThat(response.nearestStationLatitude()).isEqualTo(18.5314);
        assertThat(response.nearestStationLongitude()).isEqualTo(73.8446);
        assertThat(response.nearestStationDistanceKm()).isGreaterThan(0.0);
        assertThat(response.nearestStationDistanceKm()).isCloseTo(0.27, offset(0.05));
    }

    @Test
    @DisplayName("Case 2: Valid H3 with multiple stations selects the closest station")
    void testValidH3WithMultipleStationsSelectsClosest() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        // 3 Pune stations at varying distances from Shivajinagar H3:
        // PUN-001: ~0.27 km away
        // PUN-002: ~8.51 km away
        // PUN-003: ~8.69 km away
        MonitoringStation station1 = createStation(id1, "PUN-001", "Shivajinagar CAAQMS", 18.5314, 73.8446, "ACTIVE");
        MonitoringStation station2 = createStation(id2, "PUN-002", "Katraj Air Station", 18.4575, 73.8677, "ACTIVE");
        MonitoringStation station3 = createStation(id3, "PUN-003", "Hadapsar Industrial Zone", 18.5089, 73.9260, "ACTIVE");

        when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE")).thenReturn(List.of(station2, station1, station3));

        MonitoringCoverageResponse response = monitoringService.getMonitoringCoverage(SHIVAJINAGAR_H3, PUNE_CITY_ID);

        assertThat(response.nearestStationId()).isEqualTo(id1);
        assertThat(response.nearestStationCode()).isEqualTo("PUN-001");
        assertThat(response.nearestStationDistanceKm()).isLessThan(1.0);
    }

    @Test
    @DisplayName("Case 3: Station within 5 km gives stationsWithin5kmCount >= 1 and coverageGapFlag = 0")
    void testStationWithin5kmGivesCountAndNoGap() {
        MonitoringStation closeStation = createStation(UUID.randomUUID(), "PUN-001", "Shivajinagar CAAQMS", 18.5314, 73.8446, "ACTIVE");
        MonitoringStation farStation = createStation(UUID.randomUUID(), "PUN-002", "Katraj Air Station", 18.4575, 73.8677, "ACTIVE");

        when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE")).thenReturn(List.of(closeStation, farStation));

        MonitoringCoverageResponse response = monitoringService.getMonitoringCoverage(SHIVAJINAGAR_H3, PUNE_CITY_ID);

        assertThat(response.stationsWithin5kmCount()).isEqualTo(1);
        assertThat(response.monitoringCoverageGapFlag()).isEqualTo(0);
        assertThat(response.nearestStationDistanceKm()).isLessThanOrEqualTo(7.0);
    }

    @Test
    @DisplayName("Case 4: Nearest station > 7 km produces coverageGapFlag = 1")
    void testNearestStationOver7kmProducesCoverageGap() {
        // Nigdi / PCMC area cell (18.65028, 73.77805) -> distant from central Pune stations (~14.96 km)
        String distantH3 = "8860884119fffff";

        MonitoringStation station = createStation(UUID.randomUUID(), "PUN-001", "Shivajinagar CAAQMS", 18.5314, 73.8446, "ACTIVE");
        when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE")).thenReturn(List.of(station));

        MonitoringCoverageResponse response = monitoringService.getMonitoringCoverage(distantH3, PUNE_CITY_ID);

        assertThat(response.nearestStationDistanceKm()).isGreaterThan(7.0);
        assertThat(response.monitoringCoverageGapFlag()).isEqualTo(1);
        assertThat(response.stationsWithin5kmCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Case 5: Multiple stations where closest is inactive ignores inactive station")
    void testMultipleStationsClosestInactiveIsIgnored() {
        UUID inactiveId = UUID.randomUUID();
        UUID activeId = UUID.randomUUID();

        // Inactive station is very close (0.1 km away)
        MonitoringStation inactiveStation = createStation(inactiveId, "INACTIVE-01", "Decommissioned Station", 18.5315, 73.8470, "INACTIVE");
        // Active station is farther (~8.51 km away)
        MonitoringStation activeStation = createStation(activeId, "PUN-002", "Katraj Air Station", 18.4575, 73.8677, "ACTIVE");

        // SensorRepository.findByCityIdAndStatus(cityId, "ACTIVE") should only return active stations,
        // but even if an inactive station was present in the list, MonitoringService filters it out
        when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE")).thenReturn(List.of(inactiveStation, activeStation));

        MonitoringCoverageResponse response = monitoringService.getMonitoringCoverage(SHIVAJINAGAR_H3, PUNE_CITY_ID);

        assertThat(response.nearestStationId()).isEqualTo(activeId);
        assertThat(response.nearestStationCode()).isEqualTo("PUN-002");
        assertThat(response.nearestStationDistanceKm()).isGreaterThan(8.0);
    }

    @Test
    @DisplayName("Case 6: City with no active stations returns safe no-coverage response without fake station or zero distance")
    void testCityWithNoActiveStationsReturnsSafeNoCoverage() {
        UUID emptyCityId = UUID.fromString("550e8400-e29b-41d4-a716-446655440099");
        when(sensorRepository.findByCityIdAndStatus(emptyCityId, "ACTIVE")).thenReturn(Collections.emptyList());

        MonitoringCoverageResponse response = monitoringService.getMonitoringCoverage(SHIVAJINAGAR_H3, emptyCityId);

        assertThat(response).isNotNull();
        assertThat(response.h3Index()).isEqualTo(SHIVAJINAGAR_H3);
        assertThat(response.latitude()).isNotNull();
        assertThat(response.longitude()).isNotNull();
        assertThat(response.nearestStationId()).isNull();
        assertThat(response.nearestStationCode()).isNull();
        assertThat(response.nearestStationName()).isNull();
        assertThat(response.nearestStationLatitude()).isNull();
        assertThat(response.nearestStationLongitude()).isNull();
        assertThat(response.nearestStationDistanceKm()).isNull(); // strictly NOT 0.0
        assertThat(response.stationsWithin5kmCount()).isEqualTo(0);
        assertThat(response.monitoringCoverageGapFlag()).isEqualTo(1);
    }

    @Test
    @DisplayName("Case 7: Invalid H3 throws IllegalArgumentException")
    void testInvalidH3ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> monitoringService.getMonitoringCoverage("not-a-valid-h3", PUNE_CITY_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid H3 index");

        assertThatThrownBy(() -> monitoringService.getMonitoringCoverage("", PUNE_CITY_ID))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> monitoringService.getMonitoringCoverage(null, PUNE_CITY_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Case 8: Distance calculation consistency matches FeatureEngineeringService.calculateHaversineDistanceKm()")
    void testDistanceCalculationMatchesFeatureEngineeringService() {
        MonitoringStation station = createStation(UUID.randomUUID(), "PUN-001", "Shivajinagar CAAQMS", 18.5314, 73.8446, "ACTIVE");
        when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE")).thenReturn(List.of(station));

        MonitoringCoverageResponse response = monitoringService.getMonitoringCoverage(SHIVAJINAGAR_H3, PUNE_CITY_ID);

        LatLng center = h3Service.h3ToCenter(SHIVAJINAGAR_H3);
        double expectedRawDistance = FeatureEngineeringService.calculateHaversineDistanceKm(
                center.lat, center.lng, station.getLatitude(), station.getLongitude()
        );
        double expectedRoundedDistance = Math.round(expectedRawDistance * 100.0) / 100.0;

        assertThat(response.nearestStationDistanceKm()).isEqualTo(expectedRoundedDistance);
        assertThat(response.nearestStationDistanceKm()).isCloseTo(expectedRawDistance, offset(0.01));

        // Direct method test
        double directDistance = monitoringService.calculateHaversineDistanceKm(
                center.lat, center.lng, station.getLatitude(), station.getLongitude()
        );
        assertThat(directDistance).isEqualTo(expectedRawDistance);
    }

    @Test
    @DisplayName("Null cityId throws IllegalArgumentException")
    void testNullCityIdThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> monitoringService.getMonitoringCoverage(SHIVAJINAGAR_H3, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("City ID must not be null");
    }

    @Test
    @DisplayName("Stations with invalid coordinates are safely filtered out")
    void testStationsWithInvalidCoordinatesAreSkipped() {
        MonitoringStation invalidLat = createStation(UUID.randomUUID(), "INV-1", "Invalid Lat", 95.0, 73.8446, "ACTIVE");
        MonitoringStation nullLon = createStation(UUID.randomUUID(), "INV-2", "Null Lon", 18.5314, 0.0, "ACTIVE");
        nullLon.setLongitude(null);
        MonitoringStation validStation = createStation(UUID.randomUUID(), "VAL-1", "Valid Station", 18.5314, 73.8446, "ACTIVE");

        when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE")).thenReturn(List.of(invalidLat, nullLon, validStation));

        MonitoringCoverageResponse response = monitoringService.getMonitoringCoverage(SHIVAJINAGAR_H3, PUNE_CITY_ID);

        assertThat(response.nearestStationCode()).isEqualTo("VAL-1");
    }
}
