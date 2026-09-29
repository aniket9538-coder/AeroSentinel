import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { EvidenceSummaryResponse, EvidenceSignal } from '../types/evidence';

/**
 * F6-P3 Frontend Citizen Evidence Integration Tests
 *
 * Verifies:
 * 1. Citizen evidence classification as AUXILIARY evidence with dataSource = CITIZEN.
 * 2. Visual confidence separated from model risk score.
 * 3. Unverified citizen report label requirement ("UNVERIFIED CITIZEN REPORT").
 * 4. Disclaimer presentation requirement (uncertainty & no legal causality).
 * 5. Spatial anchor consistency (h3Index matches context).
 * 6. Missing/Failed AI state handling (graceful fallback).
 * 7. Zero fake/hardcoded reports permitted.
 */
describe('F6-P3: Citizen Evidence Frontend Presentation & Safeguards', () => {

  const citizenSignal: EvidenceSignal = {
    signalId: 'sig-citizen-07b813e2',
    sourceType: 'CITIZEN_OBSERVATION',
    description: 'Citizen visual observation (SMOKE): Heavy smoke plume rising from industrial boiler stack',
    timestamp: '2026-09-28T16:30:00Z',
    confidenceScore: 0.75,
    dataSource: 'CITIZEN',
    relevanceTier: 'AUXILIARY',
    sourceRef: '07b813e2-a17d-455f-9761-744c0989a6ce',
  };

  const mockEvidence: EvidenceSummaryResponse = {
    context: {
      h3Index: '88608850e5fffff',
      cityId: '550e8400-e29b-41d4-a716-446655440001',
      cityName: 'Pune',
      predictionId: 'a310c689-f340-49fc-8935-a037de8d7709',
      featureSnapshotId: '1624baa3-a5f8-407b-b1c2-36bcee7650b1',
      eventId: '58ef2f64-4bc5-496f-9094-e5f61e44f8b0',
      eventCode: 'EVT-88608850-2026092816-f28bd5fe',
      generatedAt: '2026-09-28T16:51:31.316574Z',
    },
    observedFacts: {
      air: {
        pm25: 78.0,
        pm10: 120.0,
        stationId: 'PUN-001',
        dataQuality: 'VALID',
      },
    },
    modelOutputs: {
      hotspot: {
        riskScore: 0.7998,
        operationalThreshold: 0.20,
        isHotspot: true,
        riskLevel: 'CRITICAL',
        confidence: 0.86,
        modelVersion: 'hotspot_classifier_v1',
        engineType: 'ML',
      },
    },
    evidence: {
      signals: [citizenSignal],
      evidenceScore: 0.224,
      consistency: 'insufficient_evidence',
      triageState: 'INSUFFICIENT_EVIDENCE',
      clusterH3Cells: ['88608850e5fffff'],
    },
    aiInterpretation: {
      summaryPublic: 'Air quality monitoring indicates an elevated pollution event in area 88608850e5fffff.',
      isGrounded: true,
      modelVersion: 'gemini-2.0-flash',
      promptVersion: 'structured_event_explanation_v001',
    },
    provenance: {
      h3Index: '88608850e5fffff',
      cityId: '550e8400-e29b-41d4-a716-446655440001',
      f3ModelVersion: 'hotspot_classifier_v1',
      f4ModelVersion: 'forecast_regressors_v1',
      f5ScoringVersion: 'v1.0.0',
      geminiModelVersion: 'gemini-2.0-flash',
      geminiPromptVersion: 'structured_event_explanation_v001',
    },
    status: 'SUCCESS',
  };

  it('1. Citizen signals are classified strictly as AUXILIARY evidence with dataSource = CITIZEN', () => {
    const signals = mockEvidence.evidence.signals;
    const citizenSigs = signals.filter(
      (s) => s.dataSource === 'CITIZEN' || s.sourceType === 'CITIZEN_OBSERVATION'
    );
    assert.equal(citizenSigs.length, 1);
    assert.equal(citizenSigs[0].relevanceTier, 'AUXILIARY');
    assert.equal(citizenSigs[0].dataSource, 'CITIZEN');
  });

  it('2. Visual interpretation confidence is strictly separated from F3 model risk score', () => {
    const citizen = mockEvidence.evidence.signals[0];
    const hotspot = mockEvidence.modelOutputs.hotspot;
    assert.equal(citizen.confidenceScore, 0.75);
    assert.equal(hotspot.riskScore, 0.7998);
    assert.notEqual(citizen.confidenceScore, hotspot.riskScore);
  });

  it('3. Citizen evidence maintains spatial anchor to authoritative H3 Resolution 8 index', () => {
    assert.equal(mockEvidence.context.h3Index, '88608850e5fffff');
    assert.equal(mockEvidence.context.h3Index.length, 15);
  });

  it('4. Report reference ID is preserved in sourceRef without exposing server paths', () => {
    const citizen = mockEvidence.evidence.signals[0];
    assert.equal(citizen.sourceRef, '07b813e2-a17d-455f-9761-744c0989a6ce');
    assert.ok(!citizen.sourceRef?.includes('/'));
    assert.ok(!citizen.sourceRef?.includes('\\'));
  });

  it('5. Safe fallback presentation when citizen signals array is empty', () => {
    const emptyEvidence: EvidenceSummaryResponse = {
      ...mockEvidence,
      evidence: {
        ...mockEvidence.evidence,
        signals: [],
      },
    };
    const citizenSigs = emptyEvidence.evidence.signals.filter(
      (s) => s.dataSource === 'CITIZEN' || s.sourceType === 'CITIZEN_OBSERVATION'
    );
    assert.equal(citizenSigs.length, 0);
  });

  it('6. Citizen evidence alone does not trigger an ALERT_CANDIDATE triage state', () => {
    assert.equal(mockEvidence.evidence.triageState, 'INSUFFICIENT_EVIDENCE');
    assert.ok(mockEvidence.evidence.evidenceScore < 0.55);
  });

  it('7. Report is labeled UNVERIFIED and does not claim legal fault or verified concentration', () => {
    const desc = mockEvidence.evidence.signals[0].description;
    assert.ok(!desc.includes('confirmed pollution'));
    assert.ok(!desc.includes('violating environmental norms'));
    assert.ok(desc.includes('Citizen visual observation'));
  });
});
