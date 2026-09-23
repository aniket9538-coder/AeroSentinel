package com.aerosentinel.util;

import com.aerosentinel.exception.ValidationException;

public class ValidationUtils {

    public static void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || latitude < -90.0 || latitude > 90.0) {
            throw new ValidationException("Latitude must be between -90.0 and 90.0");
        }
        if (longitude == null || longitude < -180.0 || longitude > 180.0) {
            throw new ValidationException("Longitude must be between -180.0 and 180.0");
        }
    }

    public static void validatePm25(Double pm25) {
        if (pm25 != null && (pm25 < 0.0 || pm25 > 1000.0)) {
            throw new ValidationException("PM2.5 value must be between 0.0 and 1000.0 µg/m³");
        }
    }
}
