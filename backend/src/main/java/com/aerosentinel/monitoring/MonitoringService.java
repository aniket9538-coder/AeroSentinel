package com.aerosentinel.monitoring;

import com.aerosentinel.city.CityRepository;
import com.aerosentinel.exception.ResourceNotFoundException;
import com.aerosentinel.feature.FeatureEngineeringService;
import com.aerosentinel.forecast.Forecast;
import com.aerosentinel.forecast.ForecastRepository;
import com.aerosentinel.hotspot.HotspotPrediction;
import com.aerosentinel.hotspot.HotspotRepository;
import com.aerosentinel.monitoring.dto.MonitoringCoverageResponse;
import com.aerosentinel.monitoring.dto.MonitoringPriorityResponse;
import com.aerosentinel.monitoring.dto.MonitoringRecommendationResponse;
import com.aerosentinel.sensor.MonitoringStation;
import com.aerosentinel.sensor.SensorRepository;
import com.aerosentinel.spatial.H3Service;
import com.uber.h3core.util.LatLng;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Authoritative service for F8 monitoring coverage, station distance, and monitoring priority calculation.
 *
 * Combines:
 * - F3 hotspot risk score & confidence
 * - F4 forecast prediction interval width (upperBound - lowerBound) as primary uncertainty proxy
 * - F8-P2 nearest active station distance and observation coverage
 * into a transparent, deterministic priority score to support mobile sensor and field inspection triage.
 */
@Service
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    public static final double COVERAGE_GAP_THRESHOLD_KM = 7.0;
    public static final double NEARBY_STATION_RADIUS_KM = 5.0;

    private final SensorRepository sensorRepository;
    private final H3Service h3Service;
    private final HotspotRepository hotspotRepository;
    private final ForecastRepository forecastRepository;
    private final MonitoringPriorityConfig priorityConfig;
    private final CityRepository cityRepository;

    @Autowired
    public MonitoringService(
            SensorRepository sensorRepository,
            H3Service h3Service,
            HotspotRepository hotspotRepository,
            ForecastRepository forecastRepository,
            MonitoringPriorityConfig priorityConfig,
            CityRepository cityRepository
    ) {
        this.sensorRepository = sensorRepository;
        this.h3Service = h3Service;
        this.hotspotRepository = hotspotRepository;
        this.forecastRepository = forecastRepository;
        this.priorityConfig = priorityConfig != null ? priorityConfig : new MonitoringPriorityConfig();
        this.cityRepository = cityRepository;
    }

    public MonitoringService(
            SensorRepository sensorRepository,
            H3Service h3Service,
            HotspotRepository hotspotRepository,
            ForecastRepository forecastRepository,
            MonitoringPriorityConfig priorityConfig
    ) {
        this(sensorRepository, h3Service, hotspotRepository, forecastRepository, priorityConfig, null);
    }

    /**
     * Backwards-compatible constructor for testing and F8-P2 distance-only execution.
     */
    public MonitoringService(SensorRepository sensorRepository, H3Service h3Service) {
        this(sensorRepository, h3Service, null, null, new MonitoringPriorityConfig(), null);
    }

    /**
     * Computes monitoring coverage for an H3 cell within a specified city (F8-P2).
     *
     * @param h3Index 15-character Uber H3 cell index
     * @param cityId  City identifier
     * @return MonitoringCoverageResponse containing nearest active station metadata, distance in km,
     *         stations within 5 km count, and coverage gap flag.
     */
    public MonitoringCoverageResponse getMonitoringCoverage(String h3Index, UUID cityId) {
        if (!h3Service.validateH3Index(h3Index)) {
            throw new IllegalArgumentException("Invalid H3 index: " + h3Index);
        }
        if (cityId == null) {
            throw new IllegalArgumentException("City ID must not be null");
        }

        LatLng center = h3Service.h3ToCenter(h3Index);
        double centerLat = center.lat;
        double centerLng = center.lng;

        List<MonitoringStation> stations = sensorRepository.findByCityIdAndStatus(cityId, "ACTIVE");

        MonitoringStation nearestStation = null;
        double minDistance = Double.MAX_VALUE;
        int count5km = 0;

        if (stations != null) {
            for (MonitoringStation station : stations) {
                if (station == null || station.getStatus() == null || !"ACTIVE".equalsIgnoreCase(station.getStatus())) {
                    continue;
                }
                Double sLat = station.getLatitude();
                Double sLng = station.getLongitude();
                if (sLat == null || sLng == null || Double.isNaN(sLat) || Double.isNaN(sLng)
                        || sLat < -90.0 || sLat > 90.0 || sLng < -180.0 || sLng > 180.0) {
                    continue;
                }

                double distance = FeatureEngineeringService.calculateHaversineDistanceKm(centerLat, centerLng, sLat, sLng);
                if (distance <= NEARBY_STATION_RADIUS_KM) {
                    count5km++;
                }
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestStation = station;
                }
            }
        }

        if (nearestStation == null) {
            log.debug("No active monitoring stations found for cityId={} near H3 cell={}", cityId, h3Index);
            return MonitoringCoverageResponse.noCoverage(h3Index, centerLat, centerLng);
        }

        double roundedDistance = round2(minDistance);
        int gapFlag = minDistance > COVERAGE_GAP_THRESHOLD_KM ? 1 : 0;

        return new MonitoringCoverageResponse(
                h3Index,
                centerLat,
                centerLng,
                nearestStation.getId(),
                nearestStation.getStationCode(),
                nearestStation.getName(),
                nearestStation.getLatitude(),
                nearestStation.getLongitude(),
                roundedDistance,
                count5km,
                gapFlag
        );
    }

    /**
     * Retrieves and computes the comprehensive F8 monitoring priority for an H3 cell.
     *
     * @param h3Index 15-character Uber H3 cell identifier
     * @param cityId  City identifier
     * @return MonitoringPriorityResponse containing all inputs, normalized factors, and priority score
     */
    public MonitoringPriorityResponse getMonitoringPriority(String h3Index, UUID cityId) {
        if (!h3Service.validateH3Index(h3Index)) {
            throw new IllegalArgumentException("Invalid H3 index: " + h3Index);
        }
        if (cityId == null) {
            throw new IllegalArgumentException("City ID must not be null");
        }
        if (hotspotRepository == null || forecastRepository == null) {
            throw new IllegalStateException("HotspotRepository and ForecastRepository must be configured for priority calculation");
        }

        // 1. Resolve authoritative F3 parent prediction
        HotspotPrediction prediction = hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(h3Index)
                .orElseThrow(() -> new ResourceNotFoundException("No hotspot prediction found for H3 cell: " + h3Index));

        // 2. Resolve authoritative F4 forecasts (prefer 1h operational horizon)
        List<Forecast> forecasts = forecastRepository.findLatestByCityIdAndH3Index(cityId, h3Index);
        if (forecasts == null || forecasts.isEmpty()) {
            forecasts = forecastRepository.findLatestByH3Index(h3Index);
        }
        if (forecasts == null || forecasts.isEmpty()) {
            throw new ResourceNotFoundException("No forecast found for H3 cell: " + h3Index);
        }

        Forecast operationalForecast = selectOperationalForecast(forecasts);
        if (operationalForecast == null) {
            throw new ResourceNotFoundException("No operational forecast available for H3 cell: " + h3Index);
        }

        // 3. Compute F8-P2 monitoring coverage
        MonitoringCoverageResponse coverage = getMonitoringCoverage(h3Index, cityId);

        // 4. Calculate deterministic priority score
        return calculatePriority(prediction, operationalForecast, coverage);
    }

    /**
     * Pure calculation engine that computes monitoring priority from provided inputs.
     * Guaranteed deterministic: identical inputs yield identical outputs.
     */
    public MonitoringPriorityResponse calculatePriority(
            HotspotPrediction prediction,
            Forecast forecast,
            MonitoringCoverageResponse coverage
    ) {
        if (prediction == null) {
            throw new IllegalArgumentException("HotspotPrediction cannot be null");
        }
        if (forecast == null) {
            throw new IllegalArgumentException("Forecast cannot be null");
        }
        if (coverage == null) {
            throw new IllegalArgumentException("MonitoringCoverageResponse cannot be null");
        }

        Double riskScore = prediction.getRiskScore();
        if (riskScore == null || Double.isNaN(riskScore) || Double.isInfinite(riskScore)) {
            throw new IllegalArgumentException("Risk score must be a valid non-null finite number");
        }

        Double lowerBound = forecast.getLowerBound();
        Double upperBound = forecast.getUpperBound();
        if (lowerBound == null || upperBound == null || Double.isNaN(lowerBound) || Double.isNaN(upperBound)
                || Double.isInfinite(lowerBound) || Double.isInfinite(upperBound)) {
            throw new IllegalArgumentException("Forecast bounds must be valid non-null finite numbers");
        }
        if (lowerBound > upperBound) {
            throw new IllegalArgumentException(
                    "Invalid forecast interval: lowerBound (" + lowerBound + ") > upperBound (" + upperBound + ")"
            );
        }

        double intervalWidth = round2(upperBound - lowerBound);
        if (intervalWidth < 0.0) {
            throw new IllegalArgumentException("Forecast interval width cannot be negative: " + intervalWidth);
        }

        // Normalizations
        double normRisk = normalizeRisk(riskScore);
        double normUncertainty = normalizeUncertainty(lowerBound, upperBound);
        double normDistance = normalizeDistance(coverage.nearestStationDistanceKm());

        // Weighted priority score
        double priorityScore = calculatePriorityScore(normRisk, normUncertainty, normDistance);
        int priorityScorePercent = (int) Math.round(priorityScore * 100.0);
        priorityScorePercent = (int) clamp(priorityScorePercent, 0, 100);

        // Classification level
        MonitoringPriority priorityLevel = classifyPriority(priorityScorePercent);

        return new MonitoringPriorityResponse(
                coverage.h3Index(),
                coverage.latitude(),
                coverage.longitude(),
                prediction.getRiskScore(),
                prediction.getRiskLevel(),
                prediction.getConfidence(),
                prediction.getId(),
                prediction.getPredictedAt(),
                forecast.getHorizonHours(),
                forecast.getPredictedPm25(),
                forecast.getLowerBound(),
                forecast.getUpperBound(),
                intervalWidth,
                normUncertainty,
                forecast.getGeneratedAt(),
                coverage.nearestStationId(),
                coverage.nearestStationCode(),
                coverage.nearestStationName(),
                coverage.nearestStationDistanceKm(),
                coverage.stationsWithin5kmCount(),
                coverage.monitoringCoverageGapFlag(),
                normRisk,
                normDistance,
                priorityScore,
                priorityScorePercent,
                priorityLevel,
                priorityConfig.getRiskWeight(),
                priorityConfig.getUncertaintyWeight(),
                priorityConfig.getDistanceWeight(),
                priorityConfig.getUncertaintyMaxIntervalWidth(),
                priorityConfig.getDistanceMaxKm(),
                priorityConfig.getMediumThreshold(),
                priorityConfig.getHighThreshold()
        );
    }

    /**
     * Selects the single operational forecast for F8 priority calculation.
     * Prefers the 1-hour horizon (operational immediacy + lowest dispersion compounding).
     * If 1h is unavailable, selects the shortest available horizon.
     */
    public Forecast selectOperationalForecast(List<Forecast> forecasts) {
        if (forecasts == null || forecasts.isEmpty()) {
            return null;
        }
        for (Forecast f : forecasts) {
            if (f.getHorizonHours() != null && f.getHorizonHours() == 1) {
                return f;
            }
        }
        return forecasts.stream()
                .filter(f -> f.getHorizonHours() != null)
                .min(Comparator.comparingInt(Forecast::getHorizonHours))
                .orElse(forecasts.get(0));
    }

    /**
     * Normalizes risk score into [0.0, 1.0].
     */
    public double normalizeRisk(double riskScore) {
        if (Double.isNaN(riskScore) || Double.isInfinite(riskScore)) {
            throw new IllegalArgumentException("Invalid risk score: " + riskScore);
        }
        return round4(clamp(riskScore, 0.0, 1.0));
    }

    /**
     * Normalizes forecast prediction interval width (upperBound - lowerBound) into [0.0, 1.0].
     * Wider interval = higher uncertainty.
     */
    public double normalizeUncertainty(double lowerBound, double upperBound) {
        if (Double.isNaN(lowerBound) || Double.isNaN(upperBound) || Double.isInfinite(lowerBound) || Double.isInfinite(upperBound)) {
            throw new IllegalArgumentException("Forecast bounds cannot be NaN or Infinite");
        }
        if (lowerBound > upperBound) {
            throw new IllegalArgumentException("Invalid forecast interval: lowerBound (" + lowerBound + ") > upperBound (" + upperBound + ")");
        }
        double intervalWidth = upperBound - lowerBound;
        if (intervalWidth < 0.0) {
            throw new IllegalArgumentException("Negative interval width: " + intervalWidth);
        }
        double normalized = intervalWidth / priorityConfig.getUncertaintyMaxIntervalWidth();
        return round4(clamp(normalized, 0.0, 1.0));
    }

    /**
     * Normalizes nearest station distance in km into [0.0, 1.0].
     * If distance is null (no active stations in city), returns 1.0 (maximum distance penalty).
     */
    public double normalizeDistance(Double distanceKm) {
        if (distanceKm == null) {
            return 1.0;
        }
        if (Double.isNaN(distanceKm) || Double.isInfinite(distanceKm)) {
            throw new IllegalArgumentException("Invalid station distance: " + distanceKm);
        }
        double normalized = distanceKm / priorityConfig.getDistanceMaxKm();
        return round4(clamp(normalized, 0.0, 1.0));
    }

    /**
     * Computes the weighted priority score:
     * priorityScore = w_R * normRisk + w_U * normUncertainty + w_D * normDistance
     */
    public double calculatePriorityScore(double normRisk, double normUncertainty, double normDistance) {
        double raw = (priorityConfig.getRiskWeight() * normRisk)
                + (priorityConfig.getUncertaintyWeight() * normUncertainty)
                + (priorityConfig.getDistanceWeight() * normDistance);
        return round4(clamp(raw, 0.0, 1.0));
    }

    /**
     * Classifies priority level based on configurable percentage thresholds:
     * LOW:    0 - (mediumThreshold - 1)
     * MEDIUM: mediumThreshold - (highThreshold - 1)
     * HIGH:   highThreshold - 100
     */
    public MonitoringPriority classifyPriority(int priorityScorePercent) {
        if (priorityScorePercent >= priorityConfig.getHighThreshold()) {
            return MonitoringPriority.HIGH;
        } else if (priorityScorePercent >= priorityConfig.getMediumThreshold()) {
            return MonitoringPriority.MEDIUM;
        } else {
            return MonitoringPriority.LOW;
        }
    }

    public MonitoringPriorityConfig getPriorityConfig() {
        return this.priorityConfig;
    }

    public static double clamp(double val, double min, double max) {
        if (Double.isNaN(val)) {
            throw new IllegalArgumentException("Cannot clamp NaN value");
        }
        return Math.max(min, Math.min(max, val));
    }

    public static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    /**
     * Resolves the authoritative F8 monitoring recommendation for an individual H3 cell (F8-P4).
     * Reuses P3 priority output directly without duplicating priority calculations.
     *
     * @param h3Index 15-character Uber H3 cell identifier
     * @param cityId  City identifier
     * @return MonitoringRecommendationResponse containing full audit lineage, priority factors,
     *         and deterministic recommendation guidance
     */
    public MonitoringRecommendationResponse getRecommendation(String h3Index, UUID cityId) {
        if (!h3Service.validateH3Index(h3Index)) {
            throw new IllegalArgumentException("Invalid H3 index: " + h3Index);
        }
        if (cityId == null) {
            throw new IllegalArgumentException("City ID must not be null");
        }
        if (cityRepository != null && !cityRepository.existsById(cityId)) {
            throw new ResourceNotFoundException("City not found with id: " + cityId);
        }

        // Reuse P3 priority calculation directly
        MonitoringPriorityResponse p3 = getMonitoringPriority(h3Index, cityId);

        MonitoringRecommendationType type = determineRecommendationType(p3.priorityLevel(), p3.monitoringCoverageGapFlag());
        String recText = generateRecommendationText(type);
        String rationale = generateRationale(p3, type);

        return MonitoringRecommendationResponse.from(p3, type, recText, rationale);
    }

    /**
     * Retrieves monitoring recommendations for all observed H3 cells across a city (F8-P4).
     * Consumes real F3/F4 records, skips unobserved cells safely without fake scores,
     * and orders by priorityScorePercent descending (with h3Index tie-breaker).
     *
     * @param cityId City identifier
     * @return Deterministically ordered list of monitoring recommendations
     */
    public List<MonitoringRecommendationResponse> getCityRecommendations(UUID cityId) {
        if (cityId == null) {
            throw new IllegalArgumentException("City ID must not be null");
        }
        if (cityRepository != null && !cityRepository.existsById(cityId)) {
            throw new ResourceNotFoundException("City not found with id: " + cityId);
        }
        if (hotspotRepository == null || forecastRepository == null) {
            return List.of();
        }

        List<HotspotPrediction> predictions = hotspotRepository.findLatestByCityId(cityId);
        if (predictions == null || predictions.isEmpty()) {
            return List.of();
        }

        List<MonitoringRecommendationResponse> recommendations = new ArrayList<>();
        for (HotspotPrediction pred : predictions) {
            String h3Index = pred.getH3Index();
            if (h3Index == null || !h3Service.validateH3Index(h3Index)) {
                continue;
            }
            try {
                MonitoringRecommendationResponse rec = getRecommendation(h3Index, cityId);
                recommendations.add(rec);
            } catch (ResourceNotFoundException e) {
                // Missing forecast or observation inputs - skip safely without fake values
                log.debug("Skipping H3 cell {} for city recommendations due to missing data: {}", h3Index, e.getMessage());
            } catch (Exception e) {
                log.warn("Error computing recommendation for H3 cell {}: {}", h3Index, e.getMessage());
            }
        }

        // Operational API ordering:
        // 1. priorityScorePercent descending
        // 2. h3Index ascending as deterministic tie-breaker
        recommendations.sort(
                Comparator.comparing(MonitoringRecommendationResponse::priorityScorePercent, Comparator.reverseOrder())
                        .thenComparing(MonitoringRecommendationResponse::h3Index)
        );

        return recommendations;
    }

    /**
     * Deterministically maps priority classification and coverage gap state to recommendation type.
     */
    public MonitoringRecommendationType determineRecommendationType(MonitoringPriority priorityLevel, Integer coverageGapFlag) {
        if (priorityLevel == null) {
            return MonitoringRecommendationType.ROUTINE_MONITORING;
        }
        return switch (priorityLevel) {
            case LOW -> MonitoringRecommendationType.ROUTINE_MONITORING;
            case MEDIUM -> MonitoringRecommendationType.TARGETED_MONITORING;
            case HIGH -> (coverageGapFlag != null && coverageGapFlag == 1)
                    ? MonitoringRecommendationType.MOBILE_SENSOR_RECOMMENDED
                    : MonitoringRecommendationType.FIELD_VERIFICATION_RECOMMENDED;
        };
    }

    /**
     * Generates concise, factual, non-alarmist recommendation text.
     */
    public String generateRecommendationText(MonitoringRecommendationType type) {
        if (type == null) {
            return "Continue routine monitoring for this H3 cell.";
        }
        return switch (type) {
            case ROUTINE_MONITORING ->
                    "Continue routine monitoring for this H3 cell.";
            case TARGETED_MONITORING ->
                    "Prioritize targeted monitoring and closer observation for this H3 cell.";
            case MOBILE_SENSOR_RECOMMENDED ->
                    "Consider deploying additional mobile monitoring in this unobserved H3 cell.";
            case FIELD_VERIFICATION_RECOMMENDED ->
                    "Consider targeted field verification and additional observation for this H3 cell.";
        };
    }

    /**
     * Builds structured, factual rationale from audited P3 inputs without generative AI.
     */
    public String generateRationale(MonitoringPriorityResponse p3, MonitoringRecommendationType type) {
        if (p3 == null) {
            return "Insufficient priority data.";
        }

        String riskInfo = String.format("Risk: %s (%.2f)",
                p3.riskLevel() != null ? p3.riskLevel() : "UNKNOWN",
                p3.riskScore() != null ? p3.riskScore() : 0.0);

        String uncertaintyInfo = String.format("Forecast uncertainty: %.1f ug/m3 interval width (normalized: %.2f)",
                p3.uncertaintyIntervalWidth() != null ? p3.uncertaintyIntervalWidth() : 0.0,
                p3.normalizedUncertainty() != null ? p3.normalizedUncertainty() : 0.0);

        String coverageInfo;
        if (p3.nearestStationCode() != null && p3.nearestStationDistanceKm() != null) {
            coverageInfo = String.format("Monitoring coverage: Nearest station %s (%s) at %.2f km (coverage gap: %s)",
                    p3.nearestStationCode(),
                    p3.nearestStationName() != null ? p3.nearestStationName() : "Station",
                    p3.nearestStationDistanceKm(),
                    (p3.monitoringCoverageGapFlag() != null && p3.monitoringCoverageGapFlag() == 1));
        } else {
            coverageInfo = "Monitoring coverage: No active monitoring stations in city (coverage gap: true)";
        }

        String priorityInfo = String.format("Priority: %s (%d/100)",
                p3.priorityLevel() != null ? p3.priorityLevel().name() : "LOW",
                p3.priorityScorePercent() != null ? p3.priorityScorePercent() : 0);

        String actionInfo = switch (type) {
            case ROUTINE_MONITORING -> "Recommendation: Routine observation sufficient under current conditions.";
            case TARGETED_MONITORING -> "Recommendation: Closer observation recommended due to moderate risk or uncertainty.";
            case MOBILE_SENSOR_RECOMMENDED -> "Recommendation: Additional mobile sensor deployment recommended due to high priority and observation coverage gap.";
            case FIELD_VERIFICATION_RECOMMENDED -> "Recommendation: Field verification recommended due to high priority within observed station range.";
        };

        return String.join(" | ", riskInfo, uncertaintyInfo, coverageInfo, priorityInfo, actionInfo);
    }

    public double calculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        return FeatureEngineeringService.calculateHaversineDistanceKm(lat1, lon1, lat2, lon2);
    }
}
