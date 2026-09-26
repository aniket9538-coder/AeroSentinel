package com.aerosentinel.integration.weather;

import com.aerosentinel.weather.WeatherObservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Maps raw Open-Meteo API response to domain WeatherObservation entities with
 * strict meteorological validation and Asia/Kolkata to UTC timestamp parsing.
 */
@Component
public class WeatherMapper {

    private static final Logger log = LoggerFactory.getLogger(WeatherMapper.class);
    public static final String SOURCE_OPEN_METEO = "OPEN_METEO";
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Kolkata");

    /**
     * Maps an Open-Meteo response to a list of validated WeatherObservation entities.
     *
     * @param response the Open-Meteo response
     * @param cityId   the associated city ID
     * @param targetLat the target latitude (from database station/city)
     * @param targetLon the target longitude (from database station/city)
     * @return list of valid WeatherObservation entities
     */
    public List<WeatherObservation> mapToWeatherObservations(WeatherProviderResponse response,
                                                             UUID cityId,
                                                             double targetLat,
                                                             double targetLon) {
        if (response == null || response.getHourly() == null) {
            log.warn("Cannot map weather: null response or missing hourly section for city {}", cityId);
            return Collections.emptyList();
        }

        WeatherProviderResponse.HourlyData hourly = response.getHourly();
        List<String> times = hourly.getTime();
        if (times == null || times.isEmpty()) {
            log.warn("Cannot map weather: empty time series for city {}", cityId);
            return Collections.emptyList();
        }

        List<Double> temps = hourly.getTemperature2m();
        List<Double> humidities = hourly.getRelativeHumidity2m();
        List<Double> windSpeeds = hourly.getWindSpeed10m();
        List<Double> windDirs = hourly.getWindDirection10m();
        List<Double> precipitations = hourly.getPrecipitation();
        List<Double> pressures = hourly.getSurfacePressure();

        ZoneId zone = DEFAULT_ZONE;
        if (response.getTimezone() != null && !response.getTimezone().isBlank()) {
            try {
                zone = ZoneId.of(response.getTimezone().trim());
            } catch (Exception e) {
                log.warn("Unknown timezone '{}' from provider, defaulting to Asia/Kolkata", response.getTimezone());
                zone = DEFAULT_ZONE;
            }
        }

        List<WeatherObservation> result = new ArrayList<>();
        int size = times.size();

        for (int i = 0; i < size; i++) {
            String timeStr = times.get(i);
            Instant observedAt;
            try {
                // Open-Meteo returns ISO local times like "2026-09-24T14:00"
                observedAt = LocalDateTime.parse(timeStr).atZone(zone).toInstant();
            } catch (DateTimeParseException e) {
                log.warn("Rejected weather point: unparseable timestamp '{}' for city {} at index {}", timeStr, cityId, i);
                continue;
            }

            Double temp = temps != null && i < temps.size() ? temps.get(i) : null;
            Double humidity = humidities != null && i < humidities.size() ? humidities.get(i) : null;
            Double windSpeed = windSpeeds != null && i < windSpeeds.size() ? windSpeeds.get(i) : null;
            Double windDir = windDirs != null && i < windDirs.size() ? windDirs.get(i) : null;
            Double rainfall = precipitations != null && i < precipitations.size() ? precipitations.get(i) : null;
            Double pressure = pressures != null && i < pressures.size() ? pressures.get(i) : null;

            // Strict Meteorological Validation
            if (temp == null || Double.isNaN(temp) || Double.isInfinite(temp)) {
                log.warn("Rejected weather point: non-finite or missing temperature at {} for city {}", timeStr, cityId);
                continue;
            }

            if (humidity != null && (Double.isNaN(humidity) || humidity < 0.0 || humidity > 100.0)) {
                log.warn("Rejected weather point: invalid relative humidity {}% at {} for city {}", humidity, timeStr, cityId);
                continue;
            }

            if (windSpeed != null && (Double.isNaN(windSpeed) || windSpeed < 0.0)) {
                log.warn("Rejected weather point: negative wind speed {} km/h at {} for city {}", windSpeed, timeStr, cityId);
                continue;
            }

            if (windDir != null && (Double.isNaN(windDir) || windDir < 0.0 || windDir > 360.0)) {
                log.warn("Rejected weather point: invalid wind direction {}° at {} for city {}", windDir, timeStr, cityId);
                continue;
            }

            if (rainfall != null && (Double.isNaN(rainfall) || rainfall < 0.0)) {
                log.warn("Rejected weather point: negative rainfall {} mm at {} for city {}", rainfall, timeStr, cityId);
                continue;
            }

            if (pressure != null && (Double.isNaN(pressure) || pressure < 300.0 || pressure > 1150.0)) {
                log.warn("Rejected weather pressure: unphysical barometric pressure {} hPa at {} for city {}", pressure, timeStr, cityId);
                pressure = null;
            }

            WeatherObservation obs = new WeatherObservation();
            obs.setCityId(cityId);
            obs.setLatitude(targetLat);
            obs.setLongitude(targetLon);
            obs.setObservedAt(observedAt);
            obs.setTemperature(temp);
            obs.setHumidity(humidity);
            obs.setWindSpeed(windSpeed);
            obs.setWindDirection(windDir);
            obs.setRainfall(rainfall);
            obs.setPressure(pressure);
            obs.setSource(SOURCE_OPEN_METEO);
            obs.setCreatedAt(Instant.now());

            result.add(obs);
        }

        return result;
    }
}
