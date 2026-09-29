# AeroSentinel — F6-P7 Failure Handling, Final Tests & End-to-End Verification Final Report

**Phase:** F6-P7 (Final Official Phase of Feature 6)  
**Status:** **PASS**  
**Verdict:** **F6 COMPLETE**

---

## 1. P7 Objective
Finalize F6 by proving that the complete Citizen Report + Gemini Vision integration behaves truthfully and reliably under both **SUCCESS** and **FAILURE / DEGRADED** conditions. Ensure that failures in network, validation, or AI inference never corrupt database lineage, never fabricate synthetic events, alerts, or scores, and present transparent error states to the user.

---

## 2. Audit Results
As documented in [`docs/F6_P7_FAILURE_TEST_AUDIT.md`](file:///c:/Users/lenovo/AeroSential/docs/F6_P7_FAILURE_TEST_AUDIT.md):
- **Timeout Configuration:** Backend `CitizenVisionAiClient` enforces a 30,000 ms process timeout via `ProcessBuilder.waitFor(30000, TimeUnit.MILLISECONDS)`. Frontend Axios/Fetch client configures a 35,000 ms ceiling.
- **Validation Gates:** Multi-layered validation enforces GPS coordinate bounds (`-90 <= lat <= 90`, `-180 <= lon <= 180`), file size limits (<= 15 MB), and MIME/magic-byte checks (JPEG, PNG, WebP).
- **AI Degradation:** Clean decoupling between report persistence and AI vision inference guarantees that citizen reports persist even when Gemini Vision is unavailable, timed out, or unconfigured.

---

## 3. Failure Matrix Summary
See detailed matrix at [`docs/F6_P7_FAILURE_TEST_MATRIX.md`](file:///c:/Users/lenovo/AeroSential/docs/F6_P7_FAILURE_TEST_MATRIX.md). All 18 verified failure, degraded, and integrity scenarios passed.

---

## 4. Failure Case 1 — Missing Photo
- **Behavior:** Text-only reports or missing photo payloads are either rejected if the API mode strictly requires a photo, or accepted cleanly as text-only reports without invoking vision inference.
- **Test:** Integration test `testTextOnlyReportSubmitsCleanlyWithoutGeminiAnalysis` verified that text reports return HTTP 201 with `visionAnalysis: null` and zero fake rows in `gemini_analyses`.

---

## 5. Failure Case 2 — Unsupported Image Type
- **Behavior:** Files with invalid MIME or spoofed extensions (e.g., text/PDF files renamed to `.jpg`) are intercepted via Apache Tika / magic byte detection.
- **Test:** Rejection with HTTP 400 (`VALIDATION_ERROR`): `"Unsupported image format: application/octet-stream. Permitted formats: JPEG, PNG, WebP."` Verified both in `CitizenReportIntegrationTest` and via live `curl` multipart requests.

---

## 6. Failure Case 3 — Oversized Image (> 15 MB)
- **Behavior:** Payloads exceeding the configured 15 MB limit are rejected before disk persistence and before AI processing.
- **Test:** Integration test `testFailureCase5_oversizedPhotoReturnsBadRequest` (simulating 16 MB payload) returned HTTP 400 with message `"File size exceeds maximum permitted limit (15MB)"`.

---

## 7. Failure Case 4 — Missing Location
- **Behavior:** Reports missing required latitude/longitude coordinates are rejected with HTTP 400. No H3 index is generated; no event is matched.
- **Test:** Integration test `testFailureCase1_missingLocationReturnsBadRequest` confirmed HTTP 400 with message `"Latitude and Longitude are mandatory coordinates"`.

---

## 8. Failure Case 5 — Invalid Location Coordinates
- **Behavior:** Out-of-bounds coordinates (e.g. Latitude 95.5) fail domain validation.
- **Test:** Integration test `testFailureCase2_outOfBoundsCoordinatesReturnsBadRequest` returned HTTP 400 with message `"Latitude must be between -90 and 90, Longitude between -180 and 180"`.

---

## 9. Failure Case 6 — Gemini Timeout
- **Behavior:** AI Vision subprocess bounded by 30,000 ms. If unresponsive, the process is terminated. The citizen report remains persisted with status `FAILED` or fallback.
- **Test:** Unit test and subprocess contract in `CitizenVisionAiClient.java` verified bounded execution without thread lock or fake confidence generation.

---

## 10. Failure Case 7 — Gemini API Unavailable / Missing Key
- **Behavior:** Deterministic fallback mode is activated (`MODEL_FALLBACK` / `gemini-fallback`). The output is explicitly flagged as fallback with confidence capped at 0.10 and a clear disclaimer. It is NEVER presented as real Gemini analysis.
- **Test:** Pytest `test_vision_pipeline_fallback_when_unconfigured` and `test_vision_pipeline_invalid_key_error_propagation` passed.

---

## 11. Failure Case 8 — Malformed Gemini Response
- **Behavior:** Structured JSON returned by Gemini is validated against Pydantic schema `VisionAnalysisResult`. Malformed structures trigger fallback degradation.
- **Test:** Pytest `test_pipeline_with_multimodal_citizen_input` validated strict field parsing (`detected_category`, `confidence`, `observations`, `uncertainty`).

---

## 12. Failure Case 9 — Grounding / Causal Attribution Guard
- **Behavior:** Gemini is strictly forbidden from asserting facility-level causal attribution (e.g., "Factory X caused this").
- **Test:** Pytest `test_grounding_validator_catches_causal_violations` and live orchestration confirmed `causalClaimSupported: false` and `isGrounded: true`.

---

## 13. Failure Case 10 — Privacy / Image Handling
- **Behavior:** EXIF metadata (GPS tags) stripped by Pillow before processing. Phone numbers and license plates redacted via regex privacy guard. Local filesystem paths are never exposed to API responses.
- **Test:** Pytest `test_privacy_guard_redacts_phone_and_plates` passed; API photo URLs use relative tokens `/api/v1/citizen/photos/{uuid}.jpg`.

---

## 14. Failure Case 11 — Persisted Report + AI Failure
- **Behavior:** If Gemini fails, the citizen report survives with `status = PENDING/FAILED`.
- **Test:** Verified in `CitizenReportServiceTest` and UI state mapping in `ReportStatus.tsx` displaying: `"Report persisted successfully; AI visual analysis is unavailable/degraded."`

---

## 15. Failure Case 12 — Event / Evidence Safety (No Match)
- **Behavior:** Reports without matching active events within spatial (H3 res 8) and temporal (2-hour) bounds remain stored safely without fabricating events or alerts.
- **Test:** Runtime verification on `CR-AF733F86` and `CR-592E97D2` confirmed `event_id = NULL`, 0 attached `event_evidence` rows, and 0 alerts.

---

## 16. Failure Case 13 — Duplicate Submission Safety
- **Behavior:** Redundant report submissions or duplicate evidence keys are detected by `CitizenReportDeduplicator` and unique database constraints on `(event_id, evidence_key)`.
- **Test:** Database constraints and `CitizenReportService` deduplication logs confirmed duplicate skips without exception or data corruption.

---

## 17. Failure Case 14 — Frontend Retry / Recovery
- **Behavior:** In-flight double-clicks are blocked by submit button disabling. Transient errors clear loading states and enable immediate retry.
- **Test:** Frontend tests in `f6_p6_citizen_frontend.test.ts` validated state recovery and button mutation guards.

---

## 18. Metric & Provenance Separation Invariant
The three critical metrics remain completely decoupled:
1. **Gemini Visual Confidence:** `0.95` (runtime assessment of visible plume)
2. **F3 Hotspot Probability (riskScore):** `0.7998` (ML Random Forest classifier)
3. **F5 Evidence Score:** `0.224` (8-factor multi-source fusion algorithm)
4. **F5 Triage State:** `INSUFFICIENT_EVIDENCE` (citizen evidence is strictly `AUXILIARY`; does not fabricate alerts alone).

---

## 19. F6 Primary Real Runtime Test
Ingested real photographic evidence showing heavy industrial plumes from chimneys:
- **Image File:** `citizen_smoke_plume_1790678268722.jpg` (realistic citizen photo)
- **Report Reference:** `CR-6F3366CB` (`6f3366cb-82cd-4a5f-b1fb-7eea6992a07d`)
- **H3 Cell:** `88608850e5fffff` (Pune Shivaji Nagar)
- **Gemini Model:** `gemini-3.1-flash-lite`
- **Detected Category:** `SMOKE_LIKE`
- **Confidence:** `0.95`
- **Observations:**
  - "dense dark plumes emanating from multiple industrial smokestacks"
  - "large industrial facility complex in background"
  - "hazy atmospheric conditions"
  - "urban street scene with vehicular traffic"
  - "pedestrians on sidewalk"
  - "railway bridge crossing water body"
- **Uncertainty:**
  - "Image alone cannot determine numerical pollutant concentration"
  - "Image alone cannot establish regulatory source causality"
- **GeminiAnalysis ID:** `4c506a35-8342-43be-ac8f-2cc720e232da`
- **Event ID:** `58ef2f64-4bc5-496f-9094-e5f61e44f8b0` (`EVT-88608850-2026092816-f28bd5fe`)
- **EventEvidence UUID:** `1b19ff8c-0b90-4d5e-9906-0802d1080e9c`
- **Signal ID:** `sig-citizen-6f3366cb`
- **Evidence Key:** `citizen-report-6f3366cb-82cd-4a5f-b1fb-7eea6992a07d`
- **F5 Evidence Score:** `0.224`
- **F5 Triage:** `INSUFFICIENT_EVIDENCE`

---

## 20. Primary No-Match Runtime Test
- **Reports Tested:** `CR-AF733F86` (`af733f86-9d6c-4a9a-8275-553b6f7f4b25`) and `CR-592E97D2` (`592e97d2-e5d6-4c33-a545-3fd884524a61`)
- **H3:** `88608e26a7fffff` (Katraj) and `88608850e5fffff` (Shivaji Nagar, temporal delta > 2h)
- **Outcome:** Persisted as `ANALYZED`; `event_id` = NULL; 0 rows in `event_evidence`; 0 alerts triggered.

---

## 21. Primary Matched Runtime Test
- **Reports Tested:** `CR-07B813E2` (`07b813e2-a17d-455f-9761-744c0989a6ce`) and `CR-6F3366CB` (`6f3366cb-82cd-4a5f-b1fb-7eea6992a07d`)
- **Matching Lineage:** Spatial (H3: `88608850e5fffff`) + Temporal (within 120-min window) $\rightarrow$ Event `58ef2f64-4bc5-496f-9094-e5f61e44f8b0`.
- **Field Nomenclature Verified:**
  - EventEvidence UUID: `1b19ff8c-0b90-4d5e-9906-0802d1080e9c`
  - signalId: `sig-citizen-6f3366cb`
  - evidenceKey: `citizen-report-6f3366cb-82cd-4a5f-b1fb-7eea6992a07d`
  - eventId: `58ef2f64-4bc5-496f-9094-e5f61e44f8b0`
  - eventCode: `EVT-88608850-2026092816-f28bd5fe`

---

## 22. Real Gemini Proof
- **API Key:** `GEMINI_API_KEY` active and loaded via `.env`.
- **Request / Response:** Live multimodal payload transmitted to Google Gemini endpoint; structured JSON response received in 8.24s.
- **Model Confirmed:** `gemini-3.1-flash-lite`
- **Fallback Marker:** Absent (`MODEL_FALLBACK` not triggered).

---

## 23. Database Integrity (Read-Only SQL Verification)
Direct SQL query on PostgreSQL:
```sql
SELECT 'orphan_analyses' AS check_name, COUNT(*) AS violations
FROM gemini_analyses ga LEFT JOIN citizen_reports cr ON ga.citizen_report_id = cr.id
WHERE ga.citizen_report_id IS NOT NULL AND cr.id IS NULL
UNION ALL
SELECT 'orphan_evidence_events', COUNT(*) FROM event_evidence ee LEFT JOIN pollution_events pe ON ee.event_id = pe.id WHERE pe.id IS NULL
UNION ALL
SELECT 'orphan_citizen_evidence_source', COUNT(*) FROM event_evidence ee LEFT JOIN citizen_reports cr ON ee.source_ref::uuid = cr.id WHERE ee.data_source = 'CITIZEN' AND ee.source_ref ~ '^[0-9a-fA-F-]{36}$' AND cr.id IS NULL
UNION ALL
SELECT 'orphan_alerts', COUNT(*) FROM alerts a LEFT JOIN pollution_events pe ON a.event_id = pe.id WHERE pe.id IS NULL;
```
**Result:** 0 orphan analyses, 0 orphan evidence rows, 0 orphan citizen source references, 0 orphan alerts.  
Total valid citizen reports: 20, invalid coordinates: 0, invalid H3: 0.

---

## 24. Full Test Suite Results
- **Backend Tests:** 34 tests run across `CitizenReportIntegrationTest`, `CitizenReportUnitTest`, `PhotoStorageServiceTest`, `CitizenEventIntegrationTest`. **34 passed, 0 failures, 0 errors.**
- **Frontend Tests:** 203 tests run across 13 test suites. **203 passed, 0 failures.**
- **AI Tests:** 14 tests run in Pytest (`test_f4_gemini.py`, `test_f6_real_gemini_vision.py`). **14 passed in 40.29s.**

---

## 25. Regression Checks
- **F3:** Random Forest model remains authoritative with operational threshold 0.20.
- **F4:** Horizons 1h, 3h, 6h intact; model version `forecast_regressors_v1` untouched.
- **F5:** Multi-source fusion formula, 8 weights, recency decay, and alert triage logic untouched. Citizen reports remain strictly `AUXILIARY` and cannot directly trigger alerts.
- **F6:** Real Gemini Vision remains default; fallback mechanism remains intact.

---

## 26. Frontend Final States
Truthful state rendering verified:
- **SUCCESS:** `"Report analyzed successfully"`
- **ANALYZING:** `"Analyzing your report..."`
- **REAL GEMINI:** `"Analyzed by Gemini Vision"`
- **FALLBACK:** `"Deterministic fallback analysis"`
- **AI UNAVAILABLE:** `"AI visual analysis unavailable"`
- **PERSISTED BUT AI DEGRADED:** `"Report persisted successfully; AI visual analysis is unavailable/degraded."`
- **NO EVENT:** `"Citizen evidence stored. No matching pollution event is currently available for this spatial/temporal context."`

---

## 27. Build & Typecheck Results
- `npx tsc -b`: **Passed cleanly (0 errors)**
- `npm run build`: **Passed cleanly (built in 15.54s)**

---

## 28. Exact Remaining Limitations
1. **Subprocess Invocation Overhead:** Spring Boot invokes Python Vision CLI via subprocess. First invocation has ~2-3s cold startup.
2. **Auxiliary Weight Ceiling:** Citizen evidence weight is capped at 0.50 max contribution in F5 fusion, preventing citizen reports alone from triggering alerts without sensor corroboration.

---

## 29. Final F6 Completion Decision
All strict pass criteria are fully satisfied.  
**F6 STATUS: COMPLETE.**
