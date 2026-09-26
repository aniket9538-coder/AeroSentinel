# AeroSentinel — FEATURE 2 (F2) — PHASE 5 REPORT
## CANONICAL WEATHER & H3 SPATIAL REST APIs WITH CONTRACT VERIFICATION

**Feature**: F2 — Weather Integration + Centralized H3 Spatial Layer  
**Phase**: Phase 5 (Canonical Weather & H3 REST APIs + Contract Hardening)  
**Execution Timestamp**: 2026-09-26T08:25:00+05:30  
**Phase Status**: **PASS**  

---

## 1. Canonical REST API Catalog

The four canonical endpoints required for Feature 2 have been fully implemented, secured, and verified:

| Canonical Endpoint | HTTP Method | Target Resource | Description |
| :--- | :--- | :--- | :--- |
| `/api/v1/cities/{cityId}/weather/latest` | `GET` | Latest City Weather | Returns the newest real weather observation for a validated city. |
| `/api/v1/grid` | `GET` | City H3 Grid Cells | Returns all database-backed resolution 8 cells for a city. |
| `/api/v1/grid/{h3Index}` | `GET` | H3 Cell Details | Returns spatial boundaries and centroid for a persisted H3 cell. |
| `/api/v1/grid/{h3Index}/observations` | `GET` | Cell Observations | Returns chronologically ascending air and weather observations for a cell. |

---

## 2. HTTP Methods & Endpoints Specification

1. **`GET /api/v1/cities/{cityId}/weather/latest`**:
   - Controller: [`CityController.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/city/CityController.java)
   - Service: [`WeatherService.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/weather/WeatherService.java)
   - Produces: `application/json`

2. **`GET /api/v1/grid`**:
   - Controller: [`GridController.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/grid/GridController.java)
   - Service: [`GridService.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/grid/GridService.java)
   - Produces: `application/json`

3. **`GET /api/v1/grid/{h3Index}`**:
   - Controller: [`GridController.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/grid/GridController.java)
   - Service: [`GridService.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/grid/GridService.java)
   - Produces: `application/json`

4. **`GET /api/v1/grid/{h3Index}/observations`**:
   - Controller: [`GridController.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/grid/GridController.java)
   - Service: [`GridService.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/grid/GridService.java)
   - Produces: `application/json`

---

## 3. Request Parameters & Constraints

| Endpoint | Parameter | Location | Type | Constraints / Validation |
| :--- | :--- | :--- | :--- | :--- |
| `/api/v1/cities/{cityId}/weather/latest` | `cityId` | Path | `UUID` | Standard UUID format; must exist in `cities` table. |
| `/api/v1/grid` | `cityId` | Query | `UUID` | Required query parameter; standard UUID format; must exist in `cities` table. |
| `/api/v1/grid/{h3Index}` | `h3Index` | Path | `String` | 15-character hex string; validated via `H3Service.validateH3Index()`; must exist in `grid_cells`. |
| `/api/v1/grid/{h3Index}/observations` | `h3Index` | Path | `String` | 15-character hex string; validated via `H3Service.validateH3Index()`; must exist in `grid_cells`. |

---

## 4. Response DTO Architecture

No JPA entities are leaked across the API boundary. All responses are encapsulated in strict Data Transfer Objects:

### A. [`WeatherLatestResponse.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/dto/weather/WeatherLatestResponse.java)
```json
{
  "cityId": "550e8400-e29b-41d4-a716-446655440001",
  "cityName": "Pune",
  "temperature": 24.0,
  "humidity": 79.0,
  "windSpeed": 15.5,
  "windDirection": 261.0,
  "rainfall": 0.0,
  "observedAt": "2026-09-26T02:30:00Z",
  "source": "OPEN_METEO",
  "h3Index": "88608850e5fffff"
}
```

### B. [`GridCellResponse.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/dto/grid/GridCellResponse.java)
```json
{
  "h3Index": "88608850e5fffff",
  "cityId": "550e8400-e29b-41d4-a716-446655440001",
  "resolution": 8,
  "center": {
    "lat": 18.53153430386518,
    "lng": 73.84714485054492
  },
  "boundary": [
    { "lat": 18.52933396493297, "lng": 73.8512303644301 },
    { "lat": 18.534124979067755, "lng": 73.85125167715375 },
    { "lat": 18.536325136234492, "lng": 73.84716609485328 },
    { "lat": 18.533734410653807, "lng": 73.84305949953044 },
    { "lat": 18.5289436509364, "lng": 73.84303832433693 },
    { "lat": 18.526743362382277, "lng": 73.84712360694361 }
  ]
}
```

### C. [`GridCellObservationResponse.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/dto/grid/GridCellObservationResponse.java)
```json
{
  "h3Index": "88608850e5fffff",
  "cityId": "550e8400-e29b-41d4-a716-446655440001",
  "airObservations": [
    {
      "id": "880e8400-e29b-41d4-a716-446655440001",
      "stationId": "PUN-001",
      "stationName": "Shivajinagar CAAQMS",
      "pm25": 72.0,
      "observedAt": "2026-09-24T00:00:00Z",
      "source": "CPCB",
      "quality": "VALID",
      "h3Index": "88608850e5fffff"
    }
  ],
  "weatherObservations": [
    {
      "id": "236144fc-b6ab-4218-8b89-f41ab456df32",
      "temperature": 23.1,
      "humidity": 90.0,
      "windSpeed": 10.8,
      "windDirection": 266.0,
      "rainfall": 0.0,
      "observedAt": "2026-09-24T17:30:00Z",
      "source": "OPEN_METEO",
      "h3Index": "88608850e5fffff"
    }
  ]
}
```

---

## 5. Input Validation Behavior

Input validation is enforced systematically before any database interaction:

1. **City UUID Validation**:
   - Malformed UUID (e.g. `GET /api/v1/cities/malformed-id/weather/latest` or `GET /api/v1/grid?cityId=abc`):
     Caught by `MethodArgumentTypeMismatchException` $\to$ Returns **HTTP 400 BAD_REQUEST**.
   - Valid UUID syntax but non-existent city in database:
     Triggers `ResourceNotFoundException("City not found with id: ...")` $\to$ Returns **HTTP 404 NOT_FOUND**.

2. **H3 Spatial Index Validation**:
   - Syntactically malformed H3 (e.g. `GET /api/v1/grid/not-an-h3`):
     Evaluated via native `H3Service.validateH3Index()`. Throws `IllegalArgumentException("Invalid H3 index format: ...")` $\to$ Returns **HTTP 400 BAD_REQUEST**.
   - Mathematically valid H3 index not present in `grid_cells` (e.g. `882681a339fffff`):
     Triggers `ResourceNotFoundException("H3 grid cell not found: ...")` $\to$ Returns **HTTP 404 NOT_FOUND**.

---

## 6. Error Response Contract

All error responses strictly conform to the existing project `GlobalExceptionHandler` contract:

```json
{
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Invalid parameter value for 'cityId': malformed-uuid",
  "timestamp": "2026-09-26T02:49:09.958118300Z"
}
```

```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "City not found with id: 00000000-0000-0000-0000-000000000000",
  "timestamp": "2026-09-26T02:49:10.012531800Z"
}
```

---

## 7. No-Data Handling Behavior

Under no circumstances are mock or placeholder values injected:

1. **Weather No-Data**:
   If a valid city exists in `cities` but has no recorded weather observations:
   - Returns **HTTP 200 OK** with:
     ```json
     {
       "cityId": "...",
       "cityName": "..."
     }
     ```
   - All meteorological metrics (`temperature`, `humidity`, etc.) remain `null`.

2. **Grid No-Data**:
   If a valid city exists but has no registered grid cells:
   - Returns **HTTP 200 OK** with empty list `[]`.

3. **Cell Observations No-Data**:
   If a valid H3 cell exists in `grid_cells` but has no recorded air or weather observations:
   - Returns **HTTP 200 OK** with:
     ```json
     {
       "h3Index": "...",
       "cityId": "...",
       "airObservations": [],
       "weatherObservations": []
     }
     ```

---

## 8. Security Architecture & Endpoint Access Policy

- **Spring Security Configuration**: Configured in [`SecurityConfig.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/config/SecurityConfig.java).
- **Public Endpoints**: `/api/v1/cities/**`, `/api/v1/weather/**`, `/api/v1/grid`, and `/api/v1/grid/**` are explicitly permitted for read-only public consumption matching the F1 contract.
- **CSRF**: Disabled for stateless REST.
- **CORS**: Active and configured via `CorsConfig` allowing cross-origin requests from the React frontend.
- **Credentials Protected**: No database internals, database connection strings, or provider secrets are exposed.

---

## 9. Database Query Strategy & Performance

To guarantee sub-10ms response times without N+1 overhead or unbounded in-memory filtering:

1. **Latest Weather Query**:
   - Direct indexed query: `findFirstByCityIdOrderByObservedAtDesc(UUID cityId)`.
   - Utilizes B-tree index `idx_weather_obs_city_time(city_id, observed_at DESC)`.
   - Generates single SQL statement:
     ```sql
     SELECT * FROM weather_observations WHERE city_id = ? ORDER BY observed_at DESC LIMIT 1;
     ```

2. **City Grid Cells Query**:
   - Direct query: `gridRepository.findByCityId(cityId)`.
   - Utilizes foreign key index on `grid_cells.city_id`.
   - Computes Leaflet-compatible boundary coordinates dynamically from native H3 geometry.

3. **Cell Observations Query**:
   - Air: `airObservationRepository.findByH3IndexOrderByObservedAtAsc(h3Index)`.
     Utilizes B-tree index `idx_air_obs_h3_time(h3_index, observed_at DESC)`.
   - Weather: `weatherRepository.findByH3IndexOrderByObservedAtAsc(h3Index)`.
     Utilizes B-tree index `idx_weather_obs_h3_time(h3_index, observed_at DESC)`.
   - Station Names: Station names are mapped in a single $O(1)$ dictionary lookup from `sensorRepository.findByCityId(cityId)` without N+1 queries.

---

## 10. Latest Weather Verification

The latest weather API output was verified against the absolute newest records in PostgreSQL for all three cities:

| City | Newest DB `observed_at` | API `observed_at` | DB Temp / Humidity / Wind | API Temp / Humidity / Wind | Match Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Pune** | `2026-09-26 02:30:00+00` | `2026-09-26T02:30:00Z` | 24.0°C / 79% / 15.5 km/h | 24.0°C / 79% / 15.5 km/h | **100% EXACT** |
| **Mumbai** | `2026-09-26 02:30:00+00` | `2026-09-26T02:30:00Z` | 28.3°C / 71% / 10.1 km/h | 28.3°C / 71% / 10.1 km/h | **100% EXACT** |
| **Delhi** | `2026-09-26 02:30:00+00` | `2026-09-26T02:30:00Z` | 25.8°C / 75% / 4.3 km/h | 25.8°C / 75% / 4.3 km/h | **100% EXACT** |

---

## 11. Grid Cells Verification

| City | DB `grid_cells` Count | API `GET /api/v1/grid` Count | Duplicate Cells | Centroid Present | 6 Perimeter Vertices |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Pune** | 3 | 3 | 0 | Yes (`18.5315, 73.8471`) | Yes (6 `LatLngPoint` vertices) |
| **Mumbai** | 2 | 2 | 0 | Yes (`19.0823, 72.8876`) | Yes (6 `LatLngPoint` vertices) |
| **Delhi** | 3 | 3 | 0 | Yes (`28.5641, 77.1873`) | Yes (6 `LatLngPoint` vertices) |
| **Total** | **8** | **8** | **0** | **100% Real** | **100% Real H3 Polygons** |

---

## 12. Cell Observations Verification

Sample real H3 cells were cross-compared between PostgreSQL and `GET /api/v1/grid/{h3Index}/observations`:

- **Sample Cell**: `88608850e5fffff` (Pune Shivajinagar):
  - DB Air Count: 12 observations
  - API Air Count: 12 observations
  - DB Weather Count: 34 observations
  - API Weather Count: 34 observations
  - Temporal Order: **Strictly Ascending** (`2026-09-24T00:00:00Z` $\to$ `2026-09-26T02:30:00Z`)
  - Provenance: Air (`CPCB`), Weather (`OPEN_METEO`)

---

## 13. Direct Database vs API Field Comparison

| Field | Database Value (`88608850e5fffff`) | API Response Value (`88608850e5fffff`) | Discrepancy |
| :--- | :--- | :--- | :--- |
| `h3Index` | `88608850e5fffff` | `88608850e5fffff` | None |
| `cityId` | `550e8400-e29b-41d4-a716-446655440001` | `550e8400-e29b-41d4-a716-446655440001` | None |
| `airObservations[0].stationId` | `PUN-001` | `PUN-001` | None |
| `airObservations[0].pm25` | `72.0` | `72.0` | None |
| `airObservations[0].observedAt` | `2026-09-24 00:00:00+00` | `2026-09-24T00:00:00Z` | None |
| `weatherObservations[0].temperature` | `23.1` | `23.1` | None |
| `weatherObservations[0].windSpeed` | `10.8` | `10.8` | None |
| `weatherObservations[0].source` | `OPEN_METEO` | `OPEN_METEO` | None |

---

## 14. Real Live HTTP Evidence (Pune, Mumbai, Delhi)

Actual live HTTP responses captured from the running backend server on port 8080:

### Pune Latest Weather
```http
HTTP/1.1 200 
Content-Type: application/json

{
  "cityId": "550e8400-e29b-41d4-a716-446655440001",
  "cityName": "Pune",
  "temperature": 24.0,
  "humidity": 79.0,
  "windSpeed": 15.5,
  "windDirection": 261.0,
  "rainfall": 0.0,
  "observedAt": "2026-09-26T02:30:00Z",
  "source": "OPEN_METEO",
  "h3Index": "88608850e5fffff"
}
```

### Mumbai Latest Weather
```http
HTTP/1.1 200 
Content-Type: application/json

{
  "cityId": "550e8400-e29b-41d4-a716-446655440002",
  "cityName": "Mumbai",
  "temperature": 28.3,
  "humidity": 71.0,
  "windSpeed": 10.1,
  "windDirection": 268.0,
  "rainfall": 0.2,
  "observedAt": "2026-09-26T02:30:00Z",
  "source": "OPEN_METEO",
  "h3Index": "88608b54d7fffff"
}
```

### Delhi Latest Weather
```http
HTTP/1.1 200 
Content-Type: application/json

{
  "cityId": "550e8400-e29b-41d4-a716-446655440003",
  "cityName": "Delhi",
  "temperature": 25.8,
  "humidity": 75.0,
  "windSpeed": 4.3,
  "windDirection": 312.0,
  "rainfall": 0.0,
  "observedAt": "2026-09-26T02:30:00Z",
  "source": "OPEN_METEO",
  "h3Index": "883da1149bfffff"
}
```

### Error Contracts
- **Malformed City UUID**:
  ```http
  HTTP/1.1 400 
  {"error":"BAD_REQUEST","message":"Invalid parameter value for 'cityId': malformed-uuid","status":400}
  ```
- **Unknown City UUID**:
  ```http
  HTTP/1.1 404 
  {"error":"NOT_FOUND","message":"City not found with id: 00000000-0000-0000-0000-000000000000","status":404}
  ```
- **Malformed H3 String**:
  ```http
  HTTP/1.1 400 
  {"error":"BAD_REQUEST","message":"Invalid H3 index format: malformed-h3","status":400}
  ```
- **Unknown Valid H3 Cell**:
  ```http
  HTTP/1.1 404 
  {"error":"NOT_FOUND","message":"H3 grid cell not found: 882681a339fffff","status":404}
  ```

---

## 15. Automated Test Suite Execution Results

All 29 newly implemented contract tests ran and passed with zero failures:

```
[INFO] Running com.aerosentinel.grid.F2CellObservationApiContractTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 28.02 s -- in com.aerosentinel.grid.F2CellObservationApiContractTest
[INFO] Running com.aerosentinel.grid.F2GridApiContractTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.400 s -- in com.aerosentinel.grid.F2GridApiContractTest
[INFO] Running com.aerosentinel.weather.F2WeatherApiContractTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.307 s -- in com.aerosentinel.weather.F2WeatherApiContractTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

Full backend test suite execution:
```
[INFO] Results:
[INFO] 
[WARNING] Tests run: 134, Failures: 0, Errors: 0, Skipped: 1
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```
- **Total Tests**: **134**
- **Failures**: **0**
- **Errors**: **0**
- **Skipped**: **1** (`LiveOpenAqIngestionVerificationTest` conditionally skipped unless `OPENAQ_API_KEY` is provided)

---

## 16. F1 Regression Verification

All foundational Feature 1 endpoints were tested live via curl and in regression unit tests:

1. **`GET /api/v1/cities`**: Returns HTTP 200 with 3 active cities (Pune, Mumbai, Delhi).
2. **`GET /api/v1/cities/{id}/air-quality/latest`**: Returns HTTP 200 with active stations and latest PM2.5 readings.
3. **`GET /api/v1/stations/{stationId}/air-quality?from=...&to=...`**: Returns HTTP 200 with time-series historical observations.
4. **Air Observations Count**: Exactly **166 original F1 records** intact with zero null H3 indexes.

---

## 17. No-Mock Backend Source Code Audit

A comprehensive grep audit across `backend/src/main/java` verified that:
- `mockWeather` / `fakeWeather`: **0 occurrences**
- `Math.random`: **0 occurrences**
- `coordinatesToMockH3`: **0 occurrences**
- `generateSynthetic`: **0 occurrences**
- Hardcoded response metrics: **0 occurrences**
- All API data originates strictly from PostgreSQL and the native Uber H3 Core engine.

---

## 18. Files Changed in Phase 5

### Created Files:
1. `backend/src/main/java/com/aerosentinel/dto/weather/WeatherLatestResponse.java`
2. `backend/src/main/java/com/aerosentinel/dto/grid/LatLngPoint.java`
3. `backend/src/main/java/com/aerosentinel/dto/grid/GridCellResponse.java`
4. `backend/src/main/java/com/aerosentinel/dto/grid/GridAirObservationResponse.java`
5. `backend/src/main/java/com/aerosentinel/dto/grid/GridWeatherObservationResponse.java`
6. `backend/src/main/java/com/aerosentinel/dto/grid/GridCellObservationResponse.java`
7. `backend/src/test/java/com/aerosentinel/weather/F2WeatherApiContractTest.java`
8. `backend/src/test/java/com/aerosentinel/grid/F2GridApiContractTest.java`
9. `backend/src/test/java/com/aerosentinel/grid/F2CellObservationApiContractTest.java`
10. `docs/F2_PHASE_5_API_CONTRACT_REPORT.md`

### Modified Files:
1. `backend/src/main/java/com/aerosentinel/config/SecurityConfig.java` (added `/api/v1/grid` explicitly to permitted endpoints)
2. `backend/src/main/java/com/aerosentinel/weather/WeatherRepository.java` (added `findByH3IndexOrderByObservedAtAsc`)
3. `backend/src/main/java/com/aerosentinel/air/AirObservationRepository.java` (added `findByH3IndexOrderByObservedAtAsc`)
4. `backend/src/main/java/com/aerosentinel/weather/WeatherService.java` (added `getLatestWeatherForCity`)
5. `backend/src/main/java/com/aerosentinel/grid/GridService.java` (added `getGridCellsForCity`, `getGridCellByH3`, `getCellObservations`, and DTO mappers)
6. `backend/src/main/java/com/aerosentinel/city/CityController.java` (added `GET /api/v1/cities/{cityId}/weather/latest`)
7. `backend/src/main/java/com/aerosentinel/grid/GridController.java` (migrated to canonical DTOs and added `/observations`)
8. `backend/src/test/java/com/aerosentinel/integration/openaq/LiveOpenAqIngestionVerificationTest.java` (enhanced test cleanup)

---

## 19. Files Untouched

As mandated by project constraints, **ZERO frontend UI files were modified**:
- `frontend/src/pages/public/WeatherSpatial.tsx`: **UNTOUCHED**
- `frontend/src/pages/public/Dashboard.tsx`: **UNTOUCHED**
- `frontend/src/components/map/PollutionMap.tsx`: **UNTOUCHED**
- `frontend/src/components/map/H3RiskLayer.tsx`: **UNTOUCHED**
- `frontend/src/components/layout/Navbar.tsx`: **UNTOUCHED**
- All other React components and CSS files: **UNTOUCHED**

---

## 20. Phase 6 Frontend Integration Dependencies

When transitioning to **Phase 6 (Frontend Integration & Cleanup)**, the React frontend can immediately wire directly into these four canonical contracts:

1. **Weather Card / Summary Widget**:
   `GET /api/v1/cities/{cityId}/weather/latest` $\to$ updates `weather` in `AppContext` with real physical temperature, humidity, wind vector, rainfall, and provider timestamp.
2. **Spatial Grid Polygon Layer**:
   `GET /api/v1/grid?cityId={cityId}` $\to$ renders real database-backed resolution 8 hexagons via `boundary` (`LatLngPoint[]`) directly on Leaflet map, eliminating frontend `gridDisk` mock generation.
3. **Hexagon Cell Selection Inspector**:
   `GET /api/v1/grid/{h3Index}` $\to$ retrieves cell metadata, resolution, and centroid.
4. **Cell Observations & Dual-Axis History Chart**:
   `GET /api/v1/grid/{h3Index}/observations` $\to$ feeds the time-series charts with paired PM2.5 and meteorological measurements sorted in chronological ascending order.

---

## Sign-off Recommendation

All four canonical endpoints are functional, validated, performant, and verified against PostgreSQL and live HTTP calls with zero mock data and zero F1 regressions.

**Phase 5 Status**: **LOCKED PASS — READY FOR PHASE 6 (FRONTEND INTEGRATION & POLISH)**.
