package com.aerosentinel.integration.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProviderFetchResult {

    private final ProviderStatus status;
    private final List<ProviderObservation> observations;
    private final String errorMessage;

    public ProviderFetchResult(ProviderStatus status, List<ProviderObservation> observations, String errorMessage) {
        this.status = status;
        this.observations = observations != null ? observations : Collections.emptyList();
        this.errorMessage = errorMessage;
    }

    public static ProviderFetchResult success(List<ProviderObservation> observations) {
        if (observations == null || observations.isEmpty()) {
            return new ProviderFetchResult(ProviderStatus.EMPTY_RESPONSE, Collections.emptyList(), null);
        }
        return new ProviderFetchResult(ProviderStatus.SUCCESS, observations, null);
    }

    public static ProviderFetchResult empty() {
        return new ProviderFetchResult(ProviderStatus.EMPTY_RESPONSE, Collections.emptyList(), null);
    }

    public static ProviderFetchResult notConfigured(String message) {
        return new ProviderFetchResult(ProviderStatus.NOT_CONFIGURED, Collections.emptyList(), message);
    }

    public static ProviderFetchResult sourceUnavailable(String message) {
        return new ProviderFetchResult(ProviderStatus.SOURCE_UNAVAILABLE, Collections.emptyList(), message);
    }

    public static ProviderFetchResult rateLimited(String message) {
        return new ProviderFetchResult(ProviderStatus.RATE_LIMITED, Collections.emptyList(), message);
    }

    public ProviderStatus getStatus() { return status; }
    public List<ProviderObservation> getObservations() { return observations; }
    public String getErrorMessage() { return errorMessage; }
    public boolean isSuccessful() { return status == ProviderStatus.SUCCESS || status == ProviderStatus.EMPTY_RESPONSE; }
}
