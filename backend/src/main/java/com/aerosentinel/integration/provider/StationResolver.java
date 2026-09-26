package com.aerosentinel.integration.provider;

import com.aerosentinel.sensor.MonitoringStation;
import java.util.Optional;

public interface StationResolver {
    Optional<MonitoringStation> resolveStation(ProviderObservation observation);
}
