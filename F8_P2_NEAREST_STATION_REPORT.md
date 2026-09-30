# F8-P2 IMPLEMENTATION REPORT: NEAREST MONITORING STATION DISTANCE SERVICE

**Feature:** F8 — Monitoring Gap + Sensor Recommendation
**Phase:** P2 — Nearest Station Distance
**Date:** 2026-09-30
**Status:** **PASS** ✅
**Repository:** `aniket9538-coder/AeroSentinel`
**Workspace:** `c:\Users\Harsh\OneDrive\Documents\Python Projects\AeroSentinel`

---

## 1. Executive Summary

Phase **F8-P2** implements the backend foundation for observation coverage assessment in AeroSentinel. Given an H3 cell and city, the service resolves the cell centroid, computes distances to all operational monitoring stations in the city using the authoritative Haversine utility, selects the nearest station, calculates the number of stations within a 5.0 km radius, and evaluates the 7.0 km monitoring coverage gap flag.

All calculations execute dynamically against real PostgreSQL data (`monitoring_stations`, `cities`, `grid_cells`). **Zero mock data, zero synthetic coordinates, zero new ML models, zero database schema changes or migrations, and zero modifications to protected features F3–F7.**

---

## 2. Files Inspected

Prior to implementation, the following existing production, test, and infrastructure files were audited:

| File | Location | Finding / Relevance |
|------|----------|---------------------|
| `MonitoringStation.java` | `backend/.../sensor/` | Authoritative entity for `monitoring_stations` table; fields `id`, `cityId`, `stationCode`, `name`, `agency`, `latitude`, `longitude`, `status`. |
| `SensorRepository.java` | `backend/.../sensor/` | Authoritative JPA repository; provides `findByCityIdAndStatus(UUID cityId, String status)`. |
| `H3Service.java` | `backend/.../spatial/` | Authoritative spatial engine; provides `h3ToCenter(h3Index)` and `validateH3Index(h3Index)`. |
| `FeatureEngineeringService.java` | `backend/.../feature/` | Source of `calculateHaversineDistanceKm(lat1, lon1, lat2, lon2)` (`public static`, returns km) and semantic reference for coverage gap logic (`COVERAGE_GAP_THRESHOLD_KM = 7.0`). |
| `SecurityConfig.java` | `backend/.../config/` | Confirmed `/api/v1/monitoring/**` was already configured as `permitAll()`. |
| `GlobalExceptionHandler.java` | `backend/.../exception/` | Confirmed `IllegalArgumentException` maps to RFC-compliant HTTP 400 Bad Request with standardized payload. |
| `HotspotController.java` | `backend/.../hotspot/` | Reference for city ID parameter conventions and Pune default fallback (`550e8400-e29b-41d4-a716-446655440001`). |
| `HotspotSpatialContext.java` | `backend/.../hotspot/` | Verified `MonitoringCoverageContext` structure (`nearestStationDistanceKm`, `stationsWithin5kmCount`, `monitoringCoverageGapFlag`). |
| `docker-compose.yml` | Project root | Defines `aerosentinel-postgres` (`postgis/postgis:16-3.4`) on port 5432. |

---

## 3. Files Changed

Only the necessary and required files for F8-P2 were modified or added. All edits were confined strictly to the `com.aerosentinel.monitoring` package:

| File | Type | Action | Description |
|------|------|--------|-------------|
| `backend/.../monitoring/dto/MonitoringCoverageResponse.java` | Production | **Created** | Immutable Java `record` representing the F8 monitoring coverage response contract. |
| `backend/.../monitoring/MonitoringService.java` | Production | **Implemented** | Core F8-P2 distance & coverage engine; replaced 0-byte stub. |
| `backend/.../monitoring/MonitoringController.java` | Production | **Implemented** | REST controller exposing `GET /api/v1/monitoring/coverage/{h3Index}`; replaced 0-byte stub. |
| `backend/.../monitoring/MonitoringServiceTest.java` | Test | **Created** | Unit test suite covering all 8 required cases and edge cases (10 tests). |
| `backend/.../monitoring/MonitoringControllerIntegrationTest.java` | Test | **Created** | MockMvc Spring Boot integration test suite against real Postgres data (7 tests). |

> [!NOTE]
> `MonitoringPriority.java` was left untouched as a 0-byte placeholder reserved for phase F8-P3.

---

## 4. Existing Utilities Reused

In strict compliance with the F8-P2 directives, no duplicate distance or spatial logic was introduced:

1. **Haversine Distance:**
   - Reused: `FeatureEngineeringService.calculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2)`
   - Method is `public static`, computes spherical great-circle distance using Earth radius $R = 6371.0\text{ km}$, and returns distance in **KILOMETERS**.
   - No second Haversine formula was written.
2. **H3 Spatial Operations:**
   - Reused: `H3Service.validateH3Index(String h3Index)` for deterministic cell validation.
   - Reused: `H3Service.h3ToCenter(String h3Index)` returning `LatLng` centroid (`lat`, `lng`).
   - The authoritative field name `h3Index` was maintained across all layers; no `cellId`, `gridId`, or `hexId` aliases were introduced.
3. **Repository Querying:**
   - Reused: `SensorRepository.findByCityIdAndStatus(UUID cityId, String status)` to query active stations in single city-scoped batch ($O(1)$ query, $O(S)$ iteration).
4. **Validation and Exception Handling:**
   - Standard Spring Boot `GlobalExceptionHandler` handles `IllegalArgumentException` $\to$ HTTP 400 Bad Request.

---

## 5. Exact Distance Semantics

```
H3 Cell Index (15-char)
          │
          ▼
   H3Service.h3ToCenter(h3Index)
          │
          ▼
   H3 Centroid (centerLat, centerLng)
          │
          ▼
   FeatureEngineeringService.calculateHaversineDistanceKm(centerLat, centerLng, stationLat, stationLng)
          │
          ▼
   Distance to Station (KILOMETERS)
```

- **Reference Point:** H3 cell centroid (not boundary, not nearest vertex).
- **Target Point:** Station WGS84 coordinates (`MonitoringStation.latitude`, `MonitoringStation.longitude`).
- **Unit:** Kilometers (km).
- **Rounding:** Rounded to 2 decimal places (`Math.round(dist * 100.0) / 100.0`) for JSON serialization, consistent with `computeMonitoringNetworkFeatures()`.
- **Nearby Radius:** $\le 5.0\text{ km}$ counts towards `stationsWithin5kmCount`.
- **Coverage Gap Rule:** Nearest active station distance $> 7.0\text{ km} \implies \text{monitoringCoverageGapFlag} = 1$; otherwise $0$.

---

## 6. Station Filtering Rules

1. **Active Stations Only:**
   - Primary database query: `sensorRepository.findByCityIdAndStatus(cityId, "ACTIVE")`.
   - In-memory defensive guard: Station must have non-null status matching `"ACTIVE"` (case-insensitive).
   - Inactive/decommissioned stations are strictly excluded from nearest-station evaluation and 5 km radius counting.
2. **Coordinate Validation:**
   - Stations with null or `NaN` coordinates, or values outside standard WGS84 boundaries ($-90 \le \text{lat} \le 90$, $-180 \le \text{lon} \le 180$), are ignored.
3. **No Active Stations Case:**
   - If a city has no active stations (e.g. Mumbai in current seeded reference data), the service returns a safe `noCoverage` response:
     - `nearestStationId = null`
     - `nearestStationCode = null`
     - `nearestStationName = null`
     - `nearestStationLatitude = null`
     - `nearestStationLongitude = null`
     - `nearestStationDistanceKm = null` (strictly NOT $0.0$)
     - `stationsWithin5kmCount = 0`
     - `monitoringCoverageGapFlag = 1`

---

## 7. API Contract

### Endpoint
```http
GET /api/v1/monitoring/coverage/{h3Index}?cityId={cityId}
```

- **Path Variable:** `h3Index` (String, 15-character H3 cell address).
- **Query Parameter:** `cityId` (UUID, optional). Defaults to Pune (`550e8400-e29b-41d4-a716-446655440001`) if omitted.
- **Security:** Public endpoint (`permitAll()` in `SecurityConfig`).

### Response Contract (`MonitoringCoverageResponse`)

```json
{
  "h3Index": "88608850e5fffff",
  "latitude": 18.53153430386518,
  "longitude": 73.84714485054492,
  "nearestStationId": "660e8400-e29b-41d4-a716-446655440001",
  "nearestStationCode": "PUN-001",
  "nearestStationName": "Shivajinagar CAAQMS",
  "nearestStationLatitude": 18.5314,
  "nearestStationLongitude": 73.8446,
  "nearestStationDistanceKm": 0.27,
  "stationsWithin5kmCount": 1,
  "monitoringCoverageGapFlag": 0
}
```

---

## 8. Test Results

### A. F8-P2 Specific Test Suite (17 Tests)

Command:
```powershell
./mvnw.cmd test "-Dtest=MonitoringServiceTest,MonitoringControllerIntegrationTest"
```

Result:
```
[INFO] Running com.aerosentinel.monitoring.MonitoringControllerIntegrationTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 21.05 s -- in com.aerosentinel.monitoring.MonitoringControllerIntegrationTest
[INFO] Running com.aerosentinel.monitoring.MonitoringServiceTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.751 s -- in com.aerosentinel.monitoring.MonitoringServiceTest
[INFO]
[INFO] Results:
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

#### Detailed Test Case Mapping:
| # | Test Case | Class | Method | Result |
|---|-----------|-------|--------|--------|
| 1 | Valid H3 with nearby active station | `MonitoringServiceTest` | `testValidH3WithNearbyActiveStation` | ✅ PASS |
| 2 | Valid H3 with multiple stations (closest selected) | `MonitoringServiceTest` | `testValidH3WithMultipleStationsSelectsClosest` | ✅ PASS |
| 3 | Station within 5 km (count $\ge 1$, gap = 0) | `MonitoringServiceTest` | `testStationWithin5kmGivesCountAndNoGap` | ✅ PASS |
| 4 | Nearest station > 7 km (gap = 1) | `MonitoringServiceTest` | `testNearestStationOver7kmProducesCoverageGap` | ✅ PASS |
| 5 | Multiple stations where closest is inactive | `MonitoringServiceTest` | `testMultipleStationsClosestInactiveIsIgnored` | ✅ PASS |
| 6 | City with no active stations (safe nulls, gap = 1) | `MonitoringServiceTest` | `testCityWithNoActiveStationsReturnsSafeNoCoverage` | ✅ PASS |
| 7 | Invalid H3 validation error | `MonitoringServiceTest` | `testInvalidH3ThrowsIllegalArgumentException` | ✅ PASS |
| 8 | Distance consistency vs FeatureEngineeringService | `MonitoringServiceTest` | `testDistanceCalculationMatchesFeatureEngineeringService` | ✅ PASS |
| 9 | Null cityId validation | `MonitoringServiceTest` | `testNullCityIdThrowsIllegalArgumentException` | ✅ PASS |
| 10 | Stations with invalid coordinates skipped | `MonitoringServiceTest` | `testStationsWithInvalidCoordinatesAreSkipped` | ✅ PASS |
| 11 | Integration: Real Pune cell (Shivajinagar PUN-001) | `MonitoringControllerIntegrationTest` | `testGetCoverageForShivajinagar` | ✅ PASS |
| 12 | Integration: Default cityId fallback | `MonitoringControllerIntegrationTest` | `testGetCoverageDefaultCityId` | ✅ PASS |
| 13 | Integration: Real Pune cell (Katraj PUN-002) | `MonitoringControllerIntegrationTest` | `testGetCoverageForKatraj` | ✅ PASS |
| 14 | Integration: Real Pune cell (Hadapsar PUN-003) | `MonitoringControllerIntegrationTest` | `testGetCoverageForHadapsar` | ✅ PASS |
| 15 | Integration: Distant Pune cell (gap = 1, dist > 7 km) | `MonitoringControllerIntegrationTest` | `testGetCoverageForDistantCell` | ✅ PASS |
| 16 | Integration: City with no stations (Mumbai safe nulls) | `MonitoringControllerIntegrationTest` | `testGetCoverageCityWithNoStations` | ✅ PASS |
| 17 | Integration: Invalid H3 returns HTTP 400 | `MonitoringControllerIntegrationTest` | `testInvalidH3ReturnsBadRequest` | ✅ PASS |

---

## 9. Real Runtime Request/Response Evidence

Executed against the live running Spring Boot backend connected to PostgreSQL (`aerosentinel-postgres`):

### Scenario 1: Well-Covered H3 Cell (Shivajinagar, Pune)
```bash
curl -s "http://localhost:8080/api/v1/monitoring/coverage/88608850e5fffff?cityId=550e8400-e29b-41d4-a716-446655440001"
```
**HTTP 200 OK Response:**
```json
{
  "h3Index": "88608850e5fffff",
  "latitude": 18.53153430386518,
  "longitude": 73.84714485054492,
  "nearestStationId": "660e8400-e29b-41d4-a716-446655440001",
  "nearestStationCode": "PUN-001",
  "nearestStationName": "Shivajinagar CAAQMS",
  "nearestStationLatitude": 18.5314,
  "nearestStationLongitude": 73.8446,
  "nearestStationDistanceKm": 0.27,
  "stationsWithin5kmCount": 1,
  "monitoringCoverageGapFlag": 0
}
```

### Scenario 2: Well-Covered H3 Cell (Katraj, Pune)
```bash
curl -s "http://localhost:8080/api/v1/monitoring/coverage/88608852c1fffff?cityId=550e8400-e29b-41d4-a716-446655440001"
```
**HTTP 200 OK Response:**
```json
{
  "h3Index": "88608852c1fffff",
  "latitude": 18.453434498265583,
  "longitude": 73.86727191335176,
  "nearestStationId": "660e8400-e29b-41d4-a716-446655440002",
  "nearestStationCode": "PUN-002",
  "nearestStationName": "Katraj Air Station",
  "nearestStationLatitude": 18.4575,
  "nearestStationLongitude": 73.8677,
  "nearestStationDistanceKm": 0.45,
  "stationsWithin5kmCount": 1,
  "monitoringCoverageGapFlag": 0
}
```

### Scenario 3: Well-Covered H3 Cell (Hadapsar, Pune)
```bash
curl -s "http://localhost:8080/api/v1/monitoring/coverage/8860885357fffff?cityId=550e8400-e29b-41d4-a716-446655440001"
```
**HTTP 200 OK Response:**
```json
{
  "h3Index": "8860885357fffff",
  "latitude": 18.506660630163616,
  "longitude": 73.92897649026045,
  "nearestStationId": "660e8400-e29b-41d4-a716-446655440003",
  "nearestStationCode": "PUN-003",
  "nearestStationName": "Hadapsar Industrial Zone",
  "nearestStationLatitude": 18.5089,
  "nearestStationLongitude": 73.926,
  "nearestStationDistanceKm": 0.4,
  "stationsWithin5kmCount": 1,
  "monitoringCoverageGapFlag": 0
}
```

### Scenario 4: Weakly-Covered H3 Cell (> 7.0 km Gap)
```bash
curl -s "http://localhost:8080/api/v1/monitoring/coverage/8860884119fffff?cityId=550e8400-e29b-41d4-a716-446655440001"
```
**HTTP 200 OK Response:**
```json
{
  "h3Index": "8860884119fffff",
  "latitude": 18.650281158145095,
  "longitude": 73.77805754536315,
  "nearestStationId": "660e8400-e29b-41d4-a716-446655440001",
  "nearestStationCode": "PUN-001",
  "nearestStationName": "Shivajinagar CAAQMS",
  "nearestStationLatitude": 18.5314,
  "nearestStationLongitude": 73.8446,
  "nearestStationDistanceKm": 14.96,
  "stationsWithin5kmCount": 0,
  "monitoringCoverageGapFlag": 1
}
```

### Scenario 5: City with No Active Stations (Mumbai)
```bash
curl -s "http://localhost:8080/api/v1/monitoring/coverage/88608850e5fffff?cityId=550e8400-e29b-41d4-a716-446655440002"
```
**HTTP 200 OK Response:**
```json
{
  "h3Index": "88608850e5fffff",
  "latitude": 18.53153430386518,
  "longitude": 73.84714485054492,
  "nearestStationId": null,
  "nearestStationCode": null,
  "nearestStationName": null,
  "nearestStationLatitude": null,
  "nearestStationLongitude": null,
  "nearestStationDistanceKm": null,
  "stationsWithin5kmCount": 0,
  "monitoringCoverageGapFlag": 1
}
```

### Scenario 6: Invalid H3 Cell Index
```bash
curl -s -i "http://localhost:8080/api/v1/monitoring/coverage/invalid-h3?cityId=550e8400-e29b-41d4-a716-446655440001"
```
**HTTP 400 Bad Request Response:**
```json
{
  "error": "BAD_REQUEST",
  "message": "Invalid H3 index: invalid-h3",
  "timestamp": "2026-09-29T20:19:23.745546900Z",
  "status": 400
}
```

---

## 10. Database Integrity Confirmation

A complete audit of the PostgreSQL database was performed after runtime execution:

1. **Table Count:** Exactly 28 tables in the `public` schema (identical to baseline). Zero new tables created.
2. **Migrations:** Exactly 17 migrations (`V1` through `V17`). Zero migration scripts added.
3. **Data Records Untouched:**
   - `monitoring_stations`: exactly 3 records (Shivajinagar, Katraj, Hadapsar — unmodified).
   - `forecasts`: 0 records modified.
   - `hotspot_predictions`: 0 records modified.
   - `pollution_events`: 30 records unmodified.
   - `alerts`: 30 records unmodified.
   - `authority_actions`: unmodified.

---

## 11. Regression Test Confirmation

The core regression test suites across F3–F7 were executed:

Command:
```powershell
./mvnw.cmd test "-Dtest=FeatureEngineeringServiceTest,HotspotPhase7ContextTest,OperationalWorkflowLifecycleTest,AlertUnitTest,InspectionUnitTest,AlertIntegrationTest"
```

Result:
```
[INFO] Running com.aerosentinel.action.OperationalWorkflowLifecycleTest
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.aerosentinel.alert.AlertIntegrationTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.aerosentinel.alert.AlertUnitTest
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.aerosentinel.feature.FeatureEngineeringServiceTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.aerosentinel.hotspot.HotspotPhase7ContextTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.aerosentinel.inspection.InspectionUnitTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] Results:
[INFO] Tests run: 75, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**All 75 regression tests passed with zero failures and zero errors.**

---

## 12. Pre-Existing Failures (Isolated & Documented)

As documented in `F8_P1_MONITORING_DATA_AUDIT_REPORT.md` and prior phase logs, `HotspotIntegrationTest` contains 4 pre-existing failures when executed in a standalone test environment without live OpenAQ API ingestion and without the physical Python artifact `hotspot_classifier_v1.joblib` located on the test runner path:
- `testGetHotspotsForPune` (`JSON path "$.freshness" expected LIVE/STALE but was NO_DATA`)
- `testDatabasePersistenceAudit` (`Expecting actual not to be empty`)
- `testGetSingleCellHotspot` (Status expected `<200>` but was `<404>` due to `MODEL_UNAVAILABLE`)
- `testMultiCityHotspotBehavior` (`Expecting actual not to be empty`)

These failures are completely isolated to the external ML model file dependency in `HotspotIntegrationTest` and are entirely unrelated to F8-P2.

---

## 13. Pass / Fail Evaluation

| # | Pass Criterion | Status | Evidence |
|---|----------------|--------|----------|
| 1 | Nearest station is computed from real station data | ✅ **PASS** | Station ID, code, name, lat, lon resolved from `monitoring_stations` |
| 2 | H3 centroid is used | ✅ **PASS** | `H3Service.h3ToCenter(h3Index)` used for centroid |
| 3 | Existing Haversine utility is reused | ✅ **PASS** | Direct delegation to `FeatureEngineeringService.calculateHaversineDistanceKm()` |
| 4 | Distance is returned in kilometers | ✅ **PASS** | Haversine $R=6371.0\text{ km}$, verified in km |
| 5 | Active stations are respected | ✅ **PASS** | Filtered by `status = 'ACTIVE'`; inactive stations skipped |
| 6 | 5 km station count works | ✅ **PASS** | `stationsWithin5kmCount` verified in unit, integration, and runtime tests |
| 7 | 7 km coverage-gap flag works | ✅ **PASS** | Flag = 0 when $\le 7.0\text{ km}$, Flag = 1 when $> 7.0\text{ km}$ |
| 8 | No fake fallback station / distance | ✅ **PASS** | Null fields and null distance when no stations exist; never fake 0.0 |
| 9 | No new ML model | ✅ **PASS** | Pure spatial deterministic calculation |
| 10 | No new DB table / migration | ✅ **PASS** | Confirmed 28 tables, 17 migrations, zero schema changes |
| 11 | F3–F7 untouched | ✅ **PASS** | Git status clean outside `monitoring/` package |
| 12 | P2-specific tests pass | ✅ **PASS** | 17/17 F8-P2 tests passed |
| 13 | Real PostgreSQL runtime verification succeeds | ✅ **PASS** | Verified against live Spring Boot on port 8080 with real PostgreSQL |
| 14 | Report created | ✅ **PASS** | `F8_P2_NEAREST_STATION_REPORT.md` written |

```
╔════════════════════════════════════════════════════════════════════════╗
║                                                                        ║
║                       F8-P2 FINAL STATUS: PASS                         ║
║                                                                        ║
║  Backend nearest monitoring station distance service fully verified.   ║
║  17 F8-P2 tests passing. 75 regression tests passing.                  ║
║  All real runtime PostgreSQL queries verified. Zero DB modifications.  ║
║                                                                        ║
╚════════════════════════════════════════════════════════════════════════╝
```
