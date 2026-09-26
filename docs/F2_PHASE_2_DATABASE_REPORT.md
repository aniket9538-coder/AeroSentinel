# AeroSentinel — FEATURE 2 (F2) — PHASE 2 REPORT
## DATABASE MIGRATION & SCHEMA HARDENING REPORT

**Feature**: F2 — Weather Integration + Centralized H3 Spatial Layer  
**Phase**: Phase 2 (Database Migration & Schema Hardening)  
**Execution Timestamp**: 2026-09-25T21:52:00+05:30  
**Phase Status**: **PASS**  

---

## 1. Migration Name
- **File**: [`backend/src/main/resources/db/migration/V6__f2_weather_and_h3_spatial_layer.sql`](file:///c:/Users/lenovo/AeroSential/backend/src/main/resources/db/migration/V6__f2_weather_and_h3_spatial_layer.sql)
- **Flyway Description**: `f2 weather and h3 spatial layer`
- **Flyway Version**: `6`
- **Checksum**: `-857994151`
- **Execution Time**: 305 ms

---

## 2. Existing Schema Inspected
Prior to authoring the migration, the complete schema, PostGIS extensions, constraints, and data types were audited in PostgreSQL:

| Table | Primary Key | Geometry Columns | Temporal Columns | Existing Foreign Keys | Row Count Pre-V6 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `cities` | `id` (UUID PK) | None (`lat`, `lon` doubles) | `created_at`, `updated_at` (`TIMESTAMPTZ`) | None | 3 |
| `monitoring_stations` | `id` (UUID PK) | `location GEOGRAPHY(Point,4326)` | `created_at` (`TIMESTAMPTZ`) | `city_id -> cities(id)` | 8 |
| `air_observations` | `id` (UUID PK) | `location GEOGRAPHY(Point,4326)` | `observed_at`, `created_at` (`TIMESTAMPTZ`) | `city_id -> cities(id)`, `station_id -> monitoring_stations(station_code)` | 166 |
| `weather_observations` | `id` (UUID PK) | `location GEOGRAPHY(Point,4326)` | `observed_at`, `created_at` (`TIMESTAMPTZ`) | `city_id -> cities(id)` | 0 |
| `grid_cells` | `id` (UUID PK) | `boundary GEOGRAPHY(Polygon,4326)` | `created_at` (`TIMESTAMPTZ`) | `city_id -> cities(id)` | 0 |

- **PostGIS Extension**: `POSTGIS="3.4.3 e365945"` verified active and functional.
- **Naming Conventions**: Snake case throughout (`air_observations`, `weather_observations`, `grid_cells`, `h3_index`, `observed_at`, `city_id`).
- **UUID Generation**: `uuid_generate_v4()`.

---

## 3. Exact Tables Changed

1. **`air_observations`**:
   - Added column `h3_index VARCHAR(30) NULL`.
   - Added index `idx_air_obs_h3` on `(h3_index)`.
   - Added index `idx_air_obs_h3_time` on `(h3_index, observed_at DESC)`.
   - Added check constraint `chk_air_obs_coords` validating latitude and longitude ranges.

2. **`weather_observations`**:
   - Added column `h3_index VARCHAR(30) NULL`.
   - Hardened `city_id` with `ALTER TABLE weather_observations ALTER COLUMN city_id SET NOT NULL`.
   - Added index `idx_weather_obs_h3` on `(h3_index)`.
   - Added index `idx_weather_obs_h3_time` on `(h3_index, observed_at DESC)`.
   - Added unique constraint and index `uq_weather_obs_city_time` on `(city_id, observed_at)`.
   - Added meteorological check constraints: `chk_weather_obs_humidity`, `chk_weather_obs_wind_speed`, `chk_weather_obs_wind_direction`, `chk_weather_obs_rainfall`, `chk_weather_obs_coords`.
   - Added PostGIS spatial synchronization trigger `trg_sync_weather_obs_location` executing `sync_weather_obs_location()`.

3. **`grid_cells`**:
   - Added index `idx_grid_cells_city_id` on `(city_id)`.
   - Added check constraint `chk_grid_cells_coords` validating center coordinates.

---

## 4. Exact Columns Added

| Table | Column Name | Data Type | Nullable | Default | Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `air_observations` | `h3_index` | `VARCHAR(30)` | **YES** | `NULL` | Uber H3 Resolution 8 spatial hexagon index. Nullable to guarantee zero disruption to existing F1 real observations. |
| `weather_observations` | `h3_index` | `VARCHAR(30)` | **YES** | `NULL` | Uber H3 Resolution 8 spatial hexagon index for weather telemetry. |

---

## 5. Exact Indexes Added

| Table | Index Name | Type | Key Columns | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| `air_observations` | `idx_air_obs_h3` | B-tree | `(h3_index)` | Rapid lookup of air quality observations by spatial cell. |
| `air_observations` | `idx_air_obs_h3_time` | B-tree | `(h3_index, observed_at DESC)` | High-performance queries for latest/historical air quality per cell. |
| `weather_observations` | `idx_weather_obs_h3` | B-tree | `(h3_index)` | Rapid lookup of weather observations by spatial cell. |
| `weather_observations` | `idx_weather_obs_h3_time` | B-tree | `(h3_index, observed_at DESC)` | High-performance queries for latest/historical weather per cell. |
| `weather_observations` | `uq_weather_obs_city_time` | B-tree (UNIQUE) | `(city_id, observed_at)` | Duplicate prevention ensuring idempotent telemetry ingestion. |
| `grid_cells` | `idx_grid_cells_city_id` | B-tree | `(city_id)` | City-level cell filtering for map grid loading. |

*Note: Existing indexes (`idx_air_obs_station_time`, `idx_air_obs_city_time`, `idx_air_obs_location`, `idx_weather_obs_city_time`, `idx_weather_obs_location`, `idx_grid_cells_boundary`, `idx_grid_cells_h3`) were inspected and preserved without duplication.*

---

## 6. Exact Constraints Added

| Table | Constraint Name | Type | Definition |
| :--- | :--- | :--- | :--- |
| `air_observations` | `chk_air_obs_coords` | CHECK | `(latitude >= -90 AND latitude <= 90 AND longitude >= -180 AND longitude <= 180)` |
| `weather_observations` | `uq_weather_obs_city_time` | UNIQUE | `UNIQUE (city_id, observed_at)` |
| `weather_observations` | `weather_observations_city_id_not_null` | NOT NULL | `city_id IS NOT NULL` |
| `weather_observations` | `chk_weather_obs_humidity` | CHECK | `(humidity IS NULL OR (humidity >= 0 AND humidity <= 100))` |
| `weather_observations` | `chk_weather_obs_wind_speed` | CHECK | `(wind_speed IS NULL OR wind_speed >= 0)` |
| `weather_observations` | `chk_weather_obs_wind_direction` | CHECK | `(wind_direction IS NULL OR (wind_direction >= 0 AND wind_direction <= 360))` |
| `weather_observations` | `chk_weather_obs_rainfall` | CHECK | `(rainfall IS NULL OR rainfall >= 0)` |
| `weather_observations` | `chk_weather_obs_coords` | CHECK | `(latitude >= -90 AND latitude <= 90 AND longitude >= -180 AND longitude <= 180)` |
| `grid_cells` | `chk_grid_cells_coords` | CHECK | `(center_latitude >= -90 AND center_latitude <= 90 AND center_longitude >= -180 AND center_longitude <= 180)` |

---

## 7. Duplicate Strategy

1. **Air Quality Duplicate Safety**:
   - Station-level observation uniqueness is guarded by application logic (`existsByStationIdAndObservedAt`) and backed by index `idx_air_obs_station_time` on `(station_id, observed_at DESC)`.
2. **Weather Observation Duplicate Safety**:
   - Analysis of `WeatherObservation.java`, `WeatherRepository.java`, and `WeatherController.java` established that weather observations represent city-wide atmospheric readings at a given provider observation timestamp.
   - Enforced database-level unique constraint `uq_weather_obs_city_time` on `(city_id, observed_at)`.
   - **Verification**: Executed a transactional test attempting to insert duplicate records with identical `(city_id, observed_at)`. PostgreSQL returned `ERROR: duplicate key value violates unique constraint "uq_weather_obs_city_time"`.

---

## 8. Existing F1 Row Counts Before / After

| Entity Table | Count Before V6 | Count After V6 | Delta | Data Integrity Status |
| :--- | :--- | :--- | :--- | :--- |
| `cities` | 3 | 3 | 0 | **100% Preserved** |
| `monitoring_stations` | 8 | 8 | 0 | **100% Preserved** |
| `air_observations` | 166 | 166 | 0 | **100% Preserved** |
| `weather_observations` | 0 | 0 | 0 | **Clean (Ready for Phase 4)** |
| `grid_cells` | 0 | 0 | 0 | **Clean (Ready for Phase 3)** |

Every single real observation from Pune (36 rows), Mumbai (52 rows), and Delhi (78 rows) remains intact with exact original `pm25`, `observed_at`, `station_id`, `city_id`, `source`, and `quality` values.

---

## 9. H3 NULL Counts

| Table | Total Rows | Rows with `h3_index IS NULL` | Rows with `h3_index NOT NULL` | Backfill Policy Compliance |
| :--- | :--- | :--- | :--- | :--- |
| `air_observations` | 166 | **166** | **0** | **100% Compliant** (ZERO fake H3 strings injected) |
| `weather_observations` | 0 | **0** | **0** | **100% Compliant** |

*In accordance with Phase 2 constraints, no synthetic H3 strings or mock coordinate formatters were used. Exactly 166 air observation rows have `h3_index IS NULL`. Phase 3 will perform the real deterministic backfill using `com.uber:h3:4.1.1`.*

---

## 10. Weather Row Count
- **Current Row Count**: **0 rows**  
- **Rationale**: Real weather ingestion is scheduled for **Phase 4** via `OpenMeteoClient`. No synthetic or mock weather data was introduced.

---

## 11. Grid Cell Row Count
- **Current Row Count**: **0 rows**  
- **Rationale**: Real H3 grid generation and persistence is scheduled for **Phase 3** using `H3Core`. No demo or synthetic polygons were inserted into the database.

---

## 12. Flyway Result
- **Command**: Spring Boot automatic Flyway migration on startup
- **Migration File**: `V6__f2_weather_and_h3_spatial_layer.sql`
- **Flyway Log Output**:
  ```text
  Successfully validated 6 migrations (execution time 00:00.216s)
  Current version of schema "public": 5
  Migrating schema "public" to version "6 - f2 weather and h3 spatial layer"
  Successfully applied 1 migration to schema "public", now at version v6 (execution time 00:00.305s)
  ```
- **Verification Query**:
  ```sql
  SELECT version, description, success, execution_time FROM flyway_schema_history WHERE version = '6';
  -- 6 | f2 weather and h3 spatial layer | t | 305
  ```

---

## 13. Tests Executed

### Unit & Repository Tests
- `OpenAqProviderUnitTest` (16 tests) — **PASS**
- `AirServiceUnitTest` (6 tests) — **PASS**
- `CityServiceUnitTest` (6 tests) — **PASS**
- `HealthControllerTest` (2 tests) — **PASS**
- Total: 30 tests, 0 failures, 0 errors.

### Integration & Contract Tests
- `AirQualityIntegrationTest` (11 tests) — **PASS**
- `F1ApiContractHardeningTest` (6 tests) — **PASS**
- Total: 17 tests, 0 failures, 0 errors.

### Live API Verification (HTTP 200)
- `GET /api/v1/cities`: Returned 3 active cities (Pune, Mumbai, Delhi).
- `GET /api/v1/cities/{puneId}/air-quality/latest`: Returned 3 stations with verified real PM2.5 readings.
- `GET /api/v1/cities/{mumbaiId}/air-quality/latest`: Returned 2 stations with verified real PM2.5 readings.
- `GET /api/v1/cities/{delhiId}/air-quality/latest`: Returned 3 stations with verified real PM2.5 readings.
- `GET /api/v1/stations/PUN-001/air-quality?from=...&to=...`: Returned 12 historical PM2.5 observations.
- `GET /api/v1/stations/MUM-001/air-quality?from=...&to=...`: Returned 18 historical PM2.5 observations.
- `GET /api/v1/stations/DEL-001/air-quality?from=...&to=...`: Returned 18 historical PM2.5 observations.
- `GET /api/v1/weather/current?cityId={puneId}`: Returned 404 Not Found (expected behavior prior to Phase 4 ingestion).

### PostGIS & Constraint Verification (Transactional Checks)
- **Spatial location generation**: Inserted point via trigger `sync_weather_obs_location()`, verified `location` populated with `POINT(73.8567 18.5204)` and `ST_DWithin` GiST spatial query matched successfully.
- **Duplicate rejection**: Verified duplicate `(city_id, observed_at)` raises `duplicate key value violates unique constraint "uq_weather_obs_city_time"`.
- **Meteorological range validation**: Verified `humidity = 150` raises `violates check constraint "chk_weather_obs_humidity"`.

---

## 14. Files Changed
1. [`backend/src/main/resources/db/migration/V6__f2_weather_and_h3_spatial_layer.sql`](file:///c:/Users/lenovo/AeroSential/backend/src/main/resources/db/migration/V6__f2_weather_and_h3_spatial_layer.sql) (Created Flyway migration V6)
2. [`docs/F2_PHASE_2_DATABASE_REPORT.md`](file:///c:/Users/lenovo/AeroSential/docs/F2_PHASE_2_DATABASE_REPORT.md) (This report)

---

## 15. Files NOT Changed
- Existing Flyway migrations: `V1__init_schema.sql`, `V2__seed_reference_data.sql`, `V3__spatial_indexes.sql`, `V4__create_authorities_table.sql`, `V5__f1_station_indexes_and_seed_observations.sql` (Strictly untouched).
- Java entity classes: `AirObservation.java`, `WeatherObservation.java`, `GridCell.java` (Deferred to Phase 3).
- Java services, clients, and controllers: No weather provider, Open-Meteo client, or H3 calculation logic implemented.
- Frontend files: No React or TypeScript modifications.
- Out-of-scope modules: Machine learning, forecasting, Gemini, citizen reports, satellite rasters, and alerts.

---

## 16. Known Limitations
1. **Air Observation H3 Index Currently Null**: All 166 existing air observations have `h3_index = NULL` because Phase 2 prohibited mock or synthetic H3 calculation.
2. **Weather Table Empty**: Contains 0 records; weather data pipeline will be implemented in Phase 4.
3. **Grid Table Empty**: Contains 0 records; resolution 8 cell generation will be implemented in Phase 3.

---

## 17. Phase 3 Prerequisites
Phase 3 (Uber H3 Java Core & Spatial Layer) is now fully unblocked. Prerequisites for Phase 3:
1. Add `com.uber:h3:4.1.1` Maven dependency to `backend/pom.xml`.
2. Update `H3Utils.java` to instantiate `H3Core` and expose real `latLngToCell(lat, lon, res)` and `cellToBoundary(h3Index)` methods.
3. Add `h3Index` field mapping to `AirObservation.java` and `WeatherObservation.java`.
4. Implement a deterministic backfill service to populate `h3_index` for all 166 existing air observation rows using station coordinates at resolution 8.
5. Implement `GridService` to generate and persist resolution 8 hexagon cells covering Pune, Mumbai, and Delhi.

---

## Final Verification Sign-Off

- [x] Flyway migration `V6` applied successfully in 305 ms.
- [x] `h3_index VARCHAR(30)` column added to `air_observations` and `weather_observations`.
- [x] 6 performance and spatial indexes added with zero duplicates.
- [x] Duplicate protection unique constraint `uq_weather_obs_city_time` active and verified.
- [x] Meteorological and coordinate check constraints active and verified.
- [x] PostGIS spatial trigger and GiST indexing verified.
- [x] Exactly 166 real F1 air observations preserved intact (0 deleted, 0 modified).
- [x] Exactly 166 rows in `air_observations` have `h3_index IS NULL` (0 fake H3 IDs injected).
- [x] Weather observations table contains 0 rows (0 mock weather rows).
- [x] Grid cells table contains 0 rows (0 mock grid cells).
- [x] All 47 backend unit, integration, and contract tests pass with 0 errors.
- [x] All live F1 REST APIs operational.

```text
F2_PHASE_2_DATABASE_STATUS = PASS
```
