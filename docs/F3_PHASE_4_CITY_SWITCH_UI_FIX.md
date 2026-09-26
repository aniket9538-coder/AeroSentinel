# F3 Phase 4 — City Switch + Selected Hotspot + Map Synchronization Report

## 1. Overview & Root Cause Analysis

### Identified Root Causes

1. **State Persistence on City Switch (`useHotspots.ts`)**:
   - When switching cities (e.g. Pune → Mumbai), `useHotspots` did not clear `selectedH3Index` or `selectedCell`.
   - The query resolved new cells for Mumbai, but the auto-selection effect in `Hotspots.tsx` checked `!selectedH3Index`. Because `selectedH3Index` still held the Pune cell index, auto-selection was bypassed, and `selectedCell` remained tied to Pune.
   - During in-flight loading, `selectedCell` continued showing stale previous-city data instead of a loading state or resetting.

2. **Asynchronous Race Conditions (Out-of-Order Responses)**:
   - Rapid switching across cities (Pune → Mumbai → Delhi) lacked an active request sequence tracker (`activeRequestIdRef`). A delayed response from an earlier city could overwrite the current city's state.

3. **Map Geometry & Viewport Decoupling (`PollutionMap.tsx`)**:
   - `MapViewController` only accepted raw `center` coordinates and ignored the dynamic H3 spatial boundaries of the loaded cells.
   - It did not compute or fit the spatial bounding box (`fitBounds`) of the active city's H3 cells, leaving the map viewport potentially off-center or clipped.
   - The selected cell could be outside the visible viewport without an automatic pan/fit.

4. **Polygon Visual Stacking (`H3RiskLayer.tsx`)**:
   - Adjacent H3 cells share borders. When a cell was selected, neighboring polygons rendered later in the SVG stack could partially overlap the selected cell's border styling.

---

## 2. Affected Files

| File | Changes Made |
|---|---|
| `frontend/src/hooks/useHotspots.ts` | Added immediate state clearing on city change, request sequencing guard (`activeRequestIdRef`), single-cell lookup sequence guard (`cellRequestIdRef`), and authoritative auto-selection of the first ranked cell (`cells[0]`) from overview cache without duplicate API requests. |
| `frontend/src/components/map/PollutionMap.tsx` | Added `calculateSpatialBounds` using `h3-js` polygon boundary vertices, upgraded `MapViewController` to fit map bounds to city H3 geometry, recenter on city switch, ensure selected cell is inside viewport without forcing awkward zoom, and accept `cityId`. |
| `frontend/src/components/map/H3RiskLayer.tsx` | Added memoized sorting to ensure the selected H3 polygon renders on top of the SVG canvas so its `#38bdf8` selected border styling is fully visible. |
| `frontend/src/pages/public/Hotspots.tsx` | Bound `cityId` to `PollutionMap`, updated `HotspotCellDetailsCard` to receive `isLoading={loading || loadingSelectedCell}` preventing stale details during city transitions, and added loading fallback indicators to summary metrics. |
| `frontend/src/utils/hotspot.test.ts` | Added 9 comprehensive automated tests verifying city switch reset, auto-selection, empty city handling, race condition discarding, table/card/map state consistency, spatial bounds calculations, and multi-city model metadata isolation. |

---

## 3. State Synchronization Design

### Unified Source of Truth
The four UI representations of a hotspot cell share a single synchronized state:
1. **Ranked Table Row**: Highlighted with `isSelected = selectedH3Index === cell.h3Index` and "Selected" button text.
2. **Right-Side Detail Card**: Receives `selectedCell` directly from `useHotspots`. Displays identical H3 index, risk score, risk level, confidence, freshness, model version, and predictedAt.
3. **Map Polygon**: Receives `selectedCellId` and applies `#38bdf8` outline, higher fill opacity (0.65), and 3px stroke weight.
4. **Map Viewport**: Smoothly fits the bounding box of the city's cells and pans to ensure the selected cell is within the viewport.

### Lifecycle of a City Switch
```
City Change (e.g. Pune -> Mumbai)
  │
  ├── 1. Immediate Synchronous Reset:
  │      overview = null
  │      selectedH3Index = null
  │      selectedCell = null
  │      loading = true
  │      details card displays: "Loading cell risk intelligence..."
  │      map starts transition toward new city center coordinates
  │
  ├── 2. In-Flight Request Dispatched:
  │      activeRequestIdRef incremented (e.g. reqId = 2)
  │      GET /api/v1/hotspots?cityId=mumbai
  │
  ├── 3. Backend Response Arrives:
  │      verify reqId === activeRequestIdRef.current (discard if stale)
  │      overview set to Mumbai data
  │      auto-select top-ranked cell: selectedH3Index = cells[0].h3Index, selectedCell = cells[0]
  │      loading = false
  │
  └── 4. View Synchronization:
         Right card renders Mumbai top cell
         Table row 0 highlighted as "Selected"
         Map fits bounds to Mumbai's H3 cell cluster
         Mumbai top cell rendered on top with sky blue highlight
```

---

## 4. Map Viewport & Geometry Fix

- Implemented `calculateSpatialBounds(hotspots, gridCells)` in `PollutionMap.tsx`. For every cell in the city, H3 vertices are converted via `h3.cellToBoundary` and expanded into a `L.LatLngBounds`.
- `MapViewController` calls `map.fitBounds(bounds, { padding: [35, 35], maxZoom: 13, animate: true })` whenever a new city's geometry arrives.
- When a cell is selected, if it falls outside `map.getBounds()`, `map.panTo([lat, lng])` pans smoothly to bring it into view without awkward zoom distortions.

---

## 5. Race-Condition Protection

Implemented using sequential request IDs:
```typescript
const requestId = ++activeRequestIdRef.current;
// ...
const data = await hotspotApi.getHotspotsByCity(cityId.trim());
if (requestId !== activeRequestIdRef.current) {
  return; // Discard superseded response
}
```
If Pune request is in flight and user selects Mumbai, any subsequent arrival of Pune response is silently dropped. Mumbai remains authoritative.

---

## 6. Automated Tests Summary

Ran full test suite via `npx tsx --test`:
- **`frontend/src/utils/hotspot.test.ts`**: **23 / 23 PASS**
  - Test 1: City change clears old selection immediately
  - Test 2: New city auto-selects first ranked cell from backend order
  - Test 3: Empty city clears selection to clean empty-state panel
  - Test 4: Late previous-city response is ignored (race guard)
  - Test 5: Selected H3 matches ranked table selected row
  - Test 6: Selected H3 matches right-side detail card exactly
  - Test 7: Map center updates coordinates on city change
  - Test 8: Selected cell visibility and spatial bounds fit behavior
  - Test 9: Model metadata belongs strictly to current city
  - Plus 14 baseline contract and styling tests
- **`frontend/src/utils/freshness.test.ts`**: **21 / 21 PASS**

Production Build Verification:
- **`npm run build`** (`tsc -b && vite build`): **PASS (Exit Code 0)**, 0 TypeScript errors.

---

## 7. Browser Smoke Result

- Dev server started cleanly on `http://localhost:3000/`.
- Backend responding on `http://localhost:8080/`.
- Verification flow:
  1. **Pune**: Loaded with ML model (`hotspot-rf-v1.0.0`), top cell `88608850e5fffff` selected.
  2. **Switch to Mumbai**: Previous Pune cell immediately cleared. Loading state displayed. Mumbai overview loaded with baseline model (`hotspot-baseline-v1`). Top cell (`88608b56b3fffff`) automatically selected. Right card, table, and map polygon synchronized to Mumbai. Map viewport fitted to Mumbai H3 bounds.
  3. **Switch to Delhi**: Delhi overview loaded with baseline model (`hotspot-baseline-v1`). Top cell (`883da11505fffff`) automatically selected. Right card, table, and map synchronized to Delhi.
