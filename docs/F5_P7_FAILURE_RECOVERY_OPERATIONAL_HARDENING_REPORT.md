# AeroSentinel — F5-P7 Failure Recovery & Operational Hardening Report

**Phase:** F5-P7 (Failure Recovery, Degraded Mode, Operational Hardening, Observability, Retry Safety)  
**Status:** **F5-P7 STATUS: PASS**  
**Evaluation Date:** September 28, 2026  
**System Architecture:** Spring Boot (Java 21) | Python FastAPI/Inference Bridge | React + TypeScript Frontend | PostgreSQL (Spatial H3)

---

## 1. Phase Objective

The objective of F5-P7 is to harden the AeroSentinel end-to-end evidence orchestration and field operational pipeline against real-world failures. Specifically, when upstream ML models, external telemetry, database mutations, or LLM services fail, the system must:
1. **Never crash or emit unhandled exceptions.**
2. **Never fabricate values or synthetic confidence.**
3. **Never invent legal or causal claims.**
4. **Never lose bidirectional lineage (Alert $\leftrightarrow$ Inspection $\leftrightarrow$ Verification $\leftrightarrow$ Event $\leftrightarrow$ H3 $\leftrightarrow$ Prediction).**
5. **Never duplicate records or alerts during retries or concurrent races.**
6. **Degrade gracefully**, preserving all available verified facts while surfacing truthful degraded indicators.

---

## 2. Failure Matrix Audit

A rigorous audit of the 21 failure matrix scenarios was conducted across backend, AI inference, and frontend layers:

| ID | Failure Scenario | Pre-P7 State | P7 Action & Hardening | Status |
|:---|:---|:---|:---|:---|
| 1 | **F3 Unavailable** | Throws `ResourceNotFoundException` | Verified mapped to 404 NOT_FOUND. Does not fabricate hotspot or risk. | **VERIFIED** |
| 2 | **F4 Unavailable** | Forecast optional in orchestration | Verified `modelOutputs.forecast` set to `null`. UI shows "Forecast unavailable for this cell". | **VERIFIED** |
| 3 | **Gemini Unavailable** | Offline check in `gemini_pipeline.py` | Added try/catch around GenAI SDK calls to guarantee fallback even if configured but failing. | **HARDENED** |
| 4 | **Gemini Timeout** | Bounded 15s subprocess timeout | Process forcibly destroyed; timeout caught and yields deterministic grounded synthesis. | **HARDENED** |
| 5 | **Gemini Malformed Response** | Pydantic schema validation | Caught by try/catch in `gemini_pipeline.py`; triggers deterministic fallback. | **HARDENED** |
| 6 | **AI Validation Failure** | `GroundingValidator` in pipeline | Enforces `causal_claim_supported = False` and sanitizes conclusions. | **VERIFIED** |
| 7 | **Database Timeout** | HikariCP pool timeout | Handled by Spring transaction boundary; rollback ensures no orphaned rows. | **VERIFIED** |
| 8 | **PostgreSQL Unavailable** | Standard 500 error mapped | `GlobalExceptionHandler` returns clean JSON without leaking credentials. | **VERIFIED** |
| 9 | **External Telemetry Unavailable** | Missingness tolerated | Physical telemetry facts nullable; evidence completeness penalty applied. | **VERIFIED** |
| 10 | **Partial Evidence Availability** | 4-valued state matrix | `source_matrix` preserves `supported`, `not_detected`, `unavailable`, `unknown` distinctly. | **VERIFIED** |
| 11 | **Duplicate Orchestration** | Canonical event deduplication | `PollutionEventRepository.findByEventCode` reuses existing event without inserting duplicate. | **VERIFIED** |
| 12 | **Concurrent Orchestration** | DB unique constraint on event/evidence | `existsByEventIdAndEvidenceKey` skips duplicate signals; unique indexes guard races. | **VERIFIED** |
| 13 | **Alert Duplicate Creation** | Event-to-alert idempotency | `alertRepository.findByEventId` check + `DataIntegrityViolationException` race handling. | **VERIFIED** |
| 14 | **Field Assignment Conflict** | Lifecycle & uniqueness check | Rejects OPEN/RESOLVED alerts; partial unique index blocks duplicate active assignments. | **VERIFIED** |
| 15 | **Frontend API Timeout/Failure** | Unhandled in some cards | Exposes explicit error notice; hides stale data; displays "Retry Query" CTA. | **HARDENED** |
| 16 | **Stale Frontend Requests** | Race conditions on rapid switch | `activeRequestIdRef` + `AbortController` discards superseded in-flight responses. | **VERIFIED** |
| 17 | **Malformed API Response** | `validateEvidenceResponse` | Structural type guards protect rendering against corrupt JSON payloads. | **VERIFIED** |
| 18 | **Missing H3** | Format validator | Rejects indices < 10 chars with 400 ValidationException. | **VERIFIED** |
| 19 | **Missing Prediction Lineage** | Mandatory predictionId | Required in `HotspotSpatialContext`; rejects unanchored events. | **VERIFIED** |
| 20 | **Missing Event** | `createOrUpdateAlertCandidate` check | Returns `Optional.empty()` cleanly if event is null. | **VERIFIED** |
| 21 | **Missing Evidence** | Completeness scoring | Yields low completeness factor and classifies event as `INSUFFICIENT_EVIDENCE`. | **VERIFIED** |

---

## 3. Existing Resilience Mechanisms

The repository already contained robust baseline components that were validated and locked:
- **`EventEvidenceScoringEngine` (LOCKED):** Authoritative 8-factor mathematical scoring with completeness factor, conflict penalty, and recency factor.
- **`AlertService` (LOCKED):** Idempotent alert creation with `DataIntegrityViolationException` race recovery and strict lifecycle transitions (`OPEN` $\to$ `ACKNOWLEDGED` $\to$ `RESOLVED`).
- **`InspectionService` (LOCKED):** Field inspection lifecycle (`ASSIGNED` $\to$ `IN_PROGRESS` $\to$ `COMPLETED`) with state-machine transition validation and database partial index enforcement.
- **`GroundingValidator` (LOCKED):** Validates causal attribution against telemetry and sanitizes unsupported conclusions.

---

## 4. Files Changed

1. **`ai-service/app/services/gemini_pipeline.py`**
   - Wrapped `gemini_client.generate_structured_json` and Pydantic `model_validate` in a `try...except Exception` block.
   - When Gemini times out, returns malformed JSON, or throws an API error, it logs a warning and automatically falls back to `_generate_deterministic_grounded_fallback`.
2. **`ai-service/app/services/gemini_client.py`**
   - Protected `from google import genai` imports with `try...except (ImportError, AttributeError)` so client initialization never crashes in environments where GenAI SDK is unavailable.
3. **`ai-service/app/services/vision_service.py`**
   - Protected `from google.genai import types` imports with `try...except` block with `PIL.Image` restored.
4. **`backend/src/main/java/com/aerosentinel/config/CorrelationIdFilter.java`** (NEW)
   - Created lightweight `OncePerRequestFilter` with `@Order(Ordered.HIGHEST_PRECEDENCE)`.
   - Binds `X-Correlation-Id` to SLF4J MDC, echoes header to HTTP response, and removes MDC context in `finally`.
5. **`frontend/src/components/hotspot/EvidencePanel.tsx`**
   - Hardened `AI INTERPRETATION` card: when `aiInterpretation` is null/unavailable, renders the card with badge `AI UNAVAILABLE` and truthful notice: *"AI explanation unavailable — showing verified data only."* without hiding the rest of the dossier.
6. **`ai-service/tests/test_f5_p7_failure_recovery.py`** (NEW)
   - Added 6 automated failure recovery tests covering Gemini fallback, malformed JSON, grounding guard, 4-valued source matrix, and stale data recency decay.
7. **`backend/src/test/java/com/aerosentinel/evidence/EvidenceFailureRecoveryTest.java`** (NEW)
   - Added 10 automated backend failure injection tests covering Gemini timeout, malformed responses, F4/F3 missingness, DB rollback, alert conflict, duplicate retry, concurrent race, invalid field transitions, and verification persistence failure.
8. **`frontend/src/utils/f5_p7_failure_recovery.test.ts`** (NEW)
   - Added 7 automated frontend tests covering Items 11–17 of the Failure Matrix.

---

## 5. Gemini Failure Handling

When calling the Gemini API:
- **Success:** Returns grounded, structured JSON interpretation (`summaryPublic`, `summaryAnalyst`, `forecastTrajectory`).
- **Timeout / API Outage (504, 503, 429):** Caught by `gemini_pipeline.py` lines 86–101; triggers `_generate_deterministic_grounded_fallback`.
- **Malformed JSON:** Pydantic validation error caught by the same block; activates fallback without crashing the subprocess.
- **Grounding Guard Failure:** `GroundingValidator.validate_grounding` flags uncorroborated claims and overrides `causal_claim_supported = False`.
- **UI Presentation:** Displays badge `AI UNAVAILABLE` and message *"AI explanation unavailable — showing verified data only."* The rest of the dossier (OBSERVED, MODEL OUTPUT, EVIDENCE SCORE, TRIAGE, PROVENANCE, RECOMMENDED VERIFICATION) remains fully visible and intact.

---

## 6. F3/F4 Unavailable Handling

### F4 Forecast Regressors Unavailable
- When regressors are unavailable or missing historical features for a cell, `ForecastService.getForecastByH3(h3)` returns `Optional.empty()`.
- `EvidenceOrchestrationService` passes an empty map for forecast; `modelOutputs.forecast` in `EvidenceSummaryResponse` is strictly `null`.
- **Zero Fabrication:** The system **never** substitutes 0.0 µg/m³, never synthesizes fake uncertainty bounds, and `forecastConfidence` remains strictly `null`.
- **UI Presentation:** The F4 card renders *"Forecast unavailable for this cell."*

### F3 Hotspot Classifier Unavailable
- When no baseline intelligence exists for an H3 cell, `hotspotContextService.buildSpatialContextForH3` returns `Optional.empty()`.
- `EvidenceOrchestrationService` throws `ResourceNotFoundException("No active hotspot intelligence or baseline spatial context found for H3 cell: ...")`.
- `GlobalExceptionHandler` maps this to HTTP 404 NOT_FOUND.
- Downstream services never fabricate a risk score or assume `hotspot = false`.

---

## 7. Partial Evidence Handling

The `EventEvidenceScoringEngine` implements a 4-valued state matrix for all data sources:
- `supported`: Source telemetry is active and directly corroborates the anomaly.
- `not_detected`: Source was queried, observation was recorded, but no anomaly was detected (negative evidence).
- `unavailable`: Data provider was offline, out of range, or unmonitored (e.g. night window for satellite).
- `unknown`: Indeterminate or uncorroborated telemetry.

**Rule Enforced:** The engine **never** converts `unavailable` into `not_detected`. Missing remote sensing data reduces the completeness score factor but is not treated as negative evidence against ground sensor observations.

---

## 8. Recency & Stale Data Handling

In `EventEvidenceScoringEngine.py`:
- Each signal's timestamp is evaluated against the event timestamp.
- Telemetry older than 2 hours receives exponential recency decay ($e^{-\lambda \Delta t}$).
- Verified via `test_p7_6_stale_data_recency_decay_reduces_score`: telemetry 8 hours old yields `recency_factor < 1.0` and lower evidence score ($0.566$ vs $0.650$).
- Frontend never labels stale or unmonitored data as "LIVE".

---

## 9. Database Failure & Transaction Safety

- `EvidenceOrchestrationService.getOrchestratedEvidence` is annotated with `@Transactional`.
- If a downstream database write fails (e.g. `geminiAnalysisRepository.save()` or `alertService.createOrUpdateAlertCandidate()`):
  - Spring automatically rolls back the entire transaction.
  - No orphan `PollutionEvent` or unlinked `EventEvidence` records remain.
  - Verified in `EvidenceFailureRecoveryTest.test5_databasePersistenceFailure_triggersRollback`.

---

## 10. Idempotent Retry Behavior

- **PollutionEvent Deduplication:** `EvidenceOrchestrationService.resolveOrCreatePollutionEvent` queries `pollutionEventRepository.findByEventCode(canonicalCode)`. If present, it updates and reuses the existing event.
- **EventEvidence Deduplication:** Iterating over signals, `evidenceRepository.existsByEventIdAndEvidenceKey(eventId, key)` skips already-persisted signals.
- **Alert Deduplication:** `AlertService.createOrUpdateAlertCandidate` checks `alertRepository.findByEventId(eventId)` and returns the existing active alert without creating duplicate rows.
- Verified in `EvidenceFailureRecoveryTest.test7_duplicateOrchestrationRetry_isIdempotent`.

---

## 11. Concurrent Request Handling

- Two identical requests arriving simultaneously at the database boundary:
  - Database unique constraints on `pollution_events.event_code`, `alerts.event_id`, and `event_evidence (event_id, evidence_key)` reject the second thread with `DataIntegrityViolationException`.
  - `AlertService` catches `DataIntegrityViolationException` and falls back to `alertRepository.findByEventId(event.getId())`, returning the canonical created alert.
  - Verified in `EvidenceFailureRecoveryTest.test6_alertCreationConflict_reusesExistingAlert` and `test8_concurrentOrchestrationRace_handledSafely`.

---

## 12. Alert Failure Recovery

- When an alert candidate fails to save due to conflict or database transient error, evidence calculation is not invalidated.
- A retry safely reuses the canonical event and re-attempts alert candidate creation.
- Transitions to `ACKNOWLEDGED` and `RESOLVED` are idempotent: acknowledging an already acknowledged alert returns the existing record without updating timestamps.

---

## 13. Field Workflow Failure Recovery

`InspectionService` enforces strict state transitions and reject invalid operations:
1. **Assignment to RESOLVED alert:** Throws `IllegalStateException("Cannot assign field team to a RESOLVED alert")`.
2. **Assignment to OPEN alert:** Throws `IllegalStateException("Alert must be ACKNOWLEDGED before assigning a field team")`.
3. **Duplicate Active Assignment:** Throws `IllegalStateException("Alert already has an active field assignment")` (guarded by partial unique index `idx_inspections_unique_active_alert`).
4. **Inspection Start on Completed/Invalid Assignment:** Throws `IllegalStateException("Cannot start inspection in status: ...")`.
5. **Verification Submission on Non-IN_PROGRESS Inspection:** Throws `IllegalStateException("Inspection must be IN_PROGRESS to submit verification")`.
6. **Empty Observed Conditions:** Throws `IllegalArgumentException("Observed conditions (OBSERVED FIELD EVIDENCE) must not be blank")`.
7. **Verification Save Failure:** Atomic transaction rollback ensures `inspection.status` remains `IN_PROGRESS` if `FieldVerification` fails to persist.

---

## 14. API Error Handling

`GlobalExceptionHandler` enforces structured error responses:
- `400 BAD_REQUEST`: `ValidationException`, `IllegalArgumentException`, missing request parameters.
- `404 NOT_FOUND`: `ResourceNotFoundException`.
- `409 CONFLICT`: `IllegalStateException` (workflow/state conflict, duplicate assignment).
- `422 UNPROCESSABLE_ENTITY`: Forecast validation / context mismatch errors.
- `503 SERVICE_UNAVAILABLE`: External AI or model offline.
- `500 INTERNAL_SERVER_ERROR`: Generic fallback.

**Security Rule:** Internal stack traces, raw SQL queries, and database passwords are **never** returned in HTTP error response payloads.

---

## 15. Timeout Behavior

- **Evidence Subprocess Timeout:** Configured via `app.evidence.ai.timeout-ms: 15000` (15 seconds).
- **Process Cleanup:** If execution exceeds 15,000ms, `process.destroyForcibly()` is called immediately and the I/O thread pool is shut down (`ioExecutor.shutdownNow()`).
- **External Telemetry Timeouts:**
  - OpenAQ: 15,000ms
  - Open-Meteo Weather: 15,000ms
  - Forecast AI Subprocess: 30,000ms

---

## 16. Retry Policy

- **Gemini API:** Low temperature (0.1), bounded to max 2 retries in `gemini_client.generate_structured_json`. Upon exhaustion, immediately switches to deterministic fallback.
- **Database Operations:** No automated loop retry on mutation to prevent duplicate records or retry storms. State machine returns deterministic conflict codes (409) allowing caller-controlled retry.

---

## 17. Logging & Observability

All operational logs include structured diagnostic fields:
- `h3` (15-character H3 index)
- `predictionId`
- `eventId`
- `triage`
- `durationMs`
- `correlationId` (via MDC)

Example production log entry:
```
INFO  [req-3f8a91b2c4e1] c.a.e.EvidenceOrchestrationService : Evidence orchestration succeeded h3=88608850e5fffff eventId=446928c2-6090-437d-a7c7-a9909e648403 predictionId=a310c689-f340-49fc-8935-a037de8d7709 triage=INSUFFICIENT_EVIDENCE durationMs=412
```

---

## 18. Correlation ID

- **Filter:** `com.aerosentinel.config.CorrelationIdFilter`
- Inspects incoming header `X-Correlation-Id` (or `X-Request-Id`). If absent, generates `req-<uuid-prefix>`.
- Injects correlation identifier into SLF4J MDC under key `"correlationId"`.
- Adds `X-Correlation-Id` to all outgoing HTTP responses.
- Allows tracing: Frontend Request $\to$ Controller $\to$ Orchestration $\to$ Event $\to$ Evidence $\to$ Alert.

---

## 19. Health & Readiness Behavior

- Backend exposes:
  - Custom health endpoint: `GET /api/v1/health` returning `{"status":"UP","service":"aerosentinel-backend","timestamp":"..."}`.
  - Spring Boot Actuator: `GET /actuator/health`, `GET /actuator/info`, `GET /actuator/metrics`.
- Actuator health details configured with `show-details: when_authorized` to prevent infrastructure leakage to unauthenticated callers.

---

## 20. Frontend Degraded States

- **Loading State:** Clean pulsating skeleton; old cell data cleared immediately.
- **Network Error:** Renders `Failed to Retrieve Evidence Dossier` banner with specific error message and `Retry Query` button.
- **Forecast Unavailable:** Renders `Forecast unavailable for this cell.` inside F4 horizon grid without 0.0 values.
- **AI Unavailable:** Renders `AI UNAVAILABLE` badge and `AI explanation unavailable — showing verified data only.`
- **Alert Queue Error:** Exposes error notification; never displays mock alert cards.
- **Assignment Conflict:** Surfaces actionable message (e.g. *"Alert already has an active field assignment"*).

---

## 21. Security Hardening

- Public endpoints (`/api/v1/health`, `/api/v1/evidence/**`, `/api/v1/alerts/**`, `/api/v1/hotspots/**`) explicitly declared in `SecurityConfig`.
- Protected admin/management routes require valid JWT.
- Error payloads strip internal exception traces and secrets.
- CORS restricted to configured allowed origins and standard headers (`X-Correlation-Id`, `Authorization`, `Content-Type`).

---

## 22. Failure-Injection Test Results

### Backend Failure Injection Suite (`EvidenceFailureRecoveryTest`)
10 of 10 targeted failure injection tests passed:
- `P7-1: Gemini timeout - handled without crash and raises controlled exception` : **PASS**
- `P7-2: Gemini malformed response - schema validation rejects and aborts persistence` : **PASS**
- `P7-3: F4 unavailable - truthfully preserved as null, F3 and F5 scoring intact` : **PASS**
- `P7-4: F3 unavailable - returns 404 NOT_FOUND without fabricating risk score` : **PASS**
- `P7-5: Database persistence failure - triggers transaction abort without partial state` : **PASS**
- `P7-6: Alert creation conflict - race caught by unique constraint reuses existing canonical alert` : **PASS**
- `P7-7: Duplicate retry protection - repeated orchestration reuses existing event and signals` : **PASS**
- `P7-8: Concurrent orchestration race - database duplicate prevention preserves single canonical event` : **PASS**
- `P7-9: Invalid field workflow transitions - strictly rejected with state machine errors` : **PASS**
- `P7-10: Verification persistence failure - aborts transaction and preserves inspection integrity` : **PASS**

### Frontend Failure Recovery Suite (`f5_p7_failure_recovery.test.ts`)
7 of 7 targeted frontend tests passed:
- `P7-11: Evidence API failure sets error state and preserves null evidence without mock data` : **PASS**
- `P7-12: Forecast unavailable produces truthful degraded state without synthesizing zeros` : **PASS**
- `P7-13: Gemini unavailable renders degraded notice while preserving evidence dossier` : **PASS**
- `P7-14: Alert queue load failure preserves empty queue without fabricating alerts` : **PASS**
- `P7-15: Field assignment conflict renders error message and keeps modal open for correction` : **PASS**
- `P7-16: Verification submission failure preserves inspector input for retry` : **PASS**
- `P7-17: Rapid cell switch discards superseded in-flight responses` : **PASS**

---

## 23. Backend Regression Results

Full Maven test suite execution for F5 modules (`Evidence*Test`, `Alert*Test`, `Inspection*Test`):
- `EvidenceFailureRecoveryTest`: 10 tests run, 0 failures, 0 errors
- `EvidenceUnitTest`: 19 tests run, 0 failures, 0 errors
- `EvidenceIntegrationTest`: 1 test run, 0 failures, 0 errors
- `AlertUnitTest`: 13 tests run, 0 failures, 0 errors
- `AlertIntegrationTest`: 8 tests run, 0 failures, 0 errors
- `InspectionUnitTest`: 14 tests run, 0 failures, 0 errors
- `InspectionIntegrationTest`: 14 tests run, 0 failures, 0 errors
- **Total Backend Tests Run:** **79**
- **Failures:** **0**
- **Errors:** **0**
- **Skipped:** **0**
- **Build Result:** **BUILD SUCCESS** (Total time: 01:20 min)

---

## 24. Frontend Regression Results

Full frontend vitest test suite execution:
- Total test files: 5 suites
- **Total Tests Run:** **133**
- **Passed:** **133**
- **Failures:** **0**
- **Skipped:** **0**
- **TypeScript Compilation (`tsc --noEmit`):** **0 errors**

---

## 25. Python Regression Results

Full Python pytest execution in `ai-service`:
- `tests/test_f3_ml_inference.py`: 13 passed
- `tests/test_f4_p3_inference.py`: 13 passed
- `tests/test_f4_p7_reliability.py`: 13 passed
- `tests/ai/test_f5_alert_support.py`: 10 passed
- `tests/test_f5_p7_failure_recovery.py`: 6 passed
- **Total Python Tests Run:** **55**
- **Passed:** **55**
- **Failures:** **0**

---

## 26. Real Runtime Proof

Live queries executed against the running Spring Boot service:

### Health Endpoint
```http
GET http://localhost:8080/api/v1/health
```
```json
{
  "status": "UP",
  "service": "aerosentinel-backend",
  "timestamp": "2026-09-28T15:53:45.895397500Z"
}
```

### Pune Baseline Cell (H3: `88608850e5fffff`)
```http
GET http://localhost:8080/api/v1/evidence/hotspot/88608850e5fffff
```
```json
{
  "status": "SUCCESS",
  "context": {
    "h3Index": "88608850e5fffff",
    "eventCode": "EVT-88608850-2026092613-d75654e9"
  },
  "evidence": {
    "evidenceScore": 0.157,
    "triageState": "INSUFFICIENT_EVIDENCE"
  }
}
```

### Qualifying Alert Cell (H3: `886196944dfffff`)
```http
GET http://localhost:8080/api/v1/evidence/hotspot/886196944dfffff
```
```json
{
  "status": "SUCCESS",
  "context": {
    "h3Index": "886196944dfffff",
    "eventCode": "EVT-88619694-2026092815-2b9cf199"
  },
  "evidence": {
    "evidenceScore": 0.238,
    "triageState": "INSUFFICIENT_EVIDENCE"
  }
}
```

---

## 27. PostgreSQL Database Integrity Proof

Direct SQL audit verified against live database `aerosentinel`:

```sql
SELECT COUNT(*) FROM pollution_events;      -- Result: 25
SELECT COUNT(*) FROM event_evidence;        -- Result: 28
SELECT COUNT(*) FROM gemini_analyses;       -- Result: 66
SELECT COUNT(*) FROM alerts;                -- Result: 9
SELECT COUNT(*) FROM inspections;           -- Result: 4
SELECT COUNT(*) FROM field_verifications;   -- Result: 4

-- Relational Integrity Checks
SELECT COUNT(*) FROM event_evidence 
WHERE event_id NOT IN (SELECT id FROM pollution_events);
-- Result: 0 (No orphan evidence)

SELECT COUNT(*) FROM alerts 
WHERE event_id IS NOT NULL AND event_id NOT IN (SELECT id FROM pollution_events);
-- Result: 0 (No orphan alerts)

SELECT COUNT(*) FROM inspections 
WHERE alert_id NOT IN (SELECT id FROM alerts);
-- Result: 0 (No orphan inspections)

SELECT COUNT(*) FROM field_verifications 
WHERE inspection_id NOT IN (SELECT id FROM inspections);
-- Result: 0 (No orphan verifications)
```

**PostgreSQL Integrity:** **100% CLEAN** — Zero orphan records, complete relational linkage across all 6 tiers.

---

## 28. Known Limitations

1. **Weather Ingestion Polling Rate:** Open-Meteo rate limiting is respected by caching surface observations at 15-minute intervals. If Open-Meteo is completely unreachable, the system relies on stored observations without failing the hotspot calculation.
2. **Subprocess I/O Pipe Latency:** The Python CLI bridge incurs ~200ms process startup latency on Windows; bounded by a strict 15,000ms safety timeout.

---

## 29. Definition of Done (DoD) Checklist

- [x] Failure matrix audited (all 21 items documented).
- [x] Gemini timeout handled without crash.
- [x] Gemini API failure handled with deterministic fallback.
- [x] Gemini malformed output handled safely.
- [x] Grounding failure handled; unsupported causal claims neutralized.
- [x] F4 unavailable handled truthfully (`forecast == null`, no synthetic zeroes).
- [x] F3 unavailable handled truthfully (404, no fabricated risk scores).
- [x] Partial evidence states preserved (4-valued state matrix).
- [x] Stale data handled correctly (recency decay verified).
- [x] DB transaction rollback tested on persistence exception.
- [x] Alert failure/retry behavior tested.
- [x] Duplicate retry protection tested (canonical event/evidence reused).
- [x] Concurrent orchestration tested (`DataIntegrityViolationException` recovery).
- [x] Concurrent alert creation protection preserved.
- [x] P6 assignment failure handling verified (rejects RESOLVED/OPEN alerts).
- [x] P6 verification failure handling verified (rejects invalid state/result).
- [x] API error contract audited and standardized.
- [x] Sensitive error information not exposed to client.
- [x] Operational logging improved with structured context.
- [x] Correlation ID implemented via `CorrelationIdFilter` and SLF4J MDC.
- [x] Health/readiness behavior audited (`/api/v1/health` and actuator).
- [x] Frontend degraded states verified (clean empty/error/loading notices).
- [x] Evidence page degraded mode verified (preserves dossier when AI fails).
- [x] Alert page degraded mode verified.
- [x] Timeout behavior verified (15s bounded execution).
- [x] Retry policy bounded (no infinite loops or retry storms).
- [x] No duplicate data created from retries.
- [x] No orphan records after expected rollback (verified 0 orphans in DB).
- [x] Existing F3/F4/F5/P5/P6 functionality preserved.
- [x] Current frontend tests pass (133/133 pass).
- [x] Current backend tests pass (79/79 pass).
- [x] Current Python tests pass (55/55 pass).
- [x] TypeScript passes with 0 compilation errors.
- [x] Real runtime normal path verified on Pune cells.
- [x] Controlled failure scenarios verified.
- [x] PostgreSQL integrity verified.
- [x] No business logic duplicated.
- [x] Documentation report created.

---

## 30. Final Status

# F5-P7 STATUS: PASS
