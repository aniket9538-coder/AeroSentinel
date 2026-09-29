# F6-P6 FRONTEND INTEGRATION REPORT
**AeroSentinel Environmental Intelligence Platform**
**Phase:** F6-P6 — Frontend Integration + Real Citizen Evidence Experience
**Date:** 2026-09-29
**Author:** Antigravity AI Agent
**Status:** PASS ✅

---

## 1. Objective
The primary objective of F6-P6 is to finalize, connect, and rigorously verify the citizen-facing frontend experience against the authoritative F6-P5 backend truth:
$$\text{Citizen Report} \to \text{Spring Boot API} \to \text{H3 Res 8} \to \text{Real Gemini Vision} \to \text{GeminiAnalysis} \to \text{Citizen EventEvidence} \to \text{F5 Evidence Engine} \to \text{Evidence \& WHY}$$

The frontend acts as a **pure presentation and API consumption layer**. In strict accordance with platform design principles:
- The frontend **never calculates** H3 cell indices, F3 risk scores, F5 evidence scores, F5 triage states, event matching, or Gemini confidence.
- All spatial and AI scores are backend-owned and consumed verbatim.
- Zero mock, synthetic, or fabricated citizen data or events are introduced.
- Strict metric separation is preserved: $\text{F3 Hotspot riskScore (0.7998)} \neq \text{F5 evidenceScore (0.224)} \neq \text{Gemini visual confidence (0.10)}$.

---

## 2. P4 Components Reused
The evaluator-grade component system developed in F6-P4 has been preserved and reused without architectural disruption:
1. **`ImageUploader.tsx`**: Reused unchanged. Provides drag-and-drop file ingestion, device camera integration (`capture="environment"`), image preview with instant teardown, file size checks ($\le 15$ MB), format whitelisting (`image/jpeg`, `image/png`, `image/webp`), and accessible validation banners.
2. **`GeminiVisionCard.tsx`**: Reused unchanged. Renders structured multimodal visual analysis, detected condition, visual confidence progress meter, observation points, analytical uncertainty statements, model versions (`gemini-3.1-flash-lite`), provider badges (`REAL GEMINI VISION`, `DETERMINISTIC FALLBACK`, `AI UNAVAILABLE`), and modal zoom inspection.
3. **`ReportStatus.tsx`**: Reused unchanged. Renders crowdsourced ground records from `GET /api/v1/citizen/reports?cityId=...` with category chips, relative timestamps, H3 hashes, AI model badges, and prominent `UNVERIFIED` tags. Truthful empty state when zero records are returned.
4. **`CitizenEvidenceLineageCard.tsx`**: Reused with one targeted fix (removed hardcoded H3 fallback string `report.h3Index || '88608850e5fffff'`, replacing it with `report.h3Index || ''` and disabling the dossier navigation CTA if H3 is absent).

---

## 3. P5 Backend Contract Consumed
The frontend binds directly to the authoritative entities returned by Spring Boot:
- **`CitizenReport` Entity**:
  - `id`: UUID (`07b813e2-a17d-455f-9761-744c0989a6ce`)
  - `reportId`: Public audit reference (`CR-07B813E2`)
  - `h3Index`: 15-char Uber H3 Resolution 8 index (`88608850e5fffff`)
  - `category`: Observation category (`SMOKE`)
  - `status`: Lifecycle state (`ANALYZED`)
  - `verificationStatus`: Officer review state (`UNVERIFIED`)
  - `visionAnalysis`:
    - `analysisStatus`: `ANALYZED`
    - `detectedCategory`: `UNKNOWN`
    - `confidence`: `0.10`
    - `observations`: `["uniform grey field. no discernible environmental features. lack of visual data"]`
    - `uncertainty`: `["Image alone cannot determine numerical pollutant concentration. Image alone cannot establish regulatory source causality"]`
    - `modelVersion`: `gemini-3.1-flash-lite`
    - `promptVersion`: `vision_analysis_v001`
- **`EvidenceSummaryResponse` Entity**:
  - `context`: `h3Index`, `eventId` (`58ef2f64-4bc5-496f-9094-e5f61e44f8b0`), `eventCode` (`EVT-88608850-2026092816-f28bd5fe`)
  - `modelOutputs.hotspot`: `riskScore: 0.7998`, `riskLevel: CRITICAL`, `isHotspot: true`
  - `evidence`:
    - `signals`: Includes `dataSource: CITIZEN`, `relevanceTier: AUXILIARY`, `signalId: sig-citizen-07b813e2`, `confidenceScore: 0.1`
    - `evidenceScore`: `0.224`
    - `triageState`: `INSUFFICIENT_EVIDENCE`
    - `consistency`: `insufficient_evidence`
    - `scoreBreakdown`: `observationStrength: 0.2`, `mlForecastSupport: 0.552`, `multiSourceAgreement: 0.667`, `conflictPenalty: 0.35`, `finalEvidenceScore: 0.224`

---

## 4. Frontend API Connections
1. **`citizenService.submitReport(formData)`**:
   - `POST /api/v1/citizen/reports` (multipart/form-data)
   - Configured with a dedicated per-request timeout of 35,000 ms to support live multimodal Gemini execution without modifying global Axios timeouts.
2. **`citizenService.getReportById(reportId)`**:
   - `GET /api/v1/citizen/reports/{reportId}`
   - Retrieves full report detail including nested `visionAnalysis`.
3. **`citizenService.getReports(cityId)`**:
   - `GET /api/v1/citizen/reports?cityId={cityId}`
   - Powers the live community feed on `/report`.
4. **`evidenceService.getEvidenceByH3(h3Index)`**:
   - `GET /api/v1/evidence/hotspot/{h3Index}`
   - Persisted, fast read-only query (<220 ms) that does not trigger expensive AI re-orchestration on page view.
5. **`evidenceApi.orchestrateEvidence(h3Index)`**:
   - `POST /api/v1/evidence/orchestrate?h3Index={h3Index}`
   - Reserved strictly for explicit manual re-orchestration actions.

---

## 5. Citizen Report Flow
The four-step submission experience on `/report` operates as follows:
- **Step 1 — Location**: Captures geographic latitude/longitude. Displays a green `"LOCATION CAPTURED"` status pill with a button to `"USE MY LOCATION"`. Explains clearly that H3 Resolution 8 spatial cells ($\approx 460$m radius) are derived exclusively on backend ingestion.
- **Step 2 — Photo Evidence**: Integrates `ImageUploader` for ground photography up to 15MB in JPG, PNG, or WebP format with live preview and camera trigger.
- **Step 3 — Observation Category & Description**: Category selection cards for `SMOKE`, `DUST`, `BURNING`, `ODOR`, and `OTHER`. Contextual description textarea with guidance to note emission duration, color, and proximity to sensitive receptors.
- **Step 4 — Submit**: Submission CTA displaying `"ANALYZING & SUBMITTING..."` while in-flight, preventing accidental duplicate clicks.

---

## 6. Gemini Display
Rendered via `GeminiVisionCard.tsx`:
- **Real Gemini Vision**: Detected when `modelVersion.startsWith('gemini')`. Renders purple `REAL GEMINI VISION` badge, model version (`gemini-3.1-flash-lite`), visual confidence (e.g., `10%` or `95%`), and observations.
- **Deterministic Fallback**: Detected when `modelVersion.includes('fallback')` or status is `FALLBACK`. Renders amber `DETERMINISTIC FALLBACK` badge with fallback rationale.
- **AI Unavailable**: Renders neutral `AI UNAVAILABLE` badge when analysis is absent or degraded.
- **Semantic Safety Boundary**: Prominently highlights:
  > **Semantic Boundary Notice**: AI visual interpretation $\neq$ numeric pollution measurement $\neq$ causal source attribution. Visual condition models evaluate visible ground plumes as auxiliary signals to corroborate physical sensor networks.

---

## 7. Citizen Evidence Display
Rendered via `CitizenEvidenceLineageCard.tsx`:
- Attributes: Report Reference (`CR-...`), H3 Spatial Cell (Res 8), Data Source (`CITIZEN`), Relevance Tier (`AUXILIARY`), Visual Category, Observation Confidence, Observation Timestamp.
- Prominent badge: `UNVERIFIED CITIZEN REPORT` (or `VERIFIED` only if confirmed by authority officer).
- Tier badge tooltip: *"Auxiliary evidence corroborates sensor trends but cannot independently trigger an alert."*

---

## 8. Event Lineage
For reports with an active matched `PollutionEvent` (e.g., `CR-07B813E2` in cell `88608850e5fffff`):
- Displays a 5-step horizontal progression:
  1. **Step 1 — Citizen Report**: `CR-07B813E2`
  2. **Step 2 — H3 Cell**: `88608850e5...`
  3. **Step 3 — Pollution Event**: `EVT-88608850...`
  4. **Step 4 — Event Evidence**: `sig-citizen-07b813e2`
  5. **Step 5 — F5 Evaluation**: `Score: 0.224`
- Contextual text: *"This citizen observation is attached as AUXILIARY corroboration to active event EVT-88608850-2026092816-f28bd5fe. Current F5 Triage State: INSUFFICIENT_EVIDENCE."*
- CTA Button: `"Inspect F5 Evidence & WHY Dossier"` navigating directly to `/analyst/evidence?h3=88608850e5fffff`.

---

## 9. F5 Score & Triage Display
- Authoritative scores consumed directly from the backend without any React computation:
  - F5 Evidence Score: `0.224`
  - F5 Triage State: `INSUFFICIENT_EVIDENCE`
  - F5 Consistency: `insufficient_evidence`
- The UI maintains strict separation between the different metrics:
  - F3 Hotspot Risk Score: `0.7998` (CRITICAL)
  - Gemini Visual Confidence: `10%` (`0.10`)
  - F5 Evidence Score: `0.224`
  - F5 Triage State: `INSUFFICIENT_EVIDENCE`

---

## 10. No-Match Handling
For reports registered in a spatial sector without an active pollution event (e.g., `CR-AF733F86` in H3 cell `88608e26a7fffff`):
- `GET /api/v1/evidence/hotspot/88608e26a7fffff` returns HTTP 404.
- `CitizenEvidenceLineageCard.tsx` renders the truthful auxiliary state:
  - Badge: `STORED AUXILIARY`
  - Title: `Citizen evidence stored`
  - Body: *"No matching pollution event is currently available for this spatial/temporal context. Your observation is securely registered with H3 cell 88608e26a7fffff. The evidence remains stored for future contextual evaluation..."*
  - Safety Invariant: *"Locked Safety Invariant: Citizen observations alone never fabricate a synthetic event or elevate an alert candidate state."*
- Zero synthetic events, zero fake signal IDs, and zero synthetic scores are generated.

---

## 11. Community Feed
Rendered by `ReportStatus.tsx` using `GET /api/v1/citizen/reports?cityId=...`:
- Live records display category, reference (`CR-...`), truncated description, H3 cell hash, relative timestamp, AI model state, and `UNVERIFIED` tags.
- Clicking any card navigates to `/report/status/{reportId}`.
- If no records exist, displays a clean empty state: *"No recent citizen evidence submitted in this sector."*

---

## 12. Cross-Page H3 Navigation
All cross-page navigation links preserve the exact 15-character H3 index:
- **Citizen Report** $\to$ `/report/status/{reportId}`: preserves report's backend H3 cell.
- **Citizen Evidence Lineage** $\to$ `/analyst/evidence?h3=88608850e5fffff`: preserves exact H3.
- **Hotspots** $\to$ `/analyst/evidence?h3=88608850e5fffff`: preserves exact H3.
- **Forecast** $\to$ `/analyst/evidence?h3=88608850e5fffff`: preserves exact H3.
- **Alerts** $\to$ `/analyst/evidence?h3=88608850e5fffff`: preserves exact H3.
- Neither city IDs nor synthetic strings overwrite the H3 parameter.

---

## 13. Accessibility
- All file upload inputs include semantic `aria-label` attributes (`"Upload citizen observation photo"` and `"Capture citizen observation photo with camera"`).
- Image preview includes descriptive `alt` text (`"Environmental observation preview"`).
- Validation and submission error banners use `role="alert"`.
- Category buttons utilize `aria-pressed={isSelected}` for screen reader state announcements.
- Status progression points include explicit text alternatives (`"Registered"`, `"Vision Pipeline"`, `"Pending Review"`).
- High contrast color tokens ensure text readability across dark surfaces.

---

## 14. Responsive Behavior
- Desktop: Two-column layout with submission form on the left ($1.4\times$) and community feed on the right ($1.0\times$); submitted dossier displays full-width cards.
- Tablet / Mobile: Grid collapses smoothly to a single-column layout with full touch-friendly target sizes ($>44\text{px}$).

---

## 15. Failure States
- **Degraded AI**: If the report is successfully stored in PostgreSQL but Gemini Vision times out or fails, the UI displays an amber alert: *"Report Persisted Successfully: Vision analysis is currently degraded or queued. Your report has been securely registered in H3 cell..."*
- **Network / API Error**: If the server fails to persist the report, an error banner with `role="alert"` clearly presents the error message.
- **File Validation**: Image format errors and size violations (>15MB) are trapped client-side before dispatch with inline warnings.

---

## 16. Tests
- **P6-Specific Tests**: 25 comprehensive tests written in `frontend/src/utils/f6_p6_citizen_frontend.test.ts` covering all 25 prompt test requirements.
- **Full Test Suite Result**: **203 tests across 13 test suites**, all passing with 0 failures:
  - `f6_p6_citizen_frontend.test.ts`: 25 passed
  - `f6_p4_citizen_ux.test.ts`: 20 passed
  - `f6_p3_citizen_evidence.test.ts`: 15 passed
  - `f6_pre_p4_ux_cleanup.test.ts`: 8 passed
  - `citizen_reporting.test.ts`: 12 passed
  - `evidence.test.ts`: 12 passed
  - `f5_p7_failure_recovery.test.ts`: 18 passed
  - `f5_ui_polish.test.ts`: 10 passed
  - `alerts.test.ts`: 10 passed
  - `inspections.test.ts`: 12 passed
  - `forecast.test.ts`: 25 passed
  - `hotspot.test.ts`: 20 passed
  - `freshness.test.ts`: 16 passed

---

## 17. TypeScript
- Type check executed via `npx tsc -b`.
- **Result**: Zero errors. Clean compilation.

---

## 18. Production Build
- Vite production build executed via `npm run build` (`tsc -b && vite build`).
- **Result**: Success (`✓ built in 54.24s`). Outputs:
  - `dist/index.html` (1.21 kB)
  - `dist/assets/index-M2dPVmeZ.css` (33.08 kB)
  - `dist/assets/index-DPyFqqnu.js` (1,387.03 kB)

---

## 19. Real Runtime Verification
Verified against live Spring Boot API on `http://localhost:8080` and Vite dev server on `http://localhost:3000`:

### Primary Report: `CR-07B813E2` (`07b813e2-a17d-455f-9761-744c0989a6ce`)
- **API URL**: `GET /api/v1/citizen/reports/07b813e2-a17d-455f-9761-744c0989a6ce`
- **H3 Cell**: `88608850e5fffff`
- **Gemini Model**: `gemini-3.1-flash-lite`
- **Visual Confidence**: `0.10` (10%)
- **Status**: `ANALYZED`
- **Verification Status**: `UNVERIFIED`
- **Event Linkage**: `EVT-88608850-2026092816-f28bd5fe` (Event ID: `58ef2f64-4bc5-496f-9094-e5f61e44f8b0`)
- **Citizen Signal ID**: `sig-citizen-07b813e2` (`relevanceTier: AUXILIARY`)
- **F5 Evidence Score**: `0.224`
- **F5 Triage State**: `INSUFFICIENT_EVIDENCE`
- **F3 Hotspot Risk Score**: `0.7998` (CRITICAL)

### No-Match Report: `CR-AF733F86` (`af733f86-9d6c-4a9a-8275-553b6f7f4b25`)
- **API URL**: `GET /api/v1/citizen/reports/af733f86-9d6c-4a9a-8275-553b6f7f4b25`
- **H3 Cell**: `88608e26a7fffff`
- **Gemini Model**: `gemini-3.1-flash-lite`
- **Visual Confidence**: `0.95` (95%)
- **Evidence Response**: HTTP 404 (No active event in spatial sector)
- **UI State**: Truthful no-match panel (`STORED AUXILIARY`, *"No matching pollution event is currently available for this spatial/temporal context"*), zero fabricated event.

---

## 20. Verification Screenshots

| Figure | Description | File |
|---|---|---|
| 1 | Citizen Report 4-Step Submission Form & Live Feed | `f6_p6_01_report_form.png` |
| 2 | Primary Report Status Progression & Gemini Vision Card | `f6_p6_02_primary_report_status.png` |
| 3 | Primary Report Citizen Evidence & Lineage Card (F5 Score 0.224) | `f6_p6_02b_primary_lineage_card.png` |
| 4 | No-Match Report Truthful Stored Auxiliary State (CR-AF733F86) | `f6_p6_03_nomatch_report_status.png` |
| 5 | Navigated Evidence & WHY Dossier Preserving H3 `88608850e5fffff` | `f6_p6_04_evidence_why_navigated.png` |

---

## 21. Changed Files
1. `frontend/src/components/citizen/CitizenEvidenceLineageCard.tsx`: Removed hardcoded H3 fallback string `'88608850e5fffff'`, replaced with `report.h3Index || ''`, rendered `"Derived on server ingestion"` when pending, and added `disabled={!h3Index}` guard to the navigation CTA button.
2. `frontend/src/utils/f6_p6_citizen_frontend.test.ts`: Created new comprehensive 25-test suite covering all prompt requirements.
3. `docs/F6_P6_FRONTEND_INTEGRATION_AUDIT.md`: Created preliminary audit document answering the 10 prompt questions before modifications.
4. `docs/F6_P6_FRONTEND_INTEGRATION_REPORT.md`: This comprehensive implementation and verification report.

---

## 22. Remaining Limitations
- Direct officer verification workflow (`POST /api/v1/citizen/reports/{id}/verify`) is an authority workflow reserved for administrative officers and is intentionally represented as unverified pending officer inspection.
- When an H3 cell lacks an active `PollutionEvent`, the citizen report remains stored as auxiliary evidence in PostgreSQL and will corroborate future sensor anomalies without fabricating synthetic events.
