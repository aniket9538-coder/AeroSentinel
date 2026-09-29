# AeroSentinel — F4-P5 Frontend Architecture & Integration Audit

## 1. Executive Summary

Phase **F4-P5 (Forecast Frontend Integration)** bridges the locked Spring Boot multi-horizon PM2.5 forecast backend with the React frontend.
This document fulfills **Phase 1 (Audit Existing Frontend)** by inspecting the current frontend architecture, locating reusable components, defining API integration points, establishing the H3 selection pattern, and listing files to modify and protect.

---

## 2. Current Forecast-Related Files

| File Path | Current State | Audit Finding |
| :--- | :--- | :--- |
| `frontend/src/pages/public/Forecast.tsx` | Mock/Placeholder | Uses hardcoded `sampleForecast` with 1–6h synthetic values, fake XGBoost model tag, and fake confidence scores (0.92–0.68). Must be connected to the real `GET /api/v1/forecast/{h3Index}` endpoint. |
| `frontend/src/pages/analyst/ForecastAnalysis.tsx` | Wrapper Component | Simply renders `<Forecast />`. No changes needed. |
| `frontend/src/components/forecasting/ForecastSummary.tsx` | Partial / Legacy | Contains 4 KPI cards defaulting to mock 6h forecast and calculating an average confidence percentage. Must be updated to support $[1, 3, 6]$ horizons and strictly render "Forecast confidence: Not available" when `forecastConfidence` is null. |
| `frontend/src/components/forecasting/ForecastTimeline.tsx` | Partial / Legacy | Renders hourly cards defaulting to mock 1–6h array. Must be updated to render the 3 authoritative horizons ($T+1\text{h}, T+3\text{h}, T+6\text{h}$) with predicted PM2.5, lower/upper bounds, target times, and delta vs base. |
| `frontend/src/components/forecasting/ForecastConfidence.tsx` | Legacy Mock | Hardcoded with `confidence = 0.88`. Must be updated or replaced to handle neutral unavailable state honestly without fabricating confidence. |
| `frontend/src/components/charts/ForecastChart.tsx` | Recharts Implementation | Features observed + forecast composition, but currently defaults to synthetic 1–6h points and synthetic confidence. Must plot base timestamp $T_0$ and the authoritative horizons ($1\text{h}, 3\text{h}, 6\text{h}$) with empirical uncertainty envelope from backend `lowerBound`/`upperBound`. |
| `frontend/src/services/forecast.service.ts` | Minimal Skeleton | Calls `/forecast/{h3Index}` but uses old `CellForecast` mock type. Needs updating to return strict typed `ForecastResponse`. |
| `frontend/src/types/index.ts` | Mixed Types | Defines legacy `HourlyForecast` and `CellForecast`. Dedicated `types/forecast.ts` will be introduced. |

---

## 3. Reusable Project Components & Design System

The application uses a dark glassmorphic design system configured in `frontend/src/index.css`. The following components are available for reuse:

- **Layout**:
  - `PageContainer` (`frontend/src/components/layout/PageContainer.tsx`): Header, title, subtitle, action buttons.
  - `Navbar` & `Sidebar` (`frontend/src/components/layout/`): Topbar and navigation.
- **Common Controls**:
  - `Card` (`frontend/src/components/common/Card.tsx`): Card container with title, subtitle, badges.
  - `Badge` (`frontend/src/components/common/Badge.tsx`): Status badges (`success`, `warning`, `danger`, `info`, `neutral`).
  - `Button` (`frontend/src/components/common/Button.tsx`): Primary, secondary, outline, danger variants.
  - `EmptyState` (`frontend/src/components/common/EmptyState.tsx`): Handles `NO_DATA` or unselected states.
  - `ErrorState` (`frontend/src/components/common/ErrorState.tsx`): Handles API/network failure with retry.
  - `Loading` (`frontend/src/components/common/Loading.tsx`): Animated spinner with custom text.
- **Visuals & Icons**:
  - `lucide-react`: `TrendingUp`, `Clock`, `RefreshCw`, `Layers`, `ShieldCheck`, `AlertTriangle`, `CheckCircle2`, `Info`, `Calendar`, `Sparkles`, `Cpu`, `Hexagon`.
- **Design Tokens**:
  - `--brand-primary`: Emerald `#10b981`
  - `--accent-amber`: Amber `#f59e0b`
  - `--accent-rose`: Rose `#f43f5e`
  - `--accent-teal`: Teal `#14b8a6`
  - `--bg-surface`, `--bg-surface-elevated`, `--bg-glass`
  - `--border-subtle`, `--border-medium`
  - `--text-primary`, `--text-secondary`, `--text-muted`

---

## 4. API & Route Integration Points

### API Integration Point:
- Backend Controller: `com.aerosentinel.forecast.ForecastController` (`@RequestMapping("/api/v1/forecast")`).
- Read Endpoint: `GET /api/v1/forecast/{h3Index}`.
- Generation Endpoint: `POST /api/v1/forecast/generate` (for explicit generation/refresh).
- Client Instance: `frontend/src/services/api.ts` (`apiClient` configured with `baseURL: '/api/v1'`).

### Route Integration Point:
- Public Route: `/forecast` $\to$ `<Forecast />` in `frontend/src/App.tsx` (Sidebar link: "Predictive Forecast").
- Analyst Route: `/analyst/forecast` $\to$ `<ForecastAnalysis />` in `frontend/src/App.tsx`.

---

## 5. H3 Cell Selection Integration Mechanism

1. **City Context**: `AppContext` manages `selectedCity` (e.g. Pune, Delhi, Mumbai).
2. **Cell Selection Source**:
   - The user selects an H3 cell from the city's active monitoring cells (e.g. Pune Shivajinagar `88608850e5fffff`), or picks from an H3 selector dropdown / hotspot cell list.
   - Default/initial selection: If Pune is selected, auto-select `88608850e5fffff` (the real PMR CAAQMS station cell where authoritative F3 parent prediction `a310c689-f340-49fc-8935-a037de8d7709` and F4 forecasts reside).
3. **Data Flow**:
   $$\text{selectedH3Index} \xrightarrow{\text{useForecast(h3Index)}} \text{GET /api/v1/forecast/{h3Index}} \xrightarrow{\text{ForecastResponse}} \text{UI}$$

---

## 6. Exact Files Planned for Modification

1. `frontend/src/types/forecast.ts` (CREATE): Typed interfaces matching backend `ForecastResponse`, `ForecastItemDto`, `ForecastFreshness`.
2. `frontend/src/services/forecastApi.ts` (CREATE): Typed API client invoking `GET /api/v1/forecast/{h3Index}` and `POST /api/v1/forecast/generate`.
3. `frontend/src/hooks/useForecast.ts` (CREATE): Custom React hook managing forecast fetching, loading, error, stale, and refresh states.
4. `frontend/src/components/forecasting/ForecastSummary.tsx` (UPDATE): Replaces fake forecast fallbacks; displays observed PM2.5, peak forecast, and factual "Forecast confidence: Not available" status.
5. `frontend/src/components/forecasting/ForecastTimeline.tsx` (UPDATE): Displays the 3 authoritative horizons ($T+1\text{h}, T+3\text{h}, T+6\text{h}$) with predicted values, lower/upper bounds, target times, and risk levels.
6. `frontend/src/components/charts/ForecastChart.tsx` (UPDATE): Recharts line + area composition displaying $T_0$ base timestamp, $1\text{h}, 3\text{h}, 6\text{h}$ points, and empirical $P10$–$P90$ uncertainty bands.
7. `frontend/src/pages/public/Forecast.tsx` (UPDATE): Complete real integration with H3 selection, real backend data, loading state, `NO_DATA` state, error state, and model metadata.
8. `frontend/src/services/forecast.service.ts` (UPDATE): Re-export from `forecastApi.ts` for backward compatibility.
9. `frontend/src/tests/forecast.test.ts` (CREATE): Automated frontend test suite verifying contract parsing, null confidence rendering, horizon counts, bounds, and UI states.

---

## 7. Files Explicitly NOT Modified

- **All F3 Hotspot Detection Java & Python code** (LOCKED).
- **All Database schema and Flyway migration SQL** (LOCKED).
- **`ai-service/models/artifacts/forecast_regressors_v1.joblib`** (IMMUTABLE).
- **Python P2 Feature Builder & P3 Inference Engine** (LOCKED).
- **Spring Boot P4 backend classes** (`Forecast.java`, `ForecastService.java`, etc.) (LOCKED).
- **All other frontend pages** (`Home.tsx`, `Dashboard.tsx`, `AirQuality.tsx`, `WeatherSpatial.tsx`, `CitizenReport.tsx`, `EvidenceAnalysis.tsx`, etc.).
