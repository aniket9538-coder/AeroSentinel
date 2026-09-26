package com.aerosentinel.grid;

import com.aerosentinel.air.AirObservation;
import com.aerosentinel.air.AirObservationRepository;
import com.aerosentinel.city.CityRepository;
import com.aerosentinel.dto.grid.GridAirObservationResponse;
import com.aerosentinel.dto.grid.GridCellObservationResponse;
import com.aerosentinel.dto.grid.GridCellResponse;
import com.aerosentinel.dto.grid.GridWeatherObservationResponse;
import com.aerosentinel.dto.grid.LatLngPoint;
import com.aerosentinel.exception.ResourceNotFoundException;
import com.aerosentinel.sensor.MonitoringStation;
import com.aerosentinel.sensor.SensorRepository;
import com.aerosentinel.spatial.H3Service;
import com.aerosentinel.weather.WeatherObservation;
import com.aerosentinel.weather.WeatherRepository;
import com.uber.h3core.util.LatLng;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GridService {

    private static final Logger log = LoggerFactory.getLogger(GridService.class);

    private final GridRepository gridRepository;
    private final H3Service h3Service;
    private final CityRepository cityRepository;
    private final SensorRepository sensorRepository;
    private final AirObservationRepository airObservationRepository;
    private final WeatherRepository weatherRepository;

    public GridService(GridRepository gridRepository,
                       H3Service h3Service,
                       CityRepository cityRepository,
                       SensorRepository sensorRepository,
                       AirObservationRepository airObservationRepository,
                       WeatherRepository weatherRepository) {
        this.gridRepository = gridRepository;
        this.h3Service = h3Service;
        this.cityRepository = cityRepository;
        this.sensorRepository = sensorRepository;
        this.airObservationRepository = airObservationRepository;
        this.weatherRepository = weatherRepository;
    }

    /**
     * Maps an internal GridCell entity to the canonical external GridCellResponse DTO
     * containing authentic centroid and perimeter boundary polygon coordinates.
     */
    public GridCellResponse toGridCellResponse(GridCell cell) {
        LatLngPoint center = new LatLngPoint(cell.getCenterLatitude(), cell.getCenterLongitude());
        List<LatLngPoint> boundary = h3Service.h3ToBoundary(cell.getH3Index()).stream()
                .map(v -> new LatLngPoint(v.lat, v.lng))
                .toList();
        return new GridCellResponse(
                cell.getH3Index(),
                cell.getCityId(),
                cell.getResolution(),
                center,
                boundary
        );
    }

    /**
     * Canonical Endpoint: GET /api/v1/grid?cityId={cityId}
     * Returns only REAL grid cells persisted in PostgreSQL for the validated city.
     */
    public List<GridCellResponse> getGridCellsForCity(UUID cityId) {
        if (!cityRepository.existsById(cityId)) {
            throw new ResourceNotFoundException("City not found with id: " + cityId);
        }
        return gridRepository.findByCityId(cityId).stream()
                .map(this::toGridCellResponse)
                .toList();
    }

    /**
     * Canonical Endpoint: GET /api/v1/grid/{h3Index}
     * Validates H3 format and retrieves persisted grid cell details.
     */
    public GridCellResponse getGridCellByH3(String h3Index) {
        if (!h3Service.validateH3Index(h3Index)) {
            throw new IllegalArgumentException("Invalid H3 index format: " + h3Index);
        }
        GridCell cell = gridRepository.findByH3Index(h3Index.trim())
                .orElseThrow(() -> new ResourceNotFoundException("H3 grid cell not found: " + h3Index));
        return toGridCellResponse(cell);
    }

    /**
     * Canonical Endpoint: GET /api/v1/grid/{h3Index}/observations
     * Returns combined normalized air and weather observations for the H3 cell,
     * ordered chronologically ascending for UI charting.
     */
    public GridCellObservationResponse getCellObservations(String h3Index) {
        if (!h3Service.validateH3Index(h3Index)) {
            throw new IllegalArgumentException("Invalid H3 index format: " + h3Index);
        }
        GridCell cell = gridRepository.findByH3Index(h3Index.trim())
                .orElseThrow(() -> new ResourceNotFoundException("H3 grid cell not found: " + h3Index));

        // Station name map for resolving station human-readable names without N+1 queries
        Map<String, String> stationNameMap = sensorRepository.findByCityId(cell.getCityId()).stream()
                .collect(Collectors.toMap(MonitoringStation::getStationCode, MonitoringStation::getName, (a, b) -> a));

        List<GridAirObservationResponse> airResponses = airObservationRepository
                .findByH3IndexOrderByObservedAtAsc(h3Index.trim()).stream()
                .map(air -> new GridAirObservationResponse(
                        air.getId(),
                        air.getStationId(),
                        stationNameMap.getOrDefault(air.getStationId(), air.getStationId()),
                        air.getPm25(),
                        air.getObservedAt(),
                        air.getSource(),
                        air.getDataQuality() != null ? air.getDataQuality() : "VALID",
                        air.getH3Index()
                ))
                .toList();

        List<GridWeatherObservationResponse> weatherResponses = weatherRepository
                .findByH3IndexOrderByObservedAtAsc(h3Index.trim()).stream()
                .map(w -> new GridWeatherObservationResponse(
                        w.getId(),
                        w.getTemperature(),
                        w.getHumidity(),
                        w.getWindSpeed(),
                        w.getWindDirection(),
                        w.getRainfall(),
                        w.getObservedAt(),
                        w.getSource(),
                        w.getH3Index()
                ))
                .toList();

        return new GridCellObservationResponse(cell.getH3Index(), cell.getCityId(), airResponses, weatherResponses);
    }

    public List<GridCell> getCellsByCity(UUID cityId) {
        return gridRepository.findByCityId(cityId);
    }

    public Optional<GridCell> getCellByH3(String h3Index) {
        return gridRepository.findByH3Index(h3Index);
    }

    public GridCell saveCell(GridCell cell) {
        return gridRepository.save(cell);
    }

    /**
     * Idempotently finds an existing grid cell by H3 index or creates and persists a new one
     * with valid PostGIS centroid and polygon boundary.
     */
    @Transactional
    public GridCell getOrCreateGridCell(String h3Index, UUID cityId) {
        if (!h3Service.validateH3Index(h3Index)) {
            throw new IllegalArgumentException("Invalid H3 index: " + h3Index);
        }

        Optional<GridCell> existingOpt = gridRepository.findByH3Index(h3Index);
        if (existingOpt.isPresent()) {
            GridCell existing = existingOpt.get();
            // Ensure boundary geometry is populated if it was null
            Optional<String> boundaryOpt = gridRepository.findBoundaryWktByH3Index(h3Index);
            if (boundaryOpt.isEmpty() || boundaryOpt.get().isBlank()) {
                String boundaryWkt = h3Service.h3ToBoundaryWkt(h3Index);
                gridRepository.updateBoundary(h3Index, boundaryWkt);
                log.info("Populated missing PostGIS boundary for existing grid cell {}", h3Index);
            }
            return existing;
        }

        LatLng center = h3Service.h3ToCenter(h3Index);
        String boundaryWkt = h3Service.h3ToBoundaryWkt(h3Index);

        GridCell newCell = new GridCell();
        newCell.setCityId(cityId);
        newCell.setH3Index(h3Index);
        newCell.setResolution(h3Service.getResolution());
        newCell.setCenterLatitude(center.lat);
        newCell.setCenterLongitude(center.lng);
        newCell.setActive(true);
        newCell.setCreatedAt(Instant.now());

        GridCell saved = gridRepository.save(newCell);
        gridRepository.updateBoundary(h3Index, boundaryWkt);
        log.info("Created and persisted real H3 grid cell: h3Index={}, cityId={}, center=({}, {})",
                h3Index, cityId, center.lat, center.lng);

        return saved;
    }

    public Optional<String> getCellBoundaryWkt(String h3Index) {
        return gridRepository.findBoundaryWktByH3Index(h3Index);
    }

    public Boolean isCellBoundaryValid(String h3Index) {
        return gridRepository.isBoundaryValid(h3Index);
    }
}
