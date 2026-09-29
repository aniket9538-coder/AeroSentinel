# AeroSentinel — Pre-F7 System Stabilization Audit
**Audit Date:** 2026-09-29  
**Target:** Pre-F7 Full Runtime, Connectivity, and Data Truthfulness Audit  
**Status:** AUDIT COMPLETE — BASELINE RECORDED

---

## 1. Executive Summary

This audit establishes the baseline state of AeroSentinel prior to commencing Pre-F7 Stabilization fixes. Features F0 through F6 have been developed and verified. The purpose of this audit is to identify exact runtime bottlenecks, timeout configurations, and data truthfulness discrepancies across the stack before applying minimal, targeted stabilization changes.

---

## 2. Frontend Layer Audit

### 2.1 API Base URL & Global Axios Client
- **File:** `frontend/src/services/api.ts`
- **Base URL:** `import.meta.env?.VITE_API_BASE_URL || '/api/v1'` (proxied by Vite to `http://localhost:8080`)
- **Global Timeout:** `10000ms` (10 seconds)
- **Finding:** The 10,000 ms global timeout is strictly enforced on all standard requests. Any endpoint taking longer than 10 seconds (such as evidence dossier reads triggering live AI evaluation) gets aborted by Axios with: `"timeout of 10000ms exceeded"`.

### 2.2 Citizen Service
- **File:** `frontend/src/services/citizen.service.ts`
- **Endpoints:**
  - `GET /citizen/reports?cityId={cityId}` (default 10s timeout)
  - `GET /citizen/reports/{reportId}` (default 10s timeout)
  - `POST /citizen/reports` (`multipart/form-data`, override timeout: `20000ms`)
- **Finding:** Submission timeout is 20s (`20000ms`), but if the backend watchdog is adjusted to 30s to allow real Gemini vision inference, this 20s client timeout would abort before backend completion. It must be bounded to 35s.

### 2.3 Evidence API
- **File:** `frontend/src/services/evidenceApi.ts`
- **Endpoints:**
  - `GET /evidence/hotspot/{h3Index}` (uses global default 10s timeout)
- **Finding:** `getEvidence` lacks a dedicated request timeout and relies on the 10,000 ms global timeout. Because the backend `GET /api/v1/evidence/hotspot/{h3Index}` runs Python CLI inference + Gemini synchronously on every call (taking 8–15 seconds), the frontend consistently times out with `"Failed to Retrieve Evidence Dossier: timeout of 10000ms exceeded"`.

### 2.4 Forecast API & Forecast Page
- **Files:** `frontend/src/services/forecastApi.ts`, `frontend/src/pages/public/Forecast.tsx`
- **Freshness Contract:**
  - `age <= 2h` $\to$ `LIVE`
  - `2h < age <= 24h` $\to$ `STALE`
  - `age > 24h` $\to$ `UNAVAILABLE`
  - `404 / missing` $\to$ `NO_DATA`
- **Finding:**
  1. The UNAVAILABLE banner in `Forecast.tsx` (line 314) contains misleading copy:
     `"Forecast telemetry is currently unavailable for this cell. Atmospheric sensors or inference regressors are undergoing scheduled recalibration."`
     This claim is untruthful because no scheduled recalibration is occurring; the forecast is simply older than 24 hours or telemetry has aged.
  2. For `88608850e5fffff` (Shivajinagar, Pune), persisted forecast is ~15h old, rendering as `STALE` with amber banner (truthful).
  3. Other preset cells (`88608852c1fffff` Pune Katraj, `8860885357fffff` Pune Hadapsar, `88608b56b3fffff` Mumbai Kurla, `883da11505fffff` Delhi R K Puram) are older than 42 hours ($>24\text{h}$) and render as `UNAVAILABLE`.

### 2.5 Alert API & Sidebar Count
- **Files:** `frontend/src/services/alertApi.ts`, `frontend/src/components/layout/Sidebar.tsx`, `frontend/src/hooks/useAuthorityQueue.ts`
- **Endpoints:**
  - `GET /alerts/authority`
  - `GET /alerts/{alertId}`
  - `PATCH /alerts/{alertId}/acknowledge`
  - `PATCH /alerts/{alertId}/resolve`
- **Sidebar Count Logic:**
  - `Sidebar.tsx` computes: `items.filter((item) => item.status && item.status !== 'RESOLVED').length`
  - Badge is rendered only if `activeAlertCount > 0`.
- **Finding:** When `alerts` table has 0 rows, `GET /api/v1/alerts/authority` returns `[]`. The sidebar badge correctly renders nothing (count 0). This is truthful and matches database reality.

### 2.6 Hotspots Page
- **File:** `frontend/src/pages/public/Hotspots.tsx`
- **Contract:** Uses F3 `HotspotPrediction` records, `riskScore`, operational threshold `0.20`, risk-level mapping (`LOW`, `MODERATE`, `HIGH`, `CRITICAL`), and canonical resolution 8 H3 cells.
- **Finding:** Authoritative F3 model predictions are loaded via `useHotspots(cityId)`. Pune has active hotspots (e.g. `88608850e5fffff`, `88608852c1fffff`, `8860885357fffff`). Unavailable cells cleanly display empty or loading states without fabricating risk scores.

---

## 3. Backend Layer Audit

### 3.1 Citizen Report Controller & Service
- **Files:**
  - `backend/src/main/java/com/aerosentinel/citizen/CitizenReportController.java`
  - `backend/src/main/java/com/aerosentinel/citizen/CitizenReportService.java`
  - `backend/src/main/java/com/aerosentinel/citizen/CitizenVisionAiClient.java`
- **Watchdog Configuration:**
  - `CitizenVisionAiClient.java` line 71: `@Value("${app.citizen.vision.timeout-ms:15000}") long timeoutMs;`
  - `application.yml`: Key `app.citizen.vision.timeout-ms` is omitted, causing the default 15,000 ms to take effect.
- **Root Cause of Fallback on Real Gemini:**
  - When a citizen report with a high-resolution photo is submitted, the subprocess spawns `ai-service/ml/inference/vision_cli.py`.
  - Image loading, base64/buffer encoding, and the Google GenAI network call take between 12 and 16 seconds under normal internet conditions.
  - At 15,000 ms, `process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)` triggers `process.destroyForcibly()`, generating:
    `"Vision process timed out after 15000 ms"`.
  - The service persists a `GeminiAnalysis` with `model_name = 'deterministic-fallback'`, `detected_category = 'UNKNOWN'`, and `confidence = 0.50`, despite `GEMINI_API_KEY` being configured and functional.
- **Lineage & Event Attachment:**
  - `CitizenReportService.attachToEventIfMatching()` looks up `pollutionEventRepository.findTopByH3IndexOrderByStartedAtDesc(report.getH3Index())`.
  - If a matching event is found, it creates an `EventEvidence` record with `dataSource = "CITIZEN"` and `relevanceTier = "AUXILIARY"`.
  - If no matching event is found, the report remains persisted with `status = "ANALYZED"`, and no synthetic event is created (truthful).

### 3.2 Evidence Controller & Orchestration Service
- **Files:**
  - `backend/src/main/java/com/aerosentinel/evidence/EvidenceController.java`
  - `backend/src/main/java/com/aerosentinel/evidence/EvidenceOrchestrationService.java`
- **Root Cause of Evidence & WHY 10s Timeout:**
  - `EvidenceController.java` defines only:
    `GET /api/v1/evidence/hotspot/{h3Index}` calling `orchestrationService.getOrchestratedEvidence(h3Index);`
  - `getOrchestratedEvidence` unconditionally calls:
    `aiResult = evidenceAiClient.evaluate(cliPayload);`
  - This executes `orchestrate_evidence_cli.py`, which prompts Google Gemini for public and analyst narrative summaries on **every single GET request**.
  - A cold or warm Gemini API request takes 8–14 seconds.
  - Because Axios has a 10s global timeout, every view or refresh of `/analyst/evidence?h3=...` in the browser fails with:
    `"Failed to Retrieve Evidence Dossier: timeout of 10000ms exceeded"`.
- **Architectural Solution:**
  - Separate READ from WRITE/ORCHESTRATION:
    - READ (`GET /api/v1/evidence/hotspot/{h3Index}`): Checks if persisted dossier data exists in PostgreSQL (`PollutionEvent` + `GeminiAnalysis` + `EventEvidence`). If present, builds and returns `EvidenceSummaryResponse` immediately from the database without invoking Python/Gemini.
    - REFRESH/ORCHESTRATE (`POST /api/v1/evidence/orchestrate`): Explicitly triggers re-evaluation, runs `evidenceAiClient.evaluate(cliPayload)`, updates persisted records, and returns the fresh dossier.
  - Frontend: Add a dedicated per-request timeout (25,000 ms) in `evidenceApi.ts` for safety.

### 3.3 Forecast Controller, Service & Mapper
- **Files:**
  - `backend/src/main/java/com/aerosentinel/forecast/ForecastController.java`
  - `backend/src/main/java/com/aerosentinel/forecast/ForecastService.java`
  - `backend/src/main/java/com/aerosentinel/forecast/ForecastMapper.java`
- **Behavior:**
  - `GET /api/v1/forecast/{h3Index}` retrieves the latest 3 horizon rows (1h, 3h, 6h).
  - If rows exist, `ForecastMapper.computeFreshness()` calculates `LIVE` ($\le 2\text{h}$), `STALE` ($2\text{h} - 24\text{h}$), or `UNAVAILABLE` ($> 24\text{h}$).
  - If no rows exist, returns 404 with JSON `{ "status": "NO_DATA", "h3Index": "..." }`.
- **Finding:** The backend contract is strictly compliant. The frontend only requires truthful copy updates to avoid misleading users about "sensor recalibration".

### 3.4 Alert Controller & Service
- **Files:**
  - `backend/src/main/java/com/aerosentinel/alert/AlertController.java`
  - `backend/src/main/java/com/aerosentinel/alert/AlertService.java`
- **Behavior:**
  - `GET /api/v1/alerts/authority` queries `alerts` table where status matches filter.
  - Currently, `alerts` table count is 0.
  - Reason: The existing real H3 hotspot events have an F5 evidence score of 0.224 (`INSUFFICIENT_EVIDENCE`), which does not meet the F5 `ALERT_CANDIDATE` threshold ($> 0.55$).
  - Finding: 0 alerts is mathematically and operationally correct. No fake alerts should be created.

---

## 4. AI Service Layer Audit

### 4.1 Vision Service & Gemini Client
- **Files:**
  - `ai-service/app/services/vision_service.py`
  - `ai-service/app/services/gemini_client.py`
  - `ai-service/ml/inference/vision_cli.py`
- **Model Configuration:**
  - `GEMINI_MODEL=gemini-3.1-flash-lite` in `ai-service/.env`
  - `GEMINI_API_KEY` configured and valid.
  - `temperature=0.1`, `max_output_tokens=2048`, `max_retries=2`.
- **Watchdog Behavior:**
  - In `CitizenVisionAiClient.java`, the subprocess timeout was `15000ms`.
  - In `vision_cli.py`, if an exception or timeout occurs, it falls back to `status="FALLBACK"` with `modelVersion="deterministic-fallback"`.
- **Finding:** The AI code itself is completely functional and successfully executed real Gemini Vision for reports `CR-07B813E2`, `CR-6226166D`, and `CR-AF733F86`. The sole cause of fallback on `CR-39BAD39C` was the 15s watchdog in Spring Boot.

---

## 5. Database Layer Audit

### 5.1 Active Table Row Counts
Verified via `docker exec aerosentinel-postgres psql`:
| Table Name | Row Count | Operational Notes |
|:---|:---:|:---|
| `citizen_reports` | 10 | Real submitted citizen observations across Pune cells |
| `gemini_analyses` | 101 | Real Gemini and historical test analyses |
| `pollution_events` | 9 | Real canonical events created from F3 hotspots |
| `event_evidence` | 54 | Authoritative multi-source signals (including CITIZEN auxiliary signals) |
| `hotspot_predictions` | 11 | F3 model predictions across Pune, Mumbai, Delhi |
| `forecasts` | 15 | F4 multi-horizon predictions (5 cells $\times$ 3 horizons) |
| `alerts` | 0 | 0 alert candidates (no event currently meets the $>0.55$ F5 threshold) |

### 5.2 Lineage Integrity Check
- **Orphan Check:**
  - All `gemini_analyses` records reference valid `citizen_report_id` or `event_id`/`prediction_id`.
  - All `event_evidence` records reference valid `event_id` in `pollution_events`.
  - All `pollution_events` reference valid `grid_cells`.
  - All `forecasts` reference valid `h3_index` and `parent_prediction_id`.
- **Citizen to Event Lineage:**
  - Citizen report `07b813e2-a17d-455f-9761-744c0989a6ce` (`CR-07B813E2`) on H3 `88608850e5fffff` is linked to `PollutionEvent` `58ef2f64-4bc5-496f-9094-e5f61e44f8b0`.
  - Corresponding `EventEvidence` records exist with `data_source = 'CITIZEN'` and `relevance_tier = 'AUXILIARY'`.
  - Reports in cells without an active open pollution event remain in `status = 'ANALYZED'` without dangling references.

---

## 6. Audit Conclusion & Action Plan

1. **Part 2 — F6 Real Gemini Reliability:**
   - Configure `app.citizen.vision.timeout-ms: 30000` in `application.yml`.
   - Update frontend `citizen.service.ts` submit timeout to `35000ms`.
   - Verify real Gemini vision inference on stored citizen photo without triggering fallback.
2. **Part 3 — Citizen Report Semantics:**
   - Ensure UI strictly presents `REAL GEMINI` when `modelVersion` is a real Gemini model, `FALLBACK` when deterministic fallback executed, and `UNAVAILABLE` when the service failed.
3. **Part 4 — Evidence & WHY Timeout:**
   - Refactor `EvidenceOrchestrationService` so `GET /api/v1/evidence/hotspot/{h3Index}` reads persisted dossier data when available without calling Gemini on every GET request.
   - Add `POST /api/v1/evidence/orchestrate` for explicit refresh.
   - Add a 25s per-request timeout to `evidenceApi.ts`.
4. **Part 5 — Forecast Page Truthfulness:**
   - Remove misleading "scheduled recalibration" copy from `Forecast.tsx`.
   - Use truthful copy for `UNAVAILABLE`, `STALE`, and `NO_DATA`.
5. **Part 6 & 7 — Hotspot & Alerts Truthfulness:**
   - Confirm Hotspots display real F3 riskScores.
   - Confirm Alerts page displays 0 with clean empty state and sidebar badge is absent.
6. **Part 8, 9, 10, 11 — Lineage, Navigation & UI States:**
   - Verify cross-page `?h3=` navigation.
   - Confirm all pages handle `LOADING`, `SUCCESS`, `NO_DATA`, `STALE`, `UNAVAILABLE`, and `ERROR` cleanly.
7. **Part 12 & 13 — Tests, Build, and Verification:**
   - Run Vitest, backend tests, and build verification.

*Audit complete. Proceeding to Part 2 implementation.*
