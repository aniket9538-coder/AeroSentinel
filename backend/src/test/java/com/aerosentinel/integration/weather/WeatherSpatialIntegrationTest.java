package com.aerosentinel.integration.weather;

import com.aerosentinel.air.AirObservation;
import com.aerosentinel.air.AirObservationRepository;
import com.aerosentinel.grid.GridCell;
import com.aerosentinel.grid.GridRepository;
import com.aerosentinel.sensor.MonitoringStation;
import com.aerosentinel.sensor.SensorRepository;
import com.aerosentinel.spatial.H3Service;
import com.aerosentinel.weather.WeatherObservation;
import com.aerosentinel.weather.WeatherRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WeatherSpatialIntegrationTest {

    @Autowired
    private WeatherIngestionService weatherIngestionService;

    @Autowired
    private WeatherRepository weatherRepository;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private AirObservationRepository airObservationRepository;

    @Autowired
    private GridRepository gridRepository;

    @Autowired
    private H3Service h3Service;

    private static final UUID PUNE_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID MUMBAI_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    private static final UUID DELHI_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");

    @Test
    @Order(1)
    @DisplayName("Integration 1: Ingest real weather from Open-Meteo for all active stations")
    void testIngestRealWeather() {
        WeatherIngestionService.WeatherIngestionSummary summary = weatherIngestionService.ingestAllStationsWeather();

        assertThat(summary.getStationsProcessed()).isEqualTo(8);
        assertThat(summary.getFetched()).isGreaterThan(0);
        // On initial or subsequent run, either inserted or duplicates should account for all fetched
        assertThat(summary.getInserted() + summary.getDuplicates()).isGreaterThan(0);
        assertThat(summary.getRejected()).isEqualTo(0);

        long totalWeather = weatherRepository.count();
        assertThat(totalWeather).isGreaterThanOrEqualTo(8); // At least 1 recent hour per station
    }

    @Test
    @Order(2)
    @DisplayName("Integration 2: City-wise weather observations are present for Pune, Mumbai, and Delhi")
    void testCityWiseWeatherPresent() {
        long puneWeatherCount = weatherRepository.countByCityId(PUNE_CITY_ID);
        long mumbaiWeatherCount = weatherRepository.countByCityId(MUMBAI_CITY_ID);
        long delhiWeatherCount = weatherRepository.countByCityId(DELHI_CITY_ID);

        assertThat(puneWeatherCount).withFailMessage("Pune weather observations count is 0").isGreaterThan(0);
        assertThat(mumbaiWeatherCount).withFailMessage("Mumbai weather observations count is 0").isGreaterThan(0);
        assertThat(delhiWeatherCount).withFailMessage("Delhi weather observations count is 0").isGreaterThan(0);
    }

    @Test
    @Order(3)
    @DisplayName("Integration 3: Every persisted weather observation has valid H3 index and OPEN_METEO source")
    void testWeatherH3AndSourceIntegrity() {
        List<WeatherObservation> observations = weatherRepository.findAll();
        assertThat(observations).isNotEmpty();

        for (WeatherObservation obs : observations) {
            assertThat(obs.getSource()).isEqualTo("OPEN_METEO");
            assertThat(obs.getH3Index()).isNotNull();
            assertThat(h3Service.validateH3Index(obs.getH3Index())).isTrue();
            assertThat(obs.getObservedAt()).isNotNull();
            assertThat(obs.getTemperature()).isNotNull();
            assertThat(obs.getCityId()).isNotNull();
        }
    }

    @Test
    @Order(4)
    @DisplayName("Integration 4: Spatial consistency - Air and Weather observations share identical H3 index for same station")
    void testAirAndWeatherSpatialConsistency() {
        // Compare PUN-001 station
        MonitoringStation pun1 = sensorRepository.findByStationCode("PUN-001").orElseThrow();
        String expectedH3 = h3Service.coordinatesToH3(pun1.getLatitude(), pun1.getLongitude());

        // Check latest air observation H3
        Optional<AirObservation> latestAir = airObservationRepository.findFirstByStationIdOrderByObservedAtDesc("PUN-001");
        assertThat(latestAir).isPresent();
        assertThat(latestAir.get().getH3Index()).isEqualTo(expectedH3);

        // Check weather observation H3 for PUN-001 coordinates
        Optional<WeatherObservation> latestWeather = weatherRepository.findFirstByH3IndexOrderByObservedAtDesc(expectedH3);
        assertThat(latestWeather).isPresent();
        assertThat(latestWeather.get().getH3Index()).isEqualTo(expectedH3);
        assertThat(latestWeather.get().getCityId()).isEqualTo(PUNE_CITY_ID);
    }

    @Test
    @Order(5)
    @DisplayName("Integration 5: Grid cells exist for all weather H3 indexes")
    void testGridCellsExistForWeatherH3() {
        List<WeatherObservation> allWeather = weatherRepository.findAll();
        Set<String> weatherH3Set = allWeather.stream()
                .map(WeatherObservation::getH3Index)
                .collect(Collectors.toSet());

        for (String h3 : weatherH3Set) {
            Optional<GridCell> cellOpt = gridRepository.findByH3Index(h3);
            assertThat(cellOpt).isPresent();
            assertThat(cellOpt.get().getCenterLatitude()).isNotNull();
            assertThat(cellOpt.get().getCenterLongitude()).isNotNull();
        }
    }

    @Test
    @Order(6)
    @DisplayName("Integration 6: Idempotent rerun: re-running weather ingestion inserts 0 duplicates")
    void testIdempotentRerun() {
        long countBefore = weatherRepository.count();

        WeatherIngestionService.WeatherIngestionSummary rerunSummary = weatherIngestionService.ingestAllStationsWeather();

        assertThat(rerunSummary.getInserted()).isEqualTo(0);
        assertThat(rerunSummary.getDuplicates()).isGreaterThan(0);

        long countAfter = weatherRepository.count();
        assertThat(countAfter).isEqualTo(countBefore);
    }
}
