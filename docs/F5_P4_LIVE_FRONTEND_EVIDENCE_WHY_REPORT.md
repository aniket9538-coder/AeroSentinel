# AEROSENTINEL — FEATURE 5 (F5-P4)
# LIVE FRONTEND EVIDENCE + GROUNDED WHY INTEGRATION REPORT

**Phase:** F5-P4 — Live Frontend Evidence + Grounded WHY Integration  
**Date:** September 28, 2026  
**Status:** PASS  
**Target H3 Spatial Cell:** `88608850e5fffff` (Pune — Shivajinagar CAAQMS Sector)  
**Endpoint:** `GET /api/v1/evidence/hotspot/{h3Index}`  
**Authoritative Response DTO:** `com.aerosentinel.dto.evidence.EvidenceSummaryResponse`  

---

## 1. Phase Objective

The primary objective of F5-P4 was to connect the verified Spring Boot evidence orchestration pipeline (`GET /api/v1/evidence/hotspot/{h3Index}`) to the React frontend application. When an evaluator or municipal operator selects an H3 hexagonal sector on the map or in the hotspot inspector, the system retrieves and displays the real Evidence + Grounded WHY attribution dossier without synthetic numbers, hardcoded mock events, or fabricated telemetry.

Crucially, the UI enforces strict, uncompromised four-tier semantic separation:
1. **`OBSERVED`**: Physical ground telemetry and atmospheric sensors only.
2. **`MODEL OUTPUT`**: F3 calibrated classifier probability and F4 multi-horizon forecast regressors only.
3. **`AI INTERPRETATION`**: Grounded Gemini / deterministic fallback narrative strictly bound to verified telemetry.
4. **`RECOMMENDED VERIFICATION`**: Operational field directives and surveillance checklists.

---

## 2. Existing Frontend Audit

An audit of the frontend repository prior to F5-P4 identified several legacy mock artifacts:
- **`src/pages/analyst/EvidenceAnalysis.tsx`**: Contained hardcoded synthetic events (`EVENT-1023`, `EVENT-1024`, `EVENT-1025`), static risk scores (`0.84`, `0.79`), and synthetic sensor strings without connection to the Spring Boot REST API.
- **`src/components/evidence/GeminiExplanation.tsx`**: Contained static fallback copy attributing fires 1.4 km upwind and citizen photos regardless of cell selection.
- **`src/services/evidence.service.ts`**: Was a partial mock service returning local promises with simulated delays.
- **`src/components/hotspot/HotspotCellDetailsCard.tsx`**: Displayed F3 hotspot risk statistics but lacked direct, one-click continuous navigation to the Evidence & Grounded WHY dossier.

---

## 3. Files Created & Modified

| File | Change Type | Purpose |
|---|---|---|
| `frontend/src/types/evidence.ts` | **Created** | Comprehensive TypeScript contract mirroring `EvidenceSummaryResponse` (Context, ObservedFacts, ModelOutputs, EvidenceData, AiInterpretation, RecommendedVerification, EvidenceProvenance). |
| `frontend/src/types/index.ts` | **Updated** | Re-exported F5 types with unambiguous type resolution (avoiding `ConfidenceBreakdown` namespace collision with F3). |
| `frontend/src/services/evidenceApi.ts` | **Created** | Authoritative Axios HTTP service providing `getEvidence(h3Index, signal?)` and runtime structural validation `validateEvidenceResponse()`. |
| `frontend/src/services/evidence.service.ts` | **Updated** | Refactored to delegate to `evidenceApi.ts` for backward compatibility. |
| `frontend/src/hooks/useEvidence.ts` | **Created** | Production React hook enforcing `activeRequestIdRef` stale-response protection, `AbortController` cancellation, error handling, and cell-switch data wiping. |
| `frontend/src/components/hotspot/EvidencePanel.tsx` | **Created** | Authoritative UI component rendering the four separated tiers, event lineage, triage state badge, score breakdown, and provenance footer. |
| `frontend/src/pages/analyst/EvidenceAnalysis.tsx` | **Updated** | Replaced all legacy mock events with live `useEvidence(selectedH3)` integration, URL search parameter synchronization (`?h3=`), preset cell selector, and custom H3 input. |
| `frontend/src/components/hotspot/HotspotCellDetailsCard.tsx` | **Updated** | Injected "Inspect Evidence & Gemini WHY →" action button for seamless F3 → F5 navigation continuity. |
| `frontend/src/components/forecasting/F3ForecastContinuityBridge.tsx` | **Updated** | Enhanced DOM bridge to prevent redundant duplicate injection when native React action buttons are present. |
| `frontend/src/utils/evidence.test.ts` | **Created** | 12 automated unit and integration tests verifying API parsing, 4-tier separation, lineage, loading, error, empty states, race condition guard, mock removal, and numerical parity. |

---

## 4. API Integration Design

```
+--------------------------------------------------------------------------------------------------+
|                                    REACT FRONTEND DATA FLOW                                      |
+--------------------------------------------------------------------------------------------------+
   [ User Selection / URL ]             [ Top Navigation Ribbon ]            [ Hotspots Map / F3 ]
   ?h3=88608850e5fffff                  Preset Dropdown / Custom Input       "Inspect Evidence & WHY"
             \                                      |                                   /
              \                                     v                                  /
               +---------------------> [ selectedH3 State ] <------------------------+
                                                    |
                                                    v
                                         useEvidence(selectedH3)
                                                    |
                                     (AbortController + Request ID)
                                                    v
                                    GET /api/v1/evidence/hotspot/{h3}
                                                    |
                                                    v
                                       Spring Boot REST Controller
                                                    |
                                        EvidenceSummaryResponse
                                                    |
                                                    v
                                       validateEvidenceResponse()
                                                    |
                                                    v
                                             <EvidencePanel />
```

---

## 5. TypeScript Contract

The frontend TypeScript contract is defined in [`frontend/src/types/evidence.ts`](file:///c:/Users/lenovo/AeroSential/frontend/src/types/evidence.ts) and matches the backend DTO structure bit-for-bit:

```typescript
export interface EvidenceSummaryResponse {
  context: EvidenceContext;
  observedFacts: ObservedFacts;
  modelOutputs: ModelOutputs;
  evidence: EvidenceData;
  aiInterpretation?: AiInterpretation | null;
  recommendedVerification?: RecommendedVerification | null;
  provenance: EvidenceProvenance;
  status: string;
}
```

Key nested structures:
- `EvidenceContext`: `h3Index`, `cityId`, `cityName`, `predictionId`, `featureSnapshotId`, `eventId`, `eventCode`, `generatedAt`.
- `ObservedFacts`: `air` (PM2.5, PM10, NO2, SO2, CO, O3, stationId, observedAt), `weather` (temperature, humidity, windSpeed, windDirection, surfacePressure), `monitoringCoverage` (nearestStationDistanceKm, stationsWithin5kmCount, gapFlag, confidence), `spatialDispersion` (pm25SpatialLagMean, windU, windV), `gisContext` (industrial dist, road dist, fireCount).
- `ModelOutputs`: `hotspot` (riskScore, operationalThreshold=0.20, isHotspot, riskLevel, confidence, modelVersion=`hotspot_classifier_v1`, engineType), `forecast` (horizons 1h, 3h, 6h; forecastConfidence=`null` locked).
- `EvidenceData`: `evidenceScore`, `triageState` (`ALERT_CANDIDATE` | `MONITOR` | `INSUFFICIENT_EVIDENCE`), `scoreBreakdown`, `signals`, `consistency`, `clusterH3Cells`.
- `AiInterpretation`: `summaryPublic`, `summaryAnalyst`, `detectedCondition`, `supportingSignals`, `forecastTrajectory`, `uncertaintyStatement`, `unsupportedConclusions`, `causalClaimSupported`, `isGrounded`.
- `RecommendedVerification`: `action`, `priority` (`ROUTINE` | `ELEVATED` | `HIGH` | `URGENT`), `guidelines`.
- `EvidenceProvenance`: `f3ModelVersion`, `f4ModelVersion`, `f5ScoringVersion`, `geminiModelVersion`, `evaluatedAt`.

---

## 6. Evidence UI Structure & Mandatory Information Separation

The `<EvidencePanel />` component groups and styles intelligence into visually distinct cards with explicit badges and color tokens:

### A. Context & Event Lineage
- **H3 Hex Index**: `88608850e5fffff` (displayed in high-contrast monospace).
- **Event Identity**: Displays `eventCode` (`EVT-88608850-2026092613-d75654e9`) and database UUID `eventId`.
- **Lineage Linkage**: Parent F3 `predictionId` (`a310c689-f340-49fc-8935-a037de8d7709`) and spatial cluster cells.
- **Evidence Score & Triage Badge**: Normalized score `0.000` to `1.000` with sub-scores (Observation strength, Forecast support, Multi-source agreement, Spatial consistency).

### B. Tier A: "OBSERVED" (Physical Reality Only)
- Explicit Header: **`OBSERVED`** with badge `Physical Reality`.
- Contains only empirical sensor data:
  - Ground Air Telemetry: PM2.5 (`78.0 µg/m³`), PM10, Station ID (`PUN-001`).
  - Surface Meteorology: Temperature (`21.8 °C`), Wind Speed (`1.36 m/s`), Humidity (`95%`), Pressure (`948 hPa`).
  - Monitoring Network: Nearest CAAQMS monitor distance (`0.27 km`), Coverage gap flag (`NO`).
  - Environmental GIS: Industrial cluster distance (`3.5 km`), Arterial highway distance (`0.4 km`), Thermal hotspots (`0`).
- **Strict Rule:** Zero model predictions or AI interpretations are permitted in this section.

### C. Tier B: "MODEL OUTPUT" (ML Calculations Only)
- Explicit Header: **`MODEL OUTPUT`** with badge `F3 Hotspot: CRITICAL`.
- Contains only numerical model inferences:
  - F3 Platt-Calibrated Random Forest: Risk Score (`0.7998` / `79.98%`), Operational Threshold (`0.20`), Hotspot Status (`HOTSPOT CONFIRMED`), Model Version (`hotspot_classifier_v1`).
  - F4 PM2.5 Trajectory: Horizons +1h (`70.62 µg/m³`), +3h (`70.55 µg/m³`), +6h (`60.91 µg/m³`) with empirical P10/P90 confidence intervals (`[68.78 – 72.48]`, `[66.65 – 73.60]`, `[55.39 – 66.33]`).
  - Note: `forecastConfidence` displayed as strictly `null` per locked contract.
- **Strict Rule:** Preserves exact numerical floating-point outputs from the backend without client-side manipulation.

### D. Tier C: "AI INTERPRETATION" (Grounded Gemini Narrative)
- Explicit Header: **`AI INTERPRETATION`** with badge `GROUNDED REASONING`.
- Explanatory Disclaimer: Explicitly informs operators that Gemini synthesizes validated telemetry and does NOT compute predictions or legal blame.
- Public Explanatory Summary: Concise narrative grounded in physical observations.
- Technical Analyst Diagnostic: Technical decomposition referencing specific observations and forecast trajectories.
- Safety & Grounding Guardrails: Renders `unsupportedConclusions` explicitly listing excluded causal claims (e.g. unverified facility stack blame without physical inspection).

### E. Tier D: "RECOMMENDED VERIFICATION" (Operational Directives)
- Explicit Header: **`RECOMMENDED VERIFICATION`** with badge `PRIORITY: ROUTINE` or `URGENT`.
- Action Directive: Verbatim operational command from the backend orchestration engine.
- Operational Checklist: Bulleted field checklist based on triage classification.

---

## 7. Loading, Error, and Empty State Handling

1. **Loading State:**
   - Active whenever `h3Index` changes or `refresh()` is called.
   - Instantly purges previous cell data (`setEvidence(null)`) to prevent showing stale evidence for a newly clicked cell.
   - Shows an animated spinning radar indicator with targeted text stating the active hex being queried.
2. **Error State:**
   - Captures network disconnects, HTTP 400 (malformed H3 hex), HTTP 404 (unmonitored sector), and HTTP 500.
   - Renders a warning banner with specific server error message and a "Retry Request" button.
3. **Empty / Insufficient State:**
   - If an H3 index has no associated telemetry or is empty, renders an informative empty state informing the user to select an active sector.
   - Never generates synthetic or placeholder telemetry.

---

## 8. Rapid H3 Selection & Stale Response Protection

To eliminate race conditions when a user rapidly clicks multiple cells (e.g. Cell A -> Cell B):
- **Request ID Monotonic Counter:** `activeRequestIdRef.current` increments synchronously on every fetch trigger.
- **AbortController In-Flight Cancellation:** Any pending HTTP request for the previous cell is aborted via `controller.abort()`.
- **Response Guard:** When a request completes, `if (requestId !== activeRequestIdRef.current) return;` ensures that if Cell A's response arrives after Cell B, it is silently discarded and never overwrites Cell B's active UI state.
- **Unit Verification:** Verified deterministically in Test 10 of `evidence.test.ts`.

---

## 9. Test Suite Execution & Results

### A. Frontend Test Suite (`npm test`)

Executed command:
```bash
npx tsx --test src/utils/*.test.ts
```

Output summary:
```
# tests 75
# suites 0
# pass 75
# fail 0
# cancelled 0
# skipped 0
# todo 0
# duration_ms 580.6098
```

Detailed breakdown of the 12 new F5-P4 automated tests:
1. `1. Evidence API Request — Successfully parses authoritative backend payload` (PASS)
2. `2. Observed Facts — Validates physical sensor data strictly separated from model outputs` (PASS)
3. `3. Model Outputs — Validates F3 risk classification and F4 multi-horizon forecasts` (PASS)
4. `4. AI Interpretation — Validates grounded Gemini explanation and safety guardrails` (PASS)
5. `5. Recommended Verification — Operational Directives (Tier D)` (PASS)
6. `6. Event Lineage — Validates complete audit trail across H3, event, and predictions` (PASS)
7. `7. Loading State — Ensures stale data is cleared during in-flight requests` (PASS)
8. `8. API Failure State — Rejects malformed responses with specific error signatures` (PASS)
9. `9. Empty Evidence State — Appropriately handles missing or null cell inputs without fabricating` (PASS)
10. `10. Stale Response Guard — Discards superseded response when activeRequestId increments` (PASS)
11. `11. Mock Values Audit — Confirms legacy mock identifiers are eliminated from production payload` (PASS)
12. `12. Numerical Parity — Confirms exact floating point parity with zero modification` (PASS)

TypeScript compiler verification:
```bash
npx tsc --noEmit
# Exit code: 0 (Zero type errors)
```

---

## 10. Regression Safety & Full Suite Results

All locked upstream test suites were executed to verify zero regression across the AeroSentinel architecture:

| Suite | Scope | Target | Result | Status |
|---|---|---|---|---|
| **Spring Boot Evidence Unit** | Backend F5 | `EvidenceUnitTest` | 19 / 19 PASS | **PASS** |
| **Spring Boot Evidence Integration** | Backend F5 | `EvidenceIntegrationTest` | 4 / 4 PASS | **PASS** |
| **Spring Boot Forecast Unit** | Backend F4 | `ForecastUnitTest` | 14 / 14 PASS | **PASS** |
| **Python Scoring Engine** | AI Service F5 | `test_f5_alert_support.py` | 10 / 10 PASS | **PASS** |
| **React Frontend Unit & Integration** | Frontend F2-F5 | `src/utils/*.test.ts` | 75 / 75 PASS | **PASS** |

**Grand Total Automated Tests Verified in P4:** 122 / 122 PASS (0 Failures, 0 Regressions).

---

## 11. Real Pune Runtime Verification

The live Spring Boot backend was queried directly at runtime for the baseline Pune H3 cell `88608850e5fffff`:

```bash
curl.exe -i http://localhost:8080/api/v1/evidence/hotspot/88608850e5fffff
```

**Live Runtime Response:**
```json
{
  "status": "SUCCESS",
  "context": {
    "h3Index": "88608850e5fffff",
    "cityId": "550e8400-e29b-41d4-a716-446655440001",
    "cityName": "Pune",
    "predictionId": "a310c689-f340-49fc-8935-a037de8d7709",
    "featureSnapshotId": "1624baa3-a5f8-407b-b1c2-36bcee7650b1",
    "eventId": "9ea81bb7-9ff2-4b4d-a80f-32e98e2cb12f",
    "eventCode": "EVT-88608850-2026092613-d75654e9",
    "generatedAt": "2026-09-26T13:09:44.571028Z"
  },
  "observedFacts": {
    "air": {
      "dataQuality": "VALID",
      "pm25": 78.0,
      "pm10": 120.0,
      "no2": 37.0,
      "so2": 14.0,
      "co": 0.9,
      "o3": 24.0,
      "observedAt": "2026-09-24T22:00:00Z",
      "stationId": "PUN-001",
      "recentPm25Mean24h": 78.0
    },
    "weather": {
      "dataQuality": "VALID",
      "temperature": 21.8,
      "humidity": 95.0,
      "windSpeedKmh": 4.9,
      "windSpeedMps": 1.36,
      "windDirection": 246.0,
      "surfacePressure": 948.0,
      "precipitation": 0.0,
      "observedAt": "2026-09-27T19:30:00Z"
    },
    "monitoringCoverage": {
      "dataQuality": "VALID",
      "nearestStationDistanceKm": 0.27,
      "stationsWithin5kmCount": 2,
      "monitoringCoverageGapFlag": 0,
      "spatialCoverageConfidence": 0.95
    },
    "spatialDispersion": {
      "dataQuality": "VALID",
      "pm25SpatialLagMean": 78.0,
      "windU": 4.3604,
      "windV": 0.0761
    },
    "gisContext": {
      "dataQuality": "VALID",
      "distToNearestIndustrialKm": 3.5,
      "distToNearestMajorRoadKm": 0.4,
      "sensitiveReceptorsCount2km": 4,
      "industrialZoneWithin2kmFlag": 0,
      "fireCount24h25km": 0
    }
  },
  "modelOutputs": {
    "hotspot": {
      "riskScore": 0.7998,
      "operationalThreshold": 0.2,
      "isHotspot": true,
      "riskLevel": "CRITICAL",
      "confidence": 0.86,
      "modelVersion": "hotspot_classifier_v1",
      "engineType": "ML"
    },
    "forecast": {
      "baseTimestamp": "2026-09-26T13:09:44.571028Z",
      "generatedAt": "2026-09-27T16:19:03.975817Z",
      "forecastModelVersion": "forecast_regressors_v1",
      "parentPredictionId": "a310c689-f340-49fc-8935-a037de8d7709",
      "forecastConfidence": null,
      "horizons": [
        { "horizonHours": 1, "predictedPm25": 70.62, "lowerBound": 68.78, "upperBound": 72.48 },
        { "horizonHours": 3, "predictedPm25": 70.55, "lowerBound": 66.65, "upperBound": 73.6 },
        { "horizonHours": 6, "predictedPm25": 60.91, "lowerBound": 55.39, "upperBound": 66.33 }
      ]
    }
  },
  "evidence": {
    "evidenceScore": 0.157,
    "scoreBreakdown": {
      "observationStrength": 0.2,
      "mlForecastSupport": 0.552,
      "multiSourceAgreement": 0.333,
      "spatialConsistency": 0.95,
      "temporalPersistence": 1.0,
      "recencyFactor": 1.0,
      "conflictPenalty": 0.35,
      "evidenceCompleteness": 0.333,
      "finalEvidenceScore": 0.157
    },
    "consistency": "insufficient_evidence",
    "triageState": "INSUFFICIENT_EVIDENCE",
    "clusterH3Cells": ["88608850e5fffff"]
  },
  "aiInterpretation": {
    "summaryPublic": "Air quality monitoring indicates an elevated pollution event in area 88608850e5fffff. The model forecasts PM2.5 concentrations of 70.6 ug/m3 in the next hour.",
    "summaryAnalyst": "F3 Model Assessment for H3 88608850e5fffff: Calibrated Hotspot Probability 0.80 (80%), Hotspot status: True. Forecast trajectory: T+1h=70.6 ug/m3, T+3h=70.5 ug/m3, T+6h=60.9 ug/m3, with validation residual uncertainty bounds [0.0, 0.0] ug/m3. Composite confidence is 0.86.",
    "detectedCondition": "Elevated Ground PM2.5 with Associated Regional Indicators",
    "forecastTrajectory": "T+1h: 70.6 ug/m3 | T+3h: 70.5 ug/m3 | T+6h: 60.9 ug/m3",
    "unsupportedConclusions": [
      "Facility-level legal causation cannot be asserted from ambient spatial modeling",
      "Remote sensing indicators represent column tropospheric density, not direct ground standard citations"
    ],
    "isGrounded": true,
    "modelVersion": "deterministic-fallback-v1.0"
  },
  "recommendedVerification": {
    "action": "Standard Routine Surveillance",
    "priority": "ROUTINE",
    "guidelines": [
      "Telemetry within nominal variance or insufficient corroboration.",
      "Continue automated 15-minute scheduled polling without dispatching field personnel."
    ]
  },
  "provenance": {
    "h3Index": "88608850e5fffff",
    "cityId": "550e8400-e29b-41d4-a716-446655440001",
    "f3PredictionId": "a310c689-f340-49fc-8935-a037de8d7709",
    "f3ModelVersion": "hotspot_classifier_v1",
    "f4ModelVersion": "forecast_regressors_v1",
    "f5ScoringVersion": "v1.0.0",
    "geminiModelVersion": "deterministic-fallback-v1.0",
    "evaluatedAt": "2026-09-27T19:03:30.596818800Z"
  }
}
```

---

## 12. Known Limitations & Strict Scope Boundaries

1. **Browser E2E Deferral:** Full browser interaction sessions and automated video recording are deferred to F5-P8 in strict compliance with the prompt guidelines.
2. **Read-Only Evidence Consumption:** F5-P4 delivers evidence consumption and inspection UI. Field inspection workflows, dispatching squads, and ticket resolution belong to Feature 6 (Authority / Citizen Action).
3. **No Model Re-Training:** Upstream F3 Random Forest weights (`hotspot_classifier_v1.joblib`) and F4 regressors (`forecast_regressors_v1.joblib`) were strictly preserved.

---

## 13. Definition of Done Checklist

- [x] Real Evidence API integrated into React (`useEvidence` calling `/api/v1/evidence/hotspot/{h3Index}`).
- [x] Selected H3 drives evidence retrieval dynamically.
- [x] Mock Evidence data removed/bypassed across `EvidenceAnalysis.tsx` and related components.
- [x] **`OBSERVED`** clearly separated with dedicated card and badge.
- [x] **`MODEL OUTPUT`** clearly separated with dedicated card and badge.
- [x] **`AI INTERPRETATION`** clearly separated with dedicated card and badge.
- [x] **`RECOMMENDED VERIFICATION`** clearly separated with dedicated card and badge.
- [x] Evidence score, score breakdown, triage state, and source signals rendered.
- [x] Event lineage (H3, eventId, eventCode, predictionId) rendered.
- [x] Provenance (model versions and timestamps) rendered.
- [x] Loading state verified.
- [x] Error state verified.
- [x] Empty/insufficient state verified.
- [x] Stale request protection verified via monotonic `activeRequestIdRef`.
- [x] TypeScript API contract matches backend `EvidenceSummaryResponse` exactly.
- [x] Numerical F3/F4 values preserved exactly without client drift.
- [x] Existing backend regression suites still PASS (Spring Boot 37/37, Python 10/10).
- [x] New frontend tests PASS (75/75 in `npm test`).
- [x] Real Pune H3 runtime verified against live Spring Boot and PostgreSQL.
- [x] No duplicated F3/F4/F5/Gemini business logic.
- [x] No fabricated evidence or AI claims.
- [x] Implementation report created.

---

## 14. Final Status

```
============================================================
F5-P4 STATUS: PASS
============================================================
```
