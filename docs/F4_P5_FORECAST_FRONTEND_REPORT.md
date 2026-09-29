# AeroSentinel — F4-P5 Forecast Frontend Integration Report

## 1. Phase Objective
The objective of Phase **F4-P5** is to integrate the real Spring Boot backend forecast REST API with the React frontend for AeroSentinel's Short-Term PM2.5 Forecast experience. 

Strict phase constraints:
- **F4-P4 is LOCKED**: Zero modifications to F3 Hotspot Detection, F3 database schema, `forecast_regressors_v1.joblib`, Python P2 feature engineering, Python P3 inference engine, or Spring Boot P4 persistence and numerical interval logic.
- **Real API Only**: No mocked forecast JSON, no hardcoded prediction numbers, no fake confidence values, no placeholder forecast charts, and no client-side residual calculations in React.
- **Honest Null Confidence**: `forecastConfidence = null` is preserved from backend to DOM, factually rendering "Forecast confidence: Not available" without converting to 0% or synthetic percentages.

---

## 2. Track A — Evaluator Requirements

| Requirement | Expected Proof | Actual Result | Status |
| :--- | :--- | :--- | :--- |
| **1. Selected H3 Location** | H3 index and monitoring station context visible | Displays `88608850e5fffff` (Pune Shivajinagar CAAQMS) with parent prediction and snapshot lineage | **PASS** |
| **2. Base PM2.5 Context** | Current/base observation PM2.5 rendered | Base timestamp `2026-09-26T13:09:44Z` and base observation point rendered in summary & chart | **PASS** |
| **3. Exact Horizons** | Short-term forecasts for exactly +1h, +3h, +6h | Distinct cards and timeline points rendered for +1h, +3h, and +6h | **PASS** |
| **4. Predicted PM2.5** | Real regression prediction values rendered | +1h: `70.62 ug/m3`, +3h: `70.55 ug/m3`, +6h: `60.91 ug/m3` | **PASS** |
| **5. Empirical Bounds** | Real empirical $P10$–$P90$ lower/upper bounds | +1h: $[68.78, 72.48]$, +3h: $[66.65, 73.60]$, +6h: $[55.39, 66.33]$ | **PASS** |
| **6. Target Time** | Target UTC/local timestamps rendered per horizon | +1h: `2:09 PM`, +3h: `4:09 PM`, +6h: `7:09 PM` calculated from base timestamp | **PASS** |
| **7. Forecast Timestamp** | Real `generatedAt` timestamp from backend | Rendered verbatim: `2026-09-27T10:22:40.113353Z` | **PASS** |
| **8. Freshness / Status** | Backend freshness and status indicators | Rendered via badge: `freshness: LIVE`, `status: SUCCESS` | **PASS** |
| **9. Honest Null Confidence** | Factual unavailable state, no fabricated values | Displays `"Forecast confidence: Not available"`; never converted to 0% | **PASS** |
| **10. Loading / Error / Empty** | Explicit states for loading, NO_DATA, STALE, error | Spinners, empty state with Pune CTA, stale warning banner, error retry card | **PASS** |

---

## 3. Track B — Engineering Implementation

| Requirement | Implementation | Verification | Status |
| :--- | :--- | :--- | :--- |
| **Phase 1: Architecture Audit** | Analyzed existing routes, components, and services | [`docs/F4_P5_FRONTEND_AUDIT.md`](file:///c:/Users/lenovo/AeroSential/docs/F4_P5_FRONTEND_AUDIT.md) created | **PASS** |
| **Phase 2: API Contract Lock** | Preserved exact Spring Boot `ForecastResponse` contract | [`frontend/src/types/forecast.ts`](file:///c:/Users/lenovo/AeroSential/frontend/src/types/forecast.ts) created | **PASS** |
| **Phase 3: API Client** | Created typed API client with schema validation | [`frontend/src/services/forecastApi.ts`](file:///c:/Users/lenovo/AeroSential/frontend/src/services/forecastApi.ts) with `validateForecastResponse` | **PASS** |
| **Phase 4: Forecast UI** | Updated summary, timeline cards, and composed chart | [`ForecastSummary.tsx`](file:///c:/Users/lenovo/AeroSential/frontend/src/components/forecasting/ForecastSummary.tsx), [`ForecastTimeline.tsx`](file:///c:/Users/lenovo/AeroSential/frontend/src/components/forecasting/ForecastTimeline.tsx), [`ForecastChart.tsx`](file:///c:/Users/lenovo/AeroSential/frontend/src/components/charts/ForecastChart.tsx) | **PASS** |
| **Phase 5: H3 Integration** | Cell selection bar triggers `GET /api/v1/forecast/{h3Index}` | Verified with cell `88608850e5fffff` and cell switching | **PASS** |
| **Phase 6: Freshness UI** | Handled `LIVE`, `STALE`, `NO_DATA`, `UNAVAILABLE` | Verified with status badges, stale banner, empty state | **PASS** |
| **Phase 7: Explicit States** | Implemented 9 distinct states, zero blank screens | Tested loading, success, empty, stale, 404, network error | **PASS** |
| **Phase 8: Responsive Layout** | Grid layout with CSS variables, auto-wrapping | Verified desktop and tablet usability, zero card overflow | **PASS** |
| **Phase 9: Real E2E Proof** | Browser/React $\to$ REST API $\to$ Spring Boot $\to$ DB | Pune CAAQMS real response retrieved and verified | **PASS** |

---

## 4. Frontend Architecture

The frontend integrates the forecast feature through a layered modular architecture:

```
[User Interface Layer]
   ├── Forecast.tsx (Route: /forecast)
   ├── ForecastSummary.tsx (Overview metrics: base PM2.5, peak forecast, lineage)
   ├── ForecastTimeline.tsx (Horizon cards: +1h, +3h, +6h with empirical bounds)
   ├── ForecastChart.tsx (Recharts ComposedChart: target times, predictions, bounds band)
   └── ForecastConfidence.tsx (Factual null confidence display)
         ↓
[Custom React Hooks]
   └── useForecast.ts (Request lifecycle, loading flags, error handling, stale states)
         ↓
[Typed API Client Layer]
   ├── forecastApi.ts (Axios requests to /forecast/{h3Index} and /forecast/generate)
   ├── forecast.service.ts (Export facade maintaining backwards compatibility)
   └── api.ts (Shared Axios client configured with Vite proxy to Spring Boot)
         ↓
[Backend REST Layer]
   └── Spring Boot ForecastController (GET /api/v1/forecast/{h3Index})
         ↓
[Database Layer]
   └── PostgreSQL `forecasts` table (Flyway V10/V11)
```

Direct database access from React is strictly impossible; all interactions traverse the authenticated/proxied Spring Boot REST API.

---

## 5. API Contract

The frontend strictly enforces the locked F4-P4 API contract defined in [`frontend/src/types/forecast.ts`](file:///c:/Users/lenovo/AeroSential/frontend/src/types/forecast.ts):

### Primary Read Endpoint
`GET /api/v1/forecast/{h3Index}`

### Response Payload Structure
```typescript
export interface ForecastResponse {
  h3Index: string;
  cityId: string;
  baseTimestamp: string | null;
  generatedAt: string;
  modelVersion: string;
  parentPredictionId: string | null;
  featureSnapshotId: string | null;
  status: ForecastStatus;       // 'SUCCESS' | 'NO_DATA' | 'STALE' | 'UNAVAILABLE' | 'MODEL_DOMAIN_UNSUPPORTED'
  freshness: ForecastFreshness; // 'LIVE' | 'STALE' | 'NO_DATA' | 'UNAVAILABLE'
  forecasts: ForecastItem[];
  forecastConfidence: number | null;
}

export interface ForecastItem {
  horizonHours: 1 | 3 | 6;
  targetTime: string;
  predictedPm25: number;
  lowerBound: number;
  upperBound: number;
  unit: string;
}
```

The runtime validation function `validateForecastResponse()` validates:
1. Object shape and non-empty `h3Index`.
2. Array structure of `forecasts`.
3. Horizon values strictly in `[1, 3, 6]`.
4. Bounds ordering: `0 <= lowerBound <= predictedPm25 <= upperBound`.
5. Null preservation for `forecastConfidence`.

---

## 6. H3 Integration

H3 selection operates through the primary page controller in [`Forecast.tsx`](file:///c:/Users/lenovo/AeroSential/frontend/src/pages/public/Forecast.tsx):
- Default selected cell: Pune Shivajinagar CAAQMS (`88608850e5fffff`).
- Quick-select station presets:
  - `88608850e5fffff`: Pune Shivajinagar CAAQMS (authoritative F4 baseline station)
  - `88608852c1fffff`: Pune Katraj Station
  - `8860885357fffff`: Pune Hadapsar Industrial Zone
- Custom H3 input: Allows analysts to input any 15-character Uber H3 Res-8 index.
- Race condition guard: `useForecast.ts` maintains an incrementing request counter (`activeRequestId`) ensuring late responses from previously selected cells are discarded immediately upon cell switch.

---

## 7. Forecast UI

The Forecast interface delivers a comprehensive monitoring display:

1. **Header & Context Bar**:
   - Title: "Short-Term PM2.5 Forecast (F4)"
   - Subtitle: "Multi-horizon empirical regression forecasts for Pune PMR (+1h, +3h, +6h)"
   - Status indicators: `LIVE`, `STALE`, `NO_DATA` freshness badges
   - Lineage summary: Displays `parentPredictionId`, `featureSnapshotId`, and `modelVersion`
2. **Forecast Summary Cards**:
   - Base Observation PM2.5 at $T_0$
   - Peak Horizon & Peak PM2.5 prediction
   - Forecast Model Version (`forecast_regressors_v1`)
   - Forecast Confidence: "Not available" (factual null handling)
3. **Forecast Timeline Cards (+1h, +3h, +6h)**:
   - Discrete card per horizon with target time header
   - Big numerical display of `predictedPm25` with `ug/m3` unit
   - Visual empirical uncertainty band $[P10, P90]$ showing exact `lowerBound` and `upperBound`
   - Absolute delta ($\Delta$) relative to baseline $T_0$ PM2.5

---

## 8. Chart Implementation

[`ForecastChart.tsx`](file:///c:/Users/lenovo/AeroSential/frontend/src/components/charts/ForecastChart.tsx) implements an interactive composed visual using Recharts:
- **X-Axis**: Target times ($T_0$, $+1\text{h}$, $+3\text{h}$, $+6\text{h}$) formatted in local/UTC time.
- **Y-Axis**: PM2.5 concentration in $\mu\text{g/m}^3$, autoscaled with padding around the interval extremes.
- **Empirical Uncertainty Area**: Shaded bounded region between `lowerBound` ($P10$) and `upperBound` ($P90$).
- **Predicted Curve**: Crisp highlighted line connecting base PM2.5 observation $T_0$ to the $+1\text{h}$, $+3\text{h}$, and $+6\text{h}$ points.
- **Strict Anti-Recalculation**: No client-side regression, residual scaling, or standard-deviation conversions are performed. The backend numerical values are plotted verbatim.
- **Backwards Compatibility**: Accommodates existing `Dashboard.tsx` usage via optional `data` prop support.

---

## 9. Confidence Handling

In accordance with Phase F4 specifications, residual-based conformal confidence scoring is deferred to Phase F5. The backend returns:
```json
"forecastConfidence": null
```
The frontend handles this with strict factual honesty:
- Display text: `"Forecast confidence: Not available"`
- Informational footnote: `"Empirical bounds (P10–P90) provide operational interval guidance. Conformal confidence scoring is scheduled for F5."`
- Prohibition enforced: The UI never converts `null` to `0%`, `86%`, `95%`, "High confidence", or "Medium confidence".

---

## 10. Status/Freshness Handling

The UI handles all backend statuses without ambiguity:

| Status / Freshness | UI Representation | Data Retention |
| :--- | :--- | :--- |
| **LIVE** | Emerald `success` badge ("LIVE"); live pulse indicator | Full forecast cards and chart displayed |
| **STALE** | Amber `warning` badge ("STALE"); persistent banner: *"Forecast data is stale — showing last computed values from [timestamp]"* | Forecast data remains visible for operational continuity |
| **NO_DATA** | Neutral EmptyState: *"No Forecast Available for this H3 Cell"*; action button directing user to Pune Shivajinagar CAAQMS | Cards and chart replaced with clean contextual guidance |
| **UNAVAILABLE** | Red ErrorState: *"Forecast Service Unavailable"*; descriptive error message and retry button | Error state rendered, preventing misleading displays |
| **SUCCESS** | Standard operational view with all 3 horizons rendered | Rendered fully |

---

## 11. Loading/Error/Empty States

The implementation contains explicit handlers for all operational states:
1. **Initial / Unselected Cell**: Prompts user to select an H3 monitoring station.
2. **Loading State**: Displays animated spinner with message *"Retrieving real multi-horizon forecast from Spring Boot..."*.
3. **Success State**: Full operational dashboard with lineage, timeline cards, and chart.
4. **NO_DATA / Empty State**: Displayed when an un-forecasted cell is queried (HTTP 404 or `status: NO_DATA`).
5. **STALE State**: Displays last-known forecast with warning banner.
6. **Service Unavailable / Network Error**: Intercepts Axios exceptions and displays retry card.
7. **Malformed API Response**: Handled by `validateForecastResponse`, preventing blank screens or uncaught JavaScript exceptions.

---

## 12. Real Pune Runtime Proof

The real backend was executed and verified against active PostgreSQL data.

### Request
```http
GET http://localhost:8080/api/v1/forecast/88608850e5fffff
```

### Raw Database-Backed Response
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

### Verification Against Expected Values
- $+1\text{h}$: `pred = 70.62`, `bounds = [68.78, 72.48]` $\to$ **EXACT MATCH**
- $+3\text{h}$: `pred = 70.55`, `bounds = [66.65, 73.60]` $\to$ **EXACT MATCH**
- $+6\text{h}$: `pred = 60.91`, `bounds = [55.39, 66.33]` $\to$ **EXACT MATCH**
- `forecastConfidence`: `null` $\to$ **EXACT MATCH**
- `parentPredictionId`: `a310c689-f340-49fc-8935-a037de8d7709` $\to$ **EXACT MATCH**

### Proxied Vite Route
```http
GET http://localhost:3000/api/v1/forecast/88608850e5fffff
HTTP/1.1 200 OK
Content-Type: application/json
```
Proxied transparently via Vite dev server proxy to Spring Boot port 8080.

---

## 13. Automated Test Results

Executed automated unit test suite across the frontend:
```bash
npm test
# Command: npx tsx --test src/utils/*.test.ts
```

### Test Summary
- **Total Tests Run**: 54
- **Total Tests Passed**: 54
- **Total Tests Failed**: 0
- **Pass Rate**: 100%

### Detailed Test Suites
1. **[`src/utils/forecast.test.ts`](file:///c:/Users/lenovo/AeroSential/frontend/src/utils/forecast.test.ts)** (10 / 10 PASS):
   - `ok 1` - 1. API Client Parsing — Successfully parses authoritative backend payload
   - `ok 2` - 2. Response Validation — Rejects invalid structures, invalid horizons, and bounds violations
   - `ok 3` - 3. Null Confidence Representation — Always preserves null and displays factual wording
   - `ok 4` - 4. Horizon Rendering — Exactly extracts and sorts horizons +1h, +3h, +6h
   - `ok 5` - 5. Bounds Rendering — Verifies empirical P10/P90 bounds match backend without client recalculation
   - `ok 6` - 6. Loading State — Tracks loading transitions and state isolation
   - `ok 7` - 7. NO_DATA State — Properly handles empty/missing cell forecast without error
   - `ok 8` - 8. STALE State — Recognizes STALE freshness and keeps forecast viewable with warning
   - `ok 9` - 9. API Error State — Handles network error and unexpected failures gracefully
   - `ok 10` - 10. H3 Selection & Fetch Sequencing — Validates cell switching and race condition suppression
2. **[`src/utils/freshness.test.ts`](file:///c:/Users/lenovo/AeroSential/frontend/src/utils/freshness.test.ts)** (21 / 21 PASS):
   - All 21 environmental freshness and error classification tests passed.
3. **[`src/utils/hotspot.test.ts`](file:///c:/Users/lenovo/AeroSential/frontend/src/utils/hotspot.test.ts)** (23 / 23 PASS):
   - All 23 hotspot spatial contract, risk styling, and city switch tests passed.

---

## 14. Frontend Build Result

Executed frontend production build:
```bash
npm run build
# Command: tsc -b && vite build
```

### Build Output Excerpt
```text
> aerosentinel-frontend@1.0.0 build
> tsc -b && vite build

vite v5.4.21 building for production...
transforming...
✓ 2551 modules transformed.
rendering chunks...
computing gzip size...
dist/index.html                     1.21 kB │ gzip:   0.66 kB
dist/assets/index-HCKc7b8T.css     32.19 kB │ gzip:   6.80 kB
dist/assets/index-CMT_xBIw.js   1,275.28 kB │ gzip: 353.86 kB
✓ built in 24.72s
```
- **TypeScript Errors**: 0
- **Compile Errors**: 0
- **Build Status**: **PASS** (Exit Code 0)

---

## 15. Regression Results

Executed authoritative F3 Hotspot Detection regression test suite:
```bash
.\mvnw.cmd test "-Dtest=HotspotIntegrationTest,MLHotspotDetectionEngineTest"
```

### Execution Output Excerpt
```text
[INFO] Running com.aerosentinel.hotspot.HotspotIntegrationTest
2026-09-27T18:05:30.920+05:30 INFO: Verified 3 persisted hotspot predictions with valid feature snapshot provenance
2026-09-27T18:05:31.449+05:30 INFO: Multi-city verified: Mumbai (cells=2), Delhi (cells=3)
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 30.75 s -- in com.aerosentinel.hotspot.HotspotIntegrationTest
[INFO] Running com.aerosentinel.hotspot.MLHotspotDetectionEngineTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.781 s -- in com.aerosentinel.hotspot.MLHotspotDetectionEngineTest
[INFO] 
[INFO] Results:
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 01:01 min
```
- **F3 Tests Run**: 14
- **F3 Tests Passed**: 14
- **F3 Regressions**: 0
- **Status**: **100% PASS**

---

## 16. Files Created

1. [`docs/F4_P5_FRONTEND_AUDIT.md`](file:///c:/Users/lenovo/AeroSential/docs/F4_P5_FRONTEND_AUDIT.md) — Comprehensive pre-implementation architecture and component audit.
2. [`frontend/src/types/forecast.ts`](file:///c:/Users/lenovo/AeroSential/frontend/src/types/forecast.ts) — TypeScript interfaces for forecast API request/response contracts and domain types.
3. [`frontend/src/services/forecastApi.ts`](file:///c:/Users/lenovo/AeroSential/frontend/src/services/forecastApi.ts) — Typed forecast REST client and response validator.
4. [`frontend/src/hooks/useForecast.ts`](file:///c:/Users/lenovo/AeroSential/frontend/src/hooks/useForecast.ts) — React hook managing forecast state, loading, errors, and race condition suppression.
5. [`frontend/src/utils/forecast.test.ts`](file:///c:/Users/lenovo/AeroSential/frontend/src/utils/forecast.test.ts) — 10 automated unit test suites for frontend forecast logic.
6. [`docs/F4_P5_FORECAST_FRONTEND_REPORT.md`](file:///c:/Users/lenovo/AeroSential/docs/F4_P5_FORECAST_FRONTEND_REPORT.md) — This final verification report.

---

## 17. Files Modified

1. [`frontend/package.json`](file:///c:/Users/lenovo/AeroSential/frontend/package.json) — Added `"test": "npx tsx --test src/utils/*.test.ts"` script.
2. [`frontend/src/services/api.ts`](file:///c:/Users/lenovo/AeroSential/frontend/src/services/api.ts) — Hardened environment variable resolution and storage access for Node/Vite test compatibility.
3. [`frontend/src/services/forecast.service.ts`](file:///c:/Users/lenovo/AeroSential/frontend/src/services/forecast.service.ts) — Re-exported `forecastApi` and aligned return types with `ForecastResponse`.
4. [`frontend/src/components/forecasting/ForecastSummary.tsx`](file:///c:/Users/lenovo/AeroSential/frontend/src/components/forecasting/ForecastSummary.tsx) — Added base PM2.5 display, peak forecast, peak horizon, and honest null confidence handling.
5. [`frontend/src/components/forecasting/ForecastTimeline.tsx`](file:///c:/Users/lenovo/AeroSential/frontend/src/components/forecasting/ForecastTimeline.tsx) — Rendered +1h, +3h, +6h forecast cards with predicted PM2.5, bounds, target time, and unit.
6. [`frontend/src/components/charts/ForecastChart.tsx`](file:///c:/Users/lenovo/AeroSential/frontend/src/components/charts/ForecastChart.tsx) — Recharts composed chart showing target times, predictions, and empirical bounds area. Added backwards-compatible `data` prop.
7. [`frontend/src/components/forecasting/ForecastConfidence.tsx`](file:///c:/Users/lenovo/AeroSential/frontend/src/components/forecasting/ForecastConfidence.tsx) — Handled `null` confidence honestly with neutral badge and explanatory note.
8. [`frontend/src/pages/public/Forecast.tsx`](file:///c:/Users/lenovo/AeroSential/frontend/src/pages/public/Forecast.tsx) — Integrated `useForecast`, H3 selection bar, freshness badges, stale banner, empty state, and error handling.

---

## 18. Files Untouched

The following systems were intentionally preserved without modification:
- **F3 Hotspot Detection**:
  - `backend/src/main/java/com/aerosentinel/hotspot/*`
  - `backend/src/test/java/com/aerosentinel/hotspot/*`
- **Database Schema**:
  - `backend/src/main/resources/db/migration/V1__*.sql` through `V11__*.sql`
- **Model Artifact**:
  - `ai-service/models/artifacts/forecast_regressors_v1.joblib`
- **Python ML Pipeline**:
  - `ai-service/ml/forecast/features/*` (P2 Feature layer)
  - `ai-service/ml/forecast/engine.py` (P3 Inference engine)
  - `ai-service/ml/inference/predict_forecast_cli.py` (P3 CLI entry point)
- **Spring Boot P4 Forecast Domain**:
  - `backend/src/main/java/com/aerosentinel/forecast/*` (Forecast entity, repository, service, mapper, process bridge)

---

## 19. Known Limitations / Missing Items

1. **Forecast Confidence is Null by Design**: Conformal confidence intervals and calibrated certainty scoring are designated for Phase F5. The UI honestly reflects this with "Forecast confidence: Not available".
2. **Horizon Support**: Supported horizons are strictly +1h, +3h, and +6h as trained in `forecast_regressors_v1.joblib`. Horizons outside this range are rejected by `validateForecastResponse`.
3. **Spatial Scope**: Operational forecasts are currently active for Pune PMR monitoring cells with F3 parent predictions.

---

## 20. Final Phase Decision

All Track A and Track B requirements have been verified against active backend services, automated test suites, production build tooling, and PostgreSQL database queries.

```
F4-P5 = PASS
```
