package com.aerosentinel.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonitoringPriorityServiceTest {

    private MonitoringProperties properties;
    private MonitoringPriorityService priorityService;

    @BeforeEach
    void setUp() {
        properties = new MonitoringProperties();
        properties.setHighRiskThreshold(70.0);
        properties.setMediumRiskThreshold(40.0);
        properties.setHighUncertaintyThreshold(0.25);
        properties.setFarStationThresholdKm(5.0);
        properties.setPeripheralStationThresholdKm(8.0);

        priorityService = new MonitoringPriorityService(properties);
    }

    @Test
    @DisplayName("HIGH Priority & Mobile Sensor: High risk + high uncertainty + far station")
    void testHighPriorityWithMobileSensorRecommendation() {
        var result = priorityService.evaluate(85.0, 0.35, 7.2);

        assertThat(result.priority()).isEqualTo(MonitoringPriority.HIGH);
        assertThat(result.recommendationType()).isEqualTo(RecommendationType.MOBILE_SENSOR);
        assertThat(result.recommendationReason())
                .contains("High model risk combined with elevated uncertainty and weak fixed-station coverage");
        assertThat(result.recommendationReason()).doesNotContain("pollution is high because");
    }

    @Test
    @DisplayName("HIGH Priority & Field Verification: High risk with either uncertainty or distance gap")
    void testHighPriorityWithFieldVerification() {
        var result = priorityService.evaluate(78.0, 0.15, 6.0);

        assertThat(result.priority()).isEqualTo(MonitoringPriority.HIGH);
        assertThat(result.recommendationType()).isEqualTo(RecommendationType.FIELD_VERIFICATION);
        assertThat(result.recommendationReason()).contains("field verification recommended");
    }

    @Test
    @DisplayName("MEDIUM Priority: Moderate risk within normal station range")
    void testMediumPriorityWithModerateRisk() {
        var result = priorityService.evaluate(55.0, 0.10, 2.1);

        assertThat(result.priority()).isEqualTo(MonitoringPriority.MEDIUM);
        assertThat(result.recommendationType()).isEqualTo(RecommendationType.MONITORING_REVIEW);
        assertThat(result.recommendationReason()).contains("Periodic surveillance review recommended");
    }

    @Test
    @DisplayName("MEDIUM Priority: Low risk but peripheral station coverage")
    void testMediumPriorityWithPeripheralStationCoverage() {
        var result = priorityService.evaluate(25.0, 0.10, 9.5);

        assertThat(result.priority()).isEqualTo(MonitoringPriority.MEDIUM);
        assertThat(result.recommendationType()).isEqualTo(RecommendationType.MONITORING_REVIEW);
    }

    @Test
    @DisplayName("LOW Priority: Low risk within station coverage perimeter")
    void testLowPriorityWithinStationRange() {
        var result = priorityService.evaluate(20.0, 0.10, 1.5);

        assertThat(result.priority()).isEqualTo(MonitoringPriority.LOW);
        assertThat(result.recommendationType()).isEqualTo(RecommendationType.NONE);
        assertThat(result.recommendationReason()).contains("within acceptable fixed monitoring station perimeter");
    }

    @Test
    @DisplayName("Edge Case: Zero active stations in city (distance = Double.MAX_VALUE) handled cleanly")
    void testZeroStationsFallbackDistance() {
        var result = priorityService.evaluate(75.0, 0.30, Double.MAX_VALUE);

        assertThat(result.priority()).isEqualTo(MonitoringPriority.HIGH);
        assertThat(result.recommendationType()).isEqualTo(RecommendationType.MOBILE_SENSOR);
    }

    @Test
    @DisplayName("Validation: Negative distance throws IllegalArgumentException")
    void testNegativeDistanceThrowsException() {
        assertThatThrownBy(() -> priorityService.evaluate(50.0, 0.20, -1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Distance cannot be negative");
    }

    @Test
    @DisplayName("Validation: Invalid risk score (> 100 or < 0) throws IllegalArgumentException")
    void testInvalidRiskThrowsException() {
        assertThatThrownBy(() -> priorityService.evaluate(105.0, 0.20, 2.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Risk score must be between 0.0 and 100.0");

        assertThatThrownBy(() -> priorityService.evaluate(-5.0, 0.20, 2.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Risk score must be between 0.0 and 100.0");
    }

    @Test
    @DisplayName("Validation: Invalid uncertainty (> 1 or < 0) throws IllegalArgumentException")
    void testInvalidUncertaintyThrowsException() {
        assertThatThrownBy(() -> priorityService.evaluate(50.0, 1.5, 2.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Uncertainty must be between 0.0 and 1.0");

        assertThatThrownBy(() -> priorityService.evaluate(50.0, -0.1, 2.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Uncertainty must be between 0.0 and 1.0");
    }
}
