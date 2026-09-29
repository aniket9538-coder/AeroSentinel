# AeroSentinel — F6-P3 Repository Audit
## Citizen Evidence → Pollution Event Integration: Lineage, Deduplication, and Spatial/Temporal Matching

**Audit Date:** 2026-09-28  
**Feature Phase:** F6-P3  
**Status:** COMPLETE & PASS  

---

### 1. Executive Summary

F6-P1 and F6-P2 successfully delivered end-to-end multipart citizen report ingestion, server-side Uber H3 Resolution 8 derivation, isolated photo storage with magic-byte verification, and Gemini Vision inference with deterministic fallback.

F6-P3 focuses strictly on closing the architectural bridge between an **analyzed Citizen Report** and the **existing Feature 5 (F5) Spatial Evidence & Decision Pipeline**. Specifically, crowdsourced ground visual observations must be integrated into canonical `PollutionEvent` records as auxiliary `EventEvidence` without allowing citizen reports by themselves to fabricate events or bypass regulatory alert thresholds to trigger `ALERT_CANDIDATE`.

---

### 2. Current Component Audit & Traceability

#### 2.1 Citizen Ingestion & Gemini Vision Flow (F6-P2 Baseline)
- **Controller:** [`CitizenReportController.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/citizen/CitizenReportController.java) exposes `POST /api/v1/citizen/reports` consuming `multipart/form-data`.
- **Service:** [`CitizenReportService.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/citizen/CitizenReportService.java) validates geographic coordinates, enforces standard incident categories, stores photos, and invokes `CitizenVisionAiClient`.
- **Entity:** [`CitizenReport.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/citizen/CitizenReport.java) maps `id`, `city_id`, `latitude`, `longitude`, `h3_index`, `category`, `description`, `image_url`, `submitted_at`, `status`, `verification_status`.
- **AI Analysis:** [`GeminiAnalysis.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/model/GeminiAnalysis.java) maps `citizen_report_id`, `detected_category`, `confidence`, `model_name`, `narrative_summary`, `h3_index`.
- **Current Observation:** When a report is ingested, `status` moves from `PENDING` $\to$ `ANALYZED` (or `ANALYSIS_UNAVAILABLE` on fallback). However, `CitizenReportService` does not currently link the report to a `PollutionEvent` or persist an `EventEvidence` record.

#### 2.2 H3 Lineage Consistency
- **Authoritative Resolution:** Strictly Uber H3 Resolution 8 (~0.737 $\text{km}^2$).
- **Calculation:** Derived server-side via [`H3Utils.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/util/H3Utils.java) and native `H3Core` in [`H3Service.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/spatial/H3Service.java).
- **Lineage Chain:**
  $$\text{Coordinates } (\text{lat, lng}) \longrightarrow \text{H3 Res 8 } (\text{CitizenReport.h3Index}) = \text{Event.h3Index} = \text{GeminiAnalysis.h3Index} = \text{EventEvidence spatial context}$$
- **Current Status:** H3 derivation is strictly server-side and robust. Client-supplied H3 claims are discarded.

#### 2.3 Existing Pollution Event Creation & Reuse Logic
- **Entity:** [`PollutionEvent.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/event/PollutionEvent.java) with `id`, `grid_cell_id`, `h3_index`, `prediction_id`, `event_code`, `severity`, `status`, `started_at`, `resolved_at`.
- **Repository:** [`PollutionEventRepository.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/event/PollutionEventRepository.java) provides:
  - `findByEventCode(String eventCode)`
  - `findTopByH3IndexOrderByStartedAtDesc(String h3Index)`
  - `findByH3IndexOrderByStartedAtDesc(String h3Index)`
  - `findByPredictionId(UUID predictionId)`
- **Orchestration Resolution:** In [`EvidenceOrchestrationService.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/evidence/EvidenceOrchestrationService.java), `resolveOrCreatePollutionEvent()` uses deterministic hourly canonical codes:
  $$\text{eventCode} = \text{"EVT-" + h3Prefix + "-" + timeClean}$$
  If an event with `canonicalCode` exists, it is reused. If not, a new `PollutionEvent` is instantiated with `status = "OPEN"` and `startedAt = ctx.predictedAt()`.
- **Constraint:** A citizen report must **never** independently create a `PollutionEvent` unless a valid environmental condition/prediction warrants it.

#### 2.4 Citizen Deduplication Logic
- **Authoritative Rule:** In [`ai-service/ml/alert_support/citizen_dedup.py`](file:///c:/Users/lenovo/AeroSential/ai-service/ml/alert_support/citizen_dedup.py):
  $$\text{Same H3 cell } + \Delta t \le 60.0 \text{ minutes } \Longrightarrow \text{duplicate / coalesced report}$$
- **Current Integration:** `CitizenReportDeduplicator` is executed in `EventEvidenceScoringEngine.py` on `raw_citizen_reports`. However, Java lacks a direct representation of this deduplication rule in `EvidenceOrchestrationService` and `CitizenReportService`.

#### 2.5 Event Evidence Persistence & Lineage
- **Entity:** [`EventEvidence.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/evidence/EventEvidence.java):
  - `eventId` (UUID, non-null)
  - `sourceType` (String, e.g., `"CITIZEN_OBSERVATION"`)
  - `evidenceKey` (String, unique per event, e.g., `"citizen-report-<UUID>"`)
  - `evidenceValue` (Text description / detected condition)
  - `weight` (Double, defaults to visual confidence score)
  - `signalId` (String, e.g., `"sig-<h3Prefix>-cit-<idx>"`)
  - `dataSource` (String, strictly `"CITIZEN"`)
  - `relevanceTier` (String, strictly `"AUXILIARY"`)
  - `sourceRef` (String, strictly citizen report UUID)
  - `confidenceScore` (Double, visual interpretation confidence)
  - `observedAt` (Instant, report submission/observation timestamp)
- **Repository:** [`EvidenceRepository.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/evidence/EvidenceRepository.java):
  - `existsByEventIdAndEvidenceKey(UUID eventId, String evidenceKey)`
  - `findByEventId(UUID eventId)`
- **Idempotency Guard:** `existsByEventIdAndEvidenceKey` ensures repeated orchestrations will never insert duplicate rows.

#### 2.6 F5 Evidence Scoring Engine Integration
- **Scoring Engine:** [`EventEvidenceScoringEngine.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/evidence/EventEvidenceScoringEngine.java) and [`scoring_engine.py`](file:///c:/Users/lenovo/AeroSential/ai-service/ml/alert_support/scoring_engine.py).
- **Citizen Contribution:**
  - Citizen observations participate in `SourceObservationState.SUPPORTED` for the `source_matrix.citizen` tier.
  - They increment `supported_physical_count` in `multi_source_agreement` and contribute to `evidence_completeness`.
  - Recency is discounted using half-life $\tau = 90.0$ minutes ([`recency.py`](file:///c:/Users/lenovo/AeroSential/ai-service/ml/alert_support/recency.py)).
- **Safety Invariant:** Citizen evidence alone **never** triggers `ALERT_CANDIDATE`. Ground observations or ML threshold crossings ($\text{riskScore} \ge 0.20$ and $\text{evidenceScore} \ge 0.55$) are mandatory for alert elevation.

---

### 3. Exact Contract Gaps to Close in F6-P3

| Item | Current State | Required F6-P3 State |
| :--- | :--- | :--- |
| **GAP 1: Java Citizen Deduplicator** | Only exists in Python `citizen_dedup.py`. | Implement [`CitizenReportDeduplicator.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/citizen/CitizenReportDeduplicator.java) enforcing identical 60-minute same-H3 coalescing rule in Java. |
| **GAP 2: Spatial & Temporal Matching** | No automated matching linking an ingested/analyzed citizen report to active `PollutionEvent`. | Implement `CitizenEventMatcher` / matching logic with strict criteria: `report.h3Index == event.h3Index` AND $|t_{\text{report}} - t_{\text{event}}| \le 120\text{ min}$ (or active open event). No event fabricated if no match exists. |
| **GAP 3: EventEvidence Persistence for Citizen Reports** | In `EvidenceOrchestrationService`, `persistEventEvidence` only saves telemetry signals; citizen reports are passed to CLI but not persisted to `event_evidence` table. | For every deduplicated, matched citizen report, persist an `EventEvidence` row with `dataSource = "CITIZEN"`, `sourceRef = report.id`, `relevanceTier = "AUXILIARY"`, and visual confidence score. |
| **GAP 4: CLI Signals Serialization** | `orchestrate_evidence_cli.py` only outputs ground telemetry in `signals_out`. | Extend CLI to include deduplicated citizen evidence signals in `signals_out` so the full unified response and evidence cards reflect citizen observations. |
| **GAP 5: Frontend Evidence Display** | `EvidencePanel.tsx` displays physical telemetry cards (Air, Meteo, GIS, Proximity) but lacks a dedicated crowdsourced citizen visual evidence card. | Add a citizen evidence card under OBSERVED / EVIDENCE displaying citizen report ID, visual interpretation, confidence, photo thumbnail/link, and unverified advisory note. |
| **GAP 6: Analysis vs Verification Lifecycle** | `status = ANALYZED` is present, but verification lifecycle `UNVERIFIED` vs `VERIFIED` must be strictly clarified in API and UI. | Preserve strict separation: `ANALYZED` indicates Gemini completed visual interpretation; `verificationStatus` remains `UNVERIFIED` until authority/inspector confirmation. |

---

### 4. Implementation Plan for F6-P3

1. **Create `CitizenReportDeduplicator.java`:** Authoritative Java implementation matching `MAX_TIME_DELTA_MINUTES = 60.0`.
2. **Enhance `EvidenceOrchestrationService.java`:**
   - Filter and deduplicate citizen reports in target H3.
   - Perform temporal matching against event `startedAt` (120-minute window).
   - Persist citizen `EventEvidence` with idempotent key `citizen-report-<UUID>`.
   - Update `GeminiAnalysis.eventId` linkage when citizen analysis is attached to an event.
3. **Enhance `CitizenReportService.java`:**
   - On report submission and vision analysis completion, check for active matching `PollutionEvent` in the same H3.
   - If a matching event exists, attach as `EventEvidence` and link `GeminiAnalysis.eventId`.
   - If no matching event exists, retain report and analysis safely without fabricating an event.
4. **Update `orchestrate_evidence_cli.py`:**
   - Include deduplicated citizen reports as atomic signals in `signals_out`.
5. **Update `EvidencePanel.tsx` & Types:**
   - Render crowdsourced visual observations under observed telemetry with photo preview, confidence badge, and advisory disclaimer.
6. **Execute Targeted Backend & Frontend Tests:**
   - Unit tests for H3 matching, temporal window, deduplication, idempotency, and alert isolation.
   - Integration tests against PostgreSQL.
7. **Execute Real Runtime Verification:**
   - Verify report `07b813e2-a17d-455f-9761-744c0989a6ce` in H3 `88608850e5fffff` linked to canonical `PollutionEvent`.
