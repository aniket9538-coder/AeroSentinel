package com.aerosentinel.integration.provider;

public interface AirQualityProvider {

    String getProviderName();

    boolean isConfigured();

    ProviderFetchResult fetchLatestObservations();

    ProviderFetchResult fetchLatestObservations(String providerLocationId);
}
