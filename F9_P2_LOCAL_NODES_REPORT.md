# F9-P2 IMPLEMENTATION REPORT: LOCAL CITY ML NODES & COMMON MODEL ENGINE

**Feature:** F9 — Federated City Network (Pune + Mumbai + Delhi)
**Phase:** P2 — Local City ML Nodes & Common Model Engine
**Date:** 2026-09-30
**Status:** **PASS** ✅
**Repository:** `aniket9538-coder/AeroSentinel`
**Workspace:** `c:\Users\Harsh\OneDrive\Documents\Python Projects\AeroSentinel`

---

## 1. Executive Summary

Phase **F9-P2** delivers the local machine learning modeling layer and municipal node client pipelines under `federated/`.

In this phase:
- Implemented standard model abstractions in `federated/models/local_model.py` (`LocalHotspotModel`) and `federated/models/global_model.py` (`GlobalHotspotModel`), enforcing strict compatibility with the 36-feature schema (`f3-features-v1`).
- Implemented local data adapters for Pune, Mumbai, and Delhi that generate isolated municipal datasets reflecting city-specific environmental distributions (urban traffic peaks, coastal marine dispersion, severe winter particulate spikes).
- Implemented municipal trainers and client coordinators (`PuneClient`, `MumbaiClient`, `DelhiClient`) inheriting from clean base classes in `federated/clients/base_client.py`.
- Formatted and serialized model update packages containing parameter weights ($\mathbf{W} \in \mathbb{R}^{36}$, $b \in \mathbb{R}$), sample counts, and evaluation metrics (MAE, RMSE, ROC-AUC, Brier score), with **zero raw telemetry leakage**.
- Validated all 8 required test cases in `federated/tests/test_local_nodes.py` with 100% pass rate.
- Preserved complete isolation of protected features F1 through F8 with **zero modifications to Spring Boot, React, or database schemas**.

---

## 2. Files Inspected & Implemented

| File | Type | Action | Description |
|---|---|---|---|
| `federated/models/local_model.py` | Production | **Implemented** | `LocalHotspotModel` wrapping `StandardScaler` + `LogisticRegression` over 36 features; provides `fit()`, `evaluate()`, `get_weights()`, `set_weights()`, `predict_proba()`, `save()`, `load()`. |
| `federated/models/global_model.py` | Production | **Implemented** | `GlobalHotspotModel` maintaining consensus parameters, version metadata, inference, and serialization. |
| `federated/clients/base_client.py` | Production | **Created** | `BaseCityTrainer` and `BaseFederatedClient` providing DRY architecture for municipal nodes. |
| `federated/clients/pune/local_data.py` | Production | **Implemented** | `PuneLocalDataLoader` (~1,200 records, traffic + localized residential burning pattern). |
| `federated/clients/pune/trainer.py` | Production | **Implemented** | `PuneTrainer` executing local train/val split and metrics evaluation. |
| `federated/clients/pune/client.py` | Production | **Implemented** | `PuneClient` coordinating round lifecycle and update packaging. |
| `federated/clients/mumbai/local_data.py` | Production | **Implemented** | `MumbaiLocalDataLoader` (~800 records, coastal humidity + marine wind dispersion pattern). |
| `federated/clients/mumbai/trainer.py` | Production | **Implemented** | `MumbaiTrainer` executing coastal local training. |
| `federated/clients/mumbai/client.py` | Production | **Implemented** | `MumbaiClient` coordinating round lifecycle and update packaging. |
| `federated/clients/delhi/local_data.py` | Production | **Implemented** | `DelhiLocalDataLoader` (~1,000 records, winter stagnation + agricultural fire smoke alignment pattern). |
| `federated/clients/delhi/trainer.py` | Production | **Implemented** | `DelhiTrainer` executing regional local training. |
| `federated/clients/delhi/client.py` | Production | **Implemented** | `DelhiClient` coordinating round lifecycle and update packaging. |
| `federated/tests/test_local_nodes.py` | Test | **Created** | Comprehensive pytest suite covering all 8 required cases. |
| `federated/run_local_nodes_demo.py` | Verification | **Created** | Automated runtime script executing local training across all 3 nodes and saving updates to `storage/models/updates/`. |

---

## 3. Common 36-Feature Schema Implementation Details

All three municipal nodes bind strictly to the locked `f3-features-v1` schema. The exact 36 ordered features implemented are:

```text
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

- **Target Definition:** `target_hotspot` $\in \{0, 1\}$.
- **Parity Guarantee:** Input DataFrames are validated against `ORDERED_FEATURE_NAMES` before any transformation; missing features raise an immediate `ValueError`.
- **Normalization:** Built-in `StandardScaler` standardizes each feature prior to logistic fitting, ensuring scale parity for FedAvg parameter averaging.

---

## 4. Municipal Node Local Data Profiles

Each city's data loader encapsulates localized microclimatic characteristics without cross-node data leakage:

### 1. Pune Municipal Node (`PUNE`)
- **City ID:** `550e8400-e29b-41d4-a716-446655440001`
- **Default Sample Count:** 1,200 records
- **Geographic Center:** $18.5204^\circ\text{ N}, 73.8567^\circ\text{ E}$
- **Profile:** Moderate baseline particulate levels (PM10: $78 \pm 22 \ \mu\text{g/m}^3$), elevated evening rush-hour peaks, moderate plateau temperatures ($28.5 \pm 4^\circ\text{C}$), humidity $56 \pm 12\%$, localized residential waste burning ($1-3$ fires).

### 2. Mumbai Coastal Node (`MUMBAI`)
- **City ID:** `550e8400-e29b-41d4-a716-446655440002`
- **Default Sample Count:** 800 records
- **Geographic Center:** $19.0760^\circ\text{ N}, 72.8777^\circ\text{ E}$
- **Profile:** Marine boundary layer, high relative humidity ($82 \pm 8\%$), strong sea-breeze dispersion (wind speed $5.4 \pm 1.8 \text{ m/s}$), prevailing westerly sea winds ($250 \pm 45^\circ$), very low biomass fire activity ($< 0.2$ fires).

### 3. Delhi Regional Node (`DELHI`)
- **City ID:** `550e8400-e29b-41d4-a716-446655440003`
- **Default Sample Count:** 1,000 records
- **Geographic Center:** $28.6139^\circ\text{ N}, 77.2090^\circ\text{ E}$
- **Profile:** Severe northern continental baseline (PM10: $165 \pm 55 \ \mu\text{g/m}^3$), thermal inversions with low stagnant wind ($1.9 \pm 0.8 \text{ m/s}$), heavy regional stubble burning ($4-6$ fires, FRP sum $120-450 \text{ MW}$), strong northwesterly upwind smoke alignment score ($0.65-0.95$).

---

## 5. Local Model Training & Evaluation Results

All three city nodes were trained locally on an $80/20$ train/validation split using `federated/run_local_nodes_demo.py`. The resulting evaluation metrics are:

| Metric | Pune Municipal Node (`pune-r1`) | Mumbai Coastal Node (`mumbai-r1`) | Delhi Regional Node (`delhi-r1`) |
|---|:---:|:---:|:---:|
| **Sample Count** | 1,200 | 800 | 1,000 |
| **Validation Samples** | 240 | 160 | 200 |
| **MAE** | 0.4312 | 0.4188 | 0.0944 |
| **RMSE** | 0.4849 | 0.4741 | 0.2665 |
| **ROC-AUC** | **0.6668** | **0.6916** | **0.8179** |
| **Brier Score** | 0.2351 | 0.2247 | 0.0710 |
| **Accuracy** | 0.6000 | 0.6125 | 0.9400 |
| **Weights Dimension** | 36 floats | 36 floats | 36 floats |
| **Intercept** | +0.204562 | +0.334057 | -2.628864 |
| **Artifact Reference** | `storage/models/updates/pune_round-001.json` | `storage/models/updates/mumbai_round-001.json` | `storage/models/updates/delhi_round-001.json` |

---

## 6. Sample Model Update Payload

Below is the verified, serialized JSON update package generated by the **Pune Municipal Node** (`storage/models/updates/pune_round-001.json`):

```json
{
  "nodeId": "PUNE",
  "roundId": "ROUND-001",
  "baseModelVersion": "global-v1",
  "localModelVersion": "pune-r1",
  "sampleCount": 1200,
  "metrics": {
    "mae": 0.4312,
    "rmse": 0.4849,
    "rocAuc": 0.6668,
    "brierScore": 0.2351,
    "accuracy": 0.6
  },
  "weights": [
    -0.079908, -0.116242, 0.864408, 0.359051, -0.08143, 0.086075,
    -0.182366, 0.050741, -0.058052, 0.071832, 0.103513, -0.015029,
    -0.003383, -0.070406, 0.042426, -0.018117, -0.349291, 0.026435,
    0.039521, 0.094676, -0.068369, -0.084506, 0.576741, -0.039735,
    0.121831, 0.135656, 0.113488, 0.044039, -0.035915, 0.399164,
    -0.227327, 0.163298, -0.06142, -0.419914, -0.257555, 0.07145
  ],
  "intercept": 0.204562,
  "artifactReference": "storage/models/updates/pune_round-001.json",
  "submittedAt": "2026-09-29T22:49:48.006512+00:00"
}
```

- **File Size:** 1,006 bytes ($< 1.0\text{ KB}$).
- **Parameter Check:** Exactly 36 float weights + 1 float intercept. Zero `NaN`, `null`, or infinite values.

---

## 7. Test Results (`test_local_nodes.py`)

The automated unit test suite executed cleanly with **8 out of 8 tests passing** in 4.57 seconds:

```text
============================= test session starts =============================
platform win32 -- Python 3.11.9, pytest-9.1.1, pluggy-1.6.0
rootdir: C:\Users\Harsh\OneDrive\Documents\Python Projects\AeroSentinel
plugins: anyio-4.15.1, asyncio-1.4.0

federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_01_feature_vector_dimension_and_ordering PASSED [ 12%]
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_02_pune_local_training PASSED [ 25%]
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_03_mumbai_local_training PASSED [ 37%]
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_04_delhi_local_training PASSED [ 50%]
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_05_parameter_update_packaging PASSED [ 62%]
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_06_global_model_parameter_synchronization PASSED [ 75%]
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_07_cross_city_contract_parity PASSED [ 87%]
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_08_no_raw_data_leakage PASSED [100%]

============================== 8 passed in 4.57s ==============================
```

### Summary of Validated Scenarios:
1. **Dimension & Ordering:** Verified 36 feature columns strictly match `f3-features-v1` for Pune, Mumbai, and Delhi.
2. **Local Model Fitting:** Verified convergence, 36 weights, and valid metric dictionaries for all three cities.
3. **Packaging Integrity:** Verified JSON serialization to disk with exact contract fields.
4. **Parameter Synchronization:** Verified that updating `LocalHotspotModel` with `GlobalHotspotModel` weights correctly shifts prediction probabilities in $[0.0, 1.0]$.
5. **Cross-City Parity (FedAvg Feasibility):** Verified that Pune, Mumbai, and Delhi update payloads have identical shapes and can be averaged using sample-weighted math without dimension or key errors.
6. **No Raw Data Leakage:** Confirmed update payloads contain zero telemetry rows, station coordinates, or observation arrays ($< 5\text{ KB}$ payload size constraint met).

---

## 8. Data Locality & Privacy Boundary Verification

1. **Zero Central Database Querying:**
   - City data loaders operate strictly within their respective municipal directories (`federated/clients/<city>/local_data.py`).
   - No SQL queries or connections to the central PostgreSQL database were used for local training.
2. **No Raw Telemetry in Updates:**
   - As proven in test `test_08_no_raw_data_leakage`, update payloads contain only numerical parameter weights, sample counts, and evaluation metrics.
3. **Stubs Retained:**
   - `federated/privacy/differential_privacy.py` and `federated/privacy/secure_aggregation.py` remain cleanly isolated extension points for future cryptographic hardening.

---

## 9. Protected Features F1–F8 Status (Zero Regressions)

In strict accordance with the prompt guidelines:
- **Zero changes** to Spring Boot backend code in `backend/`.
- **Zero changes** to React frontend code in `frontend/`.
- **Zero changes** to database schemas or Flyway migrations.
- **Zero changes** to production F1–F8 models in `ai-service/ml/`.
- All existing unit, integration, and operational capabilities across F1 through F8 remain 100% stable and intact.

---

## 10. Pass Criteria Checklist

| Criterion | Requirement | Result |
|---|---|:---:|
| **Common Model Abstractions** | `LocalHotspotModel` and `GlobalHotspotModel` implemented | **PASS** ✅ |
| **Data Adapters** | Pune, Mumbai, Delhi local data loaders generate 36-feature datasets | **PASS** ✅ |
| **Municipal Trainers** | Local trainers execute cleanly and compute valid evaluation metrics | **PASS** ✅ |
| **Update Payloads** | Exactly 36 weights + intercept with zero `NaN` values | **PASS** ✅ |
| **Data Isolation Mandate** | Raw city training data stays local at the node (zero central DB queries) | **PASS** ✅ |
| **Unit Test Coverage** | All 8 required test cases in `test_local_nodes.py` PASS | **PASS** ✅ |
| **Protection Guarantee** | Zero modifications to Spring Boot, React, or F1–F8 code | **PASS** ✅ |
| **Report Generation** | `F9_P2_LOCAL_NODES_REPORT.md` created with verified runtime evidence | **PASS** ✅ |

**FINAL STATUS: F9-P2: PASS ✅**

The codebase is fully prepared for **Phase F9-P3 (Federated Coordinator, Aggregator, Round Manager & Registry)**.
