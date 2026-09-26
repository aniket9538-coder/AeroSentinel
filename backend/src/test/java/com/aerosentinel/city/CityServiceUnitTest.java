package com.aerosentinel.city;

import com.aerosentinel.dto.city.CityResponse;
import com.aerosentinel.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CityServiceUnitTest {

    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private CityService cityService;

    private UUID puneId;
    private City puneCity;

    @BeforeEach
    void setUp() {
        puneId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        puneCity = new City();
        puneCity.setId(puneId);
        puneCity.setName("Pune");
        puneCity.setState("Maharashtra");
        puneCity.setCountry("India");
        puneCity.setTimezone("Asia/Kolkata");
        puneCity.setLatitude(18.5204);
        puneCity.setLongitude(73.8567);
        puneCity.setActive(true);
        puneCity.setCreatedAt(Instant.parse("2026-09-24T00:00:00Z"));
    }

    @Test
    @DisplayName("Unit Test 1: City exists - returns city response DTO with valid fields")
    void testCityExists() {
        when(cityRepository.findById(puneId)).thenReturn(Optional.of(puneCity));

        CityResponse response = cityService.getCityResponseById(puneId);

        assertNotNull(response);
        assertEquals(puneId, response.getId());
        assertEquals("Pune", response.getName());
        assertEquals("Maharashtra", response.getState());
        assertEquals(18.5204, response.getLatitude());
        assertEquals(73.8567, response.getLongitude());
        assertTrue(response.getActive());
        verify(cityRepository, times(1)).findById(puneId);
    }

    @Test
    @DisplayName("Unit Test 2: City not found - throws ResourceNotFoundException")
    void testCityNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(cityRepository.findById(unknownId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> cityService.getCityResponseById(unknownId));

        assertTrue(ex.getMessage().contains("City not found"));
        verify(cityRepository, times(1)).findById(unknownId);
    }

    @Test
    @DisplayName("Unit Test: Returns all active cities mapped to DTOs")
    void testGetAllActiveCityResponses() {
        when(cityRepository.findByActiveTrue()).thenReturn(List.of(puneCity));

        List<CityResponse> responses = cityService.getAllActiveCityResponses();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("Pune", responses.get(0).getName());
        verify(cityRepository, times(1)).findByActiveTrue();
    }
}
