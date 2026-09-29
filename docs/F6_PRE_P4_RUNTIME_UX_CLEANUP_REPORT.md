# AeroSentinel — Pre-F6-P4 Runtime UX Cleanup Report

**Phase:** Pre-F6-P4 Runtime UX Cleanup  
**Date:** 2026-09-29  
**Status:** PASS  

---

## 1. Executive Summary

This phase implements a minimal runtime and UX cleanup addressing the two identified operational friction points while leaving all core scoring, forecast thresholds, Gemini Vision inference, and alert lifecycle logic completely intact:
1. **Citizen Report Ingestion Timeout Override:** Increased request-level timeout for citizen report submissions (`submitReport`) from the default 10,000ms to **20,000ms**, accommodating backend synchronous Gemini Vision processing (with its existing 15-second watchdog) and eliminating false browser timeout banners. Global Axios timeout in `api.ts` remains strictly unchanged at 10,000ms.
2. **Dynamic Authority Alert Badge:** Replaced the hardcoded static `badge: '1'` in `frontend/src/components/layout/Sidebar.tsx` with a live count fetched safely through `useAuthorityQueue()`. When active unresolved alerts count is 0, no misleading `'1'` badge is rendered. When active alerts exist, the badge reflects the accurate live count matching authority queue semantics (excluding `RESOLVED` alerts). API failures default gracefully to 0 without crashing sidebar navigation.
3. **Forecast Intentionally Unchanged:** Maintained existing F4 multi-horizon temporal freshness contracts without synthetic data injection or model alterations (`<= 2h` LIVE, `> 2h && <= 24h` STALE, `> 24h` UNAVAILABLE).

---

## 2. Citizen Report Timeout Root Cause & Exact Fix

### Root Cause
- `frontend/src/services/api.ts` configures an Axios instance with `timeout: 10000` (10 seconds).
- Backend citizen report ingestion synchronously triggers the Python Gemini Vision pipeline (`vision_cli.py`), which uses an internal 15-second watchdog.
- Real Gemini inference with Google GenAI SDK can legitimately take between 11.5s and 15s depending on image processing and network round-trips.
- Because backend processing took $> 10.0$s (observed 11.89s and 15.04s), the client-side Axios request aborted with `"timeout of 10000ms exceeded"`, displaying a false error banner even though the report and its Gemini analysis were persisted successfully in PostgreSQL.

### Exact Fix
In [frontend/src/services/citizen.service.ts](file:///c:/Users/lenovo/AeroSential/frontend/src/services/citizen.service.ts#L13-L21):
- Added a per-request configuration override `{ timeout: 20000 }` to `apiClient.post('/citizen/reports', formData, ...)`.
- Kept the global Axios timeout in [frontend/src/services/api.ts](file:///c:/Users/lenovo/AeroSential/frontend/src/services/api.ts#L8) unchanged at `timeout: 10000`.
- The 20-second timeout safely encompasses the backend 15-second AI watchdog plus network overhead, ensuring neither real Gemini inference nor deterministic fallback trigger false frontend timeout errors.

```typescript
// frontend/src/services/citizen.service.ts
submitReport: async (formData: FormData): Promise<CitizenReport> => {
  const response = await apiClient.post<CitizenReport>('/citizen/reports', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    timeout: 20000,
  });
  return response.data;
},
```

---

## 3. Alerts Sidebar Badge Root Cause & Exact Fix

### Root Cause
- In [frontend/src/components/layout/Sidebar.tsx](file:///c:/Users/lenovo/AeroSential/frontend/src/components/layout/Sidebar.tsx), the navigation array `NAV_SECTIONS` previously contained a hardcoded badge value:
  ```typescript
  { label: 'Alerts', path: '/authority/alerts', icon: BellRing, badge: '1' }
  ```
- Because the PostgreSQL database currently contains 0 alert rows, the Alerts page accurately rendered `ALL: 0, OPEN: 0, ACKNOWLEDGED: 0, RESOLVED: 0`, creating a visible contradiction with the sidebar's static `'1'`.

### Exact Fix
In [frontend/src/components/layout/Sidebar.tsx](file:///c:/Users/lenovo/AeroSential/frontend/src/components/layout/Sidebar.tsx):
- Replaced the hardcoded `'1'` badge with dynamic computation reusing the existing `useAuthorityQueue` hook.
- Extracted and exported `getNavSections(activeAlertCount: number)` for clean isolation and unit testability.
- Filtered out `RESOLVED` alerts to preserve authoritative queue semantics: only active actionable alerts (`OPEN` and `ACKNOWLEDGED`) are reflected in the badge.
- When `activeAlertCount === 0`, `badge` evaluates to `undefined`, so no badge pill is rendered.
- If the authority alert API fails, `useAuthorityQueue` catches the error internally, `items` defaults to `[]`, `activeAlertCount` falls back to `0`, and the sidebar continues rendering safely without crashing.

```typescript
// frontend/src/components/layout/Sidebar.tsx
export const getNavSections = (activeAlertCount: number = 0): NavSection[] => [
  // ...
  {
    title: 'ACTION',
    items: [
      {
        label: 'Alerts',
        path: '/authority/alerts',
        icon: BellRing,
        badge: activeAlertCount > 0 ? String(activeAlertCount) : undefined,
      },
      { label: 'Authority', path: '/authority/incidents', icon: ShieldAlert },
      { label: 'Citizen Reports', path: '/citizen/report', icon: Camera },
    ],
  },
  // ...
];

export const Sidebar: React.FC = () => {
  const { sidebarOpen } = useApp();
  const location = useLocation();

  const { items } = useAuthorityQueue();
  const activeAlertCount = useMemo(() => {
    if (!Array.isArray(items)) return 0;
    return items.filter((item) => item.status && item.status !== 'RESOLVED').length;
  }, [items]);

  const navSections = useMemo(() => getNavSections(activeAlertCount), [activeAlertCount]);
  // ...
};
```

---

## 4. Forecast Contract: Intentionally Unchanged

The forecast persistence and display behavior was confirmed and left untouched:
- $\text{Age} \le 2\text{h} \implies \text{LIVE}$
- $2\text{h} < \text{Age} \le 24\text{h} \implies \text{STALE}$ (renders stale warning banner with persisted telemetry)
- $\text{Age} > 24\text{h} \implies \text{UNAVAILABLE}$ (renders `"Forecast Telemetry Unavailable"`)
- Unforecasted cells $\implies \text{NO\_DATA}$ (HTTP 404 handled gracefully)

No synthetic forecast records, fake timestamps, or model alterations were introduced.

---

## 5. Verification & Test Results

### 1. Focused Unit Tests (`frontend/src/utils/f6_pre_p4_ux_cleanup.test.ts`)
Created and executed 5 automated node tests:
- `F6-PRE-P4: 1. Citizen request timeout override is set to 20000ms while global api.ts remains 10000ms`: **PASS**
- `F6-PRE-P4: 2. Successful long-running citizen report request receives 20000ms timeout configuration`: **PASS**
- `F6-PRE-P4: 3. Alert badge count = 0 displays no misleading "1" in sidebar`: **PASS**
- `F6-PRE-P4: 4. Alert badge reflects live count and authority queue resolved semantics`: **PASS**
- `F6-PRE-P4: 5. Alert API failure gracefully defaults to 0 and does not crash sidebar navigation`: **PASS**

### 2. Full Frontend Test Suite
- Executed `npm test` across all `src/utils/*.test.ts`:
  - **Total Tests:** 163
  - **Passed:** 163
  - **Failed:** 0
  - **Suites:** 2

### 3. TypeScript Typecheck & Production Build
- Executed `npx tsc -b`: **Exit code 0** (no type errors).
- Executed `npm run build`: **Built successfully** in 35.81s (`dist/` generated cleanly).

---

## 6. Remaining Limitations

1. **Synchronous Vision Ingestion:** The citizen report submission remains synchronous from browser to backend to Gemini CLI. While 20 seconds avoids client-side timeout under normal API latencies, high network latency to Google Gemini could still approach the threshold. (Asynchronous submission with status polling can be considered for future roadmap phases).
2. **Forecast Freshness:** Monitored cells older than 24h will continue to display `"Forecast Telemetry Unavailable"` until new forecast inference runs are triggered via `POST /api/v1/forecast/generate`.

---

F6 PRE-P4 CLEANUP: PASS
