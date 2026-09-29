# AeroSentinel — F4 Multi-Horizon PM2.5 Forecast Specification (V1)

**Document ID**: `AEROSENTINEL-F4-SPEC-V1`  
**Status**: AUTHORITATIVE & LOCKED FOR IMPLEMENTATION (P2+)  
**Approved Baseline**: Physical ML Artifact `forecast_regressors_v1.joblib`  
**Reconciliation Phase**: F4-P1.1  
**Last Updated**: September 27, 2026  

---

## 1. Executive Purpose & Scope

This specification establishes the **sole authoritative engineering contract** for Feature 4 (Multi-Horizon PM2.5 Forecasting) in AeroSentinel. It reconciles historical planning notes, early design mocks, and exploratory reports with the **actual physical reality** of the verified machine learning artifact [`ai-service/models/artifacts/forecast_regressors_v1.joblib`](file:///c:/Users/lenovo/AeroSential/ai-service/models/artifacts/forecast_regressors_v1.joblib).

All future F4 development—including Phase F4-P2 (AI inference bridge), Phase F4-P3 (database migrations), Phase F4-P4 (Spring Boot service layer), and Phase F4-P5 (React UI)—must adhere strictly to this contract.

---

## 2. Core Architectural Principles

1. **Physical Artifact is Supreme**: The serialized models in `forecast_regressors_v1.joblib` dictate supported horizons, feature counts, feature ordering, and interval bounds. Code and schemas must adapt to the artifact, not vice versa.
2. **Discrete Horizons Only ($T+1\text{h}, T+3\text{h}, T+6\text{h}$)**: No continuous 1–6h interpolation, synthesis, or extrapolation is permitted.
3. **F3 Immutability & Decoupling**: F3 hotspot detection is locked. F4 attaches purely downstream via lineage keys (`parentPredictionId`, `cityId`, `h3Index`, `featureSnapshotId`, `predictedAt`). Shared 36-feature schema similarity does **not** mean shared implementation; F4 maintains its own dedicated feature builder and adapter.
4. **No Synthetic Confidence**: The model artifact outputs continuous PM2.5 concentrations and empirical error bounds ($P10$ to $P90$), not classification probabilities. F4 does not fabricate a synthetic scalar confidence score (e.g., $0.74$) nor copy F3 hotspot confidence.

---

## 3. Supported Forecast Horizons

The authoritative supported horizons are strictly:

$$\mathcal{H} = \{1, 3, 6\}$$

### Canonical Identifiers:
- `horizonHours = 1` $\longrightarrow$ `targetHorizon = "T+1h"` (1 hour ahead of $T_0$)
- `horizonHours = 3` $\longrightarrow$ `targetHorizon = "T+3h"` (3 hours ahead of $T_0$)
- `horizonHours = 6` $\longrightarrow$ `targetHorizon = "T+6h"` (6 hours ahead of $T_0$)

### Legacy Status of Other Horizons:
- **Continuous 1h, 2h, 3h, 4h, 5h, 6h**: **SUPERSEDED**. Regressors for $+2\text{h}$, $+4\text{h}$, and $+5\text{h}$ do not exist in the artifact.
- **Extended Horizons $T+12\text{h}, T+24\text{h}$**: **PLANNED / FUTURE SCOPE ONLY**. Not trained in the current artifact.

---

## 4. Authoritative Feature Contract (36 Features)

The forecaster consumes an ordered 1-D vector of **exactly 36 numeric features** (`f3-features-v1`), matching `forecast_regressors_v1.joblib.feature_cols`:

```text
F4_FEATURE_COUNT = 36
SOURCE OF TRUTH = forecast_regressors_v1.joblib.feature_cols
```

| Index | Feature Column Name | Data Type | Source Subsystem | Imputation / Default |
|:---:|:---|:---:|:---|:---|
| 0 | `latitude` | `float64` | F1 / H3 Grid | Centroid decimal lat |
| 1 | `longitude` | `float64` | F1 / H3 Grid | Centroid decimal lon |
| 2 | `pm10` | `float64` | F1 Air Quality | Last known station $\mu\text{g/m}^3$ |
| 3 | `no2` | `float64` | F1 Air Quality | Last known station $\mu\text{g/m}^3$ |
| 4 | `so2` | `float64` | F1 Air Quality | Last known station $\mu\text{g/m}^3$ |
| 5 | `co` | `float64` | F1 Air Quality | Last known station $\text{mg/m}^3$ |
| 6 | `o3` | `float64` | F1 Air Quality | Last known station $\mu\text{g/m}^3$ |
| 7 | `hour` | `float64` | UTC Timestamp | Integer $0 - 23$ |
| 8 | `day_of_week` | `float64` | UTC Timestamp | Integer $0 - 6$ |
| 9 | `is_weekend` | `float64` | UTC Timestamp | Binary $0$ or $1$ |
| 10 | `hour_sin` | `float64` | Derived Temporal | $\sin(2\pi \cdot \text{hour} / 24)$ |
| 11 | `hour_cos` | `float64` | Derived Temporal | $\cos(2\pi \cdot \text{hour} / 24)$ |
| 12 | `dow_sin` | `float64` | Derived Temporal | $\sin(2\pi \cdot \text{dow} / 7)$ |
| 13 | `dow_cos` | `float64` | Derived Temporal | $\cos(2\pi \cdot \text{dow} / 7)$ |
| 14 | `temperature` | `float64` | F2 Open-Meteo | Surface temp in $^{\circ}\text{C}$ |
| 15 | `humidity` | `float64` | F2 Open-Meteo | Relative humidity $\%$ |
| 16 | `wind_speed` | `float64` | F2 Open-Meteo | Wind speed in $\text{m/s}$ |
| 17 | `wind_direction` | `float64` | F2 Open-Meteo | Meteorological degrees |
| 18 | `wind_u` | `float64` | F2 Derived | Zonal velocity: $-w \cdot \sin(\theta)$ |
| 19 | `wind_v` | `float64` | F2 Derived | Meridional velocity: $-w \cdot \cos(\theta)$ |
| 20 | `rainfall` | `float64` | F2 Open-Meteo | Hourly precipitation in $\text{mm}$ |
| 21 | `pressure` | `float64` | F2 Open-Meteo | Surface pressure in $\text{hPa}$ |
| 22 | `pm25_spatial_lag_mean` | `float64` | F2 Spatial Lag | Neighbor H3 PM2.5 mean |
| 23 | `nearest_station_distance_km`| `float64`| F1 / GIS | Distance in $\text{km}$ |
| 24 | `stations_within_5km_count` | `float64`| F1 / GIS | Count within $5\text{km}$ |
| 25 | `monitoring_coverage_gap_flag`| `float64`| F1 / GIS | $1$ if $>7\text{km}$ or count $=0$ |
| 26 | `dist_to_nearest_industrial_km`|`float64`| F2 GIS Context | Distance in $\text{km}$ |
| 27 | `dist_to_nearest_major_road_km`|`float64`| F2 GIS Context | Distance in $\text{km}$ |
| 28 | `sensitive_receptors_count_2km`|`float64`| F2 GIS Context | Count within $2\text{km}$ |
| 29 | `industrial_zone_within_2km_flag`|`float64`| F2 GIS Context | $1$ if present, else $0$ |
| 30 | `fire_count_24h_25km` | `float64` | F2 NASA FIRMS | VIIRS hotspot count in $25\text{km}$ |
| 31 | `fire_frp_sum_24h_25km` | `float64` | F2 NASA FIRMS | Total FRP in $\text{MW}$ |
| 32 | `fire_frp_mean_24h_25km` | `float64` | F2 NASA FIRMS | Mean FRP in $\text{MW}$ |
| 33 | `nearest_fire_distance_km` | `float64` | F2 NASA FIRMS | Distance to fire in $\text{km}$ |
| 34 | `fire_frp_distance_decay` | `float64` | F2 Derived | Gaussian distance-decayed FRP |
| 35 | `fire_upwind_alignment_score`| `float64` | F2 Derived | Advection alignment score |

### Critical PM2.5 Feature Clarification:
- **`pm25_spatial_lag_mean`** is an **actual trained feature** (index 22).
- **`pm25_t`, `pm25_t-1`, `pm25_t-2`, `rolling_mean`, `rolling_max`, `change_rate`** are **CONCEPTUAL ONLY** from early design documentation. They are **NOT** present in `feature_cols` and must **NOT** be included in the inference feature vector.
- Raw `pm25` was deliberately excluded from the shared training pipeline to prevent target leakage.

---

## 5. Model Architecture & Parameters

For each horizon $h \in \{1, 3, 6\}$:
- **Model Class**: `sklearn.ensemble.RandomForestRegressor`
- **Number of Estimators**: 100
- **Max Depth**: 14
- **Random State**: 42
- **Training Method**: Independent direct fitting on forward-shifted target `target_pm25_t_plus_{h}`.
- **Output**: Point estimate $\hat{y}_h \in \mathbb{R}^+$ in $\mu\text{g/m}^3$.

---

## 6. Prediction Intervals & Uncertainty

The forecaster produces empirical prediction intervals derived from validation residual quantiles ($P10$ to $P90$):

$$\text{lowerBound} = \max(0.0, \hat{y} + P10)$$
$$\text{upperBound} = \hat{y} + P90$$

### Verified Residual Quantiles (Validation Partition):
- **Horizon 1 ($T+1\text{h}$)**: $P10 = -1.8425\,\mu\text{g/m}^3, \quad P90 = +1.8629\,\mu\text{g/m}^3$
- **Horizon 3 ($T+3\text{h}$)**: $P10 = -3.9014\,\mu\text{g/m}^3, \quad P90 = +3.0474\,\mu\text{g/m}^3$
- **Horizon 6 ($T+6\text{h}$)**: $P10 = -5.5208\,\mu\text{g/m}^3, \quad P90 = +5.4249\,\mu\text{g/m}^3$

### Guardrail Constraints:
- **Physical Non-Negative Clamping**: $\text{lowerBound} \ge 0.0\,\mu\text{g/m}^3$ is strictly enforced.
- **Asymmetry**: Intervals reflect empirical pollutant distribution skew.
- **Forecast Confidence Representation**: The artifact does not produce probabilistic confidence. The field is defined as `forecastConfidence: null` in JSON responses and nullable in database records.

---

## 7. Authoritative Output Contracts

### 7.1 AI Service REST Contract (`POST /api/v1/ml/forecast/predict`)

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
  "forecastConfidence": null,
  "metadata": {
    "algorithm": "RandomForestRegressor(n_estimators=100, max_depth=14)",
    "supportedHorizons": [1, 3, 6],
    "residualQuantilesSource": "forecast_regressors_v1.residuals",
    "clampingApplied": true
  }
}
```

### 7.2 Spring Boot REST Contract (`GET /api/v1/forecast/{h3Index}`)

```json
{
  "h3Index": "886196944dfffff",
  "cityName": "Pune",
  "generatedAt": "2026-09-27T12:00:00Z",
  "parentPredictionId": "550e8400-e29b-41d4-a716-446655440000",
  "featureSnapshotId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "unit": "ug/m3",
  "forecasts": [
    {
      "horizonHours": 1,
      "targetTime": "2026-09-27T13:00:00Z",
      "predictedPm25": 42.15,
      "lowerBound": 40.31,
      "upperBound": 44.01
    },
    {
      "horizonHours": 3,
      "targetTime": "2026-09-27T15:00:00Z",
      "predictedPm25": 48.70,
      "lowerBound": 44.80,
      "upperBound": 51.75
    },
    {
      "horizonHours": 6,
      "targetTime": "2026-09-27T18:00:00Z",
      "predictedPm25": 54.30,
      "lowerBound": 48.78,
      "upperBound": 59.72
    }
  ],
  "forecastConfidence": null,
  "modelVersion": "forecast_regressors_v1"
}
```

---

## 8. Database Schema Target (Phase F4-P3 Migration)

The `forecasts` table will be updated via Flyway migration in Phase F4-P3:

```sql
-- Target Schema: forecasts
CREATE TABLE IF NOT EXISTS forecasts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parent_prediction_id UUID REFERENCES hotspot_predictions(id) ON DELETE CASCADE,
    feature_snapshot_id UUID REFERENCES feature_snapshots(id) ON DELETE CASCADE,
    grid_cell_id UUID REFERENCES grid_cells(id) ON DELETE CASCADE,
    h3_index VARCHAR(20) NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    target_time TIMESTAMP WITH TIME ZONE NOT NULL,
    horizon_hours INTEGER NOT NULL CHECK (horizon_hours IN (1, 3, 6)),
    predicted_pm25 DOUBLE PRECISION NOT NULL,
    lower_bound DOUBLE PRECISION,
    upper_bound DOUBLE PRECISION,
    confidence DOUBLE PRECISION, -- NULLABLE: Model does not produce probabilistic confidence
    model_version VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_forecasts_prediction ON forecasts(parent_prediction_id);
CREATE INDEX IF NOT EXISTS idx_forecasts_cell_target ON forecasts(grid_cell_id, target_time ASC);
CREATE INDEX IF NOT EXISTS idx_forecasts_h3_target ON forecasts(h3_index, target_time ASC);
```

---

## 9. Contract Reconciliation Matrix

| Specification Item | Historical / Legacy Value | Authoritative Reconciled Value | Reconciliation Status |
|:---|:---|:---|:---:|
| **Horizons** | Continuous $1\text{h} - 6\text{h}$, or $+12\text{h}, +24\text{h}$ | Discrete $\{1, 3, 6\}$ ($T+1\text{h}, T+3\text{h}, T+6\text{h}$) | **LOCKED** |
| **Model Type** | XGBoost / LightGBM Autoregressive | Multi-Horizon Random Forest Regressors | **LOCKED** |
| **Feature Count** | 35 features or conceptual subset | Exactly 36 features (`f3-features-v1`) | **LOCKED** |
| **Feature Order** | Arbitrary or conceptual order | Exact artifact `feature_cols` order | **LOCKED** |
| **Lagged PM2.5** | `pm25_t`, `pm25_t-1`, `rolling_mean`, etc. | Not in feature vector; uses `pm25_spatial_lag_mean` | **LOCKED** |
| **Forecast Confidence** | Scalar $0.74$ / $0.88$ | `null` (Residual intervals supported, not confidence) | **LOCKED** |
| **Uncertainty Bounds** | Symmetric standard deviation | Asymmetric empirical residual quantiles ($P10 - P90$) | **LOCKED** |
| **Physical Constraint** | Unclamped | Clamped $\text{lowerBound} \ge 0.0\,\mu\text{g/m}^3$ | **LOCKED** |
| **F3 Attachment** | Undefined / Implicit | Explicit lineage (`parentPredictionId`, `snapshotId`) | **LOCKED** |
