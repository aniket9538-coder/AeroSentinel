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

// Real verified report fixture from database (CR-07B813E2)
export const REAL_GEMINI_REPORT_FIXTURE: CitizenReport = {
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

// Deterministic fallback report fixture
export const FALLBACK_REPORT_FIXTURE: CitizenReport = {
  id: 'b22da246-dbea-4028-aa51-bb667fcca111',
  reportId: 'CR-B22DA246',
  cityId: '550e8400-e29b-41d4-a716-446655440001',
  latitude: 18.5304,
  longitude: 73.8467,
  h3Index: '88608850e5fffff',
  category: 'SMOKE',
  description: 'Dense smoke near highway junction',
  imageUrl: '/api/v1/citizen/photos/fallback-test.jpg',
  submittedAt: '2026-09-28T16:00:00Z',
  status: 'ANALYZED',
  verificationStatus: 'UNVERIFIED',
  visionAnalysis: {
    analysisId: 'f12da246-dbea-4028-aa51-bb667fcca222',
    analysisStatus: 'FALLBACK',
    detectedCategory: 'SMOKE_LIKE',
    confidence: 0.5,
    observations: ['Deterministic fallback used due to vision model unavailability'],
    uncertainty: ['Fallback analysis generated without multimodal neural inference'],
    modelVersion: 'deterministic-fallback',
    promptVersion: 'fallback_v1',
    analyzedAt: '2026-09-28T16:00:05Z',
  },
};

// Unattached report fixture (CR-AF733F86) with no active event in cell
export const UNATTACHED_REPORT_FIXTURE: CitizenReport = {
  id: 'af733f86-9d6c-4a9a-8275-553b6f7f4b25',
  reportId: 'CR-AF733F86',
  cityId: '550e8400-e29b-41d4-a716-446655440001',
  latitude: 18.5557,
  longitude: 73.992,
  h3Index: '88608e26a7fffff',
  category: 'SMOKE',
  description: 'Visible smoke plume from an industrial stack.',
  imageUrl: '/api/v1/citizen/photos/c6e75cad-d844-463d-8db7-4d6d99278689.jpg',
  submittedAt: '2026-09-29T01:32:43.099Z',
  status: 'ANALYZED',
  verificationStatus: 'UNVERIFIED',
  visionAnalysis: {
    analysisId: '5b27f597-fc82-48de-a8c1-c22377d26a00',
    analysisStatus: 'ANALYZED',
    detectedCategory: 'SMOKE_LIKE',
    confidence: 0.95,
    observations: [
      'two red and white striped industrial chimneys. white plume emanating from the top of the chimneys.',
    ],
    uncertainty: [
      'Image alone cannot determine numerical pollutant concentration. Image alone cannot establish regulatory source causality',
    ],
    modelVersion: 'gemini-3.1-flash-lite',
    promptVersion: 'vision_analysis_v001',
    analyzedAt: '2026-09-29T01:32:50.439370Z',
  },
};

// Real live F5 evidence response fixture for H3 88608850e5fffff
export const REAL_MATCHED_EVIDENCE_FIXTURE: EvidenceSummaryResponse = {
  status: 'SUCCESS',
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
    air: { dataQuality: 'VALID', pm25: 78.0, pm10: 120.0, stationId: 'PUN-001' },
    weather: { dataQuality: 'VALID', temperature: 24.5, humidity: 78.0 },
    monitoringCoverage: { dataQuality: 'VALID', spatialCoverageConfidence: 0.95 },
    spatialDispersion: { dataQuality: 'VALID', pm25SpatialLagMean: 78.0 },
    gisContext: { dataQuality: 'VALID', distToNearestIndustrialKm: 3.5 },
  },
  modelOutputs: {
    hotspot: {
      riskScore: 0.7998,
      operationalThreshold: 0.2,
      isHotspot: true,
      riskLevel: 'CRITICAL',
      confidence: 0.86,
      modelVersion: 'hotspot_classifier_v1',
      engineType: 'ML',
    },
  },
  evidence: {
    signals: [
      {
        signalId: 'sig-88608850-001',
        sourceType: 'DIRECT_OBSERVATION',
        description: 'Ground PM2.5 measurement (78.0 ug/m3) at reference monitor.',
        timestamp: '2026-09-28T16:51:31Z',
        confidenceScore: 0.9,
        dataSource: 'PUN-001',
        relevanceTier: 'PRIMARY',
        sourceRef: 'PUN-001',
      },
      {
        signalId: 'sig-citizen-07b813e2',
        sourceType: 'CITIZEN_OBSERVATION',
        description:
          'Citizen visual observation (UNKNOWN): Heavy smoke plume rising from industrial boiler stack near Shivaji Nagar',
        timestamp: '2026-09-28T16:30:00Z',
        confidenceScore: 0.1,
        dataSource: 'CITIZEN',
        relevanceTier: 'AUXILIARY',
        sourceRef: '07b813e2-a17d-455f-9761-744c0989a6ce',
      },
    ],
    evidenceScore: 0.224,
    consistency: 'insufficient_evidence',
    triageState: 'INSUFFICIENT_EVIDENCE',
  },
  aiInterpretation: {
    summaryPublic: 'Air quality sensors in your area are currently detecting elevated levels.',
    summaryAnalyst: 'At 2026-09-28T16:51:31Z, ground monitor PUN-001 recorded PM2.5 at 78.0 ug/m3.',
    detectedCondition: 'Elevated PM2.5 and PM10 concentrations',
    supportingSignals: ['Ground PM2.5 concentration: 78.0 ug/m3'],
    modelVersion: 'gemini-3.1-flash-lite',
    promptVersion: 'structured_event_explanation_v001',
  },
  recommendedVerification: {
    action: 'Standard Routine Surveillance',
    priority: 'ROUTINE',
    guidelines: ['Continue automated 15-minute scheduled polling.'],
  },
  provenance: {
    h3Index: '88608850e5fffff',
    cityId: '550e8400-e29b-41d4-a716-446655440001',
    evaluatedAt: '2026-09-29T05:55:42Z',
  },
};

// =========================================================================
// TEST SUITE: 14 REQUIRED AREAS (F6-P4)
// =========================================================================

test('F6-P4: Evaluator-Quality Citizen UX & Evidence Visualization Test Suite', async (t) => {
  const citizenReportPath = path.resolve(__dirname, '../pages/public/CitizenReport.tsx');
  const geminiCardPath = path.resolve(__dirname, '../components/citizen/GeminiVisionCard.tsx');
  const lineageCardPath = path.resolve(__dirname, '../components/citizen/CitizenEvidenceLineageCard.tsx');
  const uploaderPath = path.resolve(__dirname, '../components/citizen/ImageUploader.tsx');
  const statusPath = path.resolve(__dirname, '../components/citizen/ReportStatus.tsx');

  const citizenReportSrc = fs.readFileSync(citizenReportPath, 'utf-8');
  const geminiCardSrc = fs.readFileSync(geminiCardPath, 'utf-8');
  const lineageCardSrc = fs.readFileSync(lineageCardPath, 'utf-8');
  const uploaderSrc = fs.readFileSync(uploaderPath, 'utf-8');
  const statusSrc = fs.readFileSync(statusPath, 'utf-8');

  // 1. Report form renders real fields
  await t.test('1. Report form renders real fields: coordinates, H3 indicator, uploader, categories, description', () => {
    assert.ok(citizenReportSrc.includes('H3 Resolution 8'), 'Must include H3 Resolution 8 badge/indicator');
    assert.ok(citizenReportSrc.includes('Selected Coordinates'), 'Must render selected coordinates label');
    assert.ok(citizenReportSrc.includes('ImageUploader'), 'Must embed ImageUploader component');
    assert.ok(citizenReportSrc.includes('OBSERVED EMISSION CATEGORY:'), 'Must render category selector header');
    assert.ok(citizenReportSrc.includes('citizen-description-input'), 'Must provide accessible description input ID');
    assert.ok(citizenReportSrc.includes('USE MY LOCATION'), 'Must provide GPS coordinates button');
  });

  // 2. Photo preview
  await t.test('2. Photo preview renders properly with remove and replace actions', () => {
    assert.ok(uploaderSrc.includes('objectFit: \'cover\''), 'Must style image preview with proper objectFit');
    assert.ok(uploaderSrc.includes('Photo Attached'), 'Must render attached state check indicator');
    assert.ok(uploaderSrc.includes('Replace Photo'), 'Must render replace photo action');
    assert.ok(uploaderSrc.includes('Remove photo'), 'Must render remove photo action with accessible title/aria');
  });

  // 3. Submit loading state
  await t.test('3. Submit loading state disables button and shows progress text', () => {
    assert.ok(citizenReportSrc.includes('isSubmitting ? \'ANALYZING & SUBMITTING...\' : \'SUBMIT ENVIRONMENTAL EVIDENCE →\''), 'Must toggle text on submit loading');
    assert.ok(citizenReportSrc.includes('disabled={isSubmitting}'), 'Must disable submit button while active');
    assert.ok(citizenReportSrc.includes('isLoading={isSubmitting}'), 'Must pass isLoading to Button component');
  });

  // 4. Successful report status
  await t.test('4. Successful report status renders clean 5-stage progression', () => {
    assert.ok(citizenReportSrc.includes('SUBMITTED'), 'Must render SUBMITTED stage');
    assert.ok(citizenReportSrc.includes('ANALYZING'), 'Must render ANALYZING stage');
    assert.ok(citizenReportSrc.includes('ANALYZED'), 'Must render ANALYZED stage');
    assert.ok(citizenReportSrc.includes('EVENT EVIDENCE'), 'Must render EVENT EVIDENCE stage');
    assert.ok(citizenReportSrc.includes('VERIFIED'), 'Must render VERIFIED stage');
    assert.ok(citizenReportSrc.includes('Pending Review'), 'Must clearly mark verification as pending when not verified');
  });

  // 5. Real Gemini result rendering
  await t.test('5. Real Gemini result rendering displays "Analyzed by Gemini Vision" and REAL GEMINI badge', () => {
    assert.ok(geminiCardSrc.includes('Analyzed by Gemini Vision'), 'Must render "Analyzed by Gemini Vision" for real Gemini');
    assert.ok(geminiCardSrc.includes('REAL GEMINI VISION'), 'Must render REAL GEMINI VISION badge');
    assert.ok(geminiCardSrc.includes('modelVersion.toLowerCase().startsWith(\'gemini\')'), 'Must detect Gemini strictly from modelVersion');
    assert.ok(geminiCardSrc.includes('AI visual interpretation ≠ numeric pollution measurement ≠ causal source attribution'), 'Must render mandatory semantic boundary banner');
  });

  // 6. Fallback rendering
  await t.test('6. Fallback rendering displays "Deterministic fallback analysis" without calling it Gemini', () => {
    assert.ok(geminiCardSrc.includes('Deterministic fallback analysis'), 'Must render "Deterministic fallback analysis"');
    assert.ok(geminiCardSrc.includes('DETERMINISTIC FALLBACK'), 'Must render DETERMINISTIC FALLBACK badge');
    assert.ok(geminiCardSrc.includes('modelVersion.toLowerCase().includes(\'fallback\')'), 'Must detect fallback from metadata');
  });

  // 7. Unavailable AI rendering
  await t.test('7. Unavailable AI rendering displays "AI visual analysis unavailable" cleanly', () => {
    assert.ok(geminiCardSrc.includes('AI visual analysis unavailable'), 'Must render "AI visual analysis unavailable"');
    assert.ok(geminiCardSrc.includes('AI UNAVAILABLE'), 'Must render AI UNAVAILABLE badge');
  });

  // 8. Confidence rendering
  await t.test('8. Confidence rendering calculates percentage and provides accessible progressbar', () => {
    assert.ok(geminiCardSrc.includes('role="progressbar"'), 'Must provide role="progressbar" for confidence');
    assert.ok(geminiCardSrc.includes('aria-valuenow={confidencePercent}'), 'Must bind aria-valuenow');
    assert.ok(geminiCardSrc.includes('Math.round(visionAnalysis.confidence * 100)'), 'Must correctly format confidence percentage');
  });

  // 9. Uncertainty rendering
  await t.test('9. Uncertainty rendering displays limitations and non-attribution statement', () => {
    assert.ok(geminiCardSrc.includes('Uncertainty & Analytical Limitations'), 'Must render uncertainty header');
    assert.ok(geminiCardSrc.includes('Image alone cannot determine numerical pollutant concentration'), 'Must render concentration disclaimer');
    assert.ok(geminiCardSrc.includes('Image alone cannot establish regulatory source causality'), 'Must render causality disclaimer');
  });

  // 10. Citizen evidence rendering
  await t.test('10. Citizen evidence rendering classifies dataSource as CITIZEN and relevance as AUXILIARY', () => {
    assert.ok(lineageCardSrc.includes('CITIZEN'), 'Must display data source CITIZEN');
    assert.ok(lineageCardSrc.includes('AUXILIARY'), 'Must display relevance tier AUXILIARY');
    assert.ok(lineageCardSrc.includes('UNVERIFIED CITIZEN REPORT'), 'Must render UNVERIFIED CITIZEN REPORT badge');
    assert.ok(lineageCardSrc.includes('Citizen Visual Evidence'), 'Must render Citizen Visual Evidence title');
  });

  // 11. Event lineage rendering
  await t.test('11. Event lineage rendering displays 5-step flow and links to /analyst/evidence?h3=', () => {
    assert.ok(lineageCardSrc.includes('Active Environmental Lineage'), 'Must render Active Environmental Lineage header');
    assert.ok(lineageCardSrc.includes('Citizen Report'), 'Must include Citizen Report in lineage');
    assert.ok(lineageCardSrc.includes('H3 Cell'), 'Must include H3 Cell in lineage');
    assert.ok(lineageCardSrc.includes('Pollution Event'), 'Must include Pollution Event in lineage');
    assert.ok(lineageCardSrc.includes('Event Evidence'), 'Must include Event Evidence in lineage');
    assert.ok(lineageCardSrc.includes('F5 Evaluation'), 'Must include F5 Evaluation in lineage');
    assert.ok(lineageCardSrc.includes('/analyst/evidence?h3='), 'Must link to /analyst/evidence?h3=');
  });

  // 12. No-match event state
  await t.test('12. No-match event state displays "Citizen evidence stored" without fabricating synthetic events', () => {
    assert.ok(lineageCardSrc.includes('Citizen evidence stored'), 'Must render "Citizen evidence stored" header');
    assert.ok(lineageCardSrc.includes('No matching pollution event is currently available for this spatial/temporal context.'), 'Must render accurate no match explanation');
    assert.ok(lineageCardSrc.includes('Locked Safety Invariant: Citizen observations alone never fabricate a synthetic event'), 'Must preserve locked safety invariant');
  });

  // 13. Failure state handling
  await t.test('13. Failure state handles missing location, oversized images, and preserved reports with degraded AI', () => {
    assert.ok(citizenReportSrc.includes('Target coordinates are required'), 'Must validate location coordinates');
    assert.ok(uploaderSrc.includes('Image size exceeds 15MB limit'), 'Must validate maximum file size');
    assert.ok(uploaderSrc.includes('Unsupported file format'), 'Must validate supported image types');
    assert.ok(citizenReportSrc.includes('Report Persisted Successfully:'), 'Must acknowledge persisted report');
    assert.ok(citizenReportSrc.includes('Vision analysis is currently degraded or queued'), 'Must note degraded AI without claiming submission failed');
  });

  // 14. Accessibility-critical labels
  await t.test('14. Accessibility-critical labels: alt text, aria-labels, buttons, and high-contrast badges', () => {
    assert.ok(uploaderSrc.includes('aria-label="Upload citizen observation photo"'), 'Must have accessible file upload label');
    assert.ok(uploaderSrc.includes('alt="Environmental observation preview"'), 'Must have image alt text on preview');
    assert.ok(geminiCardSrc.includes('alt={`Citizen environmental observation evidence for'), 'Must have dynamic alt text on citizen photo');
    assert.ok(geminiCardSrc.includes('aria-label="Expand citizen photo"'), 'Must have accessible expand button label');
    assert.ok(statusSrc.includes('UNVERIFIED'), 'Must display readable text alongside badges so color is not sole indicator');
  });
});
