package com.aerosentinel.alert;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.alert")
public class AlertProperties {

    private Double highRiskThreshold = 75.0;
    private Double mediumRiskThreshold = 50.0;
    private Double minConfidence = 0.70;

    public Double getHighRiskThreshold() {
        return highRiskThreshold;
    }

    public void setHighRiskThreshold(Double highRiskThreshold) {
        this.highRiskThreshold = highRiskThreshold;
    }

    public Double getMediumRiskThreshold() {
        return mediumRiskThreshold;
    }

    public void setMediumRiskThreshold(Double mediumRiskThreshold) {
        this.mediumRiskThreshold = mediumRiskThreshold;
    }

    public Double getMinConfidence() {
        return minConfidence;
    }

    public void setMinConfidence(Double minConfidence) {
        this.minConfidence = minConfidence;
    }
}