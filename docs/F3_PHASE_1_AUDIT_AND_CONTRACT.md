# AeroSentinel — F3 Phase 1: Hotspot Detection Complete Audit & Implementation Contract

**Document Version:** 1.0.0  
**Phase Status:** PASS  
**Scope:** Architecture Audit, F2→F3 Data Mapping, Feature Contract Definition, API/DB Design, and ML Boundary Formulation  
**Prerequisites Locked:** F0 (PASS), F1 (PASS), F2 (PASS)  

---

## 1. Executive Summary & F3 Objective

The objective of **F3 (Potential Hotspot Detection)** is to transform raw, normalized multi-modal environmental telemetry (F2 ground air quality and surface weather observations) anchored to **Uber H3 Resolution 8 spatial grid cells** into an actionable, probabilistic risk indicator: **Potential Hotspot**.

### Core F3 Definition
A **Potential Hotspot** is defined mathematically and operationally as:
> A hyperlocal spatial hexagonal cell (H3 Resolution 8, area ~0.737 km²) exhibiting severe air pollution concentrations ($\text{PM}_{2.5} \ge 60\,\mu\text{g/m}^3$, the National Ambient Air Quality Standard) that simultaneously represents an anomalous local elevation above its surrounding leave-one-out spatial neighborhood mean ($\text{PM}_{2.5} \ge 1.15 \times \overline{\text{PM}_{2.5}}_{\text{neighbors}}$).

### Regulatory & Semantic Boundary
F3 is strictly an **early warning statistical risk signal**. It must **NEVER** be represented as:
- An official Central Pollution Control Board (CPCB) AQI metric.
- Definitive proof of industrial or vehicular emission non-compliance.
- A legal violation or regulatory penalty determination.
- Confirmed attribution to a specific factory or entity (attribution belongs to F5).

---

## 2. F2 → F3 Continuity & Architecture Chain

F2 established the production foundation for spatial intelligence and surface meteorology across Pune, Mumbai, and Delhi. F3 builds directly on top of F2 without creating duplicate tables, separate geospatial frameworks, or parallel API architectures.

```
+-----------------------------------------------------------------------------------+
|                            PRODUCTION F2 FOUNDATION                               |
|  - Real CPCB Air Observations (166 rows, H3 Resolution 8 indexed)                 |
|  - Real Open-Meteo Weather Observations (320 rows, H3 indexed, pressure/rain)     |
|  - PostGIS H3 Grid Registry (8 active cells, polygon boundaries, centroid lat/lon)|
|  - PostgreSQL Persistence & Multi-city Context (Pune, Mumbai, Delhi)              |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                        F3 FEATURE ENGINEERING LAYER                               |
|  - Hourly temporal alignment (UTC floor, Asia/Kolkata cyclical features)          |
|  - Wind vectorization (orthogonal u, v in m/s with calm threshold < 0.2 m/s)      |
|  - Leave-one-out spatial lag (zero self-information leakage)                      |
|  - Monitoring network density (nearest station distance, coverage gap > 7.0 km)   |
|  - Active fire advection & decay (NASA FIRMS 24h lookback, 25km radius)           |
|  - Hyperlocal GIS context (industrial proximity, sensitive receptors)             |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                   F3 POTENTIAL HOTSPOT DETECTION ENGINE                           |
|  - HotspotDetectionEngine interface (deterministic baseline vs ML calibrated RF)  |
|  - Platt-calibrated Random Forest (hotspot_classifier_v1.joblib, cutoff = 0.20)   |
|  - Emits: riskScore, riskLevel, confidence, modelVersion, generatedAt             |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                         DOWNSTREAM CONSUMERS (F4 & F5)                            |
|  - F4: Multi-Horizon PM2.5 Forecasting (T+1h, T+3h, T+6h)                         |
|  - F5: Structured Explainable Evidence & Gemini "WHY" Root-Cause Synthesis        |
+-----------------------------------------------------------------------------------+
```

---

## 3. Actual Repository & Production State Audit

A comprehensive code and database audit was performed across the entire repository.

### 3.1 Backend Audit (Spring Boot 3.3.4 / Java 21)
- **Controllers:**
  - `AirQualityController.java` (`/api/v1/air-quality`): Exposes live and historical air observations.
  - `WeatherController.java` (`/api/v1/weather`): Exposes multi-city weather telemetry and cell-level lookups (`/h3/{h3Index}`).
  - `GridController.java` (`/api/v1/grid/cells`): Returns active H3 Resolution 8 cells with GeoJSON polygon boundaries.
  - `CityController.java` (`/api/v1/cities`): Provides municipal reference registry (Pune, Mumbai, Delhi).
  - `HotspotController.java` (`/api/v1/hotspots`): Exists as an early unpopulated prototype querying `HotspotRepository`. Currently returns 0 rows.
- **Services:**
  - `H3Service.java`: Native Uber H3 engine (`H3Core`, Resolution 8 default, WKT boundary generation, centroid resolution).
  - `WeatherSpatialService.java`: Fuses air observations with weather observations per H3 cell with freshness status.
  - `WeatherIngestionService.java`: Ingests real Open-Meteo telemetry into PostgreSQL.
  - `HotspotService.java`: Basic CRUD repository wrapper; lacks feature extraction and scoring logic.
- **Database Migrations (`src/main/resources/db/migration`):**
  - `V1__init_schema.sql`: Created core tables (`cities`, `monitoring_stations`, `air_observations`, `weather_observations`, `grid_cells`, `grid_features`, `hotspot_predictions`).
  - `V2`–`V4`: Seed data, spatial indexes, and authorities.
  - `V5`: F1 station indexes and seed observations.
  - `V6`–`V7`: F2 spatial weather layer, H3 indexes, and uniqueness constraints (`uq_weather_obs_spatial_time`).
  - `V8__f3_feature_snapshots.sql`: Added for F3 feature persistence (`feature_snapshots`).

### 3.2 Frontend Audit (React 18 / TypeScript / Vite)
- **Current Route Structure (`App.tsx`):**
  - `/weather`: `WeatherSpatial.tsx` (Production F2 spatial weather intelligence page).
  - `/pollution-map`: `PollutionMap.tsx` (Interactive Mapbox/Leaflet station and grid visualization).
  - `/hotspots`: `Hotspots.tsx` (Currently contains static mock data with hardcoded cards and dummy H3 IDs).
  - `/forecast`: `Forecast.tsx` (F4 stub).
- **Navigation (`Sidebar.tsx`):**
  - MONITOR group contains: `Dashboard` $\to$ `Air Quality` $\to$ `Pollution Map` $\to$ `Weather & Spatial` $\to$ `Hotspots` $\to$ `Forecast`.
- **H3 Map Components:**
  - `H3RiskLayer.tsx`: Reusable deck.gl/Mapbox polygon layer rendering H3 hexagonal boundaries with color scales.
  - `PollutionMap.tsx`: Production map component supporting layer toggling (stations, heatmaps, H3 risk cells).
- **Store & Freshness (`AppContext.tsx`):**
  - Global `selectedCity`, `selectedStation`, `selectedCell`.
  - Freshness tokens: `LIVE`, `STALE`, `NO_DATA`, `SOURCE_UNAVAILABLE`.

### 3.3 Database Audit (PostgreSQL 16 + PostGIS)
Live inspection of `aerosentinel` database:
- `cities`: 3 records (`Pune`, `Mumbai`, `Delhi`).
- `monitoring_stations`: 8 active stations (3 Pune, 2 Mumbai, 3 Delhi).
- `air_observations`: 166 verified records; 100% have valid `h3_index`.
  - Pune observations have 100% complete co-pollutants (`pm10`, `no2`, `so2`, `co`, `o3`).
  - Mumbai and Delhi observations currently contain `pm25`.
- `weather_observations`: 320 verified records; 100% have valid `h3_index` from `OPEN_METEO`.
  - Verified variables: `temperature`, `humidity`, `wind_speed` (km/h), `wind_direction`, `rainfall`, `pressure` (hPa).
- `grid_cells`: 8 active Resolution 8 H3 cells with valid `boundary` geometries.
- `hotspot_predictions`: 0 records currently. Schema:
  - `id UUID PRIMARY KEY`, `grid_cell_id UUID REFERENCES grid_cells(id)`, `predicted_at TIMESTAMPTZ`, `risk_score FLOAT8`, `risk_level VARCHAR(20)`, `confidence FLOAT8`, `model_version VARCHAR(50)`, `explanation_status VARCHAR(50)`, `created_at TIMESTAMPTZ`.

---

## 4. F2 → F3 Data Mapping Contract

The following mapping defines how F2 operational telemetry maps directly to F3 consumers without synthetic fabrication:

| Domain | F2 Source Field | F2 Table | F3 Feature / Target | Unit | Status | Null / Fallback Rule |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Spatial** | `h3_index` | `grid_cells` | `h3_cell_id` | Hex string | **AVAILABLE** | Reject if invalid H3 string |
| **Spatial** | `center_latitude` | `grid_cells` | `latitude` | Degrees N | **AVAILABLE** | Recompute via H3 centroid if missing |
| **Spatial** | `center_longitude` | `grid_cells` | `longitude` | Degrees E | **AVAILABLE** | Recompute via H3 centroid if missing |
| **Air** | `pm25` | `air_observations` | `pm25_current` | $\mu\text{g/m}^3$ | **AVAILABLE** | Null flags feature vector as `MISSING` |
| **Air** | Historical `pm25` | `air_observations` | `pm25_previous` | $\mu\text{g/m}^3$ | **REQUIRES DERIVATION** | Query $T-1\text{h}$ observation for cell |
| **Air** | Historical `pm25` | `air_observations` | `pm25_change` | $\mu\text{g/m}^3$ | **REQUIRES DERIVATION** | $\text{PM}_{2.5}(T) - \text{PM}_{2.5}(T-1\text{h})$ |
| **Air** | Historical `pm25` | `air_observations` | `pm25_rate_of_change`| $\mu\text{g/m}^3/\text{h}$ | **REQUIRES DERIVATION** | Delta divided by elapsed hours |
| **Air** | Multi-station `pm25`| `air_observations` | `pm25_spatial_lag_mean`| $\mu\text{g/m}^3$ | **REQUIRES DERIVATION** | Leave-one-out concurrent station mean |
| **Co-Poll** | `pm10` | `air_observations` | `pm10` | $\mu\text{g/m}^3$ | **PARTIALLY AVAILABLE** | Available in Pune; mark `MISSING` in MUM/DEL |
| **Co-Poll** | `no2` | `air_observations` | `no2` | $\mu\text{g/m}^3$ | **PARTIALLY AVAILABLE** | Available in Pune; mark `MISSING` in MUM/DEL |
| **Co-Poll** | `so2` | `air_observations` | `so2` | $\mu\text{g/m}^3$ | **PARTIALLY AVAILABLE** | Available in Pune; mark `MISSING` in MUM/DEL |
| **Co-Poll** | `co` | `air_observations` | `co` | $\text{mg/m}^3$ | **PARTIALLY AVAILABLE** | Available in Pune; mark `MISSING` in MUM/DEL |
| **Co-Poll** | `o3` | `air_observations` | `o3` | $\mu\text{g/m}^3$ | **PARTIALLY AVAILABLE** | Available in Pune; mark `MISSING` in MUM/DEL |
| **Weather** | `temperature` | `weather_observations` | `temperature` | $^\circ\text{C}$ | **AVAILABLE** | Latest $t \le T$ observation; default 25.0 |
| **Weather** | `humidity` | `weather_observations` | `humidity` | $\%$ | **AVAILABLE** | Range $[0, 100]$; default 50.0 |
| **Weather** | `wind_speed` | `weather_observations` | `wind_speed` | $\text{km/h}$ | **AVAILABLE** | Converted to $\text{m/s}$ for vector ($/ 3.6$) |
| **Weather** | `wind_direction` | `weather_observations` | `wind_direction` | Degrees | **AVAILABLE** | Range $[0, 360]$ from North clockwise |
| **Weather** | `wind_speed`, `dir` | Derived | `wind_u`, `wind_v` | $\text{m/s}$ | **REQUIRES DERIVATION** | $-ws \cdot \sin(\theta)$, $-ws \cdot \cos(\theta)$ |
| **Weather** | `rainfall` | `weather_observations` | `rainfall` | $\text{mm}$ | **AVAILABLE** | Clipped $\ge 0.0$; default 0.0 |
| **Weather** | `pressure` | `weather_observations` | `pressure` | $\text{hPa}$ | **AVAILABLE** | Open-Meteo surface pressure (default 954.3) |
| **Network** | Station registry | `monitoring_stations` | `nearest_station_distance_km` | $\text{km}$ | **REQUIRES DERIVATION** | Minimum Haversine distance to network |
| **Network** | Station registry | `monitoring_stations` | `monitoring_coverage_gap_flag`| $\{0, 1\}$ | **REQUIRES DERIVATION** | $1$ if nearest station $> 7.0\,\text{km}$, else $0$ |
| **Fires** | NASA FIRMS stream | `fire_events` | Active fire features | $\text{MW}, \text{km}$ | **REQUIRES DERIVATION** | 24h lookback, 25km radius; default 0 on absence |
| **GIS** | GeoJSON layers | Vector layers | Industrial/road dist | $\text{km}$ | **REQUIRES DERIVATION** | PMR regional defaults if layers absent |

---

## 5. Detailed F3 Feature Contract

### 5.1 Air Quality Feature Group
1. **`pm25_current`**:
   - *Source:* `air_observations.pm25`.
   - *Unit:* $\mu\text{g/m}^3$.
   - *Temporal Rule:* Most recent valid observation strictly $\le T$.
   - *Validation:* $0.0 \le \text{pm25} \le 1500.0$.
2. **`pm25_previous`**:
   - *Source:* `air_observations.pm25` at $T - 1\text{h} \pm 30\text{m}$.
   - *Unit:* $\mu\text{g/m}^3$.
   - *Null Behavior:* If unpopulated, set to `pm25_current` ($\Delta = 0$).
3. **`pm25_change`**:
   - *Formula:* $\text{pm25\_current} - \text{pm25\_previous}$.
   - *Unit:* $\mu\text{g/m}^3$.
4. **`pm25_rate_of_change`**:
   - *Formula:* $\Delta \text{pm25} / \Delta t_{\text{hours}}$.
   - *Unit:* $\mu\text{g/m}^3/\text{h}$.
5. **`pm25_spatial_lag_mean`**:
   - *Formula:* $\frac{\sum_{i \ne \text{self}} \text{pm25}_i}{N - 1}$ across concurrent city stations at time $T$.
   - *Rule:* **Strict Leave-One-Out**. Own station PM2.5 is excluded to prevent target leakage. Single station fallback: own value.
   - *Unit:* $\mu\text{g/m}^3$.

### 5.2 Meteorological Feature Group
1. **`temperature`**: Ambient temperature at 2m height ($^\circ\text{C}$). Range: $[-20.0, 60.0]$.
2. **`humidity`**: Relative humidity at 2m height ($\%$). Range: $[0.0, 100.0]$.
3. **`wind_speed`**: Raw surface wind velocity ($\text{km/h}$). Converted to $\text{m/s}$ via $v_{\text{m/s}} = v_{\text{km/h}} / 3.6$.
4. **`wind_direction`**: Meteorological angle FROM which wind originates ($0^\circ = \text{North}, 90^\circ = \text{East}$).
5. **`wind_u` & `wind_v`**:
   - *Calm Wind Handling:* If $v_{\text{m/s}} < 0.2\,\text{m/s} \implies u = 0.0, v = 0.0$.
   - *Trigonometric Formula:* $u = -v_{\text{m/s}} \cdot \sin(\theta_{\text{rad}})$, $v = -v_{\text{m/s}} \cdot \cos(\theta_{\text{rad}})$.
   - *Unit:* $\text{m/s}$.
6. **`rainfall`**: Accumulated liquid precipitation ($\text{mm}$). Clipped to $\ge 0.0$.
7. **`pressure`**: Surface atmospheric barometric pressure ($\text{hPa}$). Valid: $[300.0, 1150.0]$.

### 5.3 Spatial & Network Feature Group
1. **`h3_cell_id`**: Canonical Uber H3 Resolution 8 index (e.g., `88608850e5fffff`).
2. **`nearest_station_distance_km`**: Great-circle Haversine distance to nearest distinct monitoring station.
3. **`stations_within_5km_count`**: Count of monitoring stations within 5.0 km radius (including self $= 1$).
4. **`monitoring_coverage_gap_flag`**: Binary flag indicating spatial uncertainty:
   $$\text{monitoring\_coverage\_gap\_flag} = \begin{cases} 1 & \text{if } \text{nearest\_station\_distance\_km} > 7.0\,\text{km} \\ 0 & \text{otherwise} \end{cases}$$

### 5.4 Temporal Feature Group
- Anchor: Converted from UTC to **`Asia/Kolkata`** local solar time.
- `hour`: Local hour $[0, 23]$.
- `day_of_week`: Day index $[0, 6]$ where Monday $= 0$, Sunday $= 6$.
- `is_weekend`: $1$ if $\text{day\_of\_week} \ge 5$, else $0$.
- `hour_sin`, `hour_cos`: $\sin(2\pi \cdot \text{hour} / 24)$, $\cos(2\pi \cdot \text{hour} / 24)$.
- `dow_sin`, `dow_cos`: $\sin(2\pi \cdot \text{day\_of\_week} / 7)$, $\cos(2\pi \cdot \text{day\_of\_week} / 7)$.

### 5.5 NASA FIRMS Active Fire Feature Group
- *Lookback Window:* Strictly $[T - 24\text{h}, T]$ (no future fire observations permitted).
- *Spatial Range:* Radius $\le 25.0\,\text{km}$.
- *Absence Rule:* If zero fires detected, legitimately returns:
  `fire_count_24h_25km = 0`, `fire_frp_sum_24h_25km = 0.0`, `fire_frp_mean_24h_25km = 0.0`, `nearest_fire_distance_km = 50.0`, `fire_frp_distance_decay = 0.0`, `fire_upwind_alignment_score = 0.0`.
- *Distance Decay:* $\sum \frac{\text{FRP}_i}{d_i + 1.0}$ ($\text{MW/km}$).
- *Upwind Alignment:* Evaluates fire corridor relative to wind vector within $\pm 45^\circ$ alignment:
  $$\text{score} = \sum_{|\Delta\theta| \le 45^\circ} \frac{\text{FRP}_i \cdot \cos(\Delta\theta_i)}{d_i + 1.0}$$

### 5.6 Authoritative Model Feature Order (36 Predictors)
Inspected directly from `hotspot_classifier_v1.joblib` and `train_all_models.py select_features()`:
```
 1. latitude                        19. wind_u
 2. longitude                       20. wind_v
 3. pm10                            21. rainfall
 4. no2                             22. pressure
 5. so2                             23. pm25_spatial_lag_mean
 6. co                              24. nearest_station_distance_km
 7. o3                              25. stations_within_5km_count
 8. hour                            26. monitoring_coverage_gap_flag
 9. day_of_week                     27. dist_to_nearest_industrial_km
10. is_weekend                      28. dist_to_nearest_major_road_km
11. hour_sin                        29. sensitive_receptors_count_2km
12. hour_cos                        30. industrial_zone_within_2km_flag
13. dow_sin                         31. fire_count_24h_25km
14. dow_cos                         32. fire_frp_sum_24h_25km
15. temperature                     33. fire_frp_mean_24h_25km
16. humidity                        34. nearest_fire_distance_km
17. wind_speed                      35. fire_frp_distance_decay
18. wind_direction                  36. fire_upwind_alignment_score
```

---

## 6. Future ML Integration Boundary & Engine Abstraction

To ensure modularity and zero disruption to frontend or database layers when switching between baseline rules and Python ML microservices, F3 defines the **`HotspotDetectionEngine`** pluggable interface:

```
                          +-------------------------------+
                          |    HotspotDetectionService    |
                          |  - Builds FeatureRecord       |
                          |  - Validates Quality Status   |
                          |  - Persists Snapshot          |
                          +-------------------------------+
                                          |
                                          v
                          +-------------------------------+
                          |   <<HotspotDetectionEngine>>  |
                          |   + evaluate(FeatureRecord)   |
                          +-------------------------------+
                                    /           \
                                   /             \
                                  v               v
           +-----------------------------+  +-------------------------------+
           |    RuleBaselineEngine       |  |     PythonMlAdapterEngine     |
           |  - Deterministic NAAQS &    |  |  - REST/gRPC client           |
           |    Spatial Anomaly Formula  |  |  - Calls /ai/v1/hotspot/predict|
           |  - Cold-start & fallback    |  |  - Calibrated Random Forest   |
           +-----------------------------+  +-------------------------------+
```

### Decoupling Invariants
1. **Frontend Isolation:** React UI calls Spring Boot `GET /api/v1/hotspots`. It is completely unaware of whether the risk score was generated by an in-process heuristic or an external Random Forest service.
2. **Database Immutability:** `hotspot_predictions` stores canonical fields (`risk_score`, `risk_level`, `confidence`, `model_version`). The model version string identifies the engine provenance (e.g., `hotspot-baseline-v1` vs `hotspot-rf-calibrated-v1`).
3. **Resilience Fallback:** If the external Python AI service returns HTTP 500 or times out, the backend gracefully delegates to `RuleBaselineEngine` while tagging `explanation_status = "HEURISTIC_FALLBACK"`.

---

## 7. F3 API Contract Specification (Draft)

### 7.1 Application-Facing Backend API (Spring Boot)
**Endpoint:** `GET /api/v1/hotspots`  
**Query Parameters:**
- `cityId` (UUID, optional): Filter by municipal boundary.
- `cellId` (UUID / String, optional): Filter by specific H3 cell ID or index.

**Response Schema (`200 OK`):**
```json
{
  "cityId": "550e8400-e29b-41d4-a716-446655440001",
  "cityName": "Pune",
  "generatedAt": "2026-09-26T12:00:00Z",
  "freshness": "LIVE",
  "hotspots": [
    {
      "id": "c1f8e21a-4d32-4789-9a10-8b1e4a5c6d7e",
      "h3Index": "88608850e5fffff",
      "gridCellId": "955f1f67-e5ff-48ef-87b6-133ff958b756",
      "locationName": "Shivajinagar CAAQMS Sector",
      "riskScore": 0.78,
      "riskLevel": "HIGH",
      "confidence": 0.85,
      "isPotentialHotspot": true,
      "modelVersion": "hotspot-rf-calibrated-v1",
      "predictedAt": "2026-09-26T12:00:00Z",
      "signals": [
        "PM2.5 concentration (74.2 µg/m³) exceeds NAAQS standard (60 µg/m³)",
        "Local spatial elevation +24% above neighborhood mean (59.8 µg/m³)",
        "Low dispersion wind velocity (1.8 m/s) from East"
      ],
      "metrics": {
        "pm25": 74.2,
        "spatialLagMean": 59.8,
        "windSpeed": 1.8,
        "temperature": 27.4,
        "humidity": 58.0
      }
    }
  ]
}
```

### 7.2 AI Service Internal Boundary API (Python)
**Endpoint:** `POST /ai/v1/hotspot/predict`  
**Request Payload:**
```json
{
  "h3_cell_id": "88608850e5fffff",
  "observed_at": "2026-09-26T12:00:00Z",
  "features": {
    "latitude": 18.5315,
    "longitude": 73.8471,
    "pm10": 112.4,
    "no2": 38.6,
    "so2": 14.2,
    "co": 1.45,
    "o3": 28.1,
    "hour": 17,
    "day_of_week": 5,
    "is_weekend": 1,
    "hour_sin": -0.9659,
    "hour_cos": -0.2588,
    "dow_sin": -0.7818,
    "dow_cos": 0.6235,
    "temperature": 27.4,
    "humidity": 58.0,
    "wind_speed": 6.5,
    "wind_direction": 95.0,
    "wind_u": -1.798,
    "wind_v": -0.157,
    "rainfall": 0.0,
    "pressure": 954.3,
    "pm25_spatial_lag_mean": 59.8,
    "nearest_station_distance_km": 8.2,
    "stations_within_5km_count": 1,
    "monitoring_coverage_gap_flag": 1,
    "dist_to_nearest_industrial_km": 3.5,
    "dist_to_nearest_major_road_km": 0.4,
    "sensitive_receptors_count_2km": 4,
    "industrial_zone_within_2km_flag": 0,
    "fire_count_24h_25km": 0,
    "fire_frp_sum_24h_25km": 0.0,
    "fire_frp_mean_24h_25km": 0.0,
    "nearest_fire_distance_km": 50.0,
    "fire_frp_distance_decay": 0.0,
    "fire_upwind_alignment_score": 0.0
  }
}
```

---

## 8. Database Architecture & Prediction History Design

The existing `hotspot_predictions` table structure from `V1__init_schema.sql` is well-formed and integrates with `grid_cells(id)`:

```sql
CREATE TABLE IF NOT EXISTS hotspot_predictions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    grid_cell_id UUID REFERENCES grid_cells(id) ON DELETE CASCADE,
    predicted_at TIMESTAMPTZ NOT NULL,
    risk_score DOUBLE PRECISION NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    explanation_status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMPTZ DEFAULT now()
);
```

### Essential Hardening & Enhancements for Phase 2:
1. **Immutable Historical Audit Trail:**
   - Predictions must **never be overwritten**. Each evaluation timestamp $T$ creates a distinct row.
   - Unique constraint: `CONSTRAINT uq_hotspot_cell_time_model UNIQUE (grid_cell_id, predicted_at, model_version)`.
2. **Feature Traceability:**
   - Link predictions to feature snapshots via `feature_snapshots` table (created in Flyway V8):
     `feature_snapshot_id UUID REFERENCES feature_snapshots(id)`.
3. **Indexing Strategy:**
   - `CREATE INDEX idx_hotspots_cell_predicted ON hotspot_predictions(grid_cell_id, predicted_at DESC);`
   - `CREATE INDEX idx_hotspots_risk_time ON hotspot_predictions(risk_level, predicted_at DESC);`

---

## 9. Frontend Integration Point & UI State Machine

### 9.1 Existing Host Page
The existing route `/hotspots` (`frontend/src/pages/public/Hotspots.tsx`) is the primary host page.
- **Current State:** Contains hardcoded mock items (`hotspot-01`, `hotspot-02`, etc.).
- **Target Integration:**
  - Remove all mock static arrays.
  - Connect to `useApp()` context for `selectedCity` and active monitoring stations.
  - Consume real predictions from `GET /api/v1/hotspots?cityId={selectedCity.id}` via `hotspotService.ts`.
  - Share the existing `H3RiskLayer` polygon geometry on `PollutionMap.tsx`.

### 9.2 UI State Machine
The page must strictly adhere to the project's standard 5-state lifecycle:

```
           +-----------------------------+
           |           LOADING           |  (Spinner / Skeleton Cards)
           +-----------------------------+
                          |
             +------------+------------+
             |                         |
             v                         v
+-------------------------+  +-------------------------+
|         SUCCESS         |  |          ERROR          | (Retry button,
| (Hexagons + Rank Cards) |  |                         |  network failure banner)
+-------------------------+  +-------------------------+
             |
             +------------+------------+
             |                         |
             v                         v
+-------------------------+  +-------------------------+
|          STALE          |  |         NO_DATA         | (Zero active stations
| (Telemetry > 2h old)    |  |                         |  or unmonitored city)
+-------------------------+  +-------------------------+
```

---

## 10. Problem-Statement Alignment

| Problem Statement Challenge | F2 Contribution | F3 Hotspot Detection Contribution |
| :--- | :--- | :--- |
| **Spatial Monitoring Blind Spots** | Maps point stations to 0.737 km² H3 cells. | Detects elevated risk in unmonitored buffer zones using spatial lags and proximity features. |
| **Emerging Hyperlocal Episodes** | Tracks real-time raw PM2.5 and weather. | Computes localized rate-of-change ($\Delta \text{PM}_{2.5}/\text{h}$) to catch spikes before regional AQI shifts. |
| **Lack of Unified Spatial Context** | Collocates weather + air observations. | Feeds 36 multi-modal predictors into calibrated decision boundary ($p \ge 0.20$). |
| **Lagging Regulatory Metrics** | Displays static 24h rolling AQI. | Delivers hourly dynamic risk scores ($[0.0, 1.0]$) categorized into actionable risk levels. |

---

## 11. Testing & Validation Contract

Phase 2 implementation must fulfill the following automated test matrix:

1. **Feature Precision:**
   - Exactly 36 numeric predictors generated in authoritative order.
   - Wind speed converted from km/h to m/s ($/ 3.6$) exactly once.
   - Orthogonal wind vectors ($u, v$) follow meteorological convention; calm winds ($< 0.2\,\text{m/s}$) yield $(0, 0)$.
2. **Zero Self-Information Leakage:**
   - Station's own PM2.5 must never be included in its spatial lag mean.
3. **Zero Future Temporal Leakage:**
   - Satellite and fire observations must strictly satisfy $t \le T_{\text{observation}}$.
4. **Coverage Gap Rule:**
   - `nearest_station_distance_km > 7.0` must deterministically set `monitoring_coverage_gap_flag = 1`.
5. **Quality & Missingness Resilience:**
   - Missing co-pollutants must be flagged as `MISSING` or `UNAVAILABLE`.
   - Never replace missing values with synthetic random numbers or silent zero fallbacks.
6. **F1 & F2 Regression:**
   - All 14 existing weather unit tests, API contract tests, and frontend TypeScript compilation (`tsc --noEmit`) must remain 100% green.

---

## 12. Missing Items, Gaps & Implementation Risks

| Item / Dependency | Current Status | Risk Level | Mitigation in Phase 2 |
| :--- | :--- | :--- | :--- |
| `HotspotController.java` | **REQUIRES IMPLEMENTATION** (currently returns empty list) | Medium | Implement service layer with real DB retrieval & DTO mapper |
| `Hotspots.tsx` UI | **REQUIRES IMPLEMENTATION** (currently hardcoded static mocks) | Medium | Remove mocks, bind to `/api/v1/hotspots`, handle 5 UI states |
| Co-pollutants in Mumbai / Delhi | **MISSING** in live station stream | Low | Flag as `MISSING`; model validated primarily for Pune domain |
| Live NASA FIRMS API Worker | **MISSING** (table empty) | Low | Deterministic zero-fill fallback on absence per Member 3 logic |
| Live Sentinel-5P GEE Pipeline | **MISSING** (table empty) | Low | Authoritative 36-feature schema excludes satellite predictors |

---

## 13. Exact Phase 2 Implementation Plan

Phase 2 will be executed in three strictly sequenced stages:

### Stage 2.1: Feature Data Foundation (Spring Boot Backend)
1. Complete `FeatureEngineeringService.java` to assemble the 36-feature vector from PostgreSQL observations.
2. Apply Flyway migration `V8__f3_feature_snapshots.sql` to record feature snapshots for reproducibility.
3. Wire `HotspotDetectionService` with `RuleBaselineEngine` to generate deterministic potential hotspot records.

### Stage 2.2: API & Controller Integration
1. Update `HotspotController.java` to serve `GET /api/v1/hotspots?cityId=...`.
2. Map `hotspot_predictions` entities to `HotspotPredictionDto` with signals, confidence, and metadata.
3. Add backend contract tests verifying non-empty responses for Pune cells.

### Stage 2.3: Frontend Real Data Integration
1. Create `frontend/src/services/hotspotService.ts`.
2. Purge all mock data from `frontend/src/pages/public/Hotspots.tsx`.
3. Connect `Hotspots.tsx` to real API, wire city switching, and support `LIVE`, `STALE`, `NO_DATA`, `ERROR` states.
4. Execute `npm run build` and `npx tsc --noEmit` to confirm zero regression.

---

## 14. Definition of Done Checklist

- [x] Actual F2 implementation audited (controllers, entities, repositories, database rows).
- [x] F2 $\to$ F3 data flow and field-level continuity documented.
- [x] Required F3 features identified and cataloged across 5 categories.
- [x] Every feature has documented source, mathematical formula, unit, and null policy.
- [x] F3 REST API request and response contracts specified.
- [x] Pluggable `HotspotDetectionEngine` integration boundary defined.
- [x] Database schema and immutability strategy for predictions established.
- [x] Frontend integration point identified; mock purge planned.
- [x] Complete 5-state UI lifecycle defined.
- [x] Problem statement alignment clearly articulated.
- [x] Zero production code changed in Phase 1 (Audit only).
- [x] Existing F2 architecture remains 100% intact.
- [x] Phase 2 implementation blueprint fully detailed.

---

## 15. Final Declaration

**F3 PHASE 1 STATUS: PASS**
