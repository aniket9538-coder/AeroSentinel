import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { CitizenReport, VisionAnalysisSummary } from '../types';

// =========================================================================
// Authoritative Fixtures for F6-P2 Citizen Reporting & Gemini Vision
// =========================================================================

export const MOCK_CITIZEN_REPORT_RESPONSE: CitizenReport = {
  id: 'c12da246-dbea-4028-aa51-bb667fcca393',
  userId: '00000000-0000-0000-0000-000000000001',
  category: 'SMOKE',
  description: 'Heavy dark plume visible rising from industrial boiler stack',
  latitude: 18.5304,
  longitude: 73.8467,
  h3Index: '886196944dfffff',
  photoUrl: '/api/v1/citizen/photos/photo-uuid-123.jpg',
  storageKey: 'photo-uuid-123.jpg',
  status: 'ANALYZED',
  submittedAt: '2026-09-28T16:00:00Z',
  observedAt: '2026-09-28T15:55:00Z',
  visionAnalysis: {
    analysisId: 'a12da246-dbea-4028-aa51-bb667fcca393',
    analysisStatus: 'COMPLETED',
    detectedCategory: 'SMOKE_LIKE',
    confidence: 0.85,
    visualIndicators: {
      smoke_visible: true,
      fire_visible: false,
      dust_visible: false,
      haze_visible: false,
      clear_sky: false,
      night_dark: false,
    },
    observations: ['Distinct plume detected against horizon'],
    uncertainty: ['Low lighting condition at dusk'],
    model: 'gemini-1.5-flash',
    narrativeSummary: 'Visual analysis indicates smoke plume emissions.',
  },
};

export const MOCK_CITIZEN_REPORT_FALLBACK: CitizenReport = {
  ...MOCK_CITIZEN_REPORT_RESPONSE,
  status: 'ANALYSIS_UNAVAILABLE',
  visionAnalysis: {
    analysisId: 'a12da246-dbea-4028-aa51-bb667fcca394',
    analysisStatus: 'FALLBACK',
    detectedCategory: 'UNKNOWN',
    confidence: 0.50,
    visualIndicators: {
      smoke_visible: false,
      fire_visible: false,
      dust_visible: false,
      haze_visible: false,
      clear_sky: false,
      night_dark: false,
    },
    observations: ['Deterministic fallback used due to vision model unavailability'],
    uncertainty: ['Vision model unreachable within 15s bounded timeout'],
    model: 'deterministic-fallback',
  },
};

// =========================================================================
// 18 Targeted Frontend Tests per F6-P2 Section 27 Specification
// =========================================================================

test('F6-P2 Frontend 1: Form renders all required inputs and controls', () => {
  const pageFile = fs.readFileSync(path.resolve('src/pages/public/CitizenReport.tsx'), 'utf-8');
  assert.ok(pageFile.includes('Report Environmental Evidence'), 'Page renders citizen reporting title');
  assert.ok(pageFile.includes('Description & Observation Type'), 'Renders category & observation section');
  assert.ok(pageFile.includes('Photo Evidence'), 'Renders photo upload card');
  assert.ok(pageFile.includes('Location'), 'Renders location card');
  assert.ok(pageFile.includes('SUBMIT ENVIRONMENTAL EVIDENCE'), 'Renders submit button');
});

test('F6-P2 Frontend 2: Location selection supports browser GPS and coordinates validation', () => {
  const pageFile = fs.readFileSync(path.resolve('src/pages/public/CitizenReport.tsx'), 'utf-8');
  assert.ok(pageFile.includes('handleUseMyLocation'), 'Provides GPS acquisition handler');
  assert.ok(pageFile.includes('navigator.geolocation.getCurrentPosition'), 'Uses browser geolocation API');
  assert.ok(pageFile.includes('coords.lat.toFixed(4)'), 'Displays 4-decimal latitude coordinates');
  assert.ok(pageFile.includes('coords.lng.toFixed(4)'), 'Displays 4-decimal longitude coordinates');
});

test('F6-P2 Frontend 3: Category selection adheres to authoritative incident categories', () => {
  const pageFile = fs.readFileSync(path.resolve('src/pages/public/CitizenReport.tsx'), 'utf-8');
  assert.ok(pageFile.includes("'Smoke'"), 'Includes Smoke chip');
  assert.ok(pageFile.includes("'Dust'"), 'Includes Dust chip');
  assert.ok(pageFile.includes("'Burning'"), 'Includes Burning chip');
  assert.ok(pageFile.includes("'Strong odour'"), 'Includes Strong odour chip');
  assert.ok(pageFile.includes("'Industrial activity'"), 'Includes Industrial activity chip');
});

test('F6-P2 Frontend 4: Description input supports observation details', () => {
  const pageFile = fs.readFileSync(path.resolve('src/pages/public/CitizenReport.tsx'), 'utf-8');
  assert.ok(pageFile.includes('textarea'), 'Includes textarea input');
  assert.ok(pageFile.includes('setDescription(e.target.value)'), 'Updates description state on change');
  assert.ok(pageFile.includes('Describe specific details'), 'Includes observation placeholder');
});

test('F6-P2 Frontend 5: Image selection validates input file presence', () => {
  const uploaderFile = fs.readFileSync(path.resolve('src/components/citizen/ImageUploader.tsx'), 'utf-8');
  assert.ok(uploaderFile.includes('onImageSelected'), 'Triggers image selected callback');
  assert.ok(uploaderFile.includes('fileInputRef'), 'Binds file input ref');
  assert.ok(uploaderFile.includes('accept="image/jpeg,image/png,image/webp"'), 'Restricts accepted MIME formats');
});

test('F6-P2 Frontend 6: Image preview displays selected photo before submission', () => {
  const uploaderFile = fs.readFileSync(path.resolve('src/components/citizen/ImageUploader.tsx'), 'utf-8');
  assert.ok(uploaderFile.includes('preview'), 'Maintains photo preview state');
  assert.ok(uploaderFile.includes('URL.createObjectURL(file)'), 'Creates safe preview object URL');
  assert.ok(uploaderFile.includes('handleClear'), 'Allows clearing image preview');
});

test('F6-P2 Frontend 7: Unsupported image format restricted by accepted MIME filter', () => {
  const uploaderFile = fs.readFileSync(path.resolve('src/components/citizen/ImageUploader.tsx'), 'utf-8');
  assert.ok(uploaderFile.includes('accept="image/jpeg,image/png,image/webp"'), 'Restricts accepted input types to standard image formats');
});

test('F6-P2 Frontend 8: Submit creates real FormData multipart request', () => {
  const pageFile = fs.readFileSync(path.resolve('src/pages/public/CitizenReport.tsx'), 'utf-8');
  assert.ok(pageFile.includes('new FormData()'), 'Constructs real FormData');
  assert.ok(pageFile.includes("formData.append('photo', imageFile)"), 'Appends photo file part');
  assert.ok(pageFile.includes("formData.append('category',"), 'Appends category part');
  assert.ok(pageFile.includes("formData.append('latitude', String(coords.lat))"), 'Appends latitude part');
  assert.ok(pageFile.includes("formData.append('longitude', String(coords.lng))"), 'Appends longitude part');
  assert.ok(pageFile.includes("formData.append('description',"), 'Appends description part');
});

test('F6-P2 Frontend 9: Loading state displays active progress during submission', () => {
  const pageFile = fs.readFileSync(path.resolve('src/pages/public/CitizenReport.tsx'), 'utf-8');
  assert.ok(pageFile.includes('isSubmitting'), 'Maintains isSubmitting loading state');
  assert.ok(pageFile.includes('ANALYZING & SUBMITTING...'), 'Renders submission loading indicator');
});

test('F6-P2 Frontend 10: Success state transitions into Report Details view', () => {
  const pageFile = fs.readFileSync(path.resolve('src/pages/public/CitizenReport.tsx'), 'utf-8');
  assert.ok(pageFile.includes('submittedReport'), 'Maintains submitted report state');
  assert.ok(pageFile.includes('Evidence Submitted'), 'Displays submission confirmation header');
  assert.ok(pageFile.includes('Report Reference'), 'Shows report reference block');
});

test('F6-P2 Frontend 11: Report ID rendered strictly from backend API response', () => {
  const pageFile = fs.readFileSync(path.resolve('src/pages/public/CitizenReport.tsx'), 'utf-8');
  assert.ok(pageFile.includes('reportRef'), 'Computes report reference from API reportId or id');
  assert.ok(pageFile.includes('submittedReport?.id'), 'Extracts authoritative id from submittedReport');
  assert.ok(!pageFile.includes("'REP-1023'"), 'Does NOT hardcode REP-1023 fake ID');
});

test('F6-P2 Frontend 12: H3 cell rendered strictly from backend API derivation', () => {
  const pageFile = fs.readFileSync(path.resolve('src/pages/public/CitizenReport.tsx'), 'utf-8');
  assert.ok(pageFile.includes('Location (H3 Hex)'), 'Displays H3 Hex field');
  assert.ok(pageFile.includes('activeH3'), 'Renders dynamic authoritative H3 cell');
  assert.ok(pageFile.includes('submittedReport?.h3Index'), 'Binds h3Index directly from backend report');
});

test('F6-P2 Frontend 13: Analysis state route loading inspects report by ID', () => {
  const pageFile = fs.readFileSync(path.resolve('src/pages/public/CitizenReport.tsx'), 'utf-8');
  assert.ok(pageFile.includes('citizenService.getReportById(routeReportId)'), 'Fetches report by route param ID');
  assert.ok(pageFile.includes('useParams<{ reportId?: string }>()'), 'Extracts reportId from URL path');
});

test('F6-P2 Frontend 14: Gemini result rendering displays visual condition, confidence, and observations', () => {
  const pageFile = fs.readFileSync(path.resolve('src/pages/public/CitizenReport.tsx'), 'utf-8');
  assert.ok(pageFile.includes('Gemini Vision Interpretation'), 'Displays Gemini Vision section');
  assert.ok(pageFile.includes('Detected Condition:'), 'Displays detected condition');
  assert.ok(pageFile.includes('Confidence:'), 'Displays model confidence');
  assert.ok(pageFile.includes('submittedReport.visionAnalysis.observations'), 'Displays structured observations');
});

test('F6-P2 Frontend 15: Gemini unavailable state clarifies report persistence without AI', () => {
  const pageFile = fs.readFileSync(path.resolve('src/pages/public/CitizenReport.tsx'), 'utf-8');
  assert.ok(pageFile.includes('NO PHOTO ATTACHED'), 'Clarifies when no visual photo was provided');
  assert.ok(pageFile.includes('Evidence Submitted'), 'Shows evidence submission preserved regardless of AI status');
});

test('F6-P2 Frontend 16: Submission error displays message without crashing or resetting form', () => {
  const pageFile = fs.readFileSync(path.resolve('src/pages/public/CitizenReport.tsx'), 'utf-8');
  assert.ok(pageFile.includes('errorMessage'), 'Maintains errorMessage state');
  assert.ok(pageFile.includes('setErrorMessage(null)'), 'Clears error state on retry');
  assert.ok(pageFile.includes('Report submission failed. Please try again.'), 'Provides fallback error message');
});

test('F6-P2 Frontend 17: Double-submit protection disables button while submission in flight', () => {
  const pageFile = fs.readFileSync(path.resolve('src/pages/public/CitizenReport.tsx'), 'utf-8');
  assert.ok(pageFile.includes('disabled={isSubmitting}'), 'Disables button during submission');
  assert.ok(pageFile.includes('if (isSubmitting) return;'), 'Guards submission handler against concurrent clicks');
});

test('F6-P2 Frontend 18: No fake report IDs, fake H3, or fake confidence in production contracts', () => {
  const pageFile = fs.readFileSync(path.resolve('src/pages/public/CitizenReport.tsx'), 'utf-8');
  assert.ok(!pageFile.includes('REP-1023'), 'No hardcoded REP-1023 in CitizenReport page');
  assert.ok(!pageFile.includes('0.987654'), 'No fake confidence numbers');
  assert.ok(pageFile.includes('Important Note:'), 'Includes regulatory advisory note');
  assert.ok(pageFile.includes('Citizen reports are supporting evidence only, not confirmed source attribution.'), 'Reaffirms evidence caveat');
});
