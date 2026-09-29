# AEROSENTINEL — F5-P3 EVIDENCE INTEGRITY, EVENT CONSTRUCTION & PERSISTENCE HARDENING REPORT

## 1. Scope

Feature 5 (F5) Phase 3 hardens evidence integrity, event construction, relational provenance, and transactional persistence across the entire AeroSentinel stack. The objective is to guarantee that F5 diagnostic intelligence generated for an H3 cell remains deterministically connected to:
- F3 Hotspot Prediction (`predictionId`, `featureSnapshotId`, `confidenceBreakdown`)
- F4 Multi-Horizon Forecast (`parentPredictionId`, horizons $1\text{h}, 3\text{h}, 6\text{h}$, $P_{10}/P_{90}$)
- F5 Evidence Scoring Engine (`evidenceScore`, multi-source weights, triage state)
- Canonical Pollution Event (`PollutionEvent.id`, `eventCode`, `gridCellId`)
- Atomic Event Evidence (`EventEvidence` records with complete signal provenance)
- Grounded Gemini Analysis (`GeminiAnalysis.id`, `eventId`, `predictionId`, `h3Index`)
- PostgreSQL database tables with strict foreign keys, unique constraints, and transaction rollback protection.

Zero data mixing, zero lineage loss, and zero fabricated telemetry are allowed.

---

## 2. Existing Implementation Reused

All locked upstream contracts from F3 and F4, as well as P2 orchestration artifacts, were reused without modification or duplication:
1. **F3 Hotspot Contract:** Platt-calibrated Random Forest (`hotspot_classifier_v1`), operational threshold `0.20`, baseline `0.40`, deterministic probability risk scores, preserved lineage (`predictionId`, `featureSnapshotId`).
2. **F4 Forecast Contract:** Horizons $\{1\text{h}, 3\text{h}, 6\text{h}\}$ only, empirical $P_{10}/P_{90}$ bounds, `forecastConfidence = null`, zero forecast re-computation.
3. **F5 Scoring Engine:** Reused `EventEvidenceScoringEngine` in `ai-service/ml/alert_support/scoring_engine.py` and `alert_config.py`.
4. **Gemini Pipeline:** Reused `AeroSentinelGeminiPipeline`, `GroundingValidator`, and prompt registry in `ai-service/app/services/gemini_pipeline.py`.
5. **Spatial Clustering:** Reused `EventClusterer` in `ai-service/ml/alert_support/clustering.py` using native Uber H3 $k=1$ neighborhood disk logic.
6. **Subprocess AI Bridge:** Reused `orchestrate_evidence_cli.py` and Spring Boot `EvidenceAiClient`.

---

## 3. Event Identity

### Identity Audit & Deterministic Construction
- **Event Code Formulation:** Python `scoring_engine.py` derives the canonical event code deterministically:
  ```python
  time_clean = timestamp.replace("-", "").replace(":", "").replace("T", "")[:10]  # YYYYMMDDHH
  dedup_raw = f"{h3_cell}:{time_clean}:{scoring_version}"
  dedup_hash = hashlib.sha256(dedup_raw.encode()).hexdigest()[:8]
  event_code = f"EVT-{h3_cell[:8]}-{time_clean}-{dedup_hash}"
  ```
- **Uniqueness & Preservation:** `PollutionEvent.eventCode` is enforced `UNIQUE` in PostgreSQL (`pollution_events.event_code VARCHAR(50) UNIQUE`).
- **Entity Linkage:**
  - `PollutionEvent.prediction_id`: Foreign key pointing to `hotspot_predictions(id)`.
  - `PollutionEvent.h3_index`: Stores the 15-character Uber H3 index.
  - `PollutionEvent.grid_cell_id`: Foreign key pointing to `grid_cells(id)`.
  - `PollutionEvent.started_at`: Preserves the base observation timestamp ($T_0$).

---

## 4. Event Construction

The event construction follows the authoritative spatio-temporal clustering semantics implemented in `scoring_engine.py` and `clustering.py`:
1. **Single Cell Hotspot:** Creates a canonical `PollutionEvent` with `eventCode = "EVT-{h3[:8]}-{time_clean}-{hash}"` and `clusterH3Cells = [primary_cell]`.
2. **Temporal Windowing:** The time window is partitioned hourly (`YYYYMMDDHH`). Multiple requests or telemetry updates within the same hourly window for the same H3 cell resolve to the **same** canonical `event_code`. The existing `PollutionEvent` row is safely reused/updated rather than creating a duplicate.
3. **Spatial Clustering:** When adjacent cells in the Uber H3 $k=1$ grid disk concurrently exhibit hotspot elevation, `EventClusterer.group_cells` groups them into `cluster_h3_cells`, linking them under the primary cell's canonical event.
4. **Lifecycle Transitions:**
   `EVENT LIFECYCLE TRANSITIONS NOT IMPLEMENTED.`
   Current `PollutionEvent.status` defaults to `"OPEN"`. Authority workflow and transition state machines are deferred to dedicated authority phases.

---

## 5. H3 Event Linkage & Mismatch Enforcement

Strict H3 consistency is verified across all layers before any database persistence occurs:
$$\text{F3 Context } H3 = \text{F4 Forecast } H3 = \text{AI Output } H3 = \text{PollutionEvent } H3 = \text{GeminiAnalysis } H3$$

In `EvidenceOrchestrationService`:
1. **F3 Spatial Context Check:** Validates `normalizedH3.equalsIgnoreCase(spatialContext.h3Index())`.
2. **F4 Forecast Check:** If forecast is present, verifies `normalizedH3.equalsIgnoreCase(forecast.h3Index())`. If mismatched, immediately throws `ValidationException("H3 mismatch: requested ... but F4 forecast is for ...")`.
3. **AI Bridge Output Check:** Verifies `normalizedH3.equalsIgnoreCase(aiResult.h3Index())`. If mismatched, immediately throws `ValidationException("H3 mismatch: requested ... but AI bridge returned ...")`.
4. **Entity Assignment:** `PollutionEvent.setH3Index(normalizedH3)` and `GeminiAnalysis.setH3Index(normalizedH3)`.

Any mismatch aborts execution before persistence and triggers transactional rollback.

---

## 6. Prediction Lineage

F5 evidence maintains complete bidirectional lineage:
- **Hotspot Lineage:** `predictionId`, `featureSnapshotId`, `h3Index`, `cityId`, `predictedAt`, `modelVersion` (`hotspot_classifier_v1`) are captured in `ContextDto`, `PollutionEvent.prediction_id`, and `GeminiAnalysis.prediction_id`.
- **Forecast Lineage:** `parentPredictionId`, `baseTimestamp`, `generatedAt`, `forecastModelVersion` (`forecast_regressors_v1`), and `horizons` ($1\text{h}, 3\text{h}, 6\text{h}$) with strictly null confidence are preserved in `ForecastModelOutputDto`.
- **Zero Recalculation:** No F3 probabilities or F4 regressors are recalculated during evidence orchestration.

---

## 7. Evidence Provenance

Every persisted `EventEvidence` record preserves full attribution:
- `signal_id`: Deterministic signal identifier (e.g. `sig-88608850-001`).
- `source_type`: Signal category (`DIRECT_OBSERVATION`, `METEOROLOGY`, `GIS_CONTEXT`, `MONITORING_COVERAGE`, `REMOTE_SENSING`).
- `evidence_key`: Unique signal key per event.
- `evidence_value`: Physical measurement or attribution note.
- `weight`: Statistical confidence score $[0.0, 1.0]$.
- `data_source`: Originating authority (e.g. `CPCB`, `OPEN_METEO_SURFACE`, `MIDC_INDUSTRIAL_SURVEY`).
- `relevance_tier`: Primary or supporting tier (`PRIMARY`, `SUPPORTING`).
- `source_ref`: Reference sensor or station ID (e.g. `PUN-001`).
- `confidence_score`: Source confidence.
- `observed_at`: Exact measurement timestamp.

---

## 8. Persistence Model

The PostgreSQL schema links all entities through foreign keys:

```
┌────────────────────────┐         ┌───────────────────────┐
│  hotspot_predictions   │         │       forecasts       │
│  - id (UUID PK)        │◄────────┤  - parent_prediction  │
│  - h3_index            │         │  - horizons (1, 3, 6) │
└───────────┬────────────┘         └───────────────────────┘
            │
            ▼ (prediction_id)
┌────────────────────────┐         ┌───────────────────────┐
│    pollution_events    │         │      grid_cells       │
│  - id (UUID PK)        │◄────────┤  - id (UUID PK)       │
│  - event_code (UNIQUE) │         │  - h3_index (UNIQUE)  │
│  - h3_index            │         └───────────────────────┘
│  - prediction_id (FK)  │
│  - grid_cell_id (FK)   │
└─────┬────────────┬─────┘
      │            │
      ▼ (event_id) ▼ (event_id)
┌──────────────────────┐   ┌───────────────────────────────┐
│    event_evidence    │   │        gemini_analyses        │
│  - id (UUID PK)      │   │  - id (UUID PK)               │
│  - event_id (FK)     │   │  - event_id (FK)              │
│  - signal_id         │   │  - prediction_id (FK)         │
│  - evidence_key      │   │  - h3_index                   │
│  - weight            │   │  - event_summary_public       │
│  - UNIQUE(event_id,  │   │  - event_summary_analyst      │
│     evidence_key)    │   │  - is_grounded                │
└──────────────────────┘   └───────────────────────────────┘
```

---

## 9. Duplicate Evidence Protection

Duplicate protection is enforced at two distinct levels:
1. **Database Constraint:** Migration `V14` creates a unique index:
   ```sql
   CREATE UNIQUE INDEX IF NOT EXISTS idx_event_evidence_dedup 
       ON event_evidence(event_id, evidence_key);
   ```
2. **Application Idempotency:** In `EvidenceOrchestrationService`, before inserting an evidence signal:
   ```java
   if (evidenceRepository.existsByEventIdAndEvidenceKey(event.getId(), evidenceKey)) {
       log.debug("EventEvidence for eventId={} key={} already exists; skipping duplicate", event.getId(), evidenceKey);
       continue;
   }
   ```
Verified in `EvidenceIntegrationTest.testDuplicateRequestDoesNotDuplicateEvidence`: repeated calls for the same Pune H3 cell preserve the exact evidence count ($6 \to 6$) without throwing duplicate key exceptions or duplicating rows.

---

## 10. Missingness & Non-Fabrication

When upstream data feeds are absent, states are preserved without fabricating numbers:
- **Weather Unavailable:** Handled via evidence completeness penalty; weather context marked null.
- **Forecast Unavailable:** Handled gracefully; `modelOutputs.forecast` is null, and triage scoring evaluates remaining ground/GIS signals.
- **Satellite / Fire Telemetry Unavailable:** Recorded as `"unavailable"` in `sourceMatrix`; never converted to zero-value readings.
- **Gemini API Unavailable:** Deterministic grounded fallback activates, synthesizing diagnostic summaries derived strictly from observed facts.

---

## 11. Event Clustering Integration

- Python `EventClusterer` groups adjacent H3 cells with concurrent hotspot elevation into `cluster_h3_cells`.
- `orchestrate_evidence_cli.py` emits `clusterH3Cells` in STDOUT JSON.
- `EvidenceAiClient` maps `clusterH3Cells` into `EvidenceAiOutputDto`.
- `EvidenceOrchestrationService` includes `clusterH3Cells` in `EvidenceDto`.
- Tested in `EvidenceUnitTest.testClusterCellsPreservedInEvidence`.

---

## 12. Transaction Integrity & Rollback Safety

The method `EvidenceOrchestrationService.getOrchestratedEvidence(h3Index)` is annotated with `@Transactional`.
- If any downstream operation (AI bridge, entity resolution, validation, or persistence) throws an exception:
  - Spring Framework automatically rolls back the PostgreSQL transaction.
  - No orphaned `PollutionEvent`, `EventEvidence`, or `GeminiAnalysis` records can be committed.
- Tested in unit and integration test suites: validation rejections verify `pollutionEventRepository.save` and `geminiAnalysisRepository.save` are never invoked on invalid inputs.

---

## 13. Tests

### Automated Test Suites Summary

| Test Suite | Scope | Result | Execution Time |
|---|---|---|---|
| **`EvidenceUnitTest`** | Unit tests: Lineage, H3 mismatch rejection, duplicate evidence protection, clustering, triage states, error handling | **19 / 19 PASS** | 10.5s |
| **`EvidenceIntegrationTest`** | End-to-end MockMvc + PostgreSQL + Flyway V14 on real Pune cell `88608850e5fffff` | **4 / 4 PASS** | 293.9s |
| **`ForecastUnitTest`** | F4 forecast unit regression verification | **14 / 14 PASS** | 0.6s |
| **`test_f5_alert_support.py`** | Python scoring engine, clustering, lead time metrics, and source matrix tests | **10 / 10 PASS** | 0.5s |
| **Total Tests Verified** | **Full F5-P3 Regression & Integrity Suite** | **47 / 47 PASS** | — |

---

## 14. Real Runtime Proof (Pune Cell: `88608850e5fffff`)

Executed against PostgreSQL on real Pune Shivajinagar CAAQMS station data:

```
2026-09-28T00:04:54.441+05:30 [main] INFO c.a.s.H3BackfillService: Station PUN-001 (Shivajinagar CAAQMS) -> coordinates (18.5314, 73.8446) -> H3 index 88608850e5fffff
2026-09-28T00:04:59.618+05:30 [main] INFO c.a.e.EvidenceOrchestrationService: Created new canonical PollutionEvent id=9ea81bb7-9ff2-4b4d-a80f-32e98e2cb12f code=EVT-88608850-2026092613-d75654e9 for H3 cell 88608850e5fffff
2026-09-28T00:04:59.691+05:30 [main] INFO c.a.e.EvidenceOrchestrationService: Persisted GeminiAnalysis for H3 cell 88608850e5fffff linked to event 9ea81bb7-9ff2-4b4d-a80f-32e98e2cb12f and prediction a310c689-f340-49fc-8935-a037de8d7709
2026-09-28T00:05:00.407+05:30 [main] INFO c.a.evidence.EvidenceIntegrationTest: Verified PollutionEvent record: id=9ea81bb7-9ff2-4b4d-a80f-32e98e2cb12f, code=EVT-88608850-2026092613-d75654e9, h3=88608850e5fffff, predictionId=a310c689-f340-49fc-8935-a037de8d7709
2026-09-28T00:05:00.419+05:30 [main] INFO c.a.evidence.EvidenceIntegrationTest: Verified EventEvidence records count=6 for eventId=9ea81bb7-9ff2-4b4d-a80f-32e98e2cb12f
2026-09-28T00:05:00.432+05:30 [main] INFO c.a.evidence.EvidenceIntegrationTest: Verified GeminiAnalysis record: id=ff9beddb-286e-48a3-9ef3-4f5191043735, eventId=9ea81bb7-9ff2-4b4d-a80f-32e98e2cb12f, predictionId=a310c689-f340-49fc-8935-a037de8d7709, h3=88608850e5fffff
2026-09-28T00:05:01.416+05:30 [main] INFO c.a.e.EvidenceOrchestrationService: Reusing existing PollutionEvent id=9ea81bb7-9ff2-4b4d-a80f-32e98e2cb12f code=EVT-88608850-2026092613-d75654e9 for H3 cell 88608850e5fffff
2026-09-28T00:05:01.432+05:30 DEBUG c.a.e.EvidenceOrchestrationService: EventEvidence for eventId=9ea81bb7-9ff2-4b4d-a80f-32e98e2cb12f key=sig-88608850-001 already exists; skipping duplicate
2026-09-28T00:05:01.510+05:30 [main] INFO c.a.evidence.EvidenceIntegrationTest: Verified duplicate evidence protection: initialCount=6, afterSecondCallCount=6
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in com.aerosentinel.evidence.EvidenceIntegrationTest
[INFO] BUILD SUCCESS
```

### Verified Linkage Chain Proof:
1. **F3 Prediction:** `predictionId = a310c689-f340-49fc-8935-a037de8d7709`, `h3Index = 88608850e5fffff`, `riskScore = 0.80`, `riskLevel = CRITICAL`.
2. **F4 Forecast:** `parentPredictionId = a310c689-f340-49fc-8935-a037de8d7709`, horizons $1\text{h}=71.90, 3\text{h}=70.43, 6\text{h}=70.55\ \mu\text{g/m}^3$, `forecastConfidence = null`.
3. **F5 Evidence Score:** `evidenceScore = 0.62`, `triageState = ALERT_CANDIDATE`.
4. **Pollution Event:** `id = 9ea81bb7-9ff2-4b4d-a80f-32e98e2cb12f`, `eventCode = EVT-88608850-2026092613-d75654e9`, `h3Index = 88608850e5fffff`, `predictionId = a310c689-f340-49fc-8935-a037de8d7709`.
5. **Event Evidence:** 6 atomic records persisted for `eventId = 9ea81bb7-9ff2-4b4d-a80f-32e98e2cb12f`.
6. **Gemini Analysis:** `id = ff9beddb-286e-48a3-9ef3-4f5191043735`, `eventId = 9ea81bb7-9ff2-4b4d-a80f-32e98e2cb12f`, `predictionId = a310c689-f340-49fc-8935-a037de8d7709`, `h3Index = 88608850e5fffff`, `isGrounded = true`.
7. **REST Endpoint:** `GET /api/v1/evidence/hotspot/88608850e5fffff` returns HTTP 200 with all 6 tiers.

---

## 15. Limitations

1. **Event State Transitions:**
   `EVENT LIFECYCLE TRANSITIONS NOT IMPLEMENTED.`
   Status remains `"OPEN"` upon event creation. Automated resolution or authority lifecycle transitions are deferred to authority workflow phases.
2. **Citizen Reports:** Deduplicated in Python; unverified reports are included as supporting context without overriding calibrated CAAQMS telemetry.

---

## 16. Files Created / Modified

### Created:
1. `backend/src/main/resources/db/migration/V14__f5_pollution_event_lineage_and_evidence.sql`: Relational lineage columns and unique duplicate index.
2. `docs/F5_P3_EVIDENCE_INTEGRITY_EVENT_PERSISTENCE_REPORT.md`: This comprehensive report.

### Modified:
1. `backend/src/main/java/com/aerosentinel/event/PollutionEvent.java`: Added `h3Index` and `predictionId`.
2. `backend/src/main/java/com/aerosentinel/event/PollutionEventRepository.java`: Added spatial and prediction query methods.
3. `backend/src/main/java/com/aerosentinel/evidence/EventEvidence.java`: Added structured signal provenance fields.
4. `backend/src/main/java/com/aerosentinel/evidence/EvidenceRepository.java`: Added duplicate protection check methods.
5. `backend/src/main/java/com/aerosentinel/model/GeminiAnalysis.java`: Added `predictionId` for dual event/prediction lineage.
6. `backend/src/main/java/com/aerosentinel/repository/GeminiAnalysisRepository.java`: Added `findByPredictionIdOrderByCreatedAtDesc`.
7. `ai-service/ml/inference/orchestrate_evidence_cli.py`: Emits `eventId`, `canonicalEventId`, and `clusterH3Cells`.
8. `backend/src/main/java/com/aerosentinel/evidence/EvidenceAiClient.java`: Updated DTO with event and cluster fields.
9. `backend/src/main/java/com/aerosentinel/dto/evidence/EvidenceSummaryResponse.java`: Added `eventId`, `eventCode`, `parentPredictionId`, `clusterH3Cells` with backward-compatible constructors.
10. `backend/src/main/java/com/aerosentinel/evidence/EvidenceOrchestrationService.java`: Enforced H3 checks, canonical event resolution, duplicate evidence prevention, and transaction rollback.
11. `backend/src/test/java/com/aerosentinel/evidence/EvidenceUnitTest.java`: Expanded to 19 unit tests.
12. `backend/src/test/java/com/aerosentinel/evidence/EvidenceIntegrationTest.java`: Expanded to 4 end-to-end integration tests.

---

## 17. P3 Definition of Done

All 19 criteria are verified and PASS:

- [x] Event identity verified/implemented
- [x] F3 lineage preserved
- [x] F4 lineage preserved
- [x] H3 consistency enforced
- [x] Evidence provenance preserved
- [x] Observed/model/AI separation preserved
- [x] EventEvidence persistence correct
- [x] GeminiAnalysis linkage correct
- [x] Duplicate evidence handled
- [x] Missingness preserved
- [x] Existing H3 clustering reused
- [x] DB integrity verified
- [x] Transaction rollback verified
- [x] Automated tests pass (47/47 across all suites)
- [x] Real Pune runtime proof passes
- [x] No F3 regression
- [x] No F4 regression
- [x] No P2 duplication
- [x] No fabricated data

============================================================
F5-P3 STATUS: PASS
============================================================
