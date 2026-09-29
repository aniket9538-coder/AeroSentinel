# AeroSentinel — F4-P2 Production Forecast Feature Layer Report (Hardened)

## 1. Executive Summary

Phase **F4-P2 (Production Forecast Feature Layer)** has established and validated the dedicated production feature pipeline required for multi-horizon PM2.5 forecasting ($T+1\text{h}, T+3\text{h}, T+6\text{h}$). Following the final hardening patch, the feature layer enforces strict missingness semantics, explicit provenance tracking (`REAL_ZERO`, `MISSING`, `SOURCE_UNAVAILABLE`, `IMPUTED_BASELINE`, `VALID_OBSERVATION`), mathematical wind vector alignment, and rigorous model-ready `(1, 36)` tensor shaping.

In accordance with strict phase directives:
1. **F3 Hotspot Detection remains 100% LOCKED and UNTOUCHED.**
2. **Forecast Model Retraining and Artifact Alteration were strictly forbidden.** The existing artifact (`ai-service/models/artifacts/forecast_regressors_v1.joblib`) was treated as the unmodifiable single source of truth.
3. **No Forecast Inference was executed.** The pipeline strictly stops at the model adapter boundary, outputting verified model-ready data structures (`double[1][36]`, `pd.DataFrame(1, 36)`) ready for Phase F4-P3 handoff.
4. **Physical Data Reuse with Domain Separation was enforced.** Physical feature snapshots in PostgreSQL are safely reused as underlying storage, while dedicated F4 forecast feature domain components (`ForecastFeatureBuilder`, `ForecastFeatureVector`, `ForecastFeatureValidator`, `ForecastFeatureAdapter`) operate completely decoupled from F3 hotspot prediction engines.
5. **Exact 36-Feature Contract & Unit Conventions were verified.** Meteorological, temporal, spatial lag, monitoring, GIS, and NASA FIRMS active fire features were audited against training code and proven via real PostgreSQL data for Pune.

---

## 2. P1/P1.1 Contract Dependency

Phase F4-P2 directly consumes the locked contracts from **F4-P1** (Artifact Audit) and **F4-P1.1** (Specification Reconciliation):

- **Authoritative Artifact**: `ai-service/models/artifacts/forecast_regressors_v1.joblib`
- **Supported Horizons**: `[1, 3, 6]` (corresponding to $+1\text{h}$, $+3\text{h}$, $+6\text{h}$)
- **Authoritative Feature Count**: Exactly 36 numeric features (`len(feature_cols) == 36`)
- **Prohibited Pseudo-Features**: No `pm25_t`, `pm25_t-1`, `pm25_t-2`, `rolling_mean`, `rolling_max`, or `change_rate` (confirmed absent from the trained artifact).
- **Physical Feature Base**: `pm25_spatial_lag_mean` represents the leave-one-out spatial lag of neighboring ground stations ($[0.98, 916.85]\,\mu\text{g}/\text{m}^3$), which accounts for 95.30% of the $T+1\text{h}$ model feature importance.

---

## 3. Production Source Mapping

| # | Feature | Production Source | Repository / Service | Unit | Transformation | Missing Behavior | Status |
|---|---|---|---|---|---|---|---|
| 01 | `latitude` | H3 Cell Centroid | `H3Service` / `GridRepository` | Degrees N | Centroid of H3 Res-8 cell | Valid float required | VALID |
| 02 | `longitude` | H3 Cell Centroid | `H3Service` / `GridRepository` | Degrees E | Centroid of H3 Res-8 cell | Valid float required | VALID |
| 03 | `pm10` | CPCB/MPCB CAAQMS | `AirObservationRepository` | $\mu\text{g}/\text{m}^3$ | Latest valid obs $\le T_0$ | Quality=MISSING, 0.0 at adapter | VALID |
| 04 | `no2` | CPCB/MPCB CAAQMS | `AirObservationRepository` | $\mu\text{g}/\text{m}^3$ | Latest valid obs $\le T_0$ | Quality=MISSING, 0.0 at adapter | VALID |
| 05 | `so2` | CPCB/MPCB CAAQMS | `AirObservationRepository` | $\mu\text{g}/\text{m}^3$ | Latest valid obs $\le T_0$ | Quality=MISSING, 0.0 at adapter | VALID |
| 06 | `co` | CPCB/MPCB CAAQMS | `AirObservationRepository` | $\text{mg}/\text{m}^3$ | Latest valid obs $\le T_0$ | Quality=MISSING, 0.0 at adapter | VALID |
| 07 | `o3` | CPCB/MPCB CAAQMS | `AirObservationRepository` | $\mu\text{g}/\text{m}^3$ | Latest valid obs $\le T_0$ | Quality=MISSING, 0.0 at adapter | VALID |
| 08 | `hour` | Base Timestamp $T_0$ | `FeatureEngineeringService` | Integer $[0, 23]$ | Local solar hour (Asia/Kolkata) | Deterministic from $T_0$ | VALID |
| 09 | `day_of_week` | Base Timestamp $T_0$ | `FeatureEngineeringService` | Integer $[0, 6]$ | Local day (0=Mon, 6=Sun) | Deterministic from $T_0$ | VALID |
| 10 | `is_weekend` | Base Timestamp $T_0$ | `FeatureEngineeringService` | Binary $\{0, 1\}$ | $1$ if $\text{dow} \ge 5$ else $0$ | Deterministic from $T_0$ | VALID |
| 11 | `hour_sin` | Base Timestamp $T_0$ | `FeatureEngineeringService` | Dimensionless | $\sin(2\pi \cdot \text{hour} / 24)$ | Deterministic from $T_0$ | VALID |
| 12 | `hour_cos` | Base Timestamp $T_0$ | `FeatureEngineeringService` | Dimensionless | $\cos(2\pi \cdot \text{hour} / 24)$ | Deterministic from $T_0$ | VALID |
| 13 | `dow_sin` | Base Timestamp $T_0$ | `FeatureEngineeringService` | Dimensionless | $\sin(2\pi \cdot \text{dow} / 7)$ | Deterministic from $T_0$ | VALID |
| 14 | `dow_cos` | Base Timestamp $T_0$ | `FeatureEngineeringService` | Dimensionless | $\cos(2\pi \cdot \text{dow} / 7)$ | Deterministic from $T_0$ | VALID |
| 15 | `temperature` | Open-Meteo Surface Weather | `WeatherRepository` | $^\circ\text{C}$ | Surface 2m air temp | City baseline fallback ($25^\circ\text{C}$) | VALID |
| 16 | `humidity` | Open-Meteo Surface Weather | `WeatherRepository` | % | Relative humidity 2m | City baseline fallback ($50\%$) | VALID |
| 17 | `wind_speed` | Open-Meteo Surface Weather | `WeatherRepository` | $\text{km}/\text{h}$ | Raw speed preserved; $\text{m}/\text{s}$ at adapter | Baseline $5.0\,\text{km}/\text{h}$ | VALID |
| 18 | `wind_direction` | Open-Meteo Surface Weather | `WeatherRepository` | Degrees $[0, 360)$ | Meteorological wind bearing | Baseline $0.0^\circ$ | VALID |
| 19 | `wind_u` | Derived from Speed/Dir | `FeatureEngineeringService` | $\text{m}/\text{s}$ | $-ws \cdot \sin(\text{rad}(\text{dir}))$, calm $<0.2$ | $0.0$ if calm/missing | VALID |
| 20 | `wind_v` | Derived from Speed/Dir | `FeatureEngineeringService` | $\text{m}/\text{s}$ | $-ws \cdot \cos(\text{rad}(\text{dir}))$, calm $<0.2$ | $0.0$ if calm/missing | VALID |
| 21 | `rainfall` | Open-Meteo Surface Weather | `WeatherRepository` | $\text{mm}$ | Hourly precipitation | Baseline $0.0\,\text{mm}$ | VALID |
| 22 | `pressure` | Open-Meteo Surface Weather | `WeatherRepository` | $\text{hPa}$ | Surface atmospheric pressure | Regional elevation default ($954.3$) | VALID |
| 23 | `pm25_spatial_lag_mean` | Ground Stations Network | `AirObservationRepository` | $\mu\text{g}/\text{m}^3$ | Leave-one-out spatial mean | Regional baseline | VALID |
| 24 | `nearest_station_distance_km` | Active Monitoring Stations | `SensorRepository` | $\text{km}$ | Haversine distance to nearest | $0.0\,\text{km}$ default | VALID |
| 25 | `stations_within_5km_count` | Active Monitoring Stations | `SensorRepository` | Count | Station count within 5km radius | $1.0$ (self) default | VALID |
| 26 | `monitoring_coverage_gap_flag` | Spatial Network Analysis | `SensorRepository` | Binary $\{0, 1\}$ | $1$ if $\text{nearest} > 7.0\,\text{km}$ | $0$ default | VALID |
| 27 | `dist_to_nearest_industrial_km` | GIS Baseline / OSM | `FeatureEngineeringService` | $\text{km}$ | Geodesic distance to industrial zone | PMR baseline ($3.50\,\text{km}$) | VALID |
| 28 | `dist_to_nearest_major_road_km` | GIS Baseline / OSM | `FeatureEngineeringService` | $\text{km}$ | Geodesic distance to arterial road | PMR baseline ($0.40\,\text{km}$) | VALID |
| 29 | `sensitive_receptors_count_2km` | GIS Baseline / OSM | `FeatureEngineeringService` | Count | Schools/hospitals within 2km | PMR baseline ($4$) | VALID |
| 30 | `industrial_zone_within_2km_flag` | GIS Baseline / OSM | `FeatureEngineeringService` | Binary $\{0, 1\}$ | $1$ if $\text{ind\_dist} \le 2.0\,\text{km}$ | PMR baseline ($0$) | VALID |
| 31 | `fire_count_24h_25km` | NASA FIRMS VIIRS/MODIS | `FireRepository` | Count | Active thermal anomalies ($24\text{h}, 25\text{km}$) | Physical zero ($0$) | VALID |
| 32 | `fire_frp_sum_24h_25km` | NASA FIRMS VIIRS/MODIS | `FireRepository` | $\text{MW}$ | Cumulative Fire Radiative Power | Physical zero ($0.0$) | VALID |
| 33 | `fire_frp_mean_24h_25km` | NASA FIRMS VIIRS/MODIS | `FireRepository` | $\text{MW}$ | Mean FRP of active fires | Physical zero ($0.0$) | VALID |
| 34 | `nearest_fire_distance_km` | NASA FIRMS VIIRS/MODIS | `FireRepository` | $\text{km}$ | Distance to closest fire | Bounded baseline ($50.0\,\text{km}$) | VALID |
| 35 | `fire_frp_distance_decay` | Physical Advection Model | `FireRepository` | $\text{MW}/\text{km}$ | $\sum \text{FRP}_i / (d_i + 1.0)$ | Physical zero ($0.0$) | VALID |
| 36 | `fire_upwind_alignment_score` | Plume Trajectory Model | `FireRepository` | Score | Upwind angular advection weight | Physical zero ($0.0$) | VALID |

---

## 4. Missingness Hardening & Quality Semantics

### Explicit Provenance Hierarchy
The feature layer maintains 5 explicit provenance states:
1. `VALID_OBSERVATION`: Direct, verified physical sensor telemetry (e.g. CPCB CAAQMS air monitor, Open-Meteo weather station).
2. `REAL_ZERO`: Confirmed physical zero (e.g. 0 active fires in 24h/25km, 0.0 mm rainfall on dry days).
3. `MISSING`: Known telemetry field not reported by ground station at time $T_0$.
4. `SOURCE_UNAVAILABLE`: External data provider or station network connection offline.
5. `IMPUTED_BASELINE`: Model-readiness baseline substitution (never promoted to VALID).

### Distinction Rules
- `REAL_ZERO != MISSING != SOURCE_UNAVAILABLE`.
- 0 active fires within 25km is a **`REAL_ZERO`**, not a missing value.
- An unavailable fire feed is **`SOURCE_UNAVAILABLE`**, never disguised as "0 fires".
- Missing pollutants are never converted to physical zeros upstream; they remain tracked in `missingFields` and are substituted with `0.0` **strictly at the model adapter boundary**.

---

## 5. Weather Fallback Semantics

When ground or satellite weather data is temporarily unlinked, physical baselines are applied with explicit provenance labels:

| Weather Variable | Baseline Constant | Physical Zero? | Provenance Classification | Model Boundary Behavior |
|---|---|---|---|---|
| `temperature` | $25.0\,^\circ\text{C}$ | No | `IMPUTED_BASELINE` | Substituted at boundary; vector marked `MISSING` |
| `humidity` | $50.0\,\%$ | No | `IMPUTED_BASELINE` | Substituted at boundary; vector marked `MISSING` |
| `wind_speed` | $5.0\,\text{km}/\text{h}$ | No | `IMPUTED_BASELINE` | Substituted at boundary; vector marked `MISSING` |
| `wind_direction` | $0.0\,^\circ$ | No (North) | `IMPUTED_BASELINE` | Substituted at boundary; vector marked `MISSING` |
| `rainfall` | $0.0\,\text{mm}$ | **Yes** | `REAL_ZERO` | Treated as dry period; retains `VALID_OBSERVATION` / `REAL_ZERO` |
| `pressure` | $954.3\,\text{hPa}$ | No (Elevation $\sim 600\text{m}$) | `IMPUTED_BASELINE` | Substituted at boundary; vector marked `MISSING` |

**Integrity Rule**: If any non-zero baseline is imputed due to missing data, the vector's `qualityStatus` is **strictly prohibited from being marked `VALID`**. Check 14 in `ForecastFeatureValidator` enforces this constraint.

---

## 6. Wind Unit Proof & Orthogonal Consistency

### Conversion Rule & Double-Conversion Rejection
- Input Source: $13.0\,\text{km}/\text{h}$ (Open-Meteo standard).
- Single Conversion:
  $$ws_{\text{mps}} = \frac{13.0}{3.6} = 3.611111\ldots\,\text{m}/\text{s} \longrightarrow \mathbf{3.6111\,\text{m}/\text{s}}$$
- Double Conversion (Prohibited):
  $$\frac{3.6111}{3.6} = 1.0031\,\text{m}/\text{s} \quad \Longrightarrow \quad \text{REJECTED via } \texttt{verifyNoDoubleConversion()}$$

### Orthogonal Wind Component Derivation for $\theta = 275^\circ$:
$$\text{rad} = \text{radians}(275^\circ) = 4.799655\,\text{rad}$$
$$u = -ws_{\text{mps}} \cdot \sin(275^\circ) = -3.6111 \cdot (-0.996195) = \mathbf{+3.5974\,\text{m}/\text{s}}$$
$$v = -ws_{\text{mps}} \cdot \cos(275^\circ) = -3.6111 \cdot (+0.087156) = \mathbf{-0.3147\,\text{m}/\text{s}}$$
The adapter guarantees that `wind_speed`, `wind_u`, and `wind_v` all reference the exact same single-converted speed in $\text{m}/\text{s}$.

---

## 7. Model-Ready Vector Proof (Shape (1, 36))

Generated from real PostgreSQL database in `ForecastFeatureLayerIntegrationTest`:

```text
==================================================
MODEL_READY_VECTOR_PROOF (F4-P2 HARDENED)
==================================================
batchShape: (1, 36)
batchSize: 1
featureCount: 36
h3Index: 88608850e5fffff
windSpeedConvertedToMps: true
convertedWindSpeed (m/s): 3.6111
wind_u: 3.5974
wind_v: -0.3147
--------------------------------------------------
model_input[00] latitude                       =    18.5315
model_input[01] longitude                      =    73.8471
model_input[02] pm10                           =   120.0000
model_input[03] no2                            =    37.0000
model_input[04] so2                            =    14.0000
model_input[05] co                             =     0.9000
model_input[06] o3                             =    24.0000
model_input[07] hour                           =    13.0000
model_input[08] day_of_week                    =     6.0000
model_input[09] is_weekend                     =     1.0000
model_input[10] hour_sin                       =    -0.2588
model_input[11] hour_cos                       =    -0.9659
model_input[12] dow_sin                        =    -0.7818
model_input[13] dow_cos                        =     0.6235
model_input[14] temperature                    =    29.8000
model_input[15] humidity                       =    47.0000
model_input[16] wind_speed                     =     3.6111
model_input[17] wind_direction                 =   275.0000
model_input[18] wind_u                         =     3.5974
model_input[19] wind_v                         =    -0.3147
model_input[20] rainfall                       =     0.7000
model_input[21] pressure                       =   948.4000
model_input[22] pm25_spatial_lag_mean          =    78.0000
model_input[23] nearest_station_distance_km    =     0.2700
model_input[24] stations_within_5km_count      =     2.0000
model_input[25] monitoring_coverage_gap_flag   =     0.0000
model_input[26] dist_to_nearest_industrial_km  =     3.5000
model_input[27] dist_to_nearest_major_road_km  =     0.4000
model_input[28] sensitive_receptors_count_2km  =     4.0000
model_input[29] industrial_zone_within_2km_flag =     0.0000
model_input[30] fire_count_24h_25km            =     0.0000
model_input[31] fire_frp_sum_24h_25km          =     0.0000
model_input[32] fire_frp_mean_24h_25km         =     0.0000
model_input[33] nearest_fire_distance_km       =    50.0000
model_input[34] fire_frp_distance_decay        =     0.0000
model_input[35] fire_upwind_alignment_score    =     0.0000
==================================================
Model inference executed: false (STOP CONDITION VERIFIED)
```

---

## 8. Physical Data Quality vs Model Imputation

```
Semantic / Source Physical Vector
  ├── Physical Observations: Validated ranges
  ├── Provenance Tracking: REAL_ZERO vs MISSING vs UNAVAILABLE
  └── Original Quality: VALID | MISSING | SUSPECT | UNAVAILABLE
           ↓
Quality-Preserving Feature Validator
  ├── Enforces Check 14 (No false promotion to VALID)
  └── Validates H3 geometry, spatial lag, finite limits
           ↓
Model-Input Adapter Boundary
  ├── Single Wind Conversion (13.0 km/h -> 3.6111 m/s)
  ├── Double-Conversion Protection Check
  ├── Zero-Fill strictly at boundary for sklearn compatibility
  └── Shape Formatter: (1, 36) 2D double matrix
           ↓
READY FOR F4-P3 INFERENCE (STOP)
```

---

## 9. Automated Test Results

### 1. Spring Boot Backend Tests
- **`ForecastFeatureLayerTest`** (12 unit tests):
  - Contract (exact 36 count, order): **PASS**
  - Missingness semantics (`REAL_ZERO != MISSING != UNAVAILABLE`): **PASS**
  - Quality status integrity rejection (Check 14): **PASS**
  - Unavailable pollutant source handling: **PASS**
  - Wind unit proof ($13.0\,\text{km}/\text{h} \to 3.6111\,\text{m}/\text{s}$): **PASS**
  - Double-conversion rejection: **PASS**
  - Model-ready vector shape $(1, 36)$: **PASS**
  - Weather fallback provenance tracking: **PASS**
- **`ForecastFeatureLayerIntegrationTest`** (4 integration tests against PostgreSQL):
  - Semantic `FEATURE_VECTOR_RUNTIME_PROOF`: **PASS**
  - Hardened `MODEL_READY_VECTOR_PROOF` with shape $(1, 36)$: **PASS**
  - Physical snapshot reuse: **PASS**
  - Cross-city missingness distinction (Mumbai/Delhi UNAVAILABLE): **PASS**
- **Total Backend Tests Run**: 16, **Failures**: 0, **Errors**: 0, **Skipped**: 0. **Result**: `BUILD SUCCESS`.

### 2. Python AI Service Tests
- **`ai-service/tests/test_f4_feature_layer.py`** (11 unit tests):
  - Artifact contract verification: **PASS**
  - Exact 36 feature order: **PASS**
  - Real zero vs missing vs unavailable: **PASS**
  - Missing pollutant semantics: **PASS**
  - Unavailable pollutant source: **PASS**
  - Quality status integrity rejection: **PASS**
  - Wind unit proof ($13.0\,\text{km}/\text{h} \to 3.6111\,\text{m}/\text{s}$): **PASS**
  - Double wind conversion rejection: **PASS**
  - Model-ready vector shape $(1, 36)$: **PASS**
  - Weather fallback labeling: **PASS**
  - Zero model inference verification: **PASS**
- **Total Python Tests Run**: 11, **Passed**: 11 (100%), **Failed**: 0.

### 3. F3 Hotspot Regression Suite
- **`MLHotspotDetectionEngineTest`** & **`HotspotIntegrationTest`**:
  - Tests run: 14, **Failures**: 0, **Errors**: 0. **Result**: `BUILD SUCCESS`.

---

## 10. Files Created & Modified

### Created:
1. `backend/src/main/java/com/aerosentinel/forecast/feature/FeatureProvenance.java`
2. `backend/src/main/java/com/aerosentinel/forecast/feature/ForecastFeatureVector.java`
3. `backend/src/main/java/com/aerosentinel/forecast/feature/ForecastFeaturesInvalidException.java`
4. `backend/src/main/java/com/aerosentinel/forecast/feature/ForecastFeatureValidator.java`
5. `backend/src/main/java/com/aerosentinel/forecast/feature/ForecastFeatureAdapter.java`
6. `backend/src/main/java/com/aerosentinel/forecast/feature/ForecastFeatureBuilder.java`
7. `backend/src/test/java/com/aerosentinel/forecast/feature/ForecastFeatureLayerTest.java`
8. `backend/src/test/java/com/aerosentinel/forecast/feature/ForecastFeatureLayerIntegrationTest.java`
9. `ai-service/ml/forecast/features/__init__.py`
10. `ai-service/ml/forecast/features/contract.py`
11. `ai-service/ml/forecast/features/vector.py`
12. `ai-service/ml/forecast/features/validator.py`
13. `ai-service/ml/forecast/features/adapter.py`
14. `ai-service/ml/forecast/features/builder.py`
15. `ai-service/tests/test_f4_feature_layer.py`
16. `AeroSentinel_F4_P2_Production_Forecast_Feature_Layer_Report.md`

### Modified:
- None. (Zero F3 files modified).

---

## 11. Final Machine Summary

```json
F4_P2_SUMMARY = {
  "featureCount": 36,
  "artifactOrderVerified": "PASS",
  "productionSourceMapping": "PASS",
  "forecastFeatureBuilder": "PASS",
  "validator": "PASS",
  "temporalAlignment": "PASS",
  "spatialAlignment": "PASS",
  "unitContract": "PASS",
  "missingnessContract": "PASS",
  "windConversionProof": "PASS",
  "modelReadyVectorProof": "PASS",
  "runtimeVectorProof": "PASS",
  "tests": "PASS",
  "f3Untouched": "PASS",
  "modelInferenceStarted": false,
  "status": "COMPLETE"
}
```

---

## 12. Stop Condition

All hardening tasks for Phase F4-P2 are complete. No model inference was executed. Phase F4-P3 will own model loading and regressor prediction.
