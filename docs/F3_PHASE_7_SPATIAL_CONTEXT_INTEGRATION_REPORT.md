# AeroSentinel — F3 Phase 7 Engineering Report: Hotspot Spatial Context & Cross-Feature Integration Contract

**Status**: PASS  
**Phase**: F3 Phase 7  
**Artifact Version**: 1.0.0  
**Timestamp**: 2026-09-26T22:05:00+05:30  
**Scope**: H3-Centric Spatial Context Contract & Downstream Integration Readiness (F4 Forecast & F5 Evidence)

---

## 1. Objective

Phase 7 establishes a clean, authoritative internal context object (`HotspotSpatialContext`) connecting the selected H3 cell's hotspot prediction to physical air/weather telemetry, spatial dispersion, GIS, and monitoring coverage without creating duplicate location identifiers or fake context.

Crucially:
- **No new prediction model** is introduced; the 36-feature ML contract (`hotspot_classifier_v1.joblib`) and operational threshold (`0.20`) remain untouched.
- F3 hotspot predictions are made structurally and semantically ready for **F4 Forecast** and **F5 Evidence + Gemini** attachment.
- The Uber H3 index (Resolution 8) remains the single immutable spatial anchor across all features.
- Zero browser usage was employed; verification was performed strictly through code inspection, unit/contract tests, and live API curl telemetry.

---

## 2. F2 → F3 Continuity

Phase 7 strictly preserves and builds upon the spatial data model established in F2 and hardened in F3 Phases 1–6:
- **Spatial Resolution**: Uber H3 Resolution 8 (`~461m` edge length, `~0.737 km²` area).
- **Spatial Consistency**: The same H3 cell identifier indexes:
  - Real air observations (`AirObservation` table).
  - Multi-station weather telemetry (`WeatherObservation` table from Open-Meteo).
  - Spatial grid metadata (`GridCell` table with PostGIS geometry).
  - Feature snapshots (`FeatureSnapshot` table with 36-feature vector).
  - Hotspot prediction instances (`HotspotPrediction` table).
- **No Parallel Grid**: No secondary geospatial coordinate system, tile scheme, or bounding box abstraction was introduced.

---

## 3. H3-Centered Context Architecture

Every selected hotspot H3 cell resolves to a single unified spatial hierarchy:

```
H3 Cell (Res 8, e.g. "88608850e5fffff")
  ├── Air Telemetry (Real PM2.5, PM10, NO2, SO2, CO, O3, Station ID)
  ├── Weather Telemetry (Temp, Humidity, Wind Speed/Dir, Pressure, Rain)
  ├── Feature Snapshot (Auditable 36-feature immutable snapshot)
  ├── Hotspot Prediction (riskScore, riskLevel, confidence, engineType)
  └── Downstream Attachment Points
        ├── F4 Forecast (predictionId + h3Index)
        └── F5 Evidence (predictionId + h3Index + observedContext)
```

No alternate location identifier exists or is permitted.

---

## 4. Prediction Context Contract (`HotspotSpatialContext`)

A clean, immutable Java domain record and corresponding TypeScript interface were created:

```java
public record HotspotSpatialContext(
    UUID predictionId,
    String h3Index,
    UUID cityId,
    String cityName,
    UUID featureSnapshotId,
    Instant predictedAt,
    Double riskScore,
    String riskLevel,
    Double confidence,
    String engineType,       // "ML" or "BASELINE"
    String modelVersion,     // "hotspot_classifier_v1" or "hotspot-baseline-v1"
    String freshness,        // "LIVE", "STALE", "NO_DATA", "UNAVAILABLE"
    AirContext airContext,
    WeatherContext weatherContext,
    MonitoringCoverageContext monitoringCoverage,
    SpatialDispersionContext spatialDispersion,
    EnvironmentalGisContext environmentalGis
) {}
```

### Context Sub-records & Data Quality Indicators
1. **`AirContext`**: `dataQuality`, `pm25`, `pm10`, `no2`, `so2`, `co`, `o3`, `observedAt`, `stationId`, `recentPm25Mean24h`.
2. **`WeatherContext`**: `dataQuality`, `temperature`, `humidity`, `windSpeedKmh`, `windSpeedMps`, `windDirection`, `surfacePressure`, `precipitation`, `observedAt`.
3. **`MonitoringCoverageContext`**: `dataQuality`, `nearestStationDistanceKm`, `stationsWithin5kmCount`, `monitoringCoverageGapFlag`.
4. **`SpatialDispersionContext`**: `dataQuality`, `pm25SpatialLagMean`, `windU`, `windV`.
5. **`EnvironmentalGisContext`**: `dataQuality`, `distToNearestIndustrialKm`, `distToNearestMajorRoadKm`, `sensitiveReceptorsCount2km`, `industrialZoneWithin2kmFlag`, `fireCount24h25km`, `fireFrpSum24h25km`, `fireFrpMean24h25km`, `nearestFireDistanceKm`, `fireFrpDistanceDecay`, `fireUpwindAlignmentScore`.

---

## 5. F3 → F4 Attachment Contract

When the F4 Forecast feature is implemented, it will attach directly to F3 via:

```
F3 Prediction
    ↓
(predictionId, h3Index)
    ↓
F4 Forecast Module
```

- **Primary Input Keys**: `predictionId` (UUID) + `h3Index` (String).
- **Temporal Horizon**: F4 forecasts the evolution of pollution/risk for the specific `h3Index` starting from `predictedAt`.
- **Decoupling**: F4 does not query raw database records or unvalidated inputs; it receives validated F3 prediction identifiers and spatial metadata.

---

## 6. F3 → F5 Attachment Contract

When the F5 Evidence & Gemini feature is implemented, it will attach via:

```
F3 Prediction (predictionId, h3Index)
       +
F4 Forecast (forecastId, horizon)
       +
Observed Context (HotspotSpatialContext)
       ↓
F5 Evidence Engine (Audited Evidence Bundle + Gemini LLM Reasoning)
```

- **Zero Hallucination Guarantee**: F5 feeds only the audited `HotspotSpatialContext` into the Gemini prompt.
- **Traceability**: The resulting explanation cites `predictionId`, `featureSnapshotId`, actual air readings, and real weather dispersion parameters.

---

## 7. Freshness Contract

The temporal lifecycle is strictly locked across F2, F3, and future downstream features:

| Freshness Enum | Condition | Meaning |
| :--- | :--- | :--- |
| **`LIVE`** | `age <= 2 hours` | Highly fresh, current telemetry & prediction |
| **`STALE`** | `2 hours < age <= 24 hours` | Operational telemetry exists but has aged; fallback eligible |
| **`UNAVAILABLE`**| `age > 24 hours` | Telemetry or prediction is deprecated; regeneration required |
| **`NO_DATA`** | `timestamp == null` or empty cells | No telemetry or predictions available |

---

## 8. Data Quality Contract

Downstream consumers (F4 & F5) require explicit signal validity. The following contract is enforced:

| Status | Meaning | Downstream Treatment |
| :--- | :--- | :--- |
| **`VALID`** | Telemetry / feature is measured or reliably derived | Safe for numerical modeling and prompt generation |
| **`MISSING`** | Sensor / feature was not recorded or station offline | Must remain `null` or explicit `MISSING`; **NEVER** replace with fake zero or random value |
| **`UNAVAILABLE`**| Upstream service failed or feature is inaccessible | Downstream feature must gracefully degrade |

---

## 9. Multi-City Contract

The platform explicitly differentiates between validated ML and baseline fallback cities:

- **Pune (`550e8400-e29b-41d4-a716-446655440001`)**:
  - `engineType`: `"ML"`
  - `modelVersion`: `"hotspot_classifier_v1"`
  - Model threshold: `0.20`
  - Calibrated confidence: derived from Random Forest sigmoid calibration (`0.50 - 0.95`).
- **Mumbai (`...0002`) & Delhi (`...0003`)**:
  - `engineType`: `"BASELINE"`
  - `modelVersion`: `"hotspot-baseline-v1"`
  - Baseline degraded confidence: capped at `0.35`.
  - Missing co-pollutants (PM10, NO2, SO2, CO, O3) are explicitly returned as `null` with `dataQuality: "VALID"` for PM2.5 and weather.

---

## 10. API & DTO Stability

### Backward-Compatible Endpoints
1. `GET /api/v1/hotspots?cityId={cityId}`
   - Returns `HotspotOverviewResponse` containing `cells` with stable downstream keys (`predictionId`, `cityId`, `cityName`, `engineType`, `featureSnapshotId`).
2. `GET /api/v1/hotspots/{h3Index}`
   - Returns single `HotspotCellDto` enriched with `spatialContext` (`HotspotSpatialContext`).
3. `GET /api/v1/hotspots/{h3Index}/context` (NEW dedicated endpoint)
   - Returns direct `HotspotSpatialContext` for high-throughput headless downstream integration.

---

## 11. Verification & Test Suite

### Backend Unit & Integration Tests (`mvn test`)
- **`HotspotPhase7ContextTest`**: 6/6 tests passing:
  - `testSpatialContextContractFields`: Verifies all stable downstream keys and sub-context mappings.
  - `testRealAirAndWeatherContextPriority`: Confirms real telemetry takes precedence over snapshot fallbacks.
  - `testMissingSignalIntegrity`: Confirms missing signals remain `null` without fake zero substitution.
  - `testPuneMlMetadataIntegrity`: Confirms Pune ML engine type, model version, and confidence properties.
  - `testMumbaiBaselineDegradedMetadata`: Confirms Mumbai baseline engine type and degraded confidence cap (0.35).
  - `testSingleSpatialAnchorContinuity`: Confirms H3 is the exclusive spatial anchor across all contexts.
- **Full Backend Regression Suite**:
  - **189 tests run, 0 failures, 0 errors, 1 skipped** (`mvn test` BUILD SUCCESS).
  - Verified F2 grid, F2 weather, F3 Phase 2 feature snapshots, F3 Phase 5 ML inference, and F3 Phase 6 hardening.

### Python AI Service Tests (`pytest`)
- **13/13 tests passing**:
  - `test_artifact_loading_and_metadata`
  - `test_feature_names_and_order_exact`
  - `test_wind_unit_boundary_conversion`
  - `test_fastapi_model_info`
  - `test_fastapi_pune_inference`
  - `test_fastapi_unsupported_domain_mumbai`
  - `test_fastapi_unsupported_domain_delhi`
  - `test_fastapi_missing_features_insufficient_data`
  - `test_predict_cli_runner`
  - `test_fastapi_non_finite_features`
  - `test_fastapi_missing_individual_copollutants`
  - `test_fastapi_wrong_feature_count_array`
  - `test_fastapi_repeated_inference_determinism`

### Frontend TypeScript & Production Build
- `npm run build` (`tsc -b && vite build`): **0 errors**, built bundle in `26.40s`.

### Live API Telemetry (CURL)
- Verified `GET /api/v1/hotspots/88608850e5fffff` (Pune Shivajinagar): returns `predictionId`, `featureSnapshotId`, `engineType = "ML"`, `modelVersion = "hotspot_classifier_v1"`, and complete `spatialContext`.
- Verified `GET /api/v1/hotspots/88608850e5fffff/context`: returns standalone `HotspotSpatialContext`.
- Verified `GET /api/v1/hotspots/88608b56b3fffff/context` (Mumbai Kurla): returns `engineType = "BASELINE"`, `confidence = 0.35`, and explicit `null` for unmonitored pollutants.

---

## 12. Regression Analysis

- **Zero Regression**: All existing F2 and F3 Phase 1–6 functionality remains completely intact.
- **Contract Compatibility**: `HotspotCellDto` retains all legacy constructors and field getters; the newly added fields (`predictionId`, `cityId`, `cityName`, `engineType`, `featureSnapshotId`, `spatialContext`) are non-breaking additions.
- **Frontend Compatibility**: `frontend/src/types/hotspot.ts` was extended with optional properties matching backend DTOs without breaking existing `H3RiskLayer` or `PollutionMap` state management.

---

## 13. Known Limitations

1. **Station Density in Delhi/Mumbai**: Sensor coverage in secondary cities remains lower than Pune; fallback to baseline engine is by design.
2. **Postgres Ingestion Timing**: Telemetry older than 2 hours correctly triggers `STALE` status according to the freshness contract until the next ingestion cycle occurs.

---

## 14. Phase 8 Starting Point

Phase 8 will focus on:
1. Connecting the Phase 4 UI's selected-cell card with the rich `spatialContext` (showing real air readings, weather dispersion, and monitoring coverage).
2. End-to-end integration and final verification of the F3 Hotspot feature milestone.
3. Preparing the application for F4 Forecast feature kickoff.
