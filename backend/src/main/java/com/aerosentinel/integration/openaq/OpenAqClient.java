package com.aerosentinel.integration.openaq;

import com.aerosentinel.integration.provider.AirQualityProvider;
import com.aerosentinel.integration.provider.ProviderFetchResult;
import com.aerosentinel.integration.provider.ProviderObservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class OpenAqClient implements AirQualityProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAqClient.class);
    private static final String PROVIDER_NAME = "OPENAQ";
    private static final List<String> DEFAULT_PUNE_LOCATIONS = Arrays.asList("11613", "3409438", "60658", "8118");
    private static final List<String> DEFAULT_MUMBAI_LOCATIONS = Arrays.asList("6945", "6948");
    private static final List<String> DEFAULT_DELHI_LOCATIONS = Arrays.asList("17", "235", "50");
    private static final List<String> DEFAULT_LOCATIONS;

    static {
        List<String> list = new ArrayList<>();
        list.addAll(DEFAULT_PUNE_LOCATIONS);
        list.addAll(DEFAULT_MUMBAI_LOCATIONS);
        list.addAll(DEFAULT_DELHI_LOCATIONS);
        DEFAULT_LOCATIONS = java.util.Collections.unmodifiableList(list);
    }

    private final OpenAqProperties properties;
    private final OpenAqMapper mapper;
    private final RestClient restClient;

    @org.springframework.beans.factory.annotation.Autowired
    public OpenAqClient(OpenAqProperties properties, OpenAqMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeout = properties.getTimeoutMs() > 0 ? properties.getTimeoutMs() : 5000;
        requestFactory.setConnectTimeout(Duration.ofMillis(timeout));
        requestFactory.setReadTimeout(Duration.ofMillis(timeout));

        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    // Constructor for testing with mock RestClient
    public OpenAqClient(OpenAqProperties properties, OpenAqMapper mapper, RestClient restClient) {
        this.properties = properties;
        this.mapper = mapper;
        this.restClient = restClient;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().trim().isEmpty();
    }

    @Override
    public ProviderFetchResult fetchLatestObservations() {
        if (!isConfigured()) {
            log.warn("OpenAQ provider is not configured (missing API key). Skipping live call.");
            return ProviderFetchResult.notConfigured("OpenAQ API key is not configured");
        }

        List<ProviderObservation> combined = new ArrayList<>();
        for (String locationId : DEFAULT_LOCATIONS) {
            ProviderFetchResult result = fetchLatestObservations(locationId);
            if (!result.isSuccessful()) {
                log.warn("Fetch failed for location {}: {}", locationId, result.getErrorMessage());
                // Return immediately if rate limited or unauthorized
                if (result.getStatus() == com.aerosentinel.integration.provider.ProviderStatus.RATE_LIMITED ||
                    result.getStatus() == com.aerosentinel.integration.provider.ProviderStatus.NOT_CONFIGURED) {
                    return result;
                }
            } else {
                combined.addAll(result.getObservations());
            }
        }

        return combined.isEmpty() ? ProviderFetchResult.empty() : ProviderFetchResult.success(combined);
    }

    @Override
    public ProviderFetchResult fetchLatestObservations(String providerLocationId) {
        if (!isConfigured()) {
            log.warn("OpenAQ provider is not configured. Cannot fetch location {}", providerLocationId);
            return ProviderFetchResult.notConfigured("OpenAQ API key is not configured");
        }

        if (providerLocationId == null || providerLocationId.trim().isEmpty()) {
            return fetchLatestObservations();
        }

        try {
            log.debug("Calling OpenAQ API for locationId={}", providerLocationId);
            OpenAqResponse response = restClient.get()
                    .uri("/locations/{locationId}/sensors", providerLocationId)
                    .header("X-API-Key", properties.getApiKey())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        int code = resp.getStatusCode().value();
                        if (code == 401) {
                            throw new HttpClientErrorException(HttpStatusCode.valueOf(401), "Unauthorized");
                        } else if (code == 429) {
                            throw new HttpClientErrorException(HttpStatusCode.valueOf(429), "Too Many Requests");
                        } else if (code == 404) {
                            throw new HttpClientErrorException(HttpStatusCode.valueOf(404), "Not Found");
                        } else {
                            throw new HttpClientErrorException(resp.getStatusCode(), "Client error " + code);
                        }
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        throw new HttpServerErrorException(resp.getStatusCode(), "Server error " + resp.getStatusCode().value());
                    })
                    .body(OpenAqResponse.class);

            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                log.info("OpenAQ returned empty response for locationId={}", providerLocationId);
                return ProviderFetchResult.empty();
            }

            List<ProviderObservation> observations = mapper.mapResponse(response, providerLocationId);
            log.info("OpenAQ fetch succeeded for locationId={}: fetched {} measurements, mapped {} observations",
                    providerLocationId, response.getResults().size(), observations.size());
            return ProviderFetchResult.success(observations);

        } catch (HttpClientErrorException e) {
            int code = e.getStatusCode().value();
            if (code == 401) {
                log.error("OpenAQ authentication failed: HTTP 401 Unauthorized");
                return ProviderFetchResult.notConfigured("OpenAQ authentication failed: HTTP 401 Unauthorized");
            } else if (code == 429) {
                log.warn("OpenAQ rate limit exceeded: HTTP 429 Too Many Requests");
                return ProviderFetchResult.rateLimited("OpenAQ rate limit reached (HTTP 429)");
            } else if (code == 404) {
                log.info("OpenAQ location not found (HTTP 404): locationId={}", providerLocationId);
                return ProviderFetchResult.empty();
            }
            log.error("OpenAQ client error {}: {}", code, e.getMessage());
            return ProviderFetchResult.sourceUnavailable("OpenAQ client error: " + code);
        } catch (HttpServerErrorException e) {
            log.error("OpenAQ server error {}: {}", e.getStatusCode().value(), e.getMessage());
            return ProviderFetchResult.sourceUnavailable("OpenAQ server error: HTTP " + e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            log.error("OpenAQ connection/timeout failure: {}", e.getMessage());
            return ProviderFetchResult.sourceUnavailable("OpenAQ connection failure: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during OpenAQ fetch: {}", e.getMessage());
            return ProviderFetchResult.sourceUnavailable("OpenAQ unexpected error: " + e.getMessage());
        }
    }

    public ProviderFetchResult fetchSensorHours(String providerLocationId, String sensorId, Instant from, Instant to) {
        if (!isConfigured()) {
            log.warn("OpenAQ provider is not configured. Cannot fetch sensor hours for {}", sensorId);
            return ProviderFetchResult.notConfigured("OpenAQ API key is not configured");
        }

        try {
            log.debug("Calling OpenAQ API /sensors/{}/hours for locationId={}", sensorId, providerLocationId);
            OpenAqResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/sensors/{sensorId}/hours")
                            .queryParam("datetime_from", from.toString())
                            .queryParam("datetime_to", to.toString())
                            .queryParam("limit", 100)
                            .build(sensorId))
                    .header("X-API-Key", properties.getApiKey())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        int code = resp.getStatusCode().value();
                        if (code == 401) {
                            throw new HttpClientErrorException(HttpStatusCode.valueOf(401), "Unauthorized");
                        } else if (code == 429) {
                            throw new HttpClientErrorException(HttpStatusCode.valueOf(429), "Too Many Requests");
                        } else if (code == 404) {
                            throw new HttpClientErrorException(HttpStatusCode.valueOf(404), "Not Found");
                        } else {
                            throw new HttpClientErrorException(resp.getStatusCode(), "Client error " + code);
                        }
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        throw new HttpServerErrorException(resp.getStatusCode(), "Server error " + resp.getStatusCode().value());
                    })
                    .body(OpenAqResponse.class);

            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                log.info("OpenAQ returned empty sensor hours for sensorId={}", sensorId);
                return ProviderFetchResult.empty();
            }

            List<ProviderObservation> observations = mapper.mapResponse(response, providerLocationId);
            log.info("OpenAQ fetchSensorHours succeeded for sensorId={} (locationId={}): fetched {} measurements, mapped {} observations",
                    sensorId, providerLocationId, response.getResults().size(), observations.size());
            return ProviderFetchResult.success(observations);

        } catch (HttpClientErrorException e) {
            int code = e.getStatusCode().value();
            if (code == 401) {
                return ProviderFetchResult.notConfigured("OpenAQ authentication failed: HTTP 401 Unauthorized");
            } else if (code == 429) {
                return ProviderFetchResult.rateLimited("OpenAQ rate limit reached (HTTP 429)");
            } else if (code == 404) {
                return ProviderFetchResult.empty();
            }
            return ProviderFetchResult.sourceUnavailable("OpenAQ client error: " + code);
        } catch (HttpServerErrorException e) {
            return ProviderFetchResult.sourceUnavailable("OpenAQ server error: HTTP " + e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            return ProviderFetchResult.sourceUnavailable("OpenAQ connection failure: " + e.getMessage());
        } catch (Exception e) {
            return ProviderFetchResult.sourceUnavailable("OpenAQ unexpected error: " + e.getMessage());
        }
    }
}
