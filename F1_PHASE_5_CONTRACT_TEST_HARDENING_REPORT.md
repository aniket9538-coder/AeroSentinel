# AeroSentinel — F1 Phase 5: API Contract + Test Hardening + Regression Verification Report

**Status:** PASS  
**Date:** September 25, 2026  
**Component:** AeroSentinel Feature 1 (Air Quality Monitoring & Ingestion)

---

## 1. Objective

The objective of Phase 5 is to establish rigorous, regression-safe verification for the AeroSentinel F1 backend contracts following the completion of Phase 3 (Backend Domain + Canonical APIs) and Phase 4 (External Provider Integration). This audit proves that data flows predictably and consistently across the entire vertical slice:

$$\text{External Provider (OpenAQ)} \longrightarrow \text{Normalized Domain} \longrightarrow \text{PostgreSQL} \longrightarrow \text{Canonical Backend API} \longrightarrow \text{Contract-Compliant DTO} \longrightarrow \text{Frontend}$$

---

## 2. Initial Baseline

- **Initial Test Suite:** 53 tests passed (1 skipped when `OPENAQ_API_KEY` was omitted).
- **PostgreSQL Database Baseline:** 36 seeded observations across 3 Pune stations (`PUN-001`, `PUN-002`, `PUN-003`) covering 24 hours of PM2.5 measurements (12 CPCB, 24 MPCB).
- **Canonical Endpoints Verified:**
  1. `GET /api/v1/cities`
  2. `GET /api/v1/cities/{cityId}`
  3. `GET /api/v1/cities/{cityId}/air-quality/latest`
  4. `GET /api/v1/stations/{stationId}/air-quality?from=...&to=...`

---

## 3. Audit Findings

1. **JPA Entity Non-Exposure:**
   - [CityController.java](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/city/CityController.java): Exposes `CityResponse` and `LatestAirQualityResponse`. No JPA entities (`City`, `AirObservation`, `MonitoringStation`) are exposed.
   - [StationController.java](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/sensor/StationController.java): Exposes `AirQualityHistoryResponse`. No JPA entities are exposed.
   - Hibernate proxy and internal persistence metadata are completely isolated from responses.
2. **Standard Error Contract:**
   - [GlobalExceptionHandler.java](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/exception/GlobalExceptionHandler.java): Enforces uniform JSON error shape across all exceptions (`timestamp`, `status`, `error`, `message`).
3. **Database Cleanliness:**
   - In `LiveOpenAqIngestionVerificationTest.java`, `@BeforeEach` cleaned up records prior to execution, but `@AfterEach` was added to ensure tests never leave residual records in PostgreSQL that could contaminate deterministic seed expectations in integration tests.
4. **Network Timeout Resiliency:**
   - `application.yml` default timeout for OpenAQ was adjusted from 5000ms to 15000ms (`${OPENAQ_TIMEOUT_MS:15000}`) to prevent intermittent read timeouts under real-world network latency.

---

## 4. Existing Coverage

Prior to Phase 5, the following 53 tests were active:
- `AeroSentinelApplicationTests`: 1 test (Application Context Load)
- `AirQualityIntegrationTest`: 11 tests (Canonical city list, Pune latest air, PUN-001/002/003 histories, 404 for invalid station, 400 for `from > to`, 200 empty array for no-data range, 400 for malformed date, 400 for missing date params, 404 for invalid city ID)
- `AirServiceUnitTest`: 9 tests (Unit service tests for latest and historical air quality)
- `CityServiceUnitTest`: 3 tests (Unit service tests for city retrieval and active list)
- `HealthControllerTest`: 2 tests (`/api/v1/health` and `/actuator/health`)
- `LiveOpenAqIngestionVerificationTest`: 1 test (Live OpenAQ v3 API call, normalization, PostgreSQL insert, duplicate check, and MockMvc API check)
- `OpenAqProviderUnitTest`: 16 tests (OpenAQ JSON parsing, validation, rejections, client timeouts, rate limits, 500 errors)
- `ProviderIngestionIntegrationTest`: 10 tests (Station resolution, ingestion service, duplicates, invalid PM2.5, missing timestamp)

---

## 5. Contract Verification

All canonical endpoints strictly adhere to contract expectations:

### A. City List Contract (`GET /api/v1/cities`)
- **HTTP Status:** 200 OK
- **Payload Shape:** Array of `CityResponse` DTOs
- **Mandatory Fields:** `id`, `name`, `state`, `country`, `timezone`, `latitude`, `longitude`, `active`, `createdAt`.
- **Proof:**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "name": "Pune",
    "state": "Maharashtra",
    "country": "India",
    "timezone": "Asia/Kolkata",
    "latitude": 18.5204,
    "longitude": 73.8567,
    "active": true,
    "createdAt": "2026-09-24T11:51:49.070625Z"
  }
]
```

### B. City Detail Contract (`GET /api/v1/cities/{cityId}`)
- **HTTP Status:** 200 OK (for valid UUID) / 404 NOT_FOUND (for non-existent city) / 400 BAD_REQUEST (for malformed UUID)
- **Proof (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "name": "Pune",
  "state": "Maharashtra",
  "country": "India",
  "timezone": "Asia/Kolkata",
  "latitude": 18.5204,
  "longitude": 73.8567,
  "active": true,
  "createdAt": "2026-09-24T11:51:49.070625Z"
}
```

### C. Latest Air Quality Contract (`GET /api/v1/cities/{cityId}/air-quality/latest`)
- **HTTP Status:** 200 OK
- **Payload Shape:** `LatestAirQualityResponse` DTO
- **Observation Fields:** `stationId`, `stationName`, `pm25`, `observedAt`, `source`, `quality`
- **Proof (200 OK):**
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

### D. Station History Contract (`GET /api/v1/stations/{stationId}/air-quality?from=...&to=...`)
- **HTTP Status:** 200 OK
- **Payload Shape:** `AirQualityHistoryResponse` DTO
- **Semantics:** Inclusive boundary filtering (`observedAt BETWEEN :from AND :to`), ordered chronologically ascending (`observedAt ASC`).
- **Proof (200 OK):**
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
    },
    {
      "pm25": 95.0,
      "observedAt": "2026-09-24T02:00:00Z",
      "source": "CPCB",
      "quality": "VALID"
    }
  ]
}
```

---

## 6. Validation Verification

The validation rules are strictly maintained without raw stack traces or internal leaks:

| Test Case | Request | Response Code | Error Code | Error Message |
|:---|:---|:---:|:---:|:---|
| **Missing `from`** | `GET /api/v1/stations/PUN-001/air-quality?to=...` | 400 | `BAD_REQUEST` | `Required parameter 'from' is missing` |
| **Missing `to`** | `GET /api/v1/stations/PUN-001/air-quality?from=...` | 400 | `BAD_REQUEST` | `Required parameter 'to' is missing` |
| **Malformed `from`** | `GET /api/v1/stations/PUN-001/air-quality?from=invalid&to=...` | 400 | `VALIDATION_ERROR` | `Invalid 'from' timestamp format: 'invalid'. Expected ISO-8601 UTC format` |
| **Malformed `to`** | `GET /api/v1/stations/PUN-001/air-quality?from=...&to=invalid` | 400 | `VALIDATION_ERROR` | `Invalid 'to' timestamp format: 'invalid'. Expected ISO-8601 UTC format` |
| **`from > to`** | `from=2026-09-25T00:00:00Z&to=2026-09-24T00:00:00Z` | 400 | `VALIDATION_ERROR` | `Invalid date range: 'from' (2026-09-25T00:00:00Z) must not be after 'to' (2026-09-24T00:00:00Z)` |
| **Invalid Station** | `GET /api/v1/stations/NONEXISTENT-STATION/air-quality?...` | 404 | `NOT_FOUND` | `Monitoring station not found with code: NONEXISTENT-STATION` |
| **Invalid City ID** | `GET /api/v1/cities/00000000-0000-0000-0000-000000000000` | 404 | `NOT_FOUND` | `City not found with id: 00000000-0000-0000-0000-000000000000` |
| **Malformed UUID** | `GET /api/v1/cities/not-a-valid-uuid` | 400 | `BAD_REQUEST` | `Invalid parameter value for 'id': not-a-valid-uuid` |

---

## 7. Error Verification

The project's standard error structure is defined as:
```json
{
  "timestamp": "ISO-8601 UTC timestamp",
  "status": 400 | 404 | 500,
  "error": "BAD_REQUEST | VALIDATION_ERROR | NOT_FOUND | INTERNAL_ERROR",
  "message": "Human-readable descriptive message"
}
```
All errors generated by `GlobalExceptionHandler` conform to this exact JSON structure.

---

## 8. DTO Verification

The following canonical DTO classes encapsulate all API responses:
- `com.aerosentinel.dto.city.CityResponse`
- `com.aerosentinel.dto.air.LatestAirQualityResponse`
- `com.aerosentinel.dto.air.AirQualityObservationResponse`
- `com.aerosentinel.dto.air.AirQualityHistoryResponse`
- `com.aerosentinel.dto.air.HistoricalObservationResponse`

All fields are strongly typed, serializable to standard JSON, and carry zero persistence annotations or entity linkages.

---

## 9. Entity Exposure Audit

- Full scan of `com.aerosentinel.city.CityController` and `com.aerosentinel.sensor.StationController` reveals **zero** entity leakage.
- Direct entity types (`City`, `AirObservation`, `MonitoringStation`) are never returned by canonical endpoints.
- Integration tests explicitly verify that Hibernate-specific properties (`hibernateLazyInitializer`, `handler`) are never serialized.

---

## 10. Provider/API Regression

The regression from real OpenAQ provider to canonical API was verified:
1. `OpenAqClient` fetched live observation for location `11613` (Shivajinagar).
2. `OpenAqMapper` parsed observation into `ProviderObservation(pm25=45.5, observedAt=2026-09-24T17:30:00Z, source="OPENAQ", dataQuality="VALID")`.
3. `IngestionService` persisted the record into PostgreSQL `air_observations`.
4. `StationController` returned the record verbatim via `GET /api/v1/stations/PUN-001/air-quality`:
   - `pm25`: `45.5`
   - `observedAt`: `2026-09-24T17:30:00Z`
   - `source`: `"OPENAQ"`
   - `quality`: `"VALID"`

---

## 11. Provenance Verification

- Original source attribution is maintained truthfully:
  - OpenAQ records remain `"OPENAQ"`.
  - CPCB records remain `"CPCB"`.
  - MPCB records remain `"MPCB"`.
- The system never rewrites or alters provider provenance to artificial names.

---

## 12. Timestamp Verification

- Physical measurement timestamp: stored in `observed_at` and exposed as `observedAt` in API responses.
- AeroSentinel ingestion timestamp: stored in `created_at` and kept internal to the backend database.
- The API never substitutes ingestion time for measurement time.

---

## 13. Security Verification

- [SecurityConfig.java](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/config/SecurityConfig.java) permits unauthenticated access only to designated public read endpoints (`/api/v1/cities/**`, `/api/v1/stations/**`, `/api/v1/health`, etc.).
- There is **no** wildcard `permitAll("/**")`.
- Unmapped/protected endpoints (e.g., `GET /api/v1/unpermitted-secured-endpoint`) return **HTTP 403 Forbidden** when unauthenticated.
- Regression test `F1ApiContractHardeningTest.testSecurityBoundariesContract` verifies this boundary automatically.

---

## 14. Database/API Comparison

Direct SQL query:
```sql
SELECT station_id, pm25, observed_at, source, data_quality 
FROM air_observations 
WHERE station_id = 'PUN-001' AND observed_at = '2026-09-24 00:00:00+00';
```
Result:
```
 station_id | pm25 |      observed_at       | source | data_quality 
------------+------+------------------------+--------+--------------
 PUN-001    |   72 | 2026-09-24 00:00:00+00 | CPCB   | VALID
```
API request:
```
GET /api/v1/stations/PUN-001/air-quality?from=2026-09-24T00:00:00Z&to=2026-09-24T00:00:00Z
```
Result:
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
**Conclusion:** Exact 1-to-1 match across all attributes (`stationId`, `pm25`, `observedAt`, `source`, `quality`).

---

## 15. Empty-Data Verification

Querying a time window with no observations (e.g. `2020-01-01T00:00:00Z` to `2020-01-02T00:00:00Z`):
- **HTTP Status:** 200 OK
- **Response:** `{"stationId":"PUN-001","stationName":"Shivajinagar CAAQMS","observations":[]}`
- Does not crash with HTTP 500, does not return null, does not generate synthetic/fake fallback data.

---

## 16. Provider-Failure Verification

- In `OpenAqProviderUnitTest` and `ProviderIngestionIntegrationTest`, simulated provider failures (HTTP 500, timeouts, rate limits HTTP 429, missing API key):
  - Returns graceful error result without uncaught exceptions.
  - Zero observations inserted.
  - Existing PostgreSQL database records remain intact.
  - Canonical API remains healthy and serves existing data.

---

## 17. Tests Before Phase 5

- **Total Tests:** 53
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0 (with `OPENAQ_API_KEY`)

---

## 18. Tests Added in Phase 5

Created [F1ApiContractHardeningTest.java](file:///c:/Users/lenovo/AeroSential/backend/src/test/java/com/aerosentinel/air/F1ApiContractHardeningTest.java) with 6 automated contract hardening tests:
1. `testGetCityDetailContract()`: Verifies HTTP 200, all fields of `CityResponse`, and verifies no Hibernate/JPA internal leakage.
2. `testGetCityNotFoundContract()`: Verifies HTTP 404 with structured `NOT_FOUND` error for non-existent city UUID.
3. `testGetCityMalformedUuidContract()`: Verifies HTTP 400 with structured `BAD_REQUEST` error for malformed UUID string.
4. `testMalformedToDateReturns400Contract()`: Verifies HTTP 400 with structured `VALIDATION_ERROR` error for malformed `to` date parameter.
5. `testSecurityBoundariesContract()`: Verifies public canonical endpoints succeed unauthenticated, while unpermitted paths return HTTP 403 Forbidden.
6. `testDatabaseToApiExactConsistency()`: Verifies direct 1-to-1 equivalence between PostgreSQL entity values and canonical API JSON response.

---

## 19. Tests After Phase 5

- **Total Tests:** 59
  - `AeroSentinelApplicationTests`: 1 test
  - `AirQualityIntegrationTest`: 11 tests
  - `F1ApiContractHardeningTest`: 6 tests
  - `AirServiceUnitTest`: 9 tests
  - `CityServiceUnitTest`: 3 tests
  - `HealthControllerTest`: 2 tests
  - `LiveOpenAqIngestionVerificationTest`: 1 test
  - `OpenAqProviderUnitTest`: 16 tests
  - `ProviderIngestionIntegrationTest`: 10 tests
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0
- **Build Status:** `BUILD SUCCESS`

---

## 20. HTTP Verification

Live HTTP requests performed against running Spring Boot instance on `http://localhost:8080`:

| Endpoint | Method | Expected Status | Actual Status | Response Sample / Key Fields |
|:---|:---:|:---:|:---:|:---|
| `/api/v1/health` | GET | 200 | **200** | `{"status":"UP","service":"aerosentinel-backend"}` |
| `/api/v1/cities` | GET | 200 | **200** | Array of 3 cities: Pune, Mumbai, Delhi |
| `/api/v1/cities/550e8400-e29b-41d4-a716-446655440001` | GET | 200 | **200** | `{"id":"550e8400-e29b-41d4-a716-446655440001","name":"Pune",...}` |
| `/api/v1/cities/00000000-0000-0000-0000-000000000000` | GET | 404 | **404** | `{"error":"NOT_FOUND","message":"City not found with id: ..."}` |
| `/api/v1/cities/not-a-valid-uuid` | GET | 400 | **400** | `{"error":"BAD_REQUEST","message":"Invalid parameter value for 'id': ..."}` |
| `/api/v1/cities/.../air-quality/latest` | GET | 200 | **200** | 3 stations: `PUN-001`, `PUN-002`, `PUN-003` with latest PM2.5 |
| `/api/v1/stations/PUN-001/air-quality?from=...&to=...` | GET | 200 | **200** | 12 chronological observations for 2026-09-24 |
| `/api/v1/stations/PUN-001/air-quality?from=2026-09-25...&to=2026-09-24...` | GET | 400 | **400** | `{"error":"VALIDATION_ERROR","message":"Invalid date range: ..."}` |
| `/api/v1/stations/NONEXISTENT-STATION/air-quality?...` | GET | 404 | **404** | `{"error":"NOT_FOUND","message":"Monitoring station not found with code: ..."}` |
| `/api/v1/stations/PUN-001/air-quality?from=2020-01-01...&to=2020-01-02...` | GET | 200 | **200** | `{"stationId":"PUN-001","stationName":"Shivajinagar CAAQMS","observations":[]}` |
| `/api/v1/unpermitted-secured-endpoint` | GET | 403 | **403** | Empty body, access denied |

---

## 21. Build Verification

- **Maven Command:** `mvn test`
- **Output:**
```
[INFO] Results:
[INFO] 
[INFO] Tests run: 59, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```
- **Application Startup:** Verified through Spring Boot daemon process startup on port 8080.
- **Health Endpoint:** Verified returning `status = "UP"`.

---

## 22. Files Changed

1. [backend/src/main/resources/application.yml](file:///c:/Users/lenovo/AeroSential/backend/src/main/resources/application.yml)
   - Increased default OpenAQ timeout from 5000ms to 15000ms (`${OPENAQ_TIMEOUT_MS:15000}`) to avoid intermittent read timeouts when contacting external public API.
2. [backend/src/test/java/com/aerosentinel/integration/openaq/LiveOpenAqIngestionVerificationTest.java](file:///c:/Users/lenovo/AeroSential/backend/src/test/java/com/aerosentinel/integration/openaq/LiveOpenAqIngestionVerificationTest.java)
   - Added `@org.junit.jupiter.api.AfterEach` cleanup to guarantee that any live OpenAQ test observations are removed after execution, preserving pristine PostgreSQL database state.
3. [backend/src/test/java/com/aerosentinel/air/F1ApiContractHardeningTest.java](file:///c:/Users/lenovo/AeroSential/backend/src/test/java/com/aerosentinel/air/F1ApiContractHardeningTest.java) *(New file)*
   - Added 6 contract hardening tests for city detail, UUID validation, error contracts, security regression, and DB-to-API consistency.

---

## 23. Files Not Changed

All core production Java classes and configuration files remained intact and unedited:
- `backend/src/main/java/com/aerosentinel/city/CityController.java` (Unchanged)
- `backend/src/main/java/com/aerosentinel/city/CityService.java` (Unchanged)
- `backend/src/main/java/com/aerosentinel/sensor/StationController.java` (Unchanged)
- `backend/src/main/java/com/aerosentinel/air/AirService.java` (Unchanged)
- `backend/src/main/java/com/aerosentinel/air/AirObservationRepository.java` (Unchanged)
- `backend/src/main/java/com/aerosentinel/exception/GlobalExceptionHandler.java` (Unchanged)
- `backend/src/main/java/com/aerosentinel/config/SecurityConfig.java` (Unchanged)
- `backend/src/main/java/com/aerosentinel/integration/openaq/OpenAqClient.java` (Unchanged)
- `backend/src/main/java/com/aerosentinel/integration/openaq/OpenAqMapper.java` (Unchanged)
- `backend/src/main/java/com/aerosentinel/integration/provider/IngestionService.java` (Unchanged)
- All frontend files (Strictly untouched in Phase 5)

---

## 24. Requirement Matrix

| Requirement | Expected | Actual Proof | Status |
|:---|:---|:---|:---:|
| **City list contract** | Stable DTO (`CityResponse`) | HTTP 200, array of 3 cities, verified via `AirQualityIntegrationTest` & curl | **PASS** |
| **City detail contract** | Stable DTO (`CityResponse`) | HTTP 200, valid Pune fields, verified via `F1ApiContractHardeningTest` & curl | **PASS** |
| **Latest air contract** | Stable DTO (`LatestAirQualityResponse`) | HTTP 200, 3 stations with DB PM2.5, verified via `AirQualityIntegrationTest` & curl | **PASS** |
| **History contract** | Stable DTO (`AirQualityHistoryResponse`) | HTTP 200, 12 observations, chronological ASC, verified via `AirQualityIntegrationTest` & curl | **PASS** |
| **Validation** | Controlled HTTP 400 | Missing params, malformed dates, `from > to` verified via `AirQualityIntegrationTest`, `F1ApiContractHardeningTest` & curl | **PASS** |
| **Invalid resources** | Controlled HTTP 404 | Invalid station, non-existent city UUID return 404 with structured JSON | **PASS** |
| **Empty response** | Controlled HTTP 200 with empty array | Verified via `AirQualityIntegrationTest` & curl (`observations: []`) | **PASS** |
| **Error contract** | Consistent `{timestamp, status, error, message}` | Verified across all 400 and 404 responses | **PASS** |
| **No entity exposure** | DTO only, no JPA leakage | Verified via code audit and JSON schema assertion (`F1ApiContractHardeningTest`) | **PASS** |
| **Provenance** | Source preserved (`OPENAQ`, `CPCB`, `MPCB`) | Exact source verified in DB and API response | **PASS** |
| **Timestamp** | `observedAt` preserved | Measurement time kept distinct from backend `createdAt` | **PASS** |
| **Duplicate** | Idempotent insertion, no duplicates | Verified via `ProviderIngestionIntegrationTest` and `LiveOpenAqIngestionVerificationTest` | **PASS** |
| **Security** | Boundaries intact, no wildcard | Public read endpoints open, protected endpoints return 403 Forbidden | **PASS** |
| **Provider failure** | No fake data, DB preserved | Verified via `OpenAqProviderUnitTest` & `ProviderIngestionIntegrationTest` | **PASS** |
| **DB/API consistency** | Exact 1-to-1 match | Verified via direct SQL query vs API curl & `F1ApiContractHardeningTest` | **PASS** |
| **Frontend compatibility** | Required fields present | Verified against frontend `AirObservation` interface expectations | **PASS** |
| **Regression** | All tests pass | 59/59 tests pass with 0 failures, 0 errors, 0 skipped | **PASS** |
| **Health** | `status = UP` | Verified via `HealthControllerTest` & curl `GET /api/v1/health` | **PASS** |

---

## 25. Final Status

# **PASS**
