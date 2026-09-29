# F6-P7 FAILURE HANDLING & FINAL TEST AUDIT
**AeroSentinel Environmental Intelligence Platform**
**Phase:** F6-P7 — Failure Handling + Final Tests + End-to-End Verification
**Date:** 2026-09-29
**Author:** Antigravity AI Agent

---

## 1. Executive Summary

This audit assesses the failure handling, resilience mechanisms, boundary guards, and test coverage across the AeroSentinel platform for the Citizen Report and Gemini Vision features. 

The audit confirms that the backend and AI subsystems have extensive resilience mechanisms already embedded:
- **Server-Side Validation**: Magic-byte inspection, strict MIME whitelisting, 15MB file size limit, boundary coordinate validation, category normalization, and description truncation checks.
- **Fail-Safe Persistence**: The citizen report entity is persisted in PostgreSQL **before** calling the vision analysis subprocess. Any failure in Gemini Vision (network timeout, API failure, malformed JSON, or uncaught exception) is safely caught, preserving the citizen report while assigning a degraded or fallback state (`"UNAVAILABLE"` / `"FALLBACK"`).
- **Metric & Invariant Separation**: The F3 hotspot risk score ($0.7998$), Gemini visual confidence ($0.10$), and F5 evidence score ($0.224$) are stored in completely separate data structures and never conflated.
- **Strict Anti-Fabrication Invariant**: When no matching `PollutionEvent` exists in an H3 cell, the report remains stored as auxiliary evidence (`STORED AUXILIARY`). Zero synthetic events, fake event evidence, or synthetic alerts are created.

The audit identifies minor gaps and test requirements that F6-P7 must formally verify and document with automated test coverage and runtime proof.

---

## 2. Current Architecture & Failure Handling Audit

### 2.1 Backend Architecture & Error Handling
1. **`CitizenReportController.java`**:
   - Accepts multipart submissions at `POST /api/v1/citizen/reports`.
   - Requires `cityId`, `latitude`, `longitude`, `category`.
   - `description`, `photo`, and `observedAt` are optional on the controller layer, but `PhotoStorageService` is called whenever a photo is uploaded.
   - Throws `ValidationException` (mapped to HTTP 400 Bad Request via `GlobalExceptionHandler`).
   - Photo endpoint `GET /api/v1/citizen/photos/{storageKey}` validates key regex `^[a-zA-Z0-9_-]+\.(jpg|jpeg|png|webp)$` and probes MIME type safely.
2. **`PhotoStorageService.java`**:
   - `MAX_FILE_SIZE_BYTES`: $15 \times 1024 \times 1024$ (15 MB). Rejects larger files with `ValidationException`.
   - `ALLOWED_MIME_TYPES`: `image/jpeg`, `image/jpg`, `image/png`, `image/webp`. Rejects other MIME types.
   - `detectAndValidateMagicBytes(file)`: Validates real file header bytes:
     - JPEG: `FF D8 FF`
     - PNG: `89 50 4E 47`
     - WebP: `RIFF....WEBP`
   - Path traversal guard: Ensures resolved target path starts with configured `uploadDir`.
   - Randomizes storage filename (`UUID.randomUUID().toString() + "." + extension`), never trusting client-supplied filenames.
3. **`CitizenReportService.java`**:
   - **Location Validation**: Rejects `null` coordinates with `ValidationException("Geographic coordinates (latitude and longitude) are mandatory")`. Rejects out-of-bounds latitude ($<-90$ or $>90$) and longitude ($<-180$ or $>180$) via `H3Utils.validateCoordinates`.
   - **Spatial Indexing**: Derives Uber H3 Resolution 8 cell index ($\approx 460$m radius) from valid coordinates.
   - **Category Normalization**: Validates against allowed categories (`SMOKE`, `DUST`, `BURNING`, `ODOR`, `OTHER`), normalizing case and aliases (e.g., `"odour"` $\to$ `"ODOR"`).
   - **Description Validation**: Limits text length to 1000 characters.
   - **Persistence Sequencing**: Persists `CitizenReport` in database with status `"PENDING"` **before** invoking Gemini Vision.
   - **Subprocess Error Isolation**: Invocation of `visionAiClient.analyzeImage()` is wrapped in a `try-catch (Exception e)`. If an exception occurs, the report is **NOT rolled back**; instead, it is updated to status `"ANALYZED"` or kept `"PENDING"`, and `visionSummary` is populated with `analysisStatus: "UNAVAILABLE"`.
   - **Event Matching & Deduplication**: Queries `PollutionEventRepository.findTopByH3IndexOrderByStartedAtDesc(h3Index)`. If present, validates spatio-temporal match ($\le 120$ minutes). If matched, checks `evidenceRepository.existsByEventIdAndEvidenceKey()` before creating `EventEvidence` with `dataSource: "CITIZEN"` and `relevanceTier: "AUXILIARY"`. If no event is found, logs unattached storage and creates **zero fabricated events**.
4. **`CitizenVisionAiClient.java`**:
   - Bridges Spring Boot to `ai-service/ml/inference/vision_cli.py` via `ProcessBuilder`.
   - Timeout: Configured via `app.citizen.vision.timeout-ms` with a default of **30,000 ms** (30s) to allow cold-start and external Gemini API latency.
   - If the subprocess exceeds 30s, calls `process.destroyForcibly()` and returns `CitizenVisionResultDto.createFallback(..., "Vision process timed out after 30000 ms")`.
   - If the subprocess exits non-zero or emits malformed stdout, returns `CitizenVisionResultDto.createFallback()`.
   - **Never throws an unhandled fatal exception** to the calling service.

### 2.2 AI Subsystem & Guard Rails
1. **`vision_service.py`**:
   - Converts input image to standard RGB JPEG bytes in-memory.
   - If `gemini_client.is_configured()` (API key present), invokes Gemini Vision using system instruction `VISION_SYSTEM_INSTRUCTION` and user prompt `VISION_USER_PROMPT`.
   - If API key is missing or unconfigured, produces a deterministic fallback `CitizenVisionAnalysis` with explicit limitation string `"API key unconfigured: deterministic fallback analysis"`.
2. **`vision_cli.py`**:
   - Reads JSON payload from STDIN, invokes `analyze_citizen_image()`.
   - Sanitizes text and feature outputs through `PrivacyGuard.sanitize_vision_analysis()`.
   - Maps raw `VisualIndicators` deterministically to F6 categories (`SMOKE_LIKE`, `DUST_LIKE`, `BURNING_LIKE`, `UNKNOWN`).
   - Inspects limitations: if `"deterministic fallback"` is present, reports `modelVersion: "deterministic-fallback"`.
   - When real Gemini succeeds without fallback, reports actual model name (`gemini-3.1-flash-lite`).
   - If unhandled Python exception occurs, emits fallback JSON to STDOUT with `status: "FALLBACK"`.
3. **`grounding_guard.py` (`GroundingValidator`)**:
   - Enforces prohibition of causal attribution phrases: `"caused the pollution"`, `"caused this event"`, `"factory caused"`, `"plant caused"`, `"responsible for causing"`, `"proves the facility is at fault"`, `"proves violation"`, `"guilty of violation"`.
   - Flags violation if `causal_claim_supported` is `True`.
   - Validates that numerical claims align with deterministic sensor and model facts.
4. **`privacy_guard.py` (`PrivacyGuard`)**:
   - Regex-based redaction of vehicle registration plates (`[REDACTED_VEHICLE_PLATE]`), Indian phone numbers (`[REDACTED_PHONE_NUMBER]`), and email addresses (`[REDACTED_EMAIL]`).

### 2.3 Frontend Resilience & States
1. **`CitizenReport.tsx`**:
   - Geolocation fallback: If browser GPS fails or is denied, falls back to city center coordinates.
   - Submit guard: `isSubmitting` state disables button and displays `"ANALYZING & SUBMITTING..."`.
   - Per-request timeout: Configured at 35,000 ms in `citizen.service.ts` to accommodate 30s backend vision processing without modifying global Axios timeout.
   - Dual-view rendering: Switches to submitted evidence dossier upon receipt of persisted report entity.
   - Degraded AI banner: If `visionAnalysis.analysisStatus === 'UNAVAILABLE'`, displays:
     > *"Report Persisted Successfully: Vision analysis is currently degraded or queued. Your report has been securely registered in H3 cell ... and will be evaluated as auxiliary evidence."*
   - Never states *"Report submission failed"* when the backend has in fact persisted the report.
2. **`GeminiVisionCard.tsx`**:
   - Strictly derives provider classification from backend metadata:
     - `REAL GEMINI VISION` (purple badge) when `modelVersion.startsWith('gemini')`.
     - `DETERMINISTIC FALLBACK` (amber badge) when `modelVersion.includes('fallback')` or status is `FALLBACK`.
     - `AI UNAVAILABLE` (neutral badge) when analysis is null or status is `UNAVAILABLE`.
   - Displays observation features, uncertainty limitations, and mandatory Semantic Boundary Notice:
     > *"AI visual interpretation ≠ numeric pollution measurement ≠ causal source attribution."*
3. **`CitizenEvidenceLineageCard.tsx`**:
   - When matched event exists: Displays 5-step lineage, real event code/ID, real signal ID, F5 score, triage, and dossier link.
   - When no matching event exists: Displays truthful stored auxiliary panel:
     > *"Citizen evidence stored. No matching pollution event is currently available for this spatial/temporal context."*
     > *"Locked Safety Invariant: Citizen observations alone never fabricate a synthetic event or elevate an alert candidate state."*

---

## 3. Exact Current Timeout Configuration

| Component | Setting / Location | Value | Purpose |
|---|---|---|---|
| Frontend Request | `citizen.service.ts` | **35,000 ms** (35s) | Per-request Axios timeout allowing full multimodal round-trip. |
| Backend Vision Client | `app.citizen.vision.timeout-ms` in `application.yml` | **30,000 ms** (30s) | ProcessBuilder `waitFor` timeout for Python subprocess. |
| Evidence Read API | `evidenceApi.ts` (`getEvidence`) | **25,000 ms** (25s) | Fast read timeout for persisted hotspot evidence. |
| Evidence Orchestrate API | `evidenceApi.ts` (`orchestrateEvidence`) | **30,000 ms** (30s) | Dedicated timeout for explicit AI re-orchestration. |
| F4 Forecast Timeout | `application.yml` | Unchanged (3000 ms) | Preserved F4 inference timeout; untouched in F6. |

---

## 4. Current Test Inventory

1. **Frontend**:
   - `src/utils/f6_p6_citizen_frontend.test.ts` (25 tests)
   - `src/utils/f6_p4_citizen_ux.test.ts` (20 tests)
   - `src/utils/f6_p3_citizen_evidence.test.ts` (15 tests)
   - `src/utils/f6_pre_p4_ux_cleanup.test.ts` (8 tests)
   - `src/utils/citizen_reporting.test.ts` (12 tests)
   - `src/utils/evidence.test.ts` (12 tests)
   - `src/utils/f5_p7_failure_recovery.test.ts` (18 tests)
   - `src/utils/f5_ui_polish.test.ts` (10 tests)
   - `src/utils/alerts.test.ts` (10 tests)
   - `src/utils/inspections.test.ts` (12 tests)
   - `src/utils/forecast.test.ts` (25 tests)
   - `src/utils/hotspot.test.ts` (20 tests)
   - `src/utils/freshness.test.ts` (16 tests)
   - **Total Frontend Tests**: 203 passing tests.
2. **Backend**:
   - `PhotoStorageServiceTest.java` (7 unit tests for file type, magic bytes, size limits, traversal)
   - `CitizenReportUnitTest.java` (6 unit tests for coordinates, Gemini timeout/failure, category normalization)
   - `CitizenReportIntegrationTest.java` (End-to-end multipart submission, DB persistence, alert count verification)
   - `CitizenEventIntegrationTest.java` (11 tests for H3 matching, temporal window, deduplication, metric separation)
3. **AI Subsystem**:
   - `test_f6_real_gemini_vision.py` (Real Gemini Vision verification against live Google API)
   - `tests/ai/test_f4_gemini.py` (Grounding, non-causality, prompt schema adherence)
   - `tests/ai/test_f5_alert_support.py` (Evidence scoring, multi-source agreement)

---

## 5. Identified Gaps to Close in F6-P7

| Gap ID | Scenario | Current State | Required P7 Action |
|---|---|---|---|
| **GAP-1** | Missing Photo Submission | Form allows submitting without photo (produces a text-only report with `visionAnalysis: null`). If backend is called via programmatic contract with invalid multipart or empty file, behavior must be verified. | Add dedicated test proving that submitting without photo or with empty photo is cleanly handled without crashing, producing either validation error or text-only report with no Gemini analysis and no synthetic vision. |
| **GAP-2** | Unsupported MIME / Magic Byte Failure | Tested in `PhotoStorageServiceTest.java`, but needs integration test coverage at controller level (`POST /api/v1/citizen/reports` with PDF/TXT) proving HTTP 400 Bad Request. | Implement comprehensive integration test for unsupported file types. |
| **GAP-3** | Oversized Image (>15MB) | Tested in unit tests, needs controller-level verification returning HTTP 400/413. | Verify server-side rejection of >15MB payload. |
| **GAP-4** | Bounded Gemini Timeout Simulation | Verified via mock in `CitizenReportUnitTest`, but needs end-to-end failure matrix documentation. | Document exact timeout behavior in matrix and test suite. |
| **GAP-5** | Real Environmental Image Verification | Previous verification used a uniform grey test image resulting in low confidence (0.10). | Perform real runtime submission with an actual environmental photo depicting visible emissions to prove high confidence, structured Gemini observations, and live H3 binding. |
| **GAP-6** | Database Read-Only Integrity Check | Need explicit SQL verification of referential integrity across `citizen_reports`, `gemini_analyses`, `pollution_events`, `event_evidence`, and `alerts`. | Execute non-destructive SQL queries proving zero orphaned or synthetic records. |

---

## 6. Audit Conclusion & Next Steps

The repository is structurally sound and satisfies all architectural invariants. We can proceed directly to execute:
1. Create `docs/F6_P7_FAILURE_TEST_MATRIX.md`.
2. Add backend and AI test coverage for failure edge cases.
3. Perform the live runtime test with a real environmental image.
4. Execute database integrity checks.
5. Run all test suites, TypeScript compilation, and production build.
6. Generate `docs/F6_P7_FAILURE_TEST_FINAL_REPORT.md` and provide the final evaluation verdict.
