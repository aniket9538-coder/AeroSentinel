package com.aerosentinel.city;

import com.aerosentinel.air.AirService;
import com.aerosentinel.dto.air.LatestAirQualityResponse;
import com.aerosentinel.dto.city.CityResponse;
import com.aerosentinel.dto.weather.WeatherLatestResponse;
import com.aerosentinel.weather.WeatherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cities")
public class CityController {

    private final CityService cityService;
    private final AirService airService;
    private final WeatherService weatherService;

    public CityController(CityService cityService, AirService airService, WeatherService weatherService) {
        this.cityService = cityService;
        this.airService = airService;
        this.weatherService = weatherService;
    }

    @GetMapping
    public ResponseEntity<List<CityResponse>> getCities() {
        return ResponseEntity.ok(cityService.getAllActiveCityResponses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CityResponse> getCity(@PathVariable UUID id) {
        return ResponseEntity.ok(cityService.getCityResponseById(id));
    }

    @GetMapping("/{cityId}/air-quality/latest")
    public ResponseEntity<LatestAirQualityResponse> getLatestAirQuality(@PathVariable UUID cityId) {
        return ResponseEntity.ok(airService.getLatestAirQualityForCity(cityId));
    }

    @GetMapping("/{cityId}/weather/latest")
    public ResponseEntity<WeatherLatestResponse> getLatestWeather(@PathVariable UUID cityId) {
        return ResponseEntity.ok(weatherService.getLatestWeatherForCity(cityId));
    }
}
