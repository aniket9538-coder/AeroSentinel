# F9-P3 IMPLEMENTATION REPORT: FEDERATED COORDINATOR, AGGREGATOR, ROUND MANAGER & MODEL REGISTRY

**Feature:** F9 — Federated City Network (Pune + Mumbai + Delhi)
**Phase:** P3 — Coordinator, Aggregator, Round Manager & Model Registry
**Date:** 2026-09-30
**Status:** **PASS** ✅
**Repository:** `aniket9538-coder/AeroSentinel`
**Workspace:** `c:\Users\Harsh\OneDrive\Documents\Python Projects\AeroSentinel`

---

## 1. Executive Summary

Phase **F9-P3** delivers the centralized orchestration and consensus engine for AeroSentinel's Federated City Network. Operating under strict privacy and boundary constraints, the coordinator receives parameter updates ($\mathbf{W} \in \mathbb{R}^{36}, b \in \mathbb{R}$) and validation metrics from municipal nodes (Pune, Mumbai, Delhi) without centralizing raw citizen telemetry or air quality sensor readings.

In this phase, four core modules were implemented under `federated/coordinator/`:
1. **`aggregator.py`**: Mathematical implementation of Sample-Weighted Federated Averaging (FedAvg) over 36-feature weight vectors and weighted performance metric aggregation (MAE, RMSE, ROC-AUC, Brier score).
2. **`round_manager.py`**: Robust lifecycle state machine (`CREATED` $\rightarrow$ `MODEL_DISTRIBUTED` $\rightarrow$ `TRAINING` $\rightarrow$ `UPDATES_COLLECTING` $\rightarrow$ `AGGREGATING` $\rightarrow$ `COMPLETED` / `FAILED`) enforcing quorum policies ($\ge 2$ of $3$ nodes), stale update rejection, duplicate rejection, and weight dimension guards.
3. **`model_registry.py`**: Lineage tracking catalog managing global model versions (`global-v1` $\rightarrow$ `global-v2` $\rightarrow$ `global-v3`), active model pointers, and immutable joblib artifact persistence in `storage/models/global/`.
4. **`coordinator.py`**: High-level orchestration facade uniting round lifecycle, update intake, consensus calculation, and version publishing.

All 8 coordinator test cases in `federated/tests/test_coordinator.py` and all 8 node tests in `test_local_nodes.py` pass (`16 passed in 3.91s`). A multi-round simulation verified the end-to-end evolution from `global-v1` to `global-v2` (3 nodes) and `global-v3` (2-node quorum).

---

## 2. Files Inspected & Implemented

### 2.1 Implemented Coordinator Modules
| File | Role | Description |
|---|---|---|
| [`federated/coordinator/aggregator.py`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/federated/coordinator/aggregator.py) | Mathematical Aggregator | Implements sample-weighted FedAvg consensus across $K$ municipal parameter updates, validating weights $\in \mathbb{R}^{36}$, non-empty samples, and metric aggregations. |
| [`federated/coordinator/round_manager.py`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/federated/coordinator/round_manager.py) | Lifecycle State Machine | Manages training rounds, tracks participating nodes, checks quorum ($\ge 2$), and rejects stale base models, duplicates, and malformed payloads. |
| [`federated/coordinator/model_registry.py`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/federated/coordinator/model_registry.py) | Model Catalog & Lineage | Tracks global model lineage, active version pointer, and serializes versioned consensus artifacts (`global-vX.joblib`) to `storage/models/global/`. |
| [`federated/coordinator/coordinator.py`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/federated/coordinator/coordinator.py) | Central Orchestration Facade | Provides clean API (`start_round`, `submit_node_update`, `trigger_aggregation`, `get_active_global_model`, `get_round_status`). |
| [`federated/coordinator/__init__.py`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/federated/coordinator/__init__.py) | Package Exports | Exposes `FederatedCoordinator`, `FederatedAggregator`, `RoundManager`, `ModelRegistry`, `RoundState`. |

### 2.2 Testing & Verification Scripts
| File | Role | Description |
|---|---|---|
| [`federated/tests/test_coordinator.py`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/federated/tests/test_coordinator.py) | Coordinator Test Suite | 8 comprehensive tests verifying full round simulation, quorum thresholds, stale rejections, duplicate rejections, dimension checks, lineage, and math. |
| [`federated/run_round_simulation.py`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/federated/run_round_simulation.py) | Runtime Multi-Round Runner | Automated multi-round simulation advancing `global-v1` $\rightarrow$ `global-v2` $\rightarrow$ `global-v3` with verified disk persistence. |

---

## 3. Aggregator Mathematical Formulation & Verification

### 3.1 Mathematical Specification
Let $K$ be the set of valid municipal node updates received in round $r$.
Let $n_k$ be the local dataset sample count for node $k \in K$, with total sample count $N = \sum_{k \in K} n_k$.

#### 1. Parameter Aggregation (FedAvg)
The global consensus parameter vector $\mathbf{W}_{\text{global}} \in \mathbb{R}^{36}$ and intercept $b_{\text{global}} \in \mathbb{R}$ are computed as:
$$\mathbf{W}_{\text{global}} = \sum_{k \in K} \left( \frac{n_k}{N} \right) \mathbf{W}_k$$
$$b_{\text{global}} = \sum_{k \in K} \left( \frac{n_k}{N} \right) b_k$$

#### 2. Weighted Performance Metric Aggregation
Global generalization performance across municipal jurisdictions is evaluated using sample-weighted metric combination:
$$\text{MAE}_{\text{global}} = \sum_{k \in K} \left( \frac{n_k}{N} \right) \text{MAE}_k$$
$$\text{RMSE}_{\text{global}} = \sqrt{\sum_{k \in K} \left( \frac{n_k}{N} \right) (\text{RMSE}_k)^2}$$
$$\text{ROC-AUC}_{\text{global}} = \sum_{k \in K} \left( \frac{n_k}{N} \right) \text{ROC-AUC}_k$$
$$\text{Brier}_{\text{global}} = \sum_{k \in K} \left( \frac{n_k}{N} \right) \text{Brier}_k$$

### 3.2 Numerical Verification Example (`test_08_mathematical_fedavg_correctness`)
- **Node A:** $n_A = 100$, $W_A = [1.0] \times 36$, $b_A = 0.5$, $\text{MAE} = 0.20$, $\text{RMSE} = 0.30$
- **Node B:** $n_B = 300$, $W_B = [3.0] \times 36$, $b_B = 1.5$, $\text{MAE} = 0.40$, $\text{RMSE} = 0.50$
- **Total Samples:** $N = 400$, Weights: $\frac{100}{400} = 0.25$, $\frac{300}{400} = 0.75$
- **Expected Weights:** $0.25 \times 1.0 + 0.75 \times 3.0 = 2.50$ (Exact match)
- **Expected Intercept:** $0.25 \times 0.5 + 0.75 \times 1.5 = 1.25$ (Exact match)
- **Expected MAE:** $0.25 \times 0.20 + 0.75 \times 0.40 = 0.35$ (Exact match)
- **Expected RMSE:** $\sqrt{0.25 \times 0.09 + 0.75 \times 0.25} = \sqrt{0.0225 + 0.1875} = \sqrt{0.21} \approx 0.458257$ (Exact match)

---

## 4. Round Lifecycle State Transitions & Quorum Logic

### 4.1 State Machine
```text
           ┌──────────────┐
           │   CREATED    │
           └──────┬───────┘
                  │ start_round()
                  ▼
       ┌──────────────────────┐
       │   MODEL_DISTRIBUTED  │
       └──────────┬───────────┘
                  │ nodes dispatched
                  ▼
           ┌──────────────┐
           │   TRAINING   │
           └──────┬───────┘
                  │ submit_update() [first update]
                  ▼
       ┌──────────────────────┐
       │  UPDATES_COLLECTING  │ ◄─── (additional node updates)
       └──────────┬───────────┘
                  │ trigger_aggregation() (Quorum >= 2 met)
                  ▼
           ┌──────────────┐
           │  AGGREGATING │
           └──────┬───────┘
                  │ FedAvg + Registry save
                  ▼
           ┌──────────────┐
           │  COMPLETED   │
           └──────────────┘
```
If quorum is not met (e.g., $< 2$ updates received) or unrecoverable error occurs, the round transitions to `FAILED`.

### 4.2 Quorum Rules & Validation Guardrails
1. **Expected Municipal Nodes:** 3 (`PUNE`, `MUMBAI`, `DELHI`).
2. **Minimum Quorum Threshold:** 2 of 3 updates required to trigger aggregation.
   - 3 of 3 received $\rightarrow$ Quorum satisfied $\rightarrow$ Aggregates consensus model.
   - 2 of 3 received $\rightarrow$ Quorum satisfied $\rightarrow$ Aggregates consensus model.
   - 1 of 3 received $\rightarrow$ Quorum violated $\rightarrow$ Raises `ValueError` ("Insufficient updates for aggregation: received 1, minimum quorum is 2"). Round marked `FAILED`.
3. **Stale Model Rejection:**
   - If an update's `baseModelVersion` does not match the round's `baseModelVersion`, it is rejected immediately with `ValueError` ("Stale base model: round requires ... but update is based on ...").
4. **Duplicate Update Rejection:**
   - If a node attempts to submit more than one update for the same round, the second update is rejected with `ValueError` ("Duplicate update from node ... for round ...").
5. **Feature Dimension & Integrity Validation:**
   - Weights vector must have length exactly 36 (`FEATURE_COUNT`).
   - Weights cannot contain `NaN`, `+Inf`, or `-Inf`.
   - Sample count must be strictly $> 0$.

---

## 5. Model Registry Version Lineage & Artifact Persistence

### 5.1 Version Lineage Architecture
The `ModelRegistry` maintains an in-memory catalog of global consensus models backed by joblib serialization in `storage/models/global/`:
- **Initial State:** `global-v1` initialized with neutral zero-weights, $b = 0.0$, and baseline metadata. Active pointer set to `global-v1`.
- **Round 1 (3 nodes):** FedAvg aggregation generates `global-v2`. Active pointer atomically switches to `global-v2`.
- **Round 2 (2 nodes):** FedAvg aggregation generates `global-v3`. Active pointer atomically switches to `global-v3`.
- **Immutability:** Once registered, past versions are immutable and retained for auditability and model provenance tracking.

### 5.2 Artifact Persistence on Disk
Directory: `storage/models/global/`
```text
storage/models/global/
├── global-v1.joblib (1,406 bytes)
├── global-v2.joblib (1,407 bytes)
└── global-v3.joblib (1,399 bytes)
```

Each joblib bundle contains a self-contained dictionary:
```json
{
  "version": "global-v2",
  "weights": [ ... 36 float values ... ],
  "intercept": -0.1582,
  "feature_names": [ ... 36 ordered feature names ... ],
  "feature_count": 36,
  "metadata": {
    "version": "global-v2",
    "baseModelVersion": "global-v1",
    "roundId": "ROUND-001",
    "participatingNodes": ["PUNE", "MUMBAI", "DELHI"],
    "totalSamples": 3000,
    "aggregationStrategy": "FED_AVG",
    "metrics": {
      "mae": 0.3156,
      "rmse": 0.4215,
      "rocAuc": 0.7238,
      "brierScore": 0.1782
    }
  }
}
```

---

## 6. Unit Test Results (`test_coordinator.py`)

Execution command: `python -m pytest federated/tests/ -v`

```text
============================= test session starts =============================
platform win32 -- Python 3.11.9, pytest-9.1.1, pluggy-1.6.0
rootdir: C:\Users\Harsh\OneDrive\Documents\Python Projects\AeroSentinel
collected 16 items

federated/tests/test_coordinator.py::TestCoordinatorAndAggregator::test_01_complete_3_node_round_simulation PASSED [  6%]
federated/tests/test_coordinator.py::TestCoordinatorAndAggregator::test_02_quorum_threshold_two_of_three PASSED [ 12%]
federated/tests/test_coordinator.py::TestCoordinatorAndAggregator::test_03_insufficient_quorum_blocks_aggregation PASSED [ 18%]
federated/tests/test_coordinator.py::TestCoordinatorAndAggregator::test_04_stale_base_model_rejection PASSED [ 25%]
federated/tests/test_coordinator.py::TestCoordinatorAndAggregator::test_05_duplicate_update_rejection PASSED [ 31%]
federated/tests/test_coordinator.py::TestCoordinatorAndAggregator::test_06_mismatched_weight_dimension_rejection PASSED [ 37%]
federated/tests/test_coordinator.py::TestCoordinatorAndAggregator::test_07_model_registry_version_lineage PASSED [ 43%]
federated/tests/test_coordinator.py::TestCoordinatorAndAggregator::test_08_mathematical_fedavg_correctness PASSED [ 50%]
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_01_feature_vector_dimension_and_ordering PASSED [ 56%]
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_02_pune_local_training PASSED [ 62%]
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_03_mumbai_local_training PASSED [ 68%]
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_04_delhi_local_training PASSED [ 75%]
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_05_parameter_update_packaging PASSED [ 81%]
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_06_global_model_parameter_synchronization PASSED [ 87%]
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_07_cross_city_contract_parity PASSED [ 93%]
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_08_no_raw_data_leakage PASSED [100%]

============================= 16 passed in 3.91s ==============================
```

All 8 tests in `test_coordinator.py` and all 8 tests in `test_local_nodes.py` passed with 100% success rate.

---

## 7. Multi-Round Simulation Results

Script: [`federated/run_round_simulation.py`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/federated/run_round_simulation.py)
Execution command: `python federated/run_round_simulation.py`

### 7.1 Simulation Execution Log
```text
=================================================================
AeroSentinel F9-P3: Multi-Round Federated Coordinator Simulation
=================================================================

[*] Initial Active Global Model: global-v1

=== STARTING ROUND-001 (Base: global-v1, Expected: Pune, Mumbai, Delhi) ===
  [OK] Round Status: CREATED, Quorum Required: 2
  [*] Pune Node training (1200 samples)...
    -> Pune update submitted (MAE: 0.4312)
  [*] Mumbai Node training (800 samples)...
    -> Mumbai update submitted (MAE: 0.4188)
  [*] Delhi Node training (1000 samples)...
    -> Delhi update submitted (MAE: 0.0944)
  [*] Dispatching FedAvg Aggregation for ROUND-001...
  [OK] Round 1 Aggregation Complete!
       New Global Model Version: global-v2
       Participating Nodes:     3
       Total Samples:           3000
       Aggregated MAE:          0.3156
       Aggregated RMSE:         0.4215
       Aggregated ROC-AUC:      0.7238
       Artifact Saved:          storage/models/global/global-v2.joblib

=== STARTING ROUND-002 (Base: global-v2, Quorum: 2 of 3) ===
  [OK] Round Status: CREATED, Quorum Required: 2
  [*] Pune Node training on global-v2 (1500 samples)...
    -> Pune update submitted (MAE: 0.4034)
  [*] Mumbai Node training on global-v2 (900 samples)...
    -> Mumbai update submitted (MAE: 0.4248)
  [INFO] Delhi Node offline / non-responsive this round (testing partial quorum)...
  [*] Dispatching FedAvg Aggregation for ROUND-002...
  [OK] Round 2 Aggregation Complete!
       New Global Model Version: global-v3
       Participating Nodes:     2 (Quorum Met)
       Total Samples:           2400
       Aggregated MAE:          0.4114
       Aggregated RMSE:         0.4542
       Aggregated ROC-AUC:      0.7525
       Artifact Saved:          storage/models/global/global-v3.joblib

=== MODEL REGISTRY LINEAGE AUDIT ===
  * global-v1: Base=None, TotalSamples=0, Nodes=['PUNE', 'MUMBAI', 'DELHI']
    Artifact: storage/models/global/global-v1.joblib
  * global-v2: Base=global-v1, TotalSamples=3000, Nodes=['PUNE', 'MUMBAI', 'DELHI']
    Artifact: storage/models/global/global-v2.joblib
  * global-v3: Base=global-v2, TotalSamples=2400, Nodes=['PUNE', 'MUMBAI']
    Artifact: storage/models/global/global-v3.joblib

-----------------------------------------------------------------
Multi-Round Simulation Verification SUCCESSFUL.
Artifacts global-v2.joblib and global-v3.joblib are verified on disk.
-----------------------------------------------------------------
```

---

## 8. Validation Guardrails & Error Handling Evidence

| Guardrail Test | Scenario | Coordinator Response | Verified By |
|---|---|---|---|
| **Stale Model Rejection** | Node submits update with `baseModelVersion="global-v0"` when round expects `"global-v1"` | Rejected with `ValueError("Stale base model...")` | `test_04_stale_base_model_rejection` |
| **Duplicate Update Rejection** | Node submits a second update payload in the same training round | Rejected with `ValueError("Duplicate update from node...")` | `test_05_duplicate_update_rejection` |
| **Dimension Mismatch Rejection** | Node submits weight vector of 35 floats instead of 36 | Rejected with `ValueError("Invalid weights dimension: expected 36, got 35")` | `test_06_mismatched_weight_dimension_rejection` |
| **NaN / Inf Weights Rejection** | Node submits weight vector containing `float('nan')` | Rejected with `ValueError("Weights vector contains NaN or Inf values")` | `aggregator.py:validate_update` |
| **Quorum Failure Handling** | Only 1 node submits update before aggregation is triggered | Aggregation blocked; round marked `FAILED`; raises `ValueError` | `test_03_insufficient_quorum_blocks_aggregation` |
| **Quorum Threshold Satisfaction** | 2 of 3 nodes submit updates (e.g. Pune + Delhi) | Aggregation succeeds; consensus model published | `test_02_quorum_threshold_two_of_three` |

---

## 9. Protected Features F1–F8 Status

- **F1 City + Air Quality Ingestion:** Unmodified, preserved.
- **F2 Weather + H3 Spatial Grid:** Unmodified, preserved.
- **F3 Hotspot Detection:** Unmodified; 36-feature schema `f3-features-v1` strictly preserved.
- **F4 Forecast:** Unmodified, preserved.
- **F5 Evidence Orchestration / scoring:** Unmodified, preserved.
- **F6 Citizen Report / Gemini Vision:** Unmodified, preserved.
- **F7 Pollution Event / Alert / Authority Workflow:** Unmodified, preserved.
- **F8 Monitoring Gap / Recommendations:** Unmodified, preserved.
- **Zero backend/frontend modifications:** No Spring Boot Java files or React frontend files were modified in F9-P3.

---

## 10. Pass Criteria Checklist & Explicit Declaration

- [x] `federated/coordinator/aggregator.py` implemented with verified FedAvg math
- [x] `federated/coordinator/round_manager.py` implemented with state machine and quorum rules
- [x] `federated/coordinator/model_registry.py` implemented with immutable version catalog
- [x] `federated/coordinator/coordinator.py` facade implemented
- [x] Stale updates, duplicate updates, and invalid weights are rejected
- [x] Minimum quorum (2 of 3) is enforced
- [x] All unit tests in `test_coordinator.py` PASS (8/8)
- [x] Multi-round simulation creates verified `global-v2.joblib` and `global-v3.joblib` artifacts
- [x] No modifications made to Spring Boot, React, or F1–F8 code
- [x] `F9_P3_COORDINATOR_REPORT.md` created with verified runtime evidence

**Phase F9-P3 Status:** **PASS** ✅
