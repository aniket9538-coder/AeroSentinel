# AeroSentinel — F2 Phase 7 Report: Freshness + Error Hardening

**Phase Status**: PASS  
**Timestamp**: 2026-09-26  
**Scope**: F2 Weather & Spatial Intelligence Freshness Lifecycle & Error Boundary Hardening  

---

## 1. Freshness Architecture

The freshness evaluation in AeroSentinel F2 strictly reuses the canonical F1 temporal lifecycle design established in [frontend/src/utils/freshness.ts](file:///c:/Users/lenovo/AeroSential/frontend/src/utils/freshness.ts) and the unified DTO model.

```
+-----------------------------------------------------------------------------------+
|                            Authoritative Freshness Flow                           |
|                                                                                   |
|  Open-Meteo Provider API           PostgreSQL `weather_observations`              |
|           |                                       |                               |
|           v                                       v                               |
|   `time` (ISO-8601 UTC)                  `observed_at` (TIMESTAMPTZ)              |
|           |                                       |                               |
|           +-------------------+-------------------+                               |
|                               |                                                   |
|                               v                                                   |
|                 Backend DTO: `observedAt`                                         |
|                               |                                                   |
|                               v                                                   |
|             Frontend API Client: `WeatherObservation`                             |
|                               |                                                   |
|                               v                                                   |
|            `computeFreshnessStatus(observedAt, threshold)`                        |
|                               |                                                   |
|         +---------------------+---------------------+                             |
|         |                     |                     |                             |
|         v                     v                     v                             |
|   `LIVE` (<24h)        `STALE` (>=24h)     `NO_DATA` (null/empty)                 |
|         ^                                                                         |
|         | Provider Error                                                          |
|         +--------------------> Downgrades to `STALE` if cached DB data exists     |
|                                Displays `SOURCE_UNAVAILABLE` if no data exists    |
+-----------------------------------------------------------------------------------+
```

### Core Architectural Invariants
1. **Authoritative Timestamp**: `observedAt` is the **only** timestamp evaluated to determine whether data is `LIVE` or `STALE`.
2. **Strict Exclusions**:
   - `createdAt` (database record creation timestamp) is **never** used.
   - Frontend request time or refresh time is **never** used.
   - HTTP 200 OK status code **never** automatically confers `LIVE` status.
3. **Four Canonical States**:
   - `LIVE`: Observation timestamp is within the configured freshness threshold.
   - `STALE`: Observation timestamp is older than the configured threshold, OR upstream provider is unavailable while displaying last-known data.
   - `NO_DATA`: City or cell has zero recorded observations.
   - `SOURCE_UNAVAILABLE`: Upstream provider or network failed and no fallback observations exist.

---

## 2. Threshold Specification & Calculation

### Configuration
- **Location**: [frontend/src/utils/freshness.ts](file:///c:/Users/lenovo/AeroSential/frontend/src/utils/freshness.ts)
- **Default Prototype Value**: `DEFAULT_STALE_THRESHOLD_HOURS = 24` (86,400,000 milliseconds)
- **Status**: Configurable prototype threshold (per project convention, **not** an official regulatory SLA).

### Mathematical Calculation
$$\Delta t = t_{\text{current}} - t_{\text{observed}}$$

$$\text{Status} = \begin{cases} 
\text{NO\_DATA} & \text{if } t_{\text{observed}} \text{ is null, undefined, or empty} \\
\text{LIVE} & \text{if } 0 \le \Delta t \le \text{Threshold}_{\text{ms}} \\
\text{STALE} & \text{if } \Delta t > \text{Threshold}_{\text{ms}} \\
\text{LIVE} & \text{if } -60000 \le \Delta t < 0 \quad (\text{allowable client clock skew } \le 1\text{ min}) \\
\text{STALE} & \text{if } \Delta t < -60000 \quad (\text{suspicious future timestamp})
\end{cases}$$

### Timezone Handling
- All provider timestamps from Open-Meteo are parsed and stored as UTC ISO-8601 (`TIMESTAMPTZ` in PostgreSQL).
- Backend serializes `observedAt` as ISO-8601 UTC with standard `Z` designator (e.g. `2026-09-26T03:00:00Z`).
- Frontend evaluates freshness using millisecond epoch comparison via `Date.parse(observedAt)`. Timezone offsets (such as IST `+05:30` or UTC `Z`) are normalized to UTC epoch milliseconds before subtraction, preventing any localized timezone drift.

---

## 3. Weather Freshness Behavior

The weather freshness lifecycle was tested end-to-end from API payload through UI rendering:

| Scenario | Input Condition | Resolved State | UI Badge | Display Details |
|---|---|---|---|---|
| **Recent Observation** | $t_{\text{current}} - t_{\text{observed}} = 1.5\text{h} \le 24\text{h}$ | `LIVE` | Emerald `LIVE` | Displays real temperature, humidity, wind, rainfall |
| **Old Observation** | $t_{\text{current}} - t_{\text{observed}} = 26.0\text{h} > 24\text{h}$ | `STALE` | Amber `STALE` | Displays last-known metrics with exact relative age |
| **Zero Observations** | Valid city with empty observation table | `NO_DATA` | Slate `NO DATA` | Displays clean empty state without synthetic metrics |
| **Provider Error (Empty DB)** | Open-Meteo HTTP 503, no DB records | `SOURCE_UNAVAILABLE` | Red `SOURCE UNAVAILABLE` | Displays provider error banner with Retry button |
| **Provider Error (Existing DB)** | Open-Meteo HTTP 503, last record exists | `STALE` | Amber `STALE` | Displays last-known data + degraded provider notice |

---

## 4. Air + Weather Consistency

The F2 Weather & Spatial Intelligence dashboard presents both air quality and meteorological observations in [frontend/src/components/weather/CellDetailsCard.tsx](file:///c:/Users/lenovo/AeroSential/frontend/src/components/weather/CellDetailsCard.tsx).

1. **Independent Timestamps**: Air observations and weather observations maintain their own discrete `observedAt` timestamps. They are never conflated or unified into a single synthetic timestamp.
2. **Discrete Sources**:
   - Air observations clearly attribute provider sources: `CPCB` or `OPENAQ`.
   - Weather observations clearly attribute provider sources: `OPEN_METEO`.
3. **Discrete Metric Validations**:
   - Air card displays $PM_{2.5}$, $PM_{10}$, and $AQI$.
   - Weather card displays Temperature, Relative Humidity, Wind Speed, Wind Direction, and Rainfall.

---

## 5. Source Unavailable Hardening

When the Open-Meteo API or ingestion worker encounters upstream failures:
1. **Zero Synthetic Generation**: The application **never** generates fake weather values, random values, or replaces missing data with hardcoded estimates.
2. **Timestamp Preservation**: The application **never** substitutes the current clock time for a missing or stale provider observation.
3. **Graceful Degradation with Last-Known DB State**:
   - If PostgreSQL contains previously ingested observations for the city/cell, [frontend/src/components/weather/WeatherSummaryCard.tsx](file:///c:/Users/lenovo/AeroSential/frontend/src/components/weather/WeatherSummaryCard.tsx) displays the last-known observation.
   - The status is strictly capped at `STALE` (even if the last record was recent, it is flagged as degraded/stale).
   - An amber alert banner is rendered: `"Upstream provider sync degraded ({error}). Displaying last-known observation."` with an interactive `Retry` button.
4. **Total Outage State**:
   - If no historical observations exist in PostgreSQL, the card transitions cleanly to `SOURCE_UNAVAILABLE` with an error state and direct Retry trigger.

---

## 6. No-Data Handling

For any valid city or cell with zero recorded weather observations:
- Status resolves strictly to `NO_DATA`.
- UI renders a clear informational notice: `"No weather observations available for this city."`
- **Zero Fallback Numbers**: The UI **does not** render `0°C`, `0%`, `0 km/h`, or `0.0 mm` as placeholders. Numerical values are only shown when explicitly returned by the provider.
- Empty arrays for observations remain empty (`observations: []`).

---

## 7. API Error Handling

All canonical F2 REST endpoints strictly adhere to the unified AeroSentinel `ApiError` convention:

| Endpoint | Error Code | Trigger Condition | Handled Response Format |
|---|---|---|---|
| `/api/v1/cities/{cityId}/weather/latest` | `404 NOT_FOUND` | Unknown `cityId` (e.g. 99999) | `{"status":404, "error":"Not Found", "message":"City not found with id: 99999", "timestamp":"..."}` |
| `/api/v1/grid?cityId={cityId}` | `404 NOT_FOUND` | Non-existent `cityId` | `{"status":404, "error":"Not Found", "message":"City not found with id: 99999", "timestamp":"..."}` |
| `/api/v1/grid/{h3Index}` | `400 BAD_REQUEST` | Malformed H3 string (e.g. `invalid-h3`) | `{"status":400, "error":"Bad Request", "message":"Invalid H3 index: invalid-h3", "timestamp":"..."}` |
| `/api/v1/grid/{h3Index}` | `404 NOT_FOUND` | Valid H3 index not in database | `{"status":404, "error":"Not Found", "message":"Grid cell not found with index: ...", "timestamp":"..."}` |
| `/api/v1/grid/{h3Index}/observations` | `200 OK` | Valid cell with no data | `{"airObservations":[], "weatherObservations":[]}` |
| All Endpoints | Network / 500 | Server down / network disconnect | Caught by `weatherApi` / `gridApi`, converted to typed `ApiException`, triggers retryable UI error state |

No secondary or non-standard error contracts were introduced.

---

## 8. Frontend State Separation

[frontend/src/pages/public/WeatherSpatial.tsx](file:///c:/Users/lenovo/AeroSential/frontend/src/pages/public/WeatherSpatial.tsx) maintains 6 distinct, uncollapsed states:

1. **Loading State**: Displays skeleton pulses / loading indicators while asynchronous fetches resolve.
2. **Live State**: Emerald indicator rendered when data is loaded and `observedAt` is within 24 hours.
3. **Stale State**: Amber badge rendered when data exceeds threshold or upstream provider is degraded.
4. **No-Data State**: Slate badge and informational card rendered when zero records exist.
5. **Error State**: Red alert card rendered on 4xx/500/network errors with message and `Retry` action.
6. **Offline State**: Ambient banner and badge when `navigator.onLine === false`.

---

## 9. City Switching Isolation

When switching between cities (e.g., **Pune $\rightarrow$ Mumbai $\rightarrow$ Delhi**):
1. **Complete State Reset**:
   ```typescript
   setSelectedCell(null);
   setCellDetails(null);
   setWeather(null);
   setGridCells([]);
   setGridLoading(true);
   setWeatherLoading(true);
   setGridError(null);
   setWeatherError(null);
   ```
2. **Race Condition Prevention**:
   - `fetchIdRef.current` increments monotonically on every city switch.
   - In-flight HTTP requests from previous cities verify `if (currentFetchId !== fetchIdRef.current) return;` before updating state.
   - Old weather data, old cell details, old H3 boundaries, and old error messages **never leak** into the new city context.

---

## 10. H3 Cell Switching Isolation

When selecting or deselecting H3 hexagonal cells:
1. **Immediate Previous Cell Clearing**: The prior cell's air and weather observation arrays are immediately cleared.
2. **Cell Request Sequencing**:
   - `cellFetchIdRef.current` increments on every cell selection.
   - If a user rapidly clicks from Cell A to Cell B, an in-flight response for Cell A arriving after Cell B is selected is discarded immediately.
   - Stale cell observation payloads cannot overwrite the currently focused cell.

---

## 11. Offline Detection & Network Fault Tolerance

- Utilizes native browser network state detection (`navigator.onLine` and `window.addEventListener('online'/'offline')`).
- When offline:
  - Network requests fail immediately with descriptive error `"Network connection unavailable"`.
  - The UI does **not** falsely claim current data is `LIVE`.
  - The offline status is clearly displayed on the interface.

---

## 12. Backend Provider Ingestion Failure Safety

Audited [backend/src/main/java/com/aerosentinel/integration/weather/WeatherIngestionService.java](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/integration/weather/WeatherIngestionService.java):
1. **No Data Deletion**: Provider HTTP timeouts, non-200 responses, or parse errors do **not** trigger deletions, truncations, or cascading drops.
2. **No Data Overwrites**: Existing valid observations in PostgreSQL are preserved untouched.
3. **No Synthetic Insertion**: If the provider returns empty arrays or invalid formats, the ingestion service logs a structured warning and aborts without inserting rows.
4. **Spatial & H3 Integrity**: H3 cell indexing only occurs when valid geographic coordinates and meteorology are parsed; failed requests never corrupt the `grid_cells` table.

---

## 13. Validation & Constraint Review

Audited [backend/src/main/java/com/aerosentinel/integration/weather/WeatherMapper.java](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/integration/weather/WeatherMapper.java) and PostgreSQL schema `V6__f2_weather_and_h3_spatial_layer.sql`:

| Metric | Required Range | Enforced By | Violation Behavior |
|---|---|---|---|
| **Relative Humidity** | `0.0%` to `100.0%` | `WeatherMapper.java` + DB `CHECK (humidity >= 0 AND humidity <= 100)` | Exception thrown, record rejected |
| **Wind Speed** | $\ge 0.0\text{ m/s}$ | `WeatherMapper.java` + DB `CHECK (wind_speed >= 0)` | Exception thrown, record rejected |
| **Wind Direction** | `0.0°` to `360.0°` | `WeatherMapper.java` + DB `CHECK (wind_direction >= 0 AND wind_direction <= 360)` | Exception thrown, record rejected |
| **Rainfall** | $\ge 0.0\text{ mm}$ | `WeatherMapper.java` + DB `CHECK (rainfall >= 0)` | Exception thrown, record rejected |
| **Coordinates** | Lat: `[-90, 90]`, Lon: `[-180, 180]` | DB constraints & `H3Utils.java` | Exception thrown, record rejected |

Zero validation relaxation was introduced.

---

## 14. Timestamp Review

| Field | Meaning | Storage Column | API Property | UI Presentation |
|---|---|---|---|---|
| **`observedAt`** | Instant provider measured meteorological phenomenon | `observed_at` (`TIMESTAMPTZ`) | `observedAt` | Authoritative timestamp displayed on cards, used for freshness |
| **`createdAt`** | Instant row was inserted into local PostgreSQL | `created_at` (`TIMESTAMPTZ`) | N/A (Internal audit) | Never displayed in F2 UI, never used for freshness calculation |

No substitution of `createdAt` for `observedAt` exists in any production F2 path.

---

## 15. Mock & Synthetic Data Audit

A rigorous pattern audit was conducted across all active production code in `frontend/src/` and `backend/src/main/`:

| Search Pattern | Occurrences in F2 Production Path | Classification | Context / Notes |
|---|---|---|---|
| `Math.random` | 0 | PRODUCTION: 0, UNRELATED: 1 | 1 occurrence in unrelated F6 CitizenReport for temporary local file upload client ID |
| `mockWeather` | 0 | PRODUCTION: 0, TEST: 0 | None |
| `fakeWeather` | 0 | PRODUCTION: 0, TEST: 0 | None |
| `syntheticWeather` | 0 | PRODUCTION: 0, TEST: 0 | None |
| `fakeH3` | 0 | PRODUCTION: 0, TEST: 0 | None |
| `mockH3` | 0 | PRODUCTION: 0, TEST: 0 | None |
| `gridDisk` | 0 | PRODUCTION: 0, TEST: 0 | Real coordinate-to-H3 conversion only |
| Hardcoded Weather Numbers | 0 | PRODUCTION: 0 | All values derived from PostgreSQL API responses |
| Hardcoded Timestamps | 0 | PRODUCTION: 0 | All timestamps derived from provider/database |
| Hardcoded H3 Indices | 0 | PRODUCTION: 0 | Generated dynamically by Uber H3 library |

**Audit Verdict**: The F2 production path contains **ZERO** active mock or synthetic data.

---

## 16. Targeted Test Execution

### Frontend Unit Tests ([frontend/src/utils/freshness.test.ts](file:///c:/Users/lenovo/AeroSential/frontend/src/utils/freshness.test.ts))
Ran Node test runner on hardened freshness suite:
- **Total Tests**: 21 passed (0 failed, 0 errors)
- **Coverage**:
  - `computeFreshnessStatus`: `NO_DATA` for null/undefined/empty string
  - `computeFreshnessStatus`: `LIVE` for recent observation (1 hour ago)
  - `computeFreshnessStatus`: `STALE` for observation exceeding 24 hours (25 hours ago)
  - `computeFreshnessStatus`: exact boundary handling at 24.0 hours
  - `computeFreshnessStatus`: future timestamp tolerance (<= 1 minute) vs skew rejection (> 1 minute)
  - `computeFreshnessStatus`: custom threshold parameter verification
  - `formatFreshnessAge`: relative time formatting (`Just now`, `X minutes ago`, `X hours ago`, `X days ago`)
  - `isFresh`: boolean utility compliance
  - **F2 Targeted Tests**:
    1. Weather observation within threshold yields `LIVE`
    2. Weather observation older than threshold yields `STALE`
    3. Null weather observation yields `NO_DATA`
    4. Provider failure with existing DB observation forces `STALE` (never `LIVE`)
    5. UTC ISO-8601 timestamps parsed identically across client timezones
    6. City-switch request sequencing cancels in-flight responses
    7. Cell-switch observation state isolated between H3 cells

### Backend Contract Tests ([backend/src/test/](file:///c:/Users/lenovo/AeroSential/backend/src/test/))
Ran targeted Spring Boot REST API contract tests against running test container:
```bash
.\mvnw.cmd test "-Dtest=F2WeatherApiContractTest,F2GridApiContractTest,F2CellObservationApiContractTest,F1ApiContractHardeningTest"
```
- **F1ApiContractHardeningTest**: 6 of 6 passed
- **F2CellObservationApiContractTest**: 10 of 10 passed
- **F2GridApiContractTest**: 12 of 12 passed
- **F2WeatherApiContractTest**: 7 of 7 passed
- **Total Backend Tests**: **35 of 35 passed** (0 failures, 0 errors, 0 skipped)

---

## 17. Build & Static Analysis Results

### 1. Static TypeScript Type Checking
```bash
cd frontend && npx tsc --noEmit
```
- **Exit Code**: `0`
- **Output**: Clean, zero diagnostic errors.

### 2. Production Vite Bundle Build
```bash
cd frontend && npm run build
```
- **Exit Code**: `0`
- **Output**:
  ```text
  vite v5.4.21 building for production...
  transforming...
  ✓ 2531 modules transformed.
  rendering chunks...
  computing gzip size...
  dist/index.html                     1.21 kB │ gzip:   0.66 kB
  dist/assets/index-reyWygwD.css      5.83 kB │ gzip:   1.98 kB
  dist/assets/index-DDQC72GC.js   1,243.89 kB │ gzip: 346.68 kB
  ✓ built in 36.74s
  ```

---

## 18. Files Changed and Files Untouched

### Files Changed (Targeted Hardening Only)
1. [frontend/src/components/weather/WeatherSummaryCard.tsx](file:///c:/Users/lenovo/AeroSential/frontend/src/components/weather/WeatherSummaryCard.tsx):
   - Added `isSourceUnavailableError` helper.
   - Connected `FreshnessStatus` computation directly to card state.
   - Implemented provider-failure fallback to last-known data with `STALE` status cap and warning banner.
   - Cleanly separated `SYNCING...`, `SOURCE_UNAVAILABLE`, `ERROR`, `NO_DATA`, `LIVE`, and `STALE` status badges.
2. [frontend/src/utils/freshness.test.ts](file:///c:/Users/lenovo/AeroSential/frontend/src/utils/freshness.test.ts):
   - Expanded unit tests with 7 targeted F2 freshness, provider failure, timezone, and race sequencing assertions.

### Files Untouched
- [frontend/src/pages/public/WeatherSpatial.tsx](file:///c:/Users/lenovo/AeroSential/frontend/src/pages/public/WeatherSpatial.tsx) (UI structure and navigation preserved)
- [frontend/src/components/map/H3GridLayer.tsx](file:///c:/Users/lenovo/AeroSential/frontend/src/components/map/H3GridLayer.tsx) (Map and H3 layer preserved)
- [backend/src/main/java/com/aerosentinel/util/H3Utils.java](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/util/H3Utils.java) (Uber H3 engine untouched)
- [backend/src/main/java/com/aerosentinel/integration/weather/WeatherIngestionService.java](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/integration/weather/WeatherIngestionService.java) (Ingestion pipeline untouched)
- All F1 modules, entities, and services untouched.

---

## 19. Phase 8 Prerequisites

With Phase 7 Freshness + Error Hardening completed and passing:
1. Real Open-Meteo weather pipeline is locked.
2. Real Uber H3 spatial engine and hexagonal grid partitioning are locked.
3. Canonical REST API contracts (`/weather/latest`, `/grid`, `/grid/{h3Index}`, `/grid/{h3Index}/observations`) are locked.
4. UI state separation, request race sequencing, and freshness lifecycle are locked.
5. Zero mock/synthetic data in production path is verified.
6. The codebase is fully prepared for Phase 8: Final End-to-End Verification & Demonstration.

---

F2_PHASE_7_STATUS = PASS
