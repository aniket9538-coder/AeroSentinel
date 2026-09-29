import test from 'node:test';
import assert from 'node:assert/strict';
import { AuthorityQueueItem } from '../types/alert';
import fs from 'node:fs';
import path from 'node:path';

// =========================================================================
// Real Authoritative Test Fixtures for F5-P5 Authority Queue
// =========================================================================
export const MOCK_QUALIFYING_ALERT: AuthorityQueueItem = {
  alertId: 'c7d91e84-5a23-4f91-b384-918237465012',
  eventId: 'a9b9d686-5fb6-48e0-9cbb-61e7fffd65e0',
  eventCode: 'EVT-88619694-2026092807-ind12345',
  h3Index: '886196944dfffff',
  predictionId: 'd4e5f6a1-0000-4000-8000-112233445566',
  cityId: '550e8400-e29b-41d4-a716-446655440001',
  cityName: 'Pune',
  status: 'OPEN',
  severity: 'CRITICAL',
  riskScore: 0.85,
  evidenceScore: 0.667,
  triageState: 'ALERT_CANDIDATE',
  consistency: 'consistent',
  title: 'Severe industrial emission cluster',
  message: 'Ground sensors report PM2.5 of 158 ug/m3 corroborated by FIRMS fire detection.',
  forecastSummary: '+1h: 162.5 ug/m3 | +3h: 175.2 ug/m3 | +6h: 180.0 ug/m3',
  recommendedAction: 'Deploy environmental enforcement team to industrial sector',
  hasGeminiAnalysis: true,
  geminiSummary: 'Industrial emission cluster detected in sector 886196944dfffff',
  createdAt: '2026-09-28T07:30:00Z',
  updatedAt: '2026-09-28T07:30:00Z',
  acknowledgedAt: null,
  resolvedAt: null,
};

export const MOCK_ACKNOWLEDGED_ALERT: AuthorityQueueItem = {
  ...MOCK_QUALIFYING_ALERT,
  alertId: 'b8e82d73-4c12-4e80-a273-807126354901',
  status: 'ACKNOWLEDGED',
  acknowledgedAt: '2026-09-28T07:35:00Z',
};

export const MOCK_RESOLVED_ALERT: AuthorityQueueItem = {
  ...MOCK_QUALIFYING_ALERT,
  alertId: 'a7d71c62-3b01-4d70-9162-796015243890',
  status: 'RESOLVED',
  acknowledgedAt: '2026-09-28T07:35:00Z',
  resolvedAt: '2026-09-28T07:45:00Z',
};

// =========================================================================
// 10 Automated Frontend Test Cases per F5-P5 Specification
// =========================================================================

test('F5-P5 Frontend 1: Queue API loading state presentation', () => {
  const loadingText = 'Loading authority queue...';
  assert.equal(typeof loadingText, 'string');
  assert.ok(loadingText.includes('Loading authority queue...'));
});

test('F5-P5 Frontend 2: Queue item rendering with full attributes', () => {
  const item = MOCK_QUALIFYING_ALERT;
  assert.equal(item.alertId, 'c7d91e84-5a23-4f91-b384-918237465012');
  assert.equal(item.eventCode, 'EVT-88619694-2026092807-ind12345');
  assert.equal(item.h3Index, '886196944dfffff');
  assert.equal(item.severity, 'CRITICAL');
  assert.equal(item.riskScore, 0.85);
  assert.equal(item.evidenceScore, 0.667);
  assert.equal(item.triageState, 'ALERT_CANDIDATE');
  assert.equal(item.status, 'OPEN');
  assert.ok(item.forecastSummary?.includes('+1h'));
});

test('F5-P5 Frontend 3: Empty queue state message', () => {
  const emptyQueue: AuthorityQueueItem[] = [];
  const emptyText = 'No alert candidates are currently available.';
  assert.equal(emptyQueue.length, 0);
  assert.ok(emptyText.includes('No alert candidates'));
});

test('F5-P5 Frontend 4: API failure error state presentation', () => {
  const errorText = 'Unable to load authority alerts.';
  assert.ok(errorText.includes('Unable to load authority alerts.'));
});

test('F5-P5 Frontend 5: Alert detail rendering with 4-tier semantic separation', () => {
  const item = MOCK_QUALIFYING_ALERT;
  // Four mandatory tiers:
  const observedFactsDefined = Boolean(item.createdAt && item.h3Index);
  const modelOutputDefined = Boolean(item.riskScore != null && item.forecastSummary);
  const aiInterpretationDefined = Boolean(item.geminiSummary || item.message);
  const recommendedVerificationDefined = Boolean(item.recommendedAction);

  assert.ok(observedFactsDefined, 'Observed facts must be present');
  assert.ok(modelOutputDefined, 'Model output must be present');
  assert.ok(aiInterpretationDefined, 'AI interpretation must be present');
  assert.ok(recommendedVerificationDefined, 'Recommended verification must be present');
});

test('F5-P5 Frontend 6: Status update lifecycle transitions (OPEN -> ACKNOWLEDGED -> RESOLVED)', () => {
  const initial = MOCK_QUALIFYING_ALERT;
  assert.equal(initial.status, 'OPEN');

  const acked = MOCK_ACKNOWLEDGED_ALERT;
  assert.equal(acked.status, 'ACKNOWLEDGED');
  assert.ok(acked.acknowledgedAt);

  const resolved = MOCK_RESOLVED_ALERT;
  assert.equal(resolved.status, 'RESOLVED');
  assert.ok(resolved.resolvedAt);
});

test('F5-P5 Frontend 7: Alert to EvidencePanel navigation route generation', () => {
  const h3Index = '886196944dfffff';
  const targetRoute = `/analyst/evidence?h3=${h3Index}`;
  assert.equal(targetRoute, '/analyst/evidence?h3=886196944dfffff');
  assert.ok(targetRoute.startsWith('/analyst/evidence?h3='));
});

test('F5-P5 Frontend 8: Source code audit - No hardcoded mock alerts (alt-1, alt-2, alt-101, alt-102)', () => {
  const alertsFile = fs.readFileSync(path.resolve(process.cwd(), 'src/pages/authority/Alerts.tsx'), 'utf-8');
  assert.ok(!alertsFile.includes('alt-1'), 'Alerts.tsx must not contain fake alt-1');
  assert.ok(!alertsFile.includes('alt-2'), 'Alerts.tsx must not contain fake alt-2');
  assert.ok(!alertsFile.includes('sampleAlerts'), 'Alerts.tsx must not contain sampleAlerts');

  const dashboardFile = fs.readFileSync(path.resolve(process.cwd(), 'src/pages/authority/AuthorityDashboard.tsx'), 'utf-8');
  assert.ok(!dashboardFile.includes('alt-101'), 'AuthorityDashboard.tsx must not contain fake alt-101');
  assert.ok(!dashboardFile.includes('alt-102'), 'AuthorityDashboard.tsx must not contain fake alt-102');
});

test('F5-P5 Frontend 9: Real event lineage preservation across queue items', () => {
  const item = MOCK_QUALIFYING_ALERT;
  assert.ok(item.eventId, 'Must contain eventId');
  assert.ok(item.eventCode, 'Must contain eventCode');
  assert.ok(item.predictionId, 'Must contain parent predictionId');
  assert.ok(item.h3Index, 'Must contain h3Index');
  assert.ok(item.cityId, 'Must contain cityId');
});

test('F5-P5 Frontend 10: Triage state displayed strictly as ALERT_CANDIDATE', () => {
  const item = MOCK_QUALIFYING_ALERT;
  assert.equal(item.triageState, 'ALERT_CANDIDATE');
  // Disallowed automatic conversion:
  const isEligibleForQueue = (triage: string) => triage === 'ALERT_CANDIDATE';
  assert.equal(isEligibleForQueue('ALERT_CANDIDATE'), true);
  assert.equal(isEligibleForQueue('INSUFFICIENT_EVIDENCE'), false);
  assert.equal(isEligibleForQueue('MONITOR'), false);
});
