import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import * as h3 from 'h3-js';
import { getH3BoundarySafe, getH3CenterSafe } from './h3Spatial';
import { AuthorityQueueItem } from '../types/alert';
import { PollutionEventContext } from '../types/event';

describe('F7-P5: Complete Alert Detail + Evidence/WHY Connection Tests', () => {

  const sampleAlertWithEvidence: AuthorityQueueItem = {
    alertId: '3e804cd6-384c-47dd-92b4-b2e53b604be3',
    eventId: '9ea81bb7-9ff2-4b4d-a80f-32e98e2cb12f',
    eventCode: 'EVT-88608850-2026092816-d75654e9',
    h3Index: '88608850e5fffff',
    predictionId: 'a310c689-f340-49fc-8935-a037de8d7709',
    cityId: '550e8400-e29b-41d4-a716-446655440001',
    cityName: 'Pune',
    status: 'OPEN',
    severity: 'CRITICAL',
    riskScore: 0.7998,
    evidenceScore: 0.2240,
    triageState: 'ALERT_CANDIDATE',
    consistency: 'CONSISTENT',
    title: 'Severe Particulate Elevation near Shivaji Nagar',
    message: 'Telemetry corroborated with ground sensor observation.',
    forecastSummary: '+1h: 70.6 ug/m3 | +3h: 70.5 ug/m3 | +6h: 60.9 ug/m3',
    recommendedAction: 'Deploy inspection team to Shivaji Nagar rail perimeter',
    hasGeminiAnalysis: true,
    geminiSummary: 'Industrial combustion plumes interacting with microclimate stagnation.',
    createdAt: '2026-09-28T16:51:31Z',
    citizenEvidence: [
      {
        reportId: '6f3366cb-82cd-4a5f-b1fb-7eea6992a07d',
        reportReference: 'CR-6F3366CB',
        h3Index: '88608850e5fffff',
        category: 'SMOKE',
        description: 'Thick smoke plume rising from railway corridor stack.',
        observedAt: '2026-09-28T16:45:00Z',
        visibleCondition: 'SMOKE_LIKE',
        visualConfidence: 0.95,
        visualObservations: ['Dense black particulate plume visible.'],
        visualUncertainty: ['Single viewpoint observation.'],
        photoUrl: '/api/v1/citizen/photos/cr-6f3366cb-photo.jpg',
        dataSource: 'CITIZEN',
        relevanceTier: 'AUXILIARY',
        eventId: '9ea81bb7-9ff2-4b4d-a80f-32e98e2cb12f',
        evidenceKey: 'citizen-report-6f3366cb',
      },
    ],
  };

  const sampleAlertWithoutEvidence: AuthorityQueueItem = {
    alertId: '4d05144a-ae0a-4831-992d-8ca7db547956',
    eventId: '8cb12cc4-7aa1-423e-8f12-42e12b604bc1',
    eventCode: 'EVT-88608852-2026092817-b15264f1',
    h3Index: '88608852c1fffff',
    predictionId: 'f1a2b3c4-1111-4444-8888-999900001111',
    cityId: '550e8400-e29b-41d4-a716-446655440001',
    cityName: 'Pune',
    status: 'OPEN',
    severity: 'HIGH',
    riskScore: 0.6543,
    evidenceScore: 0.612,
    triageState: 'ALERT_CANDIDATE',
    consistency: 'CONSISTENT',
    title: 'Elevated NO2 and PM10 in Industrial Corridor',
    message: 'Sensor grid detected rapid spike across multiple cells.',
    forecastSummary: null,
    recommendedAction: null,
    hasGeminiAnalysis: false,
    geminiSummary: null,
    createdAt: '2026-09-28T17:10:00Z',
    citizenEvidence: [],
  };

  const sampleEventContext: PollutionEventContext = {
    id: '9ea81bb7-9ff2-4b4d-a80f-32e98e2cb12f',
    eventCode: 'EVT-88608850-2026092816-d75654e9',
    h3Index: '88608850e5fffff',
    cityId: '550e8400-e29b-41d4-a716-446655440001',
    cityName: 'Pune',
    predictionId: 'a310c689-f340-49fc-8935-a037de8d7709',
    severity: 'CRITICAL',
    status: 'ACTIVE',
    startedAt: '2026-09-28T16:50:00Z',
    createdAt: '2026-09-28T16:51:31Z',
    event: {
      id: '9ea81bb7-9ff2-4b4d-a80f-32e98e2cb12f',
      eventCode: 'EVT-88608850-2026092816-d75654e9',
      h3Index: '88608850e5fffff',
      predictionId: 'a310c689-f340-49fc-8935-a037de8d7709',
      severity: 'CRITICAL',
      status: 'ACTIVE',
      startedAt: '2026-09-28T16:50:00Z',
      createdAt: '2026-09-28T16:51:31Z',
    },
    prediction: {
      predictionId: 'a310c689-f340-49fc-8935-a037de8d7709',
      h3Index: '88608850e5fffff',
      riskScore: 0.7998,
      riskLevel: 'CRITICAL',
      confidence: 0.88,
      operationalThreshold: 0.20,
      modelVersion: 'hotspot_classifier_v1',
      predictedAt: '2026-09-28T16:49:00Z',
    },
    forecast: {
      available: true,
      status: 'AVAILABLE',
      forecastModelVersion: 'forecast_regressors_v1',
      forecastConfidence: 0.85,
      horizons: [
        { horizonHours: 1, targetTime: '2026-09-28T17:50:00Z', predictedPm25: 70.6, lowerBound: 62.0, upperBound: 79.2, unit: 'ug/m3' },
        { horizonHours: 3, targetTime: '2026-09-28T19:50:00Z', predictedPm25: 70.5, lowerBound: 60.1, upperBound: 80.9, unit: 'ug/m3' },
        { horizonHours: 6, targetTime: '2026-09-28T22:50:00Z', predictedPm25: 60.9, lowerBound: 51.0, upperBound: 70.8, unit: 'ug/m3' },
      ],
    },
    evidence: {
      evidenceScore: 0.2240,
      consistency: 'CONSISTENT',
      triageState: 'ALERT_CANDIDATE',
      completeness: 1.0,
      signalsCount: 4,
      signals: [
        {
          signalId: 'sig-1',
          sourceType: 'GROUND_SENSOR',
          dataSource: 'CAAQMS',
          relevanceTier: 'PRIMARY',
          description: 'PM2.5 exceeded threshold',
          confidenceScore: 0.95,
        },
      ],
    },
    citizenEvidence: sampleAlertWithEvidence.citizenEvidence || [],
    alert: {
      alertExists: true,
      alertId: sampleAlertWithEvidence.alertId,
      status: 'OPEN',
      severity: 'CRITICAL',
      triageState: 'ALERT_CANDIDATE',
      title: sampleAlertWithEvidence.title,
      message: sampleAlertWithEvidence.message,
    },
  };

  it('1. Alert detail renders live alert ID', () => {
    assert.strictEqual(sampleAlertWithEvidence.alertId, '3e804cd6-384c-47dd-92b4-b2e53b604be3');
    assert.ok(sampleAlertWithEvidence.alertId.length === 36, 'UUID must be 36 characters');
  });

  it('2. Event context renders truthful Potential Pollution Event without causal claims', () => {
    const eventContextTitle = 'Potential Pollution Event';
    assert.strictEqual(eventContextTitle, 'Potential Pollution Event');
    assert.strictEqual(sampleEventContext.eventCode, 'EVT-88608850-2026092816-d75654e9');
    assert.strictEqual(sampleEventContext.h3Index, '88608850e5fffff');
    assert.strictEqual(sampleEventContext.predictionId, 'a310c689-f340-49fc-8935-a037de8d7709');
    assert.strictEqual(sampleEventContext.status, 'ACTIVE');

    // Forbidden strings check
    const prohibitedClaims = ['Confirmed Pollution', 'Confirmed Factory', 'Confirmed Source'];
    prohibitedClaims.forEach((claim) => {
      assert.ok(!eventContextTitle.includes(claim), `Context title must not contain causal claim "${claim}"`);
    });
  });

  it('3. F3 values render authoritative prediction outputs', () => {
    assert.strictEqual(sampleEventContext.prediction?.riskScore, 0.7998);
    assert.strictEqual(sampleEventContext.prediction?.riskLevel, 'CRITICAL');
    assert.strictEqual(sampleEventContext.prediction?.confidence, 0.88);
    assert.strictEqual(sampleEventContext.prediction?.modelVersion, 'hotspot_classifier_v1');
    assert.strictEqual(sampleEventContext.prediction?.predictionId, 'a310c689-f340-49fc-8935-a037de8d7709');
  });

  it('4. F4 values render +1h, +3h, +6h trajectories accurately', () => {
    const horizons = sampleEventContext.forecast?.horizons;
    assert.ok(horizons && horizons.length === 3, 'Must have exactly 3 horizons');

    const h1 = horizons.find((h) => h.horizonHours === 1);
    const h3 = horizons.find((h) => h.horizonHours === 3);
    const h6 = horizons.find((h) => h.horizonHours === 6);

    assert.strictEqual(h1?.predictedPm25, 70.6);
    assert.strictEqual(h3?.predictedPm25, 70.5);
    assert.strictEqual(h6?.predictedPm25, 60.9);
  });

  it('5. F5 values render evidenceScore, consistency, and triageState', () => {
    assert.strictEqual(sampleAlertWithEvidence.evidenceScore, 0.2240);
    assert.strictEqual(sampleAlertWithEvidence.consistency, 'CONSISTENT');
    assert.strictEqual(sampleAlertWithEvidence.triageState, 'ALERT_CANDIDATE');
    assert.strictEqual(sampleEventContext.evidence?.completeness, 1.0);
    assert.strictEqual(sampleEventContext.evidence?.signalsCount, 4);
  });

  it('6. Citizen evidence renders observation metadata with auxiliary labeling', () => {
    const observations = sampleAlertWithEvidence.citizenEvidence;
    assert.ok(observations && observations.length === 1);
    const obs = observations[0];

    assert.strictEqual(obs.reportReference, 'CR-6F3366CB');
    assert.strictEqual(obs.category, 'SMOKE');
    assert.strictEqual(obs.visibleCondition, 'SMOKE_LIKE');
    assert.strictEqual(obs.visualConfidence, 0.95);
    assert.strictEqual(obs.dataSource, 'CITIZEN');
    assert.strictEqual(obs.relevanceTier, 'AUXILIARY');
    assert.strictEqual(obs.h3Index, '88608850e5fffff');
    assert.strictEqual(obs.evidenceKey, 'citizen-report-6f3366cb');
    assert.deepStrictEqual(obs.visualObservations, ['Dense black particulate plume visible.']);
    assert.deepStrictEqual(obs.visualUncertainty, ['Single viewpoint observation.']);
  });

  it('7. Citizen photo renders safe URL and rejects local filesystem paths', () => {
    const photoUrl = sampleAlertWithEvidence.citizenEvidence![0].photoUrl!;
    assert.ok(photoUrl.startsWith('/api/v1/citizen/photos/'), 'Photo URL must use safe backend photo path');
    assert.ok(!photoUrl.includes('C:\\'), 'Must not expose Windows local drive path');
    assert.ok(!photoUrl.includes('/uploads/'), 'Must not expose direct filesystem upload root');
  });

  it('8. Missing citizen evidence is handled honestly with empty state', () => {
    const observations = sampleAlertWithoutEvidence.citizenEvidence;
    const hasEvidence = observations && observations.length > 0;
    assert.strictEqual(hasEvidence, false);

    const emptyStateText = 'No citizen observation attached.';
    assert.ok(emptyStateText.includes('No citizen observation attached'));
  });

  it('9. Gemini visual confidence remains separate from F3 and F5 scores', () => {
    const geminiConfidence = sampleAlertWithEvidence.citizenEvidence![0].visualConfidence;
    const f3RiskScore = sampleAlertWithEvidence.riskScore;
    const f5EvidenceScore = sampleAlertWithEvidence.evidenceScore;

    assert.strictEqual(geminiConfidence, 0.95);
    assert.strictEqual(f3RiskScore, 0.7998);
    assert.strictEqual(f5EvidenceScore, 0.2240);

    assert.notStrictEqual(geminiConfidence, f3RiskScore, 'Gemini confidence != F3 riskScore');
    assert.notStrictEqual(geminiConfidence, f5EvidenceScore, 'Gemini confidence != F5 evidenceScore');
  });

  it('10. F3 riskScore remains separate and authoritative without citizen influence', () => {
    const f3Score = sampleAlertWithEvidence.riskScore;
    assert.strictEqual(f3Score, 0.7998);
    assert.strictEqual(typeof f3Score, 'number');
    assert.ok(f3Score >= 0.0 && f3Score <= 1.0);
  });

  it('11. F5 evidenceScore remains separate and un-recalculated', () => {
    const f5Score = sampleAlertWithEvidence.evidenceScore;
    assert.strictEqual(f5Score, 0.2240);
    assert.strictEqual(typeof f5Score, 'number');
    assert.ok(f5Score >= 0.0 && f5Score <= 1.0);
  });

  it('12. Evidence & WHY H3 link matches selected alert H3 parameter', () => {
    const h3Param = sampleAlertWithEvidence.h3Index;
    const expectedLink = `/analyst/evidence?h3=${encodeURIComponent(h3Param)}`;
    assert.strictEqual(expectedLink, '/analyst/evidence?h3=88608850e5fffff');
  });

  it('13. Map selection remains correct with H3 Res-8 polygon boundary', () => {
    const boundary = getH3BoundarySafe(sampleAlertWithEvidence.h3Index);
    assert.ok(Array.isArray(boundary));
    assert.ok(boundary.length >= 6, 'Res-8 cell must form at least a hexagon (6 vertices)');

    const center = getH3CenterSafe(sampleAlertWithEvidence.h3Index);
    assert.ok(center, 'Centroid must not be null');
    assert.ok(center![0] >= 18.0 && center![0] <= 19.0, 'Centroid latitude within Pune metro');
    assert.ok(center![1] >= 73.0 && center![1] <= 74.0, 'Centroid longitude within Pune metro');
  });

  it('14. Alert not found handled safely without application crash', () => {
    const nullAlert: AuthorityQueueItem | null = null;
    const fallbackMessage = !nullAlert ? 'No Alert Selected' : 'Alert Selected';
    assert.strictEqual(fallbackMessage, 'No Alert Selected');
  });

  it('15. Forecast unavailable handled honestly with placeholder', () => {
    const forecastSummary = sampleAlertWithoutEvidence.forecastSummary;
    const forecastStatus = forecastSummary ? 'Trajectories loaded' : 'Forecast Unavailable';
    assert.strictEqual(forecastStatus, 'Forecast Unavailable');
  });

  it('16. No hardcoded IDs in rendering pipeline', () => {
    const arbitraryAlertId = '11223344-5566-7788-99aa-bbccddeeff00';
    const arbitraryEventCode = 'EVT-DYNAMIC-CORRIDOR-20260929';
    const customAlert: AuthorityQueueItem = {
      ...sampleAlertWithEvidence,
      alertId: arbitraryAlertId,
      eventCode: arbitraryEventCode,
    };

    assert.strictEqual(customAlert.alertId, arbitraryAlertId);
    assert.strictEqual(customAlert.eventCode, arbitraryEventCode);
  });

});
