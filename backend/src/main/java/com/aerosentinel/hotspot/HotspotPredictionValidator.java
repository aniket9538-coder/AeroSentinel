package com.aerosentinel.hotspot;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Strict validator for Hotspot Predictions prior to persistence.
 * Prevents out-of-range scores, missing metadata, future timestamps, or orphaned predictions.
 */
@Component
public class HotspotPredictionValidator {

    public void validate(
            UUID cityId,
            String h3Index,
            Instant predictedAt,
            Double riskScore,
            String riskLevel,
            Double confidence,
            String modelVersion,
            UUID featureSnapshotId
    ) {
        if (cityId == null) {
            throw new IllegalArgumentException("cityId must not be null");
        }

        if (h3Index == null || h3Index.trim().length() < 15) {
            throw new IllegalArgumentException("h3Index must be a valid H3 identifier: " + h3Index);
        }

        if (predictedAt == null) {
            throw new IllegalArgumentException("predictedAt timestamp must not be null");
        }

        // Allow up to 5 minutes clock skew, but reject future predictions
        Instant maxAllowedTime = Instant.now().plus(5, ChronoUnit.MINUTES);
        if (predictedAt.isAfter(maxAllowedTime)) {
            throw new IllegalArgumentException("predictedAt cannot be in the future: " + predictedAt);
        }

        if (riskScore == null || Double.isNaN(riskScore) || riskScore < 0.0 || riskScore > 1.0) {
            throw new IllegalArgumentException("riskScore must be strictly within [0.0, 1.0]: " + riskScore);
        }

        if (riskLevel == null || riskLevel.trim().isEmpty()) {
            throw new IllegalArgumentException("riskLevel must not be empty");
        }
        try {
            HotspotRiskLevel.valueOf(riskLevel.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid riskLevel enum value: " + riskLevel);
        }

        if (confidence == null || Double.isNaN(confidence) || confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be strictly within [0.0, 1.0]: " + confidence);
        }

        if (modelVersion == null || modelVersion.trim().isEmpty()) {
            throw new IllegalArgumentException("modelVersion must not be empty");
        }

        if (featureSnapshotId == null) {
            throw new IllegalArgumentException("featureSnapshotId must be non-null to preserve feature provenance");
        }
    }
}
