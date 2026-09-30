package com.aerosentinel.monitoring;

import com.aerosentinel.exception.ResourceNotFoundException;
import com.aerosentinel.forecast.Forecast;
import com.aerosentinel.forecast.ForecastRepository;
import com.aerosentinel.hotspot.HotspotPrediction;
import com.aerosentinel.hotspot.HotspotRepository;
import com.aerosentinel.monitoring.dto.MonitoringPriorityResponse;
import com.aerosentinel.sensor.MonitoringStation;
import com.aerosentinel.sensor.SensorRepository;
import com.aerosentinel.spatial.H3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitoringPriorityTest {

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private HotspotRepository hotspotRepository;

    @Mock
    private ForecastRepository forecastRepository;

    private H3Service h3Service;
    private MonitoringPriorityConfig priorityConfig;
    private MonitoringService monitoringService;

    private static final UUID PUNE_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    // Pune Shivajinagar H3 (res 8) - center (18.5315, 73.8471)
    private static final String SHIVAJINAGAR_H3 = "88608850e5fffff";

    @BeforeEach
    void setUp() {
        h3Service = new H3Service(8);
        priorityConfig = new MonitoringPriorityConfig();
        // default config: riskWeight=0.45, uncertaintyWeight=0.30, distanceWeight=0.25,
        // uncertaintyMaxIntervalWidth=30.0, distanceMaxKm=15.0, mediumThreshold=40, highThreshold=70
        priorityConfig.setRiskWeight(0.45);
        priorityConfig.setUncertaintyWeight(0.30);
        priorityConfig.setDistanceWeight(0.25);
        priorityConfig.setUncertaintyMaxIntervalWidth(30.0);
        priorityConfig.setDistanceMaxKm(15.0);
        priorityConfig.setMediumThreshold(40);
        priorityConfig.setHighThreshold(70);
        priorityConfig.validate();

        monitoringService = new MonitoringService(
                sensorRepository,
                h3Service,
                hotspotRepository,
                forecastRepository,
                priorityConfig
        );
    }

    private HotspotPrediction createHotspotPrediction(double riskScore, String riskLevel, double confidence) {
        HotspotPrediction pred = new HotspotPrediction();
        pred.setId(UUID.randomUUID());
        pred.setCityId(PUNE_CITY_ID);
        pred.setH3Index(SHIVAJINAGAR_H3);
        pred.setRiskScore(riskScore);
        pred.setRiskLevel(riskLevel);
        pred.setConfidence(confidence);
        pred.setPredictedAt(Instant.now());
        pred.setModelVersion("test_v1");
        return pred;
    }

    private Forecast createForecast(int horizonHours, double predictedPm25, double lowerBound, double upperBound, Double confidence) {
        Forecast fc = new Forecast();
        fc.setId(UUID.randomUUID());
        fc.setCityId(PUNE_CITY_ID);
        fc.setH3Index(SHIVAJINAGAR_H3);
        fc.setHorizonHours(horizonHours);
        fc.setPredictedPm25(predictedPm25);
        fc.setLowerBound(lowerBound);
        fc.setUpperBound(upperBound);
        fc.setForecastConfidence(confidence);
        fc.setGeneratedAt(Instant.now());
        fc.setTargetTime(Instant.now().plusSeconds(horizonHours * 3600L));
        return fc;
    }

    private MonitoringStation createStation(UUID id, String code, String name, double lat, double lon) {
        MonitoringStation station = new MonitoringStation();
        station.setId(id);
        station.setCityId(PUNE_CITY_ID);
        station.setStationCode(code);
        station.setName(name);
        station.setLatitude(lat);
        station.setLongitude(lon);
        station.setStatus("ACTIVE");
        return station;
    }

    // =========================================================================
    // 14 MANDATORY F8-P3 TEST CASES
    // =========================================================================

    @Test
    @DisplayName("Test 1: LOW priority - Low risk + low uncertainty + short station distance (< 40)")
    void testLowPriority() {
        // Given
        HotspotPrediction prediction = createHotspotPrediction(0.10, "LOW", 0.95);
        // intervalWidth = 23.0 - 20.0 = 3.0 -> normUncertainty = 3.0 / 30.0 = 0.10
        Forecast forecast = createForecast(1, 21.5, 20.0, 23.0, null);
        // Station ~0.27 km from Shivajinagar H3 centroid -> normDist = 0.27 / 15.0 = 0.018
        MonitoringStation station = createStation(UUID.randomUUID(), "PUN-001", "Shivajinagar", 18.5314, 73.8446);

        when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3))
                .thenReturn(Optional.of(prediction));
        when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3))
                .thenReturn(List.of(forecast));
        when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE"))
                .thenReturn(List.of(station));

        // When
        MonitoringPriorityResponse response = monitoringService.getMonitoringPriority(SHIVAJINAGAR_H3, PUNE_CITY_ID);

        // Then
        // Expected: 0.45 * 0.10 + 0.30 * 0.10 + 0.25 * (0.27/15) = 0.045 + 0.030 + 0.0045 = 0.0795 -> 8%
        assertThat(response.priorityLevel()).isEqualTo(MonitoringPriority.LOW);
        assertThat(response.priorityScorePercent()).isLessThan(40);
        assertThat(response.normalizedRisk()).isCloseTo(0.10, offset(0.01));
        assertThat(response.normalizedUncertainty()).isCloseTo(0.10, offset(0.01));
        assertThat(response.monitoringCoverageGapFlag()).isEqualTo(0);
    }

    @Test
    @DisplayName("Test 2: MEDIUM priority - Combined inputs produce 40-69")
    void testMediumPriority() {
        // Given
        // risk = 0.50 -> 0.45 * 0.50 = 0.225
        HotspotPrediction prediction = createHotspotPrediction(0.50, "MEDIUM", 0.80);
        // intervalWidth = 15.0 -> normUncertainty = 15.0 / 30.0 = 0.50 -> 0.30 * 0.50 = 0.150
        Forecast forecast = createForecast(1, 55.0, 47.5, 62.5, null);
        // Station at ~7.5 km distance -> normDist = 7.5 / 15.0 = 0.50 -> 0.25 * 0.50 = 0.125
        // Total expected = 0.225 + 0.150 + 0.125 = 0.500 -> 50% -> MEDIUM
        // ~7.5 km north: 18.5315 + (7.5 / 111.0) = ~18.599
        MonitoringStation station = createStation(UUID.randomUUID(), "PUN-MED", "Medium Station", 18.599, 73.8471);

        when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3))
                .thenReturn(Optional.of(prediction));
        when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3))
                .thenReturn(List.of(forecast));
        when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE"))
                .thenReturn(List.of(station));

        // When
        MonitoringPriorityResponse response = monitoringService.getMonitoringPriority(SHIVAJINAGAR_H3, PUNE_CITY_ID);

        // Then
        assertThat(response.priorityLevel()).isEqualTo(MonitoringPriority.MEDIUM);
        assertThat(response.priorityScorePercent()).isBetween(40, 69);
    }

    @Test
    @DisplayName("Test 3: HIGH priority - High combined risk + uncertainty + distance produces >= 70")
    void testHighPriority() {
        // Given
        // risk = 0.85 -> 0.45 * 0.85 = 0.3825
        HotspotPrediction prediction = createHotspotPrediction(0.85, "HIGH", 0.90);
        // intervalWidth = 27.0 -> normUncertainty = 27.0 / 30.0 = 0.90 -> 0.30 * 0.90 = 0.270
        Forecast forecast = createForecast(1, 140.0, 126.5, 153.5, null);
        // Station at ~12.0 km -> normDist = 12.0 / 15.0 = 0.80 -> 0.25 * 0.80 = 0.200
        // Total expected = 0.3825 + 0.270 + 0.200 = 0.8525 -> 85% -> HIGH
        // ~12 km north: 18.5315 + (12.0 / 111.0) = ~18.639
        MonitoringStation station = createStation(UUID.randomUUID(), "PUN-FAR", "Far Station", 18.639, 73.8471);

        when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3))
                .thenReturn(Optional.of(prediction));
        when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3))
                .thenReturn(List.of(forecast));
        when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE"))
                .thenReturn(List.of(station));

        // When
        MonitoringPriorityResponse response = monitoringService.getMonitoringPriority(SHIVAJINAGAR_H3, PUNE_CITY_ID);

        // Then
        assertThat(response.priorityLevel()).isEqualTo(MonitoringPriority.HIGH);
        assertThat(response.priorityScorePercent()).isGreaterThanOrEqualTo(70);
    }

    @Test
    @DisplayName("Test 4: Risk influence - Increasing risk while holding other values constant increases priority score")
    void testRiskInfluence() {
        // Constant uncertainty: width = 12.0
        Forecast forecast = createForecast(1, 50.0, 44.0, 56.0, null);
        // Constant station: ~2 km
        MonitoringStation station = createStation(UUID.randomUUID(), "PUN-STN", "Near Station", 18.545, 73.8471);

        when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3))
                .thenReturn(List.of(forecast));
        when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE"))
                .thenReturn(List.of(station));

        // Low risk (0.20)
        when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3))
                .thenReturn(Optional.of(createHotspotPrediction(0.20, "LOW", 0.90)));
        MonitoringPriorityResponse responseLowRisk = monitoringService.getMonitoringPriority(SHIVAJINAGAR_H3, PUNE_CITY_ID);

        // High risk (0.80)
        when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3))
                .thenReturn(Optional.of(createHotspotPrediction(0.80, "HIGH", 0.90)));
        MonitoringPriorityResponse responseHighRisk = monitoringService.getMonitoringPriority(SHIVAJINAGAR_H3, PUNE_CITY_ID);

        // Then
        assertThat(responseHighRisk.priorityScore()).isGreaterThan(responseLowRisk.priorityScore());
        assertThat(responseHighRisk.priorityScorePercent()).isGreaterThan(responseLowRisk.priorityScorePercent());
        // Difference should be exactly 0.45 * (0.80 - 0.20) = 0.27
        assertThat(responseHighRisk.priorityScore() - responseLowRisk.priorityScore()).isCloseTo(0.27, offset(0.001));
    }

    @Test
    @DisplayName("Test 5: Uncertainty influence - Increasing interval width while holding other values constant increases score")
    void testUncertaintyInfluence() {
        // Constant risk: 0.50
        HotspotPrediction prediction = createHotspotPrediction(0.50, "MEDIUM", 0.90);
        // Constant station
        MonitoringStation station = createStation(UUID.randomUUID(), "PUN-STN", "Station", 18.545, 73.8471);

        when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3))
                .thenReturn(Optional.of(prediction));
        when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE"))
                .thenReturn(List.of(station));

        // Low uncertainty: width = 6.0 -> norm = 6/30 = 0.20
        Forecast fcLowUncertainty = createForecast(1, 50.0, 47.0, 53.0, null);
        when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3))
                .thenReturn(List.of(fcLowUncertainty));
        MonitoringPriorityResponse resLow = monitoringService.getMonitoringPriority(SHIVAJINAGAR_H3, PUNE_CITY_ID);

        // High uncertainty: width = 24.0 -> norm = 24/30 = 0.80
        Forecast fcHighUncertainty = createForecast(1, 50.0, 38.0, 62.0, null);
        when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3))
                .thenReturn(List.of(fcHighUncertainty));
        MonitoringPriorityResponse resHigh = monitoringService.getMonitoringPriority(SHIVAJINAGAR_H3, PUNE_CITY_ID);

        // Then
        assertThat(resHigh.priorityScore()).isGreaterThan(resLow.priorityScore());
        assertThat(resHigh.priorityScorePercent()).isGreaterThan(resLow.priorityScorePercent());
        // Difference: 0.30 * (0.80 - 0.20) = 0.18
        assertThat(resHigh.priorityScore() - resLow.priorityScore()).isCloseTo(0.18, offset(0.001));
    }

    @Test
    @DisplayName("Test 6: Distance influence - Increasing station distance while holding other values constant increases score")
    void testDistanceInfluence() {
        // Constant risk and forecast
        HotspotPrediction prediction = createHotspotPrediction(0.50, "MEDIUM", 0.90);
        Forecast forecast = createForecast(1, 50.0, 45.0, 55.0, null);

        when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3))
                .thenReturn(Optional.of(prediction));
        when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3))
                .thenReturn(List.of(forecast));

        // Close station (~1.5 km)
        MonitoringStation nearStation = createStation(UUID.randomUUID(), "PUN-NEAR", "Near", 18.545, 73.8471);
        when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE"))
                .thenReturn(List.of(nearStation));
        MonitoringPriorityResponse resNear = monitoringService.getMonitoringPriority(SHIVAJINAGAR_H3, PUNE_CITY_ID);

        // Far station (~10.5 km)
        MonitoringStation farStation = createStation(UUID.randomUUID(), "PUN-FAR", "Far", 18.625, 73.8471);
        when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE"))
                .thenReturn(List.of(farStation));
        MonitoringPriorityResponse resFar = monitoringService.getMonitoringPriority(SHIVAJINAGAR_H3, PUNE_CITY_ID);

        // Then
        assertThat(resFar.priorityScore()).isGreaterThan(resNear.priorityScore());
        assertThat(resFar.priorityScorePercent()).isGreaterThan(resNear.priorityScorePercent());
        assertThat(resFar.normalizedDistance()).isGreaterThan(resNear.normalizedDistance());
    }

    @Test
    @DisplayName("Test 7: Clamp behavior - Very large forecast interval width must clamp to 1.0")
    void testClampIntervalWidth() {
        HotspotPrediction prediction = createHotspotPrediction(0.50, "MEDIUM", 0.90);
        // Interval width = 120.0 (far exceeds max configured 30.0)
        Forecast forecast = createForecast(1, 100.0, 40.0, 160.0, null);
        MonitoringStation station = createStation(UUID.randomUUID(), "PUN-001", "Station", 18.5314, 73.8446);

        when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3))
                .thenReturn(Optional.of(prediction));
        when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3))
                .thenReturn(List.of(forecast));
        when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE"))
                .thenReturn(List.of(station));

        MonitoringPriorityResponse response = monitoringService.getMonitoringPriority(SHIVAJINAGAR_H3, PUNE_CITY_ID);

        // Then
        assertThat(response.uncertaintyIntervalWidth()).isEqualTo(120.0);
        assertThat(response.normalizedUncertainty()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Test 8: Clamp behavior - Very large station distance must clamp to 1.0")
    void testClampDistance() {
        HotspotPrediction prediction = createHotspotPrediction(0.50, "MEDIUM", 0.90);
        Forecast forecast = createForecast(1, 50.0, 45.0, 55.0, null);
        // Station 50 km away (far exceeds distanceMaxKm = 15.0)
        MonitoringStation station = createStation(UUID.randomUUID(), "PUN-VERY-FAR", "Distant", 18.98, 73.8471);

        when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3))
                .thenReturn(Optional.of(prediction));
        when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3))
                .thenReturn(List.of(forecast));
        when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE"))
                .thenReturn(List.of(station));

        MonitoringPriorityResponse response = monitoringService.getMonitoringPriority(SHIVAJINAGAR_H3, PUNE_CITY_ID);

        // Then
        assertThat(response.nearestStationDistanceKm()).isGreaterThan(40.0);
        assertThat(response.normalizedDistance()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Test 9: Null forecastConfidence - F4 contract compliance without errors")
    void testNullForecastConfidenceTolerated() {
        HotspotPrediction prediction = createHotspotPrediction(0.60, "MEDIUM", 0.85);
        // forecastConfidence is explicitly null per F4 locked contract
        Forecast forecast = createForecast(1, 65.0, 58.0, 72.0, null);
        MonitoringStation station = createStation(UUID.randomUUID(), "PUN-001", "Station", 18.5314, 73.8446);

        when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3))
                .thenReturn(Optional.of(prediction));
        when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3))
                .thenReturn(List.of(forecast));
        when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE"))
                .thenReturn(List.of(station));

        // When
        MonitoringPriorityResponse response = monitoringService.getMonitoringPriority(SHIVAJINAGAR_H3, PUNE_CITY_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.f3Confidence()).isEqualTo(0.85);
        assertThat(response.uncertaintyIntervalWidth()).isEqualTo(14.0);
        assertThat(response.priorityScore()).isBetween(0.0, 1.0);
    }

    @Test
    @DisplayName("Test 10: No active monitoring stations - Handles safely with normalizedDistance=1.0")
    void testNoActiveMonitoringStations() {
        HotspotPrediction prediction = createHotspotPrediction(0.40, "MEDIUM", 0.85);
        Forecast forecast = createForecast(1, 40.0, 35.0, 45.0, null);

        when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3))
                .thenReturn(Optional.of(prediction));
        when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3))
                .thenReturn(List.of(forecast));
        when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE"))
                .thenReturn(Collections.emptyList());

        // When
        MonitoringPriorityResponse response = monitoringService.getMonitoringPriority(SHIVAJINAGAR_H3, PUNE_CITY_ID);

        // Then
        assertThat(response.nearestStationId()).isNull();
        assertThat(response.nearestStationCode()).isNull();
        assertThat(response.nearestStationName()).isNull();
        assertThat(response.nearestStationDistanceKm()).isNull();
        assertThat(response.stationsWithin5kmCount()).isEqualTo(0);
        assertThat(response.monitoringCoverageGapFlag()).isEqualTo(1);
        // Since coverage is completely absent, normalized distance applies max penalty = 1.0
        assertThat(response.normalizedDistance()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Test 11: Invalid interval - lowerBound > upperBound must be rejected with IllegalArgumentException")
    void testInvalidForecastIntervalRejected() {
        HotspotPrediction prediction = createHotspotPrediction(0.50, "MEDIUM", 0.85);
        // Invalid: lowerBound (60.0) > upperBound (40.0)
        Forecast invalidForecast = createForecast(1, 50.0, 60.0, 40.0, null);

        when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3))
                .thenReturn(Optional.of(prediction));
        when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3))
                .thenReturn(List.of(invalidForecast));

        // When / Then
        assertThatThrownBy(() -> monitoringService.getMonitoringPriority(SHIVAJINAGAR_H3, PUNE_CITY_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lowerBound")
                .hasMessageContaining("upperBound");
    }

    @Test
    @DisplayName("Test 12: Boundary thresholds - Verify exact behavior at 39, 40, 69, 70")
    void testBoundaryThresholds() {
        // Case 12a: Score = 39 -> LOW
        assertThat(monitoringService.classifyPriority(39)).isEqualTo(MonitoringPriority.LOW);

        // Case 12b: Score = 40 -> MEDIUM (mediumThreshold = 40)
        assertThat(monitoringService.classifyPriority(40)).isEqualTo(MonitoringPriority.MEDIUM);

        // Case 12c: Score = 69 -> MEDIUM
        assertThat(monitoringService.classifyPriority(69)).isEqualTo(MonitoringPriority.MEDIUM);

        // Case 12d: Score = 70 -> HIGH (highThreshold = 70)
        assertThat(monitoringService.classifyPriority(70)).isEqualTo(MonitoringPriority.HIGH);
    }

    @Test
    @DisplayName("Test 13: Weight integrity - Configuration validation fails fast when weights do not sum to 1.0")
    void testWeightIntegrityValidation() {
        MonitoringPriorityConfig badConfig = new MonitoringPriorityConfig();
        // Sum = 0.50 + 0.30 + 0.30 = 1.10 != 1.0
        badConfig.setRiskWeight(0.50);
        badConfig.setUncertaintyWeight(0.30);
        badConfig.setDistanceWeight(0.30);

        assertThatThrownBy(badConfig::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must sum to 1.0");

        // Negative weight
        badConfig.setRiskWeight(-0.10);
        badConfig.setUncertaintyWeight(0.60);
        badConfig.setDistanceWeight(0.50);
        assertThatThrownBy(badConfig::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be non-negative");

        // Invalid thresholds (medium >= high)
        badConfig.setRiskWeight(0.45);
        badConfig.setUncertaintyWeight(0.30);
        badConfig.setDistanceWeight(0.25);
        badConfig.setMediumThreshold(75);
        badConfig.setHighThreshold(70);
        assertThatThrownBy(badConfig::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mediumThreshold");
    }

    @Test
    @DisplayName("Test 14: Determinism - Same inputs produce exactly the same score and priority")
    void testDeterminism() {
        HotspotPrediction prediction = createHotspotPrediction(0.62, "HIGH", 0.88);
        Forecast forecast = createForecast(1, 75.0, 63.0, 87.0, null);
        MonitoringStation station = createStation(UUID.randomUUID(), "PUN-DET", "Station", 18.560, 73.8471);

        when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3))
                .thenReturn(Optional.of(prediction));
        when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3))
                .thenReturn(List.of(forecast));
        when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE"))
                .thenReturn(List.of(station));

        MonitoringPriorityResponse run1 = monitoringService.getMonitoringPriority(SHIVAJINAGAR_H3, PUNE_CITY_ID);
        MonitoringPriorityResponse run2 = monitoringService.getMonitoringPriority(SHIVAJINAGAR_H3, PUNE_CITY_ID);

        assertThat(run1.priorityScore()).isEqualTo(run2.priorityScore());
        assertThat(run1.priorityScorePercent()).isEqualTo(run2.priorityScorePercent());
        assertThat(run1.priorityLevel()).isEqualTo(run2.priorityLevel());
        assertThat(run1.normalizedRisk()).isEqualTo(run2.normalizedRisk());
        assertThat(run1.normalizedUncertainty()).isEqualTo(run2.normalizedUncertainty());
        assertThat(run1.normalizedDistance()).isEqualTo(run2.normalizedDistance());
    }

    // =========================================================================
    // EDGE CASES & HORIZON SELECTION TESTS
    // =========================================================================

    @Nested
    @DisplayName("Edge Cases & Operational Horizon Selection")
    class EdgeCasesAndHorizonTests {

        @Test
        @DisplayName("Missing F3 prediction throws ResourceNotFoundException")
        void testMissingF3PredictionThrowsResourceNotFound() {
            when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> monitoringService.getMonitoringPriority(SHIVAJINAGAR_H3, PUNE_CITY_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No hotspot prediction found");
        }

        @Test
        @DisplayName("Missing F4 forecast throws ResourceNotFoundException")
        void testMissingF4ForecastThrowsResourceNotFound() {
            HotspotPrediction prediction = createHotspotPrediction(0.50, "MEDIUM", 0.90);
            when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3))
                    .thenReturn(Optional.of(prediction));
            when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3))
                    .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> monitoringService.getMonitoringPriority(SHIVAJINAGAR_H3, PUNE_CITY_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No forecast found");
        }

        @Test
        @DisplayName("Operational horizon preference: selects 1h when multiple horizons are present")
        void testSelects1hHorizonWhenMultiplePresent() {
            HotspotPrediction prediction = createHotspotPrediction(0.50, "MEDIUM", 0.90);
            Forecast fc1h = createForecast(1, 40.0, 36.0, 44.0, null);
            Forecast fc3h = createForecast(3, 45.0, 38.0, 52.0, null);
            Forecast fc6h = createForecast(6, 50.0, 35.0, 65.0, null);

            when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3))
                    .thenReturn(Optional.of(prediction));
            when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3))
                    .thenReturn(List.of(fc6h, fc3h, fc1h));
            when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE"))
                    .thenReturn(Collections.emptyList());

            MonitoringPriorityResponse response = monitoringService.getMonitoringPriority(SHIVAJINAGAR_H3, PUNE_CITY_ID);

            assertThat(response.forecastHorizonHours()).isEqualTo(1);
            assertThat(response.predictedPm25()).isEqualTo(40.0);
            assertThat(response.uncertaintyIntervalWidth()).isEqualTo(8.0);
        }

        @Test
        @DisplayName("Operational horizon fallback: selects shortest available horizon when 1h is unavailable")
        void testSelectsShortestHorizonWhen1hUnavailable() {
            HotspotPrediction prediction = createHotspotPrediction(0.50, "MEDIUM", 0.90);
            Forecast fc3h = createForecast(3, 45.0, 38.0, 52.0, null);
            Forecast fc6h = createForecast(6, 50.0, 35.0, 65.0, null);

            when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3))
                    .thenReturn(Optional.of(prediction));
            when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3))
                    .thenReturn(List.of(fc6h, fc3h));
            when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE"))
                    .thenReturn(Collections.emptyList());

            MonitoringPriorityResponse response = monitoringService.getMonitoringPriority(SHIVAJINAGAR_H3, PUNE_CITY_ID);

            assertThat(response.forecastHorizonHours()).isEqualTo(3);
        }

        @Test
        @DisplayName("Invalid H3 index throws IllegalArgumentException")
        void testInvalidH3Index() {
            assertThatThrownBy(() -> monitoringService.getMonitoringPriority("invalid_h3", PUNE_CITY_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid H3 index");
        }

        @Test
        @DisplayName("Null cityId throws IllegalArgumentException")
        void testNullCityId() {
            assertThatThrownBy(() -> monitoringService.getMonitoringPriority(SHIVAJINAGAR_H3, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("City ID must not be null");
        }
    }
}
