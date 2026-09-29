# F6-P4: Evaluator-Quality Citizen UX + Real Evidence Visualization Report

## 1. P4 Objective
The objective of Phase F6-P4 is to deliver an **evaluator-quality, production-ready Citizen Report user experience and live evidence visualization** on top of the established and verified F6 backend, data models, and Gemini Vision pipeline.

The fundamental architectural flow and semantic boundaries are strictly preserved:
```
Citizen Observation
  ↓
Submission (Location + Category + Description + Photo)
  ↓
Uber H3 Resolution 8 Spatial Mapping
  ↓
Real Google Gemini Vision Multimodal Inference (or Deterministic Fallback)
  ↓
Structured Visual Analysis (Category, Confidence, Observations, Uncertainty)
  ↓
Database Persistence (citizen_reports, gemini_analyses)
  ↓
Citizen Evidence (dataSource = CITIZEN, relevanceTier = AUXILIARY)
  ↓
Spatial & Temporal Cross-Corroboration with Pollution Event / F5 Evidence Engine
```

### Locked Architectural Invariants Preserved
1. **Auxiliary Ground Corroboration**: Citizen evidence is strictly auxiliary. It never proves regulatory source causality and cannot independently trigger an `ALERT_CANDIDATE` or high-priority incident.
2. **Authoritative Backend Data Only**: No hardcoded report IDs, synthetic H3 cells, fabricated event codes, or mock scores. The frontend exclusively consumes live responses from Spring Boot (`/api/v1/citizen/...`, `/api/v1/evidence/...`).
3. **No Direct Gemini Calls from Frontend**: Architecture remains `React → Spring Boot → AI Service → Google Gemini API`. Frontend never accesses or stores API credentials.
4. **Semantic Distinction**: AI visual interpretation $\neq$ numeric pollution measurement $\neq$ causal source attribution.

---

## 2. UX Changes
1. **Four-Step Reporting Workflow**:
   - **Step 1 — Location**: Visual indicator for Uber H3 Resolution 8 (&asymp; 460m radius hexagon), coordinates capture (`Lat, Lng`), "Location Captured" confirmation badge, and "USE MY LOCATION" GPS integration.
   - **Step 2 — Photo Evidence**: Drag-and-drop / browse / camera uploader with client validation feedback (formats JPG, PNG, WebP; max size 15MB), image preview, and replace/remove controls.
   - **Step 3 — Observation**: 5 structured category cards (`SMOKE`, `DUST`, `BURNING`, `ODOR`, `OTHER`) with descriptive subtext and contextual multiline description textarea.
   - **Step 4 — Submit**: High-visibility CTA with in-flight disablement and loading indicator (`ANALYZING & SUBMITTING...`).

2. **Five-Stage Status Progression Tracker**:
   - Clean, chronological status sequence:
     $$\text{SUBMITTED} \to \text{ANALYZING} \to \text{ANALYZED} \to \text{EVENT EVIDENCE} \to \text{VERIFIED / DISMISSED}$$
   - Displays real backend states: completed steps get solid teal check indicators; unverified reports display "Pending Review" with `UNVERIFIED` tags, never claiming verification before field inspector review.

3. **Two-Column Evidence Layout**:
   - **Left**: Raw citizen photo evidence under tamper-evident audit control, zoom modal on click, and responsive height.
   - **Right**: Multimodal AI interpretation with confidence score progress bar, structured observations bullet list, and analytical limitations / uncertainty callout.

4. **Recent Community Evidence Feed**:
   - Live crowdsourced feed on right column showing recent city reports. Each card displays public report reference, category chip, description, timestamp, H3 cell, status badge, and `UNVERIFIED` tag. Clicking inspects report dossier.

---

## 3. Components & Files Changed

| File | Change Description |
| :--- | :--- |
| `frontend/src/components/citizen/GeminiVisionCard.tsx` | **New Component**: Evaluator-quality split layout card displaying citizen photo on left and structured AI visual interpretation on right. Explicitly distinguishes Real Gemini vs Fallback vs Unavailable. Includes prominent semantic boundary disclaimer. |
| `frontend/src/components/citizen/CitizenEvidenceLineageCard.tsx` | **New Component**: Displays Citizen Evidence attributes (`dataSource=CITIZEN`, `relevanceTier=AUXILIARY`), compact 5-step lineage flow (`Citizen Report → H3 Cell → Pollution Event → Event Evidence → F5 Evaluation`), and clean No-Match state when unattached. |
| `frontend/src/components/citizen/ImageUploader.tsx` | **Enhanced Component**: Added client-side size validation ($\le 15\text{ MB}$), MIME type validation (`image/jpeg`, `image/png`, `image/webp`), error alert banner, attached photo state, and accessible replace/remove controls. |
| `frontend/src/components/citizen/ReportStatus.tsx` | **Enhanced Component**: Renders live community evidence cards with public report reference, status badges, model tags, and accessible keyboard navigation (`tabIndex={0}`). |
| `frontend/src/pages/public/CitizenReport.tsx` | **Refactored Page**: Integrated 4-step reporting form, 5-stage progression tracker, `GeminiVisionCard`, and `CitizenEvidenceLineageCard`. Implemented failure recovery so persisted reports with degraded AI are never reported as failed. |
| `frontend/src/utils/f6_p4_citizen_ux.test.ts` | **New Test Suite**: Comprehensive 14-area automated verification covering all user prompt requirements. |

---

## 4. API Reuse & Backend Integrity
All backend endpoints are reused as designed without duplication:
- `POST /api/v1/citizen/reports` (Multipart form-data: `cityId`, `latitude`, `longitude`, `category`, `description`, `photo`, `observedAt`)
- `GET /api/v1/citizen/reports/{reportId}` (Fetches single report with attached `VisionAnalysisSummaryDto`)
- `GET /api/v1/citizen/reports?cityId={cityId}` (Lists recent community reports for the selected city)
- `GET /api/v1/citizen/photos/{storageKey}` (Securely serves stored citizen photo evidence)
- `GET /api/v1/evidence/hotspot/{h3Index}` (Fetches unified F5 evidence summary, active event code, and contributing signals)

---

## 5. Real Gemini Presentation vs Fallback vs Unavailable
The UI strictly derives the analysis provider from backend metadata (`report.visionAnalysis.modelVersion` and `report.visionAnalysis.analysisStatus`):

| State | Detection Rule | UI Header & Badge | Description |
| :--- | :--- | :--- | :--- |
| **REAL GEMINI** | `modelVersion.startsWith('gemini')` | Header: "Analyzed by Gemini Vision"<br>Badge: `REAL GEMINI VISION` (Purple/Teal) | Multimodal neural inference executed by Google Gemini Vision. |
| **DETERMINISTIC FALLBACK** | `modelVersion === 'deterministic-fallback'` or `analysisStatus === 'FALLBACK'` | Header: "Deterministic fallback analysis"<br>Badge: `DETERMINISTIC FALLBACK` (Amber) | Heuristic rule-based fallback executed when AI service is offline or watchdog times out. |
| **AI UNAVAILABLE** | `!visionAnalysis` or status `UNAVAILABLE` | Header: "AI visual analysis unavailable"<br>Badge: `AI UNAVAILABLE` (Neutral/Gray) | Report was submitted without photo or image was unreadable. |

### Semantic Boundary Notice
Every report analysis includes the mandatory disclaimer:
> **Semantic Boundary Notice**: AI visual interpretation $\neq$ numeric pollution measurement $\neq$ causal source attribution. Visual condition models evaluate visible ground plumes as auxiliary signals to corroborate physical sensor networks.

---

## 6. Citizen Evidence Visualization
The `CitizenEvidenceLineageCard` renders authoritative attributes according to the locked evidence contract:
- **Report Reference**: e.g., `CR-07B813E2`
- **H3 Spatial Cell (Resolution 8)**: e.g., `88608850e5fffff`
- **Data Source**: `CITIZEN`
- **Relevance Tier**: `AUXILIARY` (corroborates sensor trends; cannot independently elevate alert candidate state)
- **Visual Category**: Real detected category from model (e.g. `SMOKE_LIKE` or `UNKNOWN`)
- **Observation Confidence**: Formatted percentage (e.g. `95%` or `10%`)
- **Timestamp**: Observed / submitted ISO timestamp
- **Verification Status**: Clearly labeled `UNVERIFIED CITIZEN REPORT`

---

## 7. Event Lineage Presentation
When the citizen report has been attached to an existing pollution event (such as `CR-07B813E2` attached to `EVT-88608850-2026092816-f28bd5fe`):
- Compact 5-step lineage flow:
  $$\text{Citizen Report (CR-07B813E2)} \to \text{H3 Cell (88608850e5...)} \to \text{Pollution Event (EVT-886088...)} \to \text{Event Evidence (sig-citizen-...)} \to \text{F5 Evaluation (Score 0.224)}$$
- Displays real event ID and event code.
- Includes a direct action button: **"Inspect F5 Evidence & WHY Dossier →"**, navigating seamlessly to `/analyst/evidence?h3=88608850e5fffff`.

---

## 8. No-Match State
When a report has no matching active PollutionEvent in its H3 cell (e.g., `CR-AF733F86` in H3 `88608e26a7fffff`):
- **Headline**: "Citizen evidence stored"
- **Body**: "No matching pollution event is currently available for this spatial/temporal context."
- **Explanation**: "Your observation is securely registered with H3 cell `88608e26a7fffff`. The evidence remains stored for future contextual evaluation. When stationary air quality monitors or satellite telemetry detect an anomaly in this spatial sector, this record will be automatically evaluated as auxiliary evidence."
- **Locked Safety Invariant**: Explicitly stated that citizen evidence alone never fabricates a synthetic event.

---

## 9. Failure State Handling
- **Missing Location**: Validation check prevents submission, alerting: "Target coordinates are required to map observation to an H3 spatial cell."
- **Image Size Limit**: `ImageUploader` checks file size before upload; files $> 15\text{MB}$ display: "Image size exceeds 15MB limit. Please attach a compressed photo."
- **Unsupported Format**: Rejects non-JPG/PNG/WebP formats with clear warning.
- **Report Persisted with Degraded/Failed AI**: If the backend successfully stores the citizen report in PostgreSQL but AI inference times out or fails, the UI displays:
  > **Report Persisted Successfully:** Vision analysis is currently degraded or queued. Your report has been securely registered in H3 cell and will be evaluated as auxiliary evidence.
  *(It does NOT claim submission failed).*

---

## 10. Accessibility (a11y)
- **Image Alt Text**: Detailed dynamic alt text (`alt={`Citizen environmental observation evidence for ${reportRef}`}`).
- **ARIA Attributes**: `role="progressbar"`, `aria-valuenow`, `aria-valuemin="0"`, `aria-valuemax="100"` on confidence meters.
- **Interactive Controls**: Semantic buttons with visible focus rings and accessible labels (`aria-label="Upload citizen observation photo"`, `aria-label="Expand citizen photo"`).
- **Color Independence**: Badges and status indicators pair color coding with clear textual labels (`UNVERIFIED`, `REAL GEMINI VISION`, `EVENT MATCHED`, etc.).

---

## 11. Automated Test Suite Verification

### Full Frontend Test Suite
Executed via:
```bash
npx tsx --test src/utils/*.test.ts
```
**Results**:
- Total tests executed: **178**
- Passed: **178**
- Failed: **0**
- Skipped: **0**
- Total test files: **13** (`f6_p4_citizen_ux.test.ts`, `citizen_reporting.test.ts`, `f6_p3_citizen_evidence.test.ts`, `f5_ui_polish.test.ts`, `f5_p6_field_assignment.test.ts`, `f5_p7_failure_recovery.test.ts`, `f3_city_switching.test.ts`, `hotspots.test.ts`, `weather.test.ts`, `air_quality.test.ts`, `authority_alerts.test.ts`, `inspections.test.ts`, `evidence.test.ts`)

### 14 Required F6-P4 Subtests in `f6_p4_citizen_ux.test.ts`
1. `Report form renders real fields: coordinates, H3 indicator, uploader, categories, description` — **PASS**
2. `Photo preview renders properly with remove and replace actions` — **PASS**
3. `Submit loading state disables button and shows progress text` — **PASS**
4. `Successful report status renders clean 5-stage progression` — **PASS**
5. `Real Gemini result rendering displays "Analyzed by Gemini Vision" and REAL GEMINI badge` — **PASS**
6. `Fallback rendering displays "Deterministic fallback analysis" without calling it Gemini` — **PASS**
7. `Unavailable AI rendering displays "AI visual analysis unavailable" cleanly` — **PASS**
8. `Confidence rendering calculates percentage and provides accessible progressbar` — **PASS**
9. `Uncertainty rendering displays limitations and non-attribution statement` — **PASS**
10. `Citizen evidence rendering classifies dataSource as CITIZEN and relevance as AUXILIARY` — **PASS**
11. `Event lineage rendering displays 5-step flow and links to /analyst/evidence?h3=` — **PASS**
12. `No-match event state displays "Citizen evidence stored" without fabricating synthetic events` — **PASS**
13. `Failure state handles missing location, oversized images, and preserved reports with degraded AI` — **PASS**
14. `Accessibility-critical labels: alt text, aria-labels, buttons, and high-contrast badges` — **PASS**

---

## 12. TypeScript Typecheck
Executed via:
```bash
npx tsc -b
```
**Exit Code**: `0` (Zero compiler errors).

---

## 13. Production Build
Executed via:
```bash
npm run build
```
**Result**:
- Vite build completed in `16.82s`.
- Bundled assets: `dist/assets/index-DYB6P5O9.js` (376 kB gzip), `dist/assets/index-M2dPVmeZ.css` (6.98 kB gzip).
- Exit Code: `0`.

---

## 14. Real Runtime Verification

### Verification Case 1: Real Gemini-Verified Report with Event Lineage
- **Report ID**: `07b813e2-a17d-455f-9761-744c0989a6ce`
- **Public Reference**: `CR-07B813E2`
- **Model Version**: `gemini-3.1-flash-lite` (Real Gemini Vision)
- **H3 Cell**: `88608850e5fffff`
- **Matched Pollution Event**: `EVT-88608850-2026092816-f28bd5fe` (`58ef2f64-4bc5-496f-9094-e5f61e44f8b0`)
- **Event Evidence Signal**: `sig-citizen-07b813e2` (`dataSource = CITIZEN`, `relevanceTier = AUXILIARY`)
- **F5 Evidence Score**: `0.224` (`INSUFFICIENT_EVIDENCE`)
- **UI Verification**:
  - Displays `REAL GEMINI VISION` badge and "Analyzed by Gemini Vision".
  - Shows raw ground photo on left, detected category on right, observations, and uncertainty.
  - Shows 5-stage progression: `SUBMITTED → ANALYZING → ANALYZED → EVENT EVIDENCE → VERIFIED (Pending Review)`.
  - Shows active environmental lineage with direct button to `/analyst/evidence?h3=88608850e5fffff`.

### Verification Case 2: Real Gemini Report without Active Event (No-Match State)
- **Report ID**: `af733f86-9d6c-4a9a-8275-553b6f7f4b25`
- **Public Reference**: `CR-AF733F86`
- **Model Version**: `gemini-3.1-flash-lite`
- **H3 Cell**: `88608e26a7fffff`
- **Matched Event**: `None` (404 on hotspot lookup)
- **UI Verification**:
  - Displays `Citizen evidence stored`.
  - Explains: "No matching pollution event is currently available for this spatial/temporal context."
  - Confirms evidence is stored in Resolution 8 cell `88608e26a7fffff` for future contextual evaluation.
  - Zero fabricated events.

---

## 15. Limitations
1. **Auxiliary Weighting Ceiling**: By design, citizen reports carry auxiliary weight ($w \approx 0.1 - 0.2$) and cannot independently trigger municipal inspection dispatch without ground sensor or satellite corroboration.
2. **Camera Permissions**: Taking a photo directly through the web UI relies on browser HTML5 Media Devices / input capture API, which requires user camera permission.

---

## 16. Final Status
All criteria for F6-P4 have been implemented and verified against live backend data and existing test suites.

- Live backend data consumed: **YES**
- Real Gemini vs Fallback distinguished: **YES**
- Zero fabricated evidence or events: **YES**
- F6 semantic boundaries preserved: **YES**
- Unit & integration tests pass (178/178): **YES**
- TypeScript compilation passes (`tsc -b` code 0): **YES**
- Production build passes (`npm run build` code 0): **YES**
- Live runtime verified with real reports: **YES**

```
============================================================
F6-P4 STATUS: PASS
============================================================
```
