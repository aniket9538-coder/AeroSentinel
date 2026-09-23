package com.aerosentinel.forecast;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ForecastService {

    private final ForecastRepository forecastRepository;

    public ForecastService(ForecastRepository forecastRepository) {
        this.forecastRepository = forecastRepository;
    }

    public List<Forecast> getForecastsByCell(UUID cellId) {
        return forecastRepository.findByGridCellIdOrderByTargetTimeAsc(cellId);
    }

    public Forecast saveForecast(Forecast forecast) {
        return forecastRepository.save(forecast);
    }
}
