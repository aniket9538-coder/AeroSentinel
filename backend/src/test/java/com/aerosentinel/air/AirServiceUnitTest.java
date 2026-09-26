package com.aerosentinel.air;

import com.aerosentinel.city.City;
import com.aerosentinel.city.CityRepository;
import com.aerosentinel.dto.air.AirQualityHistoryResponse;
import com.aerosentinel.dto.air.AirQualityObservationResponse;
import com.aerosentinel.dto.air.HistoricalObservationResponse;
import com.aerosentinel.dto.air.LatestAirQualityResponse;
import com.aerosentinel.exception.ResourceNotFoundException;
import com.aerosentinel.exception.ValidationException;
import com.aerosentinel.sensor.MonitoringStation;
import com.aerosentinel.sensor.SensorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AirServiceUnitTest {

    @Mock
    private AirObservationRepository airObservationRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private SensorRepository sensorRepository;

    @InjectMocks
    private AirService airService;

    private UUID puneId;
    private City puneCity;
    private MonitoringStation station1;
    private MonitoringStation station2;
    private AirObservation observation1;
    private AirObservation observation2;

    @BeforeEach
    void setUp() {
        puneId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        puneCity = new City();
        puneCity.setId(puneId);
        puneCity.setName("Pune");

        station1 = new MonitoringStation();
        station1.setId(UUID.fromString("660e8400-e29b-41d4-a716-446655440001"));
        station1.setStationCode("PUN-001");
        station1.setName("Shivajinagar CAAQMS");
        station1.setCityId(puneId);
        station1.setStatus("ACTIVE");

        station2 = new MonitoringStation();
        station2.setId(UUID.fromString("660e8400-e29b-41d4-a716-446655440002"));
        station2.setStationCode("PUN-002");
        station2.setName("Katraj Air Station");
        station2.setCityId(puneId);
        station2.setStatus("ACTIVE");

        observation1 = new AirObservation();
        observation1.setId(UUID.fromString("880e8400-e29b-41d4-a716-446655440012"));
        observation1.setCityId(puneId);
        observation1.setStationId("PUN-001");
        observation1.setPm25(78.0);
        observation1.setObservedAt(Instant.parse("2026-09-24T22:00:00Z"));
        observation1.setSource("CPCB");
        observation1.setDataQuality("VALID");

        observation2 = new AirObservation();
        observation2.setId(UUID.fromString("880e8400-e29b-41d4-a716-446655440024"));
        observation2.setCityId(puneId);
        observation2.setStationId("PUN-002");
        observation2.setPm25(62.0);
        observation2.setObservedAt(Instant.parse("2026-09-24T22:00:00Z"));
        observation2.setSource("MPCB");
        observation2.setDataQuality("VALID");
    }

    // -------------------------------------------------------------
    // LATEST AIR QUALITY UNIT TESTS
    // -------------------------------------------------------------

    @Test
    @DisplayName("Unit Test 3: Valid city returns station observations")
    void testValidCityReturnsStationObservations() {
        when(cityRepository.findById(puneId)).thenReturn(Optional.of(puneCity));
        when(sensorRepository.findByCityIdAndStatus(puneId, "ACTIVE")).thenReturn(List.of(station1));
        when(airObservationRepository.findFirstByStationIdOrderByObservedAtDesc("PUN-001"))
                .thenReturn(Optional.of(observation1));

        LatestAirQualityResponse response = airService.getLatestAirQualityForCity(puneId);

        assertNotNull(response);
        assertEquals(puneId, response.getCityId());
        assertEquals("Pune", response.getCityName());
        assertEquals(1, response.getObservations().size());

        AirQualityObservationResponse obs = response.getObservations().get(0);
        assertEquals("PUN-001", obs.getStationId());
        assertEquals("Shivajinagar CAAQMS", obs.getStationName());
        assertEquals(78.0, obs.getPm25());
        assertEquals(Instant.parse("2026-09-24T22:00:00Z"), obs.getObservedAt());
        assertEquals("CPCB", obs.getSource());
        assertEquals("VALID", obs.getQuality());
    }

    @Test
    @DisplayName("Unit Test 4: Latest observation is selected by ordering observedAt DESC")
    void testLatestObservationIsSelected() {
        when(cityRepository.findById(puneId)).thenReturn(Optional.of(puneCity));
        when(sensorRepository.findByCityIdAndStatus(puneId, "ACTIVE")).thenReturn(List.of(station1));
        when(airObservationRepository.findFirstByStationIdOrderByObservedAtDesc("PUN-001"))
                .thenReturn(Optional.of(observation1));

        LatestAirQualityResponse response = airService.getLatestAirQualityForCity(puneId);

        verify(airObservationRepository, times(1))
                .findFirstByStationIdOrderByObservedAtDesc("PUN-001");
        assertEquals(78.0, response.getObservations().get(0).getPm25());
    }

    @Test
    @DisplayName("Unit Test 5: Multiple stations handled")
    void testMultipleStationsHandled() {
        when(cityRepository.findById(puneId)).thenReturn(Optional.of(puneCity));
        when(sensorRepository.findByCityIdAndStatus(puneId, "ACTIVE")).thenReturn(List.of(station1, station2));
        when(airObservationRepository.findFirstByStationIdOrderByObservedAtDesc("PUN-001"))
                .thenReturn(Optional.of(observation1));
        when(airObservationRepository.findFirstByStationIdOrderByObservedAtDesc("PUN-002"))
                .thenReturn(Optional.of(observation2));

        LatestAirQualityResponse response = airService.getLatestAirQualityForCity(puneId);

        assertNotNull(response);
        assertEquals(2, response.getObservations().size());
        assertEquals("PUN-001", response.getObservations().get(0).getStationId());
        assertEquals("PUN-002", response.getObservations().get(1).getStationId());
    }

    @Test
    @DisplayName("Unit Test 6: No observation handled gracefully")
    void testNoObservationHandled() {
        when(cityRepository.findById(puneId)).thenReturn(Optional.of(puneCity));
        when(sensorRepository.findByCityIdAndStatus(puneId, "ACTIVE")).thenReturn(List.of(station1));
        when(airObservationRepository.findFirstByStationIdOrderByObservedAtDesc("PUN-001"))
                .thenReturn(Optional.empty());

        LatestAirQualityResponse response = airService.getLatestAirQualityForCity(puneId);

        assertNotNull(response);
        assertEquals("Pune", response.getCityName());
        assertTrue(response.getObservations().isEmpty());
    }

    // -------------------------------------------------------------
    // STATION HISTORY UNIT TESTS
    // -------------------------------------------------------------

    @Test
    @DisplayName("Unit Test 7: Valid station + valid date range returns observations")
    void testValidStationAndDateRange() {
        Instant from = Instant.parse("2026-09-24T00:00:00Z");
        Instant to = Instant.parse("2026-09-24T23:59:59Z");

        when(sensorRepository.findByStationCode("PUN-001")).thenReturn(Optional.of(station1));
        when(airObservationRepository.findByStationIdAndObservedAtBetweenOrderByObservedAtAsc("PUN-001", from, to))
                .thenReturn(List.of(observation1));

        AirQualityHistoryResponse response = airService.getStationAirQualityHistory(
                "PUN-001", "2026-09-24T00:00:00Z", "2026-09-24T23:59:59Z");

        assertNotNull(response);
        assertEquals("PUN-001", response.getStationId());
        assertEquals("Shivajinagar CAAQMS", response.getStationName());
        assertEquals(1, response.getObservations().size());
        assertEquals(78.0, response.getObservations().get(0).getPm25());
        assertEquals("CPCB", response.getObservations().get(0).getSource());
        assertEquals("VALID", response.getObservations().get(0).getQuality());
    }

    @Test
    @DisplayName("Unit Test 8: Invalid station throws ResourceNotFoundException")
    void testInvalidStationThrowsException() {
        when(sensorRepository.findByStationCode("INVALID-999")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                airService.getStationAirQualityHistory("INVALID-999", "2026-09-24T00:00:00Z", "2026-09-24T23:59:59Z"));

        assertTrue(ex.getMessage().contains("Monitoring station not found"));
    }

    @Test
    @DisplayName("Unit Test 9: Invalid date range (from > to) throws ValidationException")
    void testInvalidDateRangeThrowsException() {
        ValidationException ex = assertThrows(ValidationException.class, () ->
                airService.getStationAirQualityHistory("PUN-001", "2026-09-25T00:00:00Z", "2026-09-24T00:00:00Z"));

        assertTrue(ex.getMessage().contains("must not be after 'to'"));
    }

    @Test
    @DisplayName("Unit Test 10: Empty result returns controlled empty response")
    void testEmptyResultReturnsControlledResponse() {
        Instant from = Instant.parse("2020-01-01T00:00:00Z");
        Instant to = Instant.parse("2020-01-02T00:00:00Z");

        when(sensorRepository.findByStationCode("PUN-001")).thenReturn(Optional.of(station1));
        when(airObservationRepository.findByStationIdAndObservedAtBetweenOrderByObservedAtAsc("PUN-001", from, to))
                .thenReturn(Collections.emptyList());

        AirQualityHistoryResponse response = airService.getStationAirQualityHistory(
                "PUN-001", "2020-01-01T00:00:00Z", "2020-01-02T00:00:00Z");

        assertNotNull(response);
        assertEquals("PUN-001", response.getStationId());
        assertEquals("Shivajinagar CAAQMS", response.getStationName());
        assertNotNull(response.getObservations());
        assertTrue(response.getObservations().isEmpty());
    }

    // -------------------------------------------------------------
    // ENTITY TO DTO MAPPING PRESERVATION
    // -------------------------------------------------------------

    @Test
    @DisplayName("Unit Test 11: Entity -> DTO mapping preserves all required fields")
    void testEntityToDtoMappingPreservesAllFields() {
        when(cityRepository.findById(puneId)).thenReturn(Optional.of(puneCity));
        when(sensorRepository.findByCityIdAndStatus(puneId, "ACTIVE")).thenReturn(List.of(station1));
        when(airObservationRepository.findFirstByStationIdOrderByObservedAtDesc("PUN-001"))
                .thenReturn(Optional.of(observation1));

        LatestAirQualityResponse response = airService.getLatestAirQualityForCity(puneId);
        AirQualityObservationResponse dto = response.getObservations().get(0);

        assertEquals("PUN-001", dto.getStationId());
        assertEquals("Shivajinagar CAAQMS", dto.getStationName());
        assertEquals(78.0, dto.getPm25());
        assertEquals(Instant.parse("2026-09-24T22:00:00Z"), dto.getObservedAt());
        assertEquals("CPCB", dto.getSource());
        assertEquals("VALID", dto.getQuality());
    }
}
