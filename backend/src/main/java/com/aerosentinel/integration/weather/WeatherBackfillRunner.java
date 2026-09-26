package com.aerosentinel.integration.weather;

import com.aerosentinel.weather.WeatherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Startup runner ensuring that real recent weather history (~24 hours) is populated
 * for Pune, Mumbai, and Delhi monitoring stations upon initial startup.
 * Idempotent: skips if weather observations already exist or if disabled.
 */
@Component
public class WeatherBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WeatherBackfillRunner.class);

    private final WeatherIngestionService ingestionService;
    private final WeatherRepository weatherRepository;

    @Value("${app.weather.backfill.enabled:true}")
    private boolean enabled;

    public WeatherBackfillRunner(WeatherIngestionService ingestionService,
                                 WeatherRepository weatherRepository) {
        this.ingestionService = ingestionService;
        this.weatherRepository = weatherRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("WeatherBackfillRunner is disabled by configuration");
            return;
        }

        long existingCount = weatherRepository.count();
        if (existingCount == 0) {
            log.info("Initial weather_observations table is empty. Running initial Open-Meteo history population for all cities...");
            try {
                WeatherIngestionService.WeatherIngestionSummary summary = ingestionService.ingestAllStationsWeather();
                log.info("WeatherBackfillRunner completed: inserted {} real weather observations across {} stations",
                        summary.getInserted(), summary.getStationsProcessed());
            } catch (Exception e) {
                log.error("WeatherBackfillRunner failed during initial weather population", e);
            }
        } else {
            log.info("Weather observations already present in database (count={}). Skipping initial history runner.", existingCount);
        }
    }
}
