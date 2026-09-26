# AeroSentinel — FEATURE 2 (F2) — PHASE 4 REPORT
## REAL WEATHER PROVIDER & REAL WEATHER INGESTION PIPELINE

**Feature**: F2 — Weather Integration + Centralized H3 Spatial Layer  
**Phase**: Phase 4 (Real Weather Provider + Real Weather Ingestion Pipeline)  
**Execution Timestamp**: 2026-09-25T23:55:00+05:30  
**Phase Status**: **PASS**  

---

## 1. Weather Provider Selected & Rationale

- **Selected Provider**: [Open-Meteo Forecast API](https://open-meteo.com)
- **Rationale**:
  1. **Zero Fake Data / High Authenticity**: Open-Meteo integrates authoritative global numerical weather models (ECMWF, GFS, DWD ICON) with continuous physical reanalysis.
  2. **Free Non-Commercial Tier / Zero Barrier**: No proprietary API keys or billing barriers required for development and testing, eliminating operational friction.
  3. **High Spatial Precision**: Accepts precise floating-point decimal coordinates (`latitude`, `longitude`) matching our 8 monitoring stations.
  4. **Native Metric Standard**: Direct support for standard SI/metric units (Celsius, km/h, mm, degrees).
  5. **Timezone Support**: Explicit timezone parameter (`timezone=Asia/Kolkata`) guaranteeing deterministic synchronization with local Indian Standard Time (IST) stations.
  6. **Complete Meteorological Coverage**: Returns all required variables (`temperature_2m`, `relative_humidity_2m`, `wind_speed_10m`, `wind_direction_10m`, `precipitation`) in a single payload.

---

## 2. Open-Meteo API Endpoints & Parameters Used

- **Base Endpoint**: `https://api.open-meteo.com/v1/forecast`
- **Query Parameters**:
  - `latitude`: Station latitude (e.g., `18.5314` for Pune Shivajinagar)
  - `longitude`: Station longitude (e.g., `73.8446` for Pune Shivajinagar)
  - `hourly`: `temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m,precipitation`
  - `past_days`: `1` (ingests past 24 hours of authentic recent historical observations)
  - `forecast_days`: `1` (current forecast window)
  - `timezone`: `Asia/Kolkata`
- **Full Representative URL**:
  ```http
  GET https://api.open-meteo.com/v1/forecast?latitude=18.5314&longitude=73.8446&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m,precipitation&past_days=1&forecast_days=1&timezone=Asia%2FKolkata
  ```

---

## 3. Weather Variables Ingested & Units

| Open-Meteo Parameter | Database Column | Entity Field | Unit | Validation Range | Meteorological Significance |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `temperature_2m` | `temperature` | `temperature` | °C | `[-50.0, 60.0]` | Ambient 2m air temperature; governs thermal inversions. |
| `relative_humidity_2m` | `humidity` | `humidity` | % | `[0.0, 100.0]` | Relative humidity; affects hygroscopic growth of particulate matter. |
| `wind_speed_10m` | `wind_speed` | `windSpeed` | km/h | `[0.0, 150.0]` | Surface wind speed; drives atmospheric dispersion and ventilation. |
| `wind_direction_10m` | `wind_direction` | `windDirection` | ° (0–360) | `[0.0, 360.0]` | Wind vector azimuth; indicates trajectory of pollution plumes. |
| `precipitation` | `rainfall` | `rainfall` | mm | `[0.0, 500.0]` | Hourly precipitation; drives wet deposition and particulate scavenging. |

---

## 4. Timezone Handling Strategy

- **Open-Meteo Output**: Open-Meteo returns hourly timestamps in ISO-8601 format relative to the requested timezone without offset: `["2026-09-24T23:00", "2026-09-25T00:00", ...]`.
- **Parsing Strategy**:
  [`WeatherMapper.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/integration/weather/WeatherMapper.java) interprets each string in `Asia/Kolkata` (`UTC+05:30`) and converts it to a standard UTC `java.time.Instant`:
  ```java
  LocalDateTime ldt = LocalDateTime.parse(timeStr);
  Instant observedAt = ldt.atZone(ZoneId.of("Asia/Kolkata")).toInstant();
  ```
- **Storage Standard**: PostgreSQL `TIMESTAMPTZ` stores every observation as normalized UTC `Instant`.
- **Zero Mock Timestamps**: Observation timestamps strictly reflect the provider's physical measurement times (`observed_at`). System ingestion time is stored separately in `created_at`.

---

## 5. Weather Client Architecture

- **Class**: [`WeatherClient.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/integration/weather/WeatherClient.java)
- **Framework Component**: Spring `@Component`
- **Key Responsibilities**:
  - Validates coordinates: Latitude in `[-90.0, 90.0]`, Longitude in `[-180.0, 180.0]`.
  - Builds structured query with Spring `UriComponentsBuilder`.
  - Configures resilient HTTP connection and read timeouts (default: 15,000 ms).
  - Handles HTTP status exceptions (`HttpStatusCodeException`) and network timeouts (`ResourceAccessException`).
  - Returns `Optional<WeatherProviderResponse>`.
  - Disambiguates constructor autowiring using `@Autowired`.

---

## 6. Weather Mapper Logic & Validations

- **Class**: [`WeatherMapper.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/integration/weather/WeatherMapper.java)
- **Framework Component**: Spring `@Component`
- **Validation Pipeline**:
  1. Checks for null or empty hourly arrays.
  2. Ensures parallel arrays (`time`, `temperature_2m`, `relative_humidity_2m`, `wind_speed_10m`, `wind_direction_10m`, `precipitation`) match in length.
  3. Enforces domain boundaries:
     - `temperature`: Rejected if `< -50.0` or `> 60.0`.
     - `humidity`: Rejected if `< 0.0` or `> 100.0`.
     - `windSpeed`: Rejected if `< 0.0` or `> 150.0`.
     - `windDirection`: Rejected if `< 0.0` or `> 360.0`.
     - `precipitation`: Rejected if `< 0.0` or `> 500.0`.
  4. Automatically sets provenance metadata: `source = "OPEN_METEO"`.
  5. Assigns station coordinate geometry and links `city_id`.

---

## 7. Ingestion Pipeline Architecture

```
           +------------------------------------------+
           |       Open-Meteo REST API Endpoint       |
           |  api.open-meteo.com/v1/forecast (Metric) |
           +------------------------------------------+
                                |
                                | HTTP GET (JSON)
                                v
           +------------------------------------------+
           |           WeatherClient.java             |
           |   (Timeouts, Coordinate Validation)      |
           +------------------------------------------+
                                |
                                | WeatherProviderResponse DTO
                                v
           +------------------------------------------+
           |           WeatherMapper.java             |
           |  (Timezone Conversion Asia/Kolkata->UTC, |
           |   Meteorological Bounds Validation)      |
           +------------------------------------------+
                                |
                                | List<WeatherObservation>
                                v
+-------------------------------------------------------------------+
|                  WeatherIngestionService.java                     |
|                                                                   |
|   1. Query 8 Active Stations from SensorRepository                |
|   2. Calculate Resolution 8 H3 Index via H3Service                |
|   3. Ensure GridCell & PostGIS Polygon via GridService            |
|   4. Pre-Flight Duplicate Check via WeatherRepository             |
|      (existsByH3IndexAndObservedAt)                               |
|   5. Batch Persist New Observations to PostgreSQL                 |
+-------------------------------------------------------------------+
                                |
                                | SQL INSERT
                                v
+-------------------------------------------------------------------+
|                 PostgreSQL 16 + PostGIS 3.4                       |
|                                                                   |
|   Table: weather_observations                                     |
|   - h3_index VARCHAR(30) NOT NULL                                 |
|   - observed_at TIMESTAMPTZ NOT NULL                              |
|   - source = 'OPEN_METEO'                                         |
|   - location GEOGRAPHY(Point, 4326) [trg_sync_weather_obs_loc]    |
|   - UNIQUE (h3_index, observed_at) [V7 constraint]                |
+-------------------------------------------------------------------+
```

---

## 8. Ingestion Trigger Mechanism (Startup + Scheduled)

1. **Startup Backfill Runner**:
   - Class: [`WeatherBackfillRunner.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/integration/weather/WeatherBackfillRunner.java)
   - Interface: Spring Boot `ApplicationRunner`
   - Trigger: Executes automatically on application startup.
   - Behavior: If `weather_observations` is empty or below operational threshold, automatically contacts Open-Meteo to populate 25 hours of weather across all 8 stations in Pune, Mumbai, and Delhi.

2. **Periodic Background Scheduler**:
   - Class: [`WeatherIngestionScheduler.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/integration/weather/WeatherIngestionScheduler.java)
   - Trigger: `@Scheduled(fixedDelayString = "${app.weather.ingestion.interval-ms:900000}", initialDelay = 15000)`
   - Interval: Every 15 minutes (900,000 ms), with a 15-second initial delay.
   - Behavior: Repeatedly checks for the latest hourly readings across all active stations.

---

## 9. Scheduling Configuration & Interval

In [`application.yml`](file:///c:/Users/lenovo/AeroSential/backend/src/main/resources/application.yml):
```yaml
app:
  external:
    weather:
      open-meteo:
        base-url: https://api.open-meteo.com/v1/forecast
        timeout-ms: 15000
  weather:
    ingestion:
      interval-ms: 900000 # 15 minutes
    backfill:
      enabled: true
```

---

## 10. Multi-City Ingestion Results (Pune, Mumbai, Delhi)

The ingestion pipeline successfully fetched and processed weather observations for all 8 monitoring stations across all 3 configured cities:

| City | Station Code | Station Name | Latitude | Longitude | H3 Res 8 Index | Ingestion Status | Records Ingested |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Pune** | `PUN-001` | Shivajinagar CAAQMS | 18.5314 | 73.8446 | `88608850e5fffff` | **SUCCESS** | 25 |
| **Pune** | `PUN-002` | Katraj Air Station | 18.4575 | 73.8677 | `88608852c1fffff` | **SUCCESS** | 25 |
| **Pune** | `PUN-003` | Hadapsar Industrial Zone | 18.5089 | 73.9260 | `8860885357fffff` | **SUCCESS** | 25 |
| **Mumbai** | `MUM-001` | Kurla, Mumbai - MPCB | 19.0863 | 72.8888 | `88608b56b3fffff` | **SUCCESS** | 25 |
| **Mumbai** | `MUM-002` | Chhatrapati Shivaji Intl. Airport (T2) | 19.10078 | 72.87462 | `88608b54d7fffff` | **SUCCESS** | 25 |
| **Delhi** | `DEL-001` | R K Puram, Delhi - DPCC | 28.563262 | 77.186937 | `883da11505fffff` | **SUCCESS** | 25 |
| **Delhi** | `DEL-002` | Anand Vihar, New Delhi - DPCC | 28.646835 | 77.316032 | `883da1149bfffff` | **SUCCESS** | 25 |
| **Delhi** | `DEL-003` | Punjabi Bagh, Delhi - DPCC | 28.674045 | 77.131023 | `883da18d9dfffff` | **SUCCESS** | 25 |

---

## 11. Weather Observations Count by City

Direct PostgreSQL query:
```sql
SELECT c.name AS city, count(w.id) AS obs_count
FROM weather_observations w JOIN cities c ON w.city_id = c.id
GROUP BY c.name ORDER BY c.name;
```

| City | Active Stations | Hourly Records / Station | Total Weather Observations |
| :--- | :--- | :--- | :--- |
| **Delhi** | 3 stations | 25 records | **75** |
| **Mumbai** | 2 stations | 25 records | **50** |
| **Pune** | 3 stations | 25 records | **75** |
| **Total** | **8 stations** | **25 records** | **200** |

---

## 12. Weather Observations Count by Station / H3

Direct PostgreSQL query:
```sql
SELECT w.h3_index, c.name AS city, count(*) AS obs_count
FROM weather_observations w JOIN cities c ON w.city_id = c.id
GROUP BY w.h3_index, c.name ORDER BY c.name, w.h3_index;
```

| H3 Resolution 8 Index | City | Station Code | Observations Count |
| :--- | :--- | :--- | :--- |
| `883da1149bfffff` | Delhi | `DEL-002` (Anand Vihar) | 25 |
| `883da11505fffff` | Delhi | `DEL-001` (R K Puram) | 25 |
| `883da18d9dfffff` | Delhi | `DEL-003` (Punjabi Bagh) | 25 |
| `88608b54d7fffff` | Mumbai | `MUM-002` (CSIA T2) | 25 |
| `88608b56b3fffff` | Mumbai | `MUM-001` (Kurla) | 25 |
| `88608850e5fffff` | Pune | `PUN-001` (Shivajinagar) | 25 |
| `88608852c1fffff` | Pune | `PUN-002` (Katraj) | 25 |
| `8860885357fffff` | Pune | `PUN-003` (Hadapsar) | 25 |

---

## 13. Total Ingested Count

- **Total Weather Observations Ingested**: **200**
- **Date Range Covered**: `2026-09-24T17:30:00Z` to `2026-09-25T17:30:00Z` (25 continuous hours in Indian Standard Time: `2026-09-24 23:00 IST` to `2026-09-25 23:00 IST`).

---

## 14. Total Failed / Rejected Count & Reasons

- **Total Fetched**: 384 potential observation points (48 hours fetched per station request).
- **Total Ingested**: 200 observations (within valid past-day boundary).
- **Total Rejected**: **0** (all received readings fell within meteorological sanity limits).
- **Total Failed HTTP Calls**: **0** (100% success rate across all 8 station calls).

---

## 15. Idempotency & Duplicate Prevention Strategy

1. **Application-Level Pre-Flight Check**:
   Before saving each record, [`WeatherIngestionService.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/integration/weather/WeatherIngestionService.java) executes:
   ```java
   if (weatherRepository.existsByH3IndexAndObservedAt(h3Index, obs.getObservedAt())) {
       log.debug("Duplicate weather observation for H3 {} at {}. Skipping.", h3Index, obs.getObservedAt());
       summary.incrementDuplicates();
       continue;
   }
   ```
2. **Database Hard Constraint**:
   Migration `V7` dropped `uq_weather_obs_city_time` and added `uq_weather_obs_spatial_time` on `(h3_index, observed_at)`.
3. **Idempotency Execution Proof**:
   During `WeatherSpatialIntegrationTest`:
   - First run inserted 200 rows.
   - Second immediate run resulted in: `stations=8, fetched=384, inserted=0, duplicates=200, rejected=0`.
   - Exactly 0 duplicate records were created.

---

## 16. H3 Integration Verification

- **H3 Resolution**: **8** (Uber H3 neighborhood standard).
- **Null H3 Count**: `SELECT count(*) FROM weather_observations WHERE h3_index IS NULL;` = **0**.
- **H3 Validity**: Every populated index passed `H3Service.validateH3Index()`.
- **Spatial Alignment**: The H3 index calculated for each weather observation matches the H3 index of the corresponding monitoring station in `grid_cells`.

---

## 17. Grid Cells Creation / Association

- **Total `grid_cells` Count**: **8**
- **Association Flow**:
  Before persisting weather observations for an H3 cell, [`GridService.getOrCreateGridCell(h3Index, cityId)`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/grid/GridService.java) is invoked:
  - If the grid cell exists, it is reused.
  - If it does not exist, `H3Service.h3ToCenter()` and `H3Service.h3ToBoundaryWkt()` compute the centroid and boundary polygon, persisting the cell with PostGIS polygon geometry.
- **Active Grid Cells**: All 8 grid cells are active, valid, and indexed by `idx_grid_cells_boundary` (GiST).

---

## 18. PostGIS Geometry Verification for Weather

Direct PostgreSQL query:
```sql
SELECT 
    count(*) AS total,
    count(location) AS with_location,
    sum(CASE WHEN ST_IsValid(location::geometry) THEN 1 ELSE 0 END) AS valid_geometries,
    sum(CASE WHEN ST_GeometryType(location::geometry) = 'ST_Point' THEN 1 ELSE 0 END) AS valid_points
FROM weather_observations;
```

- **Total Records**: **200**
- **Non-null `location`**: **200** (100%)
- **`ST_IsValid(location::geometry)`**: **200** (100% valid)
- **`ST_GeometryType(location::geometry)`**: **200** (100% `ST_Point`)
- **Location Trigger**: PostgreSQL trigger `trg_sync_weather_obs_location` automatically synchronized `latitude` and `longitude` to `location` (`GEOGRAPHY(Point, 4326)`).

---

## 19. Weather Metrics Summary

Direct PostgreSQL query:
```sql
SELECT 
    c.name AS city,
    round(min(w.temperature)::numeric, 1) AS min_temp,
    round(max(w.temperature)::numeric, 1) AS max_temp,
    round(min(w.humidity)::numeric, 1) AS min_humidity,
    round(max(w.humidity)::numeric, 1) AS max_humidity,
    round(min(w.wind_speed)::numeric, 1) AS min_wind_speed,
    round(max(w.wind_speed)::numeric, 1) AS max_wind_speed,
    round(sum(w.rainfall)::numeric, 2) AS total_rainfall
FROM weather_observations w JOIN cities c ON w.city_id = c.id
GROUP BY c.name ORDER BY c.name;
```

| City | Temp Min / Max (°C) | Humidity Min / Max (%) | Wind Speed Min / Max (km/h) | Total Rainfall (mm) | Meteorological Character |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Delhi** | 25.0 / 31.7 | 48.0 / 85.0 | 2.3 / 12.7 | 0.00 | Warm, moderate humidity, southeasterly wind. |
| **Mumbai** | 24.4 / 30.1 | 69.0 / 98.0 | 1.5 / 16.5 | 5.00 | Coastal humid, westerly onshore maritime breeze, intermittent rainfall. |
| **Pune** | 21.2 / 30.5 | 46.0 / 96.0 | 9.7 / 21.1 | 5.80 | Plateau climate, diurnal temperature variation, westerly Ghats winds, intermittent rain. |

---

## 20. Air Observations Preservation Verification (166 rows intact)

Direct PostgreSQL verification:
```sql
SELECT source, count(*), min(observed_at), max(observed_at) 
FROM air_observations GROUP BY source;
```

| Source | Count | Earliest Observation | Latest Observation | Null H3 Count | Preservation Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **OPENAQ** | 130 | 2018-02-21 21:15:00 UTC | 2026-09-24 17:30:00 UTC | 0 | **100% Intact** |
| **MPCB** | 24 | 2026-09-24 00:00:00 UTC | 2026-09-24 22:00:00 UTC | 0 | **100% Intact** |
| **CPCB** | 12 | 2026-09-24 00:00:00 UTC | 2026-09-24 22:00:00 UTC | 0 | **100% Intact** |
| **Total** | **166** | | | **0** | **100% Baseline Preserved** |

---

## 21. Database Schema Changes (V7 Migration Details)

- **Migration File**: [`V7__f2_spatial_weather_uniqueness.sql`](file:///c:/Users/lenovo/AeroSential/backend/src/main/resources/db/migration/V7__f2_spatial_weather_uniqueness.sql)
- **Problem in `V6`**:
  `V6` defined `CONSTRAINT uq_weather_obs_city_time UNIQUE (city_id, observed_at)`. This prohibited having more than one weather observation per entire city at the same timestamp. However, Pune has 3 stations, Mumbai 2, and Delhi 3. When ingesting simultaneous telemetry for multiple stations in the same city, `V6` threw unique constraint violation errors.
- **Resolution in `V7`**:
  ```sql
  ALTER TABLE weather_observations DROP CONSTRAINT IF EXISTS uq_weather_obs_city_time;
  ALTER TABLE weather_observations ADD CONSTRAINT uq_weather_obs_spatial_time UNIQUE (h3_index, observed_at);
  ```
- **Architectural Value**:
  Establishes spatial identity per H3 cell rather than coarse city-level identity, enabling true neighborhood-scale microclimate tracking.

---

## 22. Test Suite Results

### A. Phase 4 Weather Unit & Integration Tests

| Test Class | Test Count | Pass | Fail | Error | Notes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| [`WeatherClientTest.java`](file:///c:/Users/lenovo/AeroSential/backend/src/test/java/com/aerosentinel/integration/weather/WeatherClientTest.java) | 5 | 5 | 0 | 0 | Tests URL encoding, query params, timeouts, coordinate validation, error handling. |
| [`WeatherMapperTest.java`](file:///c:/Users/lenovo/AeroSential/backend/src/test/java/com/aerosentinel/integration/weather/WeatherMapperTest.java) | 9 | 9 | 0 | 0 | Tests hourly array mapping, timezone offset, bounds validation, source provenance. |
| [`WeatherIngestionServiceTest.java`](file:///c:/Users/lenovo/AeroSential/backend/src/test/java/com/aerosentinel/integration/weather/WeatherIngestionServiceTest.java) | 3 | 3 | 0 | 0 | Tests multi-station orchestration, duplicate handling, client failure handling. |
| [`LiveOpenMeteoVerificationTest.java`](file:///c:/Users/lenovo/AeroSential/backend/src/test/java/com/aerosentinel/integration/weather/LiveOpenMeteoVerificationTest.java) | 1 | 1 | 0 | 0 | Performs live HTTP call to Open-Meteo and validates physical response payload. |
| [`WeatherSpatialIntegrationTest.java`](file:///c:/Users/lenovo/AeroSential/backend/src/test/java/com/aerosentinel/integration/weather/WeatherSpatialIntegrationTest.java) | 6 | 6 | 0 | 0 | Tests PostgreSQL persistence, H3 assignment, PostGIS location, idempotency, grid cells. |
| **Total Weather Tests** | **24** | **24** | **0** | **0** | **100% Pass** |

### B. Full Backend Test Suite

```
[INFO] Results:
[INFO] 
[WARNING] Tests run: 105, Failures: 0, Errors: 0, Skipped: 1
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:01 min
[INFO] Finished at: 2026-09-25T23:46:01+05:30
```
- **Total Tests Run**: **105**
- **Failures**: **0**
- **Errors**: **0**
- **Skipped**: **1** (`LiveOpenAqIngestionVerificationTest` conditionally skipped unless `OPENAQ_API_KEY` is provided in environment)
- **Status**: **BUILD SUCCESS**

---

## 23. Mock Weather Audit

A complete audit of backend production source code was conducted:
- **`mockWeather` / `fakeWeather`**: **0 occurrences in production code**.
- **`generateMockWeather` / `generateSyntheticWeather`**: **0 occurrences in production code**.
- **Hardcoded Weather Values**: **0 occurrences in production code**.
- **`Instant.now()` used for `observed_at`**: **0 occurrences** (timestamps derive exclusively from Open-Meteo physical timestamps).
- **All 200 records in `weather_observations`**: Sourced with `source = 'OPEN_METEO'`.

---

## 24. Phase 4 Deliverables Checklist

- [x] Open-Meteo Weather Client implemented with Spring `RestTemplate` and timeouts.
- [x] Open-Meteo DTO and Jackson mapping implemented.
- [x] WeatherMapper implemented with unit conversion, timezone normalization (`Asia/Kolkata` $\to$ UTC `Instant`), and meteorological range checks.
- [x] Flyway migration `V7` applied: spatial uniqueness constraint on `(h3_index, observed_at)`.
- [x] Weather domain repository queries (`existsByH3IndexAndObservedAt`, `countByCityId`, `findByH3IndexOrderByObservedAtDesc`) added.
- [x] Weather domain service methods added.
- [x] Weather Ingestion Service implemented with station discovery, H3 indexing via `H3Service`, GridCell synchronization via `GridService`, and duplicate checking.
- [x] Startup backfill runner (`WeatherBackfillRunner`) implemented.
- [x] 15-minute periodic ingestion scheduler (`WeatherIngestionScheduler`) implemented.
- [x] Configuration added to `application.yml`.
- [x] Real weather data ingested for Pune, Mumbai, and Delhi (200 records across 8 stations).
- [x] Idempotency verified: re-running ingestion inserts 0 new rows and skips 200 duplicates.
- [x] PostGIS `location` trigger verified (`ST_IsValid = true`, `ST_GeometryType = ST_Point`).
- [x] 166 original F1 air quality observations completely preserved with 0 null H3 indexes.
- [x] Full test suite passing (105 tests, 0 failures, 0 errors).
- [x] Zero mock weather in production code.

---

## 25. Sign-off Recommendation

F2 Phase 4 has successfully fulfilled all technical, architectural, spatial, and data integrity requirements. The real weather ingestion pipeline is robust, idempotent, authenticated against real Open-Meteo endpoints, and fully integrated with the H3 spatial layer.

**Recommendation**: **LOCKED PASS — PROCEED TO F2 PHASE 5 (H3 SPATIAL + WEATHER REST APIS)**.
