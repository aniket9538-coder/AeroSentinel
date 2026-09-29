package com.aerosentinel.forecast.feature;

import java.util.Collections;
import java.util.List;

/**
 * Structured exception thrown when a forecast feature vector fails validation.
 * Error code: FORECAST_FEATURES_INVALID
 */
public class ForecastFeaturesInvalidException extends RuntimeException {

    public static final String ERROR_CODE = "FORECAST_FEATURES_INVALID";

    private final String errorCode;
    private final String h3Index;
    private final List<String> validationErrors;

    public ForecastFeaturesInvalidException(String message, String h3Index, List<String> validationErrors) {
        super(message + (validationErrors != null && !validationErrors.isEmpty() ? " Violations: " + validationErrors : ""));
        this.errorCode = ERROR_CODE;
        this.h3Index = h3Index;
        this.validationErrors = validationErrors != null ? Collections.unmodifiableList(validationErrors) : Collections.emptyList();
    }

    public ForecastFeaturesInvalidException(String message, String h3Index) {
        this(message, h3Index, Collections.emptyList());
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getH3Index() {
        return h3Index;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
