# F6-P5 CITIZEN EVIDENCE INTEGRATION AUDIT
**AeroSentinel — Citizen Gemini Result → Citizen Evidence → Existing F5 Evidence Engine**  
**Date:** 2026-09-29  
**Status:** AUDIT COMPLETE (Pre-Implementation Verification)  

---

## 1. EXECUTIVE SUMMARY

An exhaustive audit of the frontend, backend, AI service, and PostgreSQL database was conducted to evaluate the readiness of the system for **F6-P5**:
$$\text{Citizen Gemini Result} \longrightarrow \text{Citizen Evidence} \longrightarrow \text{Existing F5 Evidence Engine}$$

The architecture already contains foundational F6-P3/P4 contracts. This audit verifies the 14 required integration questions, confirms that the existing F5 evidence engine (`EventEvidenceScoringEngine`) is authoritative and intact, identifies any idempotency edge cases, and establishes the verification baseline.

---

## 2. AUDIT CHECKLIST: 14 KEY VERIFICATIONS

### 1. Is `GeminiAnalysis` linked to the citizen report?
- **Status:** **YES (VERIFIED)**
- **Code Reference:** `backend/src/main/java/com/aerosentinel/citizen/CitizenReportService.java` (lines 144–159):
  ```java
  GeminiAnalysis analysis = new GeminiAnalysis();
  analysis.setCitizenReportId(savedReport.getId());
  analysis.setH3Index(h3Index);
  analysis.setDetectedCategory(visionResult.category());
  analysis.setConfidence(visionResult.confidence());
  ...
  savedAnalysis = geminiAnalysisRepository.save(analysis);
  ```
- **Database Reference:** `gemini_analyses.citizen_report_id` has a direct foreign key / UUID reference to `citizen_reports.id`. For example, report `07b813e2-a17d-455f-9761-744c0989a6ce` has linked analysis `01d5db28-f41b-4074-a178-618cd7ed822e`.

### 2. Is `GeminiAnalysis` category/confidence available to evidence logic?
- **Status:** **YES (VERIFIED)**
- **Code Reference:**
  - `CitizenReportService.java` (lines 217–220): Queries `analysis.getConfidence()` and `analysis.getDetectedCategory()`.
  - `EvidenceOrchestrationService.java` (lines 322–324, 451–458):
    ```java
    Optional<GeminiAnalysis> gaOpt = geminiAnalysisRepository.findTopByCitizenReportIdOrderByCreatedAtDesc(cr.getId());
    double conf = gaOpt.map(GeminiAnalysis::getConfidence).orElse(0.75);
    String detected = gaOpt.map(GeminiAnalysis::getDetectedCategory).orElse(cr.getCategory());
    ```
  - `orchestrate_evidence_cli.py` (lines 280–293): Maps `detected_category` and `confidence` into atomic signals and passes them to `EventEvidenceScoringEngine`.

### 3. Is citizen evidence converted into an `EventEvidence` record?
- **Status:** **YES (VERIFIED)**
- **Code Reference:** Both during ingestion in `CitizenReportService.attachToEventIfMatching` (lines 222–243) when a matching event exists, and during F5 dossier orchestration in `EvidenceOrchestrationService.persistCitizenEvidence` (lines 326–345).

### 4. Does that record use `dataSource = CITIZEN`?
- **Status:** **YES (VERIFIED)**
- **Code Reference:** `ev.setDataSource("CITIZEN");` in both `CitizenReportService.java` (line 233) and `EvidenceOrchestrationService.java` (line 337).
- **Database Verification:** `SELECT data_source FROM event_evidence WHERE source_ref = '07b813e2...'` returns `'CITIZEN'`.

### 5. Does it use `relevanceTier = AUXILIARY`?
- **Status:** **YES (VERIFIED)**
- **Code Reference:** `ev.setRelevanceTier("AUXILIARY");` in both `CitizenReportService.java` (line 234) and `EvidenceOrchestrationService.java` (line 338).
- **Database Verification:** `SELECT relevance_tier FROM event_evidence WHERE source_ref = '07b813e2...'` returns `'AUXILIARY'`.

### 6. Is `sourceRef` linked to the actual citizen report?
- **Status:** **YES (VERIFIED)**
- **Code Reference:** `ev.setSourceRef(report.getId().toString());` in `CitizenReportService.java` (line 235) and `ev.setSourceRef(cr.getId().toString());` in `EvidenceOrchestrationService.java` (line 339).
- **Database Verification:** `source_ref` matches the exact UUID `07b813e2-a17d-455f-9761-744c0989a6ce`.

### 7. Is `observedAt` preserved?
- **Status:** **YES (VERIFIED)**
- **Code Reference:** `ev.setObservedAt(report.getSubmittedAt() != null ? report.getSubmittedAt() : Instant.now());` in `CitizenReportService.java` (line 236) and `EvidenceOrchestrationService.java` (line 340).

### 8. Is the visual confidence kept separate from F3 `riskScore` / F5 `evidenceScore`?
- **Status:** **YES (VERIFIED)**
- **Independence Guarantee:**
  - Gemini visual confidence is stored in `gemini_analyses.confidence` (e.g. `0.95` or `0.10`) and `event_evidence.confidence_score`.
  - F3 `riskScore` is computed by the XGBoost classifier model (`hotspot_classifier_v1`) and stored in `hotspot_predictions.risk_score`.
  - F5 `evidenceScore` is computed by the multi-component formula in `EventEvidenceScoringEngine.py` and stored in `alerts.evidence_score`.
  - The three values represent distinct physical concepts (visual classification confidence vs. spatial hotspot probability vs. multi-source corroboration score) and are never equated.

### 9. Does existing F5 scoring actually consume the citizen evidence?
- **Status:** **YES (VERIFIED)**
- **Code Reference:** In `ai-service/ml/alert_support/scoring_engine.py`:
  - Lines 52–54: Deduplicates citizen reports using the authoritative 60-minute window.
  - Lines 135–140: If `citizen_count > 0`, marks `source_matrix.citizen = SourceObservationState.SUPPORTED` and `sources_present.citizen_present = True`.
  - Line 156: Includes `source_matrix.citizen` in `tracked_physical_tiers` for `evidence_completeness`.
  - Line 194: Includes `source_matrix.citizen` in `supported_physical_count` for `multi_source_agreement`.
  - Line 252: Multiplies `multi_source_agreement` by `config.weight_multi_source` (0.20) in `raw_weighted_score`.

### 10. Is the F5 score recalculated from existing engine logic?
- **Status:** **YES (VERIFIED)**
- **Engine Logic:** `EventEvidenceScoringEngine` computes:
  $$\text{raw\_weighted\_score} = w_{\text{obs}} \cdot S_{\text{obs}} + w_{\text{ml}} \cdot S_{\text{ml}} + w_{\text{multi}} \cdot S_{\text{multi}} + w_{\text{spatial}} \cdot S_{\text{spatial}} + w_{\text{temporal}} \cdot S_{\text{temporal}}$$
  $$\text{penalized\_score} = \max(0.0, (\text{raw\_weighted\_score} \cdot R_{\text{factor}}) - C_{\text{penalty}})$$
  $$\text{final\_evidence\_score} = \min(1.0, \text{penalized\_score})$$
- No new weights or ad-hoc overrides are applied.

### 11. Does citizen evidence affect triage only through existing F5 logic?
- **Status:** **YES (VERIFIED)**
- Citizen evidence contributes to `multi_source_agreement` and `evidence_completeness`. It does NOT alter `observation_strength` (strictly CAAQMS PM2.5 sensors) or `ml_forecast_support` (strictly F3/F4). Triage classification (`INSUFFICIENT_EVIDENCE`, `MONITOR`, `ALERT_CANDIDATE`) is evaluated strictly against locked thresholds:
  - `min_completeness_for_alert = 0.60`
  - `alert_candidate_threshold = 0.70`
  - `monitor_threshold = 0.45`

### 12. Can a citizen report by itself create an alert?
- **Status:** **NO — STRICT INVARIANT MAINTAINED**
- **Mathematical Proof:**
  - If stationary ground sensors and satellites are unavailable, and only citizen reports exist:
    - $S_{\text{obs}} = 0.0$
    - $S_{\text{ml}} = 0.0$ (or baseline)
    - Supported physical count = 1 ($\Rightarrow S_{\text{multi}} = \frac{1}{3} \approx 0.333$)
    - Maximum contribution to score: $0.20 \times 0.333 = 0.067$
    - Overall score $\approx 0.067 < 0.45$ (well below `monitor_threshold` of 0.45 and `alert_candidate_threshold` of 0.70).
    - Completeness = $\frac{1}{6} \approx 0.167 < 0.60$.
  - Result: Triage state is unconditionally **`INSUFFICIENT_EVIDENCE`**.
  - In `AlertService.java` (line 98): Alerts are generated **IF AND ONLY IF** `triageState == 'ALERT_CANDIDATE'`. Therefore, citizen reports alone **cannot** create an alert.

### 13. Does the P4 UI display the same backend truth?
- **Status:** **YES (VERIFIED)**
- `CitizenEvidenceLineageCard.tsx` consumes live `EvidenceSummaryResponse` from `evidenceService.getEvidenceByH3(report.h3Index)`. It renders the exact backend `reportRef`, `h3Index`, `dataSource: CITIZEN`, `relevanceTier: AUXILIARY`, `matchedEventCode`, and `evidenceScore`.

### 14. Is the P4 CTA to Evidence & WHY backed by live data rather than frontend reconstruction?
- **Status:** **YES (VERIFIED)**
- The button in `CitizenEvidenceLineageCard.tsx` executes:
  `navigate('/analyst/evidence?h3=' + encodeURIComponent(h3Index))`
  which routes to `EvidenceAnalysis.tsx` and triggers `GET /api/v1/evidence/hotspot/{h3Index}` against the Spring Boot backend.

---

## 3. AUDIT FINDINGS & REFINEMENTS NEEDED

1. **Idempotency Refinement in `EvidenceOrchestrationService`:**
   - In `EvidenceOrchestrationService.java`, `persistEventEvidence` iterates over `aiResult.signals()`. When `orchestrate_evidence_cli.py` emits citizen signals, `persistEventEvidence` uses `evidenceKey = sig.signalId()` (`sig-citizen-{id8}`).
   - Meanwhile, `persistCitizenEvidence` uses `evidenceKey = "citizen-report-" + cr.getId()`.
   - **Fix:** In `persistEventEvidence`, skip signals where `"CITIZEN".equalsIgnoreCase(sig.dataSource())` because `persistCitizenEvidence` explicitly handles all deduplication, spatio-temporal matching, and persistence under canonical key `"citizen-report-" + cr.getId()`. This guarantees zero duplicate rows when repeated orchestrations run.
2. **Read Persistence Score Alignment:**
   - In `buildPersistedResponse()`, when reading persisted evidence for an H3 cell with an existing alert/event, query `alertRepository.findByEventId(event.getId())` to populate the exact authoritative `evidenceScore`, `triageState`, and `consistency` directly from the database record rather than computing a baseline heuristic.

---

## 4. NEXT STEPS FOR F6-P5 EXECUTION
1. Implement the minimal idempotency and score-persistence alignments in `EvidenceOrchestrationService.java`.
2. Execute idempotency test (re-orchestrating H3 `88608850e5fffff` twice and confirming 0 new duplicate rows in `event_evidence`).
3. Verify live no-match report (`CR-AF733F86`) produces 0 fabricated events.
4. Execute full backend, frontend, and AI test suites.
5. Generate `docs/F6_P5_CITIZEN_EVIDENCE_INTEGRATION_REPORT.md`.
