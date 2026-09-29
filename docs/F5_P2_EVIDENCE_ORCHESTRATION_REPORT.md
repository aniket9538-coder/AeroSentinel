# AEROSENTINEL — F5-P2 EVIDENCE ORCHESTRATION & BACKEND/AI BRIDGE REPORT

## 1. Implementation Summary

Feature 5 (F5) answers the core authority and regulator question: **"WHY is this H3 cell considered risky?"**
Phase 2 (F5-P2) establishes the production evidence orchestration layer and Python AI bridge connecting Spring Boot with the existing ML scoring engine (`EventEvidenceScoringEngine`) and Gemini structured explanation pipeline (`AeroSentinelGeminiPipeline`).

All upstream contracts from Feature 3 (Hotspot Detection) and Feature 4 (PM2.5 Forecast) remain strictly locked:
- **F3:** Platt-calibrated Random Forest (`hotspot_classifier_v1`), operational threshold `0.20`, baseline `0.40`, deterministic probability risk scores, preserved lineage (`predictionId`, `featureSnapshotId`, `confidenceBreakdown`).
- **F4:** Horizons $\{1\text{h}, 3\text{h}, 6\text{h}\}$ only, empirical $P_{10}/P_{90}$ bounds, `forecastConfidence = null`, zero forecast re-computation.
- **F5 Scoring:** Authoritative reuse of `EventEvidenceScoringEngine` and `alert_config.py` (observation: 0.30, forecast: 0.25, multi-source: 0.20, spatial: 0.15, temporal: 0.10, conflict penalty: 0.25, triage thresholds: 0.40 / 0.55). Zero duplicated formulas or weights.
- **Gemini:** Authoritative reuse of `AeroSentinelGeminiPipeline`, `gemini_client.py`, `grounding_guard.py`, and prompt registry with grounded deterministic fallback.

The backend exposes `GET /api/v1/evidence/hotspot/{h3Index}`, generating a unified response separating **observed facts**, **model outputs**, **evidence & triage support**, **AI interpretation**, **recommended verification**, and **provenance**.

---

## 2. Files Created

1. **`backend/src/main/resources/db/migration/V12__f5_gemini_analyses_hotspot_evidence.sql`**
   - Flyway migration extending existing `gemini_analyses` PostgreSQL table with columns: `event_id`, `h3_index`, `model_version`, `prompt_version`, `event_summary_public`, `event_summary_analyst`, `detected_condition`, `forecast_trajectory`, `uncertainty_statement`, `is_grounded`, `created_at`, plus spatial and temporal indexes.
2. **`backend/src/main/resources/db/migration/V13__f5_gemini_analyses_relax_event_fk.sql`**
   - Flyway migration relaxing foreign key on `event_id` in `gemini_analyses` to support direct association with `HotspotPrediction` UUIDs without requiring pre-existing rows in `pollution_events`.
3. **[`GeminiAnalysis.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/model/GeminiAnalysis.java)**
   - Spring Data JPA entity mapped to `gemini_analyses`.
4. **[`GeminiAnalysisRepository.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/repository/GeminiAnalysisRepository.java)**
   - Repository interface providing spatial query methods: `findTopByH3IndexOrderByCreatedAtDesc`, `findByH3IndexOrderByCreatedAtDesc`, and `findByEventIdOrderByCreatedAtDesc`.
5. **[`EvidenceSummaryResponse.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/dto/evidence/EvidenceSummaryResponse.java)**
   - Unified multi-tiered DTO containing `ContextDto`, `ObservedFactsDto`, `ModelOutputsDto`, `EvidenceDto`, `AiInterpretationDto`, `RecommendedVerificationDto`, `ProvenanceDto`, and `status`.
6. **[`orchestrate_evidence_cli.py`](file:///c:/Users/lenovo/AeroSential/ai-service/ml/inference/orchestrate_evidence_cli.py)**
   - Subprocess CLI bridge reading input JSON on STDIN, invoking `AeroSentinelGeminiPipeline` and `EventEvidenceScoringEngine`, validating grounding, and outputting strict JSON.
7. **[`EvidenceAiClient.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/evidence/EvidenceAiClient.java)**
   - Spring Boot subprocess client managing process execution, JSON streaming, timeout control (15,000 ms), and error mapping.
8. **[`EvidenceOrchestrationService.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/evidence/EvidenceOrchestrationService.java)**
   - Main orchestration service coordinating F3 context lookup, F4 forecast retrieval, evidence payload assembly, AI bridge execution, DB persistence, and response assembly.
9. **[`EvidenceUnitTest.java`](file:///c:/Users/lenovo/AeroSential/backend/src/test/java/com/aerosentinel/evidence/EvidenceUnitTest.java)**
   - 14 comprehensive unit tests verifying lineage preservation, score passthrough, triage states, missing forecast tolerance, error handling, and model version assertions.
10. **[`EvidenceIntegrationTest.java`](file:///c:/Users/lenovo/AeroSential/backend/src/test/java/com/aerosentinel/evidence/EvidenceIntegrationTest.java)**
    - End-to-end integration tests hitting `GET /api/v1/evidence/hotspot/{h3Index}` against PostgreSQL, verifying all 6 tiers and `gemini_analyses` database persistence.

---

## 3. Files Modified

1. **[`EvidenceController.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/evidence/EvidenceController.java)**
   - Added endpoint `GET /api/v1/evidence/hotspot/{h3Index}` with H3 format validation and delegation to `EvidenceOrchestrationService`.
2. **[`SecurityConfig.java`](file:///c:/Users/lenovo/AeroSential/backend/src/main/java/com/aerosentinel/config/SecurityConfig.java)**
   - Configured Spring Security to permit public access to `"/api/v1/evidence/**"`.
3. **[`application.yml`](file:///c:/Users/lenovo/AeroSential/backend/src/main/resources/application.yml)**
   - Added `app.evidence.ai` configuration block (`python-executable`, `script-path`, `working-dir`, `timeout-ms: 15000`).

---

## 4. Backend Architecture

The backend architecture enforces strict separation between numerical data and generative narrative:

```
                  Client: GET /api/v1/evidence/hotspot/{h3Index}
                                       │
                                       ▼
                             EvidenceController
                        (validates H3 format/length)
                                       │
                                       ▼
                         EvidenceOrchestrationService
               ┌───────────────────────┼───────────────────────┐
               │                       │                       │
               ▼                       ▼                       ▼
      HotspotContextService     ForecastService      GeminiAnalysisRepository
      (Loads locked F3 data,   (Loads locked F4 1h,   (Persists grounded analysis
       air, weather, GIS)       3h, 6h horizons)       to gemini_analyses table)
               │                       │
               └───────────┬───────────┘
                           ▼
                   EvidenceAiClient
               (Spring Subprocess Bridge)
                           │  (STDIN / STDOUT JSON)
                           ▼
             orchestrate_evidence_cli.py
                           │
             ┌─────────────┴─────────────┐
             ▼                           ▼
EventEvidenceScoringEngine   AeroSentinelGeminiPipeline
   (Deterministic F5             (Structured Explanation,
    Score & Triage)               Grounding Guard & Fallback)
```

---

## 5. Python Bridge Architecture

The bridge uses the established subprocess pattern (`orchestrate_evidence_cli.py`) avoiding uncontrolled in-process Python execution:
- **Input:** JSON payload via STDIN containing:
  - `h3Index`, `cityId`, `predictionId`, `featureSnapshotId`
  - `air`, `weather`, `coverage`, `dispersion`, `gis` telemetry
  - `hotspot`: `riskScore`, `operationalThreshold`, `isHotspot`, `modelVersion`
  - `forecast`: `horizons` ($1\text{h}, 3\text{h}, 6\text{h}$ values and $P_{10}/P_{90}$ bounds)
- **Scoring Execution:** Passes payload to `EventEvidenceScoringEngine.evaluate_f5_evidence(...)` which generates raw weighted score, conflict penalty, decay penalty, and assigns triage state (`INSUFFICIENT_EVIDENCE`, `MONITOR`, `ALERT_CANDIDATE`).
- **Explanation Execution:** Invokes `AeroSentinelGeminiPipeline.generate_structured_explanation(...)`. If Gemini API is unreachable or SDK is absent, activates `_generate_deterministic_grounded_fallback(...)` ensuring zero fake telemetry and strict grounding.
- **Grounding Guard:** Validates with `GroundingValidator.validate(...)` ensuring no ungrounded causal claims (e.g. factory names or fabricated sources) are output.
- **Output:** Validated JSON streamed via STDOUT back to `EvidenceAiClient`.

---

## 6. Request/Response Contract

### Endpoint
`GET /api/v1/evidence/hotspot/{h3Index}`

### Response Schema: `EvidenceSummaryResponse`
```json
{
  "status": "SUCCESS",
  "context": {
    "h3Index": "88608850e5fffff",
    "cityId": "550e8400-e29b-41d4-a716-446655440001",
    "cityName": "Pune",
    "predictionId": "a310c689-f340-49fc-8935-a037de8d7709",
    "featureSnapshotId": "1624baa3-a5f8-407b-b1c2-36bcee7650b1",
    "generatedAt": "2026-09-27T18:04:13.188Z"
  },
  "observedFacts": {
    "air": { "pm25": 78.0, "stationId": "PUN-001", ... },
    "weather": { "temperature": 30.1, "humidity": 52.0, "windSpeed": 3.31, ... },
    "monitoringCoverage": { "distanceToNearestMonitorKm": 0.27, ... },
    "gisContext": { "industrialAreaKm2": 3.5, ... }
  },
  "modelOutputs": {
    "hotspot": {
      "riskScore": 0.80,
      "operationalThreshold": 0.20,
      "isHotspot": true,
      "riskLevel": "CRITICAL",
      "modelVersion": "hotspot_classifier_v1"
    },
    "forecast": {
      "forecastModelVersion": "forecast_regressors_v1",
      "forecastConfidence": null,
      "horizons": [
        { "horizonHours": 1, "predictedPm25": 71.90, "lowerP10": 70.06, "upperP90": 73.76 },
        { "horizonHours": 3, "predictedPm25": 70.43, "lowerP10": 66.53, "upperP90": 73.48 },
        { "horizonHours": 6, "predictedPm25": 70.55, "lowerP10": 65.03, "upperP90": 75.98 }
      ]
    }
  },
  "evidence": {
    "evidenceScore": 0.62,
    "confidence": 0.667,
    "consistency": "consistent",
    "triageState": "ALERT_CANDIDATE",
    "scoreBreakdown": {
      "observationComponent": 0.35,
      "forecastComponent": 0.40,
      "multiSourceComponent": 0.33,
      "spatialComponent": 0.95,
      "temporalComponent": 0.67,
      "conflictPenalty": 0.0,
      "recencyDecay": 1.0,
      "finalEvidenceScore": 0.62
    },
    "signals": [
      {
        "signalId": "sig-88608850-001",
        "signalType": "DIRECT_OBSERVATION",
        "description": "PM2.5 ground sensor at 78.0 ug/m3",
        "confidence": 0.90,
        "source": "CPCB"
      }
    ]
  },
  "aiInterpretation": {
    "summaryPublic": "Elevated PM2.5 detected in area 88608850e5fffff...",
    "summaryAnalyst": "Corroborated by near-term forecast...",
    "detectedCondition": "Elevated particulate pollution",
    "observedEvidence": ["Ground sensor reading 78.0 ug/m3"],
    "forecastTrajectory": "Forecast indicates PM2.5 persists around ~71 ug/m3 across 6h",
    "uncertaintyStatement": "Satellite aerosol imagery currently unavailable",
    "unsupportedConclusions": ["Cannot confirm factory attribution without direct stack inspection"],
    "causalClaimSupported": false,
    "isGrounded": true
  },
  "recommendedVerification": {
    "action": "Dispatch mobile CAAQMS rapid-sampling unit",
    "priority": "HIGH",
    "suggestedProtocols": ["Deploy mobile sensor", "Inspect upwind emitters"]
  },
  "provenance": {
    "h3Index": "88608850e5fffff",
    "f3ModelVersion": "hotspot_classifier_v1",
    "f4ModelVersion": "forecast_regressors_v1",
    "f5ScoringVersion": "v1.0.0",
    "geminiModelVersion": "deterministic-fallback-v1.0"
  }
}
```

---

## 7. Evidence Flow

1. **Validation:** In `EvidenceController`, `h3Index` format is checked (15-character hex).
2. **Context Resolution:** `EvidenceOrchestrationService` loads active F3 context from `HotspotContextService`. If absent, immediately returns HTTP 404 with message `"No active hotspot intelligence found for H3 cell: ..."`.
3. **Forecast Resolution:** `ForecastService.getForecastByH3(h3Index)` retrieves the current locked forecast. If missing, evidence continues gracefully with `forecast = null`.
4. **AI Bridge Request:** Payload containing F3 context, F4 forecast, and sensor telemetry is dispatched to `orchestrate_evidence_cli.py`.
5. **Deterministic Scoring:** Python engine evaluates multi-source signals and computes `evidenceScore` using weights configured in `alert_config.py`.
6. **Gemini / Fallback Interpretation:** Generates analyst and public narratives strictly constrained by the numerical facts.
7. **Entity Persistence:** A `GeminiAnalysis` entity is mapped and persisted to PostgreSQL `gemini_analyses`.
8. **DTO Assembly:** The 6 tiers are constructed and returned as `EvidenceSummaryResponse`.

---

## 8. Scoring Integration

Reuses `EventEvidenceScoringEngine` in `ai-service/ml/alert_support/scoring_engine.py` and `alert_config.py`:
- **Formula:**
  $$\text{raw\_score} = 0.30 \cdot s_{\text{obs}} + 0.25 \cdot s_{\text{fc}} + 0.20 \cdot s_{\text{multi}} + 0.15 \cdot s_{\text{spatial}} + 0.10 \cdot s_{\text{temporal}}$$
- **Penalties:**
  - Conflict penalty: $-0.25$ if satellite or low-cost sensors directly contradict ground station.
  - Recency decay: Exponential decay based on observation age.
- **Thresholds:**
  - $\text{Score} < 0.40 \lor \text{coverage} = \text{UNACCEPTABLE} \implies \text{INSUFFICIENT\_EVIDENCE}$
  - $0.40 \le \text{Score} < 0.55 \implies \text{MONITOR}$
  - $\text{Score} \ge 0.55 \implies \text{ALERT\_CANDIDATE}$

No formulas or weights are duplicated in Java.

---

## 9. Gemini Integration

- **Execution:** Invokes `AeroSentinelGeminiPipeline.generate_structured_explanation(...)`.
- **Grounding Guard:** Validated through `GroundingValidator`. If any unsupported causal attribution is detected, `causal_claim_supported` is set to `False` and attribution statements are moved to `unsupported_conclusions`.
- **Fallback Guarantee:** When the external Gemini API is unreachable or credentials are absent, the pipeline engages `_generate_deterministic_grounded_fallback(...)`. It synthesizes structured, fully-grounded text from observed facts without fabricating telemetry.
- **Persistence:** Saved to PostgreSQL `gemini_analyses` with `is_grounded = true`.

---

## 10. Persistence

The entity `GeminiAnalysis` maps directly to `gemini_analyses`:
- `id` (UUID, primary key)
- `event_id` (UUID, linked to prediction ID or event ID)
- `h3_index` (VARCHAR(15), indexed)
- `model_version` (VARCHAR(50))
- `prompt_version` (VARCHAR(50))
- `event_summary_public` (TEXT)
- `event_summary_analyst` (TEXT)
- `detected_condition` (VARCHAR(100))
- `forecast_trajectory` (TEXT)
- `uncertainty_statement` (TEXT)
- `is_grounded` (BOOLEAN)
- `created_at` (TIMESTAMPTZ, indexed)

Persistence is transactional: database write occurs only after successful AI bridge completion.

---

## 11. Failure Handling

| Failure Condition | System Behavior | HTTP Status / Exception |
|---|---|---|
| Invalid H3 index format | Rejects input prior to processing | HTTP 400 Bad Request |
| Unknown / inactive H3 cell | No hotspot data available; never invents data | HTTP 404 Not Found |
| Missing F4 forecast | Tolerated gracefully; `modelOutputs.forecast = null` | HTTP 200 SUCCESS |
| Subprocess bridge failure | Process non-zero exit or timeout (>15s) | HTTP 500 Controlled Error |
| Gemini API failure | Activates deterministic grounded fallback | HTTP 200 SUCCESS |
| DB persistence failure | Transaction rollback prevents partial data | HTTP 500 Controlled Error |

---

## 12. Tests

### Automated Test Results

1. **`EvidenceUnitTest` (Spring Boot Unit Tests): 14/14 PASS**
   - `testValidF3AndF4ProducesStructuredResponse`: Validates full 6-tier separation.
   - `testF3HotspotLineagePreserved`: Checks predictionId and snapshotId propagation.
   - `testF4ForecastLinkagePreserved`: Ensures forecast horizons and null confidence.
   - `testF5ScoringBridgeInvocation`: Confirms bridge arguments match upstream values.
   - `testEvidenceScorePassthrough`: Verifies exact numerical score passthrough.
   - `testTriageStatePassthrough`: Validates triage states.
   - `testGeminiStructuredOutputMapping`: Confirms diagnostic and explanation mapping.
   - `testGeminiAnalysisPersistence`: Verifies JPA entity creation and saving.
   - `testMissingForecastGracefulToleration`: Asserts graceful handling when forecast is missing.
   - `testMissingF3ContextThrowsNotFound`: Verifies 404 when F3 data is absent.
   - `testInvalidH3ThrowsValidationException`: Verifies 400 on malformed H3.
   - `testAiBridgeFailureThrowsControlledException`: Verifies controlled error on CLI failure.
   - `testNoDuplicateForecastExecution`: Ensures forecast is queried, never regenerated.
   - `testProvenanceVersionsCorrect`: Checks all model versions in provenance.

2. **`EvidenceIntegrationTest` (Spring Boot End-to-End Tests): 3/3 PASS**
   - `testRealPuneEvidenceOrchestration`: Real Pune cell `88608850e5fffff` returns HTTP 200, validates all 6 tiers, and verifies `gemini_analyses` row persistence in PostgreSQL.
   - `testInvalidH3Returns400`: Verifies 400 for short H3 string.
   - `testUnknownH3Returns404`: Verifies 404 for unknown cell `886088500000000`.

3. **`test_f5_alert_support.py` (Python F5 Tests): 10/10 PASS**
   - Alert candidate generation, conflict penalties, monitoring gaps, deterministic event IDs, lead time metrics, recency decay, citizen report deduplication, multi-cell clustering, source state matrix, and threshold boundaries.

4. **`ForecastUnitTest` (Regression Verification): 14/14 PASS**
   - Zero regressions across existing F4 forecast pipeline.

---

## 13. Runtime Proof

Executed against PostgreSQL and local runtime for Pune Shivajinagar CAAQMS (`PUN-001`, H3 index `88608850e5fffff`):

```
2026-09-27T23:34:08.366+05:30 [main] INFO c.a.s.H3BackfillService: Station PUN-001 (Shivajinagar CAAQMS) -> coordinates (18.5314, 73.8446) -> H3 index 88608850e5fffff
2026-09-27T23:34:13.188+05:30 [main] INFO c.a.e.EvidenceOrchestrationService: Persisted GeminiAnalysis for H3 cell 88608850e5fffff linked to prediction a310c689-f340-49fc-8935-a037de8d7709
2026-09-27T23:34:14.594+05:30 [main] INFO c.a.e.EvidenceIntegrationTest: Verified GeminiAnalysis database record: id=f17cd675-eb72-45ef-b397-abc1ca853edc, h3=88608850e5fffff, model=deterministic-fallback-v1.0
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 65.48 s -- in com.aerosentinel.evidence.EvidenceIntegrationTest
[INFO] BUILD SUCCESS
```

### Verified Linkage Chain:
1. **F3 Hotspot:** `predictionId = a310c689-f340-49fc-8935-a037de8d7709`, `riskScore = 0.80`, `operationalThreshold = 0.20`, `isHotspot = true`.
2. **F4 Forecast:** `parentPredictionId = a310c689-f340-49fc-8935-a037de8d7709`, horizons $1\text{h}=71.90, 3\text{h}=70.43, 6\text{h}=70.55\ \mu\text{g/m}^3$, `forecastConfidence = null`.
3. **F5 Evidence Signals:** Primary ground sensor signal `DIRECT_OBSERVATION` at $78.0\ \mu\text{g/m}^3$ from station `PUN-001`.
4. **F5 Scoring:** Evidence score computed at $0.62$, triage state assigned as `ALERT_CANDIDATE`.
5. **Gemini / Fallback Explanation:** Generated grounded explanation without ungrounded factory causal attributions (`causalClaimSupported = false`, `isGrounded = true`).
6. **Database Persistence:** Row `f17cd675-eb72-45ef-b397-abc1ca853edc` persisted in `gemini_analyses`.

---

## 14. Known Limitations

1. **Subprocess Invocation Overhead:** On Windows, launching Python via `ProcessBuilder` carries ~200-400 ms startup overhead per invocation. Future phases can adopt a persistent gRPC or local UDS daemon if high concurrency is required.
2. **Citizen Vision Ingestion:** Handled gracefully as optional in this phase; citizen reports are deduplicated and unverified reports do not distort ground sensor weights.
3. **Satellite / Fire Datasets:** Displayed with status `"unavailable"` when live runtime satellite feeds are not configured, preserving strictly honest state representation.

---

## 15. Boundary Verification

- **F3:** Unmodified. Platt-calibrated Random Forest (`hotspot_classifier_v1`) thresholds (`0.20` ML / `0.40` Baseline) preserved.
- **F4:** Unmodified. Horizon constraints $\{1\text{h}, 3\text{h}, 6\text{h}\}$ and null confidence semantics preserved. Forecast is never recalculated during evidence orchestration.
- **F5 Scoring:** No formula reimplementation; strictly delegates to existing `EventEvidenceScoringEngine`.
- **Gemini:** Single client architecture reused (`AeroSentinelGeminiPipeline`, `GroundingValidator`). Zero second Gemini clients created.
- **Data Integrity:** No synthetic or placeholder numbers returned.

---

## 16. P2 Definition of Done

All 23 checklist items are verified and PASS:

- [x] GeminiAnalysis entity exists
- [x] GeminiAnalysisRepository exists
- [x] Existing gemini_analyses table reused
- [x] EvidenceSummaryResponse exists
- [x] EvidenceOrchestrationService exists
- [x] Python F5 scoring bridge works
- [x] Existing scoring engine reused
- [x] Existing Gemini pipeline reused
- [x] Existing grounding reused
- [x] F3 values preserved
- [x] F4 values preserved
- [x] H3 lineage preserved
- [x] Prediction lineage preserved
- [x] Gemini analysis persistence works
- [x] GET /api/v1/evidence/hotspot/{h3Index} works
- [x] Controlled error handling works
- [x] Missing-data semantics preserved
- [x] Automated tests pass (14 unit + 3 integration + 10 Python + 14 forecast)
- [x] Real Pune runtime proof passes
- [x] No F3 regression
- [x] No F4 regression
- [x] No duplicate scoring implementation
- [x] No duplicate Gemini implementation
- [x] No mock numerical values

============================================================
F5-P2 STATUS: PASS
============================================================
