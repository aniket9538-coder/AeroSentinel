# AeroSentinel — F4-P4 Spring Boot Backend Integration Report

## 1. Executive Summary

Phase **F4-P4 (Spring Boot Backend Integration + Forecast Persistence + API Bridge)** has established, hardened, and verified the complete backend domain, database persistence, safe CLI process bridging, and RESTful API layer for multi-horizon PM2.5 forecasting ($T+1\text{h}, T+3\text{h}, T+6\text{h}$).

In accordance with strict phase directives and the final contract corrections:
1. **F3 Hotspot Detection remained 100% LOCKED and UNTOUCHED.** All 14 F3 regression tests passed with `BUILD SUCCESS`.
2. **Forecast Model Retraining and Artifact Alteration were strictly forbidden.** Numerical inference was executed solely through the F4-P3 standalone Python CLI bridge (`predict_forecast_cli.py`).
3. **Frontend Untouched.** Zero modifications were made to React pages, charts, or components; frontend integration is strictly reserved for Phase F4-P5.
4. **Authoritative Lineage Enforcement.** `parentPredictionId` is the single source of truth for parent identity. Authoritative `cityId`, `h3Index`, `featureSnapshotId`, and base timestamp $T_0$ (`predictedAt`) are derived directly from the F3 parent record. Any mismatch in caller-supplied context is rejected with HTTP 422 (`FORECAST_PARENT_CONTEXT_MISMATCH`).
5. **Exact Numerical Parity & Interval Preservation.** P4 performs zero independent interval or prediction recalculation. P3 inference output (`predictedPm25`, `lowerBound`, `upperBound`) is preserved with exact mathematical fidelity, matching the audited residual offsets ($P10_1=-1.84, P90_1=+1.86$, etc.). Lower intervals are clamped ($\ge 0$), and `forecastConfidence` is strictly `null`.
6. **Database Lineage Integrity.** Migration `V11__f4_forecast_lineage_integrity.sql` added conditional constraint `chk_forecast_success_lineage` ensuring no row can be persisted with `status = 'SUCCESS'` without non-null parent prediction, city, H3 index, and feature snapshot.
7. **Clear Timestamp Semantics.** $T_0$ (`baseTimestamp` / `predictedAt`) is decoupled from `generatedAt` (inference wall-clock time). Target times are strictly derived as $T_0 + 1\text{h}, T_0 + 3\text{h}, T_0 + 6\text{h}$.
8. **Real End-to-End Runtime Proof.** Verified on Pune Shivajinagar CAAQMS data (`88608850e5fffff`), generating and persisting authentic forecast records verified in PostgreSQL.

---

## 2. P3 Handoff Contract

Phase F4-P4 consumed the exact inference contract produced by Phase F4-P3:
- **CLI Bridge Source**: `ai-service/ml/inference/predict_forecast_cli.py`.
- **Supported Horizons**: Strictly $[1, 3, 6]$ hours.
- **Model Version**: `forecast_regressors_v1`.
- **Confidence Rule**: `forecastConfidence = null` (no synthetic uncertainty, no copying of F3 hotspot confidence).
- **Physical Bounds**: `lowerBound = max(0.0, pred + P10)`, `upperBound = pred + P90`.
- **Audited Residual Offsets**:
  - $T+1\text{h}$: $P10 = -1.8424755$, $P90 = +1.8628680$
  - $T+3\text{h}$: $P10 = -3.9013645$, $P90 = +3.0473582$
  - $T+6\text{h}$: $P10 = -5.5207627$, $P90 = +5.4248675$
- **Target Timestamps**: Deterministically generated from base timestamp $T_0 + h\text{ hours}$.

---

## 3. Forecast Domain Implementation

Implemented in package `com.aerosentinel.forecast`:
- [`Forecast.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/forecast/Forecast.java): JPA entity for forecast persistence with schema constraints and `@PrePersist` lineage validation.
- [`ForecastRepository.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/forecast/ForecastRepository.java): Spring Data JPA repository with lineage, horizon, and spatial query methods.
- [`ForecastAiClient.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/forecast/ForecastAiClient.java): Resilient `ProcessBuilder` bridge to `predict_forecast_cli.py` with bounded 15s timeout, STDIN piping, and 18-step contract validation.
- [`ForecastService.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/forecast/ForecastService.java): Transactional orchestrator resolving authoritative F3 parent context, invoking P2 feature builder, executing P3 Python CLI, enforcing snapshot lineage, and performing atomic database persistence.
- [`ForecastController.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/forecast/ForecastController.java): REST controller exposing `GET /api/v1/forecast/{h3Index}`, `POST /api/v1/forecast/generate`, and `GET /api/v1/forecast/parent/{parentId}`.
- [`ForecastResponse.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/forecast/ForecastResponse.java): Canonical DTO explicitly exposing both `baseTimestamp` ($T_0$) and `generatedAt` with strictly null `forecastConfidence`.
- [`ForecastGenerateRequest.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/forecast/ForecastGenerateRequest.java): Request payload for explicit forecast generation.
- [`ForecastMapper.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/forecast/ForecastMapper.java): Entity-to-DTO mapper with automated freshness calculation and $T_0$ extraction.
- [`ForecastException.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/forecast/ForecastException.java): Structured exception hierarchy including `ParentContextMismatch` (`FORECAST_PARENT_CONTEXT_MISMATCH`).

---

## 4. Database Schema & Migration

### Migration V10: Persistence and Lineage
Flyway migration [`V10__f4_forecast_persistence_and_lineage.sql`](file:///c:/Users/lenovo/AeroSential/backend/src/main/resources/db/migration/V10__f4_forecast_persistence_and_lineage.sql) added:
- Lineage columns: `parent_prediction_id`, `city_id`, `h3_index`, `feature_snapshot_id`.
- Horizon constraint: `chk_forecast_horizon CHECK (horizon_hours IN (1, 3, 6))`.
- Bounds constraint: `chk_forecast_bounds CHECK (lower_bound >= 0.0 AND lower_bound <= predicted_pm25 AND predicted_pm25 <= upper_bound)`.
- Unique idempotency constraint: `uq_forecast_parent_horizon UNIQUE (parent_prediction_id, horizon_hours)`.

### Migration V11: Conditional Snapshot Lineage Integrity
Flyway migration [`V11__f4_forecast_lineage_integrity.sql`](file:///c:/Users/lenovo/AeroSential/backend/src/main/resources/db/migration/V11__f4_forecast_lineage_integrity.sql) added:

```sql
-- V11: F4 Forecast Lineage Integrity and Non-Null Feature Snapshot Constraint
ALTER TABLE forecasts DROP CONSTRAINT IF EXISTS chk_forecast_success_lineage;

ALTER TABLE forecasts ADD CONSTRAINT chk_forecast_success_lineage CHECK (
    status <> 'SUCCESS'
    OR (
        parent_prediction_id IS NOT NULL
        AND city_id IS NOT NULL
        AND h3_index IS NOT NULL
        AND feature_snapshot_id IS NOT NULL
    )
);
```
This guarantees that no successful forecast row can exist without full lineage, while preserving historical legacy compatibility.

---

## 5. Repository Layer

[`ForecastRepository.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/forecast/ForecastRepository.java) provides:
- `findLatestByH3Index(String h3Index)`: Retrieves latest forecast run for a spatial cell.
- `findByParentPredictionIdOrderByHorizonHoursAsc(UUID parentPredictionId)`: Retrieves all 3 horizon rows for a parent prediction.
- `existsByParentPredictionId(UUID parentPredictionId)`: Idempotency check.
- `@Modifying @Transactional deleteByParentPredictionId(UUID parentPredictionId)`: Transactional cleanup for safe re-runs.

---

## 6. ForecastAiClient / CLI Bridge

The CLI bridge invokes `predict_forecast_cli.py` via `ProcessBuilder`:
1. **Safe Execution**: Command parameters are passed as individual array arguments (no unescaped shell strings).
2. **Standard I/O**: Passes input JSON via STDIN and captures STDOUT / STDERR asynchronously.
3. **Bounded Timeout**: 15,000 ms timeout enforced via `process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)`. On timeout, the process is forcibly destroyed (`destroyForcibly()`) and `ForecastException.AiTimeout` is thrown.
4. **Input Payload**:
```json
{
  "h3Index": "88608850e5fffff",
  "cityId": "550e8400-e29b-41d4-a716-446655440001",
  "parentPredictionId": "a310c689-f340-49fc-8935-a037de8d7709",
  "featureSnapshotId": "1624baa3-a5f8-407b-b1c2-36bcee7650b1",
  "predictedAt": "2026-09-26T13:09:44.571028Z",
  "features": { ... 36 feature values ... }
}
```

---

## 7. ForecastService Orchestration

The service executes the following strict order:
1. **Resolve Authoritative F3 Parent**: Loads `HotspotPrediction` by `parentPredictionId`.
2. **Derive Authoritative Context**: Extracts `cityId`, `h3Index`, `featureSnapshotId`, and base timestamp $T_0$ (`predictedAt`) from the F3 parent.
3. **Validate Context Equality**: If caller provides `cityId` or `h3Index`, verifies exact match. On mismatch, throws `ForecastException.ParentContextMismatch` (HTTP 422).
4. **Build Feature Vector**: Resolves or generates the 36-feature vector via `ForecastFeatureBuilder`.
5. **Validate & Adapt**: Validates vector completeness and adapts to shape $(1, 36)$ via `ForecastFeatureAdapter`.
6. **Execute Inference**: Calls `ForecastAiClient.predict(parentId, authoritativePredictedAt, modelReadyVector)`.
7. **Lineage Check**: Asserts `effectiveSnapshotId != null` before persistence.
8. **Transactional Persistence**: Deletes any stale rows for the parent prediction and persists all 3 horizons atomically.
9. **Return Canonical DTO**: Returns public `ForecastResponse`.

---

## 8. F3 → F4 Lineage Enforcement

Authoritative parentage is strictly preserved:
- `forecasts.parent_prediction_id` $\to$ `hotspot_predictions.id`
- `forecasts.city_id` $\to$ `hotspot_predictions.city_id`
- `forecasts.h3_index` $\to$ `hotspot_predictions.h3_index`
- `forecasts.feature_snapshot_id` $\to$ `hotspot_predictions.feature_snapshot_id`
- `target_time` $\to$ `hotspot_predictions.predicted_at + horizonHours`

### Mismatch Rejection Evidence:
```json
POST /api/v1/forecast/generate
{
  "parentPredictionId": "a310c689-f340-49fc-8935-a037de8d7709",
  "cityId": "99999999-9999-9999-9999-999999999999",
  "h3Index": "88608850e5fffff"
}

HTTP/1.1 422 Unprocessable Entity
{
  "timestamp": "2026-09-27T10:22:42.100Z",
  "status": 422,
  "error": "FORECAST_PARENT_CONTEXT_MISMATCH",
  "message": "Requested cityId 99999999-9999-9999-9999-999999999999 does not match authoritative F3 parent prediction cityId 550e8400-e29b-41d4-a716-446655440001"
}
```

---

## 9. Forecast Generation API

**Endpoint**: `POST /api/v1/forecast/generate`
- **Request Body**:
```json
{
  "parentPredictionId": "a310c689-f340-49fc-8935-a037de8d7709",
  "cityId": "550e8400-e29b-41d4-a716-446655440001",
  "h3Index": "88608850e5fffff"
}
```
- **Response**: HTTP 200 OK returning `ForecastResponse`.

---

## 10. Forecast Read API

**Endpoint**: `GET /api/v1/forecast/{h3Index}`
- **Behavior**: Retrieves latest persisted forecast from database.
- **Success (HTTP 200)**: Returns `ForecastResponse` with status `"SUCCESS"`.
- **No Data (HTTP 404)**: Returns controlled `NO_DATA` status:
```json
{
  "h3Index": "886088500000000",
  "status": "NO_DATA",
  "freshness": "NO_DATA",
  "forecasts": [],
  "forecastConfidence": null
}
```

---

## 11. Response Validation & Interval Preservation

The backend validates all 18 output contract invariants:
1. `status == "SUCCESS"`
2. Exactly 3 forecast horizon records
3. Horizon hours strictly $[1, 3, 6]$
4. Target times strictly in ascending chronological order
5. Physical bounds: `0.0 <= lowerBound <= predictedPm25 <= upperBound`
6. `forecastConfidence` strictly `null`
7. Exact artifact residual parity:
   - For $T+1\text{h}$ ($pred = 70.62$): $lower = \max(0, 70.62 - 1.84) = 68.78$, $upper = 70.62 + 1.86 = 72.48$.
   - For $T+3\text{h}$ ($pred = 70.55$): $lower = \max(0, 70.55 - 3.90) = 66.65$, $upper = 70.55 + 3.05 = 73.60$.
   - For $T+6\text{h}$ ($pred = 60.91$): $lower = \max(0, 60.91 - 5.52) = 55.39$, $upper = 60.91 + 5.42 = 66.33$.
8. Zero independent recalculation in Java.

---

## 12. Transactional Persistence

All 3 horizon entities are persisted in a single Spring `@Transactional` block:
- If any database constraint fails, all rows rollback atomically.
- Stale runs for the same parent prediction are deleted prior to saving new rows, guaranteeing idempotency.

---

## 13. Freshness / Status & Timestamp Semantics

### Timestamp Semantics:
- **`baseTimestamp` ($T_0$)**: The observation time of the parent prediction / feature snapshot (e.g. `2026-09-26T13:09:44.571028Z`).
- **`generatedAt`**: The wall-clock timestamp when ML inference ran (e.g. `2026-09-27T10:22:40.113353Z`).
- **`targetTime`**: Strictly derived as $T_0 + h\text{ hours}$ (e.g. $T_0 + 1\text{h} =$ `2026-09-26T14:09:44.571028Z`).

### Freshness Calculation:
- Age $\le 2\text{h} \implies \text{LIVE}$
- $2\text{h} < \text{Age} \le 24\text{h} \implies \text{STALE}$
- $\text{Age} > 24\text{h} \implies \text{UNAVAILABLE}$
- `null` timestamp $\implies \text{NO_DATA}$

---

## 14. Error Handling

| Scenario | Exception | HTTP Code | Error Code |
| :--- | :--- | :---: | :--- |
| Parent prediction not found | `ForecastException.ParentNotFound` | 404 | `FORECAST_PARENT_NOT_FOUND` |
| No forecast found for H3 | `ForecastException.NotFound` | 404 | `FORECAST_NOT_FOUND` |
| Parent context mismatch | `ForecastException.ParentContextMismatch` | 422 | `FORECAST_PARENT_CONTEXT_MISMATCH` |
| Feature vector invalid | `ForecastException.ValidationFailed` | 422 | `FORECAST_VALIDATION_ERROR` |
| Python process start failure | `ForecastException.AiUnavailable` | 503 | `FORECAST_AI_UNAVAILABLE` |
| Python process timeout (>15s) | `ForecastException.AiTimeout` | 504 | `FORECAST_AI_TIMEOUT` |
| Database constraint violation | `ForecastException.PersistenceFailed` | 500 | `FORECAST_PERSISTENCE_FAILED` |

---

## 15. Automated Test Results

### Spring Boot Backend Suites (Maven):
- [`ForecastUnitTest.java`](file:///c:/Users/lenovo/AeroSential/backend/src/test/java/com/aerosentinel/forecast/ForecastUnitTest.java): **14/14 PASS**
  - Contract validation, non-null confidence rejection, horizon set validation, bound order, mapper sorting, freshness computation, null confidence JSON serialization, `ParentContextMismatch` error code, $T_0$ vs `generatedAt` target derivation, entity lineage integrity validation, and artifact residual interval preservation.
- [`ForecastIntegrationTest.java`](file:///c:/Users/lenovo/AeroSential/backend/src/test/java/com/aerosentinel/forecast/ForecastIntegrationTest.java): **11/11 PASS**
  - Foreign key rejection, null snapshot rejection, valid persistence, end-to-end Pune generation, database proof verification, GET API contract, 404 NO_DATA response, POST generate 404 parent not found, GET by parentId, idempotency re-run, cityId mismatch 422 rejection, h3Index mismatch 422 rejection, and exact residual numerical parity.
- **Spring Boot Forecast Suite Total**: **25/25 PASS** (`BUILD SUCCESS`).

### Supporting Feature Layer & Regression Suites:
- [`ForecastFeatureLayerTest.java`](file:///c:/Users/lenovo/AeroSential/backend/src/test/java/com/aerosentinel/forecast/feature/ForecastFeatureLayerTest.java) + [`ForecastFeatureLayerIntegrationTest.java`](file:///c:/Users/lenovo/AeroSential/backend/src/test/java/com/aerosentinel/forecast/feature/ForecastFeatureLayerIntegrationTest.java): **12/12 PASS** (`BUILD SUCCESS`).
- **F3 Hotspot Regression Suite** (`HotspotIntegrationTest` + `MLHotspotDetectionEngineTest`): **14/14 PASS** (`BUILD SUCCESS`).
- **Python P2 Suite** (`test_f4_feature_layer.py`): **13/13 PASS**.
- **Python P3 Suite** (`test_f4_p3_inference.py`): **11/11 PASS**.

**Grand Total**: **75 tests, 100% PASS across Java and Python.**

---

## 16. Real End-to-End Runtime Proof

Evaluated on Pune Shivajinagar CAAQMS station (`88608850e5fffff`) with real parent prediction `a310c689-f340-49fc-8935-a037de8d7709`:

```text
==================================================
REAL_BACKEND_FORECAST_RUNTIME_PROOF
parentPredictionId = a310c689-f340-49fc-8935-a037de8d7709
cityId             = 550e8400-e29b-41d4-a716-446655440001
h3Index            = 88608850e5fffff
featureSnapshotId  = 1624baa3-a5f8-407b-b1c2-36bcee7650b1
baseTimestamp (T0) = 2026-09-26T13:09:44.571028Z
generatedAt        = 2026-09-27T10:22:40.113353Z
modelVersion       = forecast_regressors_v1
T+1h: pred=70.62 [68.78, 72.48] target=2026-09-26T14:09:44.571028Z
T+3h: pred=70.55 [66.65, 73.60] target=2026-09-26T16:09:44.571028Z
T+6h: pred=60.91 [55.39, 66.33] target=2026-09-26T19:09:44.571028Z
forecastConfidence = null
freshness          = LIVE
==================================================
```

Canonical REST API Response (`GET /api/v1/forecast/88608850e5fffff`):
```json
{
  "h3Index": "88608850e5fffff",
  "cityId": "550e8400-e29b-41d4-a716-446655440001",
  "baseTimestamp": "2026-09-26T13:09:44.571028Z",
  "generatedAt": "2026-09-27T10:22:40.113353Z",
  "modelVersion": "forecast_regressors_v1",
  "parentPredictionId": "a310c689-f340-49fc-8935-a037de8d7709",
  "featureSnapshotId": "1624baa3-a5f8-407b-b1c2-36bcee7650b1",
  "status": "SUCCESS",
  "freshness": "LIVE",
  "forecasts": [
    {
      "horizonHours": 1,
      "targetTime": "2026-09-26T14:09:44.571028Z",
      "predictedPm25": 70.62,
      "lowerBound": 68.78,
      "upperBound": 72.48,
      "unit": "ug/m3"
    },
    {
      "horizonHours": 3,
      "targetTime": "2026-09-26T16:09:44.571028Z",
      "predictedPm25": 70.55,
      "lowerBound": 66.65,
      "upperBound": 73.6,
      "unit": "ug/m3"
    },
    {
      "horizonHours": 6,
      "targetTime": "2026-09-26T19:09:44.571028Z",
      "predictedPm25": 60.91,
      "lowerBound": 55.39,
      "upperBound": 66.33,
      "unit": "ug/m3"
    }
  ],
  "forecastConfidence": null
}
```

---

## 17. Database Proof

Direct SQL query against the real PostgreSQL container `aerosentinel-postgres`:

```sql
SELECT id, parent_prediction_id, city_id, h3_index, feature_snapshot_id, horizon_hours, predicted_pm25, lower_bound, upper_bound, forecast_confidence, unit, status, target_time, generated_at
FROM forecasts
ORDER BY horizon_hours ASC;
```

**Actual Database Output**:
```text
                  id                  |         parent_prediction_id         |               city_id                |    h3_index     |         feature_snapshot_id          | horizon_hours | predicted_pm25 | lower_bound | upper_bound | forecast_confidence | unit  | status  |          target_time          |         generated_at          
--------------------------------------+--------------------------------------+--------------------------------------+-----------------+--------------------------------------+---------------+----------------+-------------+-------------+---------------------+-------+---------+-------------------------------+-------------------------------
 3f7491de-ddfc-4aec-a7ab-c6323e431ad1 | a310c689-f340-49fc-8935-a037de8d7709 | 550e8400-e29b-41d4-a716-446655440001 | 88608850e5fffff | 1624baa3-a5f8-407b-b1c2-36bcee7650b1 |             1 |          70.62 |       68.78 |       72.48 |                     | ug/m3 | SUCCESS | 2026-09-26 14:09:44.571028+00 | 2026-09-27 10:22:40.113353+00
 baee351a-45c5-4363-b593-239e08d34486 | a310c689-f340-49fc-8935-a037de8d7709 | 550e8400-e29b-41d4-a716-446655440001 | 88608850e5fffff | 1624baa3-a5f8-407b-b1c2-36bcee7650b1 |             3 |          70.55 |       66.65 |        73.6 |                     | ug/m3 | SUCCESS | 2026-09-26 16:09:44.571028+00 | 2026-09-27 10:22:40.113353+00
 63602a0e-ba4a-4cc6-9279-aa70f895d13b | a310c689-f340-49fc-8935-a037de8d7709 | 550e8400-e29b-41d4-a716-446655440001 | 88608850e5fffff | 1624baa3-a5f8-407b-b1c2-36bcee7650b1 |             6 |          60.91 |       55.39 |       66.33 |                     | ug/m3 | SUCCESS | 2026-09-26 19:09:44.571028+00 | 2026-09-27 10:22:40.113353+00
(3 rows)
```

**Verification Highlights**:
- Exactly 3 rows persisted.
- `parent_prediction_id` strictly matches `hotspot_predictions.id` (`a310c689-f340-49fc-8935-a037de8d7709`).
- `city_id` strictly matches `550e8400-e29b-41d4-a716-446655440001`.
- `h3_index` strictly matches `88608850e5fffff`.
- `feature_snapshot_id` strictly matches `1624baa3-a5f8-407b-b1c2-36bcee7650b1`.
- `forecast_confidence` is explicitly `NULL`.
- Physical bounds: $68.78 \le 70.62 \le 72.48$, $66.65 \le 70.55 \le 73.60$, $55.39 \le 60.91 \le 66.33$.
- Lower bounds $\ge 0.0$.
- Horizons strictly in $\{1, 3, 6\}$.

---

## 18. F3 Regression Results

Executed against the locked F3 regression suite:
- `HotspotIntegrationTest` (5 tests): **PASS**
- `MLHotspotDetectionEngineTest` (9 tests): **PASS**
- **Total**: `14 passed, 0 failed` (`BUILD SUCCESS` in 1m 11s).
- **Result**: F3 hotspot detection behavior, confidence scores, and database tables remain 100% UNTOUCHED and fully functional.

---

## 19. Files Created

1. `backend/src/main/resources/db/migration/V10__f4_forecast_persistence_and_lineage.sql`: Flyway migration for forecast persistence and constraints.
2. `backend/src/main/resources/db/migration/V11__f4_forecast_lineage_integrity.sql`: Flyway migration adding `chk_forecast_success_lineage` constraint.
3. `backend/src/main/java/com/aerosentinel/forecast/ForecastAiClient.java`: Spring Boot process bridge to Python CLI.
4. `backend/src/main/java/com/aerosentinel/forecast/ForecastGenerateRequest.java`: DTO for explicit generation requests.
5. `backend/src/main/java/com/aerosentinel/forecast/ForecastResponse.java`: Canonical output contract DTO with $T_0$ separation.
6. `backend/src/main/java/com/aerosentinel/forecast/ForecastMapper.java`: Entity to DTO mapper with freshness and $T_0$ calculation.
7. `backend/src/main/java/com/aerosentinel/forecast/ForecastException.java`: Domain exception hierarchy including `ParentContextMismatch`.
8. `backend/src/test/java/com/aerosentinel/forecast/ForecastUnitTest.java`: 14 unit tests for validation, residual math, mapping, and serialization.
9. `backend/src/test/java/com/aerosentinel/forecast/ForecastIntegrationTest.java`: 11 integration tests covering persistence, lineage mismatch, snapshot check, and APIs.
10. `AeroSentinel_F4_P4_SpringBoot_Backend_Integration_Report.md`: This authoritative engineering report.

---

## 20. Files Modified

1. `backend/src/main/java/com/aerosentinel/forecast/Forecast.java`: Added lineage fields, constraints, constructors, and `@PrePersist` validation.
2. `backend/src/main/java/com/aerosentinel/forecast/ForecastRepository.java`: Added lineage, horizon, latest cell, and transactional delete methods.
3. `backend/src/main/java/com/aerosentinel/forecast/ForecastService.java`: Replaced skeleton with full P2 $\to$ P3 $\to$ persistence orchestration and authoritative context validation.
4. `backend/src/main/java/com/aerosentinel/forecast/ForecastController.java`: Implemented GET /api/v1/forecast/{h3Index}, POST /generate, and GET by parent.
5. `backend/src/main/java/com/aerosentinel/exception/GlobalExceptionHandler.java`: Added HTTP error mappings for `ForecastException` subclasses including 422 for `ParentContextMismatch`.
6. `backend/src/main/resources/application.yml`: Added `app.forecast.ai` process execution configuration.

---

## 21. Files Not Modified

- All F3 hotspot classes (locked): `BaselineHotspotDetectionEngine`, `MLHotspotDetectionEngine`, `HotspotService`, `HotspotPrediction`, `HotspotRepository`.
- `ai-service/models/artifacts/forecast_regressors_v1.joblib` (unmodified).
- `ai-service/ml/forecast/engine.py` (unmodified).
- All frontend React files (reserved for Phase F4-P5): `ForecastPage.tsx`, `ForecastChart.tsx`, `useForecast.ts`, `forecastApi.ts`.

---

## 22. Known Limitations

1. **CLI Process Overhead**: Each forecast generation executes an external Python CLI process (~2-3s runtime). Suitable for on-demand hotspot analysis and scheduled batch runs. Future phases may evaluate persistent microservice RPC if sub-second generation is required.
2. **PMR Geography**: The forecast model was trained specifically on Pune Metropolitan Region environmental dynamics.
3. **Discrete Horizons**: Forecasts exist strictly at $T+1\text{h}, T+3\text{h}, T+6\text{h}$; intermediate horizons are not produced.

---

## 23. F4-P5 Handoff Contract

Phase F4-P4 hands off cleanly to Phase F4-P5 (Frontend Forecast Experience):

```text
[P3 Python Inference]
         ↓
[Spring Boot ForecastService]
         ↓
[PostgreSQL forecasts Table]
         ↓
GET /api/v1/forecast/{h3Index}  (or POST /api/v1/forecast/generate)
         ↓
─── HANDOFF TO PHASE F4-P5 (REACT FRONTEND FORECAST EXPERIENCE) ───
```

Phase P5 will consume:
- `GET /api/v1/forecast/{h3Index}` returning `ForecastResponse` with horizons 1, 3, 6.
- `baseTimestamp`: Base observation time $T_0$.
- `generatedAt`: Inference generation wall-clock time.
- `status`: `"SUCCESS"` or `"NO_DATA"`.
- `freshness`: `"LIVE"`, `"STALE"`, `"UNAVAILABLE"`.
- `forecastConfidence`: strictly `null`.
- Bounds: `predictedPm25`, `lowerBound`, `upperBound`, `unit: "ug/m3"`.

---

## 24. Final Machine-Readable Summary

```json
F4_P4_SUMMARY = {
  "springBootForecastDomain": "PASS",
  "databaseMigration": "PASS",
  "repository": "PASS",
  "cliBridge": "PASS",
  "forecastService": "PASS",
  "lineageEnforcement": "PASS",
  "generationApi": "PASS",
  "readApi": "PASS",
  "responseValidation": "PASS",
  "transactionalPersistence": "PASS",
  "freshnessStatus": "PASS",
  "errorHandling": "PASS",
  "runtimeProof": "PASS",
  "databaseProof": "PASS",
  "tests": "PASS",
  "f3Regression": "PASS",
  "f3Untouched": "PASS",
  "artifactUntouched": "PASS",
  "frontendUntouched": "PASS",
  "status": "COMPLETE"
}
```

---

## 25. Final P4 Decision

**PHASE F4-P4 IS LOCKED.** Authoritative lineage enforcement, database constraints, exact numerical parity with artifact residuals, $T_0$ vs `generatedAt` timestamp separation, and end-to-end tests are 100% verified. All stop conditions respected.
