package com.aerosentinel.forecast;

/**
 * Base exception for F4 Forecast operations.
 */
public class ForecastException extends RuntimeException {
    private final String errorCode;

    public ForecastException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ForecastException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static class ParentNotFound extends ForecastException {
        public ParentNotFound(String message) {
            super("FORECAST_PARENT_NOT_FOUND", message);
        }
    }

    public static class ParentContextMismatch extends ForecastException {
        public ParentContextMismatch(String message) {
            super("FORECAST_PARENT_CONTEXT_MISMATCH", message);
        }
    }

    public static class NotFound extends ForecastException {
        public NotFound(String message) {
            super("FORECAST_NOT_FOUND", message);
        }
    }

    public static class ValidationFailed extends ForecastException {
        public ValidationFailed(String message) {
            super("FORECAST_VALIDATION_ERROR", message);
        }
    }

    public static class AiUnavailable extends ForecastException {
        public AiUnavailable(String message) {
            super("FORECAST_AI_UNAVAILABLE", message);
        }

        public AiUnavailable(String message, Throwable cause) {
            super("FORECAST_AI_UNAVAILABLE", message, cause);
        }
    }

    public static class AiTimeout extends ForecastException {
        public AiTimeout(String message) {
            super("FORECAST_AI_TIMEOUT", message);
        }

        public AiTimeout(String message, Throwable cause) {
            super("FORECAST_AI_TIMEOUT", message, cause);
        }
    }

    public static class PersistenceFailed extends ForecastException {
        public PersistenceFailed(String message, Throwable cause) {
            super("FORECAST_PERSISTENCE_FAILED", message, cause);
        }
    }
}
