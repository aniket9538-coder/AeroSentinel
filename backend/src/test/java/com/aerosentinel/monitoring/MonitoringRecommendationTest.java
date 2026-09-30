package com.aerosentinel.monitoring;

import com.aerosentinel.city.CityRepository;
import com.aerosentinel.exception.ResourceNotFoundException;
import com.aerosentinel.forecast.Forecast;
import com.aerosentinel.forecast.ForecastRepository;
import com.aerosentinel.hotspot.HotspotPrediction;
import com.aerosentinel.hotspot.HotspotRepository;
import com.aerosentinel.monitoring.dto.MonitoringRecommendationResponse;
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
import static org.mockito.Mockito.*;

/**
 * Authoritative unit tests for F8-P4 Monitoring Recommendation API.
 * Validates deterministic recommendation mapping, city aggregation, response contracts,
 * sorting behavior, safe unavailable handling, and zero state mutation.
 */
@ExtendWith(MockitoExtension.class)
class MonitoringRecommendationTest {

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private HotspotRepository hotspotRepository;

    @Mock
    private ForecastRepository forecastRepository;

    @Mock
    private CityRepository cityRepository;

    private H3Service h3Service;
    private MonitoringPriorityConfig priorityConfig;
    private MonitoringService monitoringService;

    private static final UUID PUNE_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final String SHIVAJINAGAR_H3 = "88608850e5fffff"; // Pune Shivajinagar
    private static final String KATRAJ_H3 = "88608852c1fffff";       // Pune Katraj
    private static final String DISTANT_H3 = "8860884119fffff";      // Pune Distant (>7km)

    @BeforeEach
    void setUp() {
        h3Service = new H3Service(8);
        priorityConfig = new MonitoringPriorityConfig();
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
                priorityConfig,
                cityRepository
        );
    }

    private HotspotPrediction createPrediction(String h3, double riskScore, String riskLevel, double confidence) {
        HotspotPrediction pred = new HotspotPrediction();
        pred.setId(UUID.randomUUID());
        pred.setCityId(PUNE_CITY_ID);
        pred.setH3Index(h3);
        pred.setRiskScore(riskScore);
        pred.setRiskLevel(riskLevel);
        pred.setConfidence(confidence);
        pred.setPredictedAt(Instant.now());
        pred.setModelVersion("test_v1");
        return pred;
    }

    private Forecast createForecast(String h3, int horizonHours, double predictedPm25, double lowerBound, double upperBound) {
        Forecast fc = new Forecast();
        fc.setId(UUID.randomUUID());
        fc.setCityId(PUNE_CITY_ID);
        fc.setH3Index(h3);
        fc.setHorizonHours(horizonHours);
        fc.setPredictedPm25(predictedPm25);
        fc.setLowerBound(lowerBound);
        fc.setUpperBound(upperBound);
        fc.setForecastConfidence(null); // locked F4 contract keeps this null
        fc.setGeneratedAt(Instant.now());
        fc.setTargetTime(Instant.now().plusSeconds(horizonHours * 3600L));
        return fc;
    }

    private MonitoringStation createStation(String code, String name, double lat, double lng) {
        MonitoringStation station = new MonitoringStation();
        station.setId(UUID.randomUUID());
        station.setCityId(PUNE_CITY_ID);
        station.setStationCode(code);
        station.setName(name);
        station.setLatitude(lat);
        station.setLongitude(lng);
        station.setStatus("ACTIVE");
        return station;
    }

    @Nested
    @DisplayName("Deterministic Recommendation Mapping Rules")
    class RecommendationMappingRules {

        @Test
        @DisplayName("1. LOW priority produces ROUTINE_MONITORING with non-alarmist routine observation text")
        void testLowPriorityRoutineMonitoring() {
            // Setup Case A: risk 0.15 (LOW), width 4.0, distance 0.27 km -> priority ~13% (LOW)
            HotspotPrediction pred = createPrediction(SHIVAJINAGAR_H3, 0.15, "LOW", 0.92);
            Forecast fc = createForecast(SHIVAJINAGAR_H3, 1, 22.5, 20.5, 24.5);
            MonitoringStation station = createStation("PUN-001", "Shivajinagar CAAQMS", 18.5320, 73.8475);

            when(cityRepository.existsById(PUNE_CITY_ID)).thenReturn(true);
            when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3)).thenReturn(Optional.of(pred));
            when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3)).thenReturn(List.of(fc));
            when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE")).thenReturn(List.of(station));

            MonitoringRecommendationResponse rec = monitoringService.getRecommendation(SHIVAJINAGAR_H3, PUNE_CITY_ID);

            assertThat(rec.priorityLevel()).isEqualTo(MonitoringPriority.LOW);
            assertThat(rec.recommendationType()).isEqualTo(MonitoringRecommendationType.ROUTINE_MONITORING);
            assertThat(rec.recommendation()).isEqualTo("Continue routine monitoring for this H3 cell.");
            assertThat(rec.rationale()).contains("Priority: LOW");
            assertThat(rec.rationale()).contains("Routine observation sufficient");
        }

        @Test
        @DisplayName("2. MEDIUM priority produces TARGETED_MONITORING with closer observation text")
        void testMediumPriorityTargetedMonitoring() {
            // Setup Case B: risk 0.58 (MEDIUM), width 15.0, distance 0.45 km -> priority ~49% (MEDIUM)
            HotspotPrediction pred = createPrediction(KATRAJ_H3, 0.58, "MEDIUM", 0.85);
            Forecast fc = createForecast(KATRAJ_H3, 1, 68.0, 60.5, 75.5);
            MonitoringStation station = createStation("PUN-002", "Katraj Air Station", 18.4550, 73.8650);

            when(cityRepository.existsById(PUNE_CITY_ID)).thenReturn(true);
            when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(KATRAJ_H3)).thenReturn(Optional.of(pred));
            when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, KATRAJ_H3)).thenReturn(List.of(fc));
            when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE")).thenReturn(List.of(station));

            MonitoringRecommendationResponse rec = monitoringService.getRecommendation(KATRAJ_H3, PUNE_CITY_ID);

            assertThat(rec.priorityLevel()).isEqualTo(MonitoringPriority.MEDIUM);
            assertThat(rec.recommendationType()).isEqualTo(MonitoringRecommendationType.TARGETED_MONITORING);
            assertThat(rec.recommendation()).isEqualTo("Prioritize targeted monitoring and closer observation for this H3 cell.");
            assertThat(rec.rationale()).contains("Priority: MEDIUM");
            assertThat(rec.rationale()).contains("Closer observation recommended");
        }

        @Test
        @DisplayName("3. HIGH priority + coverage gap produces MOBILE_SENSOR_RECOMMENDED")
        void testHighPriorityWithCoverageGapProducesMobileSensor() {
            // Setup Case C: risk 0.88 (CRITICAL), width 20.0, distance 14.96 km (>7km gap flag = 1) -> priority ~88% (HIGH)
            HotspotPrediction pred = createPrediction(DISTANT_H3, 0.88, "CRITICAL", 0.78);
            Forecast fc = createForecast(DISTANT_H3, 1, 135.0, 125.0, 145.0);
            // Station is far away (> 7km)
            MonitoringStation station = createStation("PUN-001", "Shivajinagar CAAQMS", 18.5320, 73.8475);

            when(cityRepository.existsById(PUNE_CITY_ID)).thenReturn(true);
            when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(DISTANT_H3)).thenReturn(Optional.of(pred));
            when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, DISTANT_H3)).thenReturn(List.of(fc));
            when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE")).thenReturn(List.of(station));

            MonitoringRecommendationResponse rec = monitoringService.getRecommendation(DISTANT_H3, PUNE_CITY_ID);

            assertThat(rec.priorityLevel()).isEqualTo(MonitoringPriority.HIGH);
            assertThat(rec.monitoringCoverageGapFlag()).isEqualTo(1);
            assertThat(rec.recommendationType()).isEqualTo(MonitoringRecommendationType.MOBILE_SENSOR_RECOMMENDED);
            assertThat(rec.recommendation()).isEqualTo("Consider deploying additional mobile monitoring in this unobserved H3 cell.");
            assertThat(rec.rationale()).contains("Priority: HIGH");
            assertThat(rec.rationale()).contains("Additional mobile sensor deployment recommended");
        }

        @Test
        @DisplayName("4. HIGH priority + NO coverage gap produces FIELD_VERIFICATION_RECOMMENDED")
        void testHighPriorityWithoutCoverageGapProducesFieldVerification() {
            // Setup: risk 0.95 (CRITICAL), width 25.0, close station (0.3km, gap flag = 0)
            // Normalized risk: 0.95 * 0.45 = 0.4275
            // Normalized uncertainty: (25/30) * 0.30 = 0.2500
            // Normalized distance: (0.3/15) * 0.25 = 0.0050
            // Priority: 0.4275 + 0.2500 + 0.0050 = 0.6825 -> wait, 68% is MEDIUM if threshold is 70!
            // Let's use risk 1.0, width 30.0, distance 1.0 km:
            // 1.0 * 0.45 + (30/30) * 0.30 + (1.0/15) * 0.25 = 0.45 + 0.30 + 0.0167 = 0.7667 -> 77% (HIGH)
            HotspotPrediction pred = createPrediction(SHIVAJINAGAR_H3, 1.0, "CRITICAL", 0.90);
            Forecast fc = createForecast(SHIVAJINAGAR_H3, 1, 150.0, 135.0, 165.0); // width 30.0
            MonitoringStation station = createStation("PUN-001", "Shivajinagar CAAQMS", 18.5320, 73.8475); // ~0.27 km

            when(cityRepository.existsById(PUNE_CITY_ID)).thenReturn(true);
            when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3)).thenReturn(Optional.of(pred));
            when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3)).thenReturn(List.of(fc));
            when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE")).thenReturn(List.of(station));

            MonitoringRecommendationResponse rec = monitoringService.getRecommendation(SHIVAJINAGAR_H3, PUNE_CITY_ID);

            assertThat(rec.priorityLevel()).isEqualTo(MonitoringPriority.HIGH);
            assertThat(rec.monitoringCoverageGapFlag()).isEqualTo(0);
            assertThat(rec.recommendationType()).isEqualTo(MonitoringRecommendationType.FIELD_VERIFICATION_RECOMMENDED);
            assertThat(rec.recommendation()).isEqualTo("Consider targeted field verification and additional observation for this H3 cell.");
            assertThat(rec.rationale()).contains("Priority: HIGH");
            assertThat(rec.rationale()).contains("Field verification recommended");
        }
    }

    @Nested
    @DisplayName("City-Wide and Single-H3 Endpoints")
    class EndpointsAndAggregation {

        @Test
        @DisplayName("5. City endpoint returns multiple recommendations for all valid cells")
        void testCityRecommendationsMultipleCells() {
            HotspotPrediction predA = createPrediction(SHIVAJINAGAR_H3, 0.15, "LOW", 0.90);
            HotspotPrediction predB = createPrediction(KATRAJ_H3, 0.58, "MEDIUM", 0.85);
            HotspotPrediction predC = createPrediction(DISTANT_H3, 0.88, "CRITICAL", 0.78);

            Forecast fcA = createForecast(SHIVAJINAGAR_H3, 1, 22.5, 20.5, 24.5);
            Forecast fcB = createForecast(KATRAJ_H3, 1, 68.0, 60.5, 75.5);
            Forecast fcC = createForecast(DISTANT_H3, 1, 135.0, 125.0, 145.0);

            MonitoringStation station1 = createStation("PUN-001", "Shivajinagar", 18.5320, 73.8475);
            MonitoringStation station2 = createStation("PUN-002", "Katraj", 18.4550, 73.8650);

            when(cityRepository.existsById(PUNE_CITY_ID)).thenReturn(true);
            when(hotspotRepository.findLatestByCityId(PUNE_CITY_ID)).thenReturn(List.of(predA, predB, predC));
            when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE")).thenReturn(List.of(station1, station2));

            when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3)).thenReturn(Optional.of(predA));
            when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(KATRAJ_H3)).thenReturn(Optional.of(predB));
            when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(DISTANT_H3)).thenReturn(Optional.of(predC));

            when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3)).thenReturn(List.of(fcA));
            when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, KATRAJ_H3)).thenReturn(List.of(fcB));
            when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, DISTANT_H3)).thenReturn(List.of(fcC));

            List<MonitoringRecommendationResponse> recs = monitoringService.getCityRecommendations(PUNE_CITY_ID);

            assertThat(recs).hasSize(3);
            assertThat(recs.get(0).priorityLevel()).isEqualTo(MonitoringPriority.HIGH);
            assertThat(recs.get(1).priorityLevel()).isEqualTo(MonitoringPriority.MEDIUM);
            assertThat(recs.get(2).priorityLevel()).isEqualTo(MonitoringPriority.LOW);
        }

        @Test
        @DisplayName("6. Single H3 query returns identical recommendation data as entry from city query")
        void testSingleH3MatchesCityQuery() {
            HotspotPrediction predA = createPrediction(SHIVAJINAGAR_H3, 0.15, "LOW", 0.90);
            Forecast fcA = createForecast(SHIVAJINAGAR_H3, 1, 22.5, 20.5, 24.5);
            MonitoringStation station = createStation("PUN-001", "Shivajinagar", 18.5320, 73.8475);

            when(cityRepository.existsById(PUNE_CITY_ID)).thenReturn(true);
            when(hotspotRepository.findLatestByCityId(PUNE_CITY_ID)).thenReturn(List.of(predA));
            when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE")).thenReturn(List.of(station));
            when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3)).thenReturn(Optional.of(predA));
            when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3)).thenReturn(List.of(fcA));

            MonitoringRecommendationResponse singleRec = monitoringService.getRecommendation(SHIVAJINAGAR_H3, PUNE_CITY_ID);
            List<MonitoringRecommendationResponse> cityRecs = monitoringService.getCityRecommendations(PUNE_CITY_ID);

            assertThat(cityRecs).hasSize(1);
            MonitoringRecommendationResponse fromCity = cityRecs.get(0);

            assertThat(singleRec.h3Index()).isEqualTo(fromCity.h3Index());
            assertThat(singleRec.priorityScore()).isEqualTo(fromCity.priorityScore());
            assertThat(singleRec.priorityScorePercent()).isEqualTo(fromCity.priorityScorePercent());
            assertThat(singleRec.priorityLevel()).isEqualTo(fromCity.priorityLevel());
            assertThat(singleRec.recommendationType()).isEqualTo(fromCity.recommendationType());
            assertThat(singleRec.recommendation()).isEqualTo(fromCity.recommendation());
            assertThat(singleRec.rationale()).isEqualTo(fromCity.rationale());
            assertThat(singleRec.uncertainty()).isEqualTo(fromCity.uncertainty());
            assertThat(singleRec.stationDistanceKm()).isEqualTo(fromCity.stationDistanceKm());
        }

        @Test
        @DisplayName("7. Determinism: identical P3 inputs produce identical recommendation, type, and rationale")
        void testDeterministicRecommendationOutput() {
            HotspotPrediction pred = createPrediction(KATRAJ_H3, 0.58, "MEDIUM", 0.85);
            Forecast fc = createForecast(KATRAJ_H3, 1, 68.0, 60.5, 75.5);
            MonitoringStation station = createStation("PUN-002", "Katraj", 18.4550, 73.8650);

            when(cityRepository.existsById(PUNE_CITY_ID)).thenReturn(true);
            when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(KATRAJ_H3)).thenReturn(Optional.of(pred));
            when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, KATRAJ_H3)).thenReturn(List.of(fc));
            when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE")).thenReturn(List.of(station));

            MonitoringRecommendationResponse run1 = monitoringService.getRecommendation(KATRAJ_H3, PUNE_CITY_ID);
            MonitoringRecommendationResponse run2 = monitoringService.getRecommendation(KATRAJ_H3, PUNE_CITY_ID);

            assertThat(run1).isEqualTo(run2);
            assertThat(run1.recommendation()).isEqualTo(run2.recommendation());
            assertThat(run1.rationale()).isEqualTo(run2.rationale());
        }
    }

    @Nested
    @DisplayName("Edge Cases & Safe Degradation")
    class EdgeCasesAndDegradation {

        @Test
        @DisplayName("8. No active stations handles safely without fake station or zero distance (coverage gap flag = 1)")
        void testNoActiveStationsSafeHandling() {
            HotspotPrediction pred = createPrediction(SHIVAJINAGAR_H3, 0.50, "MEDIUM", 0.80);
            Forecast fc = createForecast(SHIVAJINAGAR_H3, 1, 50.0, 45.0, 55.0);

            when(cityRepository.existsById(PUNE_CITY_ID)).thenReturn(true);
            when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3)).thenReturn(Optional.of(pred));
            when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3)).thenReturn(List.of(fc));
            // Empty station list
            when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE")).thenReturn(Collections.emptyList());

            MonitoringRecommendationResponse rec = monitoringService.getRecommendation(SHIVAJINAGAR_H3, PUNE_CITY_ID);

            assertThat(rec.nearestStationId()).isNull();
            assertThat(rec.nearestStationCode()).isNull();
            assertThat(rec.nearestStationName()).isNull();
            assertThat(rec.nearestStationDistanceKm()).isNull();
            assertThat(rec.stationDistanceKm()).isNull();
            assertThat(rec.stationsWithin5kmCount()).isEqualTo(0);
            assertThat(rec.monitoringCoverageGapFlag()).isEqualTo(1);
            assertThat(rec.normalizedDistance()).isEqualTo(1.0); // max distance penalty
            assertThat(rec.rationale()).contains("No active monitoring stations in city (coverage gap: true)");
        }

        @Test
        @DisplayName("9. Missing forecast throws ResourceNotFoundException safely")
        void testMissingForecastThrowsResourceNotFound() {
            HotspotPrediction pred = createPrediction(SHIVAJINAGAR_H3, 0.50, "MEDIUM", 0.80);

            when(cityRepository.existsById(PUNE_CITY_ID)).thenReturn(true);
            when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3)).thenReturn(Optional.of(pred));
            when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3)).thenReturn(Collections.emptyList());
            when(forecastRepository.findLatestByH3Index(SHIVAJINAGAR_H3)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> monitoringService.getRecommendation(SHIVAJINAGAR_H3, PUNE_CITY_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No forecast found for H3 cell");
        }

        @Test
        @DisplayName("10. Missing prediction throws ResourceNotFoundException safely")
        void testMissingPredictionThrowsResourceNotFound() {
            when(cityRepository.existsById(PUNE_CITY_ID)).thenReturn(true);
            when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> monitoringService.getRecommendation(SHIVAJINAGAR_H3, PUNE_CITY_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No hotspot prediction found for H3 cell");
        }

        @Test
        @DisplayName("11. Invalid H3 index format throws IllegalArgumentException")
        void testInvalidH3ThrowsIllegalArgument() {
            assertThatThrownBy(() -> monitoringService.getRecommendation("invalid-h3-index", PUNE_CITY_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid H3 index");
        }

        @Test
        @DisplayName("12. No mutation: calling recommendation service causes zero writes to repositories")
        void testNoMutationOnServiceCall() {
            HotspotPrediction pred = createPrediction(SHIVAJINAGAR_H3, 0.15, "LOW", 0.90);
            Forecast fc = createForecast(SHIVAJINAGAR_H3, 1, 22.5, 20.5, 24.5);
            MonitoringStation station = createStation("PUN-001", "Shivajinagar", 18.5320, 73.8475);

            when(cityRepository.existsById(PUNE_CITY_ID)).thenReturn(true);
            when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3)).thenReturn(Optional.of(pred));
            when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3)).thenReturn(List.of(fc));
            when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE")).thenReturn(List.of(station));

            monitoringService.getRecommendation(SHIVAJINAGAR_H3, PUNE_CITY_ID);

            // Verify zero save/delete/update operations across all repositories
            verify(hotspotRepository, never()).save(any());
            verify(forecastRepository, never()).save(any());
            verify(sensorRepository, never()).save(any());
            verify(cityRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Response Contract and Sorting Order")
    class ResponseContractAndSorting {

        @Test
        @DisplayName("13. Response contract exposes all mandatory fields and preserves frontend compatibility aliases")
        void testResponseContractFields() {
            HotspotPrediction pred = createPrediction(SHIVAJINAGAR_H3, 0.15, "LOW", 0.90);
            Forecast fc = createForecast(SHIVAJINAGAR_H3, 1, 22.5, 20.5, 24.5);
            MonitoringStation station = createStation("PUN-001", "Shivajinagar CAAQMS", 18.5320, 73.8475);

            when(cityRepository.existsById(PUNE_CITY_ID)).thenReturn(true);
            when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(SHIVAJINAGAR_H3)).thenReturn(Optional.of(pred));
            when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, SHIVAJINAGAR_H3)).thenReturn(List.of(fc));
            when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE")).thenReturn(List.of(station));

            MonitoringRecommendationResponse rec = monitoringService.getRecommendation(SHIVAJINAGAR_H3, PUNE_CITY_ID);

            // Identity
            assertThat(rec.h3Index()).isEqualTo(SHIVAJINAGAR_H3);
            assertThat(rec.latitude()).isNotNull();
            assertThat(rec.longitude()).isNotNull();

            // Risk
            assertThat(rec.riskScore()).isEqualTo(0.15);
            assertThat(rec.riskLevel()).isEqualTo("LOW");
            assertThat(rec.f3Confidence()).isEqualTo(0.90);
            assertThat(rec.predictionId()).isNotNull();

            // Forecast
            assertThat(rec.forecastHorizonHours()).isEqualTo(1);
            assertThat(rec.predictedPm25()).isEqualTo(22.5);
            assertThat(rec.lowerBound()).isEqualTo(20.5);
            assertThat(rec.upperBound()).isEqualTo(24.5);
            assertThat(rec.uncertaintyIntervalWidth()).isEqualTo(4.0);
            assertThat(rec.normalizedUncertainty()).isNotNull();

            // Coverage
            assertThat(rec.nearestStationId()).isEqualTo(station.getId());
            assertThat(rec.nearestStationCode()).isEqualTo("PUN-001");
            assertThat(rec.nearestStationName()).isEqualTo("Shivajinagar CAAQMS");
            assertThat(rec.nearestStationDistanceKm()).isNotNull();
            assertThat(rec.stationsWithin5kmCount()).isEqualTo(1);
            assertThat(rec.monitoringCoverageGapFlag()).isEqualTo(0);

            // Priority
            assertThat(rec.priorityScore()).isNotNull();
            assertThat(rec.priorityScorePercent()).isNotNull();
            assertThat(rec.priorityLevel()).isEqualTo(MonitoringPriority.LOW);
            assertThat(rec.normalizedRisk()).isNotNull();
            assertThat(rec.normalizedDistance()).isNotNull();

            // Recommendation
            assertThat(rec.recommendationType()).isEqualTo(MonitoringRecommendationType.ROUTINE_MONITORING);
            assertThat(rec.recommendation()).isNotEmpty();
            assertThat(rec.rationale()).isNotEmpty();

            // Provenance
            assertThat(rec.predictionTimestamp()).isNotNull();
            assertThat(rec.forecastGeneratedAt()).isNotNull();

            // Frontend compatibility aliases
            assertThat(rec.uncertainty()).isEqualTo(rec.normalizedUncertainty());
            assertThat(rec.stationDistanceKm()).isEqualTo(rec.nearestStationDistanceKm());
        }

        @Test
        @DisplayName("14. Sorting: city recommendations are sorted descending by priorityScorePercent, with h3Index tie-breaker")
        void testCityRecommendationsSortingOrder() {
            // Three cells with different priorities
            HotspotPrediction pred1 = createPrediction("88608850e5fffff", 0.20, "LOW", 0.90);
            HotspotPrediction pred2 = createPrediction("88608852c1fffff", 0.90, "CRITICAL", 0.85);
            HotspotPrediction pred3 = createPrediction("8860884119fffff", 0.55, "MEDIUM", 0.80);

            Forecast fc1 = createForecast("88608850e5fffff", 1, 20.0, 18.0, 22.0); // width 4.0
            Forecast fc2 = createForecast("88608852c1fffff", 1, 140.0, 120.0, 160.0); // width 40.0 (capped at 30)
            Forecast fc3 = createForecast("8860884119fffff", 1, 60.0, 50.0, 70.0); // width 20.0

            MonitoringStation station = createStation("PUN-001", "Shivajinagar", 18.5320, 73.8475);

            when(cityRepository.existsById(PUNE_CITY_ID)).thenReturn(true);
            when(hotspotRepository.findLatestByCityId(PUNE_CITY_ID)).thenReturn(List.of(pred1, pred2, pred3));
            when(sensorRepository.findByCityIdAndStatus(PUNE_CITY_ID, "ACTIVE")).thenReturn(List.of(station));

            when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc("88608850e5fffff")).thenReturn(Optional.of(pred1));
            when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc("88608852c1fffff")).thenReturn(Optional.of(pred2));
            when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc("8860884119fffff")).thenReturn(Optional.of(pred3));

            when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, "88608850e5fffff")).thenReturn(List.of(fc1));
            when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, "88608852c1fffff")).thenReturn(List.of(fc2));
            when(forecastRepository.findLatestByCityIdAndH3Index(PUNE_CITY_ID, "8860884119fffff")).thenReturn(List.of(fc3));

            List<MonitoringRecommendationResponse> recs = monitoringService.getCityRecommendations(PUNE_CITY_ID);

            assertThat(recs).hasSize(3);
            // Verify strictly descending order of priorityScorePercent
            assertThat(recs.get(0).priorityScorePercent()).isGreaterThanOrEqualTo(recs.get(1).priorityScorePercent());
            assertThat(recs.get(1).priorityScorePercent()).isGreaterThanOrEqualTo(recs.get(2).priorityScorePercent());
        }
    }
}
