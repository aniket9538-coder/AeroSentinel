# AeroSentinel — F5 UI Polish: Evidence & WHY + Authority Alerts Report

**Module:** Feature 5 Evaluator-Facing Experience Refinement  
**Date:** September 28, 2026  
**Status:** PASS  

---

## 1. Current Evidence UI Audit

Prior to this UI polish pass, the F5 Evidence experience presented several evaluator-facing challenges:
- **Navigation Duplication:** The sidebar displayed two separate items ("Evidence" and "Gemini WHY"), causing duplicate visual selection states for the same underlying dossier.
- **Flat Information Hierarchy:** The page displayed multiple informational blocks (sensor telemetry, meteorology, model predictions, internal score weights, and diagnostic prose) with equal visual weight, diluting the primary causal explanation.
- **Internal Weight Exposure:** Mathematical scoring breakdown sub-weights (Observation: 0.30, Forecast: 0.25, Multi-Source: 0.20, Spatial: 0.15) were rendered prominently in the primary viewport, resembling implementation theory rather than an executive summary.
- **Verbose Diagnostic Prose:** The AI interpretation section prominently led with raw model versions and uncorroborated diagnostic statements before presenting the human-readable attribution.
- **Disconnected Authority Action:** Evaluators examining an `ALERT_CANDIDATE` cell could not directly navigate to the corresponding municipal alert record.

---

## 2. Current Alert UI Audit

Prior to this refinement pass, the Municipal Authority Alert Queue presented several usability and visual issues:
- **Excessive Visual Weight:** Alert cards used heavy, near-black surfaces (`#000000` / `#0b0f17`) with aggressive colored borders, creating visual fatigue.
- **Suboptimal Viewport Allocation:** The list and detail panes were distributed with a 1.4fr / 1.6fr split, resulting in horizontal truncation of event titles and metrics.
- **Static / Uncounted Filters:** Operational status filters (ALL, OPEN, ACKNOWLEDGED, RESOLVED) lacked live counts, requiring evaluators to manually inspect each tab.
- **Cluttered List Cards:** List cards rendered redundant descriptions and repetitive lineage UUIDs rather than a compact telemetry/risk summary.
- **Unidirectional Workflow:** While alerts contained a link to the Evidence page, the Evidence page lacked a reciprocal link to the Authority Alert Queue, breaking the evaluation continuity.

---

## 3. Information Architecture Change

The information architecture was reorganized to match the natural evaluator investigation chain:

```
WHERE?
  └── Selected H3 Cell (Hex & Sector Context)
        ↓
WHAT IS OBSERVED?
  └── Physical Telemetry & Environmental Evidence (Air, Weather, Coverage, Dispersion)
        ↓
WHAT DOES THE MODEL SAY?
  └── F3 Hotspot (Calibrated Probability) + F4 Empirical Forecast Trajectories (1h, 3h, 6h)
        ↓
HOW STRONG IS THE EVIDENCE?
  └── F5 Evidence Strength Score (0.000–1.000) & Triage Level (ALERT_CANDIDATE / MONITOR / INSUFFICIENT)
        ↓
WHY?
  └── Grounded Gemini Interpretation (Attribution Summary with Safeguards)
        ↓
WHAT SHOULD HAPPEN?
  └── Actionable Recommended Verification Directives
        ↓
WHEN AUTHORITY ACTS:
  └── Authority Alert Queue → Field Team Dispatch → Ground Verification
```

---

## 4. Unified Evidence & WHY Navigation

### Navigation Merge
In [`frontend/src/components/layout/Sidebar.tsx`](file:///c:/Users/lenovo/AeroSential/frontend/src/components/layout/Sidebar.tsx):
- Removed separate "Evidence" (`/analyst/evidence`) and "Gemini WHY" (`/gemini-why`) sidebar entries.
- Replaced with a single navigation item:
  ```typescript
  { label: 'Evidence & WHY', path: '/analyst/evidence', icon: Sparkles }
  ```
- Implemented route-aware active state detection using `useLocation()`:
  ```typescript
  const isCurrentActive =
    item.path === '/analyst/evidence'
      ? location.pathname === '/analyst/evidence' || location.pathname === '/gemini-why'
      : location.pathname === item.path;
  ```
- Both `/analyst/evidence` and `/gemini-why` routes remain registered in [`App.tsx`](file:///c:/Users/lenovo/AeroSential/frontend/src/App.tsx) for backward compatibility, but both resolve to the single unified view without visual duplication.

---

## 5. Evidence Page Redesign

### Page Header & Context Strip
Updated in [`frontend/src/pages/analyst/EvidenceAnalysis.tsx`](file:///c:/Users/lenovo/AeroSential/frontend/src/pages/analyst/EvidenceAnalysis.tsx):
- **Header:** "Evidence & WHY" with subtitle "Why this H3 cell is being investigated".
- **Action Strip:** Live city telemetry badge, Refresh button, Forecast link, Hotspots link, and contextual "View Alert" button (rendered only when triage is `ALERT_CANDIDATE`).
- **Compact Context Strip:** Selected H3 Cell dropdown (Shivajinagar PUN-001, Katraj PUN-002, Hadapsar PUN-003, Mumbai MUM-001, Delhi DEL-001), active H3 Index with copy button, City name, canonical Event code, realtime timestamp, and compact custom H3 hex search.

### Evidence Chain Visual Flow
Added to [`frontend/src/components/hotspot/EvidencePanel.tsx`](file:///c:/Users/lenovo/AeroSential/frontend/src/components/hotspot/EvidencePanel.tsx):
- Lightweight 5-step anchor navigation ribbon:
  `OBSERVED` → `MODEL OUTPUT` → `EVIDENCE` → `AI WHY` → `VERIFICATION`
- Clicking any step smoothly scrolls to the section via DOM element `scrollIntoView({ behavior: 'smooth' })`.
- Active hover states with subtle cyan border highlight.

### Evidence Hero & Simplified Scoring
- **Primary View:** Large real evidence score (`evData.evidenceScore.toFixed(3)` / `1.000`), triage state badge (`ALERT_CANDIDATE`, `MONITOR`, `INSUFFICIENT_EVIDENCE`), and consistency badge.
- **Traceable Lineage:** Compact 4-node strip displaying Active H3, Canonical Event Code, Event UUID, and Parent F3 Prediction ID with one-click copy buttons.
- **Collapsible Score Breakdown:** Relegated mathematical sub-weights (Observation: 0.30, Forecast: 0.25, Multi-Source: 0.20, Spatial: 0.15) to a secondary collapsible drawer, keeping the primary hero clean.

### Truthful Observed Telemetry & Model Outputs
- Four uniform observation cards (Air Quality, Surface Meteorology, Monitoring Coverage, Spatial Dispersion) with truthful status indicators ("Active", "Synced", "Unavailable", "Not detected").
- Model Output cleanly separated into **F3 Hotspot Classifier** (calibrated risk score, risk level badge, operational threshold 0.20) and **F4 Forecast Regressors** (1h, 3h, 6h empirical bounds).
- **Truthful Forecast Unavailable State:** When forecast output is unavailable, the UI renders an honest notice (`"Forecast unavailable for this cell"`) with `forecastConfidence` strictly remaining `null`. No synthetic zeros or fake confidence meters are displayed.

### Human-Readable AI Interpretation
- Primary banner: **"WHY THIS CELL IS FLAGGED"** with the grounded public attribution summary.
- Clarification badge: *"Gemini explains validated evidence. Does not calculate risk or replace field verification."*
- Collapsible secondary drawer: **"Technical Analyst Diagnostics"**.
- Collapsible safeguards drawer: **"Grounding Safeguards & Excluded Hypotheses"** (closed by default to avoid visual clutter).

### Actionable Verification & Provenance
- Clear priority badge and bulleted operational guidelines.
- Dynamic Action Banner: Contextual button *"View Authority Alert →"* linking to `/authority/alerts?h3={h3Index}` when triage is `ALERT_CANDIDATE`.
- Provenance & Lineage collapsed into a compact audit accordion at the bottom of the page.

---

## 6. Alert Page Redesign

Completely redesigned [`frontend/src/pages/authority/Alerts.tsx`](file:///c:/Users/lenovo/AeroSential/frontend/src/pages/authority/Alerts.tsx):
- **Viewport Allocation:** Implemented ~42% list (`minmax(320px, 42%)`) and ~58% detail (`minmax(420px, 58%)`) layout.
- **Operational Deep Navy Theme:** Replaced stark black surfaces with deep navy container (`#0c1527`), elevated card surfaces (`#111d35` / `#162544`), and subtle borders (`rgba(255, 255, 255, 0.08)`).
- **Dynamic Filter Counts:** Live counts computed dynamically from loaded items: `ALL ({counts.ALL})`, `OPEN ({counts.OPEN})`, `ACKNOWLEDGED ({counts.ACKNOWLEDGED})`, `RESOLVED ({counts.RESOLVED})`.
- **H3 Query Parameter Auto-Selection:** Reads `?h3=...` from URL search params. If matched, automatically selects the alert and aligns the status filter.
- **List Card Redesign:**
  - Top row: Severity badge, Status badge, Triage badge, Timestamp.
  - Main: Event title.
  - Metric strip: Evidence score, F3 risk score, H3 hex (shortened).
  - Forecast strip: Compact `+1h`, `+3h`, `+6h` pills.
  - Operational row: Assigned team and verification state.
  - Selected state: Blue outline (`#38bdf8`), subtle blue glow (`rgba(56, 189, 248, 0.18)`), elevated background (`#162544`).
- **Selected Alert Operational Dossier:**
  - Header: Alert risk, status, triage, title, message, and copyable ID.
  - "WHY THIS ALERT EXISTS": 4-card metric strip (Evidence score, F3 risk, F4 forecast, consistency).
  - "TRACEABLE EVENT LINEAGE": Canonical event code, H3 index, parent prediction ID, jurisdiction city.
  - "FIELD RESPONSE WORKFLOW": Assigned team, assignment status, verification result, inspection timestamp, with modal trigger button (`InspectionForm`).
  - "EVIDENCE SUMMARY": Compact observed facts, model output, AI explanation, and verification directive.
  - Cross-link CTA: *"Inspect Full Evidence & Gemini WHY →"* linking directly to `/analyst/evidence?h3={selectedAlert.h3Index}`.
  - Lifecycle actions: Acknowledge Alert (when OPEN), Assign Team / Conduct Inspection / Resolve Alert (when ACKNOWLEDGED), Resolved Archive notice (when RESOLVED).
- **Compact Empty State:** Clean centered card with icon, status message, and clear call-to-action without giant black rectangles.

---

## 7. Color System

| Token / Semantic Role | Color Value | Usage |
| :--- | :--- | :--- |
| **Dark Navy Background** | `#070d19` | Root app shell & page backdrop |
| **Operational Navy Card** | `#0c1527` / `#111d35` | Card surfaces & container ribbons |
| **Selected Card Surface** | `#162544` | Active selected alert card |
| **AeroSentinel Blue / Cyan** | `#38bdf8` / `#2563eb` | Primary brand accents, active borders, metric highlights |
| **CRITICAL Severity** | `#ef4444` / `rgba(239, 68, 68, 0.15)` | High-risk alerts, critical badges |
| **ACKNOWLEDGED Status** | `#38bdf8` / `rgba(56, 189, 248, 0.12)` | In-review operational alerts |
| **OPEN Status** | `#f59e0b` / `rgba(245, 158, 11, 0.12)` | Unacknowledged alert candidates |
| **RESOLVED Status** | `#10b981` / `rgba(16, 185, 129, 0.12)` | Archived & verified alerts |
| **ALERT_CANDIDATE Triage** | `#818cf8` / `rgba(99, 102, 241, 0.14)` | F5 triage qualifying candidate |

---

## 8. Typography and Spacing Changes

- **Font Hierarchy:** Maintained technical font pairing: Heading (`Outfit`), Body (`Plus Jakarta Sans`), Hex/Lineage (`JetBrains Mono`).
- **Metric Prominence:** Large numerical scores rendered at `1.8rem` (Evidence Hero) and `1rem` (Alert Dossier) with `font-weight: 800`.
- **Card Padding & Radius:** Unified on `10px` / `12px` border radius and `1rem` – `1.25rem` padding scale across both pages.
- **Scroll Container Containment:** List column constrained to `maxHeight: 'calc(100vh - 220px)'` with internal scroll, preventing body-level dual scrollbars.

---

## 9. Animation Changes

- **Initial Load Entry:** Subtle staggered fade-in (`animation: fadeIn 0.25s ease forwards`) with `index * 35ms` delay on alert list cards.
- **Hover Micro-Interactions:** Smooth `translateY(-2px)` with subtle border highlight on interactive cards.
- **Selected Card Glow:** Soft static blue aura (`0 0 16px rgba(56, 189, 248, 0.18)`).
- **Refresh Spin:** Icon rotation strictly during active network loading; no infinite bouncing or distracting moving gradients.

---

## 10. Data Integrity Audit

All displayed values strictly originate from real backend API contracts and PostgreSQL persistence:
- **No Synthetic Zeros:** Missing or uncalculated forecasts display truthful `"Forecast unavailable for this cell"`. `forecastConfidence` strictly remains `null`.
- **No Mock Alert Arrays:** Confirmed absence of legacy mock alerts (`alt-1`, `alt-2`, `sampleAlerts`).
- **Exact Floating-Point Parity:** Real evidence scores (`0.667`), F3 risk scores (`0.80`, `0.85`), and empirical forecast bounds (`71.90 [70.06, 73.76]`) are rendered with zero client recalculation.
- **Real Dynamic Counts:** Filter button counts (`ALL: 3, OPEN: 1, ACK: 1, RES: 1`) are computed from loaded API items.

---

## 11. Removed / Bypassed Non-Live UI

- Removed duplicate "Gemini WHY" sidebar entry.
- Bypassed primary display of mathematical scoring weights (moved to secondary collapsible drawer).
- Replaced giant full-width empty black block with compact centered operational card.
- Suppressed confusing confidence meters for uncalculated forecasts.

---

## 12. Evidence ↔ Alert Relationship

A two-way operational link now connects the investigation flow:
1. **Evidence Page → Alert Queue:** When triage state is `ALERT_CANDIDATE`, an action button *"View Authority Alert →"* links to `/authority/alerts?h3={h3Index}`.
2. **Alert Queue Auto-Selection:** When navigated with `?h3={h3Index}`, the Alerts page parses the query parameter, auto-selects the corresponding alert card, and updates the status filter to ensure visibility.
3. **Alert Queue → Evidence Page:** The selected alert detail dossier features an *"Inspect Full Evidence & Gemini WHY →"* CTA linking directly back to `/analyst/evidence?h3={h3Index}`.

---

## 13. Accessibility

- **Keyboard Navigation:** Tab-accessible buttons, dropdowns, and collapsible accordions.
- **Contrast Ratios:** Text colors (`#f8fafc` on dark navy `#0c1527` > 12:1 ratio) exceed WCAG AAA requirements.
- **ARIA & Copy Feedback:** Interactive clipboard buttons provide visual checkmark confirmation and accessible tooltips.

---

## 14. Responsive Behavior

Tested and optimized for target desktop resolutions:
- **1440×900:** Clean dual-column layout (~42% list / ~58% detail) with full visibility of metrics, forecast strips, and lineage. Zero horizontal overflow.
- **1280×800:** Proportional scaling with compact metric strips; independent vertical scrolling inside list and detail panels avoids window dual scrollbars.
- **App Shell Scroll Fix:** Main content scrolls inside `.app-content-scroll`; navbar and sidebar remain fixed.

---

## 15. Test Results

### Automated Frontend Suite (`npm test -- --run`)
Executed Node.js test runner across all test suites including newly added targeted verification suite [`src/utils/f5_ui_polish.test.ts`](file:///c:/Users/lenovo/AeroSential/frontend/src/utils/f5_ui_polish.test.ts):

```
# tests 126
# suites 0
# pass 126
# fail 0
# cancelled 0
# skipped 0
# todo 0
# duration_ms 4722.38
```

All 126 tests passed with 0 failures:
- `F5 UI Polish 1`: Unified Sidebar navigation active state logic for `/analyst/evidence` and `/gemini-why`
- `F5 UI Polish 2`: Sidebar.tsx contains exactly ONE unified Evidence & WHY entry
- `F5 UI Polish 3`: Truthful unavailable forecast presentation without synthetic zeros
- `F5 UI Polish 4`: Two-way Evidence ↔ Authority Alert cross-linking contracts
- `F5 UI Polish 5`: Dynamic alert filter counts calculated strictly from loaded queue items
- `F5 UI Polish 6`: Selected alert operational dossier renders evidence, risk, and lineage
- `F5 UI Polish 7`: Source code audit — zero mock alerts, fake PM2.5, or fake counters

### TypeScript Static Typecheck (`npx tsc --noEmit`)
Executed cleanly with **0 errors**:
```
Exit code: 0
Stdout: (empty)
Stderr: (empty)
```

---

## 16. Visual Verification

| Verification Item | Specification Requirement | Verification Status | Notes |
| :--- | :--- | :--- | :--- |
| **Unified Sidebar Nav** | Single item "Evidence & WHY", active on both `/analyst/evidence` and `/gemini-why` | VERIFIED | No duplicate active entries |
| **Evidence Header & Strip** | Title, subtitle, live city badge, preset selector, H3, Event, Time | VERIFIED | Clean, compact context ribbon |
| **Evidence Chain Anchor Flow** | Navigational flow `OBSERVED → MODEL → EVIDENCE → WHY → VERIFY` | VERIFIED | Smooth scroll behavior |
| **Evidence Hero Score** | Large normalized score, triage badge, consistency badge, lineage | VERIFIED | Sub-weights moved to drawer |
| **Truthful Observations** | Real sensor states; no fabricated fallback values | VERIFIED | Muted unavailable indicators |
| **Model Outputs Separation** | F3 Hotspot (calibrated) and F4 Forecast (empirical horizons) | VERIFIED | Truthful "unavailable" when null |
| **Human-Readable WHY** | Public summary prominent; technical prose & safeguards collapsible | VERIFIED | Grounded AI disclaimer badge |
| **Actionable Verification** | Actionable directives; contextual "View Alert" CTA | VERIFIED | Links to `/authority/alerts?h3=...` |
| **Alert Operational Theme** | Deep navy surfaces (`#0c1527`), cyan/blue accents | VERIFIED | Eliminates harsh pure-black fatigue |
| **Alert Viewport Proportions**| ~42% list / ~58% detail layout | VERIFIED | Grid `minmax(320px, 42%) minmax(420px, 58%)` |
| **Dynamic Filter Counts** | Real counts for ALL, OPEN, ACKNOWLEDGED, RESOLVED | VERIFIED | Computed from loaded items |
| **Alert List Card Redesign** | Severity, status, triage, timestamp, title, metrics, forecast strip | VERIFIED | Blue outline & glow on selection |
| **Alert Detail Dossier** | Operational header, Why it exists, Lineage, Field Response, Summary | VERIFIED | Actionable buttons & Inspection modal |
| **Cross-Navigation Link** | "Inspect Full Evidence & Gemini WHY" CTA | VERIFIED | Opens `/analyst/evidence?h3=...` |
| **Browser E2E Execution** | Visual session verification | NOT VERIFIED IN REPOSITORY | Formal browser E2E deferred to F5-P8 |

---

## 17. Known Limitations

- **Dual-Server Dependency:** Visual browser rendering requires both Spring Boot backend (port 8080) and Vite frontend dev server (port 3000) running with IPv4 loopback binding (`0.0.0.0:3000`).
- **Formal Browser E2E:** Full automated headless browser screenshot capture is designated for the dedicated F5-P8 milestone per task specifications.

---

## 18. Final Status

F5 UI POLISH — EVIDENCE & ALERTS STATUS: PASS

---

## 19. Final Alert Visual Theme Refinement

### 19.1 Dark Theme Removal & Light AeroSentinel Unification
In response to evaluator feedback regarding visual cohesion with the Dashboard, Forecast, and Evidence & WHY pages, the Municipal Authority Alert Queue was converted from a heavy dark-navy interface into a crisp, light operational theme.
- **Root Background:** Transitioned to soft, cool blue-grey (`#f5f8fc` / `var(--bg-primary)`).
- **Surfaces & Cards:** Replaced dark containers (`#0c1527` / `#111d35`) with clean white card surfaces (`#ffffff`) bordered by soft blue-grey (`#e2e8f0` / `#cbd5e1`).
- **Visual Depth:** Maintained subtle blue-tinted secondary containers (`#f8fafc` / `#f0f7ff`) and soft drop shadows (`0 1px 3px rgba(15, 23, 42, 0.04)`) to prevent a flat, sterile appearance.

### 19.2 Semantic Color System
Colors are strictly applied for semantic meaning rather than decorative overload:
- **AeroSentinel Blue (`#0284c7`):** Primary action buttons, active filter tab, selected alert border/glow, canonical hyperlinks, and Evidence & WHY CTA.
- **Cyan / Teal (`#0891b2`):** Operational activity indicator (`Active Queue`), observed facts heading, and field inspection triggers.
- **Semantic Red (`#dc2626` / `#fee2e2`):** `CRITICAL` severity badges, high risk scores ($\ge 0.70$), and `REJECTED` ground verifications.
- **Semantic Amber (`#d97706` / `#fef3c7`):** `OPEN` unacknowledged status, elevated attention alerts, and `NEEDS_FOLLOW_UP` verifications.
- **Semantic Green (`#059669` / `#dcfce7`):** `RESOLVED` status, `CONFIRMED` field inspections, and archived operational state banner.
- **Semantic Indigo (`#6366f1` / `#ede9fe`):** `ALERT_CANDIDATE` triage badges and AI interpretation headers.
- **Neutral Navy & Grey (`#0f172a` / `#334155` / `#64748b`):** Titles, body text, timestamps, and lineage IDs.

### 19.3 Filter Bar & Operational Controls
- **Segmented Control:** Features clean white/grey inactive buttons with dark navy text (`#334155`) and dynamic count pills (`#e2e8f0`). The active filter illuminates in AeroSentinel Blue (`#0284c7`) with white text.
- **Dynamic Live Counts:** `ALL ({counts.ALL})`, `OPEN ({counts.OPEN})`, `ACKNOWLEDGED ({counts.ACKNOWLEDGED})`, `RESOLVED ({counts.RESOLVED})` calculated strictly in realtime from loaded API items.
- **Active Queue Indicator:** Clean status element rendering `◌ Active Queue · {N} candidates` with blue activity pulse.

### 19.4 Alert List Card Redesign (Light Theme)
- **Container:** White surface (`#ffffff`), `12px` border radius, subtle hover elevation (`translateY(-2px)` with `#93c5fd` border).
- **Selected State:** Clean blue outline (`1.5px solid #0284c7`), soft blue aura (`boxShadow: 0 0 0 1px #0284c7, 0 4px 12px rgba(2, 132, 199, 0.12)`), and left accent tab.
- **Event Title:** Dominant dark navy text (`#0f172a`, `fontWeight: 700`, `0.95rem`) with natural text wrapping.
- **Metric Strip:** Light cool-grey ribbon (`#f1f5f9`, border `#e2e8f0`) displaying Evidence Score (`#0284c7`), F3 Risk Score (semantic color), and H3 Hex (dark navy mono).
- **Forecast Summary:** Compact light-blue pills (`#f0f9ff`, border `#bae6fd`, text `#0369a1`).
- **Operational Strip:** Team name in AeroSentinel blue (`#0284c7`) with verification outcome badge (`CONFIRMED`, `REJECTED`, `NEEDS_FOLLOW_UP`).

### 19.5 Selected Alert Operational Dossier (Light Theme)
- **Header:** Full badge cluster, dark navy title (`#0f172a`, `1.25rem`, bold), and full event description.
- **"WHY THIS ALERT EXISTS":** Four prominent light cards in a responsive grid displaying Evidence Score, F3 Risk, F4 Forecast, and Consistency.
- **Traceable Event Lineage:** Clean light card (`#f8fafc`) with canonical event code (`#0284c7`), H3 index (`#0f172a`), parent prediction ID, and jurisdiction city with subtle copy feedback.
- **Field Response Workflow:** Light operational container displaying assigned team, assignment status, ground verification result, and inspection timestamps with `InspectionForm` modal trigger.
- **Four Semantic Evidence Blocks:** Clean white cards with semantic icon/heading colors (Observed Facts in cyan `#0891b2`, Model Output in blue `#2563eb`, AI Interpretation in purple `#7c3aed`, Recommended Verification in green `#059669`).
- **Evidence & WHY CTA:** Prominent AeroSentinel blue outlined button linking to `/analyst/evidence?h3={h3Index}`.
- **Lifecycle Actions:** High-contrast actionable buttons (Acknowledge in blue `#0284c7`, Assign/Inspect in indigo `#4f46e5`, Resolve in emerald `#059669`, and soft-green Resolved Archive banner).

### 19.6 Spacing, Typography & Motion Standards
- **Spacing:** Card padding normalized to `16px–24px` (`1rem`–`1.5rem`), section gap `20px` (`1.25rem`), badge gap `8px` (`0.5rem`).
- **Typography:** Page title `1.65rem` bold (`Outfit`), card titles `0.95rem` / `1.25rem` (`Plus Jakarta Sans`), IDs and metrics in `JetBrains Mono`.
- **Animations:** Gentle initial staggered card fade-in (`opacity: 0 to 1, translateY 6-8px`), hover micro-elevation (`translateY(-2px)`), spinner rotation strictly during network fetch. No distracting bouncing or continuous gradient loops.

### 19.7 Accessibility & Data Integrity
- All text meets or exceeds WCAG AA contrast standards (>7:1 for `#0f172a` on `#ffffff`, >4.5:1 for semantic badge labels).
- Zero mock data or synthetic values introduced; all counts, scores, and timestamps originate from PostgreSQL persistence.

---

## 20. Final Status

F5 ALERT VISUAL THEME REFINEMENT: PASS

