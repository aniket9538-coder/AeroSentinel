# AeroSentinel — F4-P3 Forecast Model Inference Report

## 1. Executive Summary

Phase **F4-P3 (Forecast Model Inference)** has established, hardened, and verified the standalone model inference layer for multi-horizon PM2.5 forecasting ($T+1\text{h}, T+3\text{h}, T+6\text{h}$).

In accordance with strict phase directives:
1. **F3 Hotspot Detection remained 100% LOCKED and UNTOUCHED.**
2. **Forecast Model Retraining and Artifact Alteration were strictly forbidden.** The existing artifact (`ai-service/models/artifacts/forecast_regressors_v1.joblib`) was loaded programmatically and treated as immutable.
3. **No Database Writes or Spring Boot Services Were Created.** Persistence and REST endpoints belong to subsequent phases (F4-P4/P5).
4. **Authoritative Horizons $[1, 3, 6]$ Only.** No interpolation, extrapolation, or synthesis of unsupported horizons ($2\text{h}, 4\text{h}, 5\text{h}, 12\text{h}, 24\text{h}$) was performed.
5. **Empirical Residual Quantiles ($P_{10}/P_{90}$) Were Read from Artifact Metadata.** Physical lower-bound clamping ($\max(0.0, \text{pred} + P_{10})$) was applied to guarantee valid PM2.5 ranges.
6. **Forecast Confidence is Strictly `null`.** The RandomForest regressors do not produce probabilistic calibrated certainty; no synthetic scores or F3 hotspot confidence values were fabricated.
7. **Real DB-Derived Runtime Proof.** Real Pune CAAQMS data (H3 cell `88608850e5fffff`) was successfully evaluated through the standalone engine and CLI bridge.

---

## 2. P2 Handoff Contract

Phase F4-P3 directly consumed the validated feature layer contract from **F4-P2**:
- Input: $\mathbf{x} \in \mathbb{R}^{36}$ (shape `(1, 36)`).
- Column Order: Exactly matches `artifact['feature_cols']`.
- Lineage Context: `parentPredictionId`, `cityId`, `h3Index`, `featureSnapshotId`, `predictedAt / T0`.
- Immutability: Lineage metadata was preserved for downstream explainability and tracing, never appended to the 36-dimensional feature vector.

---

## 3. Artifact Loading

The dedicated loader (`load_forecast_artifact()` in [`ai-service/ml/forecast/engine.py`](file:///c:/Users/lenovo/AeroSential/ai-service/ml/forecast/engine.py)) loads `forecast_regressors_v1.joblib` with caching and structural validation:
- **Top-Level Keys Verified**: `models`, `feature_cols`, `residuals`, `benchmark_metrics`, `algorithm`.
- **Models Dictionary**: Verified independent `RandomForestRegressor` models keyed by `1`, `3`, `6` (and aliases `t_plus_1`, `t_plus_3`, `t_plus_6`).
- **Feature Schema**: Exactly 36 features matching `ORDERED_FEATURE_NAMES`.
- **Residual Quantiles**: Verified $P_{10}$ and $P_{90}$ keys for all three horizons.
- **Algorithm**: `RandomForestRegressor(n_estimators=100, max_depth=14)`.

---

## 4. Model Compatibility Validation

Before invoking `model.predict()`, the engine enforces:
1. Shape must be exactly `(1, 36)`.
2. Feature names and column ordering must match `artifact['feature_cols']`.
3. All feature values must be numeric (`float64`).
4. Zero NaNs allowed.
5. Zero Infinities allowed.
6. Exactly one row (single-observation inference batch).
7. Requested horizons must belong strictly to $\{1, 3, 6\}$.

Any mismatch fails immediately with structured error `INVALID_INPUT` or `FORECAST_MODEL_CONTRACT_INVALID`.

---

## 5. Multi-Horizon Inference

For each supported horizon $h \in [1, 3, 6]$, the engine evaluates the independent trained regressor:
$$\hat{y}_{T+1} = \text{regressor}_1(\mathbf{x})$$
$$\hat{y}_{T+3} = \text{regressor}_3(\mathbf{x})$$
$$\hat{y}_{T+6} = \text{regressor}_6(\mathbf{x})$$
- Zero synthetic intermediate steps ($T+2\text{h}, T+4\text{h}, T+5\text{h}$) are produced.
- Zero future steps beyond $T+6\text{h}$ are allowed.

---

## 6. Prediction Validation

For each horizon output:
- Verified numeric and finite.
- Units: $\mu\text{g}/\text{m}^3$.
- Rounded to 2 decimal places for self-consistent presentation and interval derivation.

---

## 7. Empirical Interval Generation

Residual quantiles derived from training validation holdout partitions were loaded directly from the artifact metadata:

| Horizon | Audited $P_{10}$ Residual | Audited $P_{90}$ Residual | Lower Bound Formula | Upper Bound Formula |
|---|---|---|---|---|
| **$T+1\text{h}$** | $-1.8424755$ | $+1.8628680$ | $\max(0.0, \hat{y} + P_{10})$ | $\hat{y} + P_{90}$ |
| **$T+3\text{h}$** | $-3.9013645$ | $+3.0473582$ | $\max(0.0, \hat{y} + P_{10})$ | $\hat{y} + P_{90}$ |
| **$T+6\text{h}$** | $-5.5207627$ | $+5.4248675$ | $\max(0.0, \hat{y} + P_{10})$ | $\hat{y} + P_{90}$ |

- **Physical Lower-Bound Clamping**: PM2.5 cannot be negative. The lower bound is bounded by $0.0\,\mu\text{g}/\text{m}^3$.
- **Interval Consistency Verified**: $\text{lowerBound} \le \text{predictedPm25} \le \text{upperBound}$ holds strictly across all horizons.

---

## 8. Forecast Confidence Boundary

- **Contract Rule**: `forecastConfidence = null`.
- The trained RandomForest regressors do not output calibrated probabilities.
- Reusing F3 hotspot classification confidence or fabricating confidence from residual variance is strictly prohibited.
- Downstream clients receive clear separation between deterministic predictions with empirical intervals vs probabilistic classification confidence.

---

## 9. Timestamp Generation

For each horizon $h \in [1, 3, 6]$:
$$\text{targetTime} = T_0 + h\text{ hours}$$
- Calculated strictly from the authoritative forecast base timestamp $T_0$.
- Server clock / execution time is never used for target generation.
- Formatted as ISO-8601 UTC string with explicit `+00:00` offset.

---

## 10. Inference Output Contract

```json
{
  "modelVersion": "forecast_regressors_v1",
  "generatedAt": "2026-09-27T08:27:59.295190+00:00",
  "h3Index": "88608850e5fffff",
  "cityId": "550e8400-e29b-41d4-a716-446655440001",
  "parentPredictionId": "a310c689-f340-49fc-8935-a037de8d7709",
  "featureSnapshotId": "007f7904-cee8-465c-a960-6c78bfa7c3c8",
  "status": "SUCCESS",
  "forecasts": [
    {
      "horizonHours": 1,
      "targetTime": "2026-09-27T09:04:54.689450+00:00",
      "predictedPm25": 71.90,
      "lowerBound": 70.06,
      "upperBound": 73.76,
      "unit": "ug/m3"
    },
    {
      "horizonHours": 3,
      "targetTime": "2026-09-27T11:04:54.689450+00:00",
      "predictedPm25": 70.43,
      "lowerBound": 66.53,
      "upperBound": 73.48,
      "unit": "ug/m3"
    },
    {
      "horizonHours": 6,
      "targetTime": "2026-09-27T14:04:54.689450+00:00",
      "predictedPm25": 70.55,
      "lowerBound": 65.03,
      "upperBound": 75.98,
      "unit": "ug/m3"
    }
  ],
  "forecastConfidence": null
}
```

---

## 11. CLI Bridge

Created [`ai-service/ml/inference/predict_forecast_cli.py`](file:///c:/Users/lenovo/AeroSential/ai-service/ml/inference/predict_forecast_cli.py):
- Supports input via command-line argument or STDIN stream pipe.
- Outputs strict JSON on STDOUT.
- Controlled exit codes:
  - `0`: SUCCESS
  - `1`: Malformed / empty input JSON
  - `2`: `FORECAST_MODEL_UNAVAILABLE`
  - `3`: `INVALID_INPUT` / `FORECAST_MODEL_CONTRACT_INVALID`
  - `4`: `MODEL_INFERENCE_FAILED`
- Zero database connections, zero Spring Boot dependencies, zero React coupling.

---

## 12. Error Handling & Controlled Failures

Tested and confirmed controlled failures for:
1. Missing artifact file $\to$ `FORECAST_MODEL_UNAVAILABLE` (Exit code 2).
2. Malformed / missing keys $\to$ `FORECAST_MODEL_CONTRACT_INVALID` (Exit code 3).
3. NaN in input features $\to$ `INVALID_INPUT` with specific feature name.
4. Infinite value in features $\to$ `INVALID_INPUT` with specific feature name.
5. Missing or empty `h3Index` $\to$ `INVALID_INPUT`.
6. Empty or non-JSON input $\to$ Structured error on STDERR (Exit code 1).

---

## 13. Determinism Test

Repeated evaluation with identical inputs produced bit-for-bit identical forecast records:
- `predictedPm25`: Identical across runs ($71.90, 70.43, 70.55$).
- `lowerBound`: Identical across runs ($70.06, 66.53, 65.03$).
- `upperBound`: Identical across runs ($73.76, 73.48, 75.98$).
- `targetTime`: Identical across runs.
- `generatedAt`: Tracks execution time strictly as metadata.

---

## 14. Automated Test Results

### 1. F4-P3 Inference Test Suite ([`test_f4_p3_inference.py`](file:///c:/Users/lenovo/AeroSential/ai-service/tests/test_f4_p3_inference.py))
- `test_artifact_loading_and_keys`: **PASS**
- `test_multi_horizon_inference_execution`: **PASS**
- `test_empirical_residual_intervals_and_clamping`: **PASS**
- `test_forecast_confidence_is_strictly_null`: **PASS**
- `test_timestamp_target_generation`: **PASS**
- `test_rejection_of_nan_in_features`: **PASS**
- `test_rejection_of_infinity_in_features`: **PASS**
- `test_rejection_of_missing_h3_index`: **PASS**
- `test_failure_on_missing_artifact`: **PASS**
- `test_lineage_context_preservation`: **PASS**
- `test_inference_determinism`: **PASS**
- `test_cli_execution_with_argument`: **PASS**
- `test_cli_rejection_of_empty_input`: **PASS**
- **Result**: `13 passed, 0 failed` (100%).

### 2. Combined F4 Python Suite
- `test_f4_feature_layer.py` (11 tests) + `test_f4_p3_inference.py` (13 tests):
- **Total**: `24 passed, 0 failed` (100%).

### 3. F3 Regression Suite
- `MLHotspotDetectionEngineTest` & `HotspotIntegrationTest`:
- **Total**: `14 passed, 0 failed` (`BUILD SUCCESS`).

---

## 15. Real Runtime Inference Proof

```text
==================================================
P3_RUNTIME_PROOF (REAL DB-DERIVED PUNE VECTOR)
==================================================
context:
    cityId             = 550e8400-e29b-41d4-a716-446655440001
    h3Index            = 88608850e5fffff
    parentPredictionId = a310c689-f340-49fc-8935-a037de8d7709
    featureSnapshotId  = 007f7904-cee8-465c-a960-6c78bfa7c3c8
    T0                 = 2026-09-27T08:04:54.689450Z

model:
    version            = forecast_regressors_v1
    algorithm          = RandomForestRegressor(n_estimators=100, max_depth=14)

forecast:
    T+1h (2026-09-27T09:04:54.689450+00:00):
        predictedPm25  = 71.90 ug/m3
        lowerBound     = 70.06 ug/m3
        upperBound     = 73.76 ug/m3

    T+3h (2026-09-27T11:04:54.689450+00:00):
        predictedPm25  = 70.43 ug/m3
        lowerBound     = 66.53 ug/m3
        upperBound     = 73.48 ug/m3

    T+6h (2026-09-27T14:04:54.689450+00:00):
        predictedPm25  = 70.55 ug/m3
        lowerBound     = 65.03 ug/m3
        upperBound     = 75.98 ug/m3

forecastConfidence     = null
databaseWritesExecuted = false
==================================================
```

---

## 16. Files Created

1. `ai-service/ml/forecast/engine.py` (ForecastInferenceEngine and artifact loader)
2. `ai-service/ml/inference/predict_forecast_cli.py` (Standalone forecast CLI bridge)
3. `ai-service/tests/test_f4_p3_inference.py` (13 automated inference tests)
4. `AeroSentinel_F4_P3_Forecast_Model_Inference_Report.md` (Authoritative report)

---

## 17. Files Modified

### 1. `ai-service/ml/forecast/features/builder.py`

This was a **compatibility-only parsing hardening change** to normalize equivalent incoming payload structures at the serialization boundary:

```python
# Lines 58-60 in ForecastFeatureBuilder:
if feature_map is None and isinstance(ordered_list, dict):
    feature_map = ordered_list
    ordered_list = None
```

#### Boundary & Contract Invariant Guarantees:
- **Accepts equivalent dictionary/list feature representations**: Callers can supply feature collections via `features: [...]` (36-element list), `features: {...}` (36-key dict), or `featureMap: {...}` (36-key dict).
- **Does NOT change feature values**: Raw scalar float values are passed through unmodified.
- **Does NOT change feature calculations**: Mathematical transformations, aggregations, and spatial queries in upstream feature engineering are untouched.
- **Does NOT change feature ordering**: All extracted features are strictly mapped to `ORDERED_FEATURE_NAMES` indices $[0..35]$.
- **Does NOT change units**: All units ($\mu\text{g}/\text{m}^3, ^\circ\text{C}, \%, \text{m}/\text{s}, \text{MW}$) remain identical.
- **Does NOT change missingness semantics**: Provenance classification (`VALID_OBSERVATION`, `REAL_ZERO`, `IMPUTED_BASELINE`, `MISSING`) and quality statuses (`VALID`, `MISSING`, `UNAVAILABLE`) are preserved identically.
- **Does NOT change the 36-feature contract**: Exactly 36 features are produced; no features were added, removed, or renamed.
- **Does NOT change P2 production data sources**: Upstream CAAQMS observation tables, ERA5 meteorology, and NASA FIRMS feeds are untouched.

#### Regression Evidence:
Evaluating identical feature sets across the three input representations (`payload_list`, `payload_dict`, `payload_map`) through `ForecastFeatureBuilder` $\to$ `ForecastFeatureAdapter` yields:
```text
x_list shape: (1, 36)
x_dict shape: (1, 36)
x_map shape:  (1, 36)
max_abs_diff(x_list, x_dict): 0.0
max_abs_diff(x_dict, x_map):  0.0
All arrays bit-for-bit identical: True
```

#### Formal Classification:
- **`P2 CONTRACT SEMANTICS = UNCHANGED`**
- **`P2 FEATURE LOGIC = UNCHANGED`**
- **`P3 COMPATIBILITY PATCH = SAFE`**

---

## 18. Files Not Modified

- All F3 hotspot classes (locked)
- `backend/src/main/java/com/aerosentinel/hotspot/*` (locked)
- `backend/src/main/java/com/aerosentinel/forecast/*` (reserved for P4/P5)
- `ai-service/models/artifacts/forecast_regressors_v1.joblib` (unmodified)
- All frontend files (reserved for P6)

---

## 19. Known Limitations

1. **Horizon Support**: Models exist strictly for horizons 1, 3, and 6. Other horizons ($2\text{h}, 4\text{h}, 5\text{h}$) cannot be evaluated without retraining.
2. **Empirical Quantiles**: Residual intervals are symmetric-empirical based on validation holdouts; they do not vary dynamically with input variance.
3. **PMR Geography**: The model was trained specifically on the Pune Metropolitan Region environmental profile.

---

## 20. P4 Handoff Contract

Phase F4-P3 hands off to Phase F4-P4:

```
[ForecastFeatureAdapter]
         ↓
ModelReadyFeatureVector (1, 36)
         ↓
[ForecastInferenceEngine] / [predict_forecast_cli.py]
         ↓
ForecastInferenceResult:
  ├── modelVersion: "forecast_regressors_v1"
  ├── parentPredictionId, cityId, h3Index, featureSnapshotId
  ├── forecasts: [
  │     { horizonHours: 1, targetTime: "...", predictedPm25: 71.90, lowerBound: 70.06, upperBound: 73.76, unit: "ug/m3" },
  │     { horizonHours: 3, targetTime: "...", predictedPm25: 70.43, lowerBound: 66.53, upperBound: 73.48, unit: "ug/m3" },
  │     { horizonHours: 6, targetTime: "...", predictedPm25: 70.55, lowerBound: 65.03, upperBound: 75.98, unit: "ug/m3" }
  │   ]
  └── forecastConfidence: null
         ↓
─── HANDOFF TO PHASE F4-P4 (BACKEND PERSISTENCE & API BRIDGE) ───
```

Phase P4 will own:
- Java process bridge invoking `predict_forecast_cli.py`.
- Spring Boot Forecast Service & Controller.
- Forecast table persistence.

Phase P3 stops here.

---

## 21. Final Machine-Readable Summary

```json
F4_P3_SUMMARY = {
  "artifactLoad": "PASS",
  "artifactCompatibility": "PASS",
  "modelHorizon1": "PASS",
  "modelHorizon3": "PASS",
  "modelHorizon6": "PASS",
  "intervalGeneration": "PASS",
  "lowerBoundClamping": "PASS",
  "forecastConfidenceNull": "PASS",
  "timestampGeneration": "PASS",
  "outputContract": "PASS",
  "errorHandling": "PASS",
  "determinism": "PASS",
  "cli": "PASS",
  "runtimeProof": "PASS",
  "tests": "PASS",
  "f3Untouched": "PASS",
  "artifactUntouched": "PASS",
  "status": "COMPLETE"
}
```

---

## 22. Final P3 Decision

**PHASE F4-P3 IS COMPLETE.** Model inference execution, empirical interval generation, physical clamping, CLI bridging, and determinism tests are fully verified. All stop conditions respected.
