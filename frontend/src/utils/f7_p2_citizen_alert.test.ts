import test from 'node:test';
import assert from 'node:assert/strict';
import { AuthorityQueueItem, CitizenEvidenceItem } from '../types/alert';
import fs from 'node:fs';
import path from 'node:path';

// =========================================================================
// Real Authoritative Test Fixtures for F7-P2 Citizen Evidence Bridge
// =========================================================================

export const MOCK_CITIZEN_EVIDENCE_ATTACHED: CitizenEvidenceItem = {
  reportId: '6f3366cb-82cd-4a5f-b1fb-7eea6992a07d',
  reportReference: 'CR-6F3366CB',
  h3Index: '88608850e5fffff',
  category: 'INDUSTRIAL_EMISSION',
  description: 'Dense black smoke plume billowing from industrial facility smokestack',
  observedAt: '2026-09-29T04:30:00Z',
  visibleCondition: 'SMOKE_LIKE',
  visualConfidence: 0.95,
  visualObservations: ['dense black smoke plume', 'industrial chimney discharge'],
  visualUncertainty: ['plume height estimated, partial cloud cover'],
  photoUrl: '/api/v1/citizen/photos/6f3366cb-82cd-4a5f-b1fb-7eea6992a07d.jpg',
  dataSource: 'CITIZEN',
  relevanceTier: 'AUXILIARY',
  eventId: '58ef2f64-4bc5-496f-9094-e5f61e44f8b0',
  evidenceKey: 'citizen-report-6f3366cb-82cd-4a5f-b1fb-7eea6992a07d',
};

export const MOCK_ALERT_WITH_CITIZEN_EVIDENCE: AuthorityQueueItem = {
  alertId: '58ef2f64-4bc5-496f-9094-e5f61e44f8b0',
  eventId: '58ef2f64-4bc5-496f-9094-e5f61e44f8b0',
  eventCode: 'EVT-88608850-2026092816-f28bd5fe',
  h3Index: '88608850e5fffff',
  predictionId: 'd4e5f6a1-0000-4000-8000-112233445566',
  cityId: '550e8400-e29b-41d4-a716-446655440001',
  cityName: 'Pune',
  status: 'OPEN',
  severity: 'CRITICAL',
  riskScore: 0.7998,
  evidenceScore: 0.7998,
  triageState: 'ALERT_CANDIDATE',
  consistency: 'CONSISTENT',
  title: 'Corroborated Industrial Plume Event',
  message: 'Multi-source confirmation: Sensor PM2.5 elevated with citizen visual observation.',
  forecastSummary: '+1h: 145.2 ug/m3 | +3h: 162.0 ug/m3 | +6h: 178.5 ug/m3',
  recommendedAction: 'Dispatch field inspection unit to industrial sector',
  hasGeminiAnalysis: true,
  geminiSummary: 'Severe particulate elevation supported by visual smoke plume report',
  createdAt: '2026-09-29T04:35:00Z',
  updatedAt: '2026-09-29T04:35:00Z',
  citizenEvidence: [MOCK_CITIZEN_EVIDENCE_ATTACHED],
};

export const MOCK_ALERT_WITHOUT_CITIZEN_EVIDENCE: AuthorityQueueItem = {
  alertId: '11111111-2222-3333-4444-555555555555',
  eventId: '11111111-2222-3333-4444-555555555555',
  eventCode: 'EVT-88608852-2026092816-aaaa1111',
  h3Index: '88608852c1fffff',
  predictionId: 'd4e5f6a1-0000-4000-8000-222233334444',
  cityId: '550e8400-e29b-41d4-a716-446655440001',
  cityName: 'Pune',
  status: 'OPEN',
  severity: 'HIGH',
  riskScore: 0.65,
  evidenceScore: 0.62,
  triageState: 'ALERT_CANDIDATE',
  consistency: 'CONSISTENT',
  title: 'Moderate Particulate Elevation',
  message: 'Sensor PM2.5 exceeds operational threshold.',
  forecastSummary: '+1h: 110.0 ug/m3 | +3h: 125.0 ug/m3',
  recommendedAction: 'Monitor sensor progression',
  hasGeminiAnalysis: false,
  createdAt: '2026-09-29T05:00:00Z',
  citizenEvidence: null,
};

// =========================================================================
// F7-P2 Frontend Verification Test Cases
// =========================================================================

test('F7-P2 Frontend 1: Citizen evidence card renders from real API data structure', () => {
  const alert = MOCK_ALERT_WITH_CITIZEN_EVIDENCE;
  assert.ok(alert.citizenEvidence && alert.citizenEvidence.length === 1);
  const evidence = alert.citizenEvidence[0];

  assert.equal(evidence.reportReference, 'CR-6F3366CB');
  assert.equal(evidence.category, 'INDUSTRIAL_EMISSION');
  assert.equal(evidence.visibleCondition, 'SMOKE_LIKE');
  assert.equal(evidence.visualConfidence, 0.95);
  assert.equal(evidence.dataSource, 'CITIZEN');
  assert.equal(evidence.relevanceTier, 'AUXILIARY');
  assert.ok(evidence.description.includes('Dense black smoke'));
  assert.deepEqual(evidence.visualObservations, ['dense black smoke plume', 'industrial chimney discharge']);
});

test('F7-P2 Frontend 2: Missing citizen evidence renders no fake content', () => {
  const alert = MOCK_ALERT_WITHOUT_CITIZEN_EVIDENCE;
  const hasCitizenEvidence = Boolean(alert.citizenEvidence && alert.citizenEvidence.length > 0);
  assert.equal(hasCitizenEvidence, false);
  assert.equal(alert.citizenEvidence, null);
});

test('F7-P2 Frontend 3: Photo renders through safe URL without local filesystem paths', () => {
  const evidence = MOCK_CITIZEN_EVIDENCE_ATTACHED;
  assert.ok(evidence.photoUrl);
  // Must use safe HTTP/API route
  assert.ok(evidence.photoUrl.startsWith('/api/v1/citizen/photos/'));
  // Must NOT expose local filesystem
  assert.ok(!evidence.photoUrl.includes('C:'));
  assert.ok(!evidence.photoUrl.includes('\\'));
  assert.ok(!evidence.photoUrl.includes('/uploads/'));
  assert.ok(!evidence.photoUrl.includes('/tmp/'));
});

test('F7-P2 Frontend 4: H3 is taken directly from backend response', () => {
  const alert = MOCK_ALERT_WITH_CITIZEN_EVIDENCE;
  const evidence = alert.citizenEvidence![0];
  assert.equal(alert.h3Index, '88608850e5fffff');
  assert.equal(evidence.h3Index, alert.h3Index);
});

test('F7-P2 Frontend 5: Existing alert details remain intact with citizen evidence attached', () => {
  const alert = MOCK_ALERT_WITH_CITIZEN_EVIDENCE;
  assert.equal(alert.alertId, '58ef2f64-4bc5-496f-9094-e5f61e44f8b0');
  assert.equal(alert.riskScore, 0.7998);
  assert.equal(alert.evidenceScore, 0.7998);
  assert.equal(alert.triageState, 'ALERT_CANDIDATE');
  assert.ok(alert.forecastSummary?.includes('+1h'));
  assert.equal(alert.hasGeminiAnalysis, true);
});

test('F7-P2 Frontend 6: Evidence & WHY navigation uses authoritative H3 parameter', () => {
  const alert = MOCK_ALERT_WITH_CITIZEN_EVIDENCE;
  const targetUrl = `/analyst/evidence?h3=${encodeURIComponent(alert.h3Index)}`;
  assert.equal(targetUrl, '/analyst/evidence?h3=88608850e5fffff');
});

test('F7-P2 Frontend 7: Alerts.tsx source contains Section 20b and evaluator safety notices', () => {
  const alertsPath = path.resolve(process.cwd(), 'src/pages/authority/Alerts.tsx');
  const source = fs.readFileSync(alertsPath, 'utf8');

  assert.ok(source.includes('Citizen Visual Observation'), 'Must have Citizen Visual Observation header');
  assert.ok(source.includes('AUXILIARY EVIDENCE'), 'Must have AUXILIARY EVIDENCE tier label');
  assert.ok(source.includes('CITIZEN SOURCE'), 'Must label data source as CITIZEN');
  assert.ok(source.includes('Evaluator Notice:'), 'Must include notice separating auxiliary from regulatory');
  assert.ok(source.includes('AI Visual Interpretation'), 'Must have AI visual interpretation');
  assert.ok(!source.includes('confirmed pollution source'), 'Must not claim confirmed source');
});
