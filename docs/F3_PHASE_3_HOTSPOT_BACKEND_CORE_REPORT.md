# AeroSentinel — F3 Phase 3 Report: Hotspot Domain + Spring Boot Core

**Phase Status:** `PASS`  
**Execution Timestamp:** 2026-09-26  
**Hotspot Engine Type:** `BASELINE` (Deterministic Product-Side Engine)  
**Hotspot Model Version:** `hotspot-baseline-v1`  
**Database Migration:** `V9__f3_hotspot_predictions_feature_ref.sql`  

---

## 1. Phase Objective

The objective of F3 Phase 3 was to implement the product-side F3 Hotspot Domain and Spring Boot backend core, establishing a clean, provider-agnostic architecture so that the trained ML model (`hotspot_classifier_v1.joblib`) can be plugged in later without altering the domain, API, or frontend layers.

### Strict Boundaries Preserved:
- **NO ML Model Serving / Direct Inference:** No direct coupling to `scikit-learn` or `.joblib` inside controllers, repositories, or domain entities.
- **NO Hotspot UI:** Frontend React risk map remains deferred to Phase 4.
- **NO Synthetic / Fake ML Data:** Product-side engine is explicitly identified as `engineType = "BASELINE"` with documented, deterministic multi-criteria logic.
- **NO F4 / F5 Scope Creep:** Forecast horizons and Gemini explainability remained untouched.
- **F2 & Phase 2 Continuity:** Exact Uber H3 Resolution 8 spatial indexing and `feature_snapshots` provenance maintained.

---

## 2. Phase 2 → Phase 3 Data Continuity

Phase 3 consumes the auditable Phase 2 feature data layer directly:
```
F2 Air + Weather + H3
        ↓
F3 Feature Snapshots (`feature_snapshots`, schema: `f3-features-v1`)
        ↓
ModelFeatureVectorAdapter (36-feature validation & unit contract)
        ↓
HotspotDetectionEngine (Provider-Agnostic Interface)
        ↓
HotspotPredictionValidator (Integrity Gates)
        ↓
HotspotPrediction Persistence (`hotspot_predictions`)
        ↓
HotspotController (`/api/v1/hotspots`)
```

- **H3 Consistency:** Cell identifiers are preserved end-to-end (`88608850e5fffff` for Shivajinagar, etc.).
- **Provenance Link:** Every row in `hotspot_predictions` references `feature_snapshot_id` pointing to the exact 36-feature snapshot used for risk evaluation.

---

## 3. Hotspot Domain Architecture

The hotspot domain package was created under `com.aerosentinel.hotspot`:

| Component | Class / Interface | Responsibility |
|:---|:---|:---|
| **Risk Level Enum** | `HotspotRiskLevel` | Standard environmental classification (`LOW`, `MODERATE`, `HIGH`, `CRITICAL`) with deterministic score mapping. |
| **Prediction Result** | `HotspotPredictionResult` | Immutable evaluation container (`riskScore`, `riskLevel`, `confidence`, `modelVersion`, `engineType`, `metadata`). |
| **Engine Interface** | `HotspotDetectionEngine` | Provider-agnostic evaluation contract (`evaluate(FeatureSnapshot snapshot)`). |
| **Baseline Engine** | `BaselineHotspotDetectionEngine` | Deterministic product-side engine computing physical multi-criteria atmospheric risk scores. |
| **ML Vector Adapter** | `ModelFeatureVectorAdapter` | Normalization gate ensuring exact 36-feature ordering, numeric types, and unit normalization. |
| **Entity** | `HotspotPrediction` | JPA entity mapping `hotspot_predictions` table. |
| **Validator** | `HotspotPredictionValidator` | Integrity validator verifying score ranges $[0.0, 1.0]$, confidence ranges $[0.0, 1.0]$, timestamps, and non-null snapshot references. |
| **Repository** | `HotspotRepository` | Spring Data JPA repository with indexed queries (`findLatestByCityId`, `findTopByH3IndexOrderByPredictedAtDesc`). |
| **Service Layer** | `HotspotService` | Orchestrates request flow, snapshot resolution, evaluation, validation, persistence, and DTO transformation. |
| **REST Controller** | `HotspotController` | Public endpoints (`GET /api/v1/hotspots?cityId=...`, `GET /api/v1/hotspots/{h3Index}`). |

---

## 4. Engine Interface & Replaceability

The core detection interface decouples prediction evaluation from any underlying ML runtime:

```java
public interface HotspotDetectionEngine {
    String getEngineVersion();
    String getEngineType();
    HotspotPredictionResult evaluate(FeatureSnapshot snapshot);
}
```

- When the ML model is connected in later phases, an `MlHotspotDetectionEngine` will implement this same interface.
- Neither the `HotspotController` nor `HotspotService` has any knowledge of Python, scikit-learn, or serialization formats.

---

## 5. Current Baseline Engine Behavior

The product-side baseline engine (`BaselineHotspotDetectionEngine`) is explicitly identified:
- `engineType = "BASELINE"`
- `modelVersion = "hotspot-baseline-v1"`

### Deterministic Calculation Rules:
1. **Pollutant Concentration Burden:**
   $$S_{\text{pollutant}} = \min\left(1.0, 0.40 \cdot \frac{\text{PM}_{10}}{100} + 0.30 \cdot \frac{\text{NO}_2}{80} + 0.30 \cdot \frac{\text{PM}_{2.5}^{\text{lag}}}{60}\right)$$
2. **Atmospheric Stagnation Factor:**
   $$S_{\text{stagnation}} = \begin{cases} 
   1.00 & \text{if } v < 1.0\text{ m/s} \text{ (stagnant air)} \\ 
   0.75 & \text{if } v < 2.5\text{ m/s} \\ 
   0.45 & \text{if } v < 5.0\text{ m/s} \\ 
   0.20 & \text{if } v \ge 5.0\text{ m/s} \text{ (ventilated)} 
   \end{cases}$$
3. **Fire Decay & Industrial Proximity:**
   $$S_{\text{fire}} = \min\left(1.0, \frac{\text{fire\_frp\_distance\_decay}}{25.0}\right), \quad S_{\text{industrial}} = \begin{cases} 1.0 & \text{if zone } \le 2\text{ km} \\ 0.2 & \text{otherwise} \end{cases}$$
4. **Aggregate Risk Score:**
   $$\text{riskScore} = \min(1.0, \max(0.0, 0.60 \cdot S_{\text{pollutant}} + 0.25 \cdot S_{\text{stagnation}} + 0.10 \cdot S_{\text{fire}} + 0.05 \cdot S_{\text{industrial}}))$$
5. **Confidence Scoring:**
   - Base confidence is $0.85$ for complete telemetry.
   - If `monitoring_coverage_gap_flag == 1` ($d > 7.0\text{ km}$), confidence is reduced by $0.15$ to $0.70$.
   - If telemetry is `UNAVAILABLE` or `MISSING`, confidence drops to $0.35$ with `evaluationMode = "DEGRADED_SPATIAL_FALLBACK"`.

---

## 6. Future ML Adapter Boundary

`ModelFeatureVectorAdapter` serves as the future model contract gate:
- Enforces `featureSchemaVersion == "f3-features-v1"`.
- Extracts all 36 features in exact index order matching `FeatureRecord.ORDERED_FEATURE_NAMES`.
- Converts values to strictly numeric `double[36]`.
- Implements single-boundary unit normalization (`adaptWithWindNormalization`) so that wind speed conversion ($v_{\text{mps}} = v_{\text{kmh}} / 3.6$) is applied exactly once without double conversion.

---

## 7. Prediction Persistence (`hotspot_predictions`)

Flyway migration `V9__f3_hotspot_predictions_feature_ref.sql` enhanced the existing `hotspot_predictions` table:

```sql
ALTER TABLE hotspot_predictions
ADD COLUMN IF NOT EXISTS feature_snapshot_id UUID REFERENCES feature_snapshots(id) ON DELETE SET NULL,
ADD COLUMN IF NOT EXISTS city_id UUID REFERENCES cities(id) ON DELETE CASCADE,
ADD COLUMN IF NOT EXISTS h3_index VARCHAR(30);

CREATE INDEX IF NOT EXISTS idx_hotspots_city_time ON hotspot_predictions(city_id, predicted_at DESC);
CREATE INDEX IF NOT EXISTS idx_hotspots_h3_time ON hotspot_predictions(h3_index, predicted_at DESC);
CREATE INDEX IF NOT EXISTS idx_hotspots_snapshot_id ON hotspot_predictions(feature_snapshot_id);
```

### PostgreSQL Verification:
```sql
SELECT h3_index, round(risk_score::numeric, 3) as risk_score, risk_level, 
       round(confidence::numeric, 3) as confidence, model_version, feature_snapshot_id 
FROM hotspot_predictions ORDER BY created_at DESC LIMIT 4;
```
```
    h3_index     | risk_score | risk_level | confidence |    model_version    |         feature_snapshot_id          
-----------------+------------+------------+------------+---------------------+--------------------------------------
 8860885357fffff |      0.723 | HIGH       |      0.850 | hotspot-baseline-v1 | c8a0ed0d-54f2-4c78-b9ff-f34640ce4509
 88608852c1fffff |      0.602 | MODERATE   |      0.850 | hotspot-baseline-v1 | 3ca1062f-77b1-449c-ae79-450d16964ec0
 88608850e5fffff |      0.723 | HIGH       |      0.850 | hotspot-baseline-v1 | b4514c0c-674d-4d69-9ef5-195754698662
 88608b56b3fffff |      0.264 | LOW        |      0.350 | hotspot-baseline-v1 | dce791ea-d77c-4e21-917c-82eee1778229
```

Historical predictions are preserved; evaluations append new records rather than overwriting past runs.

---

## 8. Public REST API Contract

### Overview Endpoint:
`GET /api/v1/hotspots?cityId=550e8400-e29b-41d4-a716-446655440001`

```json
{
  "cityId": "550e8400-e29b-41d4-a716-446655440001",
  "cityName": "Pune",
  "generatedAt": "2026-09-26T13:08:48.118968Z",
  "modelVersion": "hotspot-baseline-v1",
  "engineType": "BASELINE",
  "freshness": "LIVE",
  "totalCells": 3,
  "highRiskCells": 2,
  "cells": [
    {
      "h3Index": "88608850e5fffff",
      "gridCellId": "87c468e8-6e5a-4b95-a4aa-4927fb4ca730",
      "riskScore": 0.723,
      "riskLevel": "HIGH",
      "confidence": 0.85,
      "predictedAt": "2026-09-26T12:48:38.191225Z",
      "freshness": "LIVE",
      "modelVersion": "hotspot-baseline-v1"
    },
    {
      "h3Index": "88608852c1fffff",
      "gridCellId": "481a5e17-ea15-46eb-832f-a3621ae1c098",
      "riskScore": 0.602,
      "riskLevel": "MODERATE",
      "confidence": 0.85,
      "predictedAt": "2026-09-26T12:48:38.851263Z",
      "freshness": "LIVE",
      "modelVersion": "hotspot-baseline-v1"
    }
  ]
}
```

### Single-Cell Endpoint:
`GET /api/v1/hotspots/88608850e5fffff`
- Returns 200 with `HotspotCellDto` for valid cells.
- Returns 404 NOT FOUND for unknown H3 cells.

---

## 9. Freshness Behavior

Adheres strictly to the F2 freshness standard:
- `age <= 2 hours` $\rightarrow$ `LIVE`
- `2 hours < age <= 24 hours` $\rightarrow$ `STALE`
- `age > 24 hours` $\rightarrow$ `UNAVAILABLE`
- No data found $\rightarrow$ `NO_DATA`

Authoritative timestamp is `predictedAt` (matching `snapshot.observedAt`).

---

## 10. Prediction Validation Gates

Implemented in `HotspotPredictionValidator`:
1. `riskScore`: $0.0 \le \text{score} \le 1.0$ (rejects NaN, $< 0$, $> 1$).
2. `confidence`: $0.0 \le \text{confidence} \le 1.0$.
3. `riskLevel`: Must match `HotspotRiskLevel` enum.
4. `h3Index`: Must be $\ge 15$ characters.
5. `predictedAt`: Cannot be in the future (max 5 minutes skew tolerance).
6. `featureSnapshotId`: Must be non-null.

If any check fails, an `IllegalArgumentException` is thrown and the invalid prediction is rejected.

---

## 11. Multi-City Handling

- **Pune:** Complete telemetry produces `qualityStatus = VALID`, resulting in confidence $0.85$.
- **Mumbai & Delhi:** Handled gracefully. Current DB carries only PM2.5; engine detects `UNAVAILABLE` co-pollutant status and outputs controlled predictions with low confidence ($0.35$). No fake numbers are fabricated.
- Cross-city validation is not falsely claimed; scope is documented transparently.

---

## 12. Unit Contract & Wind Normalization

- **Feature Store Unit:** `wind_speed` is stored in $\text{km/h}$ (Open-Meteo source unit).
- **Derived Vectors:** `wind_u` and `wind_v` are stored in $\text{m/s}$.
- **Adapter Gate:** `ModelFeatureVectorAdapter.adaptWithWindNormalization(snapshot, true)` converts `wind_speed` to $\text{m/s}$ ($v_{\text{mps}} = v_{\text{kmh}} / 3.6$) at the model evaluation boundary.
- **Double Conversion Prevention:** Tested in `HotspotDomainUnitTest.testWindSpeedNormalization` — calling with `false` retains source unit, calling with `true` converts once, verifying zero double conversion.

---

## 13. Test Results

### Suite A: Domain Unit Tests (`HotspotDomainUnitTest`)
- Tests run: 13, Failures: 0, Errors: 0, Skipped: 0. Time: 0.56s.
- Validates score thresholds, range validations, schema version assertions, single-boundary wind conversion, stagnation rules, and coverage penalties.

### Suite B: Integration Tests (`HotspotIntegrationTest`)
- Tests run: 5, Failures: 0, Errors: 0, Skipped: 0. Time: 14.28s.
- Validates `/api/v1/hotspots` overview, database persistence with snapshot IDs, `/api/v1/hotspots/{h3Index}` single-cell retrieval, 404 error handling, and multi-city behavior.

### Suite C: Full Backend Regression Suite
- Total Tests: 61, Failures: 0, Errors: 0, Skipped: 0. `BUILD SUCCESS`.
- Included F2 weather ingestion, H3 spatial grid mappings, feature engineering, and Phase 3 hotspot domains.

---

## 14. Known Limitations

1. **Model Runtime Integration:** `hotspot_classifier_v1.joblib` is not yet directly evaluated in Spring Boot; the baseline engine serves as the operational product core.
2. **Co-Pollutants in Mumbai / Delhi:** Baseline engine correctly operates in degraded confidence mode ($0.35$) for these cities until full CPCB parameters are ingested.

---

## 15. Phase 4 Starting Point

Phase 3 is **COMPLETE**.  
The starting point for Phase 4 is:
- Consuming `GET /api/v1/hotspots?cityId=...` in the React frontend.
- Reusing the existing F2 H3 Deck.gl / Leaflet map to color cells by `riskLevel` (`LOW` = Green, `MODERATE` = Yellow, `HIGH` = Orange, `CRITICAL` = Red).
- Showing single-cell risk inspection cards via `GET /api/v1/hotspots/{h3Index}`.
