# AeroSentinel — F4-P1 Forecast Artifact & Contract Audit Report

**Document ID**: `AEROSENTINEL-F4-P1-AUDIT`  
**Phase**: F4 Phase 1 — Actual Forecast Artifact + Contract Audit  
**Author**: Antigravity AI Assistant & Engineering Team  
**Date**: September 27, 2026  
**Status**: COMPLETE / CONTRACT LOCKED FOR F4 IMPLEMENTATION  

---

## 1. Executive Summary

Phase F4-P1 was executed as a strict, read-only audit and contract-lock phase for the future multi-horizon PM2.5 forecasting subsystem of AeroSentinel. In accordance with project governance rules, **no implementation code, database migrations, Spring Boot services, React UI components, or model retraining were performed during this phase**.

The primary objective was to inspect the physical machine learning artifact [`forecast_regressors_v1.joblib`](file:///c:/Users/lenovo/AeroSential/ai-service/models/artifacts/forecast_regressors_v1.joblib), evaluate its internal structure, extract its exact feature and horizon contracts, evaluate interval/uncertainty mechanics, and compare them against historical engineering documentation and the existing locked F3 hotspot architecture.

### Key Discoveries & Authority Lock:
1. **Physical Artifact Verified**: The model binary [`forecast_regressors_v1.joblib`](file:///c:/Users/lenovo/AeroSential/ai-service/models/artifacts/forecast_regressors_v1.joblib) exists, is 131,306,591 bytes (~125.2 MB), and successfully loads as a Python dictionary.
2. **Authoritative Supported Horizons**: The artifact contains regressors strictly for **1 hour, 3 hours, and 6 hours** ($T+1\text{h}$, $T+3\text{h}$, $T+6\text{h}$). Documentation, UI mockups, and early specs describing $1\text{h}$–$6\text{h}$ continuous or $T+12\text{h}/T+24\text{h}$ are **mismatches against physical reality**. The artifact-supported horizons $\{1, 3, 6\}$ are declared **authoritative for F4**.
3. **Exact Feature Vector (36 Features)**: The forecaster expects an ordered vector of exactly 36 float features (`f3-features-v1`). Noticeably, raw `pm25` is excluded to eliminate target leakage from the shared feature generation pipeline, while spatial lag (`pm25_spatial_lag_mean`) and co-pollutants (`pm10`, `no2`, `so2`, `co`, `o3`) provide primary air quality state.
4. **Empirical Prediction Intervals vs. Synthetic Confidence**: The model provides asymmetric empirical residual intervals ($P10$ to $P90$) with physical clamping ($\text{lowerBound} \ge 0.0\,\mu\text{g/m}^3$). It does **not** output a true probabilistic confidence score. F4 must **not** invent synthetic confidence nor inherit F3 hotspot classification confidence.
5. **Contract Status**: **READY_FOR_F4_IMPLEMENTATION**. All physical properties, feature mappings, temporal dependencies, and lineage attachment anchors are verified and locked.

---

## 2. F3 Lock Dependency

Feature F3 (Hyperlocal Hotspot Detection & Decision Intelligence) was audited, corrected, and officially locked during the preceding milestone:
- **Lock Baseline**: 188 backend tests passing, 25 Python ML tests passing, 23 frontend contract tests passing.
- **F3 Immutability**: No F3 models, operational thresholds ($0.20$), decision boundaries, database schemas (`hotspot_predictions`, `feature_snapshots`), or REST controllers were modified during this audit.
- **F4 Attachment Anchors**: F4 is purely downstream of F3 and attaches via the locked contextual lineage keys:
  - `parentPredictionId` (`hotspot_predictions.id` UUID)
  - `cityId` (`cities.id` UUID)
  - `h3Index` (Resolution-8 H3 string)
  - `featureSnapshotId` (`feature_snapshots.id` UUID)
  - `predictedAt` / `capturedAt` (UTC Instant)

---

## 3. Artifact Location & Load Verification

The forecaster binary was located and inspected directly via Python runtime:

```
FORECAST_ARTIFACT_AUDIT
- path             = ai-service/models/artifacts/forecast_regressors_v1.joblib
- exists           = PASS (True)
- file_size        = 131,306,591 bytes (125.22 MB)
- sha256_hash      = 55dcee656aed9bd968053d7bf57b27e1d52e8f47585b99a4e5f9368cd7161c84
- loadable         = PASS (Loaded via joblib.load())
- serialization    = Joblib binary pickle (scikit-learn 1.8.0 / 1.9.1 compatible)
- top_level_type   = <class 'dict'>
- top_level_keys   = ['models', 'feature_cols', 'residuals', 'benchmark_metrics', 'algorithm']
- horizons         = [1, 3, 6] (with string aliases 't_plus_1', 't_plus_3', 't_plus_6')
- feature_count    = 36
- residual_bounds  = {1: [-1.84, +1.86], 3: [-3.90, +3.05], 6: [-5.52, +5.42]}
- embedded_scalers = NONE (Raw features passed with fillna(0.0))
```

---

## 4. Actual Artifact Structure

The serialized dictionary contains five top-level keys:

```
forecast_regressors_v1.joblib
 ├── "algorithm": "RandomForestRegressor(n_estimators=100, max_depth=14)"
 ├── "feature_cols": list[str] (36 ordered strings)
 ├── "models": dict[int | str, RandomForestRegressor]
 │     ├── 1 & "t_plus_1" -> RandomForestRegressor (Horizon 1h, max_depth=14, n_estimators=100)
 │     ├── 3 & "t_plus_3" -> RandomForestRegressor (Horizon 3h, max_depth=14, n_estimators=100)
 │     └── 6 & "t_plus_6" -> RandomForestRegressor (Horizon 6h, max_depth=14, n_estimators=100)
 ├── "residuals": dict[int, dict[str, float]]
 │     ├── 1: {"p10": -1.8424755062920515, "p90": 1.862868000436551}
 │     ├── 3: {"p10": -3.9013644803844243, "p90": 3.0473581785435204}
 │     └── 6: {"p10": -5.520762710872448,  "p90": 5.424867487241023}
 └── "benchmark_metrics": dict[str, dict]
       ├── "T+1h": {"val": {mae: 1.64, rmse: 6.56, r2: 0.7275}, "test": {mae: 2.97, rmse: 5.84, r2: 0.9240}}
       ├── "T+3h": {"val": {mae: 2.89, rmse: 8.21, r2: 0.5323}, "test": {mae: 5.17, rmse: 11.03, r2: 0.7294}}
       └── "T+6h": {"val": {mae: 4.54, rmse: 11.18, r2: 0.0619}, "test": {mae: 6.71, rmse: 9.84, r2: 0.7854}}
```

*Note on Model Identity*: In the `models` dictionary, integer keys and string aliases reference the exact same memory instance (`models[1] is models['t_plus_1'] == True`, `models[3] is models['t_plus_3'] == True`, `models[6] is models['t_plus_6'] == True`).

---

## 5. Exact Supported Forecast Horizons

A critical finding of this audit is the discrepancy between documentation/UI and physical artifact reality:

```
ACTUAL_ARTIFACT_HORIZONS = [1, 3, 6] (T+1h, T+3h, T+6h)
DOCUMENTED_SPEC_HORIZONS = [1, 2, 3, 4, 5, 6] (UI mock: 1h, 2h, 3h, 4h, 5h, 6h; early docs: T+12h, T+24h)
MATCH                    = NO
```

### Analysis & Authoritative Decision:
- The trained regressors exist **only** for horizons $T+1\text{h}$, $T+3\text{h}$, and $T+6\text{h}$.
- The model was trained using direct multi-horizon modeling where separate estimators fit targets generated by forward-shifting `pm25_clean` by 1, 3, and 6 hours.
- There are **no** trained estimators for $T+2\text{h}$, $T+4\text{h}$, $T+5\text{h}$, $T+12\text{h}$, or $T+24\text{h}$.
- **Decision**: The artifact-supported horizons $\{1, 3, 6\}$ are **authoritative** for F4. We will **not** retrain or expand the model during F4, nor will we use linear interpolation to fabricate predictions for missing hours. The frontend and backend contracts will be updated to display the true discrete horizons ($+1\text{h}, +3\text{h}, +6\text{h}$).

---

## 6. Exact Ordered Forecast Feature List

The forecaster expects an exact ordered vector of 36 numeric features (`feature_cols`):

| Index | Feature Name | Dtype | Range / Expected Units |
|:---:|:---|:---:|:---|
| 0 | `latitude` | `float64` | Decimal degrees ($17.5$ to $19.5$) |
| 1 | `longitude` | `float64` | Decimal degrees ($72.8$ to $75.0$) |
| 2 | `pm10` | `float64` | Concentration in $\mu\text{g/m}^3$ |
| 3 | `no2` | `float64` | Concentration in $\mu\text{g/m}^3$ |
| 4 | `so2` | `float64` | Concentration in $\mu\text{g/m}^3$ |
| 5 | `co` | `float64` | Concentration in $\text{mg/m}^3$ |
| 6 | `o3` | `float64` | Concentration in $\mu\text{g/m}^3$ |
| 7 | `hour` | `float64` | Integer hour of day ($0$ to $23$) |
| 8 | `day_of_week` | `float64` | Integer day of week ($0 = \text{Monday}$ to $6 = \text{Sunday}$) |
| 9 | `is_weekend` | `float64` | Binary flag ($1$ if Sat/Sun, else $0$) |
| 10 | `hour_sin` | `float64` | $\sin(2\pi \cdot \text{hour} / 24) \in [-1.0, 1.0]$ |
| 11 | `hour_cos` | `float64` | $\cos(2\pi \cdot \text{hour} / 24) \in [-1.0, 1.0]$ |
| 12 | `dow_sin` | `float64` | $\sin(2\pi \cdot \text{dow} / 7) \in [-1.0, 1.0]$ |
| 13 | `dow_cos` | `float64` | $\cos(2\pi \cdot \text{dow} / 7) \in [-1.0, 1.0]$ |
| 14 | `temperature` | `float64` | Celsius ($^{\circ}\text{C}$) |
| 15 | `humidity` | `float64` | Relative humidity percentage ($0.0$ to $100.0$) |
| 16 | `wind_speed` | `float64` | Converted wind speed in $\text{m/s}$ |
| 17 | `wind_direction` | `float64` | Meteorological direction in degrees ($0.0^{\circ}$ to $360.0^{\circ}$) |
| 18 | `wind_u` | `float64` | Zonal velocity: $-w \cdot \sin(\theta \cdot \pi / 180)$ |
| 19 | `wind_v` | `float64` | Meridional velocity: $-w \cdot \cos(\theta \cdot \pi / 180)$ |
| 20 | `rainfall` | `float64` | Hourly precipitation in $\text{mm}$ |
| 21 | `pressure` | `float64` | Surface atmospheric pressure in $\text{hPa}$ |
| 22 | `pm25_spatial_lag_mean` | `float64` | Leave-one-out spatial mean of neighboring H3 cells in $\mu\text{g/m}^3$ |
| 23 | `nearest_station_distance_km`| `float64` | Distance from cell centroid to nearest station in $\text{km}$ |
| 24 | `stations_within_5km_count` | `float64` | Count of active monitoring stations within $5\text{km}$ radius |
| 25 | `monitoring_coverage_gap_flag`| `float64`| Binary flag ($1$ if distance $> 7.0\text{km}$ or count $= 0$, else $0$) |
| 26 | `dist_to_nearest_industrial_km`|`float64` | Euclidean distance to closest industrial emission zone in $\text{km}$ |
| 27 | `dist_to_nearest_major_road_km`|`float64` | Distance to primary highway/arterial corridor in $\text{km}$ |
| 28 | `sensitive_receptors_count_2km`|`float64` | Schools, hospitals, elderly homes within $2\text{km}$ buffer |
| 29 | `industrial_zone_within_2km_flag`|`float64`| Binary flag ($1$ if industrial zone present $\le 2\text{km}$, else $0$) |
| 30 | `fire_count_24h_25km` | `float64` | Cumulative thermal hotspots detected within $25\text{km}$ in past 24h |
| 31 | `fire_frp_sum_24h_25km` | `float64` | Cumulative Fire Radiative Power in $\text{MW}$ |
| 32 | `fire_frp_mean_24h_25km` | `float64` | Mean Fire Radiative Power per thermal anomaly in $\text{MW}$ |
| 33 | `nearest_fire_distance_km` | `float64` | Distance to nearest active thermal detection in $\text{km}$ |
| 34 | `fire_frp_distance_decay` | `float64` | Gaussian/exponential distance-weighted FRP impact score |
| 35 | `fire_upwind_alignment_score`| `float64` | Scalar product of fire-to-cell vector and wind vector ($[0.0, 10.0]$) |

---

## 7. Forecast Feature Provenance

```
FORECAST_FEATURE_CONTRACT_V1
========================================================================================
Source Subsystem         Features Included                                        Count
----------------------------------------------------------------------------------------
F1 Station / Context     latitude, longitude, nearest_station_distance_km,          5
                         stations_within_5km_count, monitoring_coverage_gap_flag
F1 Air Quality           pm10, no2, so2, co, o3                                     5
                         (Note: pm25 excluded to prevent joint training leakage)
F2 Weather / Open-Meteo  temperature, humidity, wind_speed, wind_direction,         8
                         wind_u, wind_v, rainfall, pressure
F2 Spatial Lag           pm25_spatial_lag_mean                                      1
F2 Temporal Derivations  hour, day_of_week, is_weekend,                             7
                         hour_sin, hour_cos, dow_sin, dow_cos
F2 GIS / Infrastructure  dist_to_nearest_industrial_km,                             4
                         dist_to_nearest_major_road_km,
                         sensitive_receptors_count_2km,
                         industrial_zone_within_2km_flag
F2 Thermal / VIIRS Fire  fire_count_24h_25km, fire_frp_sum_24h_25km,               6
                         fire_frp_mean_24h_25km, nearest_fire_distance_km,
                         fire_frp_distance_decay, fire_upwind_alignment_score
----------------------------------------------------------------------------------------
Total Required Features                                                            36
========================================================================================
```

- **Missing Value Handling**: In training, missing fields were imputed with `fillna(0.0)`. In inference, payloads must supply all 36 values; any missing numerical feature must be imputed with default regional baselines prior to evaluation.
- **Physical Bounds**: All inputs must be finite numbers (`-inf < x < +inf`, `np.isnan(x) == False`).

---

## 8. F3 vs F4 Feature Comparison

```
F3_FEATURE_COUNT = 36
F4_FEATURE_COUNT = 36
FEATURE_LIST_EQUIVALENCE = 100% (Identical names, order, and count)
```

### Comparative Analysis:

| Dimension | F3 Hotspot Classifier | F4 Multi-Horizon Forecaster | Difference / Rationale |
|:---|:---|:---|:---|
| **Artifact** | `hotspot_classifier_v1.joblib` | `forecast_regressors_v1.joblib` | Distinct physical files. |
| **Model Type** | `CalibratedClassifierCV` (Platt Sigmoid over Balanced RF) | Independent `RandomForestRegressor` per horizon ($T+1, T+3, T+6$) | Classification vs. multi-horizon continuous regression. |
| **Target Variable** | Binary: `pm25 >= 60.0 & spatial_lag >= 1.15` | Continuous: `target_pm25_t_plus_{h}` ($\mu\text{g/m}^3$) | Hotspot probability vs. expected concentration. |
| **Feature Vector** | Exactly 36 features (`f3-features-v1`) | Exactly 36 features (`f3-features-v1`) | Shared feature pipeline generated in `train_all_models.py`. |
| **Current PM2.5 in Vector** | Excluded (Target leakage protection) | Excluded (Inherited from shared selection) | Both models rely on `pm25_spatial_lag_mean` and co-pollutants. |
| **Decision Threshold** | Operational threshold: $0.20$ | Not Applicable | Forecaster outputs continuous $\mu\text{g/m}^3$, not probability. |
| **Uncertainty Output** | Epistemic flag ($0/1$) & calibration certainty | Empirical residual intervals ($P10$ to $P90$) | Hotspot boundary uncertainty vs. physical error bounds. |
| **Confidence Semantics** | Composite score based on sensor distance & calibration | **NOT SUPPORTED** by model artifact | Must not inherit F3 confidence blindly. |

**Crucial Decoupling Rule**: Although F3 and F4 share the exact 36-feature snapshot, their inference adapters, response DTOs, and storage models must be completely separate. F4 must **not** import or invoke F3 classifier logic.

---

## 9. Forecast Model Architecture

```
                      +---------------------------------------+
                      |       36-Feature Snapshot Vector       |
                      |   (f3-features-v1 / fillna(0.0))      |
                      +-------------------+-------------------+
                                          |
        +---------------------------------+---------------------------------+
        |                                 |                                 |
        v                                 v                                 v
+------------------+             +------------------+             +------------------+
| Horizon T+1h     |             | Horizon T+3h     |             | Horizon T+6h     |
| RandomForest     |             | RandomForest     |             | RandomForest     |
| Regressor        |             | Regressor        |             | Regressor        |
| (depth=14, n=100)|             | (depth=14, n=100)|             | (depth=14, n=100)|
+--------+---------+             +--------+---------+             +--------+---------+
         |                                |                                |
         v                                v                                v
  Predicted PM2.5                  Predicted PM2.5                  Predicted PM2.5
   point estimate                   point estimate                   point estimate
         |                                |                                |
         v                                v                                v
+------------------+             +------------------+             +------------------+
| Validation       |             | Validation       |             | Validation       |
| Residuals        |             | Residuals        |             | Residuals        |
| [P10: -1.84,     |             | [P10: -3.90,     |             | [P10: -5.52,     |
|  P90: +1.86]     |             |  P90: +3.05]     |             |  P90: +5.42]     |
+--------+---------+             +--------+---------+             +--------+---------+
         |                                |                                |
         v                                v                                v
+------------------+             +------------------+             +------------------+
| Physical Clamp   |             | Physical Clamp   |             | Physical Clamp   |
| max(0.0, y + P10)|             | max(0.0, y + P10)|             | max(0.0, y + P10)|
+------------------+             +------------------+             +------------------+
```

### Regressor Specifications:
- **Estimator Class**: `sklearn.ensemble.RandomForestRegressor`
- **Number of Estimators**: 100 trees
- **Max Depth**: 14 levels
- **Random State**: 42 (fully deterministic)
- **Criterion**: Squared Error (`squared_error`)
- **Independence**: Fully independent estimators fitted on target columns `target_pm25_t_plus_1`, `target_pm25_t_plus_3`, `target_pm25_t_plus_6`.
- **Output Units**: Micrograms per cubic meter ($\mu\text{g/m}^3$).

---

## 10. Forecast Interval / Uncertainty Contract

The 8 mandatory interval audit questions are answered based on the physical artifact and active code:

1. **Are lower/upper bounds actually supported?**  
   **YES**. The system supports lower and upper prediction bounds for each horizon.
2. **How are they calculated?**  
   - $\text{lowerBound} = \max(0.0, \hat{y} + P10)$
   - $\text{upperBound} = \hat{y} + P90$
3. **Are they per-horizon?**  
   **YES**. Each horizon has its own distinct empirical residual spread.
4. **Are they empirical residual intervals?**  
   **YES**. Derived from quantiles of residuals ($r = y_{\text{true}} - \hat{y}$) during model evaluation.
   - *Artifact Validation Residuals* (embedded in `.joblib`):
     - $T+1\text{h}$: $P10 = -1.84$, $P90 = +1.86$
     - $T+3\text{h}$: $P10 = -3.90$, $P90 = +3.05$
     - $T+6\text{h}$: $P10 = -5.52$, $P90 = +5.42$
   - *Test Set Residuals* (recorded in `metadata_v1.json` & `confidence.py`):
     - $T+1\text{h}$: $q10 = -2.41$, $q90 = +2.15$
     - $T+3\text{h}$: $q10 = -5.12$, $q90 = +4.88$
     - $T+6\text{h}$: $q10 = -9.34$, $q90 = +8.76$
5. **Are they symmetric or asymmetric?**  
   **ASYMMETRIC**. The empirical distribution reflects natural physical skew in pollutant dispersion.
6. **Are they guaranteed to be available for every inference?**  
   **YES**. Quantiles are precomputed and deterministic.
7. **Can interval bounds become negative?**  
   Mathematical addition of negative residuals could yield values $< 0.0$ at very low point estimates.
8. **Is any clipping applied?**  
   **YES**. Non-negative physical clamping is mandatory: $\text{lowerBound} = \max(0.0, \hat{y} + P10)$.

### Distinction Regarding Forecast Confidence:
- **No Model Confidence Score**: The Random Forest regressors do not calculate probabilistic confidence.
- **Strict Prohibition**: F4 must **not** invent arbitrary confidence scores (e.g., $0.88$ or $0.92$) nor copy the F3 hotspot confidence score.
- **Contract Formulation**: In the AI service contract, `forecastConfidence` is designated as `NOT YET AVAILABLE / TO BE DEFINED IN F4 ENGINEERING`.

---

## 11. Temporal Alignment Contract

Temporal alignment follows a strict forward-looking, non-leaking pipeline:

```
[ Base Timestamp T0 ] (e.g., 2026-09-27T12:00:00Z)
        │
        ├─► Feature Snapshot at T0 (Telemetry aggregated in 1-hour UTC bin)
        │
        ├─► Model Inference (Input: 36 features at T0)
        │
        ├─► Horizon T+1h ──► Forecast for: T0 + 1 hour (2026-09-27T13:00:00Z)
        ├─► Horizon T+3h ──► Forecast for: T0 + 3 hours (2026-09-27T15:00:00Z)
        └─► Horizon T+6h ──► Forecast for: T0 + 6 hours (2026-09-27T18:00:00Z)
```

- **Timezone Standard**: Strict UTC (`Instant` in Java, ISO-8601 with `Z` in Python/JSON).
- **Hourly Binning**: Hourly telemetry is truncated to `floor("1h")`.
- **Target Leakage Prohibition**: Feature extraction explicitly drops forward-shifted columns (`target_pm25_t_plus_1`, `target_pm25_t_plus_3`, `target_pm25_t_plus_6`).

---

## 12. F3 → F4 Attachment Audit

F4 attaches downstream of F3 predictions without modifying F3:

```
HotspotPrediction (F3 Locked)
 ├── id : UUID (authoritative parentPredictionId)
 ├── cityId : UUID
 ├── h3Index : String (Resolution 8)
 ├── featureSnapshotId : UUID (links to 36-feature vector)
 └── predictedAt : Instant (T0)
         │
         ▼  [Downstream Attachment Link]
ForecastRecord (F4 Implementation)
 ├── id : UUID
 ├── parentPredictionId : UUID (Foreign key to HotspotPrediction)
 ├── featureSnapshotId : UUID (Lineage traceability)
 ├── h3Index : String
 ├── generatedAt : Instant (T0)
 ├── horizonHours : Integer (1, 3, or 6)
 ├── targetTime : Instant (T0 + horizonHours)
 ├── predictedPm25 : Double
 ├── lowerBound : Double
 └── upperBound : Double
```

**Identity vs. Feature Separation**:
- `parentPredictionId`, `cityId`, `h3Index`, and `featureSnapshotId` are **lineage/context metadata**.
- They are **NOT** inserted into the 36-dimensional numeric feature vector.

---

## 13. Existing Forecast Code Audit

| File Path | Status | Role / Findings |
|:---|:---:|:---|
| [`ai-service/models/artifacts/forecast_regressors_v1.joblib`](file:///c:/Users/lenovo/AeroSential/ai-service/models/artifacts/forecast_regressors_v1.joblib) | **VERIFIED** | Core 131 MB artifact containing 3 independent RF regressors (1h, 3h, 6h). |
| [`ai-service/ml/training/train_all_models.py`](file:///c:/Users/lenovo/AeroSential/ai-service/ml/training/train_all_models.py) | **VERIFIED** | Source training script that fitted the regressors on 36 features with `fillna(0.0)`. |
| [`ai-service/ml/forecast/model.py`](file:///c:/Users/lenovo/AeroSential/ai-service/ml/forecast/model.py) | **LEGACY** | Prototype class (`ForecastModelTrainer`, `max_depth=6`). Not used in production artifact. |
| [`ai-service/ml/inference/confidence.py`](file:///c:/Users/lenovo/AeroSential/ai-service/ml/inference/confidence.py) | **REUSABLE** | Contains `compute_forecast_intervals` implementing $P10/P90$ clipping. |
| [`ai-service/app/main.py`](file:///c:/Users/lenovo/AeroSential/ai-service/app/main.py) | **INCOMPLETE** | Loads artifact on startup. Outdated `/predict/forecast` accepts 15-field payload and falls back to heuristic formula. Lacks `/api/v1/ml/forecast/predict`. |
| [`ai-service/ml/inference/predict_cli.py`](file:///c:/Users/lenovo/AeroSential/ai-service/ml/inference/predict_cli.py) | **F3 ONLY** | Hotspot CLI process bridge only. Does not handle forecast inference. |
| [`backend/.../forecast/Forecast.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/forecast/Forecast.java) | **PARTIAL** | Early entity with `confidence NOT NULL` and `gridCellId`. Lacks `parentPredictionId` and `featureSnapshotId`. |
| [`backend/.../forecast/ForecastService.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/forecast/ForecastService.java) | **PARTIAL** | Stub service querying by cell ID. No AI client integration. |
| [`backend/.../forecast/ForecastController.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/forecast/ForecastController.java) | **PARTIAL** | Minimal stub controller (`/api/v1/forecast/cell/{cellId}`). |
| [`frontend/src/pages/public/Forecast.tsx`](file:///c:/Users/lenovo/AeroSential/frontend/src/pages/public/Forecast.tsx) | **MOCK UI** | Hardcoded 1h–6h synthetic sequence with fabricated confidence values. |

---

## 14. Existing Forecast Test Audit

```
FORECAST_TEST_COVERAGE = INSUFFICIENT FOR PRODUCTION (Basic ML unit tests pass, but end-to-end integration tests are absent)
```

### Passing Tests:
1. `ai-service/tests/ai/test_f3_ml.py::test_forecaster_inference_and_intervals` (**PASSED** in 6.64s): Verifies loading, point estimate generation, residual interval math ($P10 \le P90$), and non-negative clamping for horizons 1, 3, 6.
2. `ai-service/tests/ai/test_f3_intelligence.py::test_forecast_intervals_validity` (**PASSED** in 6.64s): Verifies that near-zero forecasts do not violate physical boundaries ($\ge 0.0$).

### Test Gaps (to be implemented in later F4 phases):
- No validation for unsupported horizons ($T+2\text{h}$, $T+4\text{h}$, $T+12\text{h}$).
- No schema validation test for missing/non-finite 36 features in forecast inference.
- No test for FastAPI `/api/v1/ml/forecast/predict` endpoint.
- No Spring Boot test validating forecast persistence or F3 attachment.

---

## 15. Specification vs. Actual System Gap Analysis

| Component | Planned Specification | Actual System State | Gap Classification |
|:---|:---|:---|:---:|
| **Horizons** | Continuous $1\text{h}$ to $6\text{h}$ (or $+12\text{h}, +24\text{h}$) | Exactly $1\text{h}$, $3\text{h}$, $6\text{h}$ ($T+1, T+3, T+6$) | **MISMATCH** |
| **Input Features** | 36 features (`f3-features-v1`) | Exactly 36 features (`f3-features-v1`) | **PASS** |
| **Current PM2.5 Feature** | Expected current PM2.5 in feature list | Excluded from feature vector; uses spatial lag & co-pollutants | **DOCUMENTED** |
| **Model Type** | Gradient Boosted / Autoregressive | Multi-Horizon Random Forest Regressors | **MISMATCH** |
| **Forecast Confidence** | Scalar confidence score ($0.0 - 1.0$) | Not present in model artifact; only residual intervals | **MISMATCH** |
| **Interval Bounds** | Empirical $P10 - P90$ with clipping | Empirical $P10 - P90$ with clipping $\ge 0.0$ | **PASS** |
| **AI API Endpoint** | `/api/v1/ml/forecast/predict` | Outdated `/predict/forecast` stub using heuristic fallback | **NOT_IMPLEMENTED** |
| **DB Forecast Schema** | Linked to prediction & snapshot | `forecasts` table has `confidence NOT NULL`, no parent FKs | **MISMATCH** |
| **Spring Boot Service** | Ingests snapshots, calls AI, persists forecasts | Minimal repository query stub by `gridCellId` | **NOT_IMPLEMENTED** |
| **React UI** | Live chart of model forecasts | Static mock data showing 6 hourly steps | **NOT_IMPLEMENTED** |

---

## 16. Draft F4 AI Output Contract

The draft specification for the future `/api/v1/ml/forecast/predict` response is locked based **strictly** on actual verified artifact capabilities:

```json
{
  "modelVersion": "forecast_regressors_v1",
  "generatedAt": "2026-09-27T12:00:00Z",
  "h3Index": "886196944dfffff",
  "cityName": "Pune",
  "parentPredictionId": "550e8400-e29b-41d4-a716-446655440000",
  "featureSnapshotId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "status": "SUCCESS",
  "forecasts": [
    {
      "horizonHours": 1,
      "targetHorizon": "T+1h",
      "targetTime": "2026-09-27T13:00:00Z",
      "predictedPm25": 42.15,
      "lowerBound": 40.31,
      "upperBound": 44.01,
      "unit": "ug/m3"
    },
    {
      "horizonHours": 3,
      "targetHorizon": "T+3h",
      "targetTime": "2026-09-27T15:00:00Z",
      "predictedPm25": 48.70,
      "lowerBound": 44.80,
      "upperBound": 51.75,
      "unit": "ug/m3"
    },
    {
      "horizonHours": 6,
      "targetHorizon": "T+6h",
      "targetTime": "2026-09-27T18:00:00Z",
      "predictedPm25": 54.30,
      "lowerBound": 48.78,
      "upperBound": 59.72,
      "unit": "ug/m3"
    }
  ],
  "forecastConfidence": "NOT YET AVAILABLE / TO BE DEFINED IN F4 ENGINEERING",
  "metadata": {
    "algorithm": "RandomForestRegressor(n_estimators=100, max_depth=14)",
    "supportedHorizons": [1, 3, 6],
    "residualQuantilesSource": "forecast_regressors_v1.residuals",
    "clampingApplied": true
  }
}
```

---

## 17. Required F4 Implementation Phases

To maintain strict isolation, future F4 implementation must be sequenced in stages:

1. **Phase F4-P2: AI Service Forecast API & CLI Bridge**
   - Implement deterministic CLI bridge [`predict_forecast_cli.py`](file:///c:/Users/lenovo/AeroSential/ai-service/ml/inference/predict_forecast_cli.py) or FastAPI endpoint `/api/v1/ml/forecast/predict`.
   - Validate 36-feature vector schema and compute non-negative clamped residual intervals.
   - Comprehensive unit and boundary test suite.
2. **Phase F4-P3: Database Schema Migration**
   - Flyway migration adding `parent_prediction_id`, `feature_snapshot_id`, and `h3_index` to `forecasts`.
   - Make `confidence` nullable or define its exact engineering derivation.
3. **Phase F4-P4: Spring Boot Forecast Integration Service**
   - Implement `ForecastAiClient` and service layer that listens to or attaches to `HotspotPrediction` lifecycle.
   - Persist multi-horizon forecasts with full lineage.
4. **Phase F4-P5: React Forecast UI & Timeline Modernization**
   - Replace static 6-step mock with real multi-horizon ($+1\text{h}, +3\text{h}, +6\text{h}$) chart and timeline.
   - Visualize empirical uncertainty envelopes ($P10$ to $P90$).

---

## 18. Files Inspected

1. `ai-service/models/artifacts/forecast_regressors_v1.joblib`
2. `ai-service/models/artifacts/hotspot_classifier_v1.joblib`
3. `ai-service/models/artifacts/metadata_v1.json`
4. `ai-service/ml/training/train_all_models.py`
5. `ai-service/ml/forecast/model.py`
6. `ai-service/ml/pipeline.py`
7. `ai-service/ml/inference/confidence.py`
8. `ai-service/ml/inference/predict_cli.py`
9. `ai-service/app/main.py`
10. `ai-service/tests/ai/test_f3_ml.py`
11. `ai-service/tests/ai/test_f3_intelligence.py`
12. `backend/src/main/resources/db/migration/V1__init_schema.sql`
13. `backend/src/main/java/com/aerosentinel/forecast/Forecast.java`
14. `backend/src/main/java/com/aerosentinel/forecast/ForecastService.java`
15. `backend/src/main/java/com/aerosentinel/forecast/ForecastRepository.java`
16. `backend/src/main/java/com/aerosentinel/forecast/ForecastController.java`
17. `frontend/src/types/index.ts`
18. `frontend/src/pages/public/Forecast.tsx`
19. `frontend/src/services/forecast.service.ts`

---

## 19. Files Changed

- None (Strict read-only audit phase). Only this audit report was authored.

---

## 20. Files NOT Changed

- All F3 production code, tests, and configuration remain 100% untouched.
- All backend entities, repositories, and migrations remain untouched.
- All frontend UI components and services remain untouched.
- All model artifacts remain untouched (no retraining, no serialization changes).

---

## 21. Risks / Unknowns

1. **Residual Discrepancy (Validation vs. Test Partition)**: The artifact `.joblib` embedded residuals are derived from the Validation partition (e.g., T+1h is $[-1.84, +1.86]$), whereas `metadata_v1.json` and `confidence.py` record Test holdout residuals (e.g., T+1h is $[-2.41, +2.15]$). Phase F4-P2 must formally declare which set of quantiles is the production standard.
2. **Database Nullability Constraint on `confidence`**: Table `forecasts` has `confidence DOUBLE PRECISION NOT NULL`. Because the forecaster does not output a confidence score, saving forecast rows will fail unless the column is made nullable via migration or populated with a defined heuristic.
3. **Frontend Expectation of 6 Consecutive Hours**: Current frontend components assume consecutive hours $1, 2, 3, 4, 5, 6$. Updating the UI to display discrete horizons $1\text{h}, 3\text{h}, 6\text{h}$ will require chart axis adjustments.

---

## 22. Final P1 Decision

```
DECISION: P1 AUDIT COMPLETE — CONTRACT LOCKED FOR F4 IMPLEMENTATION
```

The artifact `forecast_regressors_v1.joblib` is verified, structurally sound, and its contracts for horizons ($1\text{h}, 3\text{h}, 6\text{h}$), features (36 inputs), intervals (asymmetric empirical residual bounds with zero-clamping), and F3 lineage attachment are locked. Phase F4-P1 is complete. Development can safely proceed to Phase F4-P2 when authorized.

---

## Machine-Readable Summary

```json
F4_P1_AUDIT_SUMMARY = {
  "artifactExists": "PASS",
  "artifactLoad": "PASS",
  "horizonAudit": "MISMATCH",
  "featureAudit": "PASS",
  "featureOrderVerified": "PASS",
  "f3F4FeatureSeparation": "PASS",
  "modelArchitectureAudit": "PASS",
  "intervalAudit": "PASS",
  "temporalAlignmentAudit": "PASS",
  "f3AttachmentAudit": "PASS",
  "inferenceCodeAudit": "NOT_IMPLEMENTED",
  "forecastTests": "INSUFFICIENT",
  "contractStatus": "READY_FOR_F4_IMPLEMENTATION"
}
```
