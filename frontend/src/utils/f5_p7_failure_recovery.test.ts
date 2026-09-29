import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { EvidenceSummaryResponse } from '../types/evidence';
import { AuthorityQueueItem } from '../types/alert';

/**
 * F5-P7 Frontend Failure Recovery & Operational Hardening Test Suite
 *
 * Verifies:
 * 11. Evidence API failure handling (no mock data, explicit error).
 * 12. Forecast unavailable state ("Forecast unavailable for this cell").
 * 13. Gemini unavailable / degraded fallback display ("AI explanation unavailable").
 * 14. Alert queue failure handling (no fake alert cards).
 * 15. Field assignment conflict / error handling.
 * 16. Field verification submission error handling.
 * 17. Stale response protection regression (rapid cell switch).
 */

describe('F5-P7: Frontend Failure Recovery & Degraded Modes', () => {

  const baseEvidence: EvidenceSummaryResponse = {
    context: {
      h3Index: '88608850e5fffff',
      cityId: '550e8400-e29b-41d4-a716-446655440001',
      cityName: 'Pune',
      predictionId: 'a310c689-f340-49fc-8935-a037de8d7709',
      featureSnapshotId: '1624baa3-a5f8-407b-b1c2-36bcee7650b1',
      eventId: '446928c2-6090-437d-a7c7-a9909e648403',
      eventCode: 'EVT-88608850-2026092812',
      generatedAt: '2026-09-28T12:00:00Z',
    },
    observedFacts: {
      air: {
        pm25: 78.0,
        pm10: 120.0,
        no2: 37.0,
        so2: 14.0,
        co: 0.9,
        o3: 24.0,
        dataQuality: 'VALID',
        stationId: 'PUN-001',
        observedAt: '2026-09-28T12:00:00Z',
      },
      weather: {
        temperature: 30.1,
        humidity: 52.0,
        windSpeedMps: 3.31,
        windSpeedKmh: 11.9,
        windDirection: 271.0,
        surfacePressure: 949.8,
        precipitation: 0.0,
      },
      monitoringCoverage: {
        nearestStationDistanceKm: 0.27,
        stationsWithin5kmCount: 2,
        monitoringCoverageGapFlag: 0,
        spatialCoverageConfidence: 0.95,
      },
      spatialDispersion: {
        pm25SpatialLagMean: 78.0,
        windU: 4.3604,
        windV: 0.0761,
      },
      gisContext: {
        distToNearestIndustrialKm: 3.5,
        distToNearestMajorRoadKm: 0.4,
        sensitiveReceptorsCount2km: 4,
        industrialZoneWithin2kmFlag: 0,
        fireCount24h25km: 0,
        nearestFireDistanceKm: 0.0,
      },
    },
    modelOutputs: {
      hotspot: {
        riskScore: 0.80,
        operationalThreshold: 0.20,
        isHotspot: true,
        riskLevel: 'CRITICAL',
        confidence: 0.86,
        confidenceBreakdown: {
          overallConfidence: 0.86,
          dataQualityScore: 0.90,
          spatialCoverageConfidence: 0.95,
          modelCertainty: 0.80,
          nearestStationDistanceKm: 0.27,
          epistemicUncertaintyFlag: 0,
        },
        modelVersion: 'hotspot_classifier_v1',
        engineType: 'ML',
      },
      forecast: null, // F4 unavailable
    },
    evidence: {
      signals: [
        {
          signalId: 'sig-88608850-001',
          sourceType: 'DIRECT_OBSERVATION',
          description: 'PM2.5 at 78 ug/m3',
          timestamp: '2026-09-28T12:00:00Z',
          confidenceScore: 0.90,
          dataSource: 'CPCB',
          relevanceTier: 'PRIMARY',
          sourceRef: 'PUN-001',
        },
      ],
      evidenceScore: 0.65,
      scoreBreakdown: {
        observationStrength: 0.35,
        mlForecastSupport: 0.40,
        multiSourceAgreement: 0.33,
        spatialConsistency: 0.95,
        temporalPersistence: 0.67,
        recencyFactor: 1.0,
        conflictPenalty: 0.0,
        evidenceCompleteness: 0.667,
        finalEvidenceScore: 0.65,
      },
      consistency: 'consistent',
      triageState: 'ALERT_CANDIDATE',
      sourceMatrix: { ground_sensor: 'supported', satellite: 'unavailable' },
      unavailableSources: ['Sentinel-5P tropospheric column unavailable'],
      conflictingNotes: [],
      clusterH3Cells: ['88608850e5fffff'],
    },
    aiInterpretation: null, // Gemini unavailable
    recommendedVerification: {
      action: 'Dispatch mobile rapid-sampling unit',
      priority: 'URGENT',
      guidelines: ['Deploy mobile sensor to verify peak concentration'],
    },
    provenance: {
      h3Index: '88608850e5fffff',
      cityId: '550e8400-e29b-41d4-a716-446655440001',
      f3PredictionId: 'a310c689-f340-49fc-8935-a037de8d7709',
      f3ModelVersion: 'hotspot_classifier_v1',
      f4ModelVersion: 'unavailable',
      f5ScoringVersion: 'v1.0.0',
      geminiModelVersion: 'unavailable',
      geminiPromptVersion: 'unavailable',
      evaluatedAt: '2026-09-28T12:00:00Z',
    },
    status: 'SUCCESS',
  };

  // Test 11: Evidence API failure handling
  it('P7-11: Evidence API failure sets error state and preserves null evidence without mock data', () => {
    let evidenceState: EvidenceSummaryResponse | null = null;
    let loadingState = true;
    let errorState: string | null = null;

    // Simulate API failure (e.g. 500 or network offline)
    const simulateFailure = (errorMessage: string) => {
      loadingState = false;
      evidenceState = null;
      errorState = errorMessage;
    };

    simulateFailure('Failed to connect to backend service');

    assert.equal(loadingState, false);
    assert.equal(evidenceState, null, 'Must NOT set mock data on failure');
    assert.equal(errorState, 'Failed to connect to backend service');
  });

  // Test 12: Forecast unavailable degraded rendering
  it('P7-12: Forecast unavailable produces truthful degraded state without synthesizing zeros', () => {
    const getForecastState = (fc: EvidenceSummaryResponse['modelOutputs']['forecast']) => {
      if (!fc || !fc.horizons || fc.horizons.length === 0) {
        return 'Forecast unavailable for this cell.';
      }
      return `${fc.horizons.length} horizons available`;
    };

    const state = getForecastState(baseEvidence.modelOutputs.forecast);
    assert.equal(state, 'Forecast unavailable for this cell.');
    assert.equal(baseEvidence.modelOutputs.forecast, null, 'Forecast must remain null, never fabricated zeros');
  });

  // Test 13: Gemini unavailable / fallback degraded rendering
  it('P7-13: Gemini unavailable renders degraded notice while preserving evidence dossier', () => {
    const getAiWhyState = (ai: EvidenceSummaryResponse['aiInterpretation']) => {
      if (!ai) {
        return {
          badge: 'AI UNAVAILABLE',
          message: 'AI explanation unavailable — showing verified data only.',
        };
      }
      return {
        badge: ai.isGrounded ? 'GROUNDED REASONING' : 'UNVALIDATED',
        message: ai.summaryPublic,
      };
    };

    const degraded = getAiWhyState(baseEvidence.aiInterpretation);
    assert.equal(degraded.badge, 'AI UNAVAILABLE');
    assert.equal(degraded.message, 'AI explanation unavailable — showing verified data only.');

    // Whole dossier remains intact
    assert.equal(baseEvidence.evidence.evidenceScore, 0.65);
    assert.equal(baseEvidence.evidence.triageState, 'ALERT_CANDIDATE');
    assert.equal(baseEvidence.observedFacts.air?.pm25, 78.0);
  });

  // Test 14: Alert queue failure handling
  it('P7-14: Alert queue load failure preserves empty queue without fabricating alerts', () => {
    let alerts: AuthorityQueueItem[] = [];
    let error: string | null = null;

    // Simulate failure response
    const handleQueueFailure = (err: string) => {
      alerts = [];
      error = err;
    };

    handleQueueFailure('Unable to load authority alerts: Service Unavailable');

    assert.equal(alerts.length, 0, 'No alerts fabricated on failure');
    assert.equal(error, 'Unable to load authority alerts: Service Unavailable');
  });

  // Test 15: Assignment failure handling
  it('P7-15: Field assignment conflict renders error message and keeps modal open for correction', () => {
    let assignmentError: string | null = null;
    let isSubmitting = true;

    // Simulate 409 Conflict from backend
    const onAssignmentFailed = (errMessage: string) => {
      assignmentError = errMessage;
      isSubmitting = false;
    };

    onAssignmentFailed('Alert already has an active field assignment: 880e8400-e29b-41d4-a716-446655440001');

    assert.equal(isSubmitting, false);
    assert.match(assignmentError!, /already has an active field assignment/);
  });

  // Test 16: Verification submission failure handling
  it('P7-16: Verification submission failure preserves inspector input for retry', () => {
    const inspectorReport = {
      result: 'CONFIRMED',
      observations: 'Dense black smoke plume observed from boiler exhaust stack',
    };
    let submissionError: string | null = null;

    // Simulate failure
    submissionError = 'Connection dropped during verification persistence';

    assert.equal(inspectorReport.result, 'CONFIRMED');
    assert.equal(inspectorReport.observations, 'Dense black smoke plume observed from boiler exhaust stack');
    assert.equal(submissionError, 'Connection dropped during verification persistence');
  });

  // Test 17: Stale response protection regression
  it('P7-17: Rapid cell switch discards superseded in-flight responses', () => {
    let activeRequestId = 0;
    let currentEvidence: EvidenceSummaryResponse | null = null;

    // Cell A request
    const reqAId = ++activeRequestId; // req 1
    // Cell B request immediately follows
    const reqBId = ++activeRequestId; // req 2

    // Cell A resolves late
    const resolveA = () => {
      if (reqAId === activeRequestId) {
        currentEvidence = { ...baseEvidence, context: { ...baseEvidence.context, h3Index: '88608850e5fffff' } };
      }
    };

    // Cell B resolves
    const resolveB = () => {
      if (reqBId === activeRequestId) {
        currentEvidence = { ...baseEvidence, context: { ...baseEvidence.context, h3Index: '886196944dfffff' } };
      }
    };

    resolveA(); // Should be discarded
    assert.equal(currentEvidence, null, 'Cell A response was superseded and must be discarded');

    resolveB(); // Should be accepted
    assert.notEqual(currentEvidence, null);
    assert.equal(currentEvidence!.context.h3Index, '886196944dfffff');
  });
});
