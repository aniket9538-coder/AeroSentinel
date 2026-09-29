# AeroSentinel — F6/F4 Current Runtime Diagnostic Report

**Audit Type:** Strictly Read-Only Diagnostic Audit  
**Timestamp:** 2026-09-29T07:49:00+05:30 (02:19:00 UTC)  
**Environment:** Windows (Local Dev), Docker PostgreSQL 16.4 PostGIS, Spring Boot 3.3.4 (PID 51892), Vite Frontend (Port 3000)

---

## Executive Summary

This read-only audit diagnoses the root causes of the three reported UI symptoms:
1. **Citizen Reports timeout of 10000ms exceeded:** The frontend Axios client configuration enforces a strict 10,000ms request timeout (`frontend/src/services/api.ts`). When a citizen report with image is submitted, Spring Boot synchronously invokes the Python Gemini Vision CLI (`vision_cli.py`), which takes between 11.9s and 15s to complete inference via the Google Gemini API. Because this exceeds 10,000ms, the frontend Axios promise aborts with `"timeout of 10000ms exceeded"`. The backend actually completes the ingestion, persists the citizen report, runs Gemini analysis, and saves the record in PostgreSQL.
2. **Forecast Page "Forecast Telemetry Unavailable" vs Shivajinagar Stale:**
   - Shivajinagar (`88608850e5fffff`) has 3 persisted forecast rows generated on `2026-09-28 17:12:00 UTC` (age ~8h 52m). Per `ForecastMapper.computeFreshness()`, an age between 2 and 24 hours evaluates to `STALE`.
   - The other monitored preset cells (`88608852c1fffff` Katraj, `8860885357fffff` Hadapsar, `88608b56b3fffff` Mumbai Kurla, and `883da11505fffff` Delhi R K Puram) have forecast rows generated on `2026-09-27 13:42 UTC` (age ~36h). Per `ForecastMapper.computeFreshness()`, any forecast older than 24 hours is classified as `UNAVAILABLE`. In `Forecast.tsx`, `freshness === 'UNAVAILABLE'` displays the error card `"Forecast Telemetry Unavailable"`.
3. **Alerts Page (0) vs Sidebar Badge (1):**
   - The `alerts` table in PostgreSQL currently contains **0 rows**.
   - `Alerts.tsx` queries `GET /api/v1/alerts/authority`, which returns 0 rows, accurately rendering `ALL: 0, OPEN: 0, ACKNOWLEDGED: 0, RESOLVED: 0`.
   - The sidebar badge `1` is a static, hardcoded string in `frontend/src/components/layout/Sidebar.tsx` line 52: `{ label: 'Alerts', path: '/authority/alerts', icon: BellRing, badge: '1' }`.

---

## Section A: Current PostgreSQL Database Identity

Executed read-only query in running PostgreSQL container:
```sql
SELECT current_database(), current_schema();
```
**Output:**
```
 current_database | current_schema 
------------------+----------------
 aerosentinel     | public
(1 row)
```

**Spring Boot Datasource Verification (`backend/src/main/resources/application-dev.yml`):**
```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/aerosentinel}
    username: ${DATABASE_USERNAME:aerosentinel_user}
```
Spring Boot connects directly to `jdbc:postgresql://localhost:5432/aerosentinel` schema `public`.

---

## Section B: Current Alert Row Count

Executed read-only query:
```sql
SELECT count(*) FROM alerts;
```
**Output:**
```
 count 
-------
     0
(1 row)
```
The `alerts` table currently contains **0 rows**.

---

## Section C: Alert Status Counts & Recent Alerts

Executed read-only queries:
```sql
SELECT status, count(*)
FROM alerts
GROUP BY status
ORDER BY status;
```
**Output:**
```
 status | count 
--------+-------
(0 rows)
```

Most recent alerts inspection:
```sql
SELECT id, event_id, status, h3_index, prediction_id,
       event_code, evidence_score, risk_score,
       created_at, updated_at, resolved_at
FROM alerts
ORDER BY created_at DESC
LIMIT 20;
```
**Output:**
```
 id | event_id | status | h3_index | prediction_id | event_code | evidence_score | risk_score | created_at | updated_at | resolved_at 
----+----------+--------+----------+---------------+------------+----------------+------------+------------+------------+-------------
(0 rows)
```

---

## Section D: Alert Endpoint Response

Inspected backend implementation in `AlertController.java` and `AlertService.java`:
- **Endpoint:** `GET /api/v1/alerts/authority`
- **Method:** `AlertService.getAuthorityQueue(status, cityId)`
- **Query Logic:**
  - If `cityId != null && status != null && !status.equalsIgnoreCase("ALL")`: `findByCityIdAndStatusOrderByCreatedAtDesc(cityId, status.toUpperCase())`
  - If `cityId != null`: `findByCityIdOrderByCreatedAtDesc(cityId)`
  - If `status != null && !status.equalsIgnoreCase("ALL")`: `findByStatusOrderByCreatedAtDesc(status.toUpperCase())`
  - Default (no filter or `ALL`): `findAllByOrderByCreatedAtDesc()`
- **Filters by City:** Supported via `@RequestParam(required = false) UUID cityId`.
- **Filters by Status:** Supported via `@RequestParam(required = false) String status`.
- **Excludes Resolved Alerts:** **No.** `findAllByOrderByCreatedAtDesc()` includes all alert lifecycle states (`OPEN`, `ACKNOWLEDGED`, `RESOLVED`).
- **Live HTTP Request (`curl -i http://localhost:8080/api/v1/alerts/authority`):**
  - **HTTP Status:** `200 OK`
  - **Response Body:** `[]`
  - **Count:** `0`
- **Comparison with DB:** Exactly matches `SELECT count(*) FROM alerts;` (both yield 0).

---

## Section E: Sidebar / Page Count Mismatch Explanation

- **Alerts Page Implementation (`frontend/src/pages/authority/Alerts.tsx` lines 34-58):**
  - Uses `useAuthorityQueue()` hook which calls `GET /api/v1/alerts/authority`.
  - Computes counts dynamically from returned live data:
    ```typescript
    const counts = useMemo(() => ({
      ALL: items.length,
      OPEN: items.filter((i) => i.status === 'OPEN').length,
      ACKNOWLEDGED: items.filter((i) => i.status === 'ACKNOWLEDGED').length,
      RESOLVED: items.filter((i) => i.status === 'RESOLVED').length,
    }), [items]);
    ```
  - Because `items` is empty (`[]`), all 4 pill counters display `0`.
- **Sidebar Badge Implementation (`frontend/src/components/layout/Sidebar.tsx` line 52):**
  - Uses static configuration array `NAV_SECTIONS`:
    ```typescript
    { label: 'Alerts', path: '/authority/alerts', icon: BellRing, badge: '1' }
    ```
  - The badge is **hardcoded to the string `'1'`** in the UI navigation definition and does not subscribe to `useAuthorityQueue()`.

---

## Section F: Forecast Row Availability by H3

Executed read-only PostgreSQL query on `forecasts`:
```sql
SELECT h3_index,
       min(generated_at) AS first_generated,
       max(generated_at) AS last_generated,
       now() - max(generated_at) AS age,
       count(*) AS row_count
FROM forecasts
GROUP BY h3_index
ORDER BY h3_index;
```

**Output:**
| H3 Cell | City / Monitored Station | Forecast Rows | Generated At (UTC) | Current Age | Status | Backend Freshness |
|---|---|---|---|---|---|---|
| `88608850e5fffff` | Pune Shivaji Nagar (PUN-001) | 3 | 2026-09-28 17:12:00 | 08h 52m | SUCCESS | `STALE` |
| `88608852c1fffff` | Pune Katraj (PUN-002) | 3 | 2026-09-27 13:42:13 | 36h 22m | SUCCESS | `UNAVAILABLE` |
| `8860885357fffff` | Pune Hadapsar (PUN-003) | 3 | 2026-09-27 13:42:33 | 36h 21m | SUCCESS | `UNAVAILABLE` |
| `88608b56b3fffff` | Mumbai Kurla (MUM-001) | 3 | 2026-09-27 13:42:47 | 36h 21m | SUCCESS | `UNAVAILABLE` |
| `883da11505fffff` | Delhi R K Puram (DEL-001) | 3 | 2026-09-27 13:43:00 | 36h 21m | SUCCESS | `UNAVAILABLE` |
| `88608b54d7fffff` | Mumbai Airport T2 (MUM-002) | 0 | None | N/A | NO_DATA | `NO_DATA` |
| `883da1149bfffff` | Delhi Anand Vihar (DEL-002) | 0 | None | N/A | NO_DATA | `NO_DATA` |
| `883da18d9dfffff` | Delhi Punjabi Bagh (DEL-003) | 0 | None | N/A | NO_DATA | `NO_DATA` |

---

## Section G: Exact Cause of Forecast UNAVAILABLE vs Stale

Inspected `ForecastMapper.computeFreshness()` in `backend/src/main/java/com/aerosentinel/forecast/ForecastMapper.java` (lines 56–68):
```java
public static String computeFreshness(Instant generatedAt, Instant now) {
    if (generatedAt == null) {
        return "NO_DATA";
    }
    Duration age = Duration.between(generatedAt, now);
    if (age.isNegative() || age.toHours() <= 2) {
        return "LIVE";
    } else if (age.toHours() <= 24) {
        return "STALE";
    } else {
        return "UNAVAILABLE";
    }
}
```

**Causality Chain:**
1. **Shivajinagar (`88608850e5fffff`):**
   - Forecast generated during F6 test at `2026-09-28 17:12:00 UTC`.
   - Age is ~8.8 hours.
   - $2\text{h} < \text{age} \le 24\text{h} \implies \text{freshness} = \text{"STALE"}$.
   - Frontend `Forecast.tsx` renders the trajectory and warns: `"Forecast is Stale: The persisted forecast was generated more than 2 hours ago."`
2. **Katraj, Hadapsar, Kurla, R K Puram:**
   - Forecasts generated during F4 Phase 8 verification at `2026-09-27 13:42 UTC`.
   - Age is ~36.3 hours.
   - $\text{age} > 24\text{h} \implies \text{freshness} = \text{"UNAVAILABLE"}$.
   - Frontend `Forecast.tsx` lines 310–317 catches `freshness === 'UNAVAILABLE'` and displays:
     `ErrorState: title="Forecast Telemetry Unavailable", message="Forecast telemetry is currently unavailable for this cell..."`.
3. **Cells with 0 rows (e.g. Anand Vihar, Mumbai Airport):**
   - Backend `GET /api/v1/forecast/{h3}` returns HTTP 404 with `{"status": "NO_DATA"}`.
   - Frontend `forecastApi.ts` catches 404 and returns `freshness: "NO_DATA"`.
   - Frontend displays `EmptyState: title="No forecast available for this cell"`.

---

## Section H: Exact Cause of 10000ms Timeout

Inspected frontend API configuration and Spring Boot execution traces:

1. **Frontend Timeout Configuration (`frontend/src/services/api.ts` line 8):**
   ```typescript
   export const apiClient = axios.create({
     baseURL: (typeof import.meta !== 'undefined' && import.meta.env?.VITE_API_BASE_URL) || '/api/v1',
     headers: { 'Content-Type': 'application/json' },
     timeout: 10000, // 10 seconds
   });
   ```
2. **Backend Submission Flow (`CitizenReportService.java`):**
   - `citizenReportRepository.save(report)` — fast (~10ms)
   - `CitizenVisionAiClient.analyzeImage()` — synchronous execution of Python CLI (`vision_cli.py`) invoking Google Gemini 3.1 Flash Lite API with 15000ms timeout.
3. **Observed Timings in Spring Boot Task Log (`task-4530.log`):**
   - Report `b6fcb953-2773-4aa6-9d98-427648e00c24`:
     - Submission started: `07:07:18.081`
     - Vision CLI timed out after 15000 ms: `07:07:33.127`
     - Total elapsed: **15.04 seconds** ($> 10.0$s Axios limit).
   - Report `6226166d-30a6-4c7b-b5ef-72b4cc427e68`:
     - Submission started: `07:07:33.448`
     - Gemini analysis persisted: `07:07:45.340`
     - Total elapsed: **11.89 seconds** ($> 10.0$s Axios limit).
4. **Trigger Mechanism:**
   Because the backend Vision analysis took 11.9s to 15.0s, the client's Axios timer fired at exactly 10,000ms, raising `ECONNABORTED: timeout of 10000ms exceeded`.
   In `CitizenReport.tsx` line 185:
   `setErrorMessage(err?.response?.data?.message || err?.message || 'Report submission failed.');`
   renders the red warning banner: `"timeout of 10000ms exceeded"`.

---

## Section I: Citizen Report Persistence Status

Executed read-only query in PostgreSQL:
```sql
SELECT id, category, status, verification_status, h3_index, created_at
FROM citizen_reports
ORDER BY created_at DESC;
```
**Output:**
```
                  id                  | category |  status  | verification_status |    h3_index     |          created_at           
--------------------------------------+----------+----------+---------------------+-----------------+-------------------------------
 6226166d-30a6-4c7b-b5ef-72b4cc427e68 | SMOKE    | ANALYZED | UNVERIFIED          | 88608e26a7fffff | 2026-09-29 01:37:33.448901+00
 b6fcb953-2773-4aa6-9d98-427648e00c24 | SMOKE    | ANALYZED | UNVERIFIED          | 88608e26a7fffff | 2026-09-29 01:37:18.081307+00
 af733f86-9d6c-4a9a-8275-553b6f7f4b25 | SMOKE    | ANALYZED | UNVERIFIED          | 88608e26a7fffff | 2026-09-29 01:32:43.541606+00
 5fb3d7f6-e8d1-45aa-808f-a219a697bdbe | SMOKE    | ANALYZED | UNVERIFIED          | 88608850e5fffff | 2026-09-28 17:23:09.211062+00
 28d53391-f20c-4fef-a9d8-98bcc1b0b09b | SMOKE    | ANALYZED | UNVERIFIED          | 88608850e5fffff | 2026-09-28 17:11:30.496985+00
 07b813e2-a17d-455f-9761-744c0989a6ce | SMOKE    | ANALYZED | UNVERIFIED          | 88608850e5fffff | 2026-09-28 17:08:12.907639+00
 807306a7-1fd7-4347-b9bc-62d78136e6b2 | SMOKE    | ANALYZED | UNVERIFIED          | 88608850e5fffff | 2026-09-28 16:45:37.304253+00
(7 rows)
```

**Report `#af733f86` Verification:**
- Report ID: `af733f86-9d6c-4a9a-8275-553b6f7f4b25`
- Public Reference: `CR-AF733F86`
- Status: `ANALYZED`
- Verification Status: `UNVERIFIED`
- H3 Index: `88608e26a7fffff`
- Image URL: `/api/v1/citizen/photos/c6e75cad-d844-463d-8db7-4d6d99278689.jpg`
- **Result:** Report `#af733f86` was **fully persisted in the database** despite the frontend displaying the 10000ms timeout banner.

---

## Section J: Gemini Analysis Status for Report #af733f86

Executed read-only query on `gemini_analyses`:
```sql
SELECT id, citizen_report_id, model_name, detected_category,
       confidence, narrative_summary, is_grounded, analyzed_at
FROM gemini_analyses
WHERE citizen_report_id = 'af733f86-9d6c-4a9a-8275-553b6f7f4b25';
```
**Output:**
```
                  id                  |          citizen_report_id           |      model_name       | detected_category | confidence | narrative_summary | is_grounded |          analyzed_at          
--------------------------------------+--------------------------------------+-----------------------+-------------------+------------+-------------------+-------------+-------------------------------
 5b27f597-fc82-48de-a8c1-c22377d26a00 | af733f86-9d6c-4a9a-8275-553b6f7f4b25 | gemini-3.1-flash-lite | SMOKE_LIKE        |       0.95 | two red and white striped industrial chimneys. white plume emanating from the top of the chimneys. clear blue sky background | t           | 2026-09-29 01:32:50.460411+00
(1 row)
```
- Real Gemini inference: **SUCCESSFUL** (`gemini-3.1-flash-lite`, confidence 0.95, is_grounded=true).

---

## Section K: F5 / Alert Impact Check

1. **Did Citizen Report #af733f86 directly create an alert?**  
   **NO.** There was no open `PollutionEvent` in H3 cell `88608e26a7fffff`. Per F6 design principle, citizen reports never fabricate pollution events or alert candidates (`CitizenReportService.java` log: *"No active PollutionEvent found... Report stored unattached"*).
2. **Did Citizen Report #07b813e2 directly create an alert?**  
   **NO.** Report `07b813e2` was attached to open event `58ef2f64-4bc5-496f-9094-e5f61e44f8b0` as:
   - `dataSource = CITIZEN`
   - `relevanceTier = AUXILIARY`
   - `confidenceScore = 0.1`
   Upon running `EvidenceOrchestrationService`, the F5 triage state evaluated to:  
   `com.aerosentinel.alert.AlertService: F5 Triage state is 'INSUFFICIENT_EVIDENCE' for event id=58ef2f64...; no authority alert candidate created`.
3. **Current Alert Counts:** 0. No alerts were created or modified by F6.

---

## Section L: Suspected Data Reset / Switch Analysis

- **Was the database recreated?** **NO.** `flyway_schema_history` records migrations dating continuously from `2026-09-24` through `2026-09-28`.
- **Was the database switched or migrated?** **NO.** Running database remains `aerosentinel` on localhost port 5432.
- **Why are alerts at 0?**
  - In `pg_stat_user_tables`, `alerts` has `n_tup_ins = 14`, `n_tup_upd = 22`, `n_tup_del = 11`.
  - In `scratch/cleanup_test_cell.py`, `DELETE FROM grid_cells WHERE h3_index = '886196944dfffff'` was executed to clean up test cells.
  - Because `alerts` has an `ON DELETE CASCADE` constraint on `grid_cell_id` (`alerts_grid_cell_id_fkey`), deleting the test cell cascaded to the test alert candidates.
  - No production alert candidates were permanently seeded; alerts in AeroSentinel are produced exclusively when F5 triage reaches `ALERT_CANDIDATE` (score $\ge 0.55$).

---

## Section M: Recommended Next Actions (For Future Implementation)

1. **Citizen Submission Timeout Resolution:**
   - In `frontend/src/services/citizen.service.ts`, override the default Axios client timeout for `submitReport` to `30000` (30 seconds), or make the Gemini Vision analysis asynchronous (return 202 Accepted and poll `/citizen/reports/{id}`).
2. **Sidebar Badge Synchronization:**
   - Update `frontend/src/components/layout/Sidebar.tsx` to read the live count of open alerts from `useAuthorityQueue()` instead of using the hardcoded string `'1'`, or omit the badge when open count is 0.
3. **Forecast Recalibration / Generation:**
   - Trigger a refreshed forecast run via `POST /api/v1/forecast/generate` for cells where updated telemetry is desired so that their `generatedAt` timestamp falls within the `< 24h` threshold, transitioning them from `UNAVAILABLE` back to `LIVE`/`STALE`.

---

RUNTIME DIAGNOSTIC STATUS:
CLEAR
