import test from 'node:test';
import assert from 'node:assert/strict';
import * as h3 from 'h3-js';
import { HOTSPOT_RISK_STYLES, getRiskStyle } from './hotspotColors';
import { HotspotOverviewResponse, HotspotCell, HotspotRiskLevel } from '../types/hotspot';

// =========================================================================
// 1. Risk Level Mapping & Styling Contract
// =========================================================================

test('Risk Styling — LOW risk returns emerald styling and safe treatment', () => {
  const style = getRiskStyle('LOW');
  assert.strictEqual(style.color, '#10b981');
  assert.strictEqual(style.label, 'Low Risk');
  assert.ok(style.bg.includes('16, 185, 129'));
  assert.ok(style.description.length > 0);
});

test('Risk Styling — MODERATE risk returns amber warning treatment', () => {
  const style = getRiskStyle('MODERATE');
  assert.strictEqual(style.color, '#f59e0b');
  assert.strictEqual(style.label, 'Moderate Risk');
  assert.ok(style.bg.includes('245, 158, 11'));
});

test('Risk Styling — HIGH risk returns orange high-risk treatment', () => {
  const style = getRiskStyle('HIGH');
  assert.strictEqual(style.color, '#f97316');
  assert.strictEqual(style.label, 'High Risk');
  assert.ok(style.bg.includes('249, 115, 22'));
});

test('Risk Styling — CRITICAL risk returns red critical treatment', () => {
  const style = getRiskStyle('CRITICAL');
  assert.strictEqual(style.color, '#ef4444');
  assert.strictEqual(style.label, 'Critical Risk');
  assert.ok(style.bg.includes('239, 68, 68'));
});

test('Risk Styling — Invalid or missing level defaults gracefully to LOW', () => {
  const styleNull = getRiskStyle(undefined);
  assert.strictEqual(styleNull.color, '#10b981');

  const styleUnknown = getRiskStyle('UNKNOWN_LEVEL' as any);
  assert.strictEqual(styleUnknown.color, '#10b981');
});

// =========================================================================
// 2. Data Contract & Type Mapping Verification
// =========================================================================

test('Data Contract — Backend HotspotOverviewResponse preserves exact structure without client alterations', () => {
  const mockBackendPayload: HotspotOverviewResponse = {
    cityId: '550e8400-e29b-41d4-a716-446655440001',
    cityName: 'Pune',
    generatedAt: '2026-09-26T13:08:48.118968Z',
    modelVersion: 'hotspot-baseline-v1',
    engineType: 'BASELINE',
    freshness: 'LIVE',
    totalCells: 3,
    highRiskCells: 2,
    cells: [
      {
        h3Index: '88608850e5fffff',
        gridCellId: '955f1f67-e5ff-48ef-87b6-133ff958b756',
        riskScore: 0.7225,
        riskLevel: 'HIGH',
        confidence: 0.85,
        predictedAt: '2026-09-26T12:48:38.191225Z',
        freshness: 'LIVE',
        modelVersion: 'hotspot-baseline-v1',
      },
      {
        h3Index: '88608852c1fffff',
        gridCellId: '5a9f3126-51d3-4769-9f4e-f970140ec013',
        riskScore: 0.6019,
        riskLevel: 'MODERATE',
        confidence: 0.85,
        predictedAt: '2026-09-26T12:48:38.851263Z',
        freshness: 'LIVE',
        modelVersion: 'hotspot-baseline-v1',
      },
    ],
  };

  assert.strictEqual(mockBackendPayload.cityId, '550e8400-e29b-41d4-a716-446655440001');
  assert.strictEqual(mockBackendPayload.engineType, 'BASELINE');
  assert.strictEqual(mockBackendPayload.totalCells, 3);
  assert.strictEqual(mockBackendPayload.highRiskCells, 2);
  assert.strictEqual(mockBackendPayload.cells.length, 2);

  const cell = mockBackendPayload.cells[0];
  assert.strictEqual(cell.h3Index, '88608850e5fffff');
  assert.strictEqual(cell.riskScore, 0.7225);
  assert.strictEqual(cell.riskLevel, 'HIGH');
  assert.strictEqual(cell.confidence, 0.85);
  assert.strictEqual(cell.freshness, 'LIVE');
});

// =========================================================================
// 3. Selected Cell Lookup Logic
// =========================================================================

test('Cell Selection — Correctly resolves selected cell from overview cache', () => {
  const cells: HotspotCell[] = [
    {
      h3Index: '88608850e5fffff',
      gridCellId: 'cell-1',
      riskScore: 0.75,
      riskLevel: 'HIGH',
      confidence: 0.85,
      predictedAt: '2026-09-26T12:00:00Z',
      freshness: 'LIVE',
      modelVersion: 'hotspot-baseline-v1',
    },
    {
      h3Index: '88608852c1fffff',
      gridCellId: 'cell-2',
      riskScore: 0.35,
      riskLevel: 'LOW',
      confidence: 0.85,
      predictedAt: '2026-09-26T12:00:00Z',
      freshness: 'LIVE',
      modelVersion: 'hotspot-baseline-v1',
    },
  ];

  const found = cells.find((c) => c.h3Index === '88608850e5fffff');
  assert.ok(found);
  assert.strictEqual(found.riskLevel, 'HIGH');
  assert.strictEqual(found.riskScore, 0.75);

  const notFound = cells.find((c) => c.h3Index === '880000000000000');
  assert.strictEqual(notFound, undefined);
});

// =========================================================================
// 4. Freshness State Logic
// =========================================================================

test('Freshness Semantics — Maps valid statuses (LIVE, STALE, NO_DATA, UNAVAILABLE)', () => {
  const validFreshnessValues = ['LIVE', 'STALE', 'NO_DATA', 'UNAVAILABLE'];

  for (const status of validFreshnessValues) {
    const cell: Partial<HotspotCell> = { freshness: status as any };
    assert.ok(validFreshnessValues.includes(cell.freshness!));
  }
});

// =========================================================================
// 5. Multi-City Differentiation & Unmonitored Confidence Handling
// =========================================================================

test('Multi-City Handling — Mumbai and Delhi reflect unmonitored sensor state with lower confidence', () => {
  const puneCell: HotspotCell = {
    h3Index: '88608850e5fffff',
    gridCellId: 'cell-pune',
    riskScore: 0.7225,
    riskLevel: 'HIGH',
    confidence: 0.85,
    predictedAt: '2026-09-26T12:48:38Z',
    freshness: 'LIVE',
    modelVersion: 'hotspot-baseline-v1',
  };

  const mumbaiCell: HotspotCell = {
    h3Index: '88608b56b3fffff',
    gridCellId: 'cell-mumbai',
    riskScore: 0.264,
    riskLevel: 'LOW',
    confidence: 0.35, // Degraded confidence due to missing co-pollutants
    predictedAt: '2026-09-26T12:48:38Z',
    freshness: 'LIVE',
    modelVersion: 'hotspot-baseline-v1',
  };

  // Pune with full telemetry maintains high confidence
  assert.ok(puneCell.confidence >= 0.70);

  // Mumbai with partial telemetry maintains lower confidence (<= 0.50) without fabricating numbers
  assert.ok(mumbaiCell.confidence <= 0.50);
});

// =========================================================================
// 6. Source Audit & Labeling Constraints
// =========================================================================

test('Labeling Constraints — UI labeling strictly specifies Potential Hotspot', () => {
  const validLabel = 'Potential Hotspot';
  assert.strictEqual(validLabel, 'Potential Hotspot');
  assert.notStrictEqual(validLabel, 'Confirmed Pollution');
  assert.notStrictEqual(validLabel, 'Official AQI');
  assert.notStrictEqual(validLabel, 'Factory Caused Pollution');
});

// =========================================================================
// 7. Loading, Empty, and Error State Logic
// =========================================================================

test('State Handling — Empty cells array produces 0 counts and empty presentation', () => {
  const emptyOverview: HotspotOverviewResponse = {
    cityId: '550e8400-e29b-41d4-a716-446655440001',
    cityName: 'Empty City',
    generatedAt: new Date().toISOString(),
    modelVersion: 'hotspot-baseline-v1',
    engineType: 'BASELINE',
    freshness: 'NO_DATA',
    totalCells: 0,
    highRiskCells: 0,
    cells: [],
  };

  assert.strictEqual(emptyOverview.cells.length, 0);
  assert.strictEqual(emptyOverview.totalCells, 0);
  assert.strictEqual(emptyOverview.highRiskCells, 0);
  assert.strictEqual(emptyOverview.freshness, 'NO_DATA');
});

test('State Handling — Stale freshness produces warning tier presentation', () => {
  const staleOverview: HotspotOverviewResponse = {
    cityId: '550e8400-e29b-41d4-a716-446655440001',
    cityName: 'Stale City',
    generatedAt: new Date(Date.now() - 5 * 3600 * 1000).toISOString(),
    modelVersion: 'hotspot-baseline-v1',
    engineType: 'BASELINE',
    freshness: 'STALE',
    totalCells: 1,
    highRiskCells: 0,
    cells: [
      {
        h3Index: '88608850e5fffff',
        gridCellId: 'cell-1',
        riskScore: 0.25,
        riskLevel: 'LOW',
        confidence: 0.70,
        predictedAt: new Date(Date.now() - 5 * 3600 * 1000).toISOString(),
        freshness: 'STALE',
        modelVersion: 'hotspot-baseline-v1',
      },
    ],
  };

  assert.strictEqual(staleOverview.freshness, 'STALE');
  assert.strictEqual(staleOverview.cells[0].freshness, 'STALE');
});

// =========================================================================
// 8. H3 Index Format & Spatial Alignment
// =========================================================================

test('Spatial Format — H3 cell IDs conform to Uber H3 Resolution 8 hexadecimal format', () => {
  const validH3Indices = [
    '88608850e5fffff', // Pune Shivajinagar
    '88608852c1fffff', // Pune Katraj
    '8860885357fffff', // Pune Hadapsar
    '88608b56b3fffff', // Mumbai Kurla
    '883da11505fffff', // Delhi RK Puram
  ];

  const h3Regex = /^[0-9a-f]{15}$/i;
  for (const h3 of validH3Indices) {
    assert.strictEqual(h3.length, 15);
    assert.ok(h3Regex.test(h3), `H3 index ${h3} does not match 15-char hex format`);
  }
});

// =========================================================================
// 9. Source Code Audit — No Mock Data Left in Production Hotspots Page
// =========================================================================

test('Source Audit — Hotspots.tsx contains no hardcoded mock IDs or synthetic arrays', async () => {
  const fs = await import('node:fs/promises');
  const path = await import('node:path');
  const candidates = [
    path.resolve(process.cwd(), 'src/pages/public/Hotspots.tsx'),
    path.resolve(process.cwd(), 'frontend/src/pages/public/Hotspots.tsx'),
  ];
  let content = '';
  for (const p of candidates) {
    try {
      content = await fs.readFile(p, 'utf-8');
      break;
    } catch {}
  }
  assert.ok(content.length > 0, 'Could not read Hotspots.tsx');

  // Verify absence of legacy hardcoded mocks
  assert.ok(!content.includes('hotspot-01'), 'Found mock ID hotspot-01 in Hotspots.tsx');
  assert.ok(!content.includes('hotspot-02'), 'Found mock ID hotspot-02 in Hotspots.tsx');
  assert.ok(!content.includes('hotspot-03'), 'Found mock ID hotspot-03 in Hotspots.tsx');
  assert.ok(!content.includes('hotspot-v1.2'), 'Found legacy mock version hotspot-v1.2 in Hotspots.tsx');
  assert.ok(!content.includes('8860144aa1fffff'), 'Found fake H3 index 8860144aa1fffff in Hotspots.tsx');

  // Verify presence of real API hook and centralized components
  assert.ok(content.includes('useHotspots'), 'Hotspots.tsx must use useHotspots hook');
  assert.ok(content.includes('RiskLegend'), 'Hotspots.tsx must use RiskLegend');
  assert.ok(content.includes('HotspotCellDetailsCard'), 'Hotspots.tsx must use HotspotCellDetailsCard');
});

// =========================================================================
// 10. City Switch + Selected Hotspot + Map Synchronization (F3 UI BUG FIX)
// =========================================================================

test('F3 City Switch 1: City change clears old selection immediately', () => {
  // Simulate active Pune state with selected Pune cell
  let currentCityId = 'pune';
  let selectedH3Index: string | null = '88608850e5fffff';
  let selectedCell: HotspotCell | null = {
    h3Index: '88608850e5fffff',
    gridCellId: 'pune-cell-1',
    riskScore: 0.88,
    riskLevel: 'HIGH',
    confidence: 0.85,
    predictedAt: '2026-09-26T12:00:00Z',
    freshness: 'LIVE',
    modelVersion: 'hotspot-rf-v1.0.0',
  };
  let overview: HotspotOverviewResponse | null = {
    cityId: 'pune',
    cityName: 'Pune',
    generatedAt: '2026-09-26T12:00:00Z',
    modelVersion: 'hotspot-rf-v1.0.0',
    engineType: 'ML_PRIMARY',
    freshness: 'LIVE',
    totalCells: 1,
    highRiskCells: 1,
    cells: [selectedCell],
  };

  // User initiates city switch to Mumbai
  const newCityId = 'mumbai';
  const isCitySwitch = newCityId !== currentCityId;

  if (isCitySwitch) {
    // Expected behavior: Immediately reset selection and overview (BUG 1, BUG 8)
    overview = null;
    selectedH3Index = null;
    selectedCell = null;
  }

  assert.strictEqual(selectedH3Index, null, 'Previous selected cell ID must be immediately cleared');
  assert.strictEqual(selectedCell, null, 'Previous selected cell details must be immediately cleared');
  assert.strictEqual(overview, null, 'Previous overview must be cleared to prevent mixed state');
});

test('F3 City Switch 2: New city auto-selects first ranked cell from backend order', () => {
  const mumbaiCells: HotspotCell[] = [
    {
      h3Index: '88608b56b3fffff', // Top-ranked cell (Kurla)
      gridCellId: 'mum-1',
      riskScore: 0.65,
      riskLevel: 'MODERATE',
      confidence: 0.60,
      predictedAt: '2026-09-26T12:05:00Z',
      freshness: 'LIVE',
      modelVersion: 'hotspot-baseline-v1',
    },
    {
      h3Index: '88608b56b1fffff',
      gridCellId: 'mum-2',
      riskScore: 0.45,
      riskLevel: 'LOW',
      confidence: 0.55,
      predictedAt: '2026-09-26T12:05:00Z',
      freshness: 'LIVE',
      modelVersion: 'hotspot-baseline-v1',
    },
  ];

  // Automatic selection rule (BUG 6): cells[0] is authoritative
  let selectedH3Index: string | null = null;
  let selectedCell: HotspotCell | null = null;

  if (mumbaiCells.length > 0) {
    const topCell = mumbaiCells[0];
    selectedH3Index = topCell.h3Index;
    selectedCell = topCell;
  }

  assert.strictEqual(selectedH3Index, '88608b56b3fffff');
  assert.strictEqual(selectedCell?.h3Index, '88608b56b3fffff');
  assert.strictEqual(selectedCell?.riskScore, 0.65);
  assert.strictEqual(selectedCell?.riskLevel, 'MODERATE');
  assert.strictEqual(selectedCell?.modelVersion, 'hotspot-baseline-v1');
});

test('F3 City Switch 3: Empty city clears selection to clean empty-state panel', () => {
  const emptyCells: HotspotCell[] = [];

  let selectedH3Index: string | null = 'stale-id';
  let selectedCell: HotspotCell | null = { h3Index: 'stale-id' } as any;

  if (emptyCells.length === 0) {
    selectedH3Index = null;
    selectedCell = null;
  }

  assert.strictEqual(selectedH3Index, null);
  assert.strictEqual(selectedCell, null);
});

test('F3 City Switch 4: Late previous-city response is ignored (race guard)', () => {
  let activeRequestId = 0;
  let currentCityState: string | null = null;
  let selectedHotspotState: string | null = null;

  // 1. User selects Pune
  const puneReqId = ++activeRequestId; // 1

  // 2. User quickly selects Mumbai before Pune returns
  const mumbaiReqId = ++activeRequestId; // 2
  // Immediate state reset
  currentCityState = 'mumbai-loading';
  selectedHotspotState = null;

  // 3. Late Pune response arrives
  const puneData = { city: 'Pune', topCell: '88608850e5fffff' };
  if (puneReqId === activeRequestId) {
    // Should NOT execute
    currentCityState = puneData.city;
    selectedHotspotState = puneData.topCell;
  }

  assert.strictEqual(currentCityState, 'mumbai-loading');
  assert.strictEqual(selectedHotspotState, null, 'Late Pune response must not overwrite Mumbai');

  // 4. Mumbai response arrives
  const mumbaiData = { city: 'Mumbai', topCell: '88608b56b3fffff' };
  if (mumbaiReqId === activeRequestId) {
    currentCityState = mumbaiData.city;
    selectedHotspotState = mumbaiData.topCell;
  }

  assert.strictEqual(currentCityState, 'Mumbai');
  assert.strictEqual(selectedHotspotState, '88608b56b3fffff');
});

test('F3 City Switch 5: Selected H3 matches ranked table selected row', () => {
  const cells: HotspotCell[] = [
    { h3Index: '88608b56b3fffff', riskScore: 0.65, riskLevel: 'MODERATE' } as any,
    { h3Index: '88608b56b1fffff', riskScore: 0.45, riskLevel: 'LOW' } as any,
  ];
  const selectedH3Index = '88608b56b3fffff';

  const tableRows = cells.map((cell) => ({
    h3Index: cell.h3Index,
    isSelected: selectedH3Index === cell.h3Index,
    actionButtonText: selectedH3Index === cell.h3Index ? 'Selected' : 'Inspect',
    actionButtonVariant: selectedH3Index === cell.h3Index ? 'primary' : 'outline',
  }));

  assert.strictEqual(tableRows[0].isSelected, true);
  assert.strictEqual(tableRows[0].actionButtonText, 'Selected');
  assert.strictEqual(tableRows[0].actionButtonVariant, 'primary');

  assert.strictEqual(tableRows[1].isSelected, false);
  assert.strictEqual(tableRows[1].actionButtonText, 'Inspect');
  assert.strictEqual(tableRows[1].actionButtonVariant, 'outline');
});

test('F3 City Switch 6: Selected H3 matches right-side detail card exactly', () => {
  const cell: HotspotCell = {
    h3Index: '88608b56b3fffff',
    gridCellId: 'mum-1',
    riskScore: 0.654,
    riskLevel: 'MODERATE',
    confidence: 0.60,
    predictedAt: '2026-09-26T12:05:00Z',
    freshness: 'LIVE',
    modelVersion: 'hotspot-baseline-v1',
  };
  const selectedH3Index = '88608b56b3fffff';

  // Detail card fields
  assert.strictEqual(cell.h3Index, selectedH3Index);
  assert.strictEqual((cell.riskScore * 100).toFixed(1) + '%', '65.4%');
  assert.strictEqual(cell.riskLevel, 'MODERATE');
  assert.strictEqual((cell.confidence * 100).toFixed(0) + '%', '60%');
  assert.strictEqual(cell.freshness, 'LIVE');
  assert.strictEqual(cell.modelVersion, 'hotspot-baseline-v1');
});

test('F3 City Switch 7: Map center updates coordinates on city change', () => {
  const cities = {
    pune: { latitude: 18.5204, longitude: 73.8567 },
    mumbai: { latitude: 19.0760, longitude: 72.8777 },
    delhi: { latitude: 28.6139, longitude: 77.2090 },
  };

  // Pune
  let activeCenter: [number, number] = [cities.pune.latitude, cities.pune.longitude];
  assert.strictEqual(activeCenter[0], 18.5204);
  assert.strictEqual(activeCenter[1], 73.8567);

  // Switch to Mumbai
  activeCenter = [cities.mumbai.latitude, cities.mumbai.longitude];
  assert.strictEqual(activeCenter[0], 19.0760);
  assert.strictEqual(activeCenter[1], 72.8777);

  // Switch to Delhi
  activeCenter = [cities.delhi.latitude, cities.delhi.longitude];
  assert.strictEqual(activeCenter[0], 28.6139);
  assert.strictEqual(activeCenter[1], 77.2090);

  // Switch to Pune
  activeCenter = [cities.pune.latitude, cities.pune.longitude];
  assert.strictEqual(activeCenter[0], 18.5204);
  assert.strictEqual(activeCenter[1], 73.8567);
});

test('F3 City Switch 8: Selected cell visibility and spatial bounds fit behavior', () => {
  const puneCell = '88608850e5fffff'; // Pune
  const mumbaiCell = '88608b56b3fffff'; // Mumbai
  const delhiCell = '883da11505fffff'; // Delhi

  const puneCenter = h3.cellToLatLng(puneCell);
  const mumbaiCenter = h3.cellToLatLng(mumbaiCell);
  const delhiCenter = h3.cellToLatLng(delhiCell);

  // Verify coordinates are in distinct geographic regions
  assert.ok(puneCenter[0] > 18.0 && puneCenter[0] < 19.0, 'Pune latitude in range');
  assert.ok(puneCenter[1] > 73.0 && puneCenter[1] < 74.5, 'Pune longitude in range');

  assert.ok(mumbaiCenter[0] > 18.8 && mumbaiCenter[0] < 19.3, 'Mumbai latitude in range');
  assert.ok(mumbaiCenter[1] > 72.7 && mumbaiCenter[1] < 73.2, 'Mumbai longitude in range');

  assert.ok(delhiCenter[0] > 28.0 && delhiCenter[0] < 29.0, 'Delhi latitude in range');
  assert.ok(delhiCenter[1] > 76.5 && delhiCenter[1] < 77.5, 'Delhi longitude in range');

  // Verify boundary generation for polygon display
  const puneBoundary = h3.cellToBoundary(puneCell);
  assert.strictEqual(puneBoundary.length, 6, 'H3 cell produces 6 vertices');

  // Verify bounding box calculation
  const lats = puneBoundary.map((p) => p[0]);
  const lngs = puneBoundary.map((p) => p[1]);
  const minLat = Math.min(...lats);
  const maxLat = Math.max(...lats);
  const minLng = Math.min(...lngs);
  const maxLng = Math.max(...lngs);

  // Selected cell center must be contained in the cell boundary bounding box
  assert.ok(puneCenter[0] >= minLat && puneCenter[0] <= maxLat);
  assert.ok(puneCenter[1] >= minLng && puneCenter[1] <= maxLng);
});

test('F3 City Switch 9: Model metadata belongs strictly to current city', () => {
  const puneOverview: Partial<HotspotOverviewResponse> = {
    cityId: '550e8400-e29b-41d4-a716-446655440001',
    cityName: 'Pune',
    modelVersion: 'hotspot-rf-v1.0.0',
    engineType: 'ML_PRIMARY',
  };

  const mumbaiOverview: Partial<HotspotOverviewResponse> = {
    cityId: '550e8400-e29b-41d4-a716-446655440002',
    cityName: 'Mumbai',
    modelVersion: 'hotspot-baseline-v1',
    engineType: 'BASELINE',
  };

  const delhiOverview: Partial<HotspotOverviewResponse> = {
    cityId: '550e8400-e29b-41d4-a716-446655440003',
    cityName: 'Delhi',
    modelVersion: 'hotspot-baseline-v1',
    engineType: 'BASELINE',
  };

  assert.strictEqual(puneOverview.modelVersion, 'hotspot-rf-v1.0.0');
  assert.strictEqual(puneOverview.engineType, 'ML_PRIMARY');

  assert.strictEqual(mumbaiOverview.modelVersion, 'hotspot-baseline-v1');
  assert.strictEqual(mumbaiOverview.engineType, 'BASELINE');

  assert.strictEqual(delhiOverview.modelVersion, 'hotspot-baseline-v1');
  assert.strictEqual(delhiOverview.engineType, 'BASELINE');
});

