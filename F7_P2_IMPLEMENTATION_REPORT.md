# AEROSENTINEL — F7-P2 IMPLEMENTATION REPORT
**CITIZEN EVIDENCE → POLLUTION EVENT BRIDGE HARDENING + F7 FOUNDATION**

**Date:** 2026-09-29  
**Status:** ✅ **PASS**  
**Phase:** F7-P2  

---

## 1. Executive Summary

Phase F7-P2 successfully implemented and hardened the bridge linking F6 Citizen Reports and Gemini Vision Analysis to the authoritative F7 Pollution Event and Authority Alert contexts without duplicating or breaking any existing F3, F4, F5, or F6 logic.

Key achievements:
1. **Preserved Complete Lineage**: Hardened the existing bridge where an F6 citizen report linked to an open `PollutionEvent` produces exactly one canonical `EventEvidence` record with `dataSource = "CITIZEN"` and `relevanceTier = "AUXILIARY"`.
2. **Exposed Citizen Evidence to Authority Context**: Extended `AuthorityQueueItemDto` and `AlertService` to provide attached citizen observations (`CitizenEvidenceItemDto`) including safe photo URLs, Gemini visual interpretations, and uncertainty metadata without exposing server-internal filesystem paths.
3. **Enhanced Authority Queue UI (`Alerts.tsx`)**: Added a dedicated, evaluator-safe *Citizen Visual Observation* card inside the alert detail drawer displaying citizen ground photos, AI visual assessments, and explicit disclaimer notices.
4. **Resolved Events API Security Defect**: Fixed the HTTP 403 authorization issue on `GET /api/v1/events/**` by adding it to `SecurityConfig.java` permitAll rules while maintaining write endpoint protection.
5. **Enforced F5 Truthfulness**: Retained strict isolation between citizen auxiliary evidence and official regulatory sensor/model metrics. Citizen evidence alone cannot create an `ALERT_CANDIDATE`.

---

## 2. Baseline Test Results & Validation Summary

| Test Domain | Target Suite | Result | Details |
|---|---|:---:|---|
| **Backend Unit** | `AlertUnitTest` | ✅ PASS | 16/16 tests passing (including auxiliary gating & citizen DTO mapping) |
| **Backend Integration** | `AlertIntegrationTest` | ✅ PASS | 11/11 tests passing (security fix & live DB integration verified) |
| **Backend Integration** | `EvidenceIntegrationTest` | ✅ PASS | 5/5 tests passing |
| **Backend Integration** | `ForecastIntegrationTest` | ✅ PASS | 11/11 tests passing (F4 non-regression) |
| **Backend Integration** | `CitizenReportIntegrationTest` | ✅ PASS | 7/7 tests passing (F6 non-regression) |
| **Backend Integration** | `H3SpatialIntegrationTest` | ✅ PASS | 8/8 tests passing |
| **Backend Integration** | `F2GridApiContractTest` | ✅ PASS | 11/11 tests passing |
| **Backend Integration** | `HotspotIntegrationTest` | ✅ PASS | 5/5 tests passing (F3 non-regression) |
| **Frontend Tests** | Vitest (`f7_p2_citizen_alert.test.ts` & all) | ✅ PASS | 210/210 tests passing across all test files |
| **TypeScript Typecheck** | `npx tsc -b` | ✅ PASS | 0 errors |
| **Production Build** | `npm run build` | ✅ PASS | Completed with clean bundle generation |

---

## 3. Exact Files Changed

1. `backend/src/main/java/com/aerosentinel/config/SecurityConfig.java`
   - Added `"/api/v1/events/**"` to `.permitAll()` in HTTP security chain.
2. `backend/src/main/java/com/aerosentinel/alert/AlertService.java`
   - Extended `toAuthorityQueueItemDto` to query attached citizen `EventEvidence` records, fetch linked `CitizenReport` and `GeminiAnalysis` data, and construct safe `CitizenEvidenceItemDto` entries with safe photo URLs.
3. `backend/src/main/java/com/aerosentinel/dto/alert/AuthorityQueueItemDto.java`
   - Added `private List<CitizenEvidenceItemDto> citizenEvidence = new ArrayList<>();` and its getter/setter.
4. `frontend/src/types/alert.ts`
   - Added `CitizenEvidenceItem` interface.
   - Extended `AuthorityQueueItem` with optional `citizenEvidence?: CitizenEvidenceItem[]`.
5. `frontend/src/pages/authority/Alerts.tsx`
   - Added Section 20b: Citizen Observation card inside the alert detail drawer displaying report reference, category, description, Gemini visual interpretation badge, confidence score, photo preview (`<img src={obs.photoUrl}>`), and evaluator disclaimers.

---

## 4. Exact Files Added

1. `backend/src/main/java/com/aerosentinel/dto/alert/CitizenEvidenceItemDto.java`
   - Canonical DTO encapsulating citizen report reference, H3 index, category, observation time, Gemini visual condition, confidence, uncertainty, safe photo URL, data source, and evidence key.
2. `frontend/src/utils/f7_p2_citizen_alert.test.ts`
   - Comprehensive test suite covering citizen evidence card rendering, empty state handling, photo URL safety, H3 provenance, and evaluator warnings.
3. `F7_P2_IMPLEMENTATION_REPORT.md`
   - This document.

---

## 5. F6 → F7 Bridge Behavior

The production bridge links citizen evidence to pollution events using the verified lineage:
- **Exact H3 Matching**: The citizen report's Uber H3 Index (resolution 8) must match the event H3 index (`88608850e5fffff`).
- **Temporal Window**: Report observation time must fall within 120 minutes of the event opening window.
- **Evidence Characteristics**:
  - `dataSource`: `CITIZEN`
  - `relevanceTier`: `AUXILIARY`
  - `evidenceKey`: `citizen-report-{id}`
  - `signalId`: `sig-citizen-{reportReference}`
- **Verified Runtime Record**:
  - Citizen Report: `CR-6F3366CB`
  - H3 Index: `88608850e5fffff`
  - Gemini Analysis UUID: `4c506a35-8342-43be-ac8f-2cc720e232da` (model: `gemini-3.1-flash-lite`, detected: `SMOKE_LIKE`, confidence: `0.95`)
  - Pollution Event: `58ef2f64-4bc5-496f-9094-e5f61e44f8b0` (`EVT-88608850-2026092816-f28bd5fe`)
  - EventEvidence UUID: `1b19ff8c-0b90-4d5e-9906-0802d1080e9c`

---

## 6. Alert Detail Citizen Evidence & Photo Security

- **Safe URL Pattern**: Citizen photo paths stored on the server filesystem (`uploads/citizen/...`) are transformed into relative HTTP resources:  
  `/api/v1/citizen/photos/{photoPath}`
- **No Path Leakage**: Internal OS paths (e.g. `C:\Users\...\uploads`) are never exposed in JSON responses.
- **Evaluator-Safe Language**: Visual observations are labeled with disclaimers:
  - *"Citizen visual observations provide auxiliary ground context analyzed via Gemini Vision and do not constitute official regulatory sensor measurements."*

---

## 7. Events API Security Defect Resolution

- **Issue**: Requests to `GET /api/v1/events` previously failed with HTTP 403 Forbidden because `/api/v1/events/**` was not configured in `SecurityConfig.java`.
- **Fix**: Registered `"/api/v1/events/**"` under `permitAll()` along with other read-only monitoring paths (`/api/v1/hotspots/**`, `/api/v1/forecast/**`, `/api/v1/evidence/**`, `/api/v1/citizen/**`).
- **Verification**: Verified via curl that `GET /api/v1/events` returns HTTP 200 OK with open events list, and `GET /api/v1/events/{id}` returns the specific event detail.

---

## 8. Real Map Foundation

- Audit confirmed Leaflet + React-Leaflet + H3-js stack is fully operational in `PollutionMap.tsx`.
- No Google Maps or Mapbox rewrites were introduced.
- Client maps submit coordinates (`latitude`, `longitude`); the backend Uber H3 Core engine remains the sole authoritative source of H3 index generation.

---

## 9. No-Match Safety & Idempotency

- **No-Match Reports**: If a citizen report is submitted for an H3 cell without an active matching `PollutionEvent`, the system persists the report and runs Gemini Vision, but generates **NO** `PollutionEvent`, **NO** `EventEvidence`, and **NO** `Alert`.
- **Idempotency**: Repeated orchestration of the same citizen report checks existing evidence keys before insertion, preventing duplicate evidence or alert candidates.

---

## 10. Non-Regression Verification

1. **F3 Hotspot Detection**: Random Forest model, calibrated probability, 0.20 operational threshold, and risk levels remain unmodified. Tested via `HotspotIntegrationTest`.
2. **F4 Forecasting**: Forecast regressors v1, horizons [1, 3, 6], and prediction reconstruction remain intact. Tested via `ForecastIntegrationTest` (11/11 pass).
3. **F5 Evidence Scoring**: Evidence weights, recency decay, conflict penalties, and triage rules remain strictly unchanged. Tested via `AlertUnitTest` (proving citizen evidence alone cannot create an alert candidate) and `EvidenceIntegrationTest`.
4. **F6 Citizen Reporting & Gemini Vision**: Photo storage, Gemini prompt versions, and spatio-temporal matching logic remain untouched. Tested via `CitizenReportIntegrationTest` (7/7 pass).

---

## 11. Data Invariants Proven

- **INVARIANT 1**: Citizen report H3 == Event H3 when matched (`88608850e5fffff`).
- **INVARIANT 2**: Citizen `EventEvidence` source == `CITIZEN`.
- **INVARIANT 3**: Citizen `EventEvidence` relevance == `AUXILIARY`.
- **INVARIANT 4**: Gemini visual confidence (0.95) remains strictly decoupled from F3 riskScore (0.7998).
- **INVARIANT 5**: Gemini visual confidence remains strictly decoupled from F5 evidenceScore.
- **INVARIANT 6**: Citizen auxiliary evidence alone cannot satisfy `ALERT_CANDIDATE` threshold or bypass F5 rules.
- **INVARIANT 7**: A no-match citizen report cannot fabricate an event or alert.
- **INVARIANT 8**: Repeated processing is idempotent; evidence key uniqueness prevents duplicates.
- **INVARIANT 9**: F3, F4, F5, and F6 engine outputs remain identical to baseline.
- **INVARIANT 10**: Frontend alert components consume real API fields with zero hardcoded IDs.

---

## 12. Definition of Done Checklist

| Requirement | Status |
|---|:---:|
| Existing F6 citizen evidence bridge still works | ✅ PASS |
| No duplicate citizen evidence logic created | ✅ PASS |
| Citizen evidence exposed to F7 alert/event context | ✅ PASS |
| Citizen photo safely rendered without path leakage | ✅ PASS |
| H3 lineage preserved | ✅ PASS |
| Gemini lineage preserved | ✅ PASS |
| Citizen evidence remains AUXILIARY | ✅ PASS |
| Citizen evidence cannot directly create high-risk alert | ✅ PASS |
| No-match behavior is safe | ✅ PASS |
| Duplicate processing is idempotent | ✅ PASS |
| `GET /api/v1/events` works without HTTP 403 | ✅ PASS |
| Existing F3 works | ✅ PASS |
| Existing F4 works | ✅ PASS |
| Existing F5 works | ✅ PASS |
| Existing F6 works | ✅ PASS |
| No mock data contaminates production F7 flow | ✅ PASS |
| Real runtime verification completed | ✅ PASS |
| Backend tests pass | ✅ PASS |
| Frontend tests pass | ✅ PASS |
| Typecheck passes | ✅ PASS |
| Production build passes | ✅ PASS |
| No accidental protected-feature changes | ✅ PASS |
| `F7_P2_IMPLEMENTATION_REPORT.md` created | ✅ PASS |

---

## Final Status
**F7-P2: PASS**
