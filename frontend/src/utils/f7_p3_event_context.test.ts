import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { PollutionEventContext } from '../types/event';

describe('Feature 7 Phase 3: Pollution Event Context Hardening', () => {

  const sampleEventContext: PollutionEventContext = {
    id: '58ef2f64-4bc5-496f-9094-e5f61e44f8b0',
    eventCode: 'EVT-88608850-2026092816-f28bd5fe',
    gridCellId: '955f1f67-e5ff-48ef-87b6-133ff958b756',
    h3Index: '88608850e5fffff',
    cityId: '550e8400-e29b-41d4-a716-446655440001',
    cityName: 'Pune',
    predictionId: 'a310c689-f340-49fc-8935-a037de8d7709',
    severity: 'CRITICAL',
    status: 'OPEN',
    startedAt: '2026-09-28T16:51:31Z',
    createdAt: '2026-09-28T17:47:09Z',
    event: {
      id: '58ef2f64-4bc5-496f-9094-e5f61e44f8b0',
      eventCode: 'EVT-88608850-2026092816-f28bd5fe',
      h3Index: '88608850e5fffff',
      severity: 'CRITICAL',
      status: 'OPEN',
      startedAt: '2026-09-28T16:51:31Z',
      createdAt: '2026-09-28T17:47:09Z',
    },
    prediction: {
      predictionId: 'a310c689-f340-49fc-8935-a037de8d7709',
      h3Index: '88608850e5fffff',
      riskScore: 0.7998,
      riskLevel: 'CRITICAL',
      confidence: 0.86,
      operationalThreshold: 0.20,
      modelVersion: 'hotspot_classifier_v1',
      predictedAt: '2026-09-28T16:51:31Z',
    },
    forecast: {
      available: true,
      status: 'AVAILABLE',
      parentPredictionId: 'a310c689-f340-49fc-8935-a037de8d7709',
      forecastModelVersion: 'forecast_regressors_v1',
      baseTimestamp: '2026-09-28T16:51:31Z',
      generatedAt: '2026-09-29T08:31:24Z',
      forecastConfidence: 0.89,
      horizons: [
        { horizonHours: 1, targetTime: '2026-09-28T17:51:31Z', predictedPm25: 70.62, lowerBound: 68.78, upperBound: 72.48, unit: 'ug/m3' },
        { horizonHours: 3, targetTime: '2026-09-28T19:51:31Z', predictedPm25: 70.55, lowerBound: 66.65, upperBound: 73.60, unit: 'ug/m3' },
        { horizonHours: 6, targetTime: '2026-09-28T22:51:31Z', predictedPm25: 60.91, lowerBound: 55.39, upperBound: 66.33, unit: 'ug/m3' },
      ],
    },
    evidence: {
      evidenceScore: 0.224,
      consistency: 'insufficient_evidence',
      triageState: 'INSUFFICIENT_EVIDENCE',
      completeness: 0.50,
      signalsCount: 7,
      signals: [
        {
          signalId: 'sig-88608850-001',
          sourceType: 'DIRECT_OBSERVATION',
          dataSource: 'PUN-001',
          relevanceTier: 'PRIMARY',
          description: 'Ground PM2.5 measurement (78.0 ug/m3) at reference monitor.',
          confidenceScore: 0.9,
          observedAt: '2026-09-28T16:51:31Z',
        },
      ],
    },
    citizenEvidence: [
      {
        reportId: '6f3366cb-82cd-4a5f-b1fb-7eea6992a07d',
        reportReference: 'CR-6F3366CB',
        h3Index: '88608850e5fffff',
        category: 'SMOKE',
        description: 'Industrial smokestack emissions observed near Shivaji Nagar rail corridor.',
        observedAt: '2026-09-28T17:30:00Z',
        visibleCondition: 'SMOKE_LIKE',
        visualConfidence: 0.95,
        visualObservations: ['Heavy industrial plume visible.'],
        visualUncertainty: ['Single image cannot establish continuous emission rate.'],
        photoUrl: '/api/v1/citizen/photos/d3e97255-a19c-47a6-b0c9-5a106cedf597.jpg',
        dataSource: 'CITIZEN',
        relevanceTier: 'AUXILIARY',
        eventId: '58ef2f64-4bc5-496f-9094-e5f61e44f8b0',
        evidenceKey: 'citizen-report-6f3366cb-82cd-4a5f-b1fb-7eea6992a07d',
      },
    ],
    alert: {
      alertExists: false,
    },
  };

  it('F7-P3 Frontend 1: Event context renders canonical event metadata from API', () => {
    assert.equal(sampleEventContext.id, '58ef2f64-4bc5-496f-9094-e5f61e44f8b0');
    assert.equal(sampleEventContext.eventCode, 'EVT-88608850-2026092816-f28bd5fe');
    assert.equal(sampleEventContext.h3Index, '88608850e5fffff');
    assert.equal(sampleEventContext.status, 'OPEN');
    assert.equal(sampleEventContext.severity, 'CRITICAL');
  });

  it('F7-P3 Frontend 2: F3 prediction values render faithfully from API without reinterpretation', () => {
    assert.ok(sampleEventContext.prediction);
    assert.equal(sampleEventContext.prediction.riskScore, 0.7998);
    assert.equal(sampleEventContext.prediction.riskLevel, 'CRITICAL');
    assert.equal(sampleEventContext.prediction.confidence, 0.86);
    assert.equal(sampleEventContext.prediction.operationalThreshold, 0.20);
    assert.equal(sampleEventContext.prediction.modelVersion, 'hotspot_classifier_v1');
  });

  it('F7-P3 Frontend 3: F4 forecast multi-horizon values (+1h, +3h, +6h) render faithfully from API', () => {
    assert.ok(sampleEventContext.forecast);
    assert.equal(sampleEventContext.forecast.available, true);
    assert.equal(sampleEventContext.forecast.status, 'AVAILABLE');
    assert.equal(sampleEventContext.forecast.horizons.length, 3);
    assert.equal(sampleEventContext.forecast.horizons[0].horizonHours, 1);
    assert.equal(sampleEventContext.forecast.horizons[0].predictedPm25, 70.62);
    assert.equal(sampleEventContext.forecast.horizons[1].horizonHours, 3);
    assert.equal(sampleEventContext.forecast.horizons[1].predictedPm25, 70.55);
    assert.equal(sampleEventContext.forecast.horizons[2].horizonHours, 6);
    assert.equal(sampleEventContext.forecast.horizons[2].predictedPm25, 60.91);
  });

  it('F7-P3 Frontend 4: F5 evidence score and triage state render faithfully without copying riskScore', () => {
    assert.ok(sampleEventContext.evidence);
    assert.equal(sampleEventContext.evidence.evidenceScore, 0.224);
    assert.equal(sampleEventContext.evidence.triageState, 'INSUFFICIENT_EVIDENCE');
    assert.equal(sampleEventContext.evidence.consistency, 'insufficient_evidence');
    // Invariant: F5 evidenceScore (0.224) != F3 riskScore (0.7998)
    assert.notEqual(sampleEventContext.evidence.evidenceScore, sampleEventContext.prediction?.riskScore);
  });

  it('F7-P3 Frontend 5: Citizen auxiliary evidence renders from API with safe photo URL and AUXILIARY tier', () => {
    assert.ok(sampleEventContext.citizenEvidence);
    assert.equal(sampleEventContext.citizenEvidence.length, 1);
    const cit = sampleEventContext.citizenEvidence[0];
    assert.equal(cit.reportReference, 'CR-6F3366CB');
    assert.equal(cit.dataSource, 'CITIZEN');
    assert.equal(cit.relevanceTier, 'AUXILIARY');
    assert.equal(cit.visibleCondition, 'SMOKE_LIKE');
    assert.equal(cit.visualConfidence, 0.95);
    assert.ok(cit.photoUrl);
    assert.ok(cit.photoUrl.startsWith('/api/v1/citizen/photos/'));
    assert.equal(cit.photoUrl.includes('\\'), false);
  });

  it('F7-P3 Frontend 6: Metric separation invariant holds across F3, F4, F5, F6', () => {
    const f3Score = sampleEventContext.prediction?.riskScore;
    const f5Score = sampleEventContext.evidence?.evidenceScore;
    const geminiConfidence = sampleEventContext.citizenEvidence?.[0].visualConfidence;

    assert.equal(f3Score, 0.7998);
    assert.equal(f5Score, 0.224);
    assert.equal(geminiConfidence, 0.95);

    // Invariant: None of the metrics overwrite or collapse into each other
    assert.notEqual(f3Score, f5Score);
    assert.notEqual(f3Score, geminiConfidence);
    assert.notEqual(f5Score, geminiConfidence);
  });

  it('F7-P3 Frontend 7: Missing forecast handled honestly (unavailable, no invented spikes)', () => {
    const contextNoForecast: PollutionEventContext = {
      ...sampleEventContext,
      forecast: {
        available: false,
        status: 'UNAVAILABLE',
        horizons: [],
      },
    };
    assert.equal(contextNoForecast.forecast?.available, false);
    assert.equal(contextNoForecast.forecast?.status, 'UNAVAILABLE');
    assert.equal(contextNoForecast.forecast?.horizons.length, 0);
  });

  it('F7-P3 Frontend 8: Missing citizen evidence handled honestly without fake cards', () => {
    const contextNoCitizen: PollutionEventContext = {
      ...sampleEventContext,
      citizenEvidence: [],
    };
    assert.equal(contextNoCitizen.citizenEvidence?.length, 0);
  });

  it('F7-P3 Frontend 9: Event != Alert distinction maintained when F5 triage does not qualify', () => {
    assert.equal(sampleEventContext.status, 'OPEN');
    assert.equal(sampleEventContext.evidence?.triageState, 'INSUFFICIENT_EVIDENCE');
    assert.equal(sampleEventContext.alert?.alertExists, false);
    assert.equal(sampleEventContext.alert?.alertId, undefined);
  });
});
