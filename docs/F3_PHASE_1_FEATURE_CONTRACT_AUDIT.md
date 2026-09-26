# AeroSentinel — F3 Phase 1: Exact ML Feature Contract + Production Gap Audit

**Phase Status**: PASS  
**Timestamp**: 2026-09-26  
**Auditor**: Antigravity AI Engineering Team  
**Scope**: Exact Feature Specification, Physical Derivations, Unit Mapping, and Production F2 Gap Audit for the F3 Hotspot Detection Model (`hotspot_classifier_v1.joblib`)

---

## 1. Executive Summary & F3 Objective

The objective of AeroSentinel Feature 3 (F3) is **Hyperlocal Pollution Hotspot Detection**: identifying emerging localized pollution events ($PM_{2.5} \ge 60.0\ \mu\text{g/m}^3$) across unmonitored Uber H3 hexagonal cells (Resolution 8) by combining ground telemetry, multi-sensor meteorological forcing, remote sensing, and urban GIS topology.

```
+---------------------------------------------------------------------------------------------------+
|                                 F3 Intelligence Pipeline Flow                                     |
|                                                                                                   |
|  CPCB / OpenAQ Ground Telemetry  +  Open-Meteo Meteorology  +  NASA FIRMS  +  Sentinel-5P + GIS  |
|                                                |                                                  |
|                                                v                                                  |
|                           Feature Engineering & Spatial Alignment                                 |
|                                                |                                                  |
|                                                v                                                  |
|                         Exact 35-Predictor Feature Vector [X_t]                                   |
|                                                |                                                  |
|                                                v                                                  |
|           hotspot_classifier_v1.joblib (Platt-Calibrated Random Forest Classifier)                |
|                                                |                                                  |
|                      +-------------------------+-------------------------+                        |
|                      |                                                   |                        |
|                      v                                                   v                        |
|        Calibrated Risk Score [0.0, 1.0]                     Multi-Factor Confidence               |
|         Threshold: 0.20 -> is_hotspot                      (Margin, Completeness, Distance)       |
|                      |                                                   |                        |
|                      +-------------------------+-------------------------+                        |
|                                                |                                                  |
|                                                v                                                  |
|                   Non-Causal Traceable Evidence Generation (evidence.py)                          |
+---------------------------------------------------------------------------------------------------+
```

### Purpose of Phase 1
Production F3 has **not** started yet. This audit establishes the exact mathematical, temporal, spatial, and unit contracts for every input required by the existing trained F3 machine learning artifact, and conducts a rigorous gap analysis against our live, real-data F2 production foundation.

---

## 2. Model Artifact Details (`hotspot_classifier_v1.joblib`)

The technical specification of the model artifact was recovered from Member 3's engineering handoff report (`AeroSentinel F3.docx`), architectural documentation (`docs/MEMBER3_F3_REPORT.md`), and training pipeline specifications:

| Attribute | Specification | Source / Verification |
|---|---|---|
| **Artifact Filename** | `hotspot_classifier_v1.joblib` | Member 3 F3 Technical Handoff Report |
| **Companion Forecaster** | `forecast_regressors_v1.joblib` | Multi-horizon ($T+1\text{h}, T+3\text{h}, T+6\text{h}$) Random Forest Regressors |
| **Model Architecture** | Cost-Sensitive Random Forest with Post-Hoc Platt Sigmoid Probability Calibration | `CalibratedClassifierCV(method='sigmoid', cv=3)` wrapping `RandomForestClassifier` |
| **Base Estimator** | `sklearn.ensemble.RandomForestClassifier` | `n_estimators=100`, `max_depth=12`, `min_samples_split=5`, `class_weight='balanced'`, `random_state=42`, `n_jobs=-1` |
| **Calibration Method** | Platt Sigmoid Scaling (3-fold internal out-of-fold cross-validation) | Converts tree voting fractions into true posterior probabilities: $P(y=1 \mid X) = \frac{1}{1 + \exp(A \cdot f(X) + B)}$ |
| **Operational Threshold** | **`0.20`** (Calibrated probability) | Set to 0.20 to prioritize Recall (0.812) over Precision (0.584) during public health episodes |
| **Target Definition** | $y_{\text{hotspot}} = \mathbb{I}(PM_{2.5} \ge 60.0\ \mu\text{g/m}^3)$ | Statutory CPCB 24-hour National Ambient Air Quality Standard (NAAQS) threshold |
| **Benchmark Metrics** | Test PR-AUC: **0.742**, F1-Score: **0.679**, ROC-AUC: **0.891**, Brier Score: **0.0482** | Evaluated on untouched holdout test partition (10,528 records) |
| **Stored Contents** | Python dictionary containing: `model`, `feature_cols`, `operational_threshold`, `algorithm`, `benchmark_summary` | Joblib binary serialization format |
| **Inference Libraries** | `scikit-learn>=1.4.0`, `joblib>=1.3.0`, `numpy>=1.26.0`, `pandas>=2.2.0`, `pydantic>=2.7.0` | Python 3.11 execution runtime |
| **Physical Disk Status** | Serialized on Member 3 development workstation (`C:\Users\Harsh\AeroSentinel\ai-service\models\artifacts\hotspot_classifier_v1.joblib`). | **GAP IDENTIFIED**: The physical `.joblib` binary file was not transferred into the current workspace `ai-service/artifacts/models/`. |

---

## 3. Authoritative 35-Feature Input Matrix

The model strictly consumes an ordered 1-D vector of **35 numeric predictors**. The table below establishes the authoritative feature contract:

| # | Feature Name | Category | Datatype | Training Unit | Exact Source | Time Window | Spatial Scope |
|---|---|---|---|---|---|---|---|
| 1 | **`temperature_2m`** | Weather | `float32` | °C | Open-Meteo / ERA5 | Current $T$ | Point / Station |
| 2 | **`relative_humidity_2m`** | Weather | `float32` | % | Open-Meteo / ERA5 | Current $T$ | Point / Station |
| 3 | **`surface_pressure`** | Weather | `float32` | hPa | Open-Meteo / ERA5 | Current $T$ | Point / Station |
| 4 | **`wind_speed`** | Weather | `float32` | m/s | Open-Meteo / ERA5 | Current $T$ | Point / Station |
| 5 | **`wind_direction`** | Weather | `float32` | Degrees (0–360) | Open-Meteo / ERA5 | Current $T$ | Point / Station |
| 6 | **`wind_u`** | Weather | `float32` | m/s | Derived (Trigonometric) | Current $T$ | Point / Station |
| 7 | **`wind_v`** | Weather | `float32` | m/s | Derived (Trigonometric) | Current $T$ | Point / Station |
| 8 | **`boundary_layer_height`** | Weather | `float32` | m | Open-Meteo / ERA5 | Current $T$ | Point / Station |
| 9 | **`pm10`** | Co-Pollutant | `float32` | µg/m³ | CAAQMS Ground | Current $T$ | Point / Station |
| 10 | **`no2`** | Co-Pollutant | `float32` | µg/m³ | CAAQMS Ground | Current $T$ | Point / Station |
| 11 | **`so2`** | Co-Pollutant | `float32` | µg/m³ | CAAQMS Ground | Current $T$ | Point / Station |
| 12 | **`co`** | Co-Pollutant | `float32` | mg/m³ | CAAQMS Ground | Current $T$ | Point / Station |
| 13 | **`ozone`** | Co-Pollutant | `float32` | µg/m³ | CAAQMS Ground | Current $T$ | Point / Station |
| 14 | **`pm25_spatial_lag_mean`** | Spatial Lag | `float32` | µg/m³ | CAAQMS Network | Current $T$ | Leave-One-Out ($k=1$) |
| 15 | **`pm25_spatial_lag_std`** | Spatial Lag | `float32` | µg/m³ | CAAQMS Network | Current $T$ | Leave-One-Out ($k=1$) |
| 16 | **`fire_count_24h_25km`** | Active Fire | `int32` | Count | NASA FIRMS VIIRS | $[T-24\text{h}, T]$ | 25 km buffer |
| 17 | **`fire_frp_sum_24h_25km`** | Active Fire | `float32` | MW | NASA FIRMS VIIRS | $[T-24\text{h}, T]$ | 25 km buffer |
| 18 | **`fire_frp_mean_24h_25km`** | Active Fire | `float32` | MW | NASA FIRMS VIIRS | $[T-24\text{h}, T]$ | 25 km buffer |
| 19 | **`nearest_fire_distance_km`** | Active Fire | `float32` | km | NASA FIRMS VIIRS | $[T-24\text{h}, T]$ | Geodesic (50 km cap) |
| 20 | **`fire_frp_distance_decay`** | Active Fire | `float32` | MW/km | NASA FIRMS VIIRS | $[T-24\text{h}, T]$ | Inverse Distance |
| 21 | **`fire_upwind_alignment_score`** | Active Fire | `float32` | Scalar | NASA FIRMS + Wind | $[T-24\text{h}, T]$ | Upwind Corridor |
| 22 | **`satellite_no2_trop`** | Satellite | `float64` | mol/m² | Sentinel-5P TROPOMI | Latest cloud-free | Res 8 Hexagon |
| 23 | **`satellite_cloud_fraction`** | Satellite | `float32` | Ratio [0, 1] | Sentinel-5P TROPOMI | Latest overpass | Res 8 Hexagon |
| 24 | **`dist_to_nearest_industrial_km`** | GIS Context | `float32` | km | MIDC GIS Polygons | Static | Geodesic Minimum |
| 25 | **`dist_to_nearest_major_road_km`** | GIS Context | `float32` | km | OpenStreetMap Roads | Static | Geodesic Minimum |
| 26 | **`industrial_zone_within_2km_flag`** | GIS Context | `int32` | Binary [0, 1] | MIDC GIS Polygons | Static | 2.0 km buffer |
| 27 | **`sensitive_receptors_count_2km`** | GIS Context | `int32` | Count | Schools / Hospitals | Static | 2.0 km radius |
| 28 | **`nearest_station_distance_km`** | Topology | `float32` | km | CAAQMS Registry | Static | Geodesic Minimum |
| 29 | **`stations_within_5km_count`** | Topology | `int32` | Count | CAAQMS Registry | Static | 5.0 km radius |
| 30 | **`monitoring_coverage_gap_flag`** | Topology | `int32` | Binary [0, 1] | Spatial Index | Static | $d_{\text{station}} > 5.0\text{ km}$ |
| 31 | **`hour_sin`** | Temporal | `float32` | Scalar [-1, 1] | Timestamp UTC | Current $T$ | Global |
| 32 | **`hour_cos`** | Temporal | `float32` | Scalar [-1, 1] | Timestamp UTC | Current $T$ | Global |
| 33 | **`day_of_week_sin`** | Temporal | `float32` | Scalar [-1, 1] | Timestamp UTC | Current $T$ | Global |
| 34 | **`day_of_week_cos`** | Temporal | `float32` | Scalar [-1, 1] | Timestamp UTC | Current $T$ | Global |
| 35 | **`is_weekend`** | Temporal | `int32` | Binary [0, 1] | Timestamp UTC | Current $T$ | Global |

*(Note: `is_daytime` is an auxiliary diurnal indicator documented in Member 3's exploratory matrix; the core model artifact `feature_cols` contains exactly the 35 predictors above).*

### Strictly Excluded Columns (Anti-Leakage Audit)
The following columns were explicitly dropped prior to model fitting to prevent data leakage and trivial identity memorization:
- **`observed_at`**, **`hourly_bin`**: Timestamp indexes.
- **`station_id`**, **`city_id`**, **`h3_cell_id`**: Categorical entity IDs (prevents memorize-by-ID leakage).
- **`pm25`**, **`pm25_raw`**, **`pm25_clean`**: Current ground-truth target values (prevents identity mapping).
- **`target_pm25_t_plus_1`**, **`target_pm25_t_plus_3`**, **`target_pm25_t_plus_6`**: Future forward-shifted targets.
- **`satellite_aerosol_index`**, **`satellite_co_column`**, **`satellite_so2_column`**: Excluded due to $>95\%$ missingness under monsoon/cloud regimes.

---

## 4. Mathematical Formulations & Derivations

### A. Orthogonal Wind Vector Decomposition (`wind_u`, `wind_v`)
Wind direction $\theta_{\text{deg}} \in [0^\circ, 360^\circ)$ denotes the direction the wind is coming *from* ($0^\circ = \text{North}, 90^\circ = \text{East}$).
$$\theta_{\text{rad}} = \theta_{\text{deg}} \cdot \frac{\pi}{180.0}$$
$$u = \begin{cases} -w_{\text{speed}} \cdot \sin(\theta_{\text{rad}}) & \text{if } w_{\text{speed}} \ge 0.2\text{ m/s} \\ 0.0 & \text{if } w_{\text{speed}} < 0.2\text{ m/s (calm)} \end{cases}$$
$$v = \begin{cases} -w_{\text{speed}} \cdot \cos(\theta_{\text{rad}}) & \text{if } w_{\text{speed}} \ge 0.2\text{ m/s} \\ 0.0 & \text{if } w_{\text{speed}} < 0.2\text{ m/s (calm)} \end{cases}$$

### B. Leave-One-Out Spatial Neighbor Lag (`pm25_spatial_lag_mean`, `pm25_spatial_lag_std`)
To prevent a station from learning an identity mapping of its own concentration, the regional background spatial lag is calculated by strictly excluding station $i$:
$$\text{spatial\_lag}_i(t) = \frac{\left(\sum_{j=1}^{N_t} PM_{2.5, j}(t)\right) - PM_{2.5, i}(t)}{N_t - 1}$$
$$\text{spatial\_lag\_std}_i(t) = \sqrt{\frac{1}{N_t - 2} \sum_{j \ne i}^{N_t} (PM_{2.5, j}(t) - \text{spatial\_lag}_i(t))^2}$$
If $N_t \le 1$, the spatial lag falls back safely to regional historical baseline.

### C. Active Fire Distance Decay (`fire_frp_distance_decay`)
Inverse-distance weighted radiative forcing from active thermal anomalies detected within 25 km in the preceding 24 hours:
$$\text{Decay}_{\text{fire}} = \sum_{i \in \text{fires}_{25\text{km}}} \frac{\text{FRP}_i}{d_i + 1.0}$$
where $d_i$ is the Haversine geodesic distance in kilometers:
$$d_i = 2 R \cdot \arcsin\left(\sqrt{\sin^2\left(\frac{\Delta\phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta\lambda}{2}\right)}\right), \quad R = 6371.0\text{ km}$$

### D. Wind-Aligned Active Fire Plume Advection Score (`fire_upwind_alignment_score`)
Evaluates whether active fire plumes are being transported directly toward the receptor cell along the wind corridor:
1. Great-circle bearing $\beta_i$ from station to fire:
   $$y = \sin(\Delta\lambda)\cos(\phi_2), \quad x = \cos(\phi_1)\sin(\phi_2) - \sin(\phi_1)\cos(\phi_2)\cos(\Delta\lambda)$$
   $$\beta_i = (\text{degrees}(\text{atan2}(y, x)) + 360.0) \pmod{360.0}$$
2. Circular angular separation $\Delta\theta_i$ relative to wind oncoming direction $\theta_{\text{wind}}$:
   $$\Delta\theta_i = |(\beta_i - \theta_{\text{wind}} + 180.0) \pmod{360.0} - 180.0|$$
3. Upwind alignment accumulation (fires within a $90^\circ$ advection cone, i.e., $\Delta\theta_i \le 45.0^\circ$):
   $$\text{Score}_{\text{upwind}} = \sum_{i \in \text{fires}, \Delta\theta_i \le 45^\circ} \frac{\text{FRP}_i \cdot \cos(\Delta\theta_i)}{d_i + 1.0} \quad (\text{if } w_{\text{speed}} \ge 0.2\text{ m/s})$$

### E. Diurnal and Weekly Cyclical Harmonics (`hour_sin`, `hour_cos`, `day_of_week_sin`, `day_of_week_cos`)
$$\text{hour\_sin} = \sin\left(\frac{2\pi \cdot \text{hour}_{\text{UTC}}}{24}\right), \quad \text{hour\_cos} = \cos\left(\frac{2\pi \cdot \text{hour}_{\text{UTC}}}{24}\right)$$
$$\text{dow\_sin} = \sin\left(\frac{2\pi \cdot \text{dow}}{7}\right), \quad \text{dow\_cos} = \cos\left(\frac{2\pi \cdot \text{dow}}{7}\right)$$
$$\text{is\_weekend} = \mathbb{I}(\text{dow} \in \{5, 6\}) \quad (\text{Saturday or Sunday})$$

---

## 5. Production F2 Availability & Feature Classification

Every feature is classified into exactly one of four categories based on our live, running F2 infrastructure:
- **DIRECT**: Already populated and available in production database/API.
- **DERIVED**: Computed directly from existing real production data without new external sources.
- **NEW_REAL_DATA**: Requires real external ingestion or loading static real vector layers.
- **NOT_AVAILABLE**: Cannot be produced safely (Zero features in this category; all have defined real pathways).

| # | Feature Name | Classification | Current F2 Production Status | Action Required to Bridge Gap |
|---|---|---|---|---|
| 1 | `temperature_2m` | **DIRECT** | Available (`weather_observations.temperature`) | Map field name from `temperature` to `temperature_2m` |
| 2 | `relative_humidity_2m` | **DIRECT** | Available (`weather_observations.humidity`) | Map field name from `humidity` to `relative_humidity_2m` |
| 3 | `surface_pressure` | **NEW_REAL_DATA** | Column exists in DB, currently null | Add `surface_pressure` to Open-Meteo URL query params |
| 4 | `wind_speed` | **DIRECT** | Available (`weather_observations.wind_speed`) | **Unit conversion**: Convert km/h to m/s ($v / 3.6$) |
| 5 | `wind_direction` | **DIRECT** | Available (`weather_observations.wind_direction`) | Direct passthrough |
| 6 | `wind_u` | **DERIVED** | Derived from real `wind_speed` & `wind_direction` | Implement trigonometric vector decomposition in AI service |
| 7 | `wind_v` | **DERIVED** | Derived from real `wind_speed` & `wind_direction` | Implement trigonometric vector decomposition in AI service |
| 8 | `boundary_layer_height` | **NEW_REAL_DATA** | Missing from DB schema | Add `boundary_layer_height` to DB & Open-Meteo ingestion |
| 9 | `pm10` | **NEW_REAL_DATA** | Populated for Pune (36 rows), null for Mumbai/Delhi | Expand OpenAQ / CPCB ingestion to include $PM_{10}$ |
| 10 | `no2` | **NEW_REAL_DATA** | Populated for Pune (36 rows), null for Mumbai/Delhi | Expand OpenAQ / CPCB ingestion to include $NO_2$ |
| 11 | `so2` | **NEW_REAL_DATA** | Populated for Pune (36 rows), null for Mumbai/Delhi | Expand OpenAQ / CPCB ingestion to include $SO_2$ |
| 12 | `co` | **NEW_REAL_DATA** | Populated for Pune (36 rows), null for Mumbai/Delhi | Expand OpenAQ / CPCB ingestion to include $CO$ |
| 13 | `ozone` | **NEW_REAL_DATA** | Populated for Pune (36 rows), null for Mumbai/Delhi | Expand OpenAQ / CPCB ingestion to include $O_3$ |
| 14 | `pm25_spatial_lag_mean` | **DERIVED** | Computable from `air_observations.pm25` | Implement leave-one-out spatial lag calculation across cells |
| 15 | `pm25_spatial_lag_std` | **DERIVED** | Computable from `air_observations.pm25` | Implement spatial standard deviation calculation across cells |
| 16 | `fire_count_24h_25km` | **NEW_REAL_DATA** | Table `fire_events` exists, zero rows | Implement NASA FIRMS active fire ingestion worker |
| 17 | `fire_frp_sum_24h_25km` | **NEW_REAL_DATA** | Table `fire_events` exists, zero rows | Implement NASA FIRMS active fire ingestion worker |
| 18 | `fire_frp_mean_24h_25km` | **NEW_REAL_DATA** | Table `fire_events` exists, zero rows | Implement NASA FIRMS active fire ingestion worker |
| 19 | `nearest_fire_distance_km` | **NEW_REAL_DATA** | Table `fire_events` exists, zero rows | Implement NASA FIRMS active fire ingestion worker |
| 20 | `fire_frp_distance_decay` | **NEW_REAL_DATA** | Table `fire_events` exists, zero rows | Implement NASA FIRMS active fire ingestion worker |
| 21 | `fire_upwind_alignment_score` | **NEW_REAL_DATA** | Table `fire_events` exists, zero rows | Fuse NASA FIRMS active fires with real wind vectors |
| 22 | `satellite_no2_trop` | **NEW_REAL_DATA** | Table `satellite_observations` exists, zero rows | Implement Sentinel-5P TROPOMI Level-2/3 ingestion |
| 23 | `satellite_cloud_fraction` | **NEW_REAL_DATA** | Table `satellite_observations` exists, zero rows | Ingest pixel-level cloud fraction with $\le 0.30$ filter |
| 24 | `dist_to_nearest_industrial_km` | **NEW_REAL_DATA** | Static GIS vector layer not in DB | Load MIDC industrial GeoJSON polygons into PostGIS |
| 25 | `dist_to_nearest_major_road_km` | **NEW_REAL_DATA** | Static GIS vector layer not in DB | Load OpenStreetMap primary road lines into PostGIS |
| 26 | `industrial_zone_within_2km_flag` | **NEW_REAL_DATA** | Computable once MIDC GIS layer loaded | PostGIS `ST_DWithin` query ($d \le 2.0\text{ km}$) |
| 27 | `sensitive_receptors_count_2km` | **NEW_REAL_DATA** | Static GIS vector layer not in DB | Load school/hospital points into PostGIS |
| 28 | `nearest_station_distance_km` | **DERIVED** | Computable from `monitoring_stations` & `grid_cells` | Compute Haversine distance between cell center and stations |
| 29 | `stations_within_5km_count` | **DERIVED** | Computable from `monitoring_stations` & `grid_cells` | PostGIS spatial count of stations within 5.0 km |
| 30 | `monitoring_coverage_gap_flag` | **DERIVED** | Computable from `nearest_station_distance_km` | Set flag = 1 if $d_{\text{station}} > 5.0\text{ km}$, else 0 |
| 31 | `hour_sin` | **DERIVED** | Computable from `observedAt` UTC timestamp | Mathematical sine transformation of UTC hour |
| 32 | `hour_cos` | **DERIVED** | Computable from `observedAt` UTC timestamp | Mathematical cosine transformation of UTC hour |
| 33 | `day_of_week_sin` | **DERIVED** | Computable from `observedAt` UTC timestamp | Mathematical sine transformation of day of week |
| 34 | `day_of_week_cos` | **DERIVED** | Computable from `observedAt` UTC timestamp | Mathematical cosine transformation of day of week |
| 35 | `is_weekend` | **DERIVED** | Computable from `observedAt` UTC timestamp | Binary flag based on UTC day of week |

### Classification Summary
- **DIRECT**: 4 features (11.4%)
- **DERIVED**: 11 features (31.4%)
- **NEW_REAL_DATA**: 20 features (57.1%)
- **NOT_AVAILABLE**: 0 features (0.0%)

---

## 6. Unit & Schema Mismatches

| Parameter | Production F2 Reality | Model Training Expectation | Hazard / Impact | Resolution |
|---|---|---|---|---|
| **Wind Speed Unit** | `km/h` (Open-Meteo default) | `m/s` | **Critical**: Factor of $3.6\times$ distortion causes severe model prediction error | Ingest Open-Meteo with `wind_speed_unit=ms` OR divide by $3.6$ |
| **H3 Identifier Key** | `h3_index` (PostgreSQL / Spring Boot) | `h3_cell_id` (AI Pydantic contract) | Key mismatch on API payload consumption | Unify or map keys in ingestion/response adapters |
| **Temperature Key** | `temperature` (PostgreSQL / DTO) | `temperature_2m` (Model feature vector) | Missing key error during vector assembly | Adapter maps `temperature` $\rightarrow$ `temperature_2m` |
| **Humidity Key** | `humidity` (PostgreSQL / DTO) | `relative_humidity_2m` (Model feature vector) | Missing key error during vector assembly | Adapter maps `humidity` $\rightarrow$ `relative_humidity_2m` |
| **Pressure Key** | `pressure` (PostgreSQL / DTO) | `surface_pressure` (Model feature vector) | Missing key error during vector assembly | Adapter maps `pressure` $\rightarrow$ `surface_pressure` |
| **Timestamp Key** | `observedAt` (Spring Boot API contract) | `timestamp` / `observed_at` (AI service) | Parsing failure | Canonical serializer accepts `observedAt` |
| **CO Unit** | `mg/m³` (CPCB NAAQS standard) | `mg/m³` | Consistent | Verify provider data is not mistakenly in ppm or µg/m³ |

---

## 7. Legitimate Missing-Value Rules

In strict accordance with Member 3's pipeline rules: **arbitrary zeros, random values, and fake numbers are strictly prohibited**. Missing values follow distinct physical behaviors:

1. **Active Fire Absence (Zero-Inflated Physical Reality)**:
   - On days without agricultural residue burning or forest fires within 25 km, active fire features legitimately evaluate to zero:
     - `fire_count_24h_25km = 0`
     - `fire_frp_sum_24h_25km = 0.0`
     - `fire_frp_mean_24h_25km = 0.0`
     - `fire_frp_distance_decay = 0.0`
     - `fire_upwind_alignment_score = 0.0`
   - `nearest_fire_distance_km`: Evaluates to the default physical truncation ceiling of **`50.0 km`**.
2. **Sentinel-5P Optical Cloud Gaps (Nocturnal / Cloud Cover Gaps)**:
   - Polar-orbiting satellites cross a region only once daily (~13:30 local solar time), resulting in zero optical retrievals at night or under heavy monsoon clouds ($f_{\text{cloud}} > 0.30$).
   - Rule: Query trailing 24-hour window $[T-24\text{h}, T]$ for the most recent valid cloud-free pass. If absent, features impute to the **historical regional median** (never 0.0, which would distort background column densities).
3. **Co-Pollutant Sensor Gaps**:
   - Single-hour gaps ($\le 1\text{ hour}$) use time-indexed forward fill (limit=1). Multi-hour gaps fall back to regional median baselines.
4. **Composite Confidence Degradation (Never Distorting Risk)**:
   - Missing input values lower the composite confidence score:
     $$C_{\text{completeness}} = \frac{N_{\text{valid}}}{35}$$
   - Confidence decreases gracefully without artificially inflating or deflating the predicted hotspot risk.

---

## 8. Model Geography Limitation

> [!WARNING]
> **Strict Cross-City Limitation**:  
> The existing `hotspot_classifier_v1.joblib` was trained, calibrated, and evaluated **exclusively on Pune Metropolitan Region (PMR) historical telemetry** (70,176 observations across 10 CAAQMS reference stations).

- **Pune (`550e8400-e29b-41d4-a716-446655440001`)**: **SUPPORTED TRAINING DOMAIN**. Validated with high statistical fidelity (PR-AUC: 0.742, Brier: 0.0482).
- **Mumbai (`550e8400-e29b-41d4-a716-446655440002`)**: **NOT VALIDATED BY THIS ARTIFACT**. Coastal sea-breeze circulation, elevated marine humidity, and distinct industrial clustering violate Pune inland atmospheric assumptions.
- **Delhi (`550e8400-e29b-41d4-a716-446655440003`)**: **NOT VALIDATED BY THIS ARTIFACT**. Severe continental winter inversions, transboundary agricultural stubble burning (Punjab/Haryana), and massive NCR traffic corridors require domain retraining.

**Rule**: The system must explicitly disclaim cross-city validity and must not present Pune model inference on Mumbai or Delhi cells without regional calibration.

---

## 9. Comprehensive Gap Inventory & Required Enhancements

### 1. External Data Ingestion Gaps
1. **Open-Meteo URL Query Expansion**:
   - Current URL: `hourly=temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m,precipitation`
   - Required URL: `hourly=temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m,precipitation,surface_pressure,boundary_layer_height&wind_speed_unit=ms`
2. **NASA FIRMS Active Fire Ingestion**:
   - Implement scheduled client for NASA FIRMS VIIRS 375m NRT thermal anomalies within Western Maharashtra bounding box.
3. **Sentinel-5P TROPOMI Ingestion**:
   - Ingest tropospheric $NO_2$ column density with cloud fraction QA filter $\le 0.30$.
4. **Co-Pollutants Continuous Ingestion**:
   - Ensure $PM_{10}, NO_2, SO_2, CO, O_3$ are continuously ingested from CPCB/OpenAQ.

### 2. Database Schema Enhancements
1. Add `boundary_layer_height` (`double precision`) to `weather_observations`.
2. Ensure `weather_observations.pressure` is populated from `surface_pressure`.
3. Load static GIS vector tables into PostgreSQL:
   - `gis_industrial_zones` (Polygon/MultiPolygon geometry, MIDC industrial estates)
   - `gis_major_roads` (LineString/MultiLineString geometry, national highways & arterials)
   - `gis_sensitive_receptors` (Point geometry, schools and hospitals)

### 3. AI Service Feature-Calculation Modules Required
1. `weather_features.py`: Orthogonal wind vector decomposition ($u, v$) and unit normalization.
2. `spatial_features.py`: Haversine geodesic distance, great-circle azimuth bearing, leave-one-out spatial lag calculation, and coverage gap flag.
3. `fire_features.py`: Inverse-distance decay and wind-aligned advection scoring.
4. `gis_features.py`: PostGIS spatial distance and containment queries.
5. `temporal_features.py`: Cyclical sine/cosine diurnal and weekly harmonics.

---

## 10. Exact Phase 2 Implementation Plan

Phase 2 will implement the data pipelines and feature calculation foundation required to assemble the real 35-feature vector:

```text
Phase 2 Implementation Roadmap:
├── Step 1: Open-Meteo Ingestion Enhancement (surface_pressure, boundary_layer_height, wind_speed_unit=ms)
├── Step 2: Static GIS Layer Ingestion into PostGIS (MIDC industrial, OSM roads, receptors)
├── Step 3: NASA FIRMS Active Fire Real Data Ingestion Pipeline
├── Step 4: Python Feature Engineering Services (weather, spatial lag, fire advection, GIS proximity)
├── Step 5: Serialized Model Artifact Relocation & Verification Script (hotspot_classifier_v1.joblib)
└── Step 6: 35-Feature Contract End-to-End Validation against Real Pune Data
```

---

F3_PHASE_1_STATUS = PASS
