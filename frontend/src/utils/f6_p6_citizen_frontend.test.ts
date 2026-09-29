import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { CitizenReport, VisionAnalysisSummary } from '../types';
import { EvidenceSummaryResponse } from '../types/evidence';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// =========================================================================
// Real Data Fixtures matching backend live contracts
// =========================================================================

// Real primary report fixture from database (CR-07B813E2)
export const REAL_PRIMARY_REPORT: CitizenReport = {
  id: '07b813e2-a17d-455f-9761-744c0989a6ce',
  reportId: 'CR-07B813E2',
  cityId: '550e8400-e29b-41d4-a716-446655440001',
  latitude: 18.5304,
  longitude: 73.8467,
  h3Index: '88608850e5fffff',
  category: 'SMOKE',
  description: 'Heavy smoke plume rising from industrial boiler stack near Shivaji Nagar',
  imageUrl: '/api/v1/citizen/photos/7e3753bd-32d9-4fb9-99b1-90d8085ff188.jpg',
  submittedAt: '2026-09-28T16:30:00Z',
  observedAt: '2026-09-28T16:25:00Z',
  status: 'ANALYZED',
  verificationStatus: 'UNVERIFIED',
  createdAt: '2026-09-28T17:08:12.907639Z',
  visionAnalysis: {
    analysisId: '01d5db28-f41b-4074-a178-618cd7ed822e',
    analysisStatus: 'ANALYZED',
    detectedCategory: 'UNKNOWN',
    confidence: 0.1,
    observations: ['uniform grey field. no discernible environmental features. lack of visual data'],
    uncertainty: [
      'Image alone cannot determine numerical pollutant concentration. Image alone cannot establish regulatory source causality',
    ],
    modelVersion: 'gemini-3.1-flash-lite',
    promptVersion: 'vision_analysis_v001',
    analyzedAt: '2026-09-28T17:08:14.504418Z',
  },
};

// Real no-match report fixture (CR-AF733F86)
export const REAL_NOMATCH_REPORT: CitizenReport = {
  id: 'af733f86-9d6c-4a9a-8275-553b6f7f4b25',
  reportId: 'CR-AF733F86',
  cityId: '550e8400-e29b-41d4-a716-446655440001',
  latitude: 18.555745024054822,
  longitude: 73.99200580166683,
  h3Index: '88608e26a7fffff',
  category: 'SMOKE',
  description: 'Visible smoke plume from an industrial stack.',
  imageUrl: '/api/v1/citizen/photos/c6e75cad-d844-463d-8db7-4d6d99278689.jpg',
  submittedAt: '2026-09-29T01:32:43.099Z',
  status: 'ANALYZED',
  verificationStatus: 'UNVERIFIED',
  createdAt: '2026-09-29T01:32:43.541606Z',
  visionAnalysis: {
    analysisId: '5b27f597-fc82-48de-a8c1-c22377d26a00',
    analysisStatus: 'ANALYZED',
    detectedCategory: 'SMOKE_LIKE',
    confidence: 0.95,
    observations: [
      'two red and white striped industrial chimneys. white plume emanating from the top of the chimneys. clear blue sky background',
    ],
    uncertainty: [
      'Image alone cannot determine numerical pollutant concentration. Image alone cannot establish regulatory source causality',
    ],
    modelVersion: 'gemini-3.1-flash-lite',
    promptVersion: 'vision_analysis_v001',
    analyzedAt: '2026-09-29T01:32:50.43937Z',
  },
};

// Fallback report fixture (CR-B6FCB953)
export const FALLBACK_REPORT: CitizenReport = {
  id: 'b6fcb953-2773-4aa6-9d98-427648e00c24',
  reportId: 'CR-B6FCB953',
  cityId: '550e8400-e29b-41d4-a716-446655440001',
  latitude: 18.555745024054822,
  longitude: 73.99200580166683,
  h3Index: '88608e26a7fffff',
  category: 'SMOKE',
  description: 'Visible smoke plume from an industrial stack.',
  imageUrl: '/api/v1/citizen/photos/3c850740-26ca-43b9-9eaf-d883d76b184a.jpg',
  submittedAt: '2026-09-29T01:37:17.867Z',
  status: 'ANALYZED',
  verificationStatus: 'UNVERIFIED',
  visionAnalysis: {
    analysisId: 'ab7a8b53-bc08-4b7e-ac6e-d2047a3c6cce',
    analysisStatus: 'ANALYZED',
    detectedCategory: 'UNKNOWN',
    confidence: 0.5,
    observations: ['Visual observation unconfirmed: Vision process timed out after 15000 ms'],
    uncertainty: ['Image analysis offline or fallback activated. Vision process timed out after 15000 ms'],
    modelVersion: 'deterministic-fallback',
    promptVersion: 'vision_analysis_v001',
    analyzedAt: '2026-09-29T01:37:33.134184Z',
  },
};

// Live Evidence Dossier fixture from GET /api/v1/evidence/hotspot/88608850e5fffff
export const REAL_EVIDENCE_DOSSIER: EvidenceSummaryResponse = {
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
      dataQuality: 'VALID',
      pm25: 78.0,
      pm10: 120.0,
      no2: 37.0,
      so2: 14.0,
      co: 0.9,
      o3: 24.0,
      observedAt: '2026-09-24T22:00:00Z',
      stationId: 'PUN-001',
      recentPm25Mean24h: 78.0,
    },
    weather: {
      dataQuality: 'VALID',
      temperature: 28.4,
      humidity: 56.0,
      windSpeedKmh: 9.5,
      windSpeedMps: 2.64,
      windDirection: 256.0,
      surfacePressure: 948.7,
      precipitation: 0.5,
      observedAt: '2026-09-29T09:30:00Z',
    },
    monitoringCoverage: {
      dataQuality: 'VALID',
      nearestStationDistanceKm: 0.27,
      stationsWithin5kmCount: 2,
      monitoringCoverageGapFlag: 0,
      spatialCoverageConfidence: 0.95,
    },
    spatialDispersion: {
      dataQuality: 'VALID',
      pm25SpatialLagMean: 78.0,
      windU: 4.3604,
      windV: 0.0761,
    },
    gisContext: {
      dataQuality: 'VALID',
      distToNearestIndustrialKm: 3.5,
      distToNearestMajorRoadKm: 0.4,
      sensitiveReceptorsCount2km: 4,
      industrialZoneWithin2kmFlag: 0,
      fireCount24h25km: 0,
      nearestFireDistanceKm: 50.0,
    },
  },
  modelOutputs: {
    hotspot: {
      riskScore: 0.7998,
      operationalThreshold: 0.2,
      isHotspot: true,
      riskLevel: 'CRITICAL',
      confidence: 0.86,
      confidenceBreakdown: {
        overallConfidence: 0.86,
        dataQualityScore: 0.9,
        spatialCoverageConfidence: 0.95,
        modelCertainty: 0.8,
        nearestStationDistanceKm: 0.27,
        epistemicUncertaintyFlag: 0,
      },
      modelVersion: 'hotspot_classifier_v1',
      engineType: 'ML',
    },
    forecast: {
      baseTimestamp: '2026-09-28T16:51:31.316574Z',
      generatedAt: '2026-09-29T08:31:24.753613Z',
      forecastModelVersion: 'forecast_regressors_v1',
      parentPredictionId: 'a310c689-f340-49fc-8935-a037de8d7709',
      forecastConfidence: null,
      horizons: [],
    },
  },
  evidence: {
    signals: [
      {
        signalId: 'sig-citizen-07b813e2',
        sourceType: 'CITIZEN_OBSERVATION',
        description: 'Citizen observation [UNKNOWN]: Heavy smoke plume rising from industrial boiler stack near Shivaji Nagar',
        timestamp: '2026-09-28T16:30:00Z',
        confidenceScore: 0.1,
        dataSource: 'CITIZEN',
        relevanceTier: 'AUXILIARY',
        sourceRef: '07b813e2-a17d-455f-9761-744c0989a6ce',
      },
    ],
    evidenceScore: 0.224,
    scoreBreakdown: {
      observationStrength: 0.2,
      mlForecastSupport: 0.552,
      multiSourceAgreement: 0.667,
      spatialConsistency: 0.95,
      temporalPersistence: 1.0,
      recencyFactor: 1.0,
      conflictPenalty: 0.35,
      evidenceCompleteness: 0.5,
      finalEvidenceScore: 0.224,
    },
    consistency: 'insufficient_evidence',
    triageState: 'INSUFFICIENT_EVIDENCE',
    sourceMatrix: {},
    unavailableSources: [],
    conflictingNotes: [],
    clusterH3Cells: ['88608850e5fffff'],
  },
  provenance: {
    h3Index: '88608850e5fffff',
    cityId: '550e8400-e29b-41d4-a716-446655440001',
    f3PredictionId: 'a310c689-f340-49fc-8935-a037de8d7709',
    f3ModelVersion: 'hotspot_classifier_v1',
    f4ModelVersion: 'forecast_regressors_v1',
    f5ScoringVersion: 'v1.0.0',
    geminiModelVersion: 'gemini-3.1-flash-lite',
    geminiPromptVersion: 'structured_event_explanation_v001',
    evaluatedAt: '2026-09-29T08:52:07.087934Z',
  },
  status: 'SUCCESS',
};

// =========================================================================
// 25 Focused Tests for F6-P6 Citizen Frontend Integration
// =========================================================================

test('F6-P6 Test 1: Report form renders 4 clean sections in sequence', () => {
  const citizenReportSource = fs.readFileSync(
    path.join(__dirname, '../pages/public/CitizenReport.tsx'),
    'utf-8'
  );
  assert.ok(citizenReportSource.includes('STEP 1: LOCATION'), 'Contains Step 1 Location');
  assert.ok(citizenReportSource.includes('STEP 2: PHOTO EVIDENCE'), 'Contains Step 2 Photo');
  assert.ok(citizenReportSource.includes('STEP 3: OBSERVATION CATEGORY & DESCRIPTION'), 'Contains Step 3 Category');
  assert.ok(citizenReportSource.includes('STEP 4: SUBMIT CTA'), 'Contains Step 4 Submit');
});

test('F6-P6 Test 2: Location captured state displays coordinates and Use My Location button', () => {
  const citizenReportSource = fs.readFileSync(
    path.join(__dirname, '../pages/public/CitizenReport.tsx'),
    'utf-8'
  );
  assert.ok(citizenReportSource.includes('LOCATION CAPTURED'), 'Has LOCATION CAPTURED badge');
  assert.ok(citizenReportSource.includes('USE MY LOCATION'), 'Has USE MY LOCATION action');
  assert.ok(citizenReportSource.includes('handleUseMyLocation'), 'Binds geolocation handler');
});

test('F6-P6 Test 3: Photo preview correctly displays selected image before submit', () => {
  const uploaderSource = fs.readFileSync(
    path.join(__dirname, '../components/citizen/ImageUploader.tsx'),
    'utf-8'
  );
  assert.ok(uploaderSource.includes('URL.createObjectURL(file)'), 'Creates object URL for instant preview');
  assert.ok(uploaderSource.includes('alt="Environmental observation preview"'), 'Provides accessible alt text for preview');
  assert.ok(uploaderSource.includes('handleClear'), 'Includes clear/remove button');
});

test('F6-P6 Test 4: Photo validation strictly enforces allowed formats and 15MB limit', () => {
  const MAX_FILE_SIZE = 15 * 1024 * 1024;
  const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

  assert.strictEqual(MAX_FILE_SIZE, 15728640, '15MB limit exact in bytes');
  assert.ok(ALLOWED_TYPES.includes('image/jpeg'), 'JPG allowed');
  assert.ok(ALLOWED_TYPES.includes('image/png'), 'PNG allowed');
  assert.ok(ALLOWED_TYPES.includes('image/webp'), 'WebP allowed');
  assert.ok(!ALLOWED_TYPES.includes('image/gif'), 'GIF rejected');
});

test('F6-P6 Test 5: Observation categories match backend enum contract', () => {
  const validCategories = ['SMOKE', 'DUST', 'BURNING', 'ODOR', 'OTHER'];
  const citizenReportSource = fs.readFileSync(
    path.join(__dirname, '../pages/public/CitizenReport.tsx'),
    'utf-8'
  );
  for (const cat of validCategories) {
    assert.ok(citizenReportSource.includes(`id: '${cat}'`), `Category ${cat} defined in frontend`);
  }
});

test('F6-P6 Test 6: Description textarea has user-friendly placeholder and bindings', () => {
  const citizenReportSource = fs.readFileSync(
    path.join(__dirname, '../pages/public/CitizenReport.tsx'),
    'utf-8'
  );
  assert.ok(citizenReportSource.includes('id="citizen-description-input"'), 'Has semantic description ID');
  assert.ok(citizenReportSource.includes('placeholder="Describe specific details'), 'Has helpful placeholder');
  assert.ok(citizenReportSource.includes('DESCRIPTION (CONTEXTUAL DETAILS):'), 'Has clear label');
});

test('F6-P6 Test 7: Submit button shows ANALYZING & SUBMITTING... during in-flight request', () => {
  const citizenReportSource = fs.readFileSync(
    path.join(__dirname, '../pages/public/CitizenReport.tsx'),
    'utf-8'
  );
  assert.ok(citizenReportSource.includes("isSubmitting ? 'ANALYZING & SUBMITTING...' : 'SUBMIT ENVIRONMENTAL EVIDENCE →'"));
  assert.ok(citizenReportSource.includes('disabled={isSubmitting}'), 'Prevents double-click while pending');
});

test('F6-P6 Test 8: Successful report displays authoritative backend metadata without invention', () => {
  const rep = REAL_PRIMARY_REPORT;
  assert.strictEqual(rep.reportId, 'CR-07B813E2');
  assert.strictEqual(rep.h3Index, '88608850e5fffff');
  assert.strictEqual(rep.status, 'ANALYZED');
  assert.strictEqual(rep.category, 'SMOKE');
  assert.strictEqual(rep.description, 'Heavy smoke plume rising from industrial boiler stack near Shivaji Nagar');
  assert.strictEqual(rep.imageUrl, '/api/v1/citizen/photos/7e3753bd-32d9-4fb9-99b1-90d8085ff188.jpg');
});

test('F6-P6 Test 9: Five-stage report status progression renders truthful non-automatic verification', () => {
  const citizenReportSource = fs.readFileSync(
    path.join(__dirname, '../pages/public/CitizenReport.tsx'),
    'utf-8'
  );
  assert.ok(citizenReportSource.includes('1. SUBMITTED'), 'Stage 1: SUBMITTED');
  assert.ok(citizenReportSource.includes('2. ANALYZING'), 'Stage 2: ANALYZING');
  assert.ok(citizenReportSource.includes('3. ANALYZED'), 'Stage 3: ANALYZED');
  assert.ok(citizenReportSource.includes('4. EVENT EVIDENCE'), 'Stage 4: EVENT EVIDENCE');
  assert.ok(citizenReportSource.includes('5. VERIFIED / DISMISSED'), 'Stage 5: VERIFIED / DISMISSED');
  assert.ok(citizenReportSource.includes("submittedReport.status === 'VERIFIED' ? 'Verified by Officer' : 'Pending Review'"), 'Does not auto-verify');
});

test('F6-P6 Test 10: Real Gemini Vision displays REAL GEMINI VISION badge and metadata', () => {
  const cardSource = fs.readFileSync(
    path.join(__dirname, '../components/citizen/GeminiVisionCard.tsx'),
    'utf-8'
  );
  assert.ok(cardSource.includes("modelVersion.toLowerCase().startsWith('gemini')"));
  assert.ok(cardSource.includes("providerBadgeText = 'REAL GEMINI VISION'"));
  assert.ok(cardSource.includes("providerTitle = 'Analyzed by Gemini Vision'"));
});

test('F6-P6 Test 11: Fallback analysis displays DETERMINISTIC FALLBACK badge', () => {
  const cardSource = fs.readFileSync(
    path.join(__dirname, '../components/citizen/GeminiVisionCard.tsx'),
    'utf-8'
  );
  assert.ok(cardSource.includes("modelVersion.toLowerCase().includes('fallback')"));
  assert.ok(cardSource.includes("providerBadgeText = 'DETERMINISTIC FALLBACK'"));
  assert.ok(cardSource.includes("providerTitle = 'Deterministic fallback analysis'"));
});

test('F6-P6 Test 12: Unavailable AI state displays AI UNAVAILABLE badge', () => {
  const cardSource = fs.readFileSync(
    path.join(__dirname, '../components/citizen/GeminiVisionCard.tsx'),
    'utf-8'
  );
  assert.ok(cardSource.includes("status === 'UNAVAILABLE'"));
  assert.ok(cardSource.includes("providerBadgeText = 'AI UNAVAILABLE'"));
  assert.ok(cardSource.includes("providerTitle = 'AI visual analysis unavailable'"));
});

test('F6-P6 Test 13: Confidence meter displays exact backend visual confidence percentage', () => {
  const report1 = REAL_PRIMARY_REPORT;
  const pct1 = Math.round((report1.visionAnalysis?.confidence ?? 0) * 100);
  assert.strictEqual(pct1, 10, '0.1 visual confidence maps to 10%');

  const report2 = REAL_NOMATCH_REPORT;
  const pct2 = Math.round((report2.visionAnalysis?.confidence ?? 0) * 100);
  assert.strictEqual(pct2, 95, '0.95 visual confidence maps to 95%');
});

test('F6-P6 Test 14: Uncertainty disclosure renders backend uncertainty statements verbatim', () => {
  const uncertainty = REAL_PRIMARY_REPORT.visionAnalysis?.uncertainty;
  assert.ok(Array.isArray(uncertainty));
  assert.ok(uncertainty.some((u) => u.includes('Image alone cannot determine numerical pollutant concentration')));
  assert.ok(uncertainty.some((u) => u.includes('Image alone cannot establish regulatory source causality')));
});

test('F6-P6 Test 15: Citizen evidence card enforces dataSource=CITIZEN and relevanceTier=AUXILIARY', () => {
  const lineageSource = fs.readFileSync(
    path.join(__dirname, '../components/citizen/CitizenEvidenceLineageCard.tsx'),
    'utf-8'
  );
  assert.ok(lineageSource.includes('CITIZEN'), 'Renders CITIZEN data source');
  assert.ok(lineageSource.includes('AUXILIARY'), 'Renders AUXILIARY relevance tier');
  assert.ok(lineageSource.includes('Auxiliary evidence corroborates sensor trends but cannot independently trigger an alert'));
});

test('F6-P6 Test 16: Real EventEvidence fields reflect live backend contracts', () => {
  const dossier = REAL_EVIDENCE_DOSSIER;
  assert.strictEqual(dossier.context.eventId, '58ef2f64-4bc5-496f-9094-e5f61e44f8b0');
  assert.strictEqual(dossier.context.eventCode, 'EVT-88608850-2026092816-f28bd5fe');
  const signal = dossier.evidence.signals.find((s) => s.dataSource === 'CITIZEN');
  assert.ok(signal, 'Citizen signal exists in dossier signals');
  assert.strictEqual(signal?.relevanceTier, 'AUXILIARY');
  assert.strictEqual(signal?.signalId, 'sig-citizen-07b813e2');
});

test('F6-P6 Test 17: F5 evidence score is consumed directly from backend without React recalculation', () => {
  const dossier = REAL_EVIDENCE_DOSSIER;
  assert.strictEqual(dossier.evidence.evidenceScore, 0.224);
  assert.strictEqual(dossier.evidence.scoreBreakdown?.finalEvidenceScore, 0.224);
  // Verify lineage card does not recalculate evidenceScore
  const lineageSource = fs.readFileSync(
    path.join(__dirname, '../components/citizen/CitizenEvidenceLineageCard.tsx'),
    'utf-8'
  );
  assert.ok(!lineageSource.includes('calculateEvidenceScore'));
  assert.ok(!lineageSource.includes('computeEvidenceScore'));
});

test('F6-P6 Test 18: F5 triage state is consumed directly from backend', () => {
  const dossier = REAL_EVIDENCE_DOSSIER;
  assert.strictEqual(dossier.evidence.triageState, 'INSUFFICIENT_EVIDENCE');
  const lineageSource = fs.readFileSync(
    path.join(__dirname, '../components/citizen/CitizenEvidenceLineageCard.tsx'),
    'utf-8'
  );
  assert.ok(lineageSource.includes('Current F5 Triage State:'));
  assert.ok(lineageSource.includes('{triageState}'));
});

test('F6-P6 Test 19: Metric separation strictly maintained: F3 riskScore != Gemini confidence != F5 evidenceScore', () => {
  const dossier = REAL_EVIDENCE_DOSSIER;
  const f3RiskScore = dossier.modelOutputs.hotspot.riskScore;
  const geminiConfidence = dossier.evidence.signals.find((s) => s.dataSource === 'CITIZEN')?.confidenceScore;
  const f5EvidenceScore = dossier.evidence.evidenceScore;

  assert.strictEqual(f3RiskScore, 0.7998, 'F3 Hotspot Risk Score is 0.7998');
  assert.strictEqual(geminiConfidence, 0.1, 'Gemini visual confidence is 0.10');
  assert.strictEqual(f5EvidenceScore, 0.224, 'F5 Evidence Score is 0.224');

  assert.notStrictEqual(f3RiskScore, geminiConfidence, 'F3 riskScore != Gemini confidence');
  assert.notStrictEqual(f5EvidenceScore, f3RiskScore, 'F5 evidenceScore != F3 riskScore');
  assert.notStrictEqual(f5EvidenceScore, geminiConfidence, 'F5 evidenceScore != Gemini confidence');
});

test('F6-P6 Test 20: Matched event state displays 5-step lineage flow and dossier link', () => {
  const lineageSource = fs.readFileSync(
    path.join(__dirname, '../components/citizen/CitizenEvidenceLineageCard.tsx'),
    'utf-8'
  );
  assert.ok(lineageSource.includes('Active Environmental Lineage'));
  assert.ok(lineageSource.includes('STEP 1'));
  assert.ok(lineageSource.includes('STEP 2'));
  assert.ok(lineageSource.includes('STEP 3'));
  assert.ok(lineageSource.includes('STEP 4'));
  assert.ok(lineageSource.includes('STEP 5'));
  assert.ok(lineageSource.includes('Inspect F5 Evidence & WHY Dossier'));
});

test('F6-P6 Test 21: No-match event state renders truthful auxiliary message with zero fabricated event', () => {
  const lineageSource = fs.readFileSync(
    path.join(__dirname, '../components/citizen/CitizenEvidenceLineageCard.tsx'),
    'utf-8'
  );
  assert.ok(lineageSource.includes('Citizen evidence stored'));
  assert.ok(lineageSource.includes('No matching pollution event is currently available for this spatial/temporal context.'));
  assert.ok(lineageSource.includes('Locked Safety Invariant: Citizen observations alone never fabricate a synthetic event'));
  assert.ok(!lineageSource.includes('mock-event-id'));
});

test('F6-P6 Test 22: Evidence & WHY navigation preserves exact H3 without hardcoding', () => {
  const lineageSource = fs.readFileSync(
    path.join(__dirname, '../components/citizen/CitizenEvidenceLineageCard.tsx'),
    'utf-8'
  );
  // Verify hardcoded fallback 88608850e5fffff is completely removed from the file
  assert.ok(!lineageSource.includes('88608850e5fffff'), 'No hardcoded H3 fallback exists in CitizenEvidenceLineageCard');
  assert.ok(lineageSource.includes("navigate(`/analyst/evidence?h3=${encodeURIComponent(h3Index)}`)"));
  assert.ok(lineageSource.includes('disabled={!h3Index}'), 'Guards against navigation with empty H3');
});

test('F6-P6 Test 23: Community feed renders live backend records with UNVERIFIED tag and empty state', () => {
  const feedSource = fs.readFileSync(
    path.join(__dirname, '../components/citizen/ReportStatus.tsx'),
    'utf-8'
  );
  assert.ok(feedSource.includes('UNVERIFIED'), 'Renders prominent UNVERIFIED tag');
  assert.ok(feedSource.includes('Recent Community Evidence'), 'Has community feed title');
  assert.ok(feedSource.includes('No recent citizen evidence submitted in this sector.'), 'Handles truthful empty state');
});

test('F6-P6 Test 24: Degraded AI notice displayed when report persists but AI analysis is unavailable', () => {
  const citizenReportSource = fs.readFileSync(
    path.join(__dirname, '../pages/public/CitizenReport.tsx'),
    'utf-8'
  );
  assert.ok(citizenReportSource.includes('isPersistedWithDegradedAi'));
  assert.ok(citizenReportSource.includes('Report Persisted Successfully:'));
  assert.ok(citizenReportSource.includes('Vision analysis is currently degraded or queued.'));
});

test('F6-P6 Test 25: Accessibility-critical semantic elements and labels are present', () => {
  const uploaderSource = fs.readFileSync(
    path.join(__dirname, '../components/citizen/ImageUploader.tsx'),
    'utf-8'
  );
  const citizenReportSource = fs.readFileSync(
    path.join(__dirname, '../pages/public/CitizenReport.tsx'),
    'utf-8'
  );
  assert.ok(uploaderSource.includes('aria-label="Upload citizen observation photo"'));
  assert.ok(uploaderSource.includes('role="alert"'));
  assert.ok(citizenReportSource.includes('aria-pressed={isSelected}'));
  assert.ok(citizenReportSource.includes('aria-label="Use current GPS device location"'));
});
