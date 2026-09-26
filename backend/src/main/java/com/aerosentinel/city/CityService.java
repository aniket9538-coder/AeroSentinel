package com.aerosentinel.city;

import com.aerosentinel.dto.city.CityResponse;
import com.aerosentinel.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CityService {

    private final CityRepository cityRepository;

    public CityService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    public List<City> getAllActiveCities() {
        return cityRepository.findByActiveTrue();
    }

    public Optional<City> getCityById(UUID id) {
        return cityRepository.findById(id);
    }

    public List<CityResponse> getAllActiveCityResponses() {
        return cityRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CityResponse getCityResponseById(UUID id) {
        return cityRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + id));
    }

    public CityResponse toResponse(City city) {
        return new CityResponse(
                city.getId(),
                city.getName(),
                city.getState(),
                city.getCountry(),
                city.getTimezone(),
                city.getLatitude(),
                city.getLongitude(),
                city.getActive(),
                city.getCreatedAt()
        );
    }

    public City saveCity(City city) {
        return cityRepository.save(city);
    }
}
