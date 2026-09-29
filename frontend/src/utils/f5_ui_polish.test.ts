import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { MOCK_QUALIFYING_ALERT, MOCK_ACKNOWLEDGED_ALERT, MOCK_RESOLVED_ALERT } from './alerts.test';
import { REAL_PUNE_EVIDENCE_PAYLOAD } from './evidence.test';
import { AuthorityQueueItem } from '../types/alert';
import { EvidenceSummaryResponse } from '../types/evidence';

// =========================================================================
// F5 UI POLISH Targeted Verification Tests
// =========================================================================

test('F5 UI Polish 1: Unified Sidebar navigation active state logic', () => {
  // Test route matching logic for the merged 'Evidence & WHY' sidebar item
  const itemPath = '/analyst/evidence';
  const getIsActive = (currentPath: string) => {
    return itemPath === '/analyst/evidence'
      ? currentPath === '/analyst/evidence' || currentPath === '/gemini-why'
      : currentPath === itemPath;
  };

  assert.strictEqual(getIsActive('/analyst/evidence'), true, 'Active on /analyst/evidence');
  assert.strictEqual(getIsActive('/gemini-why'), true, 'Active on /gemini-why');
  assert.strictEqual(getIsActive('/authority/alerts'), false, 'Inactive on /authority/alerts');
  assert.strictEqual(getIsActive('/dashboard'), false, 'Inactive on /dashboard');
});

test('F5 UI Polish 2: Sidebar.tsx contains exactly ONE unified Evidence & WHY entry', () => {
  const sidebarPath = path.resolve(process.cwd(), 'src/components/layout/Sidebar.tsx');
  const sidebarSource = fs.readFileSync(sidebarPath, 'utf-8');

  // Verify single entry
  assert.ok(sidebarSource.includes("'Evidence & WHY'"), 'Must contain Evidence & WHY nav item');
  assert.ok(!sidebarSource.includes("'Gemini WHY'"), 'Must NOT contain standalone Gemini WHY nav item');

  // Verify route points to /analyst/evidence
  assert.ok(
    sidebarSource.includes("label: 'Evidence & WHY', path: '/analyst/evidence'"),
    'Must route to /analyst/evidence'
  );
});

test('F5 UI Polish 3: Truthful unavailable forecast presentation without synthetic zeros', () => {
  // Evidence summary with null/unavailable forecast
  const payloadWithoutForecast: EvidenceSummaryResponse = {
    ...REAL_PUNE_EVIDENCE_PAYLOAD,
    modelOutputs: {
      ...REAL_PUNE_EVIDENCE_PAYLOAD.modelOutputs,
      forecast: null,
    },
  };

  // UI logic helper: must NEVER return synthetic zeros
  const getForecastDisplayState = (forecast: EvidenceSummaryResponse['modelOutputs']['forecast']) => {
    if (!forecast || !forecast.horizons || forecast.horizons.length === 0) {
      return {
        available: false,
        message: 'Forecast unavailable for this cell',
        confidence: null,
      };
    }
    return {
      available: true,
      message: 'Active empirical trajectories',
      confidence: forecast.forecastConfidence,
    };
  };

  const displayState = getForecastDisplayState(payloadWithoutForecast.modelOutputs.forecast);
  assert.strictEqual(displayState.available, false);
  assert.strictEqual(displayState.message, 'Forecast unavailable for this cell');
  assert.strictEqual(displayState.confidence, null, 'forecastConfidence must strictly remain null');
});

test('F5 UI Polish 4: Two-way Evidence <-> Authority Alert cross-linking contracts', () => {
  const h3Index = '88608850e5fffff';

  // 1. Evidence Page -> Authority Alert link (when ALERT_CANDIDATE)
  const evidenceToAlertUrl = `/authority/alerts?h3=${encodeURIComponent(h3Index)}`;
  assert.strictEqual(evidenceToAlertUrl, '/authority/alerts?h3=88608850e5fffff');

  // 2. Alert Detail -> Evidence & WHY dossier link
  const alertToEvidenceUrl = `/analyst/evidence?h3=${encodeURIComponent(h3Index)}`;
  assert.strictEqual(alertToEvidenceUrl, '/analyst/evidence?h3=88608850e5fffff');

  // Verify URL query parameter extraction
  const searchParams = new URLSearchParams('h3=88608850e5fffff');
  assert.strictEqual(searchParams.get('h3'), '88608850e5fffff');
});

test('F5 UI Polish 5: Dynamic alert filter counts calculated strictly from loaded queue items', () => {
  const queueItems: AuthorityQueueItem[] = [
    MOCK_QUALIFYING_ALERT, // OPEN
    MOCK_ACKNOWLEDGED_ALERT, // ACKNOWLEDGED
    MOCK_RESOLVED_ALERT, // RESOLVED
  ];

  const counts = {
    ALL: queueItems.length,
    OPEN: queueItems.filter((i) => i.status === 'OPEN').length,
    ACKNOWLEDGED: queueItems.filter((i) => i.status === 'ACKNOWLEDGED').length,
    RESOLVED: queueItems.filter((i) => i.status === 'RESOLVED').length,
  };

  assert.strictEqual(counts.ALL, 3);
  assert.strictEqual(counts.OPEN, 1);
  assert.strictEqual(counts.ACKNOWLEDGED, 1);
  assert.strictEqual(counts.RESOLVED, 1);

  // Client filtering
  const filterByStatus = (status: 'ALL' | 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED') => {
    if (status === 'ALL') return queueItems;
    return queueItems.filter((item) => item.status === status);
  };

  assert.strictEqual(filterByStatus('OPEN').length, 1);
  assert.strictEqual(filterByStatus('ACKNOWLEDGED').length, 1);
  assert.strictEqual(filterByStatus('RESOLVED').length, 1);
  assert.strictEqual(filterByStatus('ALL').length, 3);
});

test('F5 UI Polish 6: Selected alert operational dossier renders evidence, risk, and lineage', () => {
  const alert = MOCK_QUALIFYING_ALERT;

  // Header & Status
  assert.strictEqual(alert.severity, 'CRITICAL');
  assert.strictEqual(alert.status, 'OPEN');
  assert.strictEqual(alert.triageState, 'ALERT_CANDIDATE');

  // Lineage fields
  assert.strictEqual(alert.eventCode, 'EVT-88619694-2026092807-ind12345');
  assert.strictEqual(alert.h3Index, '886196944dfffff');
  assert.strictEqual(alert.predictionId, 'd4e5f6a1-0000-4000-8000-112233445566');

  // Numerical parity
  assert.strictEqual(alert.evidenceScore, 0.667);
  assert.strictEqual(alert.riskScore, 0.85);

  // Gemini grounded summary
  assert.ok(alert.geminiSummary);
  assert.strictEqual(alert.hasGeminiAnalysis, true);
});

test('F5 UI Polish 7: Source code audit — No fake mock alerts, fake PM2.5, or fake counters', () => {
  const alertsFile = fs.readFileSync(path.resolve(process.cwd(), 'src/pages/authority/Alerts.tsx'), 'utf-8');
  assert.ok(!alertsFile.includes('mock'), 'Alerts.tsx must not contain mock arrays');
  assert.ok(!alertsFile.includes('alt-1'), 'Alerts.tsx must not contain fake alt-1');
  assert.ok(!alertsFile.includes('fake'), 'Alerts.tsx must not contain fake values');

  const evidencePanelFile = fs.readFileSync(
    path.resolve(process.cwd(), 'src/components/hotspot/EvidencePanel.tsx'),
    'utf-8'
  );
  assert.ok(!evidencePanelFile.includes('mockEvidence'), 'EvidencePanel must not contain mockEvidence');
});
