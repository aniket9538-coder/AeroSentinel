# AeroSentinel — F3 Phase 5: Trained ML Hotspot Inference Integration Report

## 1. Objective
The goal of F3 Phase 5 is to connect the real trained machine learning model artifact (`hotspot_classifier_v1.joblib`) to the persisted F3 `FeatureSnapshot` records, executing calibrated probability inference through the pluggable `HotspotDetectionEngine` contract, and serving predictions through the existing Spring Boot REST APIs and React UI without breaking any F2 or F3 Phase 1–4 contracts.

---

## 2. Previous F3 Architecture (Phases 1–4 Review)
- **F3 Phase 1**: Feature contract audit establishing 36 numeric features in fixed order.
- **F3 Phase 2**: Real production feature data layer storing `FeatureSnapshot` entities with `VALID` / `DEGRADED` / `UNAVAILABLE` quality statuses in PostgreSQL JSONB.
- **F3 Phase 3**: Product-side hotspot domain, JPA persistence (`hotspot_predictions`), `BaselineHotspotDetectionEngine`, and REST endpoints (`GET /api/v1/hotspots`, `GET /api/v1/hotspots/{h3Index}`).
- **F3 Phase 4**: Frontend hotspot intelligence integrating H3 risk layer, sidebar metrics, threshold badges, and freshness chips into the existing React/Leaflet dashboard.

---

## 3. ML Artifact Audit
The actual trained model artifact was audited directly from the filesystem:
- **Location**: `ai-service/models/artifacts/hotspot_classifier_v1.joblib`
- **File Size**: 20,495,190 bytes (~20.5 MB)
- **Container Structure**: Python dictionary serialized via `joblib` containing:
  - `model`: Trained scikit-learn estimator
  - `feature_cols`: List of 36 feature column names in exact order
  - `operational_threshold`: `0.20`
  - `algorithm`: `"CalibratedClassifierCV(Sigmoid, cv=3) over Balanced RandomForest"`
  - `benchmark_summary`: Metrics dictionary (PR-AUC: 0.6559, ROC-AUC: 0.8879, Optimal F1: 0.6728)
- **Model Object Hierarchy**:
  - Top level: `CalibratedClassifierCV`
  - Method: `Sigmoid` (Platt scaling)
  - Number of calibrated estimators: 3 (`cv=3`)
  - Base estimator: `RandomForestClassifier` with `class_weight='balanced'`
- **Target Classes**: `[0, 1]` with binary positive class `1` located at index `1` (`pos_idx = 1`).

---

## 4. Exact 36-Feature Verification
The 36 features in `hotspot_classifier_v1.joblib` match `select_features()` from `ai-service/ml/training/train_all_models.py` and `FeatureRecord.ORDERED_FEATURE_NAMES` in exact sequence:

| Index | Feature Name | Source | Type | Unit |
|---|---|---|---|---|
| 0 | `latitude` | H3 Centroid | Float | Degrees |
| 1 | `longitude` | H3 Centroid | Float | Degrees |
| 2 | `pm10` | CPCB / Ingestion | Float | µg/m³ |
| 3 | `no2` | CPCB / Ingestion | Float | µg/m³ |
| 4 | `so2` | CPCB / Ingestion | Float | µg/m³ |
| 5 | `co` | CPCB / Ingestion | Float | mg/m³ |
| 6 | `o3` | CPCB / Ingestion | Float | µg/m³ |
| 7 | `hour` | Observation Time | Integer | 0–23 |
| 8 | `day_of_week` | Observation Time | Integer | 0–6 |
| 9 | `is_weekend` | Observation Time | Integer | 0 or 1 |
| 10 | `hour_sin` | Temporal | Float | [-1, 1] |
| 11 | `hour_cos` | Temporal | Float | [-1, 1] |
| 12 | `dow_sin` | Temporal | Float | [-1, 1] |
| 13 | `dow_cos` | Temporal | Float | [-1, 1] |
| 14 | `temperature` | Open-Meteo | Float | °C |
| 15 | `humidity` | Open-Meteo | Float | % |
| 16 | `wind_speed` | Open-Meteo (converted) | Float | m/s |
| 17 | `wind_direction` | Open-Meteo | Float | Degrees (0–360) |
| 18 | `wind_u` | Derived Weather | Float | m/s |
| 19 | `wind_v` | Derived Weather | Float | m/s |
| 20 | `rainfall` | Open-Meteo | Float | mm |
| 21 | `pressure` | Open-Meteo | Float | hPa |
| 22 | `pm25_spatial_lag_mean` | Spatial Lag (LOO) | Float | µg/m³ |
| 23 | `nearest_station_distance_km` | Spatial Station | Float | km |
| 24 | `stations_within_5km_count` | Spatial Station | Integer | count |
| 25 | `monitoring_coverage_gap_flag`| Coverage Gap | Integer | 0 or 1 |
| 26 | `dist_to_nearest_industrial_km`| GIS OSM | Float | km |
| 27 | `dist_to_nearest_major_road_km`| GIS OSM | Float | km |
| 28 | `sensitive_receptors_count_2km`| GIS OSM | Integer | count |
| 29 | `industrial_zone_within_2km_flag`| GIS OSM | Integer | 0 or 1 |
| 30 | `fire_count_24h_25km` | NASA FIRMS | Integer | count |
| 31 | `fire_frp_sum_24h_25km` | NASA FIRMS | Float | MW |
| 32 | `fire_frp_mean_24h_25km` | NASA FIRMS | Float | MW |
| 33 | `nearest_fire_distance_km` | NASA FIRMS | Float | km |
| 34 | `fire_frp_distance_decay` | Spatial Decay | Float | MW/km |
| 35 | `fire_upwind_alignment_score` | Dispersion Vector | Float | [0, 1] |

---

## 5. Feature-Vector Adapter
The `ModelFeatureVectorAdapter` in both Java (`com.aerosentinel.hotspot.ModelFeatureVectorAdapter`) and Python (`ai-service/app/main.py`) validates:
1. Exact feature count: 36.
2. Exact key names in strict order.
3. Numeric type conversion (`Double` / `float`).
4. Finite value check (no `NaN`, `+Infinity`, `-Infinity`).
5. Missingness check: reject snapshots with unpopulated required features.

---

## 6. Wind Unit Boundary Verification
- `FeatureSnapshot` stores `wind_speed` in km/h (direct Open-Meteo API source unit), while `wind_u` and `wind_v` are derived in m/s.
- The model expects `wind_speed` in **m/s**.
- Conversion formula:
  $$\text{wind\_speed}_{\text{mps}} = \frac{\text{wind\_speed}_{\text{kmh}}}{3.6}$$
- **Double-conversion guard**: Conversion is applied strictly once at the model boundary in `ModelFeatureVectorAdapter.toModelFeatureVector()` and `ai-service/ml/inference/predict_cli.py`.
- **Proof test**: 36.0 km/h is verified to convert to exactly 10.0 m/s (`test_wind_unit_boundary_conversion` in `test_f3_ml_inference.py` and `MLHotspotDetectionEngineTest.testWindSpeedUnitConversionKmToMps()`).

---

## 7. ML Engine Architecture & Separation of Concerns
The architecture preserves strict separation of concerns:
```
           HotspotController (Spring Boot Web)
                        │
                  HotspotService
                        │
             ┌──────────┴──────────┐
             ▼                     ▼
   MLHotspotDetectionEngine    BaselineHotspotDetectionEngine
   (for Pune PMR)              (for Mumbai & Delhi)
             │
   AiServiceHotspotClient
        ┌────┴────────────────────────┐
        ▼                             ▼
   FastAPI HTTP Endpoint        predict_cli.py (Fallback)
   /api/v1/ml/hotspot/predict         │
        └──────────────┬──────────────┘
                       ▼
          hotspot_classifier_v1.joblib
```
- Controllers, Repositories, Entities, and React never touch `joblib` or Python internals.
- The interface `HotspotDetectionEngine` remains the single contract.

---

## 8. Model Loading
- **Python Service**: The artifact is loaded once at FastAPI startup (`lifespan` handler) or upon CLI process invocation into an in-memory cache.
- **Fail-Fast**: If the artifact is missing or corrupted, the service raises `ModelArtifactNotFoundError` or `ModelCorruptError` and returns `MODEL_UNAVAILABLE` (HTTP 503).
- **Zero Secrets**: No tokens or credentials are logged during model loading.

---

## 9. Probability & Operational Threshold Handling
- Inference executes `predict_proba(vector)` on the calibrated classifier.
- The positive-class index is dynamically resolved via `model.classes_ == 1` (`pos_idx = 1`).
- Operational decision rule:
  $$\text{isHotspot} = \begin{cases} \text{true} & \text{if } p \ge 0.20 \\ \text{false} & \text{if } p < 0.20 \end{cases}$$
- The raw calibrated probability $p \in [0, 1]$ is stored as `risk_score` in `hotspot_predictions`.

---

## 10. Risk Level Mapping
The 4-tier risk classification maps directly from the calibrated probability $p$ and operational threshold $\theta = 0.20$:

| Probability Range | Risk Level | Meaning | Action Trigger |
|---|---|---|---|
| $p < 0.20$ | `LOW` | Below operational threshold | Baseline monitoring |
| $0.20 \le p < 0.40$ | `MODERATE` | Above operational threshold | Advisory alert |
| $0.40 \le p < 0.70$ | `HIGH` | High probability hotspot | Targeted inspection |
| $p \ge 0.70$ | `CRITICAL` | Severe calibrated probability | Emergency abatement |

---

## 11. Confidence Architecture
Model probability and confidence remain decoupled:
- **`risk_score`** = Calibrated positive class probability $p$ (e.g. `0.7998`).
- **`confidence`** = Data quality completeness combined with distance to operational margin:
  $$\text{confidence} = 0.50 + 0.30 \times \text{qualityScore} + 0.20 \times |p - \theta|$$
- Cap: Maximum confidence is bounded by feature freshness and completeness. It never displays 98%+ unless fully supported.

---

## 12. Model Domain & Geography Policy
- The model `hotspot_classifier_v1.joblib` was trained and cross-validated exclusively on Pune Metropolitan Region (PMR) CAAQMS stations.
- **Enforcement**:
  - Pune cells (`550e8400-e29b-41d4-a716-446655440001`): Executed through `MLHotspotDetectionEngine`. Predictions carry `modelVersion = "hotspot_classifier_v1"` and `engineType = "ML"`.
  - Non-Pune cities (Mumbai, Delhi): Handled gracefully via `BaselineHotspotDetectionEngine` with `modelVersion = "hotspot-baseline-v1"` and `engineType = "BASELINE"`. The ML engine rejects non-Pune requests with `MODEL_DOMAIN_UNSUPPORTED`.

---

## 13. Database Integration & Feature Traceability
- Table: `hotspot_predictions` (reused, no redundant tables created).
- Every ML prediction records:
  - `h3_index`: Unique Uber H3 cell index (Resolution 8).
  - `risk_score`: Calibrated ML probability (4 decimal places).
  - `risk_level`: Mapped risk level (`CRITICAL`, `HIGH`, `MODERATE`, `LOW`).
  - `confidence`: Confidence score.
  - `model_version`: `"hotspot_classifier_v1"`.
  - `feature_snapshot_id`: Direct foreign key to `feature_snapshots.id`.
- Immutable prediction history preserved with tie-breaking `ORDER BY predicted_at DESC, created_at DESC`.

---

## 14. API Compatibility
Public endpoints remain 100% backwards-compatible:
1. `GET /api/v1/hotspots?cityId={cityId}`: Returns `HotspotOverviewResponse` with `engineType = "ML"`, `modelVersion = "hotspot_classifier_v1"`, and cell-level DTOs.
2. `GET /api/v1/hotspots/{h3Index}`: Returns `HotspotCellDto` for the selected cell with latest ML predictions.

---

## 15. Failure Handling Matrix

| Scenario | Handled By | System State / Output |
|---|---|---|
| Model file missing | `AiServiceHotspotClient` / `FastAPI` | `MODEL_UNAVAILABLE` (fallback to baseline) |
| Model loading error | `ModelLoader` | `MODEL_LOAD_FAILED` |
| FeatureSnapshot is null / degraded | `MLHotspotDetectionEngine` | `INSUFFICIENT_DATA` |
| Missing required feature | `ModelFeatureVectorAdapter` | `INSUFFICIENT_DATA` |
| Non-Pune city ID | `HotspotService` / `MLHotspotDetectionEngine` | `MODEL_DOMAIN_UNSUPPORTED` (routed to baseline) |
| Non-finite float value | `ModelFeatureVectorAdapter` | `INVALID_FEATURE_VALUE` |
| Inference exception | `AiServiceHotspotClient` | `MODEL_INFERENCE_FAILED` |

---

## 16. Real Pune Inference Result
Executed against real persisted Pune CAAQMS snapshot from Shivajinagar (`88608850e5fffff`):

- **Feature Snapshot ID**: `1624baa3-a5f8-407b-b1c2-36bcee7650b1`
- **H3 Index**: `88608850e5fffff`
- **Observed Timestamp**: `2026-09-26T13:09:44.571028Z`
- **Raw Input Telemetry**:
  - PM2.5: 84.5 µg/m³
  - PM10: 142.0 µg/m³
  - NO2: 38.2 µg/m³
  - Wind Speed (raw): 15.7 km/h $\to$ normalized: 4.36 m/s
  - Wind Direction: 245.0°
  - Temperature: 27.4 °C, Humidity: 68.0%
- **Inference Output**:
  - Calibrated Probability (`riskScore`): **0.7998** (79.98%)
  - Operational Threshold: **0.20**
  - Positive Decision (`isHotspot`): **true** ($0.7998 \ge 0.20$)
  - Risk Level: **`CRITICAL`**
  - Confidence: **0.86** (86.0%)
  - Model Version: **`hotspot_classifier_v1`**
  - Engine Type: **`ML`**

---

## 17. Baseline vs. ML Engineering Comparison
Controlled comparison on the identical Pune Shivajinagar snapshot (`1624baa3-a5f8-407b-b1c2-36bcee7650b1`):

| Attribute | Baseline Engine (`hotspot-baseline-v1`) | ML Engine (`hotspot_classifier_v1`) |
|---|---|---|
| **Risk Score** | 0.7225 (Heuristic composite) | 0.7998 (Calibrated empirical probability) |
| **Risk Level** | HIGH | CRITICAL |
| **Confidence** | 0.85 | 0.86 |
| **Wind Handling** | Raw km/h thresholds | Normalized m/s vector alignment |
| **Spatial Feature Support** | Direct station distance | 36 features including spatial lag & fire decay |
| **Primary Strength** | Lightweight, zero-dependency | Empirically calibrated against historical CAAQMS ground truth |

---

## 18. Automated Test Results
- **Python ML Inference Suite** (`tests/test_f3_ml_inference.py`):
  - 9/9 tests PASSED
- **Python Feature Contract Suite** (`tests/test_f3_feature_contract.py`):
  - 12/12 tests PASSED
  - Total Python: **21/21 passed (100%)**
- **Spring Boot Backend Test Suite**:
  - `MLHotspotDetectionEngineTest`: 9/9 passed
  - `HotspotIntegrationTest`: 7/7 passed
  - `HotspotPredictionValidatorTest`: 6/6 passed
  - Total Backend: **172 passed, 0 failures, 1 skipped (100%)**
- **Frontend Production Build**:
  - `tsc -b && vite build` passed with 0 errors in 18.61s.

---

## 19. F2 / F3 Regression Verification
- All F2 Air observation ingestion tests: **PASS**
- All F2 Weather ingestion and spatial association tests: **PASS**
- All F2 H3 spatial boundary and index resolution tests: **PASS**
- All F3 Phase 2 Feature Snapshot derivation tests: **PASS**
- All F3 Phase 3 Baseline detection engine tests: **PASS**
- All F3 Phase 4 Frontend component types and builds: **PASS**

---

## 20. Known Limitations
1. **PMR Geographic Scope**: The model is trained exclusively on Pune Metropolitan Region. Non-Pune cities must continue using the baseline engine until multi-city training datasets are calibrated.
2. **Missing Co-pollutants in Mumbai/Delhi**: Certain stations in Mumbai/Delhi lack continuous SO2/CO sensors, resulting in `UNAVAILABLE` feature snapshots for those cells.
3. **Execution Latency**: Python inter-process execution via CLI fallback has higher latency (~120ms) than native FastAPI HTTP invocation (~15ms).

---

## 21. Phase 6 Starting Point
F3 Phase 5 is complete with all criteria satisfied. The starting point for Phase 6 is:
- Live Pune predictions powered by `hotspot_classifier_v1` with calibrated probabilities persisted in `hotspot_predictions`.
- Baseline fallback active for Mumbai and Delhi.
- Production feature pipeline feeding valid 36-feature vectors into the ML engine.
