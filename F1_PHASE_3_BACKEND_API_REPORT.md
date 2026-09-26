# F1 PHASE 3 — BACKEND DOMAIN + API LAYER REPORT
**Project:** AeroSentinel  
**Feature:** Feature 1 (F1 — City + Air Quality Foundation)  
**Phase:** Phase 3 — Backend Domain + API Layer  
**Status:** **PASS**

---

## 1. Objective
Transform the verified Phase 2 PostgreSQL database foundation into a production-grade, secure, frontend-ready Spring Boot API layer for Feature 1. Expose canonical REST endpoints for municipal air quality without synthetic calculations, ensure strict date-range validation and structured error handling, enforce DTO encapsulation, resolve route authorization issues, and thoroughly prove the implementation with both unit and integration tests and actual HTTP verification.

---

## 2. Initial Repository State
Prior to Phase 3 implementation, inspection of the backend repository revealed:
- **Database**: PostgreSQL 16 + PostGIS with Flyway V5 applied, containing 3 cities, 3 Pune monitoring stations (`PUN-001`, `PUN-002`, `PUN-003`), and 36 deterministic 24-hour development seed observations (12 per station).
- **Existing Entities**: `City`, `MonitoringStation`, and `AirObservation` mapped via JPA.
- **DTOs**: `com.aerosentinel.dto.air` and `com.aerosentinel.dto.city` were empty directories; existing controllers directly returned JPA entities.
- **Controllers**:
  - `CityController`: handled `/api/v1/cities` and `/api/v1/cities/{id}`, returning `City` entities.
  - `SensorController`: handled `/api/v1/sensors`, returning `MonitoringStation` entities.
  - `AirController`: handled non-canonical `/api/v1/air/current` and `/api/v1/air/observations`.
  - No `StationController` or canonical station history endpoint existed.
- **Security**: `SecurityConfig` permitted `/api/v1/cities/**` and `/api/v1/air/**`, but omitted `/api/v1/stations/**` and `/api/v1/sensors/**`, causing HTTP 403 Forbidden errors.
- **Exception Handling**: `GlobalExceptionHandler` only handled `ResourceNotFoundException`, `ValidationException`, and generic `Exception`. It lacked handlers for parameter omission and type mismatches.

---

## 3. Problems Identified
1. **Missing Canonical API Contracts**:
   - `GET /api/v1/cities/{cityId}/air-quality/latest` was missing.
   - `GET /api/v1/stations/{stationId}/air-quality?from=...&to=...` was missing.
2. **Security 403 Forbidden on Station/Sensor Routes**:
   - `/api/v1/stations/**` and `/api/v1/sensors/**` were absent from `permitAll()`.
3. **JPA Entity Leakage**:
   - Controllers returned JPA entities directly, risking accidental schema exposure.
4. **Missing Repository Queries**:
   - No explicit Spring Data JPA query for single latest observation per station.
   - No explicit query for date-range bounded historical observations.
   - No method to look up a station by station code (`stationCode`).
5. **Incomplete Error Handling**:
   - Missing query parameters or malformed timestamps resulted in unhandled exceptions or 500 errors instead of clean 400 Bad Request responses.

---

## 4. Requirements Implemented
1. **DTO Layer**: Implemented `CityResponse`, `AirQualityObservationResponse`, `LatestAirQualityResponse`, `HistoricalObservationResponse`, and `AirQualityHistoryResponse`.
2. **Repository Enhancement**:
   - `SensorRepository`: Added `findByStationCode`, `existsByStationCode`, and `findByCityIdAndStatus`.
   - `AirObservationRepository`: Added `findFirstByStationIdOrderByObservedAtDesc` and `findByStationIdAndObservedAtBetweenOrderByObservedAtAsc`.
3. **Service Logic**:
   - `AirService.getLatestAirQualityForCity`: Validates city existence, finds active stations, retrieves latest observation per station, maps to DTOs.
   - `AirService.getStationAirQualityHistory`: Validates station existence, validates ISO-8601 timestamps and range constraints (`from <= to`), queries indexed repository, maps to DTOs.
   - `CityService`: Added `getAllActiveCityResponses` and `getCityResponseById` returning DTOs.
4. **REST Controllers**:
   - `CityController`: Enhanced with `GET /api/v1/cities/{cityId}/air-quality/latest`.
   - `StationController`: Created canonical `GET /api/v1/stations/{stationId}/air-quality`.
5. **Security Configuration**: Permitted `/api/v1/stations/**` and `/api/v1/sensors/**`.
6. **Exception Handling**: Added handlers for `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException`, `IllegalArgumentException`, `ValidationException`, and `ResourceNotFoundException`.
7. **Zero Synthetic Data**: Every single observation value is fetched directly from PostgreSQL.

---

## 5. Files Changed

| File Path | Description of Changes |
| :--- | :--- |
| `backend/src/main/java/com/aerosentinel/dto/city/CityResponse.java` | Created City response DTO |
| `backend/src/main/java/com/aerosentinel/dto/air/AirQualityObservationResponse.java` | Created single observation DTO |
| `backend/src/main/java/com/aerosentinel/dto/air/LatestAirQualityResponse.java` | Created city latest air quality response DTO |
| `backend/src/main/java/com/aerosentinel/dto/air/HistoricalObservationResponse.java` | Created station historical item DTO |
| `backend/src/main/java/com/aerosentinel/dto/air/AirQualityHistoryResponse.java` | Created station history container DTO |
| `backend/src/main/java/com/aerosentinel/sensor/SensorRepository.java` | Added `findByStationCode` and `findByCityIdAndStatus` |
| `backend/src/main/java/com/aerosentinel/air/AirObservationRepository.java` | Added `findFirstByStationIdOrderByObservedAtDesc` and `findByStationIdAndObservedAtBetweenOrderByObservedAtAsc` |
| `backend/src/main/java/com/aerosentinel/city/CityService.java` | Added DTO mapping methods for city responses |
| `backend/src/main/java/com/aerosentinel/air/AirService.java` | Implemented canonical latest air and station history business logic |
| `backend/src/main/java/com/aerosentinel/city/CityController.java` | Added DTO responses and `/{cityId}/air-quality/latest` endpoint |
| `backend/src/main/java/com/aerosentinel/sensor/StationController.java` | Created controller with `/{stationId}/air-quality` endpoint |
| `backend/src/main/java/com/aerosentinel/config/SecurityConfig.java` | Added `/api/v1/stations/**` and `/api/v1/sensors/**` to permitted routes |
| `backend/src/main/java/com/aerosentinel/exception/GlobalExceptionHandler.java` | Added handlers for missing params and type mismatches |
| `backend/src/test/java/com/aerosentinel/city/CityServiceUnitTest.java` | Created unit tests for City service |
| `backend/src/test/java/com/aerosentinel/air/AirServiceUnitTest.java` | Created unit tests for Air service |
| `backend/src/test/java/com/aerosentinel/air/AirQualityIntegrationTest.java` | Created Spring Boot MockMvc integration tests |

---

## 6. Files NOT Changed
- `backend/src/main/resources/db/migration/V5__f1_station_indexes_and_seed_observations.sql`: Preserved intact; no new migration required.
- All frontend source files: Strictly excluded from Phase 3 scope.
- Unrelated backend domain modules (`weather`, `fire`, `satellite`, `grid`, `hotspot`, `forecast`, `citizen`, `alert`, `inspection`, `action`, `federated`): Strictly untouched.

---

## 7. DTO Architecture
DTOs cleanly decouple domain persistence models from external API contracts:
- **`CityResponse`**: Returns `id`, `name`, `state`, `country`, `timezone`, `latitude`, `longitude`, `active`, `createdAt`.
- **`AirQualityObservationResponse`**: Returns `stationId`, `stationName`, `pm25`, `observedAt`, `source`, `quality`.
- **`LatestAirQualityResponse`**: Returns `cityId`, `cityName`, `observations` (`List<AirQualityObservationResponse>`).
- **`HistoricalObservationResponse`**: Returns `pm25`, `observedAt`, `source`, `quality`.
- **`AirQualityHistoryResponse`**: Returns `stationId`, `stationName`, `observations` (`List<HistoricalObservationResponse>`).

---

## 8. Service Architecture
- Controllers remain thin HTTP routing delegates.
- **`CityService`**: Performs city lookups and entity-to-DTO conversion.
- **`AirService`**:
  - Handles validation for city existence and monitoring station existence.
  - Handles date-range validation and parsing.
  - Coordinates database retrieval and DTO transformation.
  - Returns controlled empty lists when no records match a valid query range.

---

## 9. Repository Changes
Added specialized query methods leveraging Phase 2 composite indexes:
- `findFirstByStationIdOrderByObservedAtDesc(String stationId)`: Uses `idx_air_obs_station_time` with `LIMIT 1`.
- `findByStationIdAndObservedAtBetweenOrderByObservedAtAsc(String stationId, Instant from, Instant to)`: Uses `idx_air_obs_station_time` range filter and sort.
- `findByStationCode(String stationCode)`: Performs indexed unique station code lookup.
- `findByCityIdAndStatus(UUID cityId, String status)`: Filters active stations for municipal rollups.

---

## 10. Security Changes
Updated `SecurityConfig.java` to grant public read access to Feature 1 endpoints:
```java
.requestMatchers(
    "/api/v1/health",
    "/api/v1/cities/**",
    "/api/v1/stations/**",
    "/api/v1/sensors/**",
    "/api/v1/air/**",
    ...
).permitAll()
```
Global bypasses (`permitAll("/**")`) were **strictly avoided**. All other routes remain authenticated.

---

## 11. API Contracts

### 11.1 City List
- **Endpoint**: `GET /api/v1/cities`
- **Status**: `200 OK`
- **Response**: Array of active municipal regions (`Pune`, `Mumbai`, `Delhi`).

### 11.2 City Details
- **Endpoint**: `GET /api/v1/cities/{cityId}`
- **Status**: `200 OK` (or `404 Not Found`)

### 11.3 Latest Air Quality for City
- **Endpoint**: `GET /api/v1/cities/{cityId}/air-quality/latest`
- **Status**: `200 OK` (or `404 Not Found`)
- **Structure**:
  ```json
  {
    "cityId": "550e8400-e29b-41d4-a716-446655440001",
    "cityName": "Pune",
    "observations": [
      {
        "stationId": "PUN-001",
        "stationName": "Shivajinagar CAAQMS",
        "pm25": 78.0,
        "observedAt": "2026-09-24T22:00:00Z",
        "source": "CPCB",
        "quality": "VALID"
      }
    ]
  }
  ```

### 11.4 Station Historical Air Quality
- **Endpoint**: `GET /api/v1/stations/{stationId}/air-quality?from={from}&to={to}`
- **Parameters**: `from` (ISO-8601 UTC), `to` (ISO-8601 UTC)
- **Status**: `200 OK` (or `400 Bad Request`, `404 Not Found`)
- **Structure**:
  ```json
  {
    "stationId": "PUN-001",
    "stationName": "Shivajinagar CAAQMS",
    "observations": [
      {
        "pm25": 72.0,
        "observedAt": "2026-09-24T00:00:00Z",
        "source": "CPCB",
        "quality": "VALID"
      }
    ]
  }
  ```

---

## 12. Request Examples
- `curl -s http://localhost:8080/api/v1/cities`
- `curl -s http://localhost:8080/api/v1/cities/550e8400-e29b-41d4-a716-446655440001/air-quality/latest`
- `curl -s "http://localhost:8080/api/v1/stations/PUN-001/air-quality?from=2026-09-24T00:00:00Z&to=2026-09-24T23:59:59Z"`

---

## 13. Actual Response Examples (Captured via Live HTTP)

### Latest Air Quality for Pune
```json
{
  "cityId": "550e8400-e29b-41d4-a716-446655440001",
  "cityName": "Pune",
  "observations": [
    {
      "stationId": "PUN-001",
      "stationName": "Shivajinagar CAAQMS",
      "pm25": 78.0,
      "observedAt": "2026-09-24T22:00:00Z",
      "source": "CPCB",
      "quality": "VALID"
    },
    {
      "stationId": "PUN-002",
      "stationName": "Katraj Air Station",
      "pm25": 62.0,
      "observedAt": "2026-09-24T22:00:00Z",
      "source": "MPCB",
      "quality": "VALID"
    },
    {
      "stationId": "PUN-003",
      "stationName": "Hadapsar Industrial Zone",
      "pm25": 86.0,
      "observedAt": "2026-09-24T22:00:00Z",
      "source": "MPCB",
      "quality": "VALID"
    }
  ]
}
```

### Full-Day History for PUN-001 (12 Observations)
```json
{
  "stationId": "PUN-001",
  "stationName": "Shivajinagar CAAQMS",
  "observations": [
    {"pm25": 72.0, "observedAt": "2026-09-24T00:00:00Z", "source": "CPCB", "quality": "VALID"},
    {"pm25": 95.0, "observedAt": "2026-09-24T02:00:00Z", "source": "CPCB", "quality": "VALID"},
    {"pm25": 118.0, "observedAt": "2026-09-24T04:00:00Z", "source": "CPCB", "quality": "VALID"},
    {"pm25": 104.0, "observedAt": "2026-09-24T06:00:00Z", "source": "CPCB", "quality": "VALID"},
    {"pm25": 82.0, "observedAt": "2026-09-24T08:00:00Z", "source": "CPCB", "quality": "VALID"},
    {"pm25": 76.0, "observedAt": "2026-09-24T10:00:00Z", "source": "CPCB", "quality": "VALID"},
    {"pm25": 92.0, "observedAt": "2026-09-24T12:00:00Z", "source": "CPCB", "quality": "VALID"},
    {"pm25": 122.0, "observedAt": "2026-09-24T14:00:00Z", "source": "CPCB", "quality": "VALID"},
    {"pm25": 112.0, "observedAt": "2026-09-24T16:00:00Z", "source": "CPCB", "quality": "VALID"},
    {"pm25": 98.0, "observedAt": "2026-09-24T18:00:00Z", "source": "CPCB", "quality": "VALID"},
    {"pm25": 86.0, "observedAt": "2026-09-24T20:00:00Z", "source": "CPCB", "quality": "VALID"},
    {"pm25": 78.0, "observedAt": "2026-09-24T22:00:00Z", "source": "CPCB", "quality": "VALID"}
  ]
}
```

---

## 14. Error Behavior (Verified via Live HTTP)

| Scenario | Request | HTTP Code | Error Response Body |
| :--- | :--- | :--- | :--- |
| **Invalid City ID** | `GET /api/v1/cities/00000000-0000-0000-0000-000000000000` | **404** | `{"status":404,"error":"NOT_FOUND","message":"City not found with id: 00000000-0000-0000-0000-000000000000"}` |
| **Invalid Station Code** | `GET /api/v1/stations/INVALID-CODE/air-quality?from=...&to=...` | **404** | `{"status":404,"error":"NOT_FOUND","message":"Monitoring station not found with code: INVALID-CODE"}` |
| **Inverted Date Range** | `GET /api/v1/stations/PUN-001/air-quality?from=2026-09-25...&to=2026-09-24...` | **400** | `{"status":400,"error":"VALIDATION_ERROR","message":"Invalid date range: 'from' (2026-09-25T00:00:00Z) must not be after 'to' (2026-09-24T00:00:00Z)"}` |
| **Malformed Date** | `GET /api/v1/stations/PUN-001/air-quality?from=invalid-date&to=2026-09-24...` | **400** | `{"status":400,"error":"VALIDATION_ERROR","message":"Invalid 'from' timestamp format: 'invalid-date'. Expected ISO-8601 UTC format (e.g. 2026-09-24T00:00:00Z)"}` |
| **Missing Parameter** | `GET /api/v1/stations/PUN-001/air-quality` | **400** | `{"status":400,"error":"BAD_REQUEST","message":"Required parameter 'from' is missing"}` |
| **No-Data Range** | `GET /api/v1/stations/PUN-001/air-quality?from=2020-01-01...&to=2020-01-02...` | **200** | `{"stationId":"PUN-001","stationName":"Shivajinagar CAAQMS","observations":[]}` |

---

## 15. Date/Time Handling
- All internal dates use Java `Instant` representing UTC timestamps.
- Dates are parsed from ISO-8601 UTC strings (`YYYY-MM-DDTHH:mm:ssZ`).
- Jackson's `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS` is disabled via [JacksonConfig.java](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/config/JacksonConfig.java), guaranteeing compliant ISO-8601 strings in all JSON outputs.

---

## 16. Database/API Cross-Verification

Direct comparison between PostgreSQL `air_observations` query and API output for `PUN-001`:

```sql
SELECT station_id, observed_at, pm25, source, quality
FROM air_observations
WHERE station_id = 'PUN-001'
ORDER BY observed_at ASC;
```

| Record # | DB station_id | DB observed_at | DB pm25 | DB source | DB quality | API observedAt | API pm25 | API source | API quality | Match? |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| 1 | PUN-001 | 2026-09-24 00:00:00+00 | 72.0 | CPCB | VALID | 2026-09-24T00:00:00Z | 72.0 | CPCB | VALID | **EXACT** |
| 2 | PUN-001 | 2026-09-24 02:00:00+00 | 95.0 | CPCB | VALID | 2026-09-24T02:00:00Z | 95.0 | CPCB | VALID | **EXACT** |
| 3 | PUN-001 | 2026-09-24 04:00:00+00 | 118.0 | CPCB | VALID | 2026-09-24T04:00:00Z | 118.0 | CPCB | VALID | **EXACT** |
| 4 | PUN-001 | 2026-09-24 06:00:00+00 | 104.0 | CPCB | VALID | 2026-09-24T06:00:00Z | 104.0 | CPCB | VALID | **EXACT** |
| 5 | PUN-001 | 2026-09-24 08:00:00+00 | 82.0 | CPCB | VALID | 2026-09-24T08:00:00Z | 82.0 | CPCB | VALID | **EXACT** |
| 6 | PUN-001 | 2026-09-24 10:00:00+00 | 76.0 | CPCB | VALID | 2026-09-24T10:00:00Z | 76.0 | CPCB | VALID | **EXACT** |
| 7 | PUN-001 | 2026-09-24 12:00:00+00 | 92.0 | CPCB | VALID | 2026-09-24T12:00:00Z | 92.0 | CPCB | VALID | **EXACT** |
| 8 | PUN-001 | 2026-09-24 14:00:00+00 | 122.0 | CPCB | VALID | 2026-09-24T14:00:00Z | 122.0 | CPCB | VALID | **EXACT** |
| 9 | PUN-001 | 2026-09-24 16:00:00+00 | 112.0 | CPCB | VALID | 2026-09-24T16:00:00Z | 112.0 | CPCB | VALID | **EXACT** |
| 10 | PUN-001 | 2026-09-24 18:00:00+00 | 98.0 | CPCB | VALID | 2026-09-24T18:00:00Z | 98.0 | CPCB | VALID | **EXACT** |
| 11 | PUN-001 | 2026-09-24 20:00:00+00 | 86.0 | CPCB | VALID | 2026-09-24T20:00:00Z | 86.0 | CPCB | VALID | **EXACT** |
| 12 | PUN-001 | 2026-09-24 22:00:00+00 | 78.0 | CPCB | VALID | 2026-09-24T22:00:00Z | 78.0 | CPCB | VALID | **EXACT** |

**Verification Confirmation**: Exactly 12 records returned. 0 synthetic transformations. Values in JSON match PostgreSQL disk storage with 100% fidelity.

---

## 17. Unit Test Results
Executed via `mvn test`:
- `com.aerosentinel.air.AirServiceUnitTest`:
  - `testValidCityReturnsStationObservations`: **PASS**
  - `testLatestObservationIsSelected`: **PASS**
  - `testMultipleStationsHandled`: **PASS**
  - `testNoObservationHandled`: **PASS**
  - `testValidStationAndDateRange`: **PASS**
  - `testInvalidStationThrowsException`: **PASS**
  - `testInvalidDateRangeThrowsException`: **PASS**
  - `testEmptyResultReturnsControlledResponse`: **PASS**
  - `testEntityToDtoMappingPreservesAllFields`: **PASS**
- `com.aerosentinel.city.CityServiceUnitTest`:
  - `testCityExists`: **PASS**
  - `testCityNotFound`: **PASS**
  - `testGetAllActiveCityResponses`: **PASS**

---

## 18. Integration Test Results
Executed via `mvn test` against real PostgreSQL container:
- `com.aerosentinel.air.AirQualityIntegrationTest`:
  - `testGetCities`: **PASS** (3 seeded cities)
  - `testGetLatestAirQualityForPune`: **PASS** (PUN-001: 78.0, PUN-002: 62.0, PUN-003: 86.0)
  - `testGetStationHistoryPun001`: **PASS** (12 observations)
  - `testGetStationHistoryPun002`: **PASS** (12 observations)
  - `testGetStationHistoryPun003`: **PASS** (12 observations)
  - `testGetInvalidStationReturns404`: **PASS**
  - `testInvalidDateRangeReturns400`: **PASS**
  - `testNoDataRangeReturnsControlledEmptyResponse`: **PASS**
  - `testMalformedDateReturns400`: **PASS**
  - `testMissingDateParamsReturns400`: **PASS**
  - `testInvalidCityReturns404`: **PASS**

---

## 19. Build Results
- **Command**: `mvn test`
- **Output Summary**:
  ```
  [INFO] Results:
  [INFO] Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
  [INFO] ------------------------------------------------------------------------
  [INFO] BUILD SUCCESS
  [INFO] ------------------------------------------------------------------------
  ```
- **Compilation Errors**: 0
- **Test Failures**: 0

### Complete Itemized Test Suite Inventory (26 Tests Total)

| # | Test Class | Package | Tests Run | Failures | Errors | Skipped | Individual Test Methods |
| :-: | :--- | :--- | :-: | :-: | :-: | :-: | :--- |
| 1 | `CityServiceUnitTest` | `com.aerosentinel.city` | **3** | 0 | 0 | 0 | 1. `testCityExists`<br>2. `testCityNotFound`<br>3. `testGetAllActiveCityResponses` |
| 2 | `AirServiceUnitTest` | `com.aerosentinel.air` | **9** | 0 | 0 | 0 | 1. `testValidCityReturnsStationObservations`<br>2. `testLatestObservationIsSelected`<br>3. `testMultipleStationsHandled`<br>4. `testNoObservationHandled`<br>5. `testValidStationAndDateRange`<br>6. `testInvalidStationThrowsException`<br>7. `testInvalidDateRangeThrowsException`<br>8. `testEmptyResultReturnsControlledResponse`<br>9. `testEntityToDtoMappingPreservesAllFields` |
| 3 | `AirQualityIntegrationTest` | `com.aerosentinel.air` | **11** | 0 | 0 | 0 | 1. `testGetCities`<br>2. `testGetLatestAirQualityForPune`<br>3. `testGetStationHistoryPun001`<br>4. `testGetStationHistoryPun002`<br>5. `testGetStationHistoryPun003`<br>6. `testGetInvalidStationReturns404`<br>7. `testInvalidDateRangeReturns400`<br>8. `testNoDataRangeReturnsControlledEmptyResponse`<br>9. `testMalformedDateReturns400`<br>10. `testMissingDateParamsReturns400`<br>11. `testInvalidCityReturns404` |
| 4 | `HealthControllerTest` | `com.aerosentinel.health` | **2** | 0 | 0 | 0 | 1. `testHealthEndpointReturnsUp`<br>2. `testHealthEndpointCors` |
| 5 | `AeroSentinelApplicationTests` | `com.aerosentinel` | **1** | 0 | 0 | 0 | 1. `contextLoads` |
| | **TOTAL TEST SUITE** | | **26** | **0** | **0** | **0** | **3 + 9 + 11 + 2 + 1 = 26** |

## 20. Startup Results
- Spring Boot started on port 8080 in 7.036 seconds with dev profile.
- Flyway validated 5 migrations; current schema version: 5.
- Hibernate spatial / PostgreSQL PostGIS dialect contributor initialized successfully.

---

## 21. Regression Results
- `GET /api/v1/health` $\to$ `HTTP 200 {"status":"UP","service":"aerosentinel-backend",...}` (**PASS**)
- `GET /api/v1/cities` $\to$ `HTTP 200` with Pune, Mumbai, Delhi (**PASS**)
- `HealthControllerTest`: 2 tests passed.
- `AeroSentinelApplicationTests`: 1 test passed.
- **F0 Regression**: 0 regressions detected.

---

## 22. Requirement Verification Matrix

| Requirement | Expected | Actual Proof | Status |
| :--- | :--- | :--- | :---: |
| **City List** | 3 cities | HTTP 200 `[{"name":"Pune"},{"name":"Mumbai"},{"name":"Delhi"}]` | **PASS** |
| **City Detail** | Pune details | HTTP 200 `{"id":"550e...0001","name":"Pune",...}` | **PASS** |
| **Latest Air API** | 3 Pune stations | HTTP 200 with PUN-001, PUN-002, PUN-003 | **PASS** |
| **Latest PM2.5** | Real DB values | PUN-001: 78.0, PUN-002: 62.0, PUN-003: 86.0 | **PASS** |
| **History PUN-001** | 12 records | HTTP 200, array size 12 | **PASS** |
| **History PUN-002** | 12 records | HTTP 200, array size 12 | **PASS** |
| **History PUN-003** | 12 records | HTTP 200, array size 12 | **PASS** |
| **Timestamp** | DB observed_at | Exact ISO-8601 UTC match | **PASS** |
| **Source** | CPCB / MPCB | CPCB on PUN-001, MPCB on PUN-002 & PUN-003 | **PASS** |
| **Quality** | VALID | VALID on all 36 observation points | **PASS** |
| **Invalid City** | 404 Not Found | HTTP 404 `{"error":"NOT_FOUND",...}` | **PASS** |
| **Invalid Station** | 404 Not Found | HTTP 404 `{"error":"NOT_FOUND",...}` | **PASS** |
| **Invalid Range** | 400 Bad Request | HTTP 400 `{"error":"VALIDATION_ERROR",...}` | **PASS** |
| **Malformed Date** | 400 Bad Request | HTTP 400 `{"error":"VALIDATION_ERROR",...}` | **PASS** |
| **Missing Params** | 400 Bad Request | HTTP 400 `{"error":"BAD_REQUEST",...}` | **PASS** |
| **Empty Data** | Controlled response | HTTP 200 `{"stationId":"PUN-001","observations":[]}` | **PASS** |
| **DTO Usage** | No entity exposure | Dedicated DTO classes in `com.aerosentinel.dto` | **PASS** |
| **Security** | Permitted routes | `/api/v1/stations/**` & `/api/v1/sensors/**` return 200 | **PASS** |
| **Unit Tests** | 100% Pass | 12 unit tests passed (9 air, 3 city) | **PASS** |
| **Integration Tests** | 100% Pass | 11 integration tests passed against PostgreSQL | **PASS** |
| **No Synthetic Data** | DB origin only | PostgreSQL SELECT cross-verification match | **PASS** |
| **F0 Regression** | 0 regressions | Health and City endpoints verified operational | **PASS** |

---

## 23. Remaining F1 Work (Later Phases)
- **Phase 4**: Real CPCB / MPCB external provider client integration and automated ingestion scheduler.
- **Phase 5**: Backend hardening, security audits, and rate-limiting.
- **Phase 6**: Frontend UI integration with live station selector and 24-hour historical trend charts.
- **Phase 7**: Freshness state machine (`LIVE`, `FRESH`, `STALE`, `OFFLINE`) and error boundary presentation.
- **Phase 8**: End-to-end integration and smoke verification.

---

## 24. Final Phase 3 Status
# **PASS**
All Phase 3 requirements have been fully implemented, validated by automated test suites, cross-verified with live PostgreSQL data, and proven via manual HTTP interactions. Execution stops here as instructed.
