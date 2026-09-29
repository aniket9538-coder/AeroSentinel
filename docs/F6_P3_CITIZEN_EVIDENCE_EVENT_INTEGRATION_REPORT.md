# AEROSENTINEL — F6-P3 CITIZEN EVIDENCE → POLLUTION EVENT INTEGRATION REPORT
**Lineage + Deduplication + Spatio-Temporal Matching**  
**Date:** September 28, 2026  
**Status:** PASS  

---

## 1. Objective
The objective of F6-P3 is to connect analyzed citizen reports to the authoritative AeroSentinel pollution-event/evidence pipeline while ensuring:
1. **Lineage:** Server-side H3 Resolution 8 spatial context is maintained seamlessly from `CitizenReport` → `GeminiAnalysis` → `PollutionEvent` → `EventEvidence` → `F5 Evidence Scoring`.
2. **Auxiliary Role:** Citizen evidence strictly enters the evidence pipeline as `AUXILIARY` telemetry (`dataSource = "CITIZEN"`). Under no circumstances can a citizen report alone trigger an event or an `ALERT_CANDIDATE`.
3. **Deduplication:** Reuse authoritative 60-minute same-H3 cell coalescing.
4. **Spatio-Temporal Matching:** Deterministic matching against active events within the spatial cell (or its immediate clustering neighborhood) and a temporal relevance window ($\le 120$ minutes). If no matching event exists, **no synthetic event is fabricated**.
5. **Idempotency:** Repeated orchestration runs must produce zero duplicate `EventEvidence` records.
6. **Integrity & Truth in AI:** Explicit distinction between real Gemini Vision inference and deterministic fallback when `GEMINI_API_KEY` is unconfigured.

---

## 2. Repository Audit Findings
Pre-implementation audit confirmed:
- **`CitizenReport` & `GeminiAnalysis`**: F6-P2 correctly established the database tables, H3 generation, and initial vision pipeline.
- **`PollutionEvent` & `EventEvidence`**: Existing F5 entities possessed `dataSource`, `evidenceKey`, `relevanceTier`, and `confidenceScore` fields ready for ingestion.
- **`CitizenReportDeduplicator`**: Authoritative logic (60-minute window, same H3) was verified in Python (`ml/alert_support/citizen_dedup.py`) and ported to Java backend (`com.aerosentinel.citizen.CitizenReportDeduplicator`) to guarantee cross-layer deduplication parity.
- **Gaps Identified**:
  - `EvidenceOrchestrationService.java` previously orchestrated satellite and ground sensor signals without pulling analyzed citizen reports into the canonical event evidence set.
  - `orchestrate_evidence_cli.py` lacked explicit emission of citizen reports as evidence signals.
  - `EvidencePanel.tsx` lacked auxiliary crowdsourced citizen visual observation cards with truth-in-AI disclaimers.

Audit document: [`docs/F6_P3_CITIZEN_EVIDENCE_EVENT_INTEGRATION_AUDIT.md`](file:///c:/Users/lenovo/AeroSential/docs/F6_P3_CITIZEN_EVIDENCE_EVENT_INTEGRATION_AUDIT.md).

---

## 3. Files Changed
1. **`backend/src/main/java/com/aerosentinel/citizen/CitizenReportDeduplicator.java`**
   - Implements authoritative 60-minute same-H3 coalescing algorithm in Java.
2. **`backend/src/main/java/com/aerosentinel/citizen/CitizenEventMatcher.java`**
   - Evaluates candidate events for spatial exact match (H3 Resolution 8) and temporal relevance ($\le 120$ min).
3. **`backend/src/main/java/com/aerosentinel/citizen/CitizenReportService.java`**
   - Connects analyzed citizen reports to existing events as auxiliary evidence upon intake without fabricating events.
4. **`backend/src/main/java/com/aerosentinel/evidence/EvidenceOrchestrationService.java`**
   - Gathers deduplicated citizen reports during hotspot orchestration, persists unique `EventEvidence` entries (`evidenceKey = "citizen-report-" + reportId`), links `gemini_analyses.event_id`, and formats them into the unified evidence response.
5. **`ai-service/ml/inference/orchestrate_evidence_cli.py`**
   - Correctly integrates `CitizenReportDeduplicator` and formats citizen signals with `sourceType = "CITIZEN_OBSERVATION"`, `dataSource = "CITIZEN"`, `relevanceTier = "AUXILIARY"`.
6. **`frontend/src/components/hotspot/EvidencePanel.tsx`**
   - Renders crowdsourced citizen visual evidence under the `OBSERVED` telemetry section with visual category badges, observation confidence, photo access link, and clear "Unverified Citizen Report" disclaimers.
7. **Test Suites Added/Updated**:
   - `backend/src/test/java/com/aerosentinel/citizen/CitizenEventIntegrationTest.java` (11 integration tests)
   - `frontend/src/components/hotspot/__tests__/f6_p3_citizen_evidence.test.ts` (7 frontend unit tests)

---

## 4. Existing Components Reused
- **H3 Resolution 8**: Utilized existing `H3Service.java` (`geoToH3Address` with resolution 8).
- **Gemini Vision Pipeline**: Reused existing `GeminiVisionService.java` and `GeminiAnalysisRepository.java`.
- **F5 Evidence Architecture**: Reused `PollutionEventRepository.java`, `EventEvidenceRepository.java`, and `EventEvidenceScoringEngine`.
- **Photo Storage**: Reused `PhotoStorageService.java` with secure hash-based retrieval.

---

## 5. H3 Lineage Proof
Authoritative lineage is strictly maintained using server-side Resolution 8:
```
Latitude / Longitude (e.g., 18.5204, 73.8567)
       ↓  H3Service.geoToH3Address(lat, lon, 8)
CitizenReport.h3Index = 88608850e5fffff
       ↓  CitizenEventMatcher.matches(...)
PollutionEvent.h3Index = 88608850e5fffff
       ↓  EventEvidence.spatialContext = 88608850e5fffff
F5 Hotspot Evidence Scoring Engine
```
Client-supplied H3 values are never trusted; coordinates are always indexed on the backend.

---

## 6. Citizen Deduplication Proof
- **Authoritative Rule**: Reports within the same H3 resolution 8 cell within a 60-minute sliding window are deduplicated.
- **Verification**:
  - `CitizenReportDeduplicatorTest`: Multiple submissions within 60 minutes in `88608850e5fffff` coalesce into 1 canonical report.
  - Submissions outside 60 minutes or in different cells (e.g. `88608850e7fffff`) remain distinct.
  - CLI and Java backend both implement identical deduplication logic.

---

## 7. Temporal & Spatial Matching Implementation
- **Spatial Matching**: Primary match requires `report.h3Index.equalsIgnoreCase(event.h3Index)`.
- **Temporal Matching**: Configurable threshold $\Delta t \le 120$ minutes (`java.time.Duration.between(reportTime, eventTime).abs() <= 120 min`).
- **No-Match Behavior**: If no active or recent pollution event exists within the spatial cell and temporal window:
  - The citizen report and its Gemini analysis remain persisted in `citizen_reports` and `gemini_analyses`.
  - **No synthetic pollution event is fabricated.**
  - `gemini_analyses.event_id` remains `NULL`.
  - Zero `EventEvidence` records are created until a legitimate physical monitoring event is detected.

---

## 8. Event Reuse / Creation Behavior
- Citizen reports **never** create new events independently.
- When an existing pollution event (created via F3/F5 sensor/satellite threshold triggers) exists in the cell, the citizen report is attached as auxiliary evidence.
- Event identity, event code formatting (`EVT-{h3}-{timestamp}-{hash}`), and idempotency rules remain strictly preserved.

---

## 9. EventEvidence Persistence
For matched citizen reports, an `EventEvidence` record is persisted with:
- **`eventId`**: FK linking to canonical `pollution_events.id`.
- **`dataSource`**: `"CITIZEN"`.
- **`sourceType`**: `"CITIZEN_OBSERVATION"`.
- **`evidenceKey`**: `"citizen-report-" + report.getId()`.
- **`relevanceTier`**: `"AUXILIARY"` (Strict invariant).
- **`confidenceScore`**: Visual observation confidence from Gemini analysis (e.g. 0.75).
- **`observedAt`**: Actual citizen report timestamp.
- **`sourceRef`**: UUID string of the citizen report.

---

## 10. F5 Scoring Integration
- Citizen evidence enters the existing `EventEvidenceScoringEngine` under the `CITIZEN` data source.
- Weights: Citizen evidence carries low auxiliary weight ($\le 0.05$).
- Strict Rule Enforced: Auxiliary evidence alone can never escalate an event to `ALERT_CANDIDATE`. Physical sensor or satellite primary telemetry is mandatory for alert candidate generation.

---

## 11. Idempotency Proof
- **Evidence Key**: Uniquely set to `citizen-report-{citizenReportId}`.
- **Repository Check**: Prior to persistence, `eventEvidenceRepository.findByEventIdAndEvidenceKey(event.getId(), evidenceKey)` is checked.
- **Live Verification**: Calling the orchestration endpoint twice on hotspot `88608850e5fffff` maintained the exact same count of 9 evidence records (0 duplicate rows created).

---

## 12. Gemini Real-vs-Fallback Status
- **Status in Live Environment**: Deterministic fallback active (`modelVersion = "deterministic-fallback-v1.0"`) because `GEMINI_API_KEY` was not configured in environment variables.
- **Integrity Guarantee**: The system truthfully marks the analysis metadata as fallback and does **not** falsely claim real Gemini neural inference.
- **Readiness**: Once `GEMINI_API_KEY` is provided, `GeminiVisionService` seamlessly calls Google Generative Language API without code changes.

---

## 13. Frontend Integration
- Modified `frontend/src/components/hotspot/EvidencePanel.tsx`:
  - Implemented dynamic rendering of crowdsourced citizen visual evidence under `OBSERVED` telemetry.
  - Shows visual category (e.g. `SMOKE_LIKE`), visual confidence (e.g. `75%`), timestamp, H3 resolution 8 anchor, and verified thumbnail photo.
  - Prominently displays: `"Crowdsourced auxiliary report — pending physical sensor correlation."` and `"Unverified Citizen Report"`.
  - Handles fallback/missing AI state gracefully.

---

## 14. Unit Test Results
- **Backend Unit Tests**:
  - `CitizenReportUnitTest`: 8/8 passed
  - `EvidenceUnitTest`: 19/19 passed
  - `CitizenEventIntegrationTest` unit methods: passed
- **Total Backend Unit Tests**: **27/27 PASS**

---

## 15. Integration Test Results
- **`CitizenEventIntegrationTest.java`**:
  1. `testExactH3Matching`: PASS
  2. `testNoEventFabricationWhenNoMatch`: PASS
  3. `testCitizenEvidencePersistedAsAuxiliary`: PASS
  4. `testRepeatedOrchestrationIsIdempotent`: PASS
  5. `testCitizenEvidenceAloneDoesNotCreateAlertCandidate`: PASS
  6. `testSameH3Within60MinCoalesces`: PASS
  7. `testOutside60MinDoesNotCoalesce`: PASS
  8. `testVisualConfidenceSeparation`: PASS
  9. `testAnalyzedDoesNotEqualVerified`: PASS
  10. `testPhotoStorageAccessible`: PASS
  11. `testTemporalMatchingRejectsOutside120Min`: PASS
- **Total Integration Tests**: **11/11 PASS**

---

## 16. Frontend Test Results
- **`f6_p3_citizen_evidence.test.ts`**:
  1. `renders citizen visual evidence card when present in evidence summary`: PASS
  2. `does not display citizen evidence card when no citizen signals exist`: PASS
  3. `separates visual confidence from sensor risk score`: PASS
  4. `displays deterministic fallback banner when AI analysis used fallback`: PASS
  5. `handles photo rendering and graceful missing photo link`: PASS
  6. `handles report in ANALYZED state without claiming VERIFIED`: PASS
  7. `never labels citizen evidence as confirmed pollution or alert candidate`: PASS
- **Total Frontend Test Suite**: **158/158 PASS** (across all frontend test files)

---

## 17. TypeScript Result
- Executed `npx tsc --noEmit` in `frontend/`.
- **Result**: Code exited with code 0 (zero errors).

---

## 18. PostgreSQL Lineage Verification
Direct query on database `aerosentinel`:
```sql
SELECT 
  cr.id AS citizen_report_id, 
  cr.h3_index AS citizen_h3, 
  ga.id AS gemini_analysis_id, 
  ga.detected_category, 
  ga.confidence, 
  pe.id AS event_id, 
  pe.event_code, 
  ee.id AS evidence_id, 
  ee.data_source, 
  ee.evidence_key, 
  ee.relevance_tier 
FROM citizen_reports cr 
JOIN gemini_analyses ga ON ga.citizen_report_id = cr.id 
JOIN pollution_events pe ON ga.event_id = pe.id 
JOIN event_evidence ee ON ee.event_id = pe.id AND ee.source_ref = cr.id::text 
WHERE cr.id = '07b813e2-a17d-455f-9761-744c0989a6ce';
```
**Output:**
```
-[ RECORD 1 ]------+--------------------------------------
citizen_report_id  | 07b813e2-a17d-455f-9761-744c0989a6ce
citizen_h3         | 88608850e5fffff
gemini_analysis_id | 01d5db28-f41b-4074-a178-618cd7ed822e
detected_category  | SMOKE_LIKE
confidence         | 0.75
event_id           | 58ef2f64-4bc5-496f-9094-e5f61e44f8b0
event_code         | EVT-88608850-2026092816-f28bd5fe
evidence_id        | fe1a1846-f17c-449a-b789-244b9e043e08
data_source        | CITIZEN
evidence_key       | citizen-report-07b813e2-a17d-455f-9761-744c0989a6ce
relevance_tier     | AUXILIARY
```
- **Orphan Count**: 0 orphan records.
- **Duplicate Count**: 0 duplicates (`COUNT(*) = 1` for `evidence_key`).

---

## 19. Real Runtime Evidence
- **API Endpoint Call**: `GET http://localhost:8080/api/v1/evidence/hotspot/88608850e5fffff`
- **Response Snippet**:
```json
{
  "hotspotH3": "88608850e5fffff",
  "evidenceCount": 9,
  "canonicalEventCode": "EVT-88608850-2026092816-f28bd5fe",
  "signals": [
    {
      "sourceType": "CITIZEN_OBSERVATION",
      "dataSource": "CITIZEN",
      "relevanceTier": "AUXILIARY",
      "confidenceScore": 0.75,
      "spatialContext": "88608850e5fffff",
      "visualCategory": "SMOKE_LIKE",
      "photoUrl": "/api/v1/citizen/photos/7e3753bd-32d9-4fb9-99b1-90d8085ff188.jpg",
      "citizenReportId": "07b813e2-a17d-455f-9761-744c0989a6ce",
      "verificationStatus": "ANALYZED"
    }
  ]
}
```
- **Alert Count**: `SELECT count(*) FROM alerts;` returned `0`. Auxiliary citizen evidence strictly adhered to non-alerting rules.

---

## 20. Known Limitations
1. In development environments without an active Google AI Studio key, Gemini analysis executes in deterministic fallback mode (`deterministic-fallback-v1.0`). Real neural inference is automatically enabled as soon as `GEMINI_API_KEY` is exported.
2. Citizen reports outside the temporal window ($\Delta t > 120$ min) or without an active physical pollution event are preserved in the database but remain unattached until future correlated monitoring data triggers event clustering.

---

## 21. Final PASS/FAIL
All requirements, invariants, tests, and database lineage checks have been executed and verified:
- Spatial Lineage (H3 Res 8): PASS
- Citizen Deduplication (60-min window): PASS
- Temporal / Spatial Matching: PASS
- Non-Fabrication of Events: PASS
- Auxiliary Role & Non-Alert Elevation: PASS
- Idempotency & Zero Duplication: PASS
- Full PostgreSQL Foreign Key Traceability: PASS
- Frontend Visualization & Truth-in-AI: PASS

**Overall Result: PASS**
