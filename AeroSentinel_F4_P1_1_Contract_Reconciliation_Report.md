# AeroSentinel — F4-P1.1 Contract Reconciliation & Specification Lock Report

**Document ID**: `AEROSENTINEL-F4-P1.1-RECONCILIATION`  
**Phase**: F4 Phase 1.1 — Contract Reconciliation & Specification Lock  
**Author**: Antigravity AI Assistant & Engineering Team  
**Date**: September 27, 2026  
**Status**: COMPLETE / LOCKED FOR PHASE F4-P2  

---

## 1. Purpose

The objective of Phase F4-P1.1 is to eliminate all remaining discrepancies between historical project documentation and the physical machine learning artifact [`forecast_regressors_v1.joblib`](file:///c:/Users/lenovo/AeroSential/ai-service/models/artifacts/forecast_regressors_v1.joblib) discovered during Phase F4-P1. 

Prior to beginning implementation in Phase F4-P2, this phase establishes a unified, unambiguous source of truth across all project specifications, API contracts, architectural guides, and engineering reports.

---

## 2. P1 Artifact Facts Being Preserved

The physical model artifact [`ai-service/models/artifacts/forecast_regressors_v1.joblib`](file:///c:/Users/lenovo/AeroSential/ai-service/models/artifacts/forecast_regressors_v1.joblib) is the supreme technical authority. All factual properties audited in Phase F4-P1 are strictly preserved without modification:

- **File Path**: `ai-service/models/artifacts/forecast_regressors_v1.joblib`
- **File Size**: 131,306,591 bytes (~125.22 MB)
- **SHA-256 Hash**: `55dcee656aed9bd968053d7bf57b27e1d52e8f47585b99a4e5f9368cd7161c84`
- **Format**: Joblib serialized Python dictionary
- **Estimators**: 3 independent `RandomForestRegressor` models (100 trees, max depth 14, `random_state=42`)
- **Supported Horizons**: Exactly $[1, 3, 6]$ ($T+1\text{h}, T+3\text{h}, T+6\text{h}$)
- **Feature Vector**: Exactly 36 ordered float features (`f3-features-v1`)
- **Uncertainty Bounds**: Asymmetric empirical residual quantiles ($P10$ to $P90$) with physical clamping ($\ge 0.0\,\mu\text{g/m}^3$)
- **Forecast Confidence**: Null / Not produced natively by the model

---

## 3. Horizon Reconciliation

Historical documents contained references to continuous $1\text{h}$–$6\text{h}$ forecasts or extended horizons ($T+12\text{h}, T+24\text{h}$).

### Reconciled Standard:
The supported forecast horizons are **strictly and exclusively**:

$$\mathcal{H} = [1, 3, 6]$$

**Canonical Form**:
- `horizonHours = 1` $\longrightarrow$ `T+1h`
- `horizonHours = 3` $\longrightarrow$ `T+3h`
- `horizonHours = 6` $\longrightarrow$ `T+6h`

### Resolution Rules:
- **No Interpolation**: No artificial linear interpolation or spline fitting to generate $+2\text{h}$, $+4\text{h}$, or $+5\text{h}$.
- **No Extrapolation**: No synthetic extrapolation for $+12\text{h}$ or $+24\text{h}$.
- **No Retraining**: The artifact will not be retrained.
- **Classification**: All historical 1–6h continuous or 12h/24h mentions are formally classified as **SUPERSEDED** or **PLANNED (FUTURE SCOPE)**.

---

## 4. Feature Contract Reconciliation

Earlier exploratory notes referenced conceptual feature sets (e.g., 35 features, `pm25_t`, `pm25_t-1`, `rolling_mean`, `change_rate`).

### Reconciled Standard:
The authoritative feature contract is governed directly by `forecast_regressors_v1.joblib.feature_cols`:

```text
F4_FEATURE_COUNT = 36
SOURCE OF TRUTH = forecast_regressors_v1.joblib.feature_cols
```

### Exact 36-Feature Ordered Sequence:
1. `latitude` (Float64)
2. `longitude` (Float64)
3. `pm10` (Float64)
4. `no2` (Float64)
5. `so2` (Float64)
6. `co` (Float64)
7. `o3` (Float64)
8. `hour` (Float64)
9. `day_of_week` (Float64)
10. `is_weekend` (Float64)
11. `hour_sin` (Float64)
12. `hour_cos` (Float64)
13. `dow_sin` (Float64)
14. `dow_cos` (Float64)
15. `temperature` (Float64)
16. `humidity` (Float64)
17. `wind_speed` (Float64)
18. `wind_direction` (Float64)
19. `wind_u` (Float64)
20. `wind_v` (Float64)
21. `rainfall` (Float64)
22. `pressure` (Float64)
23. `pm25_spatial_lag_mean` (Float64)
24. `nearest_station_distance_km` (Float64)
25. `stations_within_5km_count` (Float64)
26. `monitoring_coverage_gap_flag` (Float64)
27. `dist_to_nearest_industrial_km` (Float64)
28. `dist_to_nearest_major_road_km` (Float64)
29. `sensitive_receptors_count_2km` (Float64)
30. `industrial_zone_within_2km_flag` (Float64)
31. `fire_count_24h_25km` (Float64)
32. `fire_frp_sum_24h_25km` (Float64)
33. `fire_frp_mean_24h_25km` (Float64)
34. `nearest_fire_distance_km` (Float64)
35. `fire_frp_distance_decay` (Float64)
36. `fire_upwind_alignment_score` (Float64)

*Rule*: No features may be renamed, reordered, added, or removed.

---

## 5. PM2.5 Feature Clarification

To ensure zero ambiguity during feature builder construction in Phase F4-P2:

1. **`pm25_spatial_lag_mean` is an actual trained feature** (Index 22). It represents the spatial leave-one-out mean of PM2.5 concentrations in neighboring H3 cells.
2. **`pm25_t`, `pm25_t-1`, `pm25_t-2`, `rolling_mean`, `rolling_max`, `change_rate` are NOT model inputs**. They were early conceptual ideas and do not exist in `feature_cols`.
3. **Raw `pm25` is excluded**. The model training pipeline deliberately excluded raw `pm25` to prevent target leakage during joint training.
4. **Implementation Mandate**: Phase F4-P2 feature engineering must populate `pm25_spatial_lag_mean` and co-pollutants (`pm10`, `no2`, etc.), but must **not** attempt to compute or append unsupported temporal lag features.

---

## 6. F3 vs F4 Contract Boundary

Even though F3 hotspot detection and F4 forecasting currently share the 36-feature schema (`f3-features-v1`):

> **Core Rule**: Shared feature schema similarity does NOT mean shared implementation.

- **F3 Hotspot Subsystem**:
  - Classifies acute spatial risk $P(\text{hotspot}=1)$ using Platt-calibrated Random Forest.
  - Applies operational threshold $0.20$.
  - Generates classification certainty and epistemic uncertainty flags.
  - Schema: `hotspot_predictions`.
- **F4 Forecasting Subsystem**:
  - Predicts continuous PM2.5 concentrations ($\mu\text{g/m}^3$) at $+1\text{h}, +3\text{h}, +6\text{h}$ using 3 independent regression trees.
  - Generates asymmetric empirical error bounds ($P10$ to $P90$) with physical clamping ($\ge 0.0\,\mu\text{g/m}^3$).
  - Schema: `forecasts`.
- **F4 Implementation Requirement**: F4 must use a **dedicated forecast feature builder and adapter** in Phase F4-P2. It must **not** import or call F3 classification routines.

---

## 7. Forecast Confidence Clarification

The actual trained forecast artifact does **not** output a probabilistic confidence score.

### Prohibitions:
- **DO NOT** output synthetic confidence scores (e.g., `"forecastConfidence": 0.74`).
- **DO NOT** inherit or copy F3 hotspot confidence into forecast records.
- **DO NOT** construct arbitrary percentage metrics from model leaf variance without empirical calibration.

### Standardized Contract:
- In API responses: `forecastConfidence: null`.
- In database schemas: `confidence` column made nullable.
- In UI components: Display empirical prediction bounds ($\text{lowerBound} - \text{upperBound}$) rather than a misleading scalar confidence percentage.

---

## 8. Legacy Documentation Findings

| Document | Legacy / Superseded Content | Authoritative Resolution | Classification |
|:---|:---|:---|:---:|
| [`docs/API_CONTRACT.md`](file:///c:/Users/lenovo/AeroSential/docs/API_CONTRACT.md) | `GET /api/v1/forecast/{h3Index}` rolling 1–6h with `confidence: 0.92` | Updated to discrete $T+1\text{h}, T+3\text{h}, T+6\text{h}$, `forecastConfidence: null` | **SUPERSEDED & UPDATED** |
| [`docs/ML_ARCHITECTURE.md`](file:///c:/Users/lenovo/AeroSential/docs/ML_ARCHITECTURE.md) | Autoregressive XGBoost 1–6h with temporal lags ($t-1, t-3$) | Annotated with multi-horizon RF ($1\text{h}, 3\text{h}, 6\text{h}$) and 36-feature artifact | **SUPERSEDED & ANNOTATED** |
| [`docs/MEMBER3_F3_REPORT.md`](file:///c:/Users/lenovo/AeroSential/docs/MEMBER3_F3_REPORT.md) | Sec 22–24: 1–6h, `pm25_t`, `rolling_mean`; Sec 27: `confidence: 0.74` | Annotated with contract reconciliation notices pointing to V1 spec | **SUPERSEDED & ANNOTATED** |
| [`docs/PRD.md`](file:///c:/Users/lenovo/AeroSential/docs/PRD.md) | FR-3: "Rolling 1 to 6-hour PM2.5 concentration forecasts" | Preserved as high-level product intent; clarified as discrete 1h, 3h, 6h in V1 spec | **HISTORICAL INTENT** |
| [`docs/DATABASE_SCHEMA.md`](file:///c:/Users/lenovo/AeroSential/docs/DATABASE_SCHEMA.md) | `forecasts` table has `confidence DOUBLE PRECISION NOT NULL` | Target Flyway migration specified in V1 spec making `confidence` nullable | **TARGET MIGRATION DEFINED** |
| [`frontend/src/pages/public/Forecast.tsx`](file:///c:/Users/lenovo/AeroSential/frontend/src/pages/public/Forecast.tsx) | Hardcoded 6-hour array with synthetic confidence values | Scheduled for modernization in Phase F4-P5 (UI) | **LEGACY MOCK** |

---

## 9. Authoritative Current F4 Contract

The canonical specification is published in:  
**[`docs/F4_FORECAST_SPECIFICATION_V1.md`](file:///c:/Users/lenovo/AeroSential/docs/F4_FORECAST_SPECIFICATION_V1.md)**

### Canonical Draft Output Structure:

```json
{
  "modelVersion": "forecast_regressors_v1",
  "generatedAt": "2026-09-27T12:00:00Z",
  "h3Index": "886196944dfffff",
  "parentPredictionId": "550e8400-e29b-41d4-a716-446655440000",
  "featureSnapshotId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "status": "SUCCESS",
  "forecasts": [
    {
      "horizonHours": 1,
      "targetTime": "2026-09-27T13:00:00Z",
      "predictedPm25": 42.15,
      "lowerBound": 40.31,
      "upperBound": 44.01,
      "unit": "ug/m3"
    },
    {
      "horizonHours": 3,
      "targetTime": "2026-09-27T15:00:00Z",
      "predictedPm25": 48.70,
      "lowerBound": 44.80,
      "upperBound": 51.75,
      "unit": "ug/m3"
    },
    {
      "horizonHours": 6,
      "targetTime": "2026-09-27T18:00:00Z",
      "predictedPm25": 54.30,
      "lowerBound": 48.78,
      "upperBound": 59.72,
      "unit": "ug/m3"
    }
  ],
  "forecastConfidence": null
}
```

*(Note: Numeric values above are illustrative placeholders; runtime values are computed deterministically by the regressor).*

---

## 10. Files Updated

1. **[`docs/F4_FORECAST_SPECIFICATION_V1.md`](file:///c:/Users/lenovo/AeroSential/docs/F4_FORECAST_SPECIFICATION_V1.md)**: Created as the canonical, authoritative specification for Feature 4.
2. **[`docs/API_CONTRACT.md`](file:///c:/Users/lenovo/AeroSential/docs/API_CONTRACT.md)**: Updated `GET /api/v1/forecast/{h3Index}` to discrete horizons $1\text{h}, 3\text{h}, 6\text{h}$ with `forecastConfidence: null`.
3. **[`docs/ML_ARCHITECTURE.md`](file:///c:/Users/lenovo/AeroSential/docs/ML_ARCHITECTURE.md)**: Annotated forecaster and feature engineering sections marking conceptual XGBoost / continuous 1-6h notes as superseded.
4. **[`docs/MEMBER3_F3_REPORT.md`](file:///c:/Users/lenovo/AeroSential/docs/MEMBER3_F3_REPORT.md)**: Annotated sections 22, 23, 24, 27 with reconciliation notices linking to the V1 specification.

---

## 11. Files Not Modified

- All F3 production and test code (`backend/.../hotspot/**`, `ai-service/ml/inference/predict_cli.py`, etc.).
- Machine learning artifact [`forecast_regressors_v1.joblib`](file:///c:/Users/lenovo/AeroSential/ai-service/models/artifacts/forecast_regressors_v1.joblib) (untouched, no retraining).
- Database migrations and entities (untouched in this phase).
- Frontend application code (untouched in this phase).

---

## 12. Final P2 Readiness

All conceptual ambiguities regarding horizons, features, PM2.5 feature roles, adapter separation, and confidence representation have been resolved and locked.

**Phase F4-P2 can begin safely when authorized by the user.**

---

## Machine-Readable Result

```json
F4_P1_1_RECONCILIATION = {
  "artifactPreserved": "PASS",
  "horizonsLockedTo": [1, 3, 6],
  "featureCountLockedTo": 36,
  "featureOrderSource": "artifact.feature_cols",
  "legacyHorizonMismatchResolved": "PASS",
  "legacyFeatureMismatchResolved": "PASS",
  "forecastConfidenceClarified": "PASS",
  "f3Untouched": "PASS",
  "contractStatus": "LOCKED_FOR_P2"
}
```
