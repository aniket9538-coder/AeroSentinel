package com.aerosentinel.weather;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WeatherService {

    private final WeatherRepository weatherRepository;

    public WeatherService(WeatherRepository weatherRepository) {
        this.weatherRepository = weatherRepository;
    }

    public Optional<WeatherObservation> getCurrentWeather(UUID cityId) {
        return weatherRepository.findFirstByCityIdOrderByObservedAtDesc(cityId);
    }

    public List<WeatherObservation> getWeatherHistory(UUID cityId) {
        return weatherRepository.findByCityIdOrderByObservedAtDesc(cityId);
    }

    public WeatherObservation saveWeather(WeatherObservation weather) {
        return weatherRepository.save(weather);
    }
}
