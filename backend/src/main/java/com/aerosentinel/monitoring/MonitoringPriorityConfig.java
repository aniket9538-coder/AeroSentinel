package com.aerosentinel.monitoring;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration parameters for F8 Monitoring Priority calculation.
 * Exposes configurable weights, normalization thresholds, and priority classifications.
 *
 * NOTE: Default weights (0.45 / 0.30 / 0.25) and classification thresholds (40 / 70)
 * are F8 MVP implementation choices for decision support, not externally mandated PRD constants.
 */
@Component
public class MonitoringPriorityConfig {

    private double riskWeight;
    private double uncertaintyWeight;
    private double distanceWeight;
    private double uncertaintyMaxIntervalWidth;
    private double distanceMaxKm;
    private int mediumThreshold;
    private int highThreshold;

    public MonitoringPriorityConfig(
            @Value("${app.monitoring.priority.risk-weight:0.45}") double riskWeight,
            @Value("${app.monitoring.priority.uncertainty-weight:0.30}") double uncertaintyWeight,
            @Value("${app.monitoring.priority.distance-weight:0.25}") double distanceWeight,
            @Value("${app.monitoring.priority.uncertainty-max-interval-width:20.0}") double uncertaintyMaxIntervalWidth,
            @Value("${app.monitoring.priority.distance-max-km:20.0}") double distanceMaxKm,
            @Value("${app.monitoring.priority.medium-threshold:40}") int mediumThreshold,
            @Value("${app.monitoring.priority.high-threshold:70}") int highThreshold
    ) {
        this.riskWeight = riskWeight;
        this.uncertaintyWeight = uncertaintyWeight;
        this.distanceWeight = distanceWeight;
        this.uncertaintyMaxIntervalWidth = uncertaintyMaxIntervalWidth;
        this.distanceMaxKm = distanceMaxKm;
        this.mediumThreshold = mediumThreshold;
        this.highThreshold = highThreshold;
        validate();
    }

    public MonitoringPriorityConfig() {
        this(0.45, 0.30, 0.25, 20.0, 20.0, 40, 70);
    }

    @PostConstruct
    public void validate() {
        double weightSum = riskWeight + uncertaintyWeight + distanceWeight;
        if (Math.abs(weightSum - 1.0) > 1e-4) {
            throw new IllegalArgumentException(
                    "Priority weights must sum to 1.0. Found: " + weightSum +
                    " (risk=" + riskWeight + ", uncertainty=" + uncertaintyWeight + ", distance=" + distanceWeight + ")"
            );
        }
        if (riskWeight < 0 || uncertaintyWeight < 0 || distanceWeight < 0) {
            throw new IllegalArgumentException("Priority weights must be non-negative.");
        }
        if (uncertaintyMaxIntervalWidth <= 0) {
            throw new IllegalArgumentException("uncertaintyMaxIntervalWidth must be greater than 0.");
        }
        if (distanceMaxKm <= 0) {
            throw new IllegalArgumentException("distanceMaxKm must be greater than 0.");
        }
        if (mediumThreshold < 0 || highThreshold > 100 || mediumThreshold >= highThreshold) {
            throw new IllegalArgumentException(
                    "Thresholds must satisfy 0 <= mediumThreshold < highThreshold <= 100. " +
                    "Found: medium=" + mediumThreshold + ", high=" + highThreshold
            );
        }
    }

    public double getRiskWeight() { return riskWeight; }
    public void setRiskWeight(double riskWeight) { this.riskWeight = riskWeight; }

    public double getUncertaintyWeight() { return uncertaintyWeight; }
    public void setUncertaintyWeight(double uncertaintyWeight) { this.uncertaintyWeight = uncertaintyWeight; }

    public double getDistanceWeight() { return distanceWeight; }
    public void setDistanceWeight(double distanceWeight) { this.distanceWeight = distanceWeight; }

    public double getUncertaintyMaxIntervalWidth() { return uncertaintyMaxIntervalWidth; }
    public void setUncertaintyMaxIntervalWidth(double uncertaintyMaxIntervalWidth) { this.uncertaintyMaxIntervalWidth = uncertaintyMaxIntervalWidth; }

    public double getDistanceMaxKm() { return distanceMaxKm; }
    public void setDistanceMaxKm(double distanceMaxKm) { this.distanceMaxKm = distanceMaxKm; }

    public int getMediumThreshold() { return mediumThreshold; }
    public void setMediumThreshold(int mediumThreshold) { this.mediumThreshold = mediumThreshold; }

    public int getHighThreshold() { return highThreshold; }
    public void setHighThreshold(int highThreshold) { this.highThreshold = highThreshold; }
}
