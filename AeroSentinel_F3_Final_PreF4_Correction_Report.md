# AeroSentinel — F3 Final Pre-F4 Correction & Lock Report

**Project:** AeroSentinel  
**Phase:** F3 Hotspot Detection Engine & Spatial Risk Classification  
**Status:** **LOCKED FOR F4**  
**Date:** September 27, 2026  
**Artifact Version:** 1.2.0-FINAL  

---

## 1. Executive Summary

This report establishes the authoritative, production-grade completion and formal **LOCK** of Feature 3 (**F3: Hotspot Detection Engine & Spatial Risk Classification**) prior to the commencement of Feature 4 (**F4: PM2.5 Multi-Horizon Forecast Engine**).

An exhaustive, end-to-end contract audit was conducted across the operational stack—spanning the React/TypeScript frontend, the Spring Boot REST and service boundaries, Hibernate/PostgreSQL persistence, and the Python FastAPI/CLI machine learning inference subsystem (`hotspot_classifier_v1.joblib`). All core contract fields required by downstream forecast attachment were verified.

Minimal, non-breaking contract patches were applied to surface the runtime `operationalThreshold` (0.20 for ML, 0.40 for Baseline), the explicit boolean indicator `isHotspot` ($\text{riskScore} \ge \text{operationalThreshold}$), and structured epistemic uncertainty fields (`confidenceBreakdown`, `spatialCoverageConfidence`) at both the cell DTO and spatial context boundaries. Full regression suites across F1, F2, F3, Python ML, and TypeScript production builds were executed with 100% pass rates. F3 is hereby designated **LOCKED FOR F4**.

---

## 2. Scope of This Correction

The scope of this task was strictly bounded to ensure contract stability without architecture redesign:
- **In Scope:**
  - Audit of the end-to-end F3 contract across all layers.
  - Minimal additive patches to DTOs, mappers, and TypeScript interfaces to surface missing mandatory fields (`isHotspot`, `operationalThreshold`, `confidenceBreakdown`, `spatialCoverageConfidence`).
  - Formalizing the downstream F3 $\rightarrow$ F4 Forecast Attachment identity:
    $$\text{predictionId} + \text{cityId} + \text{h3Index} + \text{featureSnapshotId}$$
  - Full regression execution: F1 air quality, F2 weather & H3 spatial grid, F3 hotspot domain/inference, Python ML test suite, and Vite/TypeScript production compilation.
  - Verification of actual runtime API and database proof rows.
  - Establishing explicit F3 Lock governance.
- **Out of Scope (Strictly Enforced Boundaries):**
  - **F3 does NOT define forecast horizons, forecast values, or forecast intervals.**
  - **F4 strictly owns all forecast horizons, forecast predictions, forecast intervals, and forecast-specific metadata.**
  - **Forecast confidence must NOT automatically inherit F3 hotspot prediction confidence.**
  - **`confidenceBreakdown` is optional for F4 consumption, but remains authoritative for F3 traceability and F5 explainability.**
  - No rebuilding or restructuring of F3 architecture.
  - No retraining or altering of the ML model artifact (`hotspot_classifier_v1.joblib`).
  - No arbitrary threshold changes (0.20 ML operational threshold preserved).
  - No schema rewrites or Flyway database table mutations.
  - No changes to F2 contracts or data ingestion pipelines.
  - No implementation of F4 forecasting algorithms or Gemini F5 explanation systems.

---

## 3. What Was Already Working in F3

Prior to this correction task, the core F3 capability was already fully operational and verified:
1. **Calibrated ML Inference Pipeline:**
   - 36-feature vector adaptation via [ModelFeatureVectorAdapter.java](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/hotspot/ModelFeatureVectorAdapter.java) in exact scikit-learn feature order.
   - Wind speed conversion boundary ($km/h \rightarrow m/s$) strictly normalized once at the adapter boundary.
   - Dual-channel inference client via [AiServiceHotspotClient.java](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/hotspot/AiServiceHotspotClient.java) (REST HTTP + fallback CLI sub-process).
2. **Geographic Domain Enforcement:**
   - Model domain guard active for Pune Metropolitan Region (PMR) (`550e8400-e29b-41d4-a716-446655440001`).
   - Deterministic [BaselineHotspotDetectionEngine.java](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/hotspot/BaselineHotspotDetectionEngine.java) actively handling non-Pune cities (Mumbai, Delhi) with controlled degraded confidence flags.
3. **Spatial Provenance:**
   - Single spatial anchor: Uber H3 Resolution 8 hexadecimal string.
   - Feature snapshots persisted in `feature_snapshots` table with foreign key linkage in `hotspot_predictions.feature_snapshot_id`.
4. **Rich Multi-Sensor Context:**
   - [HotspotContextService.java](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/hotspot/HotspotContextService.java) pulling physical air pollutant telemetry, Open-Meteo weather parameters, GIS receptor counts, and MODIS/VIIRS fire decay scores without fabricated values.

---

## 4. F3-C1 Contract Audit

The end-to-end data pipeline was traced across every tier:
```
Frontend (React Hotspots Component & useHotspots Hook)
   ↓ HTTP GET /api/v1/hotspots?cityId={cityId} & /api/v1/hotspots/{h3Index}
HotspotController.java
   ↓
HotspotService.java
   ↓
HotspotDetectionEngine (MLHotspotDetectionEngine / BaselineHotspotDetectionEngine)
   ↓
ModelFeatureVectorAdapter (36 features strictly ordered)
   ↓
AiServiceHotspotClient (FastAPI /api/v1/ml/hotspot/predict or predict_cli.py)
   ↓
Artifact: hotspot_classifier_v1.joblib (CalibratedClassifierCV / RandomForest)
   ↓
Response: probability [0.0, 1.0], riskLevel, confidence, operationalThreshold (0.20)
   ↓
Database Persistence: HotspotPrediction Entity (PostgreSQL Table: hotspot_predictions)
   ↓
HotspotContextService.java (Attaches AirContext, WeatherContext, GIS, Dispersion)
   ↓
HotspotCellDto & HotspotOverviewResponse
   ↓ JSON REST Output
Frontend State Store & Map Renderer
```

---

## 5. Field-by-Field Contract Verification

The table below lists each of the 15 contract fields, its architectural source, representation across layers, and verification status:

| # | Field Name | Architectural Source | Backend Java / DB Representation | Frontend TS Representation | Downstream F4 Availability | Status |
|---|---|---|---|---|---|---|
| 1 | `predictionId` | DB Primary Key (`gen_random_uuid()`) | `HotspotPrediction.id` / `hotspot_predictions.id` / `HotspotCellDto.predictionId` | `HotspotCell.predictionId: string` | Primary foreign key anchor for F4 | **PASS** |
| 2 | `cityId` | Foreign Key (`cities.id`) | `HotspotPrediction.cityId` / `HotspotCellDto.cityId` / `HotspotOverviewResponse.cityId` | `HotspotOverviewResponse.cityId: string` | City partitioning key | **PASS** |
| 3 | `h3Index` / `h3CellId` | Canonical Uber H3 Res 8 Hex | `HotspotPrediction.h3Index` / `grid_cells.h3_index` / `HotspotCellDto.h3Index` | `HotspotCell.h3Index: string` | Canonical spatial anchor | **PASS** |
| 4 | `isHotspot` | ML Model Decision Rule ($p \ge \theta$) | `HotspotCellDto.isHotspot` / `HotspotSpatialContext.isHotspot` | `HotspotCell.isHotspot: boolean` | Hotspot filtering & alert trigger | **PATCH** |
| 5 | `riskScore` | Calibrated probability [0.0, 1.0] | `HotspotPrediction.riskScore` / `HotspotCellDto.riskScore` | `HotspotCell.riskScore: number` | Continuous risk weight | **PASS** |
| 6 | `operationalThreshold` | Deployed ML config / engine metadata | `HotspotCellDto.operationalThreshold` / `HotspotOverviewResponse.operationalThreshold` | `HotspotCell.operationalThreshold: number` | Model decision auditability | **PATCH** |
| 7 | `riskLevel` | 4-tier categorical tier (LOW/MOD/HIGH/CRIT) | `HotspotPrediction.riskLevel` / `HotspotCellDto.riskLevel` | `HotspotCell.riskLevel: HotspotRiskLevel` | Priority categorization | **PASS** |
| 8 | `overallConfidence` | Epistemic uncertainty composite [0.0, 1.0] | `HotspotPrediction.confidence` / `HotspotCellDto.confidence` | `HotspotCell.confidence: number` | F3 hotspot prediction confidence | **PASS** |
| 9 | `confidenceBreakdown` | Sensor quality, coverage, model certainty | `HotspotSpatialContext.ConfidenceBreakdown` | `HotspotSpatialContext.confidenceBreakdown` | F3 confidence breakdown (optional for F4) | **PATCH** |
| 10 | `spatialCoverageConfidence` | Distance decay to nearest monitor | `MonitoringCoverageContext.spatialCoverageConfidence` | `MonitoringCoverageContext.spatialCoverageConfidence` | Spatial sensor sparsity detection | **PATCH** |
| 11 | `featureSnapshotId` | Foreign Key (`feature_snapshots.id`) | `HotspotPrediction.featureSnapshotId` / `HotspotCellDto.featureSnapshotId` | `HotspotCell.featureSnapshotId: string` | Feature provenance linkage | **PASS** |
| 12 | `modelVersion` | Runtime model identifier | `HotspotPrediction.modelVersion` (`hotspot_classifier_v1`) | `HotspotCell.modelVersion: string` | Model lineage tracking | **PASS** |
| 13 | `engineType` | Execution provider flag | `HotspotCellDto.engineType` (`ML` vs `BASELINE`) | `HotspotCell.engineType: string` | Multi-city engine attribution | **PASS** |
| 14 | `generatedAt` / `predictedAt` | Inference execution timestamp | `HotspotPrediction.predictedAt` / `HotspotCellDto.predictedAt` | `HotspotCell.predictedAt: string` | Temporal alignment with F4 | **PASS** |
| 15 | `freshness` / `status` | Age-based SLA categorization | `HotspotCellDto.freshness` (`LIVE`, `STALE`, `UNAVAILABLE`) | `HotspotCell.freshness: HotspotFreshness` | Data freshness gating | **PASS** |

---

## 6. F3-C2 Forecast Attachment Contract

The exact, locked contract binding F3 hotspot predictions to downstream F4 forecast consumption is defined below.

### Explicit Boundary Principles:
1. **Producer Responsibility (F3):**
   - F3 strictly produces the Hotspot Prediction Anchor (identity, cell, snapshot reference, threshold, probability, and F3 hotspot prediction confidence).
   - **F3 must NOT define forecast horizons, forecast values, or forecast intervals.**
2. **Consumer Responsibility (F4):**
   - **F4 exclusively owns:**
     - Forecast horizons
     - Forecast values (point estimates)
     - Forecast uncertainty (empirical prediction intervals)
     - Forecast-specific confidence and model metadata
3. **Confidence Ownership & Decoupling:**
   - `HotspotPrediction.confidence` represents the **F3 hotspot prediction confidence** ($[0.0, 1.0]$), reflecting input sensor completeness and classification certainty for hotspot emergence.
   - **Forecast confidence must NOT inherit the F3 confidence automatically.** F4 owns its own forecast-specific confidence and uncertainty estimations.
4. **Confidence Breakdown Governance:**
   - `confidenceBreakdown` is **optional for F4 consumption**, but remains an authoritative part of the F3 prediction and spatial-context contract for end-to-end traceability, spatial auditability, and downstream F5 explainability.

```text
HotspotPrediction (F3 Producer — LOCKED)
 ├── predictionId         : UUID (Primary Anchor FK)
 ├── cityId               : UUID (City Partitioning Key)
 ├── h3Index              : String (Canonical Spatial Anchor, Uber H3 Res 8 Hex)
 ├── featureSnapshotId    : UUID (Feature Provenance Link)
 ├── predictedAt          : Instant (Base Observation Timestamp T0)
 ├── isHotspot            : boolean (Emergence Flag: riskScore >= operationalThreshold)
 ├── operationalThreshold : double (0.20 ML / 0.40 Baseline)
 ├── riskScore            : double (Calibrated Probability p [0.0, 1.0])
 ├── riskLevel            : String ("LOW", "MODERATE", "HIGH", "CRITICAL")
 ├── modelVersion         : String ("hotspot_classifier_v1" / "hotspot-baseline-v1")
 ├── confidence           : double
 │      = F3 hotspot prediction confidence
 └── confidenceBreakdown  : F3 confidence breakdown (optional for F4 consumption)
        ├── overallConfidence
        ├── dataQualityScore
        ├── spatialCoverageConfidence
        ├── modelCertainty
        ├── nearestStationDistanceKm
        └── epistemicUncertaintyFlag

                ↓ Referenced By

F4 Forecast (Consumer — OWNS FORECAST)
 ├── parentPredictionId   : UUID (FK -> F3 predictionId)
 ├── cityId               : UUID (Matches F3 cityId)
 ├── h3Index              : String (Matches F3 canonical spatial index)
 └── F4-owned forecast fields:
       ├── forecastId                  : UUID
       ├── featureSnapshotId           : UUID (Provenance to features used for forecast)
       ├── forecastGeneratedAt         : Instant
       ├── forecastModelVersion        : String
       ├── forecastConfidence          : double (F4-owned forecast confidence; does NOT automatically inherit F3 confidence)
       └── horizons / predictions      : F4 forecast outputs & intervals
```

---

## 7. Minimal Corrections Performed

To resolve the fields marked `PATCH` without breaking backward compatibility or altering schemas:

1. **[HotspotCellDto.java](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/hotspot/HotspotCellDto.java):**
   - Added canonical record fields: `boolean isHotspot`, `Double operationalThreshold`.
   - Added backwards-compatible 14-parameter and 8-parameter constructors defaulting threshold logic (`0.20` for `hotspot_classifier_v1`, `0.40` for baseline).
2. **[HotspotOverviewResponse.java](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/hotspot/HotspotOverviewResponse.java):**
   - Added canonical record field: `Double operationalThreshold`.
   - Added backwards-compatible 9-parameter constructor.
3. **[HotspotSpatialContext.java](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/hotspot/HotspotSpatialContext.java):**
   - Added `boolean isHotspot`, `Double operationalThreshold`, and `ConfidenceBreakdown confidenceBreakdown` to main record.
   - Added `ConfidenceBreakdown` sub-record with `overallConfidence`, `dataQualityScore`, `spatialCoverageConfidence`, `modelCertainty`, `nearestStationDistanceKm`, and `epistemicUncertaintyFlag`.
   - Added `Double spatialCoverageConfidence` to `MonitoringCoverageContext`.
   - Added backwards-compatible 17-parameter constructor for existing callers.
4. **[HotspotService.java](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/hotspot/HotspotService.java):**
   - Populated `isHotspot` ($p \ge \theta$) and `operationalThreshold` when creating `HotspotCellDto` lists and `HotspotOverviewResponse`.
   - Handled single-cell retrieval in `getHotspotByH3(h3Index)` to ensure both cached and on-demand predictions carry threshold values.
5. **[HotspotContextService.java](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/hotspot/HotspotContextService.java):**
   - Mapped `spatialCoverageConfidence` from real sensor distance metrics and coverage gap flags into `MonitoringCoverageContext` and `ConfidenceBreakdown`.
6. **[BaselineHotspotDetectionEngine.java](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/hotspot/BaselineHotspotDetectionEngine.java):**
   - Explicitly added `"operationalThreshold": 0.40` and `"isHotspot": riskScore >= 0.40` into evaluation metadata for both degraded and full evaluation branches.
7. **[frontend/src/types/hotspot.ts](file:///c:/Users/lenovo/AeroSential/frontend/src/types/hotspot.ts):**
   - Added `isHotspot?: boolean` and `operationalThreshold?: number` to `HotspotCell`, `HotspotOverviewResponse`, and `HotspotSpatialContext`.
   - Added `ConfidenceBreakdown` interface and `spatialCoverageConfidence?: number | null` to `MonitoringCoverageContext`.
8. **[HotspotPhase7ContextTest.java](file:///c:/Users/lenovo/AeroSential/backend/src/test/java/com/aerosentinel/hotspot/HotspotPhase7ContextTest.java) & [HotspotIntegrationTest.java](file:///c:/Users/lenovo/AeroSential/backend/src/test/java/com/aerosentinel/hotspot/HotspotIntegrationTest.java):**
   - Added assertions verifying that `isHotspot` correctly flips at the decision boundary ($p < \theta \implies \text{false}$, $p \ge \theta \implies \text{true}$) and that `operationalThreshold` is returned in REST endpoints.

---

## 8. Regression Test Results Summary

| Suite / Component | Scope | Command | Total Tests | Passed | Failed | Errors | Skipped | Status |
|---|---|---|---|---|---|---|---|---|
| Backend Full Regression | F1, F2, F3 Complete | `.\mvnw.cmd test` | 189 | 188 | 0 | 0 | 1* | **PASS** |
| Hotspot Integration & Context | F3 Integration & REST | `.\mvnw.cmd test -Dtest=HotspotPhase7ContextTest` | 7 | 7 | 0 | 0 | 0 | **PASS** |
| Hotspot Domain & Hardening | F3 Engines & Resilience | `.\mvnw.cmd test "-Dtest=HotspotDomainUnitTest,..."` | 38 | 38 | 0 | 0 | 0 | **PASS** |
| Frontend Type & Production Build | TypeScript + Vite | `npm run build` | - | - | 0 | 0 | - | **PASS** |
| Frontend Contract Unit Tests | UI Contracts & State | `npx tsx --test src/utils/hotspot.test.ts` | 23 | 23 | 0 | 0 | 0 | **PASS** |
| Python ML & Feature Contract | ML Inference & Features | `python -m pytest tests/test_f3_*.py` | 25 | 25 | 0 | 0 | 0 | **PASS** |

*\*Note: 1 test in `LiveOpenAqIngestionVerificationTest` was intentionally skipped due to unconfigured external API keys in standard build environments.*

---

## 9. F2 Regression Results

All F2 spatial and weather components were tested and showed zero regressions:
- **`com.aerosentinel.spatial.H3ServiceTest`**: 12 tests passed (H3 res 8 hexagon derivation, centroid conversion, boundary resolution).
- **`com.aerosentinel.grid.F2GridApiContractTest`**: 12 tests passed (H3 cell geometries, city boundaries, GeoJSON export).
- **`com.aerosentinel.grid.F2CellObservationApiContractTest`**: 10 tests passed (dual-sensor air + weather aggregation by H3 cell).
- **`com.aerosentinel.weather.F2WeatherApiContractTest`**: 7 tests passed (Open-Meteo telemetry mapping, atmospheric stagnation metrics).
- **`com.aerosentinel.integration.weather.WeatherSpatialIntegrationTest`**: 6 tests passed (multi-city weather ingestion, duplicate avoidance).

---

## 10. F3 Regression Results

All 45 dedicated F3 tests passed with 100% success rate:
- **`com.aerosentinel.hotspot.HotspotDomainUnitTest`** (13 tests): Risk score boundary tests, threshold boundary tests, confidence penalty calculations.
- **`com.aerosentinel.hotspot.MLHotspotDetectionEngineTest`** (9 tests): Strict 36-feature vector adaptation, unit conversion ($km/h \rightarrow m/s$), PMR domain guard, fallback handling.
- **`com.aerosentinel.hotspot.HotspotPhase6HardeningTest`** (11 tests): Stale observation handling, fail-safe fallback, multi-city degraded confidence.
- **`com.aerosentinel.hotspot.HotspotPhase7ContextTest`** (7 tests): Hotspot context assembly, `isHotspot` threshold validation, confidence breakdown, downstream attachment contracts.
- **`com.aerosentinel.hotspot.HotspotIntegrationTest`** (5 tests): Full REST mock MVC API contract verification for `/api/v1/hotspots` and `/api/v1/hotspots/{h3Index}`.

---

## 11. Frontend Build Results

The React + TypeScript frontend was compiled to production bundles with zero errors:
```
> aerosentinel-frontend@1.0.0 build
> tsc -b && vite build

vite v5.4.21 building for production...
transforming...
✓ 2549 modules transformed.
rendering chunks...
computing gzip size...
dist/index.html                     1.21 kB │ gzip:   0.66 kB
dist/assets/index-HCKc7b8T.css     32.19 kB │ gzip:   6.80 kB
dist/assets/index-w2KrwU9I.js   1,267.89 kB │ gzip: 352.25 kB
✓ built in 23.75s
```
Frontend contract tests via `npx -y tsx --test src/utils/hotspot.test.ts` passed 23/23 tests in 562ms.

---

## 12. Python ML Results

All 25 Python AI/ML tests executed cleanly under Python 3.13:
```
tests/test_f3_feature_contract.py::test_01_feature_count PASSED                         [  4%]
tests/test_f3_feature_contract.py::test_02_feature_names_and_order_match_joblib PASSED [  8%]
tests/test_f3_feature_contract.py::test_04_datatypes_numeric PASSED                     [ 12%]
tests/test_f3_feature_contract.py::test_05_06_07_wind_conversion_and_vector_derivation PASSED [ 16%]
tests/test_f3_feature_contract.py::test_08_spatial_lag_leave_one_out PASSED            [ 20%]
tests/test_f3_feature_contract.py::test_09_10_monitoring_coverage_gap PASSED           [ 24%]
tests/test_f3_feature_contract.py::test_11_temporal_features_cyclical PASSED            [ 28%]
tests/test_f3_feature_contract.py::test_12_13_fire_features_and_no_future_leakage PASSED [ 32%]
tests/test_f3_feature_contract.py::test_14_15_16_satellite_features_and_cloud_filtering PASSED [ 36%]
tests/test_f3_feature_contract.py::test_17_18_gis_features PASSED                       [ 40%]
tests/test_f3_feature_contract.py::test_19_missing_feature_handling PASSED             [ 44%]
tests/test_f3_feature_contract.py::test_20_deterministic_output PASSED                  [ 48%]
tests/test_f3_ml_inference.py::test_artifact_loading_and_metadata PASSED               [ 52%]
tests/test_f3_ml_inference.py::test_feature_names_and_order_exact PASSED               [ 56%]
tests/test_f3_ml_inference.py::test_wind_unit_boundary_conversion PASSED               [ 60%]
tests/test_f3_ml_inference.py::test_fastapi_model_info PASSED                           [ 64%]
tests/test_f3_ml_inference.py::test_fastapi_pune_inference PASSED                       [ 68%]
tests/test_f3_ml_inference.py::test_fastapi_unsupported_domain_mumbai PASSED           [ 72%]
tests/test_f3_ml_inference.py::test_fastapi_unsupported_domain_delhi PASSED            [ 76%]
tests/test_f3_ml_inference.py::test_fastapi_missing_features_insufficient_data PASSED  [ 80%]
tests/test_f3_ml_inference.py::test_predict_cli_runner PASSED                           [ 84%]
tests/test_f3_ml_inference.py::test_fastapi_non_finite_features PASSED                  [ 88%]
tests/test_f3_ml_inference.py::test_fastapi_missing_individual_copollutants PASSED     [ 92%]
tests/test_f3_ml_inference.py::test_fastapi_wrong_feature_count_array PASSED           [ 96%]
tests/test_f3_ml_inference.py::test_fastapi_repeated_inference_determinism PASSED      [100%]
============================== 25 passed in 23.09s ===============================
```

---

## 13. Runtime / API Proof

### Execution Command:
```bash
mvn test -Dtest=HotspotIntegrationTest#testGetSingleCellHotspot
```
**Request:** `GET /api/v1/hotspots/88608850e5fffff` with `Content-Type: application/json`

### Actual Log Output & Verbatim JSON Excerpt:
```text
2026-09-27T11:54:47.366+05:30 INFO 40136 --- [aerosentinel-backend] [ main] c.a.hotspot.HotspotIntegrationTest : API_RUNTIME_RESPONSE_EXCERPT: {"h3Index":"88608850e5fffff","gridCellId":"955f1f67-e5ff-48ef-87b6-133ff958b756","riskScore":0.7998,"riskLevel":"CRITICAL","confidence":0.86,"predictedAt":"2026-09-26T13:09:44.571028Z","freshness":"STALE","modelVersion":"hotspot_classifier_v1","predictionId":"a310c689-f340-49fc-8935-a037de8d7709","cityId":"550e8400-e29b-41d4-a716-446655440001","cityName":"Pune","engineType":"ML","featureSnapshotId":"1624baa3-a5f8-407b-b1c2-36bcee7650b1","spatialContext":{"predictionId":"a310c689-f340-49fc-8935-a037de8d7709","h3Index":"88608850e5fffff","cityId":"550e8400-e29b-41d4-a716-446655440001","cityName":"Pune","featureSnapshotId":"1624baa3-a5f8-407b-b1c2-36bcee7650b1","predictedAt":"2026-09-26T13:09:44.571028Z","riskScore":0.7998,"riskLevel":"CRITICAL","confidence":0.86,"engineType":"ML","modelVersion":"hotspot_classifier_v1","freshness":"STALE","airContext":{"dataQuality":"VALID","pm25":78.0,"pm10":120.0,"no2":37.0,"so2":14.0,"co":0.9,"o3":24.0,"observedAt":"2026-09-24T22:00:00Z","stationId":"PUN-001","recentPm25Mean24h":78.0},"weatherContext":{"dataQuality":"VALID","temperature":30.1,"humidity":52.0,"windSpeedKmh":11.9,"windSpeedMps":3.31,"windDirection":271.0,"surfacePressure":949.8,"precipitation":0.0,"observedAt":"2026-09-27T06:30:00Z"},"monitoringCoverage":{"dataQuality":"VALID","nearestStationDistanceKm":0.27,"stationsWithin5kmCount":2,"monitoringCoverageGapFlag":0,"spatialCoverageConfidence":0.95},"spatialDispersion":{"dataQuality":"VALID","pm25SpatialLagMean":78.0,"windU":4.3604,"windV":0.0761},"environmentalGis":{"dataQuality":"VALID","distToNearestIndustrialKm":3.5,"distToNearestMajorRoadKm":0.4,"sensitiveReceptorsCount2km":4,"industrialZoneWithin2kmFlag":0,"fireCount24h25km":0,"fireFrpSum24h25km":0.0,"fireFrpMean24h25km":0.0,"nearestFireDistanceKm":50.0,"fireFrpDistanceDecay":0.0,"fireUpwindAlignmentScore":0.0},"isHotspot":true,"operationalThreshold":0.2,"confidenceBreakdown":{"overallConfidence":0.86,"dataQualityScore":0.9,"spatialCoverageConfidence":0.95,"modelCertainty":0.8,"nearestStationDistanceKm":0.27,"epistemicUncertaintyFlag":0}},"isHotspot":true,"operationalThreshold":0.2}
```

### Formatted REST Proof Excerpt:
```json
{
  "predictionId": "a310c689-f340-49fc-8935-a037de8d7709",
  "cityId": "550e8400-e29b-41d4-a716-446655440001",
  "cityName": "Pune",
  "h3Index": "88608850e5fffff",
  "gridCellId": "955f1f67-e5ff-48ef-87b6-133ff958b756",
  "featureSnapshotId": "1624baa3-a5f8-407b-b1c2-36bcee7650b1",
  "riskScore": 0.7998,
  "riskLevel": "CRITICAL",
  "confidence": 0.86,
  "isHotspot": true,
  "operationalThreshold": 0.20,
  "modelVersion": "hotspot_classifier_v1",
  "engineType": "ML",
  "predictedAt": "2026-09-26T13:09:44.571028Z",
  "freshness": "STALE"
}
```

---

## 14. Database Proof

### Database Query:
```sql
SELECT 
    hp.id AS prediction_id,
    hp.city_id,
    hp.h3_index,
    hp.risk_score,
    hp.risk_level,
    hp.confidence,
    hp.model_version,
    hp.feature_snapshot_id,
    hp.predicted_at,
    fs.id AS snapshot_id,
    fs.feature_schema_version,
    fs.quality_status
FROM hotspot_predictions hp
JOIN feature_snapshots fs ON hp.feature_snapshot_id = fs.id
WHERE hp.city_id = '550e8400-e29b-41d4-a716-446655440001'
ORDER BY hp.predicted_at DESC
LIMIT 1;
```

### Actual Log Output From Runtime Test Execution:
```text
2026-09-27T11:54:47.115+05:30 INFO 40136 --- [aerosentinel-backend] [ main] c.a.hotspot.HotspotIntegrationTest : DATABASE_ROW_VERIFICATION: id=8e8bab50-ca1c-4abc-bdba-fd9dc221e916, cityId=550e8400-e29b-41d4-a716-446655440001, h3Index=8860885357fffff, riskScore=0.8892, riskLevel=CRITICAL, confidence=0.882, modelVersion=hotspot_classifier_v1, featureSnapshotId=c8a0ed0d-54f2-4c78-b9ff-f34640ce4509, predictedAt=2026-09-26T13:08:48.118968Z
```

### Verified Table Excerpt:
| Column | Value | Verification Notes |
|---|---|---|
| `predictionId` (`id`) | `8e8bab50-ca1c-4abc-bdba-fd9dc221e916` | Non-null unique primary key |
| `cityId` (`city_id`) | `550e8400-e29b-41d4-a716-446655440001` | Pune city UUID |
| `h3Index` (`h3_index`) | `8860885357fffff` | Canonical Uber H3 Res 8 index |
| `featureSnapshotId` (`feature_snapshot_id`) | `c8a0ed0d-54f2-4c78-b9ff-f34640ce4509` | Foreign key referencing `feature_snapshots.id` |
| `riskScore` (`risk_score`) | `0.8892` | Calibrated model probability $p \ge 0.20$ |
| `riskLevel` (`risk_level`) | `CRITICAL` | Operational tier matching probability |
| `confidence` | `0.882` | F3 hotspot prediction confidence |
| `modelVersion` (`model_version`) | `hotspot_classifier_v1` | Runtime metadata from deployed ML artifact |
| `predictedAt` (`predicted_at`) | `2026-09-26T13:08:48.118968Z` | Base observation anchor timestamp |

**Lineage Verification:**  
`predictionId` (`8e8bab50...`) $\longleftrightarrow$ `featureSnapshotId` (`c8a0ed0d...`) $\longleftrightarrow$ `h3Index` (`8860885357fffff`) is 100% matched and persistent in PostgreSQL.

---

## 15. F3 $\rightarrow$ F4 Contract Diagram

```mermaid
graph TD
    subgraph F3_Producer ["F3 Hotspot Detection Engine (PRODUCER — LOCKED)"]
        FS[Feature Snapshot<br/>ID: featureSnapshotId<br/>36 Derived Sensor Features] --> INF[ML Inference Engine<br/>hotspot_classifier_v1<br/>Operational Threshold: 0.20]
        INF --> PRED[Hotspot Prediction Record<br/>predictionId: UUID<br/>cityId: UUID<br/>h3Index: Hex Res 8<br/>featureSnapshotId: UUID<br/>predictedAt: Instant<br/>isHotspot: boolean<br/>operationalThreshold: 0.20<br/>riskScore: 0.8892<br/>riskLevel: CRITICAL<br/>confidence: 0.882 (F3 hotspot prediction confidence)<br/>confidenceBreakdown: sub-record (optional for F4)<br/>modelVersion: hotspot_classifier_v1]
        PRED --> DB[(PostgreSQL: hotspot_predictions)]
        PRED --> REST[REST API: /api/v1/hotspots]
    end

    subgraph F4_Consumer ["F4 Forecast Engine (CONSUMER — OWNS FORECAST)"]
        REST -.-> F4_INGEST[Forecast Ingestion Task]
        DB -.-> F4_INGEST
        F4_INGEST --> ATTACH{Forecast Attachment Binding<br/>parentPredictionId + cityId + h3Index}
        ATTACH --> F4_OWNED[F4-Owned Engine<br/>- Forecast Horizons<br/>- Forecast Predictions<br/>- Forecast Uncertainty<br/>- Forecast-Specific Confidence (Independent of F3)]
    end
```

---

## 16. Files Changed

Only minimal contract files were changed:
1. `backend/src/main/java/com/aerosentinel/hotspot/HotspotCellDto.java`
2. `backend/src/main/java/com/aerosentinel/hotspot/HotspotOverviewResponse.java`
3. `backend/src/main/java/com/aerosentinel/hotspot/HotspotSpatialContext.java`
4. `backend/src/main/java/com/aerosentinel/hotspot/HotspotService.java`
5. `backend/src/main/java/com/aerosentinel/hotspot/HotspotContextService.java`
6. `backend/src/main/java/com/aerosentinel/hotspot/BaselineHotspotDetectionEngine.java`
7. `backend/src/test/java/com/aerosentinel/hotspot/HotspotIntegrationTest.java`
8. `backend/src/test/java/com/aerosentinel/hotspot/HotspotPhase7ContextTest.java`
9. `frontend/src/types/hotspot.ts`

---

## 17. Files NOT Changed

The following critical files were intentionally preserved and not modified:
- `backend/src/main/resources/db/migration/*` (Zero Flyway schema changes; existing schema fully supports foreign keys and JSON contexts).
- `ai-service/ml/artifacts/hotspot_classifier_v1.joblib` (Model artifact preserved intact).
- `ai-service/ml/inference/predict_cli.py` (CLI inference runner preserved intact).
- `ai-service/ml/inference/confidence.py` (Confidence math preserved intact).
- `backend/src/main/java/com/aerosentinel/hotspot/MLHotspotDetectionEngine.java` (ML engine logic preserved intact).
- `backend/src/main/java/com/aerosentinel/hotspot/ModelFeatureVectorAdapter.java` (36-feature vector mapping preserved intact).
- `backend/src/main/java/com/aerosentinel/spatial/H3Service.java` (Canonical Uber H3 spatial resolution 8 engine preserved intact).
- `backend/src/main/java/com/aerosentinel/weather/*` & `com/aerosentinel/air/*` (F1/F2 domain models and repositories preserved intact).

---

## 18. Risks / Known Limitations

1. **PMR Geographic Specialization:** The machine learning artifact `hotspot_classifier_v1.joblib` was trained strictly for Pune Metropolitan Region (PMR). Non-Pune cities (Mumbai, Delhi) automatically route to `BaselineHotspotDetectionEngine` (`hotspot-baseline-v1`) with lower baseline confidence (~0.35–0.45) due to lack of local multi-pollutant co-location. F4 must respect `engineType` and `confidence` when producing multi-city forecasts.
2. **Operational Threshold Semantics:** The operational threshold for ML is fixed at `0.20` based on model calibration and environmental cost-matrix optimization. It must not be confused with `0.50` binary probability. Any forecast consuming this prediction must treat $p \ge 0.20$ as active hotspot emergence.
3. **Database Concurrency:** Ingestion and prediction generation use idempotent upserts anchored on `(h3_index, predicted_at)`.

---

## 19. Final F3 Lock Decision

### Lock Statement:
- **F3 hotspot inference is production-integrated for its supported geography.**
- **F3 prediction contract is stable.**
- **`predictionId` is stable and downstream-addressable.**
- **`h3Index` is the single shared spatial key.**
- **`featureSnapshotId` snapshot identity is traceable.**
- **Threshold and `isHotspot` semantics are fixed and verified.**
- **Confidence semantics represent F3 hotspot prediction confidence.**
- **`confidenceBreakdown` is optional for F4, but authoritative for F3 traceability and F5 explainability.**
- **Forecast confidence does NOT inherit F3 confidence automatically.**
- **F4 exclusively owns all forecast horizons, values, intervals, and metadata.**
- **`modelVersion` is traceable.**
- **F4 can attach forecast records to an exact F3 prediction.**
- **F2 regression passed.**
- **F3 regression passed.**
- **Frontend build passed.**
- **Python ML tests passed.**

> **MANDATE:** F3 must not be structurally modified during F4/F5 development. Only bug fixes are permitted after lock.

---

## 20. F4 Handoff Notes

When developing Feature 4 (PM2.5 Multi-Horizon Forecast Engine), adhere strictly to these handoff boundaries:
1. **Primary Attachment Identity:**
   Every F4 forecast entity must include:
   ```java
   UUID parentPredictionId;   // HotspotPrediction.id
   String h3Index;            // Uber H3 Res 8
   UUID cityId;               // City foreign key
   UUID featureSnapshotId;    // Feature snapshot foreign key
   Instant baseObservedAt;    // Time anchor T0
   ```
2. **F4 Forecast Ownership & Confidence Decoupling:**
   - F4 owns all forecasting logic, horizons, point estimates, confidence intervals, and prediction intervals.
   - Forecast confidence must **NOT** automatically inherit F3 hotspot prediction confidence. F4 determines its own forecast uncertainty.
   - `confidenceBreakdown` is optional for F4 consumption, but remains part of the authoritative F3 prediction and spatial context contract for system auditability and F5 explainability.
3. **Forecast Ingestion Query:**
   F4 should retrieve active predictions using:
   ```java
   hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(h3Index)
   ```
   or query batch predictions by `cityId` where `isHotspot == true` and `freshness == "LIVE"`.
4. **Spatial Decoupling:**
   Do not create secondary spatial keys or alter the H3 resolution. F4 runs strictly on the established Resolution 8 H3 grid.

---

## Machine-Readable Lock Summary

```json
F3_LOCK_SUMMARY = {
  "contractAudit": "PASS",
  "forecastAttachment": "PASS",
  "f2Regression": "PASS",
  "f3Regression": "PASS",
  "frontendBuild": "PASS",
  "pythonML": "PASS",
  "runtimeProof": "PASS",
  "databaseProof": "PASS",
  "f3Status": "LOCKED"
}
```
