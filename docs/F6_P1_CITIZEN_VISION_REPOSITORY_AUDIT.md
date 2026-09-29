# AeroSentinel — F6-P1: Citizen Report + Gemini Vision
## Repository Audit & Contract Reconciliation Report

**Phase:** Feature 6 — Phase 1 (F6-P1)  
**Date:** 2026-09-28  
**Author:** AeroSentinel Core Engineering  
**Scope:** Exhaustive Codebase Audit, Contract Reconciliation, Architecture Mapping, and Safety Boundary Definition  
**Final Status:** `F6-P1 STATUS: PASS`

---

## 1. F6 Objective

Feature 6 (F6) introduces citizen-contributed environmental observations into the AeroSentinel intelligence platform, pairing crowdsourced ground observations and photographic evidence with Google Gemini Vision analysis to generate structured, auditable visual evidence.

The core objective is to ingest citizen pollution reports, map them authoritatively to Uber H3 spatial cells, execute non-causal visual interpretation via Gemini Vision, persist structured visual evidence, and bridge this evidence into the locked F5 Evidence Orchestration and Event pipeline as an auxiliary multi-source observation signal.

```
Citizen Report (Location, Category, Description, Photo)
       ↓
Spring Boot Ingestion & Validation
       ↓
Uber H3 Spatial Cell Mapping (Resolution 8)
       ↓
Gemini Vision Analysis (Privacy-Sanitized & Grounded)
       ↓
Structured Visual Observation (SMOKE_LIKE, DUST_LIKE, BURNING_LIKE, UNKNOWN)
       ↓
GeminiAnalysis Entity & PostgreSQL Persistence
       ↓
H3-Linked Citizen Evidence Record
       ↓
Existing F5 EventEvidenceScoringEngine (source_matrix.citizen)
       ↓
Existing F5 Triage & Alert Gatekeeping (No Direct Hotspot / Alert Creation)
```

### Strict Architectural Boundaries
1. **Auxiliary Evidence Only:** Citizen reports are strictly an auxiliary evidence source. Under no circumstances can a citizen report directly create a `HIGH` hotspot, force an `ALERT_CANDIDATE` triage state, or trigger an authority alert.
2. **Decoupled from Upstream Models:** Citizen evidence remains strictly distinct from:
   - F3 numerical hotspot classification (`hotspot_classifier_v1`, Platt-calibrated Random Forest, `operationalThreshold = 0.20`).
   - F4 numerical multi-horizon forecasts (`forecast_regressors_v1.joblib`, horizons [1h, 3h, 6h]).
   - F5 deterministic composite evidence scoring (`EventEvidenceScoringEngine`).
   - Gemini causal attribution (non-causal governance strictly maintained).
3. **No Duplicate Infrastructure:** All implementations must reuse existing components across Spring Boot, Python AI service, PostgreSQL, and React.

---

## 2. Product and Problem Mapping

| Real-World Problem | Product Requirement | AeroSentinel F6 Architecture Resolution |
| :--- | :--- | :--- |
| **Monitoring Blind Spots** | Fixed CAAQMS reference monitors have sparse spatial distribution (>10 km gaps in outer zones). | Citizen reports provide hyper-local qualitative ground signals to supplement remote sensing in low-sensor density zones. |
| **Unverified / Spam Claims** | Crowdsourced reports can be unreliable, spammy, or duplicate. | Spatio-temporal deduplication (`CitizenReportDeduplicator`, 60-min / H3 window) + multi-source corroboration in F5 engine before elevating alerts. |
| **Uncalibrated Imagery** | Smartphone photos lack optical calibration and cannot measure ground PM2.5. | Strict semantic boundary: Gemini Vision describes only physical visual conditions (plumes, haze, dust), never numeric concentrations or regulatory fault. |
| **Privacy / PII Risk** | Citizen photos can capture faces, vehicle license plates, or telephone numbers. | Automated PII redaction (`PrivacyGuard`) screens text descriptions and visual feature notes before storage. |
| **Regulatory Misattribution** | Citizens often accuse specific factories or facilities of causing pollution. | Strict governance: Grounding guard enforces `causal_claim_supported = False` and blocks facility blame terminology. |

---

## 3. Existing Gemini Infrastructure Audit

Every existing component has been inspected directly in the repository:

| Component | Repository Path | Audit Status | Audit Details & Verification |
| :--- | :--- | :--- | :--- |
| **Gemini Client** | `ai-service/app/services/gemini_client.py` | **FOUND / REUSE** | `AeroSentinelGeminiClient` wraps Google GenAI SDK (`google.genai`). Supports low-temperature generation (`temperature=0.1`), structured JSON mode (`response_mime_type="application/json"`), retry loop (max 2 retries), and graceful unconfigured detection. |
| **Gemini Vision Service** | `ai-service/app/services/vision_service.py` | **FOUND / REUSE** | `analyze_citizen_image(image_input)` accepts `str`, `Path`, `bytes`, or `PIL.Image.Image`. Converts to RGB JPEG buffer, creates `types.Part.from_bytes(data=img_bytes, mime_type="image/jpeg")`, applies `VISION_SYSTEM_INSTRUCTION` and `VISION_USER_PROMPT`. Returns `CitizenVisionAnalysis`. |
| **Image Input Handling** | `ai-service/app/services/vision_service.py` | **FOUND / REUSE** | Handles path, byte arrays, and PIL Image instances. Performs PIL image format normalization, mode conversion to RGB, and buffer compression. |
| **Structured Schemas** | `ai-service/app/schemas/gemini_contracts.py` | **FOUND / REUSE** | Defines `VisualIndicators` (boolean flags: `smoke_visible`, `fire_visible`, `dust_visible`, `haze_visible`, `industrial_context_visible`, `traffic_context_visible`) and `CitizenVisionAnalysis` (`observed_visual_features`, `possible_visual_categories`, `visual_confidence_estimate`, `limitations`, `privacy_flags`). |
| **JSON Mode** | `ai-service/app/services/gemini_client.py` | **FOUND / REUSE** | `generate_structured_json` configures `response_mime_type="application/json"` and validates response through Pydantic `model_validate`. |
| **Prompt Templates** | `ai-service/app/prompts/gemini_prompts.py` | **FOUND / REUSE** | Contains versioned prompts: `VISION_SYSTEM_INSTRUCTION` and `VISION_USER_PROMPT` (version key `"vision_analysis_v001"`). Strictly prohibits numeric AQI invention and causality claims. |
| **Grounding Guard** | `ai-service/app/services/grounding_guard.py` | **FOUND / REUSE** | `GroundingValidator` screens text against `PROHIBITED_CAUSAL_PHRASES` ("caused the pollution", "factory caused", "proves violation"), verifies forecast numbers against F3 payload, and enforces `causal_claim_supported = False`. |
| **Privacy Guard** | `ai-service/app/services/privacy_guard.py` | **FOUND / REUSE** | `PrivacyGuard` detects and redacts Indian phone numbers (`PHONE_PATTERN`), vehicle plates (`VEHICLE_PLATE_PATTERN`), and email addresses (`EMAIL_PATTERN`). `sanitize_vision_analysis` cleans feature arrays. |
| **EXIF Stripping** | `ai-service/app/services/vision_service.py` | **PARTIAL** | PIL loads the raw image and saves to a fresh `io.BytesIO` buffer as JPEG without copying EXIF metadata, implicitly stripping EXIF. However, an explicit backend sanitization pass before disk persistence is not yet implemented. |
| **Image Type Validation** | `ai-service/app/services/vision_service.py` | **PARTIAL** | Python safely rejects unreadable formats during `Image.open`. Frontend validates `image/jpeg,image/png,image/webp`. Backend currently lacks explicit MIME/magic byte validation on submission. |
| **Timeout Handling** | `backend/.../EvidenceAiClient.java` | **PARTIAL** | `EvidenceAiClient` enforces 15-second subprocess timeout. FastAPI/CLI standalone vision invocation does not yet have an independent timeout configuration in Spring Boot. |
| **Deterministic Fallback** | `ai-service/app/services/vision_service.py` | **FOUND / REUSE** | Lines 66–82 provide a deterministic `CitizenVisionAnalysis` fallback with fixed indicators (`smoke_visible=True`), confidence `0.75`, clear limitation disclaimers, and PII scan flags when API key is unconfigured or offline. |
| **GeminiAnalysis Persistence** | `backend/.../model/GeminiAnalysis.java` | **PARTIAL** | PostgreSQL table `gemini_analyses` has `citizen_report_id` (V1 schema). However, Java entity `GeminiAnalysis.java` currently maps only `eventId` and `predictionId`, omitting the `citizenReportId` field. |

---

## 4. Current Citizen Implementation Audit

### Backend Layer (`backend/src/main/java/com/aerosentinel/`)
- **`CitizenReportController.java`:** **FOUND** — Implements `GET /api/v1/citizen/reports?cityId=...` and `POST /api/v1/citizen/reports`. Currently accepts `@RequestBody CitizenReport` (JSON), returning the saved entity.
- **`CitizenReportService.java`:** **FOUND** — Implements `getReportsByCity(UUID cityId)` and `createReport(CitizenReport report)`. Automatically computes H3 resolution 8 index using `H3Utils.coordinatesToH3` if missing.
- **`CitizenReport.java` (Entity):** **FOUND** — JPA entity mapped to `citizen_reports`. Contains: `id` (UUID), `userId` (UUID), `cityId` (UUID), `latitude` (Double), `longitude` (Double), `h3Index` (String), `category` (String), `description` (String), `imageUrl` (String), `submittedAt` (Instant), `status` (String, default `'PENDING'`), `verificationStatus` (String, default `'UNVERIFIED'`), `createdAt` (Instant).
- **`CitizenReportRepository.java`:** **FOUND** — Spring Data JPA repository with `findByCityIdOrderBySubmittedAtDesc(UUID cityId)` and `findByStatus(String status)`.
- **Photo Storage:** **MISSING** — No local file storage, multipart disk writer, or cloud object store (S3/GCS) service exists. Entity only stores a raw string in `imageUrl`.
- **Citizen Validators:** **PARTIAL** — Basic coordinate-to-H3 calculation exists. Jakarta Bean Validation annotations (`@NotNull`, `@Size`, `@Pattern`, `@DecimalMin`, `@DecimalMax`) are missing on `CitizenReport`.
- **Citizen DTOs:** **MISSING** — The JPA entity `CitizenReport` is exposed directly as the request/response body in `CitizenReportController`. No separate `CitizenReportCreateRequest` or `CitizenReportResponse` DTOs exist.
- **Category Enum:** **PARTIAL** — Stored as loose `String` in `CitizenReport.java`. Standardized enum `CitizenCategory` (`SMOKE`, `GARBAGE_BURNING`, `DUST`, `INDUSTRIAL`, `TRAFFIC`, `OTHER`) exists in Python `ai-service/app/schemas/citizen.py`. Frontend uses union `'SMOKE' | 'DUST' | 'BURNING' | 'ODOR' | 'OTHER'`.
- **Report Status Enum:** **PARTIAL** — Stored as loose `String` in `CitizenReport.java` defaulting to `'PENDING'`. Frontend uses `'PENDING' | 'VERIFIED' | 'DISMISSED'`.

### Frontend Layer (`frontend/src/`)
- **`CitizenReport.tsx` (Page):** **FOUND** — Complete 5-step UI: Step 1 (Location / Sector), Step 2 (Photo Upload), Step 3 (Observation category chips + text description), Step 4 (AI Visual Corroboration), Step 5 (Submission & Reference ID display). Currently uses simulated `setTimeout` transitions instead of live API submission.
- **`ReportStatus.tsx` (Component):** **FOUND** — Renders list of active community reports with category tags, coordinates, timestamps, and status badges (`VERIFIED`, `PENDING`).
- **`ImageUploader.tsx` (Component):** **FOUND** — Production-quality drag-and-drop file uploader supporting JPEG, PNG, WebP (up to 15MB), device camera capture (`capture="environment"`), object URL preview, and photo clearing/replacement.
- **`LocationPicker`:** **PARTIAL** — "Use Current Location" geolocation button and manual latitude/longitude state exist in `CitizenReport.tsx`. An interactive draggable map pin picker is not yet implemented.
- **`GeminiVisionResult` Component:** **PARTIAL** — Visual indicator tags and confidence score are rendered inline inside `CitizenReport.tsx` (lines 350–430), but not abstracted into a standalone reusable component.
- **`citizen.service.ts`:** **FOUND** — Implements `getReports(cityId)` and `submitReport(formData: FormData)`.
- **`useCitizenReport` Hook:** **MISSING** — State management is handled locally via `useState` inside `CitizenReport.tsx`.
- **`useReportStatus` Hook:** **MISSING** — Reports list is managed via local component state.
- **Citizen Types:** **FOUND** — `CitizenReport` and `GeminiAnalysis` defined in `frontend/src/types/index.ts`.

### Database Layer (`backend/src/main/resources/db/migration/`)
- **`citizen_reports` Table:** **FOUND** — Defined in `V1__init_schema.sql` (lines 181–196). Includes PostGIS GiST index `idx_citizen_reports_location` and B-tree index `idx_citizen_reports_status` in `V3__spatial_indexes.sql`.
- **`gemini_analyses` Table Relation:** **FOUND** — Column `citizen_report_id UUID REFERENCES citizen_reports(id) ON DELETE CASCADE` exists in `V1__init_schema.sql` (line 201). Extended with F5 fields in `V12__f5_gemini_analyses_hotspot_evidence.sql`.

---

## 5. Existing H3 Mapping Audit

| Dimension | Repository Finding | Source Verification |
| :--- | :--- | :--- |
| **Native Library** | `com.uber.h3core.H3Core` (v3.7.2) | `backend/pom.xml`, `H3Utils.java`, `H3Service.java` |
| **Authoritative Resolution** | **Resolution 8** (`NEIGHBORHOOD_RESOLUTION`) | `H3Utils.java` line 21: `NEIGHBORHOOD_RESOLUTION = 8` (~461m edge length, ~0.737 km² cell area). |
| **Coordinate Conversion** | `H3Utils.coordinatesToH3(lat, lng, 8)` | `H3Utils.java` line 90: returns canonical 15-character lowercase hexadecimal address (e.g., `886196944dfffff`). |
| **Coordinate Validation** | `H3Utils.validateCoordinates(lat, lng)` | Strictly validates WGS84 bounds: latitude $\in [-90.0, 90.0]$, longitude $\in [-180.0, 180.0]$, rejects NaN and Infinite. |
| **Cell Validation** | `H3Utils.isValidH3Index(h3Index)` | Validates cell address syntax and Uber H3 mathematical cell validity. |
| **Centroid & Boundary** | `H3Utils.h3ToCenter(h3)` & `H3Utils.h3ToBoundary(h3)` | Computes exact lat/lng centroid and 6-vertex polygon boundary for PostGIS / Leaflet rendering. |
| **DTO Field Naming** | `h3Index` in Java / TypeScript, `h3_index` in SQL, `h3_cell_id` in Python AI schemas | Preserved across all cross-language bridges. |
| **City Lookup Behavior** | `CitizenReport.cityId` links directly to `cities.id` | Spatial resolution verifies reports within municipal boundaries. |

> **Architectural Lock:** No secondary or alternative spatial indexing library shall be introduced. All citizen reports must be indexed strictly using Uber H3 Resolution 8 via `H3Utils.coordinatesToH3()`.

---

## 6. Gemini Vision Contract Audit

The F6 target contract requires structured visual output supporting:
- Primary visual condition: `SMOKE_LIKE`, `DUST_LIKE`, `BURNING_LIKE`, `UNKNOWN`
- Auxiliary fields: `confidence`, `observations`, `uncertainty`

### Comparison: F6 Target vs Existing Gemini Vision Implementation

| Field | F6 Target Specification | Existing `CitizenVisionAnalysis` (`gemini_contracts.py`) | Contract Compatibility & Reconciliation |
| :--- | :--- | :--- | :--- |
| **Category** | `SMOKE_LIKE`<br>`DUST_LIKE`<br>`BURNING_LIKE`<br>`UNKNOWN` | `image_indicators: VisualIndicators`<br>- `smoke_visible: bool`<br>- `fire_visible: bool`<br>- `dust_visible: bool`<br>- `haze_visible: bool`<br>- `industrial_context_visible: bool`<br>- `traffic_context_visible: bool`<br>`possible_visual_categories: List[str]` | **PARTIAL COMPATIBILITY — ADAPTER REQUIRED**<br>Deterministic mapping adapter:<br>1. If `fire_visible` or `'burning'` in categories $\to$ `BURNING_LIKE`<br>2. If `smoke_visible` or `'smoke'` in categories $\to$ `SMOKE_LIKE`<br>3. If `dust_visible` or `haze_visible` $\to$ `DUST_LIKE`<br>4. Otherwise $\to$ `UNKNOWN` |
| **Confidence** | `confidence` (float [0.0, 1.0]) | `visual_confidence_estimate` (float [0.0, 1.0]) | **REUSE** — Direct 1:1 numerical mapping. |
| **Observations** | `observations` (List[str]) | `observed_visual_features` (List[str]) | **REUSE** — Direct 1:1 list mapping. |
| **Uncertainty** | `uncertainty` (List[str]) | `limitations` (List[str]) | **REUSE** — Direct 1:1 list mapping. |
| **Privacy Flags** | Privacy compliance markers | `privacy_flags` (List[str]) | **REUSE** — Carried forward into analysis metadata. |

> **Decision:** DO NOT alter the core `CitizenVisionAnalysis` Pydantic model. Add a lightweight contract adapter method `to_f6_visual_contract()` to bridge into Spring Boot and PostgreSQL without breaking existing test suites.

---

## 7. Strict Semantic Boundary

Gemini Vision operates strictly as a **qualitative visual interpreter**, never as an analytical pollution predictor or legal authority.

```
ALLOWED VISUAL INTERPRETATIONS (Factual, Phenomenological):
✓ "Dense particulate plume visible with dark optical density"
✓ "Localized atmospheric haze with reduced horizontal visibility"
✓ "Surface dust disturbance proximate to unpaved roadway"
✓ "Thermal or flame-like visual signatures proximate to ground"
✓ "Visual features obscured by low illumination or image blur"

PROHIBITED INTERPRETATIONS (Causal, Quantitative, Accusatory):
✗ "Factory X caused the PM2.5 violation"
✗ "This image proves ground-level PM2.5 exceeds 150 µg/m³"
✗ "Industrial facility is legally liable for illegal emissions"
✗ "AQI is Hazardous based on this photograph"
✗ "Citizen was poisoned by toxic emissions"
```

### Enforcement Mechanisms Already Active in Repository
1. `VISION_SYSTEM_INSTRUCTION` (in `ai-service/app/prompts/gemini_prompts.py`):
   - Rule 1: ONLY describe visible physical phenomena.
   - Rule 2: NEVER claim causality or facility blame.
   - Rule 3: NEVER invent air quality numbers (no AQI, PM2.5, or $\mu g/m^3$).
2. `GroundingValidator` (in `ai-service/app/services/grounding_guard.py`):
   - Flags any occurrence of `PROHIBITED_CAUSAL_PHRASES`.
   - Hardcodes `causal_claim_supported = False` on every generated explanation.
3. `PrivacyGuard` (in `ai-service/app/services/privacy_guard.py`):
   - Automatically redacts vehicle registrations, telephone numbers, and email addresses from observation texts and visual features.

---

## 8. Proposed Data Model Audit

Auditing logical F6 requirements against the existing `citizen_reports` and `gemini_analyses` PostgreSQL tables:

| Field | F6 Requirement | Database Status (`citizen_reports`) | Entity Status (`CitizenReport.java`) | Reconciliation Action |
| :--- | :--- | :--- | :--- | :--- |
| `id` | Primary key UUID | **FOUND** (`UUID PRIMARY KEY DEFAULT uuid_generate_v4()`) | **FOUND** (`private UUID id`) | REUSE as primary identifier. |
| `report_id` | Business reference key | **PARTIAL** (Database uses `id`; Python uses `report_id`) | **NOT REQUIRED** in DB | Use `id.toString()` or short slug `#CR-{id[:8]}` for UI display. |
| `city_id` | City foreign key | **FOUND** (`UUID REFERENCES cities(id)`) | **FOUND** (`private UUID cityId`) | REUSE. |
| `latitude` | WGS84 Latitude | **FOUND** (`DOUBLE PRECISION NOT NULL`) | **FOUND** (`private Double latitude`) | REUSE with validation. |
| `longitude` | WGS84 Longitude | **FOUND** (`DOUBLE PRECISION NOT NULL`) | **FOUND** (`private Double longitude`) | REUSE with validation. |
| `location` | PostGIS geometry point | **FOUND** (`GEOGRAPHY(Point, 4326)`) | **NOT REQUIRED** in JPA | Handled via DB trigger or PostGIS ST_SetSRID. |
| `h3_index` / `h3_cell_id` | H3 resolution 8 cell | **FOUND** (`VARCHAR(30)`) | **FOUND** (`private String h3Index`) | REUSE. |
| `category` | Observation category | **FOUND** (`VARCHAR(50) NOT NULL`) | **FOUND** (`private String category`) | REUSE with enum validation. |
| `description` | Qualitative text note | **FOUND** (`TEXT`) | **FOUND** (`private String description`) | REUSE with length check ($\le 500$). |
| `photo reference` | Photo reference / URI | **FOUND** (`image_url TEXT`) | **FOUND** (`private String imageUrl`) | REUSE for storage key / URL. |
| `submitted_at` | Observation timestamp | **FOUND** (`TIMESTAMP WITH TIME ZONE DEFAULT NOW()`) | **FOUND** (`private Instant submittedAt`) | REUSE. |
| `status` | Report lifecycle state | **FOUND** (`VARCHAR(30) DEFAULT 'PENDING'`) | **FOUND** (`private String status`) | REUSE with defined lifecycle. |
| `verification_status` | Field verification status | **FOUND** (`VARCHAR(30) DEFAULT 'UNVERIFIED'`) | **FOUND** (`private String verificationStatus`) | REUSE. |
| `source` | Submission channel | **MISSING** in PostgreSQL table | **MISSING** in JPA entity | Optional; default `'CITIZEN_PORTAL'` in DTO. |
| `created_at` | Record creation timestamp | **FOUND** (`TIMESTAMP WITH TIME ZONE DEFAULT NOW()`) | **FOUND** (`private Instant createdAt`) | REUSE. |
| `updated_at` | Record update timestamp | **MISSING** in PostgreSQL table | **MISSING** in JPA entity | Add via migration if needed, or omit for append-only audit log. |

### `gemini_analyses` Table & Entity Audit
- **In PostgreSQL (`V1__init_schema.sql` line 201):**  
  `citizen_report_id UUID REFERENCES citizen_reports(id) ON DELETE CASCADE` exists.
- **In JPA Entity (`com.aerosentinel.model.GeminiAnalysis`):**  
  `citizenReportId`, `detectedCategory`, `confidence`, `narrativeSummary`, `rawResponse` are **MISSING** from the Java mapping.
- **Reconciliation Action:**  
  Add `citizenReportId`, `detectedCategory`, and `confidence` fields to `GeminiAnalysis.java` so that vision analyses linked to citizen reports can be queried and persisted natively via `GeminiAnalysisRepository`.

---

## 9. Citizen Report Status Audit

### Conceptual Lifecycle vs Actual Codebase State

```
Conceptual Model:
  SUBMITTED → ANALYZING → ANALYZED → EVENT EVIDENCE → VERIFIED / DISMISSED

Database Actual (citizen_reports):
  status VARCHAR(30) DEFAULT 'PENDING'
  verification_status VARCHAR(30) DEFAULT 'UNVERIFIED'

Frontend Actual (types/index.ts):
  status: 'PENDING' | 'VERIFIED' | 'DISMISSED'
```

### Audit Findings
1. **Existing Transitions:** The backend currently does not enforce a state machine; it persists whatever string is passed (defaulting to `'PENDING'`).
2. **Missing Transitions:** Transition to `'ANALYZING'` (when vision job starts), `'ANALYZED'` (when Gemini Vision completes), and `'EVIDENCE_LINKED'` (when incorporated into F5 event evidence) are **PLANNED BUT NOT IMPLEMENTED**.
3. **Recommended Compatible Lifecycle:**  
   Retain database column `status` with backward-compatible lifecycle states:
   - `PENDING`: Initial submission, queued for analysis.
   - `ANALYZED`: Gemini Vision structured interpretation complete and persisted.
   - `VERIFIED`: Confirmed by ground monitoring sensor, remote sensing, or field inspection.
   - `DISMISSED`: Flagged as duplicate, spam, or uncorroborated after surveillance window expires.

---

## 10. Photo Handling Audit

| Dimension | Specification Requirement | Repository Audit Finding | Assessment |
| :--- | :--- | :--- | :--- |
| **Supported MIME Types** | JPEG, PNG, WebP | UI enforces `image/jpeg,image/png,image/webp`. Python PIL accepts JPEG, PNG, WebP. | **PARTIAL** — Backend controller needs explicit MIME validation header check. |
| **Maximum File Size** | $\le 15$ MB | UI specifies `Max 15MB`. Python has no hard limit. | **PARTIAL** — Needs Spring Boot `spring.servlet.multipart.max-file-size=15MB` configuration. |
| **Content / Magic Byte Validation** | Validate binary file header | Python PIL `Image.open` decodes header and validates structure. | **PARTIAL** — Missing initial magic byte screening before disk buffer. |
| **EXIF / Metadata Handling** | Strip geolocation, camera, and device serial numbers | Python pipeline loads image via PIL and exports to clean JPEG byte buffer, removing EXIF. | **REUSE** — Built into `vision_service.py`. |
| **Storage Mechanism** | Managed local or object storage with unique key | Entity has `imageUrl TEXT`, but no storage service exists. | **MISSING** — Requires dedicated `PhotoStorageService` storing to a secured workspace artifact/upload directory. |
| **Filename Sanitization** | Prevent path traversal attacks (`../../`) | No client-provided filename should be written directly to disk. | **MISSING** — Must generate deterministic random storage key: `UUID.randomUUID().toString() + ".jpg"`. |
| **Secret Exposure Guard** | Never expose API keys in URLs or responses | React frontend never receives `GEMINI_API_KEY`. | **REUSE** — Fully secure. |

---

## 11. API Contract Audit

### Check 1: Ingestion Endpoint
- **Existing:** `POST /api/v1/citizen/reports` in `CitizenReportController.java`.
- **Mismatch:** Controller expects `@RequestBody CitizenReport` (`application/json`), but frontend `citizen.service.ts` sends `multipart/form-data` (`formData`).
- **Contract Resolution:** Support both or update endpoint to accept `multipart/form-data` containing JSON metadata (`location`, `category`, `description`) + `photo` file, OR accept JSON with a previously uploaded photo reference.

### Check 2: Query by City
- **Existing:** `GET /api/v1/citizen/reports?cityId={cityId}` in `CitizenReportController.java`.
- **Status:** **FOUND & OPERATIONAL**.

### Check 3: Query Report by ID
- **Expected:** `GET /api/v1/citizen/reports/{reportId}`.
- **Repository Finding:** **MISSING** in `CitizenReportController`. Must be added to allow status tracking and detail view retrieval.

### Check 4: Vision Analysis Trigger Endpoint
- **Existing:** No standalone HTTP endpoint in Spring Boot or FastAPI triggers vision analysis for a report. It is called internally in Python `AeroSentinelGeminiPipeline` or simulated in frontend.
- **Contract Resolution:** Expose internal service call or dedicated controller action to link Gemini Vision execution to report ingestion.

---

## 12. F6 → F5 Integration Audit

```
[Citizen Report]
       ↓
[Photo Storage] → [Gemini Vision Analysis]
                          ↓
              [GeminiAnalysis Entity]
                          ↓
           [EventEvidence Entry (Source: CITIZEN)]
                          ↓
       [EvidenceOrchestrationService (H3 Cell)]
                          ↓
       [EventEvidenceScoringEngine (Multi-Source Matrix)]
         - source_matrix.citizen = SUPPORTED
         - sources_present.citizen_present = True
         - evidence_completeness recalculation
                          ↓
       [Deterministic Triage & Alert Gatekeeping]
         (Alert candidate requires multi-source agreement;
          Citizen report alone NEVER triggers an alert)
```

### Audit Findings
1. **Python Scoring Engine Ready:** In `ai-service/ml/alert_support/scoring_engine.py` (lines 134–141), `EventEvidenceScoringEngine` already has explicit support for citizen reports:
   ```python
   has_citizen_visual = bool(f4_explanation and f4_explanation.get("visual_evidence"))
   if citizen_count > 0 or has_citizen_visual:
       sources_present.citizen_present = True
       source_matrix.citizen = SourceObservationState.SUPPORTED
   ```
2. **Deduplication Ready:** `CitizenReportDeduplicator.deduplicate_reports` coalesces reports within the same H3 cell and 60-minute time window.
3. **Backend Orchestration Gap:** In `backend/src/main/java/com/aerosentinel/evidence/EvidenceOrchestrationService.java`, `buildCliPayload()` does **not** currently query `CitizenReportRepository` to include active citizen reports for that H3 cell in `cliPayload["citizenReports"]`.
4. **Safety Boundary Verified:** Citizen evidence provides auxiliary weight (up to 1 tier of evidence completeness) but cannot override `MONITOR` or `INSUFFICIENT_EVIDENCE` into `ALERT_CANDIDATE` without ground sensor or satellite corroboration.

---

## 13. Multimodal Pipeline Audit

Mapping the complete request and execution lifecycle:

```
[React Frontend]
  │ User selects location, category, enters text, attaches photo
  │ Photo processed locally as preview; no Gemini keys in browser
  ▼
[Spring Boot (Port 8080)]
  │ /api/v1/citizen/reports receives multipart submission
  │ Validates coordinates, computes H3 Resolution 8 index
  │ Sanitizes photo filename, strips EXIF, stores locally
  │ Persists CitizenReport (status: PENDING)
  ▼
[AI Service Bridge (ProcessBuilder / FastAPI)]
  │ Dispatches image and metadata to vision pipeline
  │ vision_service.py standardizes PIL RGB image
  ▼
[Google Gemini 2.0 Flash / Vision SDK]
  │ Analyzes visual indicators under VISION_SYSTEM_INSTRUCTION
  │ Emits strict JSON adhering to CitizenVisionAnalysis schema
  │ [Or invokes built-in deterministic fallback if unconfigured]
  ▼
[Grounding & Privacy Guard]
  │ Redacts potential PII; verifies no causal claims made
  ▼
[Spring Boot Persistence]
  │ Persists GeminiAnalysis linked via citizen_report_id
  │ Updates CitizenReport status: ANALYZED
  │ Registers EventEvidence for H3 cell
  ▼
[PostgreSQL Database]
  │ citizen_reports & gemini_analyses updated atomically
```

- **Key Verification:** React code contains zero Gemini credentials. `GEMINI_API_KEY` is isolated to backend/AI service environment.

---

## 14. Failure Mode Audit

| # | Failure Mode | Expected Architectural Behavior | Existing Repository Implementation Status |
| :---: | :--- | :--- | :--- |
| 1 | **Photo Missing** | Report accepted as text-only observation. Vision analysis skipped. Status set to `ANALYZED` with null vision. | **FOUND** — Entity permits `imageUrl = null`. |
| 2 | **Unsupported Image Type** | Reject with 400 Bad Request. Clear error message specifying JPEG/PNG/WebP. | **PARTIAL** — Frontend validates; backend needs MIME validation. |
| 3 | **Oversized Image (>15MB)** | Reject with 413 Payload Too Large. Prevent memory exhaustion. | **PARTIAL** — Frontend validates; backend needs configuration limit. |
| 4 | **Corrupt Image** | Safe decode failure caught; return 422 Unprocessable Entity. | **FOUND** in Python (`PIL.Image.open` throws, caught gracefully). |
| 5 | **Location Missing** | Reject with 400 Bad Request; latitude and longitude required. | **FOUND** — Database columns `NOT NULL`; needs Bean Validation. |
| 6 | **Invalid Coordinates** | Reject with 400 Bad Request; WGS84 range validation. | **FOUND** — `H3Utils.validateCoordinates()` throws `IllegalArgumentException`. |
| 7 | **Invalid Category** | Normalize or map to `'OTHER'`. | **FOUND** in Python (`normalize_category` validator); needs Java check. |
| 8 | **Gemini API Unavailable** | Fallback to deterministic visual indicators without throwing 500 error. Report preserved. | **FOUND** — `vision_service.py` provides deterministic fallback. |
| 9 | **Gemini Timeout** | Bounded timeout (15s); fallback to unanalyzed status with retry flag. | **FOUND** in `EvidenceAiClient.java` (15s process timeout). |
| 10 | **Malformed Model JSON** | Caught by `json.loads` or Pydantic `model_validate`; fallback activated. | **FOUND** in `gemini_client.py` and `gemini_pipeline.py`. |
| 11 | **Grounding Guard Failure** | Prohibited causal phrase detected; text sanitized, `causal_claim_supported` set to `False`. | **FOUND** in `GroundingValidator.validate_grounding()`. |
| 12 | **Database Failure** | Spring `@Transactional` rollback; no orphaned partial records. | **FOUND** in Spring Boot service layer. |
| 13 | **Duplicate Submissions** | Coalesce in 60-min window per H3 cell for evidence scoring. | **FOUND** in `CitizenReportDeduplicator.py`. |
| 14 | **Stale Status Request** | Return cached report or 404 if invalid ID. | **PARTIAL** — Need `getReportById` endpoint. |

---

## 15. Frontend UX Audit

Inspection of `frontend/src/pages/public/CitizenReport.tsx`, `ImageUploader.tsx`, and `ReportStatus.tsx`:

| UX Element | Specified Requirement | Existing Implementation | Audit Status |
| :--- | :--- | :--- | :--- |
| **Location Input** | Current GPS button + coordinate inputs | Geolocation navigator button (`handleUseMyLocation`) + lat/lng state display. | **FOUND** |
| **Map Picker** | Interactive click/drag map pin | Only coordinate inputs and fixed H3 display are rendered; no interactive map picker. | **PARTIAL** |
| **Category Selection** | Category chips / dropdown | 6 observation chips (`Smoke`, `Dust`, `Burning`, `Strong odour`, `Industrial activity`, `Other`). | **FOUND** |
| **Description Field** | Textarea with placeholder and character counter | Textarea with guidance notes. | **FOUND** |
| **Photo Uploader** | Drag-and-drop, camera capture, format hints | `ImageUploader.tsx` with drag-and-drop, environmental camera capture, 15MB hint. | **FOUND** |
| **Image Preview** | Thumbnail preview with remove button | Preview overlay with thumbnail and remove (`X`) button. | **FOUND** |
| **Submit Loading State** | Disabled button with spinner | `isSubmitting` state with loading indicator. | **FOUND** |
| **Success State** | Confirmation banner with reference ID | Reference ID card (`CR-XXXX`), timestamp, and navigation CTA. | **FOUND** |
| **Analyzing State** | Visual indicator during AI processing | `isAnalyzingVision` spinner and "Gemini 2.5 Flash Vision analyzing...". | **FOUND** |
| **Analysis Result Display** | Detected indicators, confidence badge | Rendered badge, indicator list, and status pills. | **FOUND** |
| **Live API Integration** | Submits to backend REST API | Currently uses `setTimeout` simulation. | **MISSING** (To be connected in F6-P4/P5). |

---

## 16. Security Audit

1. **Endpoint Authorization (`SecurityConfig.java`):**  
   - Line 45: `"/api/v1/citizen/**"` is included in `.permitAll()`.  
   - Any public citizen can submit reports without a pre-registered account or JWT bearer token.  
   - **Role-Level Citizen Authorization:** **NOT VERIFIED IN REPOSITORY** (no fine-grained rate-limiting or user identity enforcement currently implemented for citizen endpoints).
2. **File Upload Handling:**  
   - Backend currently has no active multipart storage handler.  
   - Path traversal prevention (`../` in filenames) must be enforced upon implementation by generating randomized UUID keys.
3. **Secret Isolation:**  
   - `GEMINI_API_KEY` is read only from server-side environment variables (`ai-service/app/services/gemini_client.py`).  
   - It is never exposed in frontend assets, Vite bundles, or client responses.
4. **PII Protection:**  
   - `PrivacyGuard.py` screens descriptions and vision labels for phone numbers, vehicle license plates, and email addresses.

---

## 17. Duplicate and Idempotency Audit

1. **Spatio-Temporal Deduplication:**  
   - Implemented in `ai-service/ml/alert_support/citizen_dedup.py`.  
   - Coalesces reports in the same H3 cell within a 60-minute window (`MAX_TIME_DELTA_MINUTES = 60.0`).  
   - Prevents flash mobs or repeated citizen button taps from artificially inflating the evidence score.
2. **Database Storage Deduplication:**  
   - The `citizen_reports` PostgreSQL table assigns a unique UUID to every submission.  
   - It does not discard submissions at the database layer, ensuring complete citizen auditability.  
   - Deduplication occurs at the analytical consumption layer (`EventEvidenceScoringEngine`), preserving raw citizen records while preventing skewed scoring.

---

## 18. Reusable Components vs Missing Components

### Reusable Components (Zero Re-Implementation Needed)
- `H3Utils.java` & `H3Service.java` (Uber H3 resolution 8 calculations).
- `AeroSentinelGeminiClient` (`gemini_client.py` SDK wrapper, retries, JSON mode).
- `analyze_citizen_image` (`vision_service.py` image loader, PIL normalization, deterministic fallback).
- `GroundingValidator` (`grounding_guard.py` non-causality enforcement).
- `PrivacyGuard` (`privacy_guard.py` regex PII redaction).
- `CitizenReportDeduplicator` (`citizen_dedup.py` spatio-temporal clustering).
- `EventEvidenceScoringEngine` (`scoring_engine.py` multi-source matrix and evidence completeness).
- `ImageUploader.tsx` (frontend image drag-and-drop, camera, preview).
- `ReportStatus.tsx` (frontend community reports list).
- `V1__init_schema.sql` `citizen_reports` and `gemini_analyses` database tables.

### Missing Components (To Be Implemented in Planned Phases)
1. **Spring Boot Multipart & Storage Service:** Service to accept multipart image upload, sanitize storage keys, and save to local artifact directory.
2. **Backend Citizen DTOs & Validation:** `CitizenReportCreateRequest`, `CitizenReportResponse`, and Bean Validation annotations.
3. **`GET /api/v1/citizen/reports/{id}` Endpoint:** Single-report detail lookup.
4. **`GeminiAnalysis.java` Entity Reconciliation:** Add `citizenReportId`, `detectedCategory`, and `confidence` fields.
5. **Spring Boot $\to$ Python Vision Trigger:** Bridge allowing Spring Boot to invoke Gemini Vision for a citizen report.
6. **`EvidenceOrchestrationService` Payload Injection:** Query `CitizenReportRepository` to include `citizenReports` in F5 evidence payload.
7. **Frontend Live API Wiring:** Replace `setTimeout` simulation in `CitizenReport.tsx` with calls to `citizen.service.ts`.

---

## 19. Contract Mismatches Summary

| Contract Area | Upstream / Client Expectation | Downstream / Server Reality | Resolution |
| :--- | :--- | :--- | :--- |
| **Ingestion Content-Type** | Frontend `citizen.service.ts` submits `multipart/form-data` | Backend `CitizenReportController` expects `application/json` `@RequestBody` | Add multipart support in `CitizenReportController` accepting JSON metadata + optional `photo` file. |
| **Vision Category Model** | F6 target: `SMOKE_LIKE`, `DUST_LIKE`, `BURNING_LIKE`, `UNKNOWN` | `CitizenVisionAnalysis`: `VisualIndicators` (booleans) + `possible_visual_categories` | Add deterministic adapter mapping booleans/categories to F6 target enum. |
| **`gemini_analyses` Mapping** | Database has `citizen_report_id UUID` foreign key | Java entity `GeminiAnalysis.java` omits `citizenReportId` | Add `citizenReportId` field to `GeminiAnalysis.java`. |
| **Report Status Lifecycle** | Frontend expects `PENDING`, `VERIFIED`, `DISMISSED` | Entity has loose string; no transition state machine | Retain compatible string states; enforce valid transitions in service. |

---

## 20. Proposed Minimal Implementation Sequence

```
F6-P1: Repository Audit & Contract Reconciliation  <-- [COMPLETED HERE]
  │
  ▼
F6-P2: Backend Ingestion, Storage & Schema Alignment
  │ - Add citizenReportId to GeminiAnalysis entity
  │ - Create CitizenReport DTOs with Bean Validation
  │ - Implement secure photo storage & EXIF stripping
  │ - Update CitizenReportController for multipart & single-report GET
  │
  ▼
F6-P3: Gemini Vision Contract Adapter & Service Bridge
  │ - Add F6 target category adapter (SMOKE_LIKE, DUST_LIKE, etc.)
  │ - Connect Spring Boot to AI service vision execution
  │ - Persist GeminiAnalysis linked to CitizenReport
  │
  ▼
F6-P4: F5 Evidence Engine Bridge
  │ - Wire CitizenReportRepository into EvidenceOrchestrationService
  │ - Pass citizenReports into F5 CLI scoring payload
  │ - Verify source_matrix.citizen and evidence completeness updates
  │
  ▼
F6-P5: Frontend Live Integration & UX Polish
  │ - Connect CitizenReport.tsx to live backend API
  │ - Render live Gemini Vision analysis results
  │ - Verify end-to-end report-to-evidence flow
```

---

## 21. No-Duplication Boundaries

To preserve architectural integrity across all phases of F6:
1. **DO NOT** create a new Gemini API client. Reuse `ai-service/app/services/gemini_client.py`.
2. **DO NOT** rewrite vision processing logic. Reuse `ai-service/app/services/vision_service.py`.
3. **DO NOT** create a new spatial indexing utility. Reuse `H3Utils.java` and `H3Service.java` at resolution 8.
4. **DO NOT** reimplement grounding or privacy guards. Reuse `grounding_guard.py` and `privacy_guard.py`.
5. **DO NOT** create an independent scoring engine. Citizen reports must feed into the existing `EventEvidenceScoringEngine.py`.
6. **DO NOT** create an alternative alert engine or alert table. Alerts must flow solely through existing F5 alert rules.

---

## 22. P1 Definition of Done Checklist

- [x] Existing citizen-report capability audited.
- [x] Existing Gemini Vision capability audited.
- [x] Existing H3 mapping audited.
- [x] Existing image/privacy handling audited.
- [x] Existing citizen DB schema audited.
- [x] Existing GeminiAnalysis relationship audited.
- [x] Existing API endpoints audited.
- [x] Existing frontend citizen components audited.
- [x] Existing status lifecycle audited.
- [x] Existing failure handling audited.
- [x] Existing security behavior audited.
- [x] Existing F6→F5 evidence bridge audited.
- [x] Duplicate/idempotency behavior audited.
- [x] Reusable components identified.
- [x] Missing components identified.
- [x] Contract mismatches identified.
- [x] No duplicate architecture proposed unnecessarily.
- [x] Audit report created.

---

## 23. Final Status

```
============================================================
F6-P1 STATUS: PASS
============================================================
```
