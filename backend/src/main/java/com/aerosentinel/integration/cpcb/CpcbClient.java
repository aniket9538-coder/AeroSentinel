package com.aerosentinel.integration.cpcb;

import com.aerosentinel.integration.provider.AirQualityProvider;
import com.aerosentinel.integration.provider.ProviderFetchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * CPCB Provider Client Stub.
 *
 * Audit Finding:
 * CPCB (Central Pollution Control Board) does not expose a publicly accessible,
 * stable, authenticated REST API for real-time programmatic ingestion without
 * proprietary governmental MOU/network access. DNS for api.cpcb.gov.in does not resolve.
 *
 * Per Phase 4 instructions, OpenAQ is selected as the active verified external provider.
 * This class documents CPCB's unavailability and safely reports SOURCE_UNAVAILABLE without
 * inventing endpoints or generating synthetic data.
 */
@Component
public class CpcbClient implements AirQualityProvider {

    private static final Logger log = LoggerFactory.getLogger(CpcbClient.class);
    private static final String PROVIDER_NAME = "CPCB";

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public ProviderFetchResult fetchLatestObservations() {
        log.warn("CPCB provider call skipped: no public CPCB REST API endpoint available");
        return ProviderFetchResult.sourceUnavailable("CPCB does not expose a public REST API; integration unavailable");
    }

    @Override
    public ProviderFetchResult fetchLatestObservations(String providerLocationId) {
        log.warn("CPCB provider call skipped for location {}: no public CPCB REST API endpoint available", providerLocationId);
        return ProviderFetchResult.sourceUnavailable("CPCB does not expose a public REST API; integration unavailable");
    }
}
