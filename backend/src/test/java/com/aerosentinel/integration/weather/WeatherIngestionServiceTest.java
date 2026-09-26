package com.aerosentinel.integration.weather;

import com.aerosentinel.grid.GridCell;
import com.aerosentinel.grid.GridService;
import com.aerosentinel.sensor.MonitoringStation;
import com.aerosentinel.sensor.SensorRepository;
import com.aerosentinel.spatial.H3Service;
import com.aerosentinel.weather.WeatherObservation;
import com.aerosentinel.weather.WeatherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherIngestionServiceTest {

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private WeatherClient weatherClient;

    @Mock
    private WeatherMapper weatherMapper;

    @Mock
    private H3Service h3Service;

    @Mock
    private GridService gridService;

    @Mock
    private WeatherRepository weatherRepository;

    private WeatherIngestionService ingestionService;

    private static final UUID PUNE_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final String PUNE_H3 = "88608850e5fffff";
    private MonitoringStation puneStation;

    @BeforeEach
    void setUp() {
        ingestionService = new WeatherIngestionService(
                sensorRepository, weatherClient, weatherMapper, h3Service, gridService, weatherRepository
        );

        puneStation = new MonitoringStation();
        puneStation.setStationCode("PUN-001");
        puneStation.setName("Shivajinagar");
        puneStation.setCityId(PUNE_CITY_ID);
        puneStation.setLatitude(18.5314);
        puneStation.setLongitude(73.8446);
    }

    @Test
    @DisplayName("1. Successful station ingestion calculates H3, ensures grid cell, and saves observations")
    void testSuccessfulStationIngestion() {
        when(h3Service.coordinatesToH3(18.5314, 73.8446)).thenReturn(PUNE_H3);
        when(gridService.getOrCreateGridCell(PUNE_H3, PUNE_CITY_ID)).thenReturn(new GridCell());

        WeatherProviderResponse fakeResp = new WeatherProviderResponse();
        when(weatherClient.fetchHourlyWeather(18.5314, 73.8446, 1, 1)).thenReturn(Optional.of(fakeResp));

        Instant now = Instant.now();
        WeatherObservation obs1 = new WeatherObservation();
        obs1.setCityId(PUNE_CITY_ID);
        obs1.setObservedAt(now.minusSeconds(3600));
        obs1.setTemperature(26.0);

        WeatherObservation obs2 = new WeatherObservation();
        obs2.setCityId(PUNE_CITY_ID);
        obs2.setObservedAt(now);
        obs2.setTemperature(27.0);

        when(weatherMapper.mapToWeatherObservations(fakeResp, PUNE_CITY_ID, 18.5314, 73.8446))
                .thenReturn(List.of(obs1, obs2));

        when(weatherRepository.existsByH3IndexAndObservedAt(eq(PUNE_H3), any())).thenReturn(false);

        WeatherIngestionService.WeatherIngestionSummary summary = ingestionService.ingestStationWeather(puneStation);

        assertThat(summary.getStationsProcessed()).isEqualTo(1);
        assertThat(summary.getFetched()).isEqualTo(2);
        assertThat(summary.getInserted()).isEqualTo(2);
        assertThat(summary.getDuplicates()).isEqualTo(0);

        verify(weatherRepository, times(2)).save(any(WeatherObservation.class));
        verify(gridService).getOrCreateGridCell(PUNE_H3, PUNE_CITY_ID);
    }

    @Test
    @DisplayName("2. Existing observation timestamp is skipped as duplicate without re-saving")
    void testDuplicateObservationSkipped() {
        when(h3Service.coordinatesToH3(18.5314, 73.8446)).thenReturn(PUNE_H3);
        when(gridService.getOrCreateGridCell(PUNE_H3, PUNE_CITY_ID)).thenReturn(new GridCell());

        WeatherProviderResponse fakeResp = new WeatherProviderResponse();
        when(weatherClient.fetchHourlyWeather(18.5314, 73.8446, 1, 1)).thenReturn(Optional.of(fakeResp));

        Instant now = Instant.now();
        WeatherObservation obs = new WeatherObservation();
        obs.setCityId(PUNE_CITY_ID);
        obs.setObservedAt(now);
        obs.setTemperature(28.0);

        when(weatherMapper.mapToWeatherObservations(fakeResp, PUNE_CITY_ID, 18.5314, 73.8446))
                .thenReturn(List.of(obs));

        // Mock duplicate in DB
        when(weatherRepository.existsByH3IndexAndObservedAt(PUNE_H3, now)).thenReturn(true);

        WeatherIngestionService.WeatherIngestionSummary summary = ingestionService.ingestStationWeather(puneStation);

        assertThat(summary.getInserted()).isEqualTo(0);
        assertThat(summary.getDuplicates()).isEqualTo(1);
        verify(weatherRepository, never()).save(any(WeatherObservation.class));
    }

    @Test
    @DisplayName("3. Empty provider response produces 0 inserted and no synthetic fallbacks")
    void testEmptyProviderResponse() {
        when(h3Service.coordinatesToH3(18.5314, 73.8446)).thenReturn(PUNE_H3);
        when(gridService.getOrCreateGridCell(PUNE_H3, PUNE_CITY_ID)).thenReturn(new GridCell());

        when(weatherClient.fetchHourlyWeather(18.5314, 73.8446, 1, 1)).thenReturn(Optional.empty());

        WeatherIngestionService.WeatherIngestionSummary summary = ingestionService.ingestStationWeather(puneStation);

        assertThat(summary.getFetched()).isEqualTo(0);
        assertThat(summary.getInserted()).isEqualTo(0);
        verify(weatherRepository, never()).save(any(WeatherObservation.class));
    }
}
