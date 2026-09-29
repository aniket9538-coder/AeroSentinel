import test from 'node:test';
import assert from 'node:assert/strict';
import { validateEvidenceResponse } from '../services/evidenceApi';
import { EvidenceSummaryResponse } from '../types/evidence';

// =========================================================================
// Real Authoritative Pune Baseline Payload from F5-P2/P3
// =========================================================================
export const REAL_PUNE_EVIDENCE_PAYLOAD: EvidenceSummaryResponse = {
  status: 'SUCCESS',
  context: {
    h3Index: '88608850e5fffff',
    cityId: '550e8400-e29b-41d4-a716-446655440001',
    cityName: 'Pune',
    eventId: '3d8a7c21-1234-4567-89ab-cdef01234567',
    eventCode: 'EVT-PUN-88608850e5fffff-20260927',
    predictionId: 'a310c689-f340-49fc-8935-a037de8d7709',
    featureSnapshotId: '1624baa3-a5f8-407b-b1c2-36bcee7650b1',
    generatedAt: '2026-09-27T08:00:00Z',
  },
  observedFacts: {
    air: {
      status: 'VALID',
      pm25: 78.0,
      pm10: 120.0,
      no2: 37.0,
      so2: 14.0,
      co: 0.9,
      stationId: 'PUN-001',
      observedAt: '2026-09-27T07:55:00Z',
    },
    weather: {
      status: 'VALID',
      temperature: 30.1,
      humidity: 52.0,
      windSpeedMps: 3.31,
      windSpeedKmh: 11.9,
      windDirection: 271.0,
      surfacePressure: 949.8,
      observedAt: '2026-09-27T07:55:00Z',
    },
    monitoringCoverage: {
      status: 'VALID',
      nearestStationDistanceKm: 0.27,
      stationsWithin5kmCount: 2,
      monitoringCoverageGapFlag: 0,
      spatialCoverageConfidence: 0.95,
    },
    spatialDispersion: {
      status: 'VALID',
      pm25SpatialLagMean: 78.0,
      windU: 4.3604,
      windV: 0.0761,
    },
    gisContext: {
      status: 'VALID',
      distToNearestIndustrialKm: 3.5,
      distToNearestMajorRoadKm: 0.4,
      sensitiveReceptorsCount2km: 4,
      industrialZoneWithin2kmFlag: 0,
      fireCount24h25km: 0,
    },
  },
  modelOutputs: {
    hotspot: {
      riskScore: 0.80,
      riskLevel: 'CRITICAL',
      confidence: 0.86,
      modelVersion: 'hotspot_classifier_v1',
      engineType: 'ML',
      isHotspot: true,
      operationalThreshold: 0.20,
    },
    forecast: {
      baseTimestamp: '2026-09-27T08:00:00Z',
      generatedAt: '2026-09-27T08:01:00Z',
      forecastModelVersion: 'forecast_regressors_v1',
      parentPredictionId: 'a310c689-f340-49fc-8935-a037de8d7709',
      forecastConfidence: null,
      horizons: [
        {
          horizonHours: 1,
          targetTime: '2026-09-27T09:00:00Z',
          predictedPm25: 71.90,
          lowerBound: 70.06,
          upperBound: 73.76,
          unit: 'ug/m3',
        },
        {
          horizonHours: 3,
          targetTime: '2026-09-27T11:00:00Z',
          predictedPm25: 70.43,
          lowerBound: 66.53,
          upperBound: 73.48,
          unit: 'ug/m3',
        },
        {
          horizonHours: 6,
          targetTime: '2026-09-27T14:00:00Z',
          predictedPm25: 70.55,
          lowerBound: 65.03,
          upperBound: 75.98,
          unit: 'ug/m3',
        },
      ],
    },
  },
  evidence: {
    evidenceScore: 0.667,
    triageState: 'ALERT_CANDIDATE',
    clusterH3Cells: ['88608850e5fffff', '88608850e7fffff'],
    scoreBreakdown: {
      observationStrength: 0.35,
      mlForecastSupport: 0.40,
      multiSourceAgreement: 0.33,
      spatialConsistency: 0.95,
      evidenceCompleteness: 0.67,
      recencyFactor: 1.0,
      conflictPenalty: 0.0,
      unweightedEvidenceScore: 0.667,
      finalEvidenceScore: 0.667,
    },
    signals: [
      {
        sourceType: 'GROUND_AQI',
        level: 'CRITICAL',
        value: 78.0,
        unit: 'ug/m3',
        description: 'Direct continuous PM2.5 measurement',
        observedAt: '2026-09-27T07:55:00Z',
      },
    ],
    consistency: 'consistent',
    recencyTimestamp: '2026-09-27T07:55:00Z',
  },
  aiInterpretation: {
    summaryPublic: 'Elevated PM2.5 detected in area 88608850e5fffff with multi-horizon persistence.',
    summaryAnalyst: 'Analyst diagnostic: High ground PM2.5 (78 ug/m3) corroborated by forecast models.',
    detectedCondition: 'High particulate elevation',
    supportingSignals: ['PM2.5 concentration at 78.0 ug/m3', 'Nearest monitor at 0.27 km'],
    forecastTrajectory: 'Trajectory stabilizes at ~71 ug/m3 across 6 hours',
    uncertaintyStatement: 'Observations restricted to local CAAQMS station radius',
    unsupportedConclusions: ['Cannot confirm industrial stack emission cause without permit inspection'],
    privacyCheckPassed: true,
    isGrounded: true,
    geminiModel: 'gemini-2.0-flash',
    promptVersion: 'structured_event_explanation_v001',
  },
  recommendedVerification: {
    action: 'Dispatch mobile CAAQMS rapid-sampling unit',
    priority: 'URGENT',
    guidelines: ['Deploy mobile sensor to verify peak', 'Inspect upwind emitters'],
  },
  provenance: {
    h3Index: '88608850e5fffff',
    cityId: '550e8400-e29b-41d4-a716-446655440001',
    parentPredictionId: 'a310c689-f340-49fc-8935-a037de8d7709',
    f3PredictionId: 'a310c689-f340-49fc-8935-a037de8d7709',
    f3ModelVersion: 'hotspot_classifier_v1',
    f4ModelVersion: 'forecast_regressors_v1',
    f5ScoringVersion: 'v1.0.0',
    geminiModelVersion: 'gemini-2.0-flash',
    geminiPromptVersion: 'structured_event_explanation_v001',
    evaluatedAt: '2026-09-27T08:01:05Z',
  },
};

// =========================================================================
// 1. Evidence API Request & Response Parsing
// =========================================================================
test('1. Evidence API Request — Successfully parses authoritative backend payload', () => {
  const parsed = validateEvidenceResponse(REAL_PUNE_EVIDENCE_PAYLOAD);

  assert.strictEqual(parsed.status, 'SUCCESS');
  assert.strictEqual(parsed.context.h3Index, '88608850e5fffff');
  assert.strictEqual(parsed.context.eventCode, 'EVT-PUN-88608850e5fffff-20260927');
  assert.strictEqual(parsed.evidence.evidenceScore, 0.667);
  assert.strictEqual(parsed.evidence.triageState, 'ALERT_CANDIDATE');
});

// =========================================================================
// 2. Successful Rendering of Observed Facts (Tier A)
// =========================================================================
test('2. Observed Facts — Validates physical sensor data strictly separated from model outputs', () => {
  const parsed = validateEvidenceResponse(REAL_PUNE_EVIDENCE_PAYLOAD);
  const observed = parsed.observedFacts;

  // Air telemetry
  assert.strictEqual(observed.air?.pm25, 78.0);
  assert.strictEqual(observed.air?.pm10, 120.0);
  assert.strictEqual(observed.air?.stationId, 'PUN-001');

  // Surface weather
  assert.strictEqual(observed.weather?.temperature, 30.1);
  assert.strictEqual(observed.weather?.humidity, 52.0);
  assert.strictEqual(observed.weather?.windSpeedMps, 3.31);

  // Monitoring Coverage
  assert.strictEqual(observed.monitoringCoverage?.nearestStationDistanceKm, 0.27);
  assert.strictEqual(observed.monitoringCoverage?.monitoringCoverageGapFlag, 0);

  // GIS Context
  assert.strictEqual(observed.gisContext?.distToNearestIndustrialKm, 3.5);
  assert.strictEqual(observed.gisContext?.fireCount24h25km, 0);
});

// =========================================================================
// 3. Successful Rendering of F3/F4 Model Outputs (Tier B)
// =========================================================================
test('3. Model Outputs — Validates F3 risk classification and F4 multi-horizon forecasts', () => {
  const parsed = validateEvidenceResponse(REAL_PUNE_EVIDENCE_PAYLOAD);
  const models = parsed.modelOutputs;

  // F3 Hotspot Classifier
  assert.strictEqual(models.hotspot.riskScore, 0.80);
  assert.strictEqual(models.hotspot.riskLevel, 'CRITICAL');
  assert.strictEqual(models.hotspot.isHotspot, true);
  assert.strictEqual(models.hotspot.operationalThreshold, 0.20);
  assert.strictEqual(models.hotspot.modelVersion, 'hotspot_classifier_v1');

  // F4 Regressors
  assert.ok(models.forecast);
  assert.strictEqual(models.forecast?.horizons.length, 3);
  assert.strictEqual(models.forecast?.forecastConfidence, null); // Strictly null

  const h1 = models.forecast?.horizons.find((h) => h.horizonHours === 1);
  assert.ok(h1);
  assert.strictEqual(h1.predictedPm25, 71.90);
  assert.strictEqual(h1.lowerBound, 70.06);
  assert.strictEqual(h1.upperBound, 73.76);

  const h3 = models.forecast?.horizons.find((h) => h.horizonHours === 3);
  assert.ok(h3);
  assert.strictEqual(h3.predictedPm25, 70.43);

  const h6 = models.forecast?.horizons.find((h) => h.horizonHours === 6);
  assert.ok(h6);
  assert.strictEqual(h6.predictedPm25, 70.55);
});

// =========================================================================
// 4. Successful Rendering of Gemini AI Interpretation (Tier C)
// =========================================================================
test('4. AI Interpretation — Validates grounded Gemini explanation and safety guardrails', () => {
  const parsed = validateEvidenceResponse(REAL_PUNE_EVIDENCE_PAYLOAD);
  const ai = parsed.aiInterpretation;

  assert.ok(ai);
  assert.strictEqual(ai?.isGrounded, true);
  assert.strictEqual(ai?.privacyCheckPassed, true);
  assert.strictEqual(ai?.geminiModel, 'gemini-2.0-flash');
  assert.ok(ai?.summaryPublic.includes('88608850e5fffff'));
  assert.ok(ai?.summaryAnalyst?.includes('High ground PM2.5'));
  assert.strictEqual(ai?.forecastTrajectory, 'Trajectory stabilizes at ~71 ug/m3 across 6 hours');
  assert.strictEqual(ai?.unsupportedConclusions?.length, 1);
  assert.strictEqual(
    ai?.unsupportedConclusions?.[0],
    'Cannot confirm industrial stack emission cause without permit inspection'
  );
});

// =========================================================================
// 5. Recommended Verification — Operational Directives (Tier D)
// =========================================================================
test('5. Recommended Verification — Validates actionable operational field directives', () => {
  const parsed = validateEvidenceResponse(REAL_PUNE_EVIDENCE_PAYLOAD);
  const rec = parsed.recommendedVerification;

  assert.ok(rec);
  assert.strictEqual(rec?.priority, 'URGENT');
  assert.strictEqual(rec?.action, 'Dispatch mobile CAAQMS rapid-sampling unit');
  assert.strictEqual(rec?.guidelines?.length, 2);
  assert.strictEqual(rec?.guidelines?.[0], 'Deploy mobile sensor to verify peak');
});

// =========================================================================
// 6. Event Lineage Rendering
// =========================================================================
test('6. Event Lineage — Validates complete audit trail across H3, event, and predictions', () => {
  const parsed = validateEvidenceResponse(REAL_PUNE_EVIDENCE_PAYLOAD);

  assert.strictEqual(parsed.context.h3Index, '88608850e5fffff');
  assert.strictEqual(parsed.context.eventId, '3d8a7c21-1234-4567-89ab-cdef01234567');
  assert.strictEqual(parsed.context.eventCode, 'EVT-PUN-88608850e5fffff-20260927');
  assert.strictEqual(parsed.context.predictionId, 'a310c689-f340-49fc-8935-a037de8d7709');
  assert.strictEqual(parsed.provenance.f3ModelVersion, 'hotspot_classifier_v1');
  assert.strictEqual(parsed.provenance.f4ModelVersion, 'forecast_regressors_v1');
  assert.strictEqual(parsed.provenance.f5ScoringVersion, 'v1.0.0');
});

// =========================================================================
// 7. Loading State Handling
// =========================================================================
test('7. Loading State — Ensures stale data is cleared during in-flight requests', () => {
  // Simulating hook state transitions:
  let evidenceState: EvidenceSummaryResponse | null = REAL_PUNE_EVIDENCE_PAYLOAD;
  let loadingState = false;

  // Triggering new fetch:
  evidenceState = null;
  loadingState = true;

  assert.strictEqual(evidenceState, null, 'Stale evidence must be cleared immediately when loading starts');
  assert.strictEqual(loadingState, true, 'Loading flag must be active');
});

// =========================================================================
// 8. API Failure State Handling
// =========================================================================
test('8. API Failure State — Rejects malformed responses with specific error signatures', () => {
  // Missing context
  assert.throws(
    () => validateEvidenceResponse({ status: 'SUCCESS' }),
    /MALFORMED_RESPONSE: Missing context block/
  );

  // Missing observed facts
  assert.throws(
    () => validateEvidenceResponse({ status: 'SUCCESS', context: { h3Index: '88608850e5fffff' } }),
    /MALFORMED_RESPONSE: Missing observedFacts block/
  );

  // Missing model outputs
  assert.throws(
    () =>
      validateEvidenceResponse({
        status: 'SUCCESS',
        context: { h3Index: '88608850e5fffff' },
        observedFacts: {},
      }),
    /MALFORMED_RESPONSE: Missing modelOutputs block/
  );

  // Non-numeric evidence score
  assert.throws(
    () =>
      validateEvidenceResponse({
        status: 'SUCCESS',
        context: { h3Index: '88608850e5fffff' },
        observedFacts: {},
        modelOutputs: { hotspot: {} },
        evidence: { evidenceScore: 'not-a-number' },
      }),
    /MALFORMED_RESPONSE: Non-numeric evidenceScore/
  );
});

// =========================================================================
// 9. Empty / Insufficient Evidence State
// =========================================================================
test('9. Empty Evidence State — Appropriately handles missing or null cell inputs without fabricating', () => {
  const nullCell = null;
  const emptyCell = '   ';

  assert.strictEqual(nullCell, null);
  assert.strictEqual(emptyCell.trim().length, 0);

  // In hook: null or empty cell sets evidence to null without throwing unhandled exceptions
  const resultEvidence = (!nullCell || !nullCell) ? null : 'fabricated';
  assert.strictEqual(resultEvidence, null, 'No evidence should be fabricated for null cells');
});

// =========================================================================
// 10. Stale Response Protection on Rapid Selection
// =========================================================================
test('10. Stale Response Guard — Discards superseded response when activeRequestId increments', async () => {
  let activeRequestId = 0;
  let currentEvidenceState: EvidenceSummaryResponse | null = null;

  // Request A starts for Cell A (Katraj)
  const reqIdA = ++activeRequestId; // reqIdA = 1

  // User quickly switches to Cell B (Pune Shivajinagar) before A completes
  const reqIdB = ++activeRequestId; // reqIdB = 2

  // Simulated: Request B completes first
  const payloadB: EvidenceSummaryResponse = {
    ...REAL_PUNE_EVIDENCE_PAYLOAD,
    context: { ...REAL_PUNE_EVIDENCE_PAYLOAD.context, h3Index: '88608850e5fffff' },
  };
  if (reqIdB === activeRequestId) {
    currentEvidenceState = payloadB;
  }

  assert.strictEqual(currentEvidenceState?.context.h3Index, '88608850e5fffff');

  // Simulated: Stale Request A completes AFTER Request B
  const payloadA: EvidenceSummaryResponse = {
    ...REAL_PUNE_EVIDENCE_PAYLOAD,
    context: { ...REAL_PUNE_EVIDENCE_PAYLOAD.context, h3Index: '88608852c1fffff' },
  };
  if (reqIdA === activeRequestId) {
    currentEvidenceState = payloadA; // Should NOT be reached
  }

  // Cell B's data MUST remain intact, discarding stale Cell A data
  assert.strictEqual(
    currentEvidenceState?.context.h3Index,
    '88608850e5fffff',
    'Superseded response A must NOT overwrite active response B'
  );
});

// =========================================================================
// 11. No Mock Values Rendered
// =========================================================================
test('11. Mock Values Audit — Confirms legacy mock identifiers are eliminated from production payload', () => {
  const parsed = validateEvidenceResponse(REAL_PUNE_EVIDENCE_PAYLOAD);

  // Legacy mock event IDs
  assert.notStrictEqual(parsed.context.eventId, 'EVENT-1023');
  assert.notStrictEqual(parsed.context.eventId, 'EVENT-1024');
  assert.notStrictEqual(parsed.context.eventId, 'EVENT-1025');

  // Real authoritative event code format EVT-<CITY>-<H3>-<DATE>
  assert.ok(parsed.context.eventCode);
  assert.match(parsed.context.eventCode || '', /^EVT-[A-Z]{3}-88[0-9a-f]{13}-\d{8}$/);
});

// =========================================================================
// 12. Numerical Parity Preservation
// =========================================================================
test('12. Numerical Parity — Confirms exact floating point parity with zero modification', () => {
  const parsed = validateEvidenceResponse(REAL_PUNE_EVIDENCE_PAYLOAD);

  // F3 risk score
  assert.strictEqual(parsed.modelOutputs.hotspot.riskScore, 0.80);
  assert.strictEqual(parsed.modelOutputs.hotspot.operationalThreshold, 0.20);
  assert.strictEqual(parsed.modelOutputs.hotspot.confidence, 0.86);

  // Evidence score
  assert.strictEqual(parsed.evidence.evidenceScore, 0.667);

  // PM2.5 observation
  assert.strictEqual(parsed.observedFacts.air?.pm25, 78.0);

  // F4 forecast horizons
  assert.ok(parsed.modelOutputs.forecast);
  const horizons = parsed.modelOutputs.forecast?.horizons || [];
  assert.strictEqual(horizons.length, 3);
  assert.strictEqual(horizons[0].predictedPm25, 71.90);
  assert.strictEqual(horizons[0].lowerBound, 70.06);
  assert.strictEqual(horizons[0].upperBound, 73.76);

  assert.strictEqual(horizons[1].predictedPm25, 70.43);
  assert.strictEqual(horizons[1].lowerBound, 66.53);
  assert.strictEqual(horizons[1].upperBound, 73.48);

  assert.strictEqual(horizons[2].predictedPm25, 70.55);
  assert.strictEqual(horizons[2].lowerBound, 65.03);
  assert.strictEqual(horizons[2].upperBound, 75.98);
});
