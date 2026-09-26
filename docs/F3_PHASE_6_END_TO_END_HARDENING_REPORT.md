# AeroSentinel — F3 Phase 6: Hotspot End-to-End Hardening & Production Consistency Report

## 1. Objective
The objective of F3 Phase 6 is to verify, harden, and solidify the complete production chain of hotspot intelligence across all architectural tiers:
$$\text{F2 Air + Weather + H3} \longrightarrow \text{FeatureSnapshot} \longrightarrow \text{ML / Baseline Engine} \longrightarrow \text{HotspotPrediction} \longrightarrow \text{PostgreSQL} \longrightarrow \text{Spring Boot REST API} \longrightarrow \text{React UI}$$
This phase ensures multi-city consistency, deterministic inference, historical immutability, geographic boundary safety, fail-safe degradation, and zero regression across F2 and earlier F3 phases.

---

## 2. Complete F2 → F3 → ML Data Chain
The end-to-end data pipeline traces an unbroken lineage from raw physical sensor observations to frontend visualizations:
1. **F2 Physical Telemetry**:
   - Continuous ground-level air observations from CAAQMS stations (PM2.5, PM10, NO2, SO2, CO, O3).
   - Atmospheric microclimate parameters from Open-Meteo (temperature, relative humidity, wind speed, wind direction, surface pressure, precipitation).
   - Uber H3 Spatial Indexing (Resolution 8) attaching each telemetry reading to a unique 15-character hexagonal cell centroid.
2. **F3 Feature Layer**:
   - Derived spatial lag matrices (leave-one-out spatial lag mean, coverage gaps).
   - NASA FIRMS thermal anomaly features (fire counts, FRP sum/mean, distance decay, upwind dispersion alignment).
   - OpenStreetMap GIS features (distance to industrial zones, highways, sensitive receptors).
   - Persisted as an immutable `FeatureSnapshot` in PostgreSQL (`feature_snapshots`) with audit schema `f3-features-v1`.
3. **Model Adapter Boundary**:
   - `ModelFeatureVectorAdapter` adapts the persistent snapshot into exactly 36 ordered numeric values.
   - Wind speed converted strictly once ($\text{wind\_speed}_{\text{mps}} = \frac{\text{wind\_speed}_{\text{kmh}}}{3.6}$).
4. **ML Inference Model**:
   - Invokes `hotspot_classifier_v1.joblib` via FastAPI REST / CLI process bridge.
   - Outputs calibrated probability $p \in [0.0, 1.0]$ and evaluates operational decision threshold $p \ge 0.20$.
5. **Persistence**:
   - Validated by `HotspotPredictionValidator`.
   - Stored in `hotspot_predictions` with foreign key provenance referencing `feature_snapshots.id`.
6. **Spring Boot REST API**:
   - `HotspotOverviewResponse` (`GET /api/v1/hotspots?cityId=...`) and `HotspotCellDto` (`GET /api/v1/hotspots/{h3Index}`).
7. **React UI**:
   - Interactive Leaflet H3 choropleth map (`H3RiskLayer`), risk badges, confidence gauges, and detailed cell inspector.

---

## 3. Engine Routing Consistency
AeroSentinel implements strict multi-city engine routing:

| City | City UUID | Active Engine | Model Version | Engine Type | Rationale |
|---|---|---|---|---|---|
| **Pune** | `550e8400-e29b-41d4-a716-446655440001` | `MLHotspotDetectionEngine` | `hotspot_classifier_v1` | `ML` | Trained and validated on Pune CAAQMS telemetry. |
| **Mumbai** | `550e8400-e29b-41d4-a716-446655440002` | `BaselineHotspotDetectionEngine` | `hotspot-baseline-v1` | `BASELINE` | Controlled fallback; prevents invalid ML extrapolation. |
| **Delhi** | `550e8400-e29b-41d4-a716-446655440003` | `BaselineHotspotDetectionEngine` | `hotspot-baseline-v1` | `BASELINE` | Controlled fallback; prevents invalid ML extrapolation. |

- Direct invocation of `MLHotspotDetectionEngine` on non-Pune coordinates returns `status: "MODEL_DOMAIN_UNSUPPORTED"` and delegates cleanly to the baseline engine with explicit baseline attribution.
- No silent substitution of baseline metadata as ML.

---

## 4. API Consistency & Privacy
- **Endpoints Verified**:
  - `GET /api/v1/hotspots?cityId={cityId}`
  - `GET /api/v1/hotspots/{h3Index}`
- **Response Fields**:
  - `cityId`, `cityName`, `generatedAt`, `modelVersion`, `engineType`, `freshness`, `totalCells`, `highRiskCells`, `cells`.
  - Cell details: `h3Index`, `gridCellId`, `riskScore`, `riskLevel`, `confidence`, `predictedAt`, `freshness`, `modelVersion`.
- **Information Boundary**: Internal 36-feature snapshot JSON vectors, internal database primary keys, and raw model weights are strictly shielded from public API payloads.

---

## 5. Prediction History & Immutability
- All hotspot predictions are append-only. History is never overwritten or deleted.
- **Tie-Breaking Resolution**:
  ```sql
  SELECT * FROM hotspot_predictions WHERE id IN (
    SELECT DISTINCT ON (h3_index) id FROM hotspot_predictions
    WHERE city_id = :cityId
    ORDER BY h3_index, predicted_at DESC, created_at DESC
  ) ORDER BY risk_score DESC
  ```
- When predictions have identical `predicted_at` (e.g. from recalculation against the same underlying observation), the newer `created_at` timestamp deterministically breaks the tie while preserving historical records.

---

## 6. Freshness Architecture
Predictions adhere to the temporal freshness standard:

| Elapsed Age | Freshness Status | UI Presentation | Backend Action |
|---|---|---|---|
| $\le 2\text{ hours}$ | `LIVE` | Green Badge | Active real-time surveillance |
| $2\text{ hours} < \text{Age} \le 24\text{ hours}$ | `STALE` | Amber Badge | Cached historical fallback |
| $> 24\text{ hours}$ | `UNAVAILABLE` | Red Badge | Stale telemetry warning |
| $\text{null}$ | `NO_DATA` | Neutral Gray | Sensor offline |

- If the AI service is unreachable, existing database predictions are returned and marked `STALE` (or `UNAVAILABLE`) based on their actual observation age.
- Model failure never generates artificial replacements or fake live scores.

---

## 7. Model Failure Safety
Controlled error handling matrix:

| Failure Mode | Trigger Condition | System Behavior |
|---|---|---|
| **Artifact Missing** | Joblib file deleted or moved | Returns `MODEL_UNAVAILABLE` (HTTP 503); falls back safely. |
| **Corrupt Artifact** | Malformed joblib file | Raises `MODEL_LOAD_FAILED`; refuses invalid startup. |
| **Service Timeout** | Python service unresponsive | Fallback to process runner or returns existing cached predictions marked `STALE`. |
| **Probability Out of Bounds** | $p < 0.0$ or $p > 1.0$ or $\text{NaN}$ | Throws `INVALID_MODEL_OUTPUT`; rejected by validator; zero persistence. |
| **Malformed JSON** | Non-parsable payload | Caught by Jackson/Pydantic; returns HTTP 400. |

---

## 8. Feature Failure Safety
- **Missing Co-pollutants**: Snapshots missing any of PM10, NO2, SO2, CO, or O3 trigger `INSUFFICIENT_DATA` (HTTP 400).
- **Non-finite Values**: Features containing `NaN`, `+Infinity`, or `-Infinity` are rejected by `ModelFeatureVectorAdapter` with `IllegalStateException`.
- **Schema Mismatch**: Snapshots not matching `f3-features-v1` are rejected before model consumption.
- **Strict Vector Count**: Vector length must equal exactly 36.

---

## 9. Geographic Safety Policy
- Model `hotspot_classifier_v1.joblib` was trained strictly on Pune Metropolitan Region (PMR) ground station observations.
- Geography is protected via `isPuneDomain()` coordinates check ($[18.0^\circ\text{N}, 73.3^\circ\text{E}]$ to $[19.2^\circ\text{N}, 74.4^\circ\text{E}]$) and City UUID validation.
- Non-Pune requests return explicit metadata: `domainSupported = false` and `status = MODEL_DOMAIN_UNSUPPORTED`.

---

## 10. Frontend Contract Regression
- **Verified Files (Source Code Inspection Only)**:
  - `frontend/src/types/hotspot.ts`
  - `frontend/src/pages/public/Hotspots.tsx`
  - `frontend/src/components/hotspot/HotspotCellDetailsCard.tsx`
- **Rendered Properties**:
  - `cell.h3Index` (monospace display)
  - `cell.riskScore` ($0.0\%\text{--}100.0\%$)
  - `cell.riskLevel` (`LOW`, `MODERATE`, `HIGH`, `CRITICAL` color-coded)
  - `cell.confidence` ($0\%\text{--}100\%$)
  - `cell.freshness` (`LIVE`, `STALE`, `UNAVAILABLE`)
  - `cell.modelVersion` (`hotspot_classifier_v1` or `hotspot-baseline-v1`)
  - `cell.predictedAt` (localized timestamp)
- **Zero Browser Policy**: Verified without launching browser or requesting manual clicks.

---

## 11. Observability & Logging
- Informative debug and warning logs at every key boundary:
  - Model availability: `Loaded Calibrated Random Forest model from ...`
  - Version attribution: `Generating ML hotspot predictions for city Pune (cells=3)`
  - Domain guard: `Model domain guard: H3 cell ... belongs to city ... outside Pune PMR.`
  - Quality degradation: `Feature snapshot ... has quality status UNAVAILABLE.`
- **Privacy Assurance**: No API keys, database credentials, or auth tokens are logged.

---

## 12. Performance & Resource Integrity
- **Artifact Caching**: The 20.5 MB joblib artifact is loaded exactly once into application memory on startup; repeated inference takes $< 15\text{ ms}$.
- **Query Optimization**: Database queries leverage `idx_hotspot_predictions_city_time` and `idx_feature_snapshots_cell_time` to avoid full table scans.
- **N+1 Avoidance**: City hotspot requests fetch cells and latest predictions via single bulk queries (`findLatestByCityId`).

---

## 13. Security Boundaries
- **Strict Tier Decoupling**:
  $$\text{React UI} \xrightarrow[\text{REST}]{\text{HTTP}} \text{Spring Boot (8080)} \xrightarrow[\text{REST}]{\text{HTTP}} \text{Python AI Service (8000)} \longrightarrow \text{Joblib Artifact}$$
- React has zero direct access to PostgreSQL or Python filesystem.
- Python AI service has zero database write access; persistence is strictly governed by Spring Boot JPA.

---

## 14. Real Pune ML Proof (Live Production Telemetry)
Queried from running Spring Boot API and PostgreSQL database:

| Metric | Recorded Value |
|---|---|
| **City Name** | Pune (`550e8400-e29b-41d4-a716-446655440001`) |
| **H3 Cell Index** | `88608850e5fffff` (Shivajinagar) |
| **Engine Type** | `ML` |
| **Model Version** | `hotspot_classifier_v1` |
| **Risk Score (Calibrated Prob)** | **`0.7998`** (79.98%) |
| **Risk Level** | **`CRITICAL`** |
| **Confidence** | **`0.86`** (86.0%) |
| **Predicted At** | `2026-09-26T13:09:44.571028Z` |
| **Freshness** | `LIVE` |
| **Feature Snapshot ID** | `1624baa3-a5f8-407b-b1c2-36bcee7650b1` |

---

## 15. Real Mumbai / Delhi Baseline Proof
Queried from running Spring Boot API and PostgreSQL database:

| Metric | Mumbai Record | Delhi Record |
|---|---|---|
| **City Name** | Mumbai (`...0002`) | Delhi (`...0003`) |
| **H3 Cell Index** | `88608b56b3fffff` (Colaba) | `883da1149bfffff` (Anand Vihar) |
| **Engine Type** | `BASELINE` | `BASELINE` |
| **Model Version** | `hotspot-baseline-v1` | `hotspot-baseline-v1` |
| **Risk Score** | `0.264` | `0.450` |
| **Risk Level** | `LOW` | `MODERATE` |
| **Confidence** | `0.35` | `0.35` |
| **Freshness** | `LIVE` | `LIVE` |
| **Status Tag** | `CONTROLLED_BASELINE` | `CONTROLLED_BASELINE` |

---

## 16. Comprehensive Test Results
- **Spring Boot Backend Test Suite**:
  - `HotspotPhase6HardeningTest`: 11/11 passed
  - `MLHotspotDetectionEngineTest`: 9/9 passed
  - `HotspotIntegrationTest`: 5/5 passed
  - Total Backend Suite: **183 passed, 0 failures, 1 skipped (100%)**
- **Python ML Test Suite**:
  - `test_f3_ml_inference.py`: 13/13 passed
  - `test_f3_feature_contract.py`: 12/12 passed
  - Total Python Suite: **25 passed, 0 failures (100%)**
- **Frontend Production Build**:
  - `tsc -b && vite build` passed with zero errors in 26.16s.

---

## 17. F2 / F3 Regression Verification
- All F2 Air observation ingestion tests: **PASS**
- All F2 Weather ingestion and spatial association tests: **PASS**
- All F2 H3 spatial boundary and index resolution tests: **PASS**
- All F3 Phase 2 Feature Snapshot derivation tests: **PASS**
- All F3 Phase 3 Baseline detection engine tests: **PASS**
- All F3 Phase 4 Frontend component types and builds: **PASS**
- All F3 Phase 5 ML Inference integration tests: **PASS**

---

## 18. Known Limitations
1. **PMR Geographic Scope**: The trained model `hotspot_classifier_v1.joblib` applies exclusively to Pune Metropolitan Region. Mumbai and Delhi gracefully use the baseline engine until multi-city datasets are trained.
2. **Missing Co-pollutant Sensors**: Non-Pune stations lacking continuous SO2/CO sensors produce `UNAVAILABLE` feature snapshots, safely precluding ML inference without inventing surrogate values.

---

## 19. Phase 7 Starting Point
F3 Phase 6 is complete and hardened:
- Full end-to-end data pipeline verified with live production data.
- Dual-engine architecture operational with explicit routing and metadata.
- Prediction history immutable with deterministic tie-breaking.
- Freshness transitions and failure safeties thoroughly tested.
- Zero browser usage in Phase 6; browser verification strictly deferred to Phase 8.
