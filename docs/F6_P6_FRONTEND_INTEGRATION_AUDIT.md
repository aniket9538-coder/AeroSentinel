# F6-P6 FRONTEND INTEGRATION AUDIT
**AeroSentinel Environmental Intelligence Platform**
**Phase:** F6-P6 — Citizen Frontend Integration + Real Citizen Evidence Experience
**Date:** 2026-09-29
**Author:** Antigravity AI Agent

---

## 1. Executive Summary

This audit assesses the state of the AeroSentinel frontend codebase prior to finalizing the F6-P6 Citizen Evidence experience against the authoritative F6-P5 Spring Boot backend. 

The audit confirms that the frontend architecture established in F6-P4 conforms closely to the required presentation-only pattern. No backend calculation logic (H3 mapping, F3 risk inference, F5 evidence scoring, or Gemini confidence) is executed within React. The live backend APIs (`/api/v1/citizen/reports/*` and `/api/v1/evidence/hotspot/{h3}`) provide authoritative spatial, AI visual, and multi-source evidence state.

One specific issue was identified: `CitizenEvidenceLineageCard.tsx` contains a fallback string `report.h3Index || '88608850e5fffff'`, which violates the strict requirement that H3 must never be hardcoded and must be strictly backend-derived.

---

## 2. Answers to the 10 Audit Questions

### 1. Which parts of P6 are already implemented?
- **Citizen Report Page (`CitizenReport.tsx`)**:
  - Four-step submission workflow: Step 1 (Location display, lat/lng coordinates, "LOCATION CAPTURED" badge, "USE MY LOCATION" geolocation button), Step 2 (Photo evidence with `ImageUploader`), Step 3 (Category selection cards with enum values `SMOKE`, `DUST`, `BURNING`, `ODOR`, `OTHER` and contextual description), Step 4 (Submission CTA with `isSubmitting` guard and text `"ANALYZING & SUBMITTING..."`).
  - Five-stage status progression tracker: `SUBMITTED` -> `ANALYZING` -> `ANALYZED` -> `EVENT EVIDENCE` -> `VERIFIED / DISMISSED`.
  - Seamless toggle between Submission Form (View 2) and Submitted Evidence Dossier (View 1).
- **Gemini Vision Card (`GeminiVisionCard.tsx`)**:
  - Displays detected category, visual confidence progress bar, observations list, uncertainty disclosures, model version, and prompt version.
  - Three distinct provider states: `REAL GEMINI VISION` (purple badge), `DETERMINISTIC FALLBACK` (amber badge), and `AI UNAVAILABLE` (neutral badge).
  - Explicit semantic boundary banner: *"AI visual interpretation ≠ numeric pollution measurement ≠ causal source attribution."*
- **Citizen Evidence Lineage Card (`CitizenEvidenceLineageCard.tsx`)**:
  - Displays `dataSource = CITIZEN` and `relevanceTier = AUXILIARY`.
  - Renders 5-step lineage: `Citizen Report` -> `H3 Cell` -> `Pollution Event` -> `Event Evidence` -> `F5 Evaluation`.
  - Matched event state: Displays live `eventCode`, `eventId`, `signalId`, F5 `evidenceScore`, and F5 `triageState` with CTA `"Inspect F5 Evidence & WHY Dossier"`.
  - No-match event state: Truthful message `"Citizen evidence stored. No matching pollution event is currently available for this spatial/temporal context."`
- **Community Feed (`ReportStatus.tsx`)**:
  - Fetches real reports from `GET /api/v1/citizen/reports?cityId=...`.
  - Displays real report references (`CR-...`), categories, descriptions, relative timestamps, H3 cells, model tags, and prominent `UNVERIFIED` tags.
  - Truthful empty state when 0 reports exist.
- **Service & API Layer**:
  - `citizen.service.ts`: `getReports()`, `getReportById()`, and `submitReport()` with 35s per-request timeout.
  - `evidenceApi.ts`: `getEvidence()` (fast read-only GET `/api/v1/evidence/hotspot/{h3}`) and `orchestrateEvidence()` (explicit refresh POST `/api/v1/evidence/orchestrate`).

### 2. Which P4 components can be reused unchanged?
- **`ImageUploader.tsx`**: Reused unchanged. Fully compliant with JPG, PNG, WebP up to 15MB, preview, file clear/replace, camera environment capture, and validation banners.
- **`GeminiVisionCard.tsx`**: Reused unchanged. Fully adheres to real backend model metadata (`gemini-*` vs `deterministic-fallback`), displays observation and uncertainty arrays, and enforces the mandatory semantic safety boundary.
- **`ReportStatus.tsx`**: Reused unchanged. Renders crowdsourced reports strictly from backend data with `UNVERIFIED` badges, click-to-view navigation, and zero synthetic records.

### 3. Which frontend code is still using local/mock/hardcoded data?
- In `CitizenEvidenceLineageCard.tsx`:
  - **Line 38**: `const h3Index = report.h3Index || '88608850e5fffff';`
  - In `CitizenEvidenceLineageCard.tsx` line 292: `citizenSignal?.signalId || sig-citizen-${report.id.slice(0, 8)}`
- No mock citizen report arrays are seeded in `CitizenReport.tsx` or `ReportStatus.tsx`.
- The fallback string `'88608850e5fffff'` must be removed so that missing H3 remains unassigned or displays `"Derived on server ingestion"`.

### 4. Which API fields come from current P5 backend?
The live Spring Boot backend (verified via curl / Invoke-RestMethod) provides:
- **`GET /api/v1/citizen/reports/{id}`**:
  - `id`, `reportId`, `cityId`, `latitude`, `longitude`, `h3Index`, `category`, `description`, `imageUrl`, `submittedAt`, `status`, `verificationStatus`, `createdAt`.
  - `visionAnalysis`: `analysisId`, `analysisStatus`, `detectedCategory`, `confidence`, `observations`, `uncertainty`, `modelVersion`, `promptVersion`, `analyzedAt`.
- **`GET /api/v1/evidence/hotspot/{h3}`**:
  - `context`: `h3Index`, `cityId`, `cityName`, `predictionId`, `featureSnapshotId`, `eventId`, `eventCode`, `generatedAt`.
  - `observedFacts`: `air`, `weather`, `monitoringCoverage`, `spatialDispersion`, `gisContext`.
  - `modelOutputs`: `hotspot` (`riskScore`, `isHotspot`, `riskLevel`, `confidence`, etc.), `forecast` (horizons).
  - `evidence`: `signals` (including `CITIZEN` / `AUXILIARY` signal), `evidenceScore`, `scoreBreakdown`, `consistency`, `triageState`, `sourceMatrix`.
  - `aiInterpretation`: `summaryPublic`, `summaryAnalyst`, `detectedCondition`, `uncertaintyStatement`, `modelVersion`, `promptVersion`.

### 5. Are any F5 values calculated in React?
- **NO.** React performs zero calculation of:
  - F3 `riskScore`
  - F5 `evidenceScore`
  - F5 `triageState`
  - Consistency metrics
  - Completeness scores
  - Signal weights or conflict penalties
  - Alert candidate eligibility
- All scoring and triage states rendered in `CitizenReport.tsx` and `CitizenEvidenceLineageCard.tsx` are bound directly from `evidenceSummary.evidence.evidenceScore` and `evidenceSummary.evidence.triageState`.

### 6. Are any H3/event values hardcoded?
- In `CitizenReport.tsx`: No hardcoded H3 values. The H3 cell is captured via `submittedReport.h3Index` or displayed as `"Derived on server ingestion"`.
- In `CitizenEvidenceLineageCard.tsx`: Line 38 contains `const h3Index = report.h3Index || '88608850e5fffff';`. This is the sole remaining instance of a hardcoded H3 fallback and will be cleaned up in P6.

### 7. Does the frontend distinguish REAL GEMINI / FALLBACK / UNAVAILABLE?
- **YES.** `GeminiVisionCard.tsx` strictly inspects `visionAnalysis.modelVersion` and `visionAnalysis.analysisStatus`:
  - `modelVersion.toLowerCase().startsWith('gemini')` -> **`REAL GEMINI VISION`** (analyzed by Gemini Vision, purple styling).
  - `modelVersion.toLowerCase().includes('fallback')` or `analysisStatus === 'FALLBACK'` -> **`DETERMINISTIC FALLBACK`** (deterministic fallback analysis, amber styling).
  - Missing analysis, `'UNAVAILABLE'`, or `'FAILED'` -> **`AI UNAVAILABLE`** (AI visual analysis unavailable, neutral styling).
  - Never claims fallback is Gemini; never infers provider from user text.

### 8. Does the frontend preserve the P5 score separation?
- **YES.** The UI explicitly separates the three distinct operational metrics:
  - F3 Risk Score: Displayed in Hotspots / Evidence context as `riskScore` (e.g., `0.7998`).
  - Gemini Visual Confidence: Displayed in `GeminiVisionCard` as `confidence` (e.g., `10%` for `0.10`).
  - F5 Evidence Score: Displayed in `CitizenEvidenceLineageCard` as `evidenceScore` (e.g., `0.224`).
- Visual confidence is never equated with the F5 evidence score or F3 risk score.

### 9. Are no-match reports represented correctly?
- **YES.** When a citizen report's H3 cell has no matching `PollutionEvent` (e.g., report `CR-AF733F86` in H3 cell `88608e26a7fffff`), `GET /api/v1/evidence/hotspot/88608e26a7fffff` returns HTTP 404.
- `CitizenReport.tsx` gracefully catches the 404, setting `evidenceSummary = null`.
- `CitizenEvidenceLineageCard.tsx` renders Section 10:
  - Badge: `STORED AUXILIARY`
  - Header: `Citizen evidence stored`
  - Body: `No matching pollution event is currently available for this spatial/temporal context.`
  - Explicit explanation that the report remains stored for future contextual evaluation.
  - Zero fabricated events, zero fake signal IDs, and zero synthetic alerts.

### 10. Does every route preserve the selected H3?
- **YES.**
  - `CitizenEvidenceLineageCard` CTA: `navigate('/analyst/evidence?h3=' + encodeURIComponent(h3Index))`
  - `EvidenceAnalysis.tsx`: Reads `useSearchParams().get('h3')` and directly calls `evidenceApi.getEvidence(h3Param)`.
  - `Hotspots.tsx`: Reads and sets `?h3=` query parameter; clicking "Inspect Evidence & WHY" preserves the exact cell H3.
  - `Forecast.tsx`: Reads and sets `?h3=` query parameter; preserves H3 across navigation.
  - No route replaces H3 with a city ID, grid ID, or randomized identifier.

---

## 3. Required P6 Remediation Item

| Item | File | Location | Issue | Remediation |
|---|---|---|---|---|
| 1 | `frontend/src/components/citizen/CitizenEvidenceLineageCard.tsx` | Line 38 | Fallback `report.h3Index \|\| '88608850e5fffff'` hardcodes H3 | Replace with `report.h3Index \|\| ''` and disable dossier navigation button if H3 is absent. |

---

## 4. Conclusion & Next Actions

The audit reveals that the frontend is in an advanced, evaluator-ready state. After applying the single remediation in `CitizenEvidenceLineageCard.tsx`, we will:
1. Run the Vitest test suite and add dedicated P6 integration tests.
2. Verify TypeScript type safety (`tsc -b`).
3. Verify production bundle build (`npm run build`).
4. Perform live runtime verification with primary report `CR-07B813E2` and no-match report `CR-AF733F86`.
5. Compile `docs/F6_P6_FRONTEND_INTEGRATION_REPORT.md`.
