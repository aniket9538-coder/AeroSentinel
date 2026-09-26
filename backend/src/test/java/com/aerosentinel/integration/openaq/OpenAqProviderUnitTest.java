package com.aerosentinel.integration.openaq;

import com.aerosentinel.air.AirObservationRepository;
import com.aerosentinel.integration.provider.*;
import com.aerosentinel.sensor.MonitoringStation;
import com.aerosentinel.sensor.SensorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(MockitoExtension.class)
class OpenAqProviderUnitTest {

    private OpenAqMapper mapper;
    private ObjectMapper objectMapper;

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private AirObservationRepository airObservationRepository;

    private DefaultStationResolver stationResolver;

    private static final String SAMPLE_OPENAQ_JSON = """
            {
              "meta": {
                "name": "openaq-api",
                "page": 1,
                "limit": 100,
                "found": 1
              },
              "results": [
                {
                  "datetime": {
                    "utc": "2026-09-25T04:00:00Z",
                    "local": "2026-09-25T09:30:00+05:30"
                  },
                  "value": 45.2,
                  "coordinates": {
                    "latitude": 18.5314,
                    "longitude": 73.8446
                  },
                  "sensorsId": 12345,
                  "locationsId": 8118,
                  "parameter": {
                    "id": 2,
                    "name": "pm25",
                    "units": "µg/m³",
                    "displayName": "PM2.5"
                  }
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        mapper = new OpenAqMapper();
        objectMapper = new ObjectMapper();
        stationResolver = new DefaultStationResolver(sensorRepository);
    }

    @Test
    @DisplayName("1. Provider response parsing - deserializes OpenAQ v3 JSON correctly")
    void testProviderResponseParsing() throws Exception {
        OpenAqResponse response = objectMapper.readValue(SAMPLE_OPENAQ_JSON, OpenAqResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getResults()).hasSize(1);

        OpenAqResponse.OpenAqMeasurement measurement = response.getResults().get(0);
        assertThat(measurement.getValue()).isEqualTo(45.2);
        assertThat(measurement.getLocationsId()).isEqualTo(8118L);
        assertThat(measurement.getCoordinates().getLatitude()).isEqualTo(18.5314);
        assertThat(measurement.getCoordinates().getLongitude()).isEqualTo(73.8446);
        assertThat(measurement.getParameter().getId()).isEqualTo(2);
        assertThat(measurement.getParameter().getName()).isEqualTo("pm25");
        assertThat(measurement.getDatetime().getUtc()).isEqualTo("2026-09-25T04:00:00Z");
    }

    @Test
    @DisplayName("2. PM2.5 mapping - maps valid PM2.5 measurement into ProviderObservation")
    void testPm25Mapping() throws Exception {
        OpenAqResponse response = objectMapper.readValue(SAMPLE_OPENAQ_JSON, OpenAqResponse.class);
        List<ProviderObservation> observations = mapper.mapResponse(response, "8118");

        assertThat(observations).hasSize(1);
        ProviderObservation obs = observations.get(0);
        assertThat(obs.getPm25()).isEqualTo(45.2);
        assertThat(obs.getProviderStationId()).isEqualTo("8118");
        assertThat(obs.getQuality()).isEqualTo("VALID");
    }

    @Test
    @DisplayName("3. Timestamp mapping - preserves exact UTC instant")
    void testTimestampMapping() {
        OpenAqResponse.OpenAqDateTime dt = new OpenAqResponse.OpenAqDateTime("2026-09-25T04:00:00Z", null);
        Instant instant = mapper.parseTimestamp(dt);

        assertThat(instant).isEqualTo(Instant.parse("2026-09-25T04:00:00Z"));
    }

    @Test
    @DisplayName("4. Source mapping - preserves provenance as OPENAQ")
    void testSourceMapping() throws Exception {
        OpenAqResponse response = objectMapper.readValue(SAMPLE_OPENAQ_JSON, OpenAqResponse.class);
        List<ProviderObservation> observations = mapper.mapResponse(response, "8118");

        assertThat(observations.get(0).getSource()).isEqualTo("OPENAQ");
    }

    @Test
    @DisplayName("5. Coordinate mapping - correctly maps latitude and longitude")
    void testCoordinateMapping() throws Exception {
        OpenAqResponse response = objectMapper.readValue(SAMPLE_OPENAQ_JSON, OpenAqResponse.class);
        List<ProviderObservation> observations = mapper.mapResponse(response, "8118");

        ProviderObservation obs = observations.get(0);
        assertThat(obs.getLatitude()).isEqualTo(18.5314);
        assertThat(obs.getLongitude()).isEqualTo(73.8446);
    }

    @Test
    @DisplayName("6. Invalid PM2.5 rejection - negative values are rejected")
    void testInvalidPm25RejectionNegative() {
        OpenAqResponse.OpenAqMeasurement measurement = new OpenAqResponse.OpenAqMeasurement();
        measurement.setValue(-5.0);
        measurement.setParameter(new OpenAqResponse.OpenAqParameter(2, "pm25", "µg/m³", "PM2.5"));
        measurement.setDatetime(new OpenAqResponse.OpenAqDateTime("2026-09-25T04:00:00Z", null));

        Optional<ProviderObservation> result = mapper.mapMeasurement(measurement, "8118");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("7. Missing timestamp rejection - null or invalid timestamps are rejected")
    void testMissingTimestampRejection() {
        OpenAqResponse.OpenAqMeasurement measurement = new OpenAqResponse.OpenAqMeasurement();
        measurement.setValue(25.0);
        measurement.setParameter(new OpenAqResponse.OpenAqParameter(2, "pm25", "µg/m³", "PM2.5"));
        measurement.setDatetime(null); // missing timestamp

        Optional<ProviderObservation> result = mapper.mapMeasurement(measurement, "8118");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("8. Non-PM2.5 parameter rejection - parameter != PM2.5 is rejected")
    void testNonPm25ParameterRejection() {
        OpenAqResponse.OpenAqMeasurement measurement = new OpenAqResponse.OpenAqMeasurement();
        measurement.setValue(50.0);
        measurement.setParameter(new OpenAqResponse.OpenAqParameter(1, "pm10", "µg/m³", "PM10"));
        measurement.setDatetime(new OpenAqResponse.OpenAqDateTime("2026-09-25T04:00:00Z", null));

        Optional<ProviderObservation> result = mapper.mapMeasurement(measurement, "8118");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("9. Station mapping success - resolves known OpenAQ location ID 8118 to PUN-001")
    void testStationMappingSuccess() {
        MonitoringStation station = new MonitoringStation();
        station.setStationCode("PUN-001");
        station.setName("Shivaji Nagar");
        station.setLatitude(18.5314);
        station.setLongitude(73.8446);
        when(sensorRepository.findByStationCode("PUN-001")).thenReturn(Optional.of(station));

        ProviderObservation obs = new ProviderObservation();
        obs.setProviderStationId("8118");

        Optional<MonitoringStation> resolved = stationResolver.resolveStation(obs);
        assertThat(resolved).isPresent();
        assertThat(resolved.get().getStationCode()).isEqualTo("PUN-001");
    }

    @Test
    @DisplayName("10. Station mapping failure - unknown station and distant coordinates rejected")
    void testStationMappingFailure() {
        ProviderObservation obs = new ProviderObservation();
        obs.setProviderStationId("UNKNOWN-9999");
        obs.setLatitude(28.6139); // Delhi coords, not Pune
        obs.setLongitude(77.2090);

        when(sensorRepository.findByStationCode("UNKNOWN-9999")).thenReturn(Optional.empty());
        when(sensorRepository.findAll()).thenReturn(Collections.emptyList());

        Optional<MonitoringStation> resolved = stationResolver.resolveStation(obs);
        assertThat(resolved).isEmpty();
    }

    @Test
    @DisplayName("11. Duplicate detection - existing observation prevents re-insertion")
    void testDuplicateDetection() {
        IngestionService ingestionService = new IngestionService(airObservationRepository, stationResolver);

        MonitoringStation station = new MonitoringStation();
        station.setStationCode("PUN-001");
        station.setCityId(UUID.randomUUID());
        station.setLatitude(18.5314);
        station.setLongitude(73.8446);

        when(sensorRepository.findByStationCode("PUN-001")).thenReturn(Optional.of(station));

        Instant observedAt = Instant.parse("2026-09-25T04:00:00Z");
        ProviderObservation obs = new ProviderObservation("8118", 18.5314, 73.8446, observedAt, 42.0, "OPENAQ", "VALID");

        // Mock duplicate found
        when(airObservationRepository.existsByStationIdAndObservedAt("PUN-001", observedAt)).thenReturn(true);

        IngestionSummary summary = ingestionService.processObservations("OPENAQ", List.of(obs));

        assertThat(summary.getFetched()).isEqualTo(1);
        assertThat(summary.getMapped()).isEqualTo(1);
        assertThat(summary.getDuplicates()).isEqualTo(1);
        assertThat(summary.getInserted()).isEqualTo(0);
        verify(airObservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("12. Provider error handling - HTTP 500 returns SOURCE_UNAVAILABLE and 0 records")
    void testProviderErrorHandling500() {
        OpenAqProperties props = new OpenAqProperties();
        props.setApiKey("test-api-key");
        props.setBaseUrl("https://api.openaq.org/v3");

        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openaq.org/v3");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        server.expect(requestTo("https://api.openaq.org/v3/locations/8118/sensors"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-Key", "test-api-key"))
                .andRespond(withServerError());

        OpenAqClient client = new OpenAqClient(props, mapper, restClient);
        ProviderFetchResult result = client.fetchLatestObservations("8118");

        assertThat(result.getStatus()).isEqualTo(ProviderStatus.SOURCE_UNAVAILABLE);
        assertThat(result.getObservations()).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("13. Empty provider response - returns EMPTY_RESPONSE without synthetic fallback")
    void testEmptyProviderResponse() {
        OpenAqProperties props = new OpenAqProperties();
        props.setApiKey("test-api-key");
        props.setBaseUrl("https://api.openaq.org/v3");

        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openaq.org/v3");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        String emptyBody = """
                {
                  "meta": { "found": 0 },
                  "results": []
                }
                """;

        server.expect(requestTo("https://api.openaq.org/v3/locations/8118/sensors"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(emptyBody, MediaType.APPLICATION_JSON));

        OpenAqClient client = new OpenAqClient(props, mapper, restClient);
        ProviderFetchResult result = client.fetchLatestObservations("8118");

        assertThat(result.getStatus()).isEqualTo(ProviderStatus.EMPTY_RESPONSE);
        assertThat(result.getObservations()).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("14. HTTP 429 handling - rate limiting returns RATE_LIMITED status")
    void testHttp429Handling() {
        OpenAqProperties props = new OpenAqProperties();
        props.setApiKey("test-api-key");
        props.setBaseUrl("https://api.openaq.org/v3");

        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openaq.org/v3");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        server.expect(requestTo("https://api.openaq.org/v3/locations/8118/sensors"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withRawStatus(429));

        OpenAqClient client = new OpenAqClient(props, mapper, restClient);
        ProviderFetchResult result = client.fetchLatestObservations("8118");

        assertThat(result.getStatus()).isEqualTo(ProviderStatus.RATE_LIMITED);
        assertThat(result.getObservations()).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("15. Unconfigured provider - reports NOT_CONFIGURED without calling network")
    void testUnconfiguredProvider() {
        OpenAqProperties props = new OpenAqProperties();
        props.setApiKey(""); // unconfigured
        OpenAqClient client = new OpenAqClient(props, mapper);

        assertThat(client.isConfigured()).isFalse();
        ProviderFetchResult result = client.fetchLatestObservations();

        assertThat(result.getStatus()).isEqualTo(ProviderStatus.NOT_CONFIGURED);
        assertThat(result.getObservations()).isEmpty();
    }

    @Test
    @DisplayName("16. No synthetic values - verify zero fallback or random numbers are generated")
    void testNoSyntheticDataGenerated() {
        OpenAqResponse.OpenAqMeasurement measurement = new OpenAqResponse.OpenAqMeasurement();
        measurement.setValue(null); // missing value
        measurement.setParameter(new OpenAqResponse.OpenAqParameter(2, "pm25", "µg/m³", "PM2.5"));
        measurement.setDatetime(new OpenAqResponse.OpenAqDateTime("2026-09-25T04:00:00Z", null));

        Optional<ProviderObservation> obs = mapper.mapMeasurement(measurement, "8118");
        assertThat(obs).isEmpty(); // MUST NOT invent a synthetic PM2.5 value
    }
}
