package com.aerosentinel.integration.weather;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task for periodic real weather ingestion.
 * Defaults to 15 minutes (900,000 ms), configurable via app.weather.ingestion.interval-ms.
 * Disabled in tests via app.weather.ingestion.enabled=false.
 */
@Component
@ConditionalOnProperty(name = "app.weather.ingestion.enabled", havingValue = "true", matchIfMissing = true)
public class WeatherIngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(WeatherIngestionScheduler.class);

    private final WeatherIngestionService ingestionService;

    public WeatherIngestionScheduler(WeatherIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Scheduled(fixedDelayString = "${app.weather.ingestion.interval-ms:900000}", initialDelay = 15000)
    public void runScheduledIngestion() {
        log.info("Triggering scheduled weather ingestion from Open-Meteo...");
        try {
            WeatherIngestionService.WeatherIngestionSummary summary = ingestionService.ingestAllStationsWeather();
            log.info("Scheduled weather ingestion completed: inserted={}, duplicates={}, rejected={}",
                    summary.getInserted(), summary.getDuplicates(), summary.getRejected());
        } catch (Exception e) {
            log.error("Scheduled weather ingestion failed", e);
        }
    }
}
