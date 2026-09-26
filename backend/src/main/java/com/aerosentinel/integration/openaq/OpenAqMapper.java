package com.aerosentinel.integration.openaq;

import com.aerosentinel.integration.provider.ProviderObservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class OpenAqMapper {

    private static final Logger log = LoggerFactory.getLogger(OpenAqMapper.class);
    public static final String SOURCE_OPENAQ = "OPENAQ";
    public static final int PM25_PARAMETER_ID = 2;
    public static final String PM25_NAME = "pm25";

    public List<ProviderObservation> mapResponse(OpenAqResponse response, String fallbackLocationId) {
        List<ProviderObservation> observations = new ArrayList<>();
        if (response == null || response.getResults() == null) {
            return observations;
        }

        for (OpenAqResponse.OpenAqMeasurement measurement : response.getResults()) {
            mapMeasurement(measurement, fallbackLocationId).ifPresent(observations::add);
        }
        return observations;
    }

    public Optional<ProviderObservation> mapMeasurement(OpenAqResponse.OpenAqMeasurement measurement, String fallbackLocationId) {
        if (measurement == null) {
            log.warn("Rejection: measurement object is null");
            return Optional.empty();
        }

        // 1. Verify Parameter is PM2.5
        if (!isPm25Parameter(measurement.getParameter())) {
            String paramName = measurement.getParameter() != null ? measurement.getParameter().getName() : "null";
            log.debug("Skipping non-PM2.5 measurement: parameter={}", paramName);
            return Optional.empty();
        }

        // 2. Validate PM2.5 value
        Double pm25 = measurement.getValue();
        if (pm25 == null || Double.isNaN(pm25) || Double.isInfinite(pm25) || pm25 < 0.0) {
            log.warn("Rejection REJECTED_INVALID_PM25: invalid PM2.5 value={}", pm25);
            return Optional.empty();
        }

        // 3. Validate and Parse Timestamp
        Instant observedAt = parseTimestamp(measurement.getDatetime());
        if (observedAt == null) {
            log.warn("Rejection REJECTED_INVALID_TIMESTAMP: missing or unparseable timestamp");
            return Optional.empty();
        }

        // 4. Resolve Provider Station / Location ID
        String stationId = null;
        if (measurement.getLocationsId() != null) {
            stationId = String.valueOf(measurement.getLocationsId());
        } else if (fallbackLocationId != null && !fallbackLocationId.isBlank()) {
            stationId = fallbackLocationId;
        }

        // 5. Extract Coordinates
        Double lat = null;
        Double lon = null;
        if (measurement.getCoordinates() != null) {
            lat = measurement.getCoordinates().getLatitude();
            lon = measurement.getCoordinates().getLongitude();
        }

        // 6. Build Normalized ProviderObservation
        ProviderObservation observation = new ProviderObservation();
        observation.setProviderStationId(stationId);
        observation.setLatitude(lat);
        observation.setLongitude(lon);
        observation.setObservedAt(observedAt);
        observation.setPm25(pm25);
        observation.setSource(SOURCE_OPENAQ);
        observation.setQuality("VALID");
        observation.setRawParameter(PM25_NAME);
        if (measurement.getParameter() != null) {
            observation.setUnit(measurement.getParameter().getUnits());
        }

        return Optional.of(observation);
    }

    public boolean isPm25Parameter(OpenAqResponse.OpenAqParameter parameter) {
        if (parameter == null) {
            return false;
        }
        if (parameter.getId() != null && parameter.getId() == PM25_PARAMETER_ID) {
            return true;
        }
        if (parameter.getName() != null && PM25_NAME.equalsIgnoreCase(parameter.getName().trim())) {
            return true;
        }
        if (parameter.getDisplayName() != null && "pm2.5".equalsIgnoreCase(parameter.getDisplayName().trim())) {
            return true;
        }
        return false;
    }

    public Instant parseTimestamp(OpenAqResponse.OpenAqDateTime datetime) {
        if (datetime == null) {
            return null;
        }
        if (datetime.getUtc() != null && !datetime.getUtc().isBlank()) {
            try {
                return Instant.parse(datetime.getUtc().trim());
            } catch (DateTimeParseException e) {
                try {
                    return OffsetDateTime.parse(datetime.getUtc().trim()).toInstant();
                } catch (DateTimeParseException ignored) {}
            }
        }
        if (datetime.getLocal() != null && !datetime.getLocal().isBlank()) {
            try {
                return OffsetDateTime.parse(datetime.getLocal().trim()).toInstant();
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }
}
